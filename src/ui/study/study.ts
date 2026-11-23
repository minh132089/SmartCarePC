import { create } from 'zustand'
import { participantAnswers } from './participant'
import type { RatingKind } from './questions'

/**
 * Chế độ thử nghiệm 4 tuần của nhóm nghiên cứu. Người tham gia mở link có ?thunghiem=P07 hoặc tự nhập mã,
 * từ đó app hiện dải "Thử nghiệm" có nút mở form phản hồi (câu hỏi trong questions.ts).
 * Mã thử nghiệm được lưu riêng; hồ sơ tự nguyện xác nhận ở trang Cloudflare được gắn khi gửi feedback.
 */
export type Phase = 'before' | 'after'
export type Answers = Record<string, unknown>

export const PHASE_LABEL: Record<Phase, string> = { before: "First use", after: "End of study" }
export const CODE_PATTERN = /^[A-Za-z0-9_-]{1,20}$/

const KEY = 'pcsc-study'
const QUEUE = 'pcsc-feedback-queue'

// localStorage có thể bị chặn (ẩn danh, chặn cookie): khi đó chế độ thử nghiệm chỉ sống tới lúc tải lại trang
function read<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : fallback
  } catch {
    return fallback
  }
}
/** Ghi localStorage; trả về false khi bị chặn (ẩn danh, chặn cookie, đầy) */
function write(key: string, value: unknown) {
  try {
    if (value == null) localStorage.removeItem(key)
    else localStorage.setItem(key, JSON.stringify(value))
    return true
  } catch {
    return false
  }
}

interface Saved {
  code: string
  phase: Phase
  /** Đã gửi gì trong lần đo này: "baseline" (form lần đầu), "followup" (form cuối), "feedback" (form chung / chấm sao) */
  answered: string[]
  /** Câu own_pc của form lần đầu: chỉ người có máy bàn mới được dẫn sang bảo dưỡng và được hỏi về bảo dưỡng ở form cuối */
  ownPc?: string
}

interface StudyState {
  code: string | null
  phase: Phase
  answered: string[]
  ownPc: string | null
  /**
   * Hộp thoại đang mở: nhập mã, form lần đầu (baseline), form cuối nghiên cứu (followup), form phản hồi chung,
   * hoặc chấm sao sau khi lắp xong cả máy (form phản hồi nằm dưới)
   */
  dialog: 'join' | 'baseline' | 'followup' | 'feedback' | 'rating' | null
  /** Hộp chấm sao đang hỏi về chức năng nào (lắp máy, bảo dưỡng, nâng cấp) */
  ratingFor: RatingKind
  join: (code: string, phase: Phase) => void
  leave: () => void
  open: (dialog: StudyState['dialog']) => void
  /** Mở hộp chấm sao của một chức năng vừa xong */
  rate: (kind: RatingKind) => void
  markAnswered: (key: string) => void
  setOwnPc: (value: string) => void
}

/**
 * Người dùng bấm "Để sau" ở form đầu: không tự mở lại khi tải lại trang trong cùng phiên trình duyệt.
 * Khoá theo mã người tham gia và chỉ cho form đầu: người khác dùng chung tab, hay form cuối kỳ, vẫn được mời bình thường.
 */
const laterKey = (code: string) => `pcsc-baseline-later:${code}`
export function postponeBaseline() {
  const code = useStudy.getState().code
  if (!code) return
  try { sessionStorage.setItem(laterKey(code), '1') } catch { /* bỏ qua */ }
}
function postponed(code: string, form: 'baseline' | 'followup' | null) {
  if (form !== 'baseline') return false
  try { return sessionStorage.getItem(laterKey(code)) === '1' } catch { return false }
}

/** Form của lần đo này còn chưa trả lời: lần đầu là baseline, cuối nghiên cứu là followup */
export function pendingForm(phase: Phase, answered: string[]): 'baseline' | 'followup' | null {
  const form = phase === 'before' ? 'baseline' : 'followup'
  return answered.includes(form) ? null : form
}

