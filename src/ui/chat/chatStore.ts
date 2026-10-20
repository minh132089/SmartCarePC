import { create } from 'zustand'
import { FAMILY_IDS, type FamilyId } from '../../data/families'
import { playDone, playSubmit, startThinking, stopThinking } from '../sound/sfx'

export type Issue = { rule: string; message: string }
export type BuildLine = {
  code: string
  category: string
  name: string
  quantity: number
  /** Có khi trợ lý chốt cấu hình (propose_build): thông số nên mua, lý do chọn, giá tham khảo cho cả dòng */
  spec?: string | null
  reason?: string | null
  priceMinVnd?: number | null
  priceMaxVnd?: number | null
  /**
   * Sản phẩm thật trợ lý tìm được (search_products): tên, cửa hàng, ảnh và trang mua. Backend chỉ giữ link có trong
   * kết quả tìm kiếm, nên không có link bịa. Chưa tìm được thì các trường này null và hiện khung ảnh mẫu.
   */
  productName?: string | null
  shop?: string | null
  imageUrl?: string | null
  sourceUrl?: string | null
  /** Engine so giá đọc từ trang cửa hàng: còn hàng, bảo hành (tháng), lúc kiểm tra giá (ISO); null nếu không biết */
  inStock?: boolean | null
  warrantyMonths?: number | null
  checkedAt?: string | null
  /** Cùng model ở cửa hàng khác, rẻ nhất trước */
  compare?: ShopPrice[] | null
  /** Giá đã đọc lại trên chính trang của sourceUrl; false là giá tham khảo từ Google Shopping, chưa kiểm tra */
  priceChecked?: boolean | null
}
export type ShopPrice = { shop: string; priceVnd: number; url: string; inStock?: boolean | null; checkedAt?: string | null }
/** Cấu hình trợ lý vừa kiểm tra bằng bộ kiểm tra tương thích của backend */
export type BuildCard = {
  items: BuildLine[]
  compatible: boolean
  errors: Issue[]
  warnings: Issue[]
  /**
   * Theo bộ luật chuẩn (docs/pc_compatibility_rules.md): unknowns là điểm app chưa xác minh được (vd CPU Support List
   * của bo, gá tản), recommendations là lời khuyên không bắt buộc
   */
  unknowns?: Issue[] | null
  recommendations?: Issue[] | null
  /** Nhu cầu trợ lý đã hiểu và ngân sách người dùng nói (chỉ có ở cấu hình đã chốt) */
  summary?: string | null
  budgetVnd?: number | null
  /** Ghi chú khi số thanh RAM hoặc số quạt khác máy mẫu 3D của vỏ đã chọn (các món chính luôn khớp) */
  modelNote?: string | null
  /** Phương án: cheap = Rẻ, balanced = Phù hợp cho bạn, quality = Chất lượng */
  tier?: Tier | null
  /** Build trong phương án: 1 = trợ lý chọn; 2, 3 = cùng linh kiện, sản phẩm khác giá gần để tham khảo */
  variant?: number
}
/** Build đang xem trong bảng so sánh */
export type Pick = { tier: Tier; variant: number }
export type Tier = 'cheap' | 'balanced' | 'quality'
/** options: ba phương án để so sánh (khi trợ lý đưa đủ); build là phương án "Phù hợp cho bạn" */
export type Msg = { role: 'user' | 'assistant'; content: string; build?: BuildCard | null; options?: BuildCard[] | null }
/** advice: trang tư vấn chọn máy. assembly: khung hỏi đáp khi lắp. */
export type ChatMode = 'advice' | 'assembly'
export type ChatContext = { buildCode?: string | null; guideStep?: string | null }

/** Gửi tối đa từng này tin gần nhất, đủ để trợ lý nhớ mạch chuyện mà không quá dài */
const HISTORY = 20
const OFFLINE = 'Chưa kết nối được máy chủ. Hãy bật backend (cổng 8083) rồi thử lại.'
/** Hỏi lại kết quả vài giây một lần; mạng chập chờn thì bỏ qua tới từng này lần hỏi hỏng liên tiếp (~15 giây) */
const POLL_MS = 1500
const MAX_MISSES = 10

