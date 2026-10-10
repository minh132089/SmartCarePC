import { create } from 'zustand'
import { FAMILY_IDS, type FamilyId } from '../../data/families'

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
  /** Ảnh sản phẩm trợ lý tìm được trên web và trang chứa ảnh đó (https); chưa có thì hiện khung ảnh mẫu */
  imageUrl?: string | null
  sourceUrl?: string | null
}
/** Cấu hình trợ lý vừa kiểm tra bằng bộ kiểm tra tương thích của backend */
export type BuildCard = {
  items: BuildLine[]
  compatible: boolean
  errors: Issue[]
  warnings: Issue[]
  /** Nhu cầu trợ lý đã hiểu và ngân sách người dùng nói (chỉ có ở cấu hình đã chốt) */
  summary?: string | null
  budgetVnd?: number | null
}
export type Msg = { role: 'user' | 'assistant'; content: string; build?: BuildCard | null }
/** advice: trang tư vấn chọn máy. assembly: khung hỏi đáp khi lắp. */
export type ChatMode = 'advice' | 'assembly'
export type ChatContext = { buildCode?: string | null; guideStep?: string | null }

/** Gửi tối đa từng này tin gần nhất, đủ để trợ lý nhớ mạch chuyện mà không quá dài */
const HISTORY = 20
const OFFLINE = 'Chưa kết nối được máy chủ. Hãy bật backend (cổng 8083) rồi thử lại.'

interface ChatState {
  messages: Msg[]
  busy: boolean
  error: string | null
  /** Gửi một câu; history mặc định là hội thoại hiện tại (retry truyền hội thoại đã bỏ câu lỗi) */
  send: (text: string, ctx: ChatContext, history?: Msg[]) => Promise<void>
  retry: (ctx: ChatContext) => void
  reset: () => void
}

/**
 * Mỗi chế độ chat có một kho riêng, nằm ngoài component nên chuyển trang qua lại hội thoại vẫn còn.
 * Gọi /api/chat của backend (Vite chuyển tiếp sang cổng 8083); khoá AI chỉ nằm ở backend.
 */
function createChat(mode: ChatMode) {
  let abort: AbortController | null = null
  return create<ChatState>((set, get) => ({
    messages: [],
    busy: false,
    error: null,
    send: async (text, ctx, history) => {
      const content = text.trim()
      if (!content || get().busy) return
      const next: Msg[] = [...(history ?? get().messages), { role: 'user', content }]
      set({ messages: next, busy: true, error: null })
      const ctrl = new AbortController()
      abort = ctrl
      try {
        const res = await fetch('/api/chat', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ mode, ...ctx, messages: next.slice(-HISTORY).map(toWire) }),
          signal: ctrl.signal,
        })
        const data = await res.json().catch(() => null)
        if (!res.ok) throw new Error(data?.detail ?? (res.status >= 500 ? OFFLINE : 'Trợ lý chưa trả lời được, bạn thử lại nhé.'))
        set({ messages: [...next, { role: 'assistant', content: data.reply, build: data.build }] })
      } catch (e) {
        if (ctrl.signal.aborted) return
        set({ error: e instanceof TypeError ? OFFLINE : (e as Error).message })
      } finally {
        if (abort === ctrl) {
          abort = null
          set({ busy: false })
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
      set({ messages: [], busy: false, error: null })
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
