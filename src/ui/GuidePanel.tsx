import { useEffect, useMemo, useRef, useState } from 'react'
import { getBuild } from '../data/builds'
import { FAMILIES } from '../data/families'
import { firstUndone, guideSteps, loadDone, saveDone, sceneFor, type GuideStep } from '../data/guide'
import type { PartId } from '../data/parts'
import { useViewer } from '../store'
import { FlowSteps } from './AdvicePage'

const press = 'transition-transform duration-100 ease-out active:scale-[0.97] motion-reduce:active:scale-100'
const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'

/**
 * Chế độ hướng dẫn lắp máy: thay chỗ bảng bên trái bằng danh sách các bước có ô đánh dấu.
 * Mở một bước thì hình 3D lắp đúng tới bước đó và tô sáng linh kiện đang lắp.
 */
export function GuidePanel() {
  const familyId = useViewer((s) => s.familyId)
  const buildId = useViewer((s) => s.buildId)
  const index = useViewer((s) => s.guideIndex)
  const setIndex = useViewer((s) => s.setGuideIndex)
  const setGuide = useViewer((s) => s.setGuide)
  const setStep = useViewer((s) => s.setStep)
  const select = useViewer((s) => s.select)

  const machine = familyId ?? buildId
  const machineLabel = familyId ? FAMILIES[familyId].label : getBuild(buildId).label
  const steps = useMemo(() => guideSteps(familyId, buildId), [familyId, buildId])
  const [done, setDone] = useState(() => loadDone(machine))
  const [replay, setReplay] = useState(0)
  const [confirmReset, setConfirmReset] = useState(false)
  const listRef = useRef<HTMLOListElement>(null)
  const current = steps[Math.min(index, steps.length - 1)]

  useEffect(() => saveDone(machine, done), [machine, done])

  // Hình 3D: đưa máy về trạng thái ngay trước bước này, rồi lắp linh kiện của bước vào
  useEffect(() => {
    const { before, after } = sceneFor(current)
    setStep(before)
    select(current.part as PartId | null)
    if (before === after) return
    const t = setTimeout(() => setStep(after), 450)
    return () => clearTimeout(t)
  }, [current, replay, setStep, select])

  // Đổi bước thì cuộn danh sách tới đầu bước đó. Chờ bước cũ thu lại và bước mới mở ra xong (.reveal 240 ms),
  // nếu cuộn ngay thì vị trí bị lệch vì chiều cao danh sách còn đang đổi
  useEffect(() => {
    const t = setTimeout(() => {
      const row = listRef.current?.querySelector<HTMLElement>(`[data-step="${current.id}"]`)
      const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
      // Bước 1 thì giữ phần đầu bảng (tiêu đề, thanh tiến độ) trong tầm nhìn
      if (current.id === steps[0].id) row?.closest('aside')?.scrollTo({ top: 0, behavior: reduce ? 'auto' : 'smooth' })
      else row?.scrollIntoView({ block: 'start', behavior: reduce ? 'auto' : 'smooth' })
    }, 260)
    return () => clearTimeout(t)
  }, [current.id, steps])

  const toggle = (id: string) =>
    setDone((d) => {
      const n = new Set(d)
      if (n.has(id)) n.delete(id)
      else n.add(id)
      return n
    })

  /** "Xong bước này": đánh dấu rồi chuyển tới bước kế tiếp chưa làm */
  const complete = () => {
    const n = new Set(done).add(current.id)
    setDone(n)
    const next = steps.findIndex((s, i) => i > index && !n.has(s.id))
    if (next >= 0) setIndex(next)
    else if (index < steps.length - 1) setIndex(index + 1)
  }

  const count = steps.filter((s) => done.has(s.id)).length
  const allDone = count === steps.length
  const pending = firstUndone(steps, done)

  return (
    <aside className="h-[46%] w-full shrink-0 overflow-y-auto px-5 pt-6 pb-8 md:h-full md:w-[400px] md:px-7 md:pt-10">
      <FlowSteps current={2} />
      <button
        onClick={() => setGuide(false)}
        className={`-ml-1 mt-5 flex items-center gap-1 rounded-full px-1 py-0.5 text-[15px] font-medium text-accent hover:underline ${focus}`}
      >
        <svg width="12" height="12" viewBox="0 0 12 12" aria-hidden>
          <path d="M7.5 2.5 4 6l3.5 3.5" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
        Xem mô hình và linh kiện
      </button>

      <p className="mt-4 text-[13px] font-semibold text-ink-2">{machineLabel}</p>
      <h1 className="display mt-1">Hướng dẫn lắp máy</h1>
      <p className="mt-3 text-[15px] leading-[1.47] text-ink-2">
        Làm lần lượt từng bước. Lắp xong bước nào thì đánh dấu bước đó, hình bên cạnh sẽ lắp theo.
      </p>

      <div className="mt-5">
        <div className="flex items-baseline justify-between text-[13px]">
          <span className="font-semibold text-ink">
            Đã xong {count}/{steps.length} bước
          </span>
          <span className="text-ink-3">{Math.round((count / steps.length) * 100)}%</span>
        </div>
        <div
          className="mt-1.5 h-2 overflow-hidden rounded-full bg-fill"
          role="progressbar"
          aria-valuemin={0}
          aria-valuemax={steps.length}
          aria-valuenow={count}
          aria-label="Tiến độ lắp máy"
        >
          <div className="h-full rounded-full bg-[#34c759] transition-[width] duration-300 ease-out" style={{ width: `${(count / steps.length) * 100}%` }} />
        </div>
      </div>

      {allDone && (
        <div className="swap mt-5 rounded-2xl bg-[#eaf7ee] p-4 text-[14px] leading-[1.45] text-[#1d5c30]">
          <p className="text-[15px] font-semibold">Bạn đã lắp xong cả máy!</p>
          <p className="mt-1">Nếu máy chưa lên hình, mở lại bước cuối để xem các lỗi hay gặp.</p>
        </div>
      )}

      <ol ref={listRef} className="mt-5 overflow-hidden rounded-2xl bg-white">
        {steps.map((s, i) => {
          const open = s === current
          const isDone = done.has(s.id)
          return (
            <li key={s.id} data-step={s.id} className={i < steps.length - 1 ? 'border-b border-black/[0.06]' : ''}>
              <div className="flex items-center gap-3 px-4 py-3">
                <CheckBox checked={isDone} onChange={() => toggle(s.id)} label={`Đánh dấu đã xong bước ${i + 1}: ${s.title}`} />
                <button
                  onClick={() => setIndex(i)}
                  aria-expanded={open}
                  className={`min-w-0 flex-1 rounded text-left ${focus}`}
                >
                  <span className={`block text-[12px] ${open ? 'text-accent' : 'text-ink-3'}`}>Bước {i + 1}</span>
                  <span className={`block text-[15px] leading-snug ${open ? 'font-semibold text-ink' : isDone ? 'text-ink-2' : 'text-ink'}`}>
                    {s.title}
                  </span>
                </button>
              </div>
              <div className="reveal" data-open={open}>
                <div>
                  <StepBody
                    step={s}
                    tabbable={open}
                    done={isDone}
                    last={i === steps.length - 1}
                    behind={!isDone && pending < i ? { n: pending + 1, title: steps[pending].title, go: () => setIndex(pending) } : null}
                    onComplete={complete}
                    onNext={() => setIndex(Math.min(i + 1, steps.length - 1))}
                    onReplay={s.part ? () => setReplay((r) => r + 1) : null}
                  />
                </div>
              </div>
            </li>
          )
        })}
      </ol>

      {count > 0 && (
        <button
          onClick={() => {
            if (!confirmReset) return setConfirmReset(true)
            setDone(new Set())
            setIndex(0)
            setConfirmReset(false)
          }}
          onBlur={() => setConfirmReset(false)}
          className={`mt-4 px-1 text-[13px] font-medium ${confirmReset ? 'text-[#b42318]' : 'text-ink-2 hover:text-ink'} ${focus}`}
        >
          {confirmReset ? 'Bấm lần nữa để xoá hết dấu tích' : 'Làm lại từ đầu'}
        </button>
      )}
    </aside>
  )
}

