import type { StudyQuestion } from './questions'

export type AnswerMap = Record<string, string | string[] | number>

/** Câu có showIf chỉ hiện khi ít nhất một điều kiện "key=value" khớp với câu trả lời hiện tại */
export function visible(q: StudyQuestion, a: AnswerMap) {
  if (!q.showIf) return true
  return q.showIf.split('|').some((cond) => {
    const [k, v] = cond.split('=')
    const got = a[k]
    return Array.isArray(got) ? got.includes(v) : String(got ?? '') === v
  })
}

/** Câu trả lời để gửi: bỏ câu đang ẩn và câu tự luận để trống */
export function collect(questions: StudyQuestion[], a: AnswerMap) {
  const answers: Record<string, unknown> = {}
  for (const q of questions) {
    const v = a[q.key]
    if (!visible(q, a) || v == null) continue
    if (typeof v === 'string' && !v.trim()) continue
    answers[q.key] = typeof v === 'string' ? v.trim() : v
  }
  return { answers, empty: Object.keys(answers).length === 0 }
}
