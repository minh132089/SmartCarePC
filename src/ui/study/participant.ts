import { create } from 'zustand'

export type Participant = { name: string; age: number; occupation: string }
const KEY = 'pcsc-participant-v1'

export function validParticipant(value: unknown): value is Participant {
  if (!value || typeof value !== 'object') return false
  const p = value as Partial<Participant>
  return typeof p.name === 'string' && p.name.trim().length >= 2 && p.name.trim().length <= 80
    && Number.isInteger(p.age) && p.age! >= 1 && p.age! <= 120
    && typeof p.occupation === 'string' && p.occupation.trim().length >= 2 && p.occupation.trim().length <= 80
}

/** Chỉ bắt buộc với website Cloudflare Pages; localhost và demo qua LAN không bị chặn. */
export function needsParticipant(hostname: string) {
  return hostname.toLowerCase().endsWith('.pages.dev')
}

function read(): Participant | null {
  try {
    const p: unknown = JSON.parse(localStorage.getItem(KEY) ?? 'null')
    return validParticipant(p) ? p : null
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
    const clean = { name: profile.name.trim(), age: profile.age, occupation: profile.occupation.trim() }
    try { localStorage.setItem(KEY, JSON.stringify(clean)) } catch { /* Giữ trong phiên khi trình duyệt chặn lưu. */ }
    set({ profile: clean, confirmedThisVisit: true, editing: false })
  },
}))

export function participantAnswers() {
  const p = useParticipant.getState().profile
  return p ? { participant_name: p.name, participant_age: p.age, participant_occupation: p.occupation } : {}
}