function initial(): Pick<StudyState, 'code' | 'phase' | 'answered' | 'ownPc' | 'dialog'> {
  const saved = read<Saved | null>(KEY, null)
  // Link mời: ?thunghiem=P07. Đọc xong thì xoá khỏi thanh địa chỉ để mã không bị chép lan khi chia sẻ link.
  const params = typeof window !== 'undefined' ? new URLSearchParams(window.location.search) : null
  const fromUrl = params?.get('thunghiem')?.trim()
  if (fromUrl && CODE_PATTERN.test(fromUrl)) {
    const code = fromUrl.toUpperCase()
    // ?lan=sau: link mời cho lần đo sau 4 tuần
    const phase: Phase = params!.get('lan') === 'sau' ? 'after' : saved?.code === code ? saved.phase : 'before'
    params!.delete('thunghiem')
    params!.delete('lan')
    const q = params!.toString()
    window.history.replaceState(null, '', window.location.pathname + (q ? `?${q}` : '') + window.location.hash)
    const same = saved?.code === code && saved.phase === phase
    const ownPc = saved?.code === code ? saved.ownPc : undefined
    const next = { code, phase, answered: same ? saved!.answered : [], ownPc }
    write(KEY, next)
    // Mở link mời thì hiện hộp thoại tham gia để người dùng đọc lời giới thiệu và xác nhận mã
    return { ...next, ownPc: ownPc ?? null, dialog: same ? pendingForm(phase, next.answered) : 'join' }
  }
  if (!saved) return { code: null, phase: 'before', answered: [], ownPc: null, dialog: null }
  // Chưa trả lời form của lần đo này thì mở ngay khi vào app, trước mọi hướng dẫn (trừ khi vừa bấm "Để sau" trong phiên này)
  const form = saved.code ? pendingForm(saved.phase, saved.answered ?? []) : null
  return { code: saved.code, phase: saved.phase, answered: saved.answered ?? [], ownPc: saved.ownPc ?? null, dialog: form && !postponed(saved.code, form) ? form : null }
}

export const useStudy = create<StudyState>((set, get) => ({
  ...initial(),
  ratingFor: 'build',
  join: (code, phase) => {
    const s = get()
    const c = code.trim().toUpperCase()
    const answered = s.code === c && s.phase === phase ? s.answered : []
    const ownPc = s.code === c ? s.ownPc : null
    // Người tham gia khác trên cùng trình duyệt: xoá dấu tích lắp / bảo dưỡng lưu theo máy của người trước, để người sau không
    // thấy bài đã xong sẵn (QA review 23/11)
    if (s.code && s.code !== c) clearGuideProgress()
    write(KEY, { code: c, phase, answered, ownPc: ownPc ?? undefined })
    // Nhập mã xong là vào thẳng form của lần đo này (lần đầu: trước khi xem gợi ý hay hướng dẫn)
    set({ code: c, phase, answered, ownPc, dialog: pendingForm(phase, answered) })
  },
  leave: () => {
    write(KEY, null)
    set({ code: null, phase: 'before', answered: [], ownPc: null, dialog: null })
  },
  open: (dialog) => set(dialog === 'rating' ? { dialog, ratingFor: 'build' } : { dialog }),
  rate: (ratingFor) => set({ dialog: 'rating', ratingFor }),
  markAnswered: (key) => {
    const s = get()
    if (!s.code || s.answered.includes(key)) return
    const answered = [...s.answered, key]
    write(KEY, { code: s.code, phase: s.phase, answered, ownPc: s.ownPc ?? undefined })
    set({ answered })
  },
  setOwnPc: (value) => {
    const s = get()
    if (!s.code) return
    write(KEY, { code: s.code, phase: s.phase, answered: s.answered, ownPc: value })
    set({ ownPc: value })
  },
}))

type Payload = {
  participantCode: string | null
  phase: Phase | null
  machine?: string | null
  answers: Answers
}

class InvalidFeedback extends Error {}

async function post(p: Payload) {
  // Quá 20 giây thì huỷ (mạng treo): không để hộp thoại khoá mãi ở trạng thái "Đang gửi"
  const res = await fetch('/api/feedback', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(p),
    signal: AbortSignal.timeout(20_000),
  })
  // 400 là dữ liệu sai: gửi lại cũng vô ích nên bỏ, không giữ trong hàng đợi
  if (res.status === 400) throw new InvalidFeedback("Invalid feedback")
  if (!res.ok) throw new Error(String(res.status))
}

const idOf = (p: Payload) => String(p.answers?.submission_id ?? '')
const MAX_QUEUE = 200

