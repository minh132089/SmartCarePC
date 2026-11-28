import { create } from 'zustand'
import { EMPTY_ANSWERS, maintenancePlan, TASKS, type MaintenanceTask } from '../../data/maintenance'
import { readStored, useMaintenance } from './maintenanceState'

/**
 * Email nhắc bảo dưỡng cho người đã đăng nhập (thay cho app khay Windows). Máy chủ gửi thư xác nhận trước, chủ hộp thư
 * bấm link thì mới nhận nhắc: server không kiểm tra đăng nhập nên bước này chống bật hộ email người khác.
 * Trình duyệt giữ token để gửi lại ngày hẹn mỗi khi lịch đổi (làm xong, hoãn) và để tắt.
 * Không gửi mã người tham gia nghiên cứu lên cùng email: dữ liệu nghiên cứu vẫn ẩn danh.
 */
const KEY = 'pcsc-email-reminder-v1'
const API = '/api/reminders/email'
/** Link trong thư chỉ trỏ về các địa chỉ này (app.reminders.origins ở backend) */
export const EMAIL_ORIGINS = ['https://smartcarepc.pages.dev', 'http://localhost:5173']
export interface EmailSub { token: string; email: string; status: 'pending' | 'active' }

interface EmailState {
  /** null: đang hỏi máy chủ. false: máy chủ chưa cấu hình Gmail hoặc không với tới, ẩn lựa chọn email */
  available: boolean | null
  sub: EmailSub | null
  busy: boolean
  error: string | null
  check: () => Promise<void>
  /** proof: bằng chứng email lúc đăng nhập Google. Có thì bật ngay, không thì máy chủ gửi thư xác nhận */
  enable: (email: string, proof?: string) => Promise<void>
  disable: () => Promise<void>
}

const DATE = /^\d{4}-\d{2}-\d{2}$/
function datesNow() {
  const dates = useMaintenance.getState().dates
  return Object.fromEntries((Object.keys(TASKS) as MaintenanceTask[]).filter(t => DATE.test(dates[t] ?? '')).map(t => [t, dates[t]]))
}
/** Chu kỳ lặp từng việc theo câu trả lời (vd bụi 3 tháng nếu có thú nuôi): máy chủ tự nhắc lần sau theo chu kỳ này */
function monthsNow() {
  const plan = maintenancePlan(useMaintenance.getState().answers ?? EMPTY_ANSWERS)
  return Object.fromEntries((Object.keys(TASKS) as MaintenanceTask[]).map(t => [t, plan[t].months]))
}
function save(sub: EmailSub | null) {
  try { if (sub) localStorage.setItem(KEY, JSON.stringify(sub)); else localStorage.removeItem(KEY) } catch { /* không lưu được thì chỉ giữ trong tab này */ }
}
async function call(path: string, method: string, body?: unknown) {
  const r = await fetch(API + path, { method, headers: body ? { 'Content-Type': 'application/json' } : undefined, body: body ? JSON.stringify(body) : undefined, signal: AbortSignal.timeout(10000) })
  if (!r.ok) {
    const detail = await r.json().catch(() => null) as { message?: string; detail?: string } | null
    throw Object.assign(new Error(detail?.detail ?? detail?.message ?? (r.status === 429 ? 'Too many requests today. Try again tomorrow.' : 'Could not reach the server. Try again later.')), { status: r.status })
  }
  return r.status === 204 ? null : r.json()
}

export const useEmailReminders = create<EmailState>((set, get) => ({
  available: null,
  sub: readStored<EmailSub | null>(KEY, null),
  busy: false,
  error: null,
  check: async () => {
    if (!EMAIL_ORIGINS.includes(location.origin)) { set({ available: false }); return }
    try {
      const r = await call('/available', 'GET') as { available: boolean }
      set({ available: r.available })
    } catch { set({ available: false }); return }
    const sub = get().sub
    if (!sub) return
    try {
      const s = await call(`/${sub.token}`, 'GET') as { status: EmailSub['status']; email: string }
      const next = { ...sub, status: s.status, email: s.email }
      save(next); set({ sub: next })
      void syncDates()
    } catch (e) {
      // Máy chủ không còn bản này (đã tắt từ link trong thư, hoặc quá hạn xác nhận): quên token
      if ((e as { status?: number }).status === 404) { save(null); set({ sub: null }) }
    }
  },
  enable: async (email, proof) => {
    set({ busy: true, error: null })
    try {
      const r = await call('', 'POST', { email, dates: datesNow(), months: monthsNow(), origin: location.origin, proof }) as { token: string; status: EmailSub['status'] }
      const sub = { token: r.token, email: email.trim().toLowerCase(), status: r.status }
      save(sub); set({ sub })
    } catch (e) { set({ error: (e as Error).message }) }
    finally { set({ busy: false }) }
  },
  disable: async () => {
    const sub = get().sub
    if (!sub) return
    set({ busy: true, error: null })
    try {
      await call(`/${sub.token}`, 'DELETE')
      save(null); set({ sub: null })
    } catch (e) {
      if ((e as { status?: number }).status === 404) { save(null); set({ sub: null }) }
      else set({ error: (e as Error).message })
    } finally { set({ busy: false }) }
  },
}))

/** Gửi ngày hẹn mới lên máy chủ. Lỗi mạng thì bỏ qua: lần mở app sau (check) gửi lại */
async function syncDates() {
  const sub = useEmailReminders.getState().sub
  const dates = datesNow()
  if (!sub || !Object.keys(dates).length) return
  try { await call(`/${sub.token}`, 'PUT', { dates, months: monthsNow() }) } catch { /* thử lại lần sau */ }
}

/** Bấm nút trong trang mở từ link của thư: bật nhắc (nhacemail) hoặc tắt nhắc (huyemail) */
export async function confirmFromEmail(token: string) {
  const r = await call('/confirm', 'POST', { token }) as { status: EmailSub['status']; email: string }
  const sub = useEmailReminders.getState().sub
  if (sub && sub.email === r.email) { const next = { ...sub, status: r.status }; save(next); useEmailReminders.setState({ sub: next }) }
  return r.email
}
export async function stopFromEmail(token: string) {
  await call('/stop', 'POST', { token })
  if (useEmailReminders.getState().sub?.token === token) { save(null); useEmailReminders.setState({ sub: null }) }
}

if (typeof window !== 'undefined') {
  // Lịch đổi (đặt lại, làm xong, hoãn 1 tuần) hoặc câu trả lời đổi chu kỳ: gửi cho máy chủ để thư nhắc đúng ngày
  useMaintenance.subscribe((s, prev) => { if (s.dates !== prev.dates || s.answers !== prev.answers) void syncDates() })
}
