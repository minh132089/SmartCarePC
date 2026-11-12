import { create } from 'zustand'

/** Hồ sơ tự nguyện: chỉ tuổi và nghề. Không thu tên thật, vì khảo sát đã hứa ẩn danh; phản hồi gắn với mã người tham gia. */
export type Participant = { age: number; occupation: string }
const KEY = 'pcsc-participant-v1'

export function validParticipant(value: unknown): value is Participant {
  if (!value || typeof value !== 'object') return false
  const p = value as Partial<Participant>
  return Number.isInteger(p.age) && p.age! >= 1 && p.age! <= 120
    && typeof p.occupation === 'string' && p.occupation.trim().length >= 2 && p.occupation.trim().length <= 80
}

/** Chỉ bắt buộc với website Cloudflare Pages; localhost và demo qua LAN không bị chặn. */
export function needsParticipant(hostname: string) {
  return hostname.toLowerCase().endsWith('.pages.dev')
}

function read(): Participant | null {
  try {
    const p: unknown = JSON.parse(localStorage.getItem(KEY) ?? 'null')
    // Bản cũ có lưu tên: bỏ tên, chỉ giữ tuổi và nghề
    return validParticipant(p) ? { age: p.age, occupation: p.occupation } : null
  } catch { return null }
}

export const useParticipant = create<{
  profile: Participant | null
  /** Chỉ giữ trong trang hiện tại; tải lại hoặc mở tab mới phải xác nhận lại. */
  confirmedThisVisit: boolean
  editing: boolean
  edit: () => void
  save: (profile: Participant) => void
}>((set) => ({
  profile: read(),
  confirmedThisVisit: false,
  editing: false,
  edit: () => set({ editing: true }),
  save: (profile) => {
    if (!validParticipant(profile)) return
    const clean = { age: profile.age, occupation: profile.occupation.trim() }
    try { localStorage.setItem(KEY, JSON.stringify(clean)) } catch { /* Giữ trong phiên khi trình duyệt chặn lưu. */ }
    set({ profile: clean, confirmedThisVisit: true, editing: false })
  },
}))

export function participantAnswers() {
  const p = useParticipant.getState().profile
  return p ? { participant_age: p.age, participant_occupation: p.occupation } : {}
}