/** Bỏ đúng một bản khỏi hàng đợi, đọc lại hàng đợi ngay lúc đó (bản mới thêm trong lúc đang gửi vẫn còn nguyên) */
function dropQueued(id: string) {
  const left = read<Payload[]>(QUEUE, []).filter((q) => idOf(q) !== id)
  write(QUEUE, left.length ? left : null)
}

/** Phản hồi này còn nằm chờ trên máy (chưa tới máy chủ) */
export const isQueued = (id: string) => read<Payload[]>(QUEUE, []).some((q) => idOf(q) === id)

let flushing: Promise<void> | null = null
/**
 * Gửi lại những phản hồi chưa tới được máy chủ (mất mạng, backend tắt). Mỗi lúc chỉ một lượt gửi; gửi xong bản nào thì xoá
 * đúng bản đó. Trước đây cả hàng đợi bị ghi đè bằng danh sách đọc từ đầu lượt, nên bản xếp hàng giữa chừng bị mất (QA review 23/11).
 * Máy chủ vẫn lỗi thì dừng lượt này, thử lại lần sau.
 */
export function flushQueue(): Promise<void> {
  if (flushing) return flushing
  flushing = (async () => {
    try {
      for (const p of read<Payload[]>(QUEUE, [])) {
        try {
          await post(p)
        } catch (e) {
          if (!(e instanceof InvalidFeedback)) break
        }
        dropQueued(idOf(p))
      }
    } finally {
      flushing = null
    }
  })()
  return flushing
}

export type SendResult = 'sent' | 'queued' | 'failed'

/** Đang gửi phản hồi (dùng chung giữa các lần mở hộp thoại): chặn gửi trùng khi đóng rồi mở lại giữa chừng */
export const useSending = create<{ sending: boolean }>(() => ({ sending: false }))

/**
 * Gửi phản hồi kèm mã và lần đo hiện tại. Mỗi lần gửi có submission_id cố định để máy chủ bỏ bản trùng (gửi lại từ hàng
 * đợi, bấm hai lần). Không tới được máy chủ thì cất vào hàng đợi trên máy để gửi bù.
 * Trả về 'sent' (máy chủ đã nhận), 'queued' (đã cất trên máy), 'failed' (không gửi được và không cất được: giữ form để thử lại).
 */
/** Mã một lần gửi: tạo một lần khi mở hộp thoại, giữ nguyên khi bấm "Gửi lại" để máy chủ nhận ra bản trùng */
export function newSubmissionId() {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export async function sendFeedback(data: Omit<Payload, 'participantCode' | 'phase'>, id: string = newSubmissionId()): Promise<SendResult> {
  if (useSending.getState().sending) return 'failed'
  useSending.setState({ sending: true })
  const { code, phase } = useStudy.getState()
  const payload: Payload = { participantCode: code, phase: code ? phase : null, ...data, answers: { ...participantAnswers(), ...data.answers, submission_id: id } }
  try {
    await post(payload)
    void flushQueue()
    return 'sent'
  } catch (e) {
    if (e instanceof InvalidFeedback) return 'failed'
    // Hàng đợi đầy thì báo không gửi được (giữ form) thay vì âm thầm bỏ bản cũ nhất
    const queue = read<Payload[]>(QUEUE, [])
    if (queue.length >= MAX_QUEUE) return 'failed'
    return write(QUEUE, [...queue, payload]) ? 'queued' : 'failed'
  } finally {
    useSending.setState({ sending: false })
  }
}

/** Xoá dấu tích theo máy của hướng dẫn lắp và bảo dưỡng (data/guide.ts: khoá pcsc-guide-done:*) */
function clearGuideProgress() {
  try {
    for (const k of Object.keys(localStorage)) if (k.startsWith('pcsc-guide-done:')) localStorage.removeItem(k)
  } catch { /* bỏ qua */ }
}

if (typeof window !== 'undefined') {
  window.addEventListener('online', () => void flushQueue())
  // Máy chủ có thể trở lại khi trình duyệt vẫn có mạng (không có sự kiện online): thử lại mỗi phút và khi quay lại tab
  window.addEventListener('focus', () => void flushQueue())
  window.setInterval(() => { if (read<Payload[]>(QUEUE, []).length) void flushQueue() }, 60_000)
  void flushQueue()
}