function StepBody({
  step,
  tabbable,
  done,
  last,
  behind,
  onComplete,
  onNext,
  onReplay,
}: {
  step: GuideStep
  tabbable: boolean
  done: boolean
  last: boolean
  /** Còn bước trước chưa xong: nhắc người dùng làm theo thứ tự */
  behind: { n: number; title: string; go: () => void } | null
  onComplete: () => void
  onNext: () => void
  onReplay: (() => void) | null
}) {
  const tab = tabbable ? 0 : -1
  return (
    <div className="pr-4 pb-4 pl-[50px] text-[14px] leading-[1.45]">
      <p className="text-ink-2">{step.why}</p>

      {behind && (
        <p className="mt-3 rounded-xl bg-[#eef5fe] px-3 py-2 text-[13px] text-[#0b4f9c]">
          Bạn chưa đánh dấu xong bước {behind.n} ({behind.title}). Nên lắp theo đúng thứ tự.{' '}
          <button onClick={behind.go} tabIndex={tab} className="font-semibold underline">
            Về bước {behind.n}
          </button>
        </p>
      )}

      {step.tools && (
        <div className="mt-3">
          <p className="text-[12px] font-semibold text-ink-2">Cần chuẩn bị</p>
          <ul className="mt-1.5 flex flex-wrap gap-1.5">
            {step.tools.map((t) => (
              <li key={t} className="rounded-full bg-fill px-2.5 py-1 text-[12.5px] text-ink">
                {t}
              </li>
            ))}
          </ul>
        </div>
      )}

      <p className="mt-3 text-[12px] font-semibold text-ink-2">Làm theo thứ tự</p>
      <ol className="mt-1.5 list-decimal space-y-1.5 pl-4 text-ink marker:text-ink-3">
        {step.actions.map((a) => (
          <li key={a} className="pl-1">
            {a}
          </li>
        ))}
      </ol>

      {step.warning && (
        <p className="mt-3 rounded-xl bg-[#fff4e5] px-3 py-2 text-[13px] text-[#7a4b00]">
          <span className="font-semibold">Cẩn thận: </span>
          {step.warning}
        </p>
      )}
      <p className="mt-2 rounded-xl bg-[#eaf7ee] px-3 py-2 text-[13px] text-[#1d5c30]">
        <span className="font-semibold">Lắp đúng khi: </span>
        {step.check}
      </p>

      <div className="mt-4 flex flex-wrap gap-2">
        {done ? (
          !last && (
            <button onClick={onNext} tabIndex={tab} className={`rounded-full bg-accent px-4 py-2 text-[14px] font-medium text-white hover:bg-accent-hover ${press} ${focus}`}>
              Sang bước tiếp
            </button>
          )
        ) : (
          <button
            onClick={onComplete}
            tabIndex={tab}
            className={`flex items-center gap-1.5 rounded-full bg-accent px-4 py-2 text-[14px] font-medium text-white hover:bg-accent-hover ${press} ${focus}`}
          >
            <svg width="13" height="13" viewBox="0 0 14 14" aria-hidden>
              <path d="M3 7.3 5.8 10 11 4" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            {last ? 'Xong, máy đã lên hình' : 'Xong bước này'}
          </button>
        )}
        {onReplay && (
          <button onClick={onReplay} tabIndex={tab} className={`rounded-full bg-fill px-4 py-2 text-[14px] font-medium text-ink hover:bg-[#dcdce1] ${press} ${focus}`}>
            Xem lại cách lắp
          </button>
        )}
      </div>
    </div>
  )
}

