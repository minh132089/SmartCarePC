import { useEffect, useRef, useState } from 'react'
import {
  isComplete,
  MACHINE_INFO,
  machineImage,
  machineLabel,
  machineTagline,
  QUESTIONS,
  rank,
  type Answers,
  type MachineId,
} from '../data/advice'
import { firstUndone, guideSteps, loadDone } from '../data/guide'
import { useViewer } from '../store'

const press = 'transition-transform duration-100 ease-out active:scale-[0.97] motion-reduce:active:scale-100'
const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'
const STORE_KEY = 'pcsc-advice'

function loadAnswers(): Answers {
  try {
    return JSON.parse(localStorage.getItem(STORE_KEY) ?? '{}') as Answers
  } catch {
    return {}
  }
}

/**
 * Trang 1: tư vấn chọn máy. Người dùng bấm chọn 4 câu, app gợi ý một kiểu máy kèm lý do,
 * rồi "Xem và lắp máy này" mở trang 2 (mô hình 3D) với hướng dẫn lắp từng bước.
 * Trang này không vẽ cảnh 3D nên không làm nóng máy người dùng.
 */
export function AdvicePage() {
  const [answers, setAnswers] = useState<Answers>(loadAnswers)
  const resultRef = useRef<HTMLDivElement>(null)
  const done = isComplete(answers)
  const ranked = rank(answers)
  const count = QUESTIONS.filter((q) => answers[q.id]).length

  useEffect(() => {
    try {
      localStorage.setItem(STORE_KEY, JSON.stringify(answers))
    } catch {
      // không lưu được thì thôi, lần sau trả lời lại
    }
  }, [answers])

  const choose = (q: string, a: string) => {
    const next = { ...answers, [q]: a }
    setAnswers(next)
    // Vừa trả lời câu cuối: trên điện thoại khung gợi ý nằm dưới, cuộn xuống cho người dùng thấy
    if (isComplete(next) && !isComplete(answers) && window.matchMedia('(max-width: 767px)').matches) {
      const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
      setTimeout(() => resultRef.current?.scrollIntoView({ behavior: reduce ? 'auto' : 'smooth', block: 'start' }), 50)
    }
  }

  return (
    <div className="h-full overflow-y-auto bg-canvas">
      <div className="mx-auto max-w-[1080px] px-5 pt-8 pb-12 md:px-10 md:pt-12">
        <FlowSteps current={1} />
        <p className="mt-6 text-[13px] font-semibold text-ink-2">PC SmartCare</p>
        <h1 className="display mt-1 text-balance">Chọn chiếc máy hợp với bạn.</h1>
        <p className="mt-3 max-w-[52ch] text-[15px] leading-[1.47] text-ink-2">
          Trả lời 4 câu ngắn. Mình gợi ý kiểu máy phù hợp, rồi hướng dẫn bạn tự lắp từng bước trên mô hình 3D.
        </p>

        <div className="mt-8 grid gap-8 md:grid-cols-[minmax(0,1fr)_380px] md:items-start">
          <div className="space-y-7">
            {QUESTIONS.map((q, qi) => (
              <section key={q.id}>
                <h2 className="text-[17px] font-semibold tracking-[-0.01em] text-ink">
                  <span className="text-ink-3">{qi + 1}. </span>
                  {q.title}
                </h2>
                <div role="radiogroup" aria-label={q.title} className="mt-3 grid gap-2 sm:grid-cols-3">
                  {q.options.map((o) => {
                    const on = answers[q.id] === o.id
                    return (
                      <button
                        key={o.id}
                        role="radio"
                        aria-checked={on}
                        onClick={() => choose(q.id, o.id)}
                        className={`rounded-2xl bg-white px-4 py-3 text-left ring-1 transition-[box-shadow,color] duration-150 ${press} ${focus} ${
                          on ? 'ring-2 ring-accent' : 'ring-black/[0.06] hover:ring-black/15'
                        }`}
                      >
                        <span className={`block text-[15px] ${on ? 'font-semibold text-accent' : 'font-medium text-ink'}`}>{o.label}</span>
                        <span className="mt-0.5 block text-[13px] leading-snug text-ink-2">{o.hint}</span>
                      </button>
                    )
                  })}
                </div>
              </section>
            ))}
          </div>

          <div ref={resultRef} className="scroll-mt-4 md:sticky md:top-6">
            {done ? (
              <Result key={ranked[0]} best={ranked[0]} others={ranked.slice(1, 3)} answers={answers} />
            ) : (
              <div className="rounded-[24px] bg-white p-6">
                <p className="text-[15px] font-semibold text-ink">Gợi ý cho bạn</p>
                <p className="mt-1 text-[14px] leading-[1.45] text-ink-2">Trả lời đủ 4 câu bên cạnh, gợi ý sẽ hiện ở đây.</p>
                <div className="mt-4 flex gap-1.5" aria-label={`Đã trả lời ${count}/4 câu`}>
                  {QUESTIONS.map((q) => (
                    <span key={q.id} className={`h-1.5 flex-1 rounded-full ${answers[q.id] ? 'bg-accent' : 'bg-fill'}`} />
                  ))}
                </div>
                <p className="mt-1.5 text-[12px] text-ink-3">Đã trả lời {count}/4 câu</p>
              </div>
            )}
          </div>
        </div>

        <SkipLink />
      </div>
    </div>
  )
}

