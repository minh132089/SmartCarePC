import { visible, type AnswerMap } from './answers'
import { Choices, Note, Question, Scale } from './StudyParts'
import type { StudyQuestion } from './questions'

/** Vẽ một danh sách câu hỏi từ questions.ts. compact = bản gọn dưới mỗi bước lắp. */
export function QuestionList({
  questions,
  answers,
  onChange,
  idPrefix = '',
  compact = false,
}: {
  questions: StudyQuestion[]
  answers: AnswerMap
  onChange: (next: AnswerMap) => void
  idPrefix?: string
  compact?: boolean
}) {
  const set = (k: string, v: string | string[] | number) => onChange({ ...answers, [k]: v })
  return (
    <>
      {questions
        .filter((q) => visible(q, answers))
        .map((q) => {
          const name = `${idPrefix}${q.key}`
          const value = answers[q.key]
          const input =
            q.type === 'scale' ? (
              <Scale name={name} value={typeof value === 'number' ? value : null} onChange={(v) => set(q.key, v)} low={q.low ?? ''} high={q.high ?? ''} />
            ) : q.type === 'text' ? (
              <Note value={typeof value === 'string' ? value : ''} onChange={(v) => set(q.key, v)} placeholder={q.placeholder ?? 'Viết bằng lời của bạn'} />
            ) : (
              <Choices
                name={name}
                multi={q.type === 'multi'}
                options={q.options ?? []}
                value={Array.isArray(value) ? value : typeof value === 'string' ? [value] : []}
                onChange={(v) => set(q.key, q.type === 'multi' ? v : v[0])}
              />
            )
          if (!compact) {
            return (
              <Question key={q.key} label={q.text} hint={q.hint}>
                {input}
              </Question>
            )
          }
          return (
            <fieldset key={q.key} className="mt-3 first:mt-0">
              <legend className="text-[13.5px] leading-snug font-semibold text-ink">{q.text}</legend>
              {q.hint && <p className="text-[12px] text-ink-3">{q.hint}</p>}
              <div className="mt-1.5">{input}</div>
            </fieldset>
          )
        })}
    </>
  )
}