type ChatReply = { reply: string; build?: BuildCard | null; options?: BuildCard[] | null }
type JobView = { id: string; status: 'running' | 'done' | 'error'; stage?: string | null; result?: ChatReply | null; error?: string | null }

/** Backend báo lỗi ở detail (ProblemDetail); proxy trên Cloudflare Pages báo ở message */
function errorText(data: { detail?: string; message?: string } | null, status: number) {
  return data?.detail ?? data?.message ?? (status >= 500 ? OFFLINE : 'Trợ lý chưa trả lời được, bạn thử lại nhé.')
}

function sleep(ms: number, signal: AbortSignal) {
  return new Promise<void>((resolve, reject) => {
    const t = setTimeout(resolve, ms)
    signal.addEventListener('abort', () => (clearTimeout(t), reject(new DOMException('aborted', 'AbortError'))), { once: true })
  })
}

/**
 * Câu hỏi chạy nền ở backend: gửi lấy mã việc, rồi hỏi lại tới khi xong. Không yêu cầu nào kéo dài cả phút nên không
 * bị cắt giữa chừng (link demo qua Cloudflare cắt ở 100 giây). onStage nhận bước backend đang làm.
 */
async function ask(body: unknown, signal: AbortSignal, onStage: (stage: string | null) => void): Promise<ChatReply> {
  const res = await fetch('/api/chat/jobs', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body), signal })
  const job = await res.json().catch(() => null)
  if (!res.ok) throw new Error(errorText(job, res.status))
  let misses = 0
  for (;;) {
    await sleep(POLL_MS, signal)
    let r: Response
    try {
      r = await fetch(`/api/chat/jobs/${job.id}`, { signal })
    } catch (e) {
      if (signal.aborted || ++misses > MAX_MISSES) throw e
      continue
    }
    const data: JobView | null = await r.json().catch(() => null)
    if (!r.ok || !data) {
      // Lỗi mạng / tunnel thoáng qua: hỏi lại; 404 (việc không còn) hay lỗi kéo dài thì báo
      if (r.status >= 500 && ++misses <= MAX_MISSES) continue
      throw new Error(errorText(data as never, r.status))
    }
    misses = 0
    if (data.status === 'done' && data.result) return data.result
    if (data.status === 'error') throw new Error(data.error ?? 'Trợ lý chưa trả lời được, bạn thử lại nhé.')
    onStage(data.stage ?? null)
  }
}

interface ChatState {
  messages: Msg[]
  busy: boolean
  /** Bước backend đang làm khi chờ trả lời (vd "Đang tìm giá ở các cửa hàng…") */
  stage: string | null
  error: string | null
  /** Gửi một câu; history mặc định là hội thoại hiện tại (retry truyền hội thoại đã bỏ câu lỗi) */
  send: (text: string, ctx: ChatContext, history?: Msg[]) => Promise<void>
  retry: (ctx: ChatContext) => void
  reset: () => void
}

/**
 * Mỗi chế độ chat có một kho riêng, nằm ngoài component nên chuyển trang qua lại hội thoại vẫn còn.
 * Gọi /api/chat/jobs của backend (Vite chuyển tiếp sang cổng 8083); khoá AI chỉ nằm ở backend.
 */
function createChat(mode: ChatMode) {
  let abort: AbortController | null = null
  return create<ChatState>((set, get) => ({
    messages: [],
    busy: false,
    stage: null,
    error: null,
    send: async (text, ctx, history) => {
      const content = text.trim()
      if (!content || get().busy) return
      const next: Msg[] = [...(history ?? get().messages), { role: 'user', content }]
      set({ messages: next, busy: true, stage: null, error: null })
      playSubmit()
      startThinking()
      const ctrl = new AbortController()
      abort = ctrl
      try {
        const body = { mode, ...ctx, messages: next.slice(-HISTORY).map(toWire) }
        const data = await ask(body, ctrl.signal, (stage) => abort === ctrl && set({ stage }))
        set({ messages: [...next, { role: 'assistant', content: data.reply, build: data.build, options: data.options }] })
        stopThinking()
        playDone()
      } catch (e) {
        if (ctrl.signal.aborted) return
        set({ error: e instanceof TypeError ? OFFLINE : (e as Error).message })
      } finally {
        if (abort === ctrl) {
          abort = null
          stopThinking()
          set({ busy: false, stage: null })
        }
      }
    },
    /** Lỗi thì bỏ câu hỏi vừa gửi ra khỏi hội thoại rồi gửi lại đúng câu đó */
    retry: (ctx) => {
      const { messages, send } = get()
      const last = messages.at(-1)
      if (last?.role === 'user') void send(last.content, ctx, messages.slice(0, -1))
    },
    reset: () => {
      abort?.abort()
      abort = null
      stopThinking()
      set({ messages: [], busy: false, stage: null, error: null })
    },
  }))
}

