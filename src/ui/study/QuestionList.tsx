import { visible, type AnswerMap } from './answers'
import { Choices, focus, Note, Question, Scale } from './StudyParts'
import { localize, type Lang, type StudyQuestion } from './questions'

/** Vẽ một danh sách câu hỏi từ questions.ts. compact = bản gọn dưới mỗi bước lắp. */
export function QuestionList({
  questions,
  answers,
  onChange,
  idPrefix = '',
  compact = false,
  lang = 'en',
}: {
  questions: StudyQuestion[]
  answers: AnswerMap
  onChange: (next: AnswerMap) => void
  idPrefix?: string
  compact?: boolean
  lang?: Lang
}) {
  const set = (k: string, v: string | string[] | number) => onChange({ ...answers, [k]: v })
  return (
    <>
      {questions
        .filter((q) => visible(q, answers))
        .map((q) => localize(q, lang))
        .map((q) => {
          const name = `${idPrefix}${q.key}`
          const value = answers[q.key]
          const input =
            q.type === 'scale' ? (
              <Scale name={name} value={typeof value === 'number' ? value : null} onChange={(v) => set(q.key, v)} low={q.low ?? ''} high={q.high ?? ''} />
            ) : q.type === 'number' ? (
              <input
                type="number"
                inputMode="numeric"
                min={1}
                max={120}
                step={1}
                aria-label={q.text}
                value={typeof value === 'number' ? value : ''}
                onChange={(e) => set(q.key, e.target.value === '' ? '' : Number(e.target.value))}
                placeholder={q.placeholder}
                className={`w-32 rounded-xl bg-surface px-3 py-2 text-[14px] text-ink ring-1 ring-line-strong placeholder:text-ink-3 ${focus}`}
              />
            ) : q.type === 'line' ? (
              <LineAnswer name={name} q={q} value={typeof value === 'string' ? value : ''} onChange={(v) => set(q.key, v)} lang={lang} />
            ) : q.type === 'text' ? (
              <Note value={typeof value === 'string' ? value : ''} onChange={(v) => set(q.key, v)} placeholder={q.placeholder ?? (lang === 'vi' ? 'Viết bằng lời của bạn' : "Use your own words")} />
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
              <div key={q.key} className="mt-5 first:mt-0">
                {q.section && <p className="mb-3 rounded-xl bg-fill px-3 py-2 text-[13.5px] leading-snug font-medium text-ink">{q.section}</p>}
                <Question label={q.text} hint={q.hint}>
                  {input}
                </Question>
              </div>
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

/** Một dòng chữ tự do, kèm các gợi ý dạng viên thuốc để chọn nhanh. Chọn gợi ý thì lưu value của gợi ý (tiếng Anh). */
function LineAnswer({ name, q, value, onChange, lang }: { name: string; q: StudyQuestion; value: string; onChange: (v: string) => void; lang: Lang }) {
  const options = q.options ?? []
  const picked = options.some((o) => o.value === value)
  return (
    <div className="space-y-2">
      {options.length > 0 && <Choices name={name} options={options} value={picked ? [value] : []} onChange={(v) => onChange(v[0] ?? '')} />}
      <input
        type="text"
        data-nofocus
        aria-label={q.text}
        maxLength={80}
        value={picked ? '' : value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={options.length ? (lang === 'vi' ? 'Hoặc tự gõ' : 'Or type your own') : q.placeholder}
        className={`w-full rounded-xl bg-surface px-3 py-2 text-[14px] text-ink ring-1 ring-line-strong placeholder:text-ink-3 ${focus}`}
      />
    </div>
  )
}
