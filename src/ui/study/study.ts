import { create } from 'zustand'
import { participantAnswers } from './participant'

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
  join: (code: string, phase: Phase) => void
  leave: () => void
  open: (dialog: StudyState['dialog']) => void
  markAnswered: (key: string) => void
  setOwnPc: (value: string) => void
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
  // Chưa trả lời form của lần đo này thì mở ngay khi vào app, trước mọi hướng dẫn
  return { code: saved.code, phase: saved.phase, answered: saved.answered ?? [], ownPc: saved.ownPc ?? null, dialog: saved.code ? pendingForm(saved.phase, saved.answered ?? []) : null }
}

export const useStudy = create<StudyState>((set, get) => ({
  ...initial(),
  join: (code, phase) => {
    const s = get()
    const c = code.trim().toUpperCase()
    const answered = s.code === c && s.phase === phase ? s.answered : []
    const ownPc = s.code === c ? s.ownPc : null
    write(KEY, { code: c, phase, answered, ownPc: ownPc ?? undefined })
    // Nhập mã xong là vào thẳng form của lần đo này (lần đầu: trước khi xem gợi ý hay hướng dẫn)
    set({ code: c, phase, answered, ownPc, dialog: pendingForm(phase, answered) })
  },
  leave: () => {
    write(KEY, null)
    set({ code: null, phase: 'before', answered: [], ownPc: null, dialog: null })
  },
  open: (dialog) => set({ dialog }),
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

/** Gửi lại những phản hồi chưa tới được máy chủ (mất mạng, backend tắt) */
export async function flushQueue() {
  const queue = read<Payload[]>(QUEUE, [])
  if (queue.length === 0) return
  const left: Payload[] = []
  for (const p of queue) {
    try {
      await post(p)
    } catch (e) {
      if (!(e instanceof InvalidFeedback)) left.push(p)
    }
  }
  write(QUEUE, left.length ? left : null)
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
    return write(QUEUE, [...read<Payload[]>(QUEUE, []), payload].slice(-200)) ? 'queued' : 'failed'
  } finally {
    useSending.setState({ sending: false })
  }
}

if (typeof window !== 'undefined') {
  window.addEventListener('online', () => void flushQueue())
  void flushQueue()
}