/** Ô đánh dấu tròn: dùng input checkbox thật để đọc được bằng trình đọc màn hình và bấm được bằng phím cách */
function CheckBox({ checked, onChange, label }: { checked: boolean; onChange: () => void; label: string }) {
  return (
    <label className="relative grid size-[22px] shrink-0 cursor-pointer place-items-center">
      <input type="checkbox" checked={checked} onChange={onChange} aria-label={label} className="peer absolute inset-0 cursor-pointer opacity-0" />
      <span
        aria-hidden
        className={`grid size-[22px] place-items-center rounded-full ring-[1.5px] transition-colors duration-150 peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-accent ${
          checked ? 'bg-[#34c759] text-white ring-[#34c759]' : 'bg-white text-transparent ring-ink-3/60 peer-hover:ring-ink-2'
        }`}
      >
        <svg width="12" height="12" viewBox="0 0 14 14">
          <path d="M3 7.3 5.8 10 11 4" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </span>
    </label>
  )
}

/** Dòng chữ trên hình 3D: đang ở bước nào */
export function GuideCaption() {
  const guide = useViewer((s) => s.guide)
  const familyId = useViewer((s) => s.familyId)
  const buildId = useViewer((s) => s.buildId)
  const index = useViewer((s) => s.guideIndex)
  const steps = useMemo(() => guideSteps(familyId, buildId), [familyId, buildId])
  if (!guide) return null
  const s = steps[Math.min(index, steps.length - 1)]
  const text = `Bước ${index + 1}/${steps.length}: ${s.title}`
  return (
    <div className="pointer-events-none absolute top-4 left-0 z-10 flex w-full justify-center px-4" aria-live="polite">
      <div key={text} className="swap glass rounded-full bg-white/80 px-4 py-2 text-center text-[14px] font-medium text-ink shadow-[0_6px_24px_rgba(0,0,0,0.10)] ring-1 ring-black/5 backdrop-blur-xl">
        {text}
      </div>
    </div>
  )
}