/** Mở trang hướng dẫn với máy đã chọn và bật luôn hướng dẫn lắp, tới bước còn dở */
function useOpenMachine() {
  const s = useViewer()
  return (id: MachineId) => {
    if (id === 'gaming') s.setBuild('gaming')
    else s.setFamily(id)
    const steps = guideSteps(id === 'gaming' ? null : id, 'gaming')
    s.setGuideIndex(firstUndone(steps, loadDone(id)))
    s.setGuide(true)
    s.setPage('build')
  }
}

function Result({ best, others, answers }: { best: MachineId; others: MachineId[]; answers: Answers }) {
  const open = useOpenMachine()
  const info = MACHINE_INFO[best]
  const img = machineImage(best)
  const picked = QUESTIONS.map((q) => q.options.find((o) => o.id === answers[q.id])?.label).filter(Boolean)
  return (
    <div className="swap overflow-hidden rounded-[24px] bg-white">
      <div className="bg-white px-4 pt-5">
        {img ? (
          <img src={img} alt={machineLabel(best)} className="mx-auto aspect-[4/3] w-full max-w-[300px] object-contain" />
        ) : (
          <div className="mx-auto grid aspect-[4/3] w-full max-w-[300px] place-items-center rounded-xl bg-[#16181d] text-[15px] font-semibold text-[#22d3ee]">
            GAMING
          </div>
        )}
      </div>
      <div className="p-5">
        <p className="text-[13px] font-semibold text-accent">Gợi ý cho bạn</p>
        <h2 className="mt-0.5 text-[24px] leading-tight font-semibold tracking-[-0.02em] text-ink">{machineLabel(best)}</h2>
        <p className="mt-1.5 text-[14px] leading-[1.45] text-ink-2">{machineTagline(best)}</p>
        <p className="mt-3 text-[12px] leading-snug text-ink-3">Vì bạn chọn: {picked.join(' · ')}</p>

        <ul className="mt-4 space-y-2 text-[14px] leading-[1.45] text-ink">
          {info.why.map((w) => (
            <li key={w} className="flex gap-2">
              <svg width="16" height="16" viewBox="0 0 14 14" aria-hidden className="mt-[3px] shrink-0 text-[#34c759]">
                <path d="M3 7.3 5.8 10 11 4" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              {w}
            </li>
          ))}
        </ul>
        <p className="mt-3 rounded-xl bg-[#fff4e5] px-3 py-2 text-[13px] leading-snug text-[#7a4b00]">
          <span className="font-semibold">Cần biết: </span>
          {info.note}
        </p>
        <p className="mt-3 text-[13px] text-ink-2">
          Độ khó khi tự lắp: <span className="font-semibold text-ink">{info.level}</span>
        </p>

        <button
          onClick={() => open(best)}
          className={`mt-5 w-full rounded-full bg-accent px-5 py-3 text-[15px] font-medium text-white hover:bg-accent-hover ${press} ${focus}`}
        >
          Xem và lắp máy này
        </button>

        <p className="mt-5 text-[12px] font-semibold text-ink-2">Lựa chọn khác</p>
        <ul className="mt-1.5 divide-y divide-black/[0.06]">
          {others.map((m) => (
            <li key={m}>
              <button onClick={() => open(m)} className={`flex w-full items-center justify-between gap-3 py-2.5 text-left ${focus}`}>
                <span>
                  <span className="block text-[14px] font-medium text-ink">{machineLabel(m)}</span>
                  <span className="block text-[12px] leading-snug text-ink-2">{MACHINE_INFO[m].note}</span>
                </span>
                <svg width="12" height="12" viewBox="0 0 12 12" aria-hidden className="shrink-0 text-ink-3">
                  <path d="M4.5 2.5 8 6l-3.5 3.5" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}

function SkipLink() {
  const setPage = useViewer((s) => s.setPage)
  return (
    <button onClick={() => setPage('build')} className={`mt-10 text-[14px] font-medium text-accent hover:underline ${focus}`}>
      Bỏ qua, xem mô hình 3D luôn ›
    </button>
  )
}

/** Thanh hai bước ở đầu trang: 1 Tư vấn, 2 Lắp máy. Dùng chung cho cả hai trang. */
export function FlowSteps({ current }: { current: 1 | 2 }) {
  const setPage = useViewer((s) => s.setPage)
  const items = [
    { n: 1, label: 'Tư vấn chọn máy', short: 'Tư vấn', page: 'advice' as const },
    { n: 2, label: 'Hướng dẫn lắp máy', short: 'Lắp máy', page: 'build' as const },
  ]
  return (
    <nav aria-label="Các bước" className="flex items-center gap-2 text-[13px]">
      {items.map((it, i) => {
        const on = it.n === current
        return (
          <div key={it.n} className="flex items-center gap-2">
            {i > 0 && <span className="h-px w-5 bg-black/15" aria-hidden />}
            <button
              onClick={() => setPage(it.page)}
              aria-current={on ? 'step' : undefined}
              className={`flex items-center gap-1.5 rounded-full py-1 pr-3 pl-1 ${focus} ${on ? 'bg-white font-semibold text-ink' : 'text-ink-2 hover:text-ink'}`}
            >
              <span className={`grid size-5 place-items-center rounded-full text-[11px] font-semibold ${on ? 'bg-accent text-white' : 'bg-fill text-ink-2'}`}>
                {it.n}
              </span>
              <span className="whitespace-nowrap sm:hidden">{it.short}</span>
              <span className="hidden whitespace-nowrap sm:inline">{it.label}</span>
            </button>
          </div>
        )
      })}
    </nav>
  )
}