export const useAdviceChat = createChat('advice')
export const useAssemblyChat = createChat('assembly')

/**
 * Tin gửi lên backend chỉ gồm role và content. Thẻ cấu hình được ghi kèm dưới dạng mã linh kiện,
 * để khi người dùng nói "đổi card khác" trợ lý vẫn biết cấu hình trước đó gồm những gì.
 */
function toWire(m: Msg) {
  if (!m.build) return { role: m.role, content: m.content }
  const parts = m.build.items.map((i) => `${i.code}×${i.quantity}`).join(', ')
  return { role: m.role, content: `${m.content}\n\n[Cấu hình đã kiểm tra: ${parts}]` }
}

/** Tổng giá tham khảo của cấu hình (null nếu trợ lý chưa đưa giá) */
export function totalOf(card: BuildCard): { min: number; max: number } | null {
  const priced = card.items.filter((i) => i.priceMinVnd != null || i.priceMaxVnd != null)
  if (priced.length === 0) return null
  return priced.reduce(
    (t, i) => ({ min: t.min + (i.priceMinVnd ?? i.priceMaxVnd ?? 0), max: t.max + (i.priceMaxVnd ?? i.priceMinVnd ?? 0) }),
    { min: 0, max: 0 },
  )
}

/** Đổi số đồng thành chữ dễ đọc: 12 500 000 → "12,5 triệu", 850 000 → "850 nghìn" */
export function vnd(n: number): string {
  if (n >= 1e6) return `${(n / 1e6).toLocaleString('vi-VN', { maximumFractionDigits: 1 })} triệu`
  return `${Math.round(n / 1e3).toLocaleString('vi-VN')} nghìn`
}

/** Khoảng giá: "11,2 - 13 triệu"; hai đầu bằng nhau thì chỉ ghi một số */
export function vndRange(min: number, max: number): string {
  if (Math.abs(max - min) < 50_000) return vnd(max)
  if (min >= 1e6 && max >= 1e6) return `${(min / 1e6).toLocaleString('vi-VN', { maximumFractionDigits: 1 })} - ${vnd(max)}`
  return `${vnd(min)} - ${vnd(max)}`
}

/** Kiểu máy 3D tương ứng với vỏ trong cấu hình (vd case_mini_sff → mini_sff), null nếu app chưa có mô hình */
export function machineOfCard(card: BuildCard): FamilyId | 'gaming' | null {
  const code = card.items.find((i) => i.category === 'case')?.code.replace(/^case_/, '')
  if (code === 'gaming') return 'gaming'
  return (FAMILY_IDS as readonly string[]).includes(code ?? '') ? (code as FamilyId) : null
}

/**
 * Dòng trạng thái của thẻ cấu hình. Theo bộ luật chuẩn, chưa xác minh hết (UNKNOWN) thì không khẳng định là lắp chung được.
 */
export function compatText(card: BuildCard): { ok: boolean; short: string; long: string } {
  if (!card.compatible) return { ok: false, short: 'Cấu hình này chưa lắp được', long: 'Cấu hình này chưa lắp được, xem lỗi bên dưới.' }
  if (card.unknowns?.length)
    return { ok: true, short: 'Chưa thấy lỗi lắp chung', long: 'Chưa thấy lỗi lắp chung. Còn vài điểm cần xác minh trước khi mua (xem bên dưới).' }
  return { ok: true, short: 'Các linh kiện lắp chung được', long: 'Các linh kiện lắp chung được.' }
}
