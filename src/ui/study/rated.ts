import type { RatingKind } from './questions'

const RATED = 'pcsc-rated'

/**
 * Hộp chấm sao đã tự bật lên cho việc này chưa (mỗi máy lắp, mỗi lần bảo dưỡng, mỗi kế hoạch nâng cấp một lần). Lưu trên
 * trình duyệt; bị chặn lưu thì coi như chưa
 */
export function ratedBefore(key: string) {
  try {
    return (JSON.parse(localStorage.getItem(RATED) ?? '[]') as string[]).includes(key)
  } catch {
    return false
  }
}

export function markRated(key: string) {
  try {
    const list = JSON.parse(localStorage.getItem(RATED) ?? '[]') as string[]
    localStorage.setItem(RATED, JSON.stringify([...list.filter((m) => m !== key), key].slice(-50)))
  } catch {
    /* bỏ qua */
  }
}

/** Mã "đã trả lời" của người tham gia cho từng hộp chấm sao (lắp máy giữ mã cũ "feedback") */
export function ratingAnswered(kind: RatingKind) {
  return kind === 'build' ? 'feedback' : `feedback_${kind}`
}
