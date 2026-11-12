import { create } from 'zustand'
import { useSession } from '../Account'

/**
 * Tuổi và nghề của người tham gia, hỏi trong form đầu tiên (baseline). Lưu lại trên trình duyệt để các phản hồi sau
 * (form cuối, góp ý chung, chấm sao) cũng mang theo, giống form hồ sơ cũ.
 */
export type Participant = { age: number; occupation: string }
const KEY = 'pcsc-participant-v1'

export function validParticipant(value: unknown): value is Participant {
  if (!value || typeof value !== 'object') return false
  const p = value as Partial<Participant>
  return Number.isInteger(p.age) && p.age! >= 1 && p.age! <= 120
    && typeof p.occupation === 'string' && p.occupation.trim().length >= 2 && p.occupation.trim().length <= 80
}

function read(): Participant | null {
  try {
    const p: unknown = JSON.parse(localStorage.getItem(KEY) ?? 'null')
    return validParticipant(p) ? { age: p.age, occupation: p.occupation } : null
  } catch { return null }
}

export const useParticipant = create<{ profile: Participant | null; save: (profile: Participant) => void }>((set) => ({
  profile: read(),
  save: (profile) => {
    if (!validParticipant(profile)) return
    const clean = { age: profile.age, occupation: profile.occupation.trim() }
    try { localStorage.setItem(KEY, JSON.stringify(clean)) } catch { /* Giữ trong phiên khi trình duyệt chặn lưu. */ }
    set({ profile: clean })
  },
}))

/**
 * Thông tin gửi kèm mỗi phản hồi: tuổi, nghề (nếu đã trả lời) và tên, email của tài khoản đang đăng nhập.
 * Tên và email chỉ nhóm nghiên cứu xem được (trang /feedback cần khoá), không công khai.
 */
export function participantAnswers() {
  const p = useParticipant.getState().profile
  const s = useSession.getState().session
  return {
    ...(p ? { participant_age: p.age, participant_occupation: p.occupation } : {}),
    ...(s ? { participant_name: s.name, participant_email: s.email } : {}),
  }
}
