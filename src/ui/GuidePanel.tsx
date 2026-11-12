import { useEffect, useMemo, useRef, useState } from 'react'
import { CaretLeft, Check, Cube, ListNumbers } from '@phosphor-icons/react'
import { getBuild } from '../data/builds'
import { FAMILIES } from '../data/families'
import { firstUndone, guideSteps, loadDone, saveDone, sceneFor, type GuideStep } from '../data/guide'
import type { PartId } from '../data/parts'
import { useViewer } from '../store'
import { FlowSteps } from './AdvicePage'
import { useStudy } from './study/study'
import { StudyBar } from './study/StudyBar'
import { playTick } from './sound/sfx'
import { maintenanceSteps, TASKS } from '../data/maintenance'
import { maintenanceKey, recordStep, recordUsage, useMaintenance } from './maintenance/maintenanceState'
import { MaintenanceEntry, Symptoms } from './maintenance/MaintenanceHub'
import { Choices, Question } from './study/StudyParts'
import SpringCheck from '../components/SpringCheck'

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
  const setGuidePart = useViewer((s) => s.setGuidePart)
  const full = useViewer((s) => s.guideFull)
  const advised = useViewer((s) => s.advisedBuild)
  const hasGpu = advised == null || advised.items.some((i) => i.category === 'gpu')

  const kind = useViewer(s => s.guideKind)
  const task = useViewer(s => s.maintenanceTask)
  const cooler = useViewer(s => s.maintenanceCooler)
  const maintenanceGpu = useViewer(s => s.maintenanceHasGpu)
  const isMaintenance = kind === 'maintenance'
  const run = useMaintenance(s => s.run)
  const finishMaintenance = useMaintenance(s => s.finish)
  const queueProblem = useMaintenance(s => s.queueProblem)
  const storageWarning = useMaintenance(s => s.storageWarning)
  const [who, setWho] = useState('')
  const [symptoms, setSymptoms] = useState<string[]>([])
  const [stillHot, setStillHot] = useState(false)
  const machine = isMaintenance ? maintenanceKey({ machine: familyId ?? 'gaming', cooler, hasGpu: maintenanceGpu }, task) : familyId ?? buildId
  const machineLabel = familyId ? FAMILIES[familyId].label : getBuild(buildId).label
  const steps = useMemo(() => isMaintenance ? maintenanceSteps(task, familyId, buildId, maintenanceGpu, cooler) : guideSteps(familyId, buildId, hasGpu), [isMaintenance, task, familyId, buildId, hasGpu, maintenanceGpu, cooler])
  const [done, setDone] = useState(() => new Set([...loadDone(machine), ...(isMaintenance && run?.machine === machine ? Object.keys(run.alone) : [])]))
  const [replay, setReplay] = useState(0)
  const [confirmReset, setConfirmReset] = useState(false)
  const listRef = useRef<HTMLOListElement>(null)
  const completionRef = useRef<HTMLDivElement>(null)
  const current = steps[Math.min(index, steps.length - 1)]
  // Lắp xong cả máy: mời chấm sao + phản hồi (mọi người dùng). Người tham gia thử nghiệm đã gửi phản hồi thì thôi mời
  const askFeedback = useStudy((s) => !s.answered.includes('feedback'))
  const openStudy = useStudy((s) => s.open)

  useEffect(() => saveDone(machine, done), [machine, done])

  // Hình 3D: đưa máy về trạng thái ngay trước bước này (linh kiện của các bước sau chưa hiện),
  // rồi lắp linh kiện của bước vào. "Xem toàn cảnh" thì hiện cả máy đã lắp đủ.
  useEffect(() => {
    if (isMaintenance) {
      setStep(0)
      setGuidePart(full ? null : current.part)
      select(full ? null : current.part as PartId | null)
      useViewer.setState({ maintenanceLift: !full && !!current.coolerOut, sideOpen: !full && !!current.panelOpen, powered: false })
      return
    }
    if (full) {
      setStep(0)
      select(null)
      return
    }
    const { before, after } = sceneFor(current)
    setGuidePart(current.part)
    setStep(before)
    select(current.part as PartId | null)
    if (before === after) return
    const t = setTimeout(() => setStep(after), 450)
    return () => clearTimeout(t)
  }, [current, replay, full, isMaintenance, setStep, select, setGuidePart])

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

  const toggle = (id: string) => {
    if (!isMaintenance && !done.has(id)) recordUsage('build_step_done', { machine, stepId: id, via: 'check' })
    playTick(!done.has(id))
    setDone((d) => {
      const n = new Set(d)
      if (n.has(id)) n.delete(id)
      else n.add(id)
      return n
    })
  }

  /** "Xong bước này": đánh dấu rồi chuyển tới bước kế tiếp chưa làm */
  const complete = (alone = true) => {
    if (isMaintenance) recordStep(current.id, alone)
    else recordUsage('build_step_done', { machine, stepId: current.id, alone })
    if (!done.has(current.id)) playTick()
    const n = new Set(done).add(current.id)
    setDone(n)
    const next = steps.findIndex((s, i) => i > index && !n.has(s.id))
    if (next >= 0) setIndex(next)
    else if (index < steps.length - 1) setIndex(index + 1)
  }

  const count = steps.filter((s) => done.has(s.id)).length
  const allDone = count === steps.length
  const pending = firstUndone(steps, done)

  useEffect(() => {
    if (allDone && isMaintenance && !run?.completed) completionRef.current?.scrollIntoView({ block: 'center', behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' })
  }, [allDone, isMaintenance, run?.completed])

  // Vừa tick xong bước cuối (chưa xong -> xong hết): tự hiện hộp chấm sao, mỗi máy một lần. Mở lại trang khi đã xong
  // từ trước thì không bật lên nữa (nút trong ô chúc mừng vẫn mở được)
  const wasDone = useRef(allDone)
  useEffect(() => {
    if (!isMaintenance && allDone && !wasDone.current) recordUsage('build_guide_completed', { machine, steps: steps.length })
    if (!isMaintenance && allDone && !wasDone.current && !ratedBefore(machine)) {
      markRated(machine)
      openStudy('rating')
    }
    wasDone.current = allDone
  }, [allDone, machine, openStudy, isMaintenance, steps.length])

  // Mở hướng dẫn lắp (mỗi máy một lần mỗi lần mở)
  useEffect(() => {
    if (!isMaintenance) recordUsage('build_guide_opened', { machine })
  }, [isMaintenance, machine])

  return (
    <aside className="h-[46%] w-full shrink-0 overflow-y-auto px-5 pt-6 pb-8 md:h-full md:w-[400px] md:px-7 md:pt-10">
      <FlowSteps current={2} maintenance={isMaintenance} />
      <StudyBar />
      <button
        onClick={() => setGuide(false)}
        className={`-ml-1 mt-5 flex items-center gap-1 rounded-full px-1 py-0.5 text-[15px] font-medium text-link hover:underline ${focus}`}
      >
        <CaretLeft size={14} aria-hidden />
        Back to the model and components
      </button>

      <p className="mt-4 text-[13px] font-semibold text-ink-2">{machineLabel}</p>
      <h1 className="display mt-1">{isMaintenance ? TASKS[task].title : 'Assembly guide'}</h1>
      <p className="mt-3 text-[15px] leading-[1.47] text-ink-2">
        {isMaintenance ? 'One task at a time. Follow the safety checks and tell us whether you needed help.' : 'Follow the steps in order. Mark each one complete as you go; the 3D view follows your progress.'}
      </p>
      {isMaintenance && <><p className="mt-3 text-[12px] text-ink-3">Reference illustration: {machineLabel}. {cooler === 'unknown' ? 'Cooler not identified: compare it with the pictures before loosening anything.' : ''} Written instructions follow your chosen cooler. Use your component manual for exact catches and mounting hardware.</p><MaintenanceEntry className="mt-3" /></>}
      {isMaintenance && storageWarning && <p className="mt-3 text-[13px] text-info">Browser storage is unavailable. Progress and reminder dates last for this tab only; use a calendar export to keep reminders.</p>}
      {queueProblem && <p role="alert" className="mt-3 text-[13px] text-info">This browser cannot save study events. Keep this tab open and connect to the server before leaving.</p>}
      {!isMaintenance && !hasGpu && <p className="mt-3 text-[13px] text-info">Your selected build uses integrated graphics. The model and guide omit the dedicated GPU.</p>}
      {advised?.modelNote && <p className="mt-3 text-[12px] leading-relaxed text-ink-3">{advised.modelNote}</p>}

      <div className="mt-5">
        <div className="flex items-baseline justify-between text-[13px]">
          <span className="font-semibold text-ink">
            Completed {count}/{steps.length} steps
          </span>
          <span className="text-ink-3">{Math.round((count / steps.length) * 100)}%</span>
        </div>
        <div
          className="mt-1.5 h-2 overflow-hidden rounded-full bg-fill"
          role="progressbar"
          aria-valuemin={0}
          aria-valuemax={steps.length}
          aria-valuenow={count}
          aria-label={isMaintenance ? "Maintenance progress" : "Assembly progress"}
        >
          <div className="h-full rounded-full bg-ok-strong transition-[width] duration-300 ease-out" style={{ width: `${(count / steps.length) * 100}%` }} />
        </div>
      </div>

      {allDone && !isMaintenance && (
        <div className="swap mt-5 rounded-2xl bg-ok-bg p-4 text-[14px] leading-[1.45] text-ok">
          <p className="text-[15px] font-semibold">Your PC is assembled!</p>
          <p className="mt-1">If there is no display, revisit the final step for common checks.</p>
          {askFeedback && (
            <button
              onClick={() => openStudy('rating')}
              className={`mt-3 rounded-full bg-accent px-4 py-2 text-[14px] font-medium text-white hover:bg-accent-hover ${press} ${focus}`}
            >
              Rate your experience
            </button>
          )}
        </div>
      )}

      {allDone && isMaintenance && <div ref={completionRef} className="mt-5 rounded-2xl bg-surface p-4 ring-1 ring-line">
        {run?.completed ? <><p className="font-semibold text-ok">Maintenance recorded</p><p className="mt-1 text-[13px] text-ink-2">Your next reminder date has been updated in this browser. Update any calendar events separately.</p><MaintenanceEntry className="mt-3" /></> : <>
          <Question label="Who did the work?"><Choices name="maintenance-who" value={[who]} options={[{ value: 'me-alone', label: 'Me alone' }, { value: 'with-help', label: 'Me with someone’s help' }, { value: 'someone-else', label: 'Someone else did it' }]} onChange={v => setWho(v[0])} /></Question>
          {task === 'check' && <Question label="What did you notice?"><Symptoms value={symptoms} onChange={setSymptoms} /></Question>}
          {task === 'paste' && <Question label="After 10 minutes of heavy use, what do you notice?" hint="New paste usually lowers temperatures within minutes.">
            <Symptoms value={symptoms} onChange={setSymptoms} />
            {symptoms.some(x => x === 'heat' || x === 'shutdown') && <p className="mt-2 text-[13px] text-warn">If it still overheats with fresh paste, a fan or pump may be failing. Ask a technician to check the cooler.</p>}
          </Question>}
          {task === 'dust' && <label className="mt-3 flex gap-2 text-[13px]"><input type="checkbox" checked={stillHot} onChange={e => setStillHot(e.target.checked)} /> Heat or shutdowns still happen after dust cleaning</label>}
          <button disabled={!who} onClick={() => finishMaintenance(task, who, stillHot, task === 'check' || task === 'paste' ? symptoms : undefined)} className={`mt-4 rounded-full bg-accent px-4 py-2 text-[14px] text-white disabled:opacity-40 ${focus}`}>Finish maintenance</button>
        </>}
      </div>}

      <ol ref={listRef} className="mt-5 overflow-hidden rounded-2xl bg-surface">
        {steps.map((s, i) => {
          const open = s === current
          const isDone = done.has(s.id)
          return (
            <li key={s.id} data-step={s.id} className={i < steps.length - 1 ? 'border-b border-line' : ''}>
              <div className="flex items-center gap-3 px-4 py-3">
                {isMaintenance ? <span className={`grid size-6 shrink-0 place-items-center rounded-lg bg-fill ${isDone ? 'text-ok' : 'text-ink-3'}`} aria-label={isDone ? 'Completed' : 'Not completed'}>{isDone ? <Check size={16} /> : i + 1}</span> : <CheckBox checked={isDone} onChange={() => toggle(s.id)} label={`Mark step complete: ${i + 1}: ${s.title}`} />}
                <button
                  onClick={() => setIndex(i)}
                  aria-expanded={open}
                  className={`min-w-0 flex-1 rounded text-left ${focus}`}
                >
                  <span className={`block text-[12px] ${open ? 'text-link' : 'text-ink-3'}`}>Step {i + 1}</span>
                  <span className={`guide-step-title block text-[15px] leading-snug ${open ? 'font-semibold text-ink' : isDone ? 'text-ink-2' : 'text-ink'}`} data-done={isDone}>
                    {s.title}
                  </span>
                </button>
              </div>
              <div className="reveal" data-open={open}>
                <div inert={!open}>
                  <StepBody
                    step={s}
                    tabbable={open}
                    done={isDone}
                    last={i === steps.length - 1}
                    behind={!isDone && pending < i ? { n: pending + 1, title: steps[pending].title, go: () => setIndex(pending) } : null}
                    maintenance={isMaintenance}
                    answer={run?.alone[s.id]}
                    onHelp={() => complete(false)}
                    onComplete={() => complete(true)}
                    onNext={() => setIndex(Math.min(i + 1, steps.length - 1))}
                    onReplay={!isMaintenance && s.part ? () => setReplay((r) => r + 1) : null}
                  />
                </div>
              </div>
            </li>
          )
        })}
      </ol>

      {count > 0 && !isMaintenance && (
        <button
          onClick={() => {
            if (!confirmReset) return setConfirmReset(true)
            setDone(new Set())
            setIndex(0)
            setConfirmReset(false)
          }}
          onBlur={() => setConfirmReset(false)}
          className={`mt-4 px-1 text-[13px] font-medium ${confirmReset ? 'text-bad' : 'text-ink-2 hover:text-ink'} ${focus}`}
        >
          {confirmReset ? "Select again to clear all checkmarks" : "Start over"}
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
  maintenance,
  answer,
  onHelp,
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
  maintenance?: boolean
  answer?: boolean
  onHelp?: () => void
  onNext: () => void
  onReplay: (() => void) | null
}) {
  const tab = tabbable ? 0 : -1
  return (
    <div className="pr-4 pb-4 pl-[50px] text-[14px] leading-[1.45]">
      <p className="text-ink-2">{step.why}</p>

      {behind && (
        <p className="mt-3 rounded-xl bg-info-bg px-3 py-2 text-[13px] text-info">
          You have not completed step {behind.n} ({behind.title}). Follow the steps in order.{' '}
          <button onClick={behind.go} tabIndex={tab} className="font-semibold underline">
            Go to step {behind.n}
          </button>
        </p>
      )}

      {step.tools && (
        <div className="mt-3">
          <p className="text-[12px] font-semibold text-ink-2">What you need</p>
          <ul className="mt-1.5 flex flex-wrap gap-1.5">
            {step.tools.map((t) => (
              <li key={t} className="rounded-full bg-fill px-2.5 py-1 text-[12.5px] text-ink">
                {t}
              </li>
            ))}
          </ul>
        </div>
      )}

      <p className="mt-3 text-[12px] font-semibold text-ink-2">Follow these instructions</p>
      <ol className="mt-1.5 list-decimal space-y-1.5 pl-4 text-ink marker:text-ink-3">
        {step.actions.map((a) => (
          <li key={a} className="pl-1">
            {a}
          </li>
        ))}
      </ol>

      {step.warning && (
        <p className="mt-3 rounded-xl bg-warn-bg px-3 py-2 text-[13px] text-warn">
          <span className="font-semibold">Caution: </span>
          {step.warning}
        </p>
      )}
      <p className="mt-2 rounded-xl bg-ok-bg px-3 py-2 text-[13px] text-ok">
        <span className="font-semibold">Check: </span>
        {step.check}
      </p>

      {maintenance && done && <p className="mt-3 text-[12px] text-ok">{answer ? "Completed by myself" : "Completed with help"}</p>}
      <div className="mt-4 flex flex-wrap gap-2">
        {done ? (
          !last && (
            <button onClick={onNext} tabIndex={tab} className={`rounded-full bg-accent px-4 py-2 text-[14px] font-medium text-white hover:bg-accent-hover ${press} ${focus}`}>
              Next step
            </button>
          )
        ) : (
          <button
            onClick={onComplete}
            disabled={maintenance && !!behind}
            tabIndex={tab}
            className={`flex items-center gap-1.5 rounded-full bg-accent px-4 py-2 text-[14px] font-medium text-white hover:bg-accent-hover disabled:opacity-40 ${press} ${focus}`}
          >
            <Check size={14} aria-hidden />
            {maintenance ? "Done it myself" : last ? "Done, the PC has a display" : "Complete this step"}
          </button>
        )}
        {maintenance && !done && <button disabled={!!behind} onClick={onHelp} tabIndex={tab} className={`rounded-full bg-fill px-4 py-2 text-[14px] text-ink disabled:opacity-40 ${focus}`}>I needed help</button>}
        {onReplay && (
          <button onClick={onReplay} tabIndex={tab} className={`rounded-full bg-fill px-4 py-2 text-[14px] font-medium text-ink hover:bg-fill-hover ${press} ${focus}`}>
            Replay installation
          </button>
        )}
      </div>
    </div>
  )
}

/** Spring Check của ReactBits; tiêu đề bước vẫn là nút riêng để mở hướng dẫn. */
function CheckBox({ checked, onChange, label }: { checked: boolean; onChange: () => void; label: string }) {
  return <SpringCheck checked={checked} onChange={onChange} label={null} ariaLabel={label} color="var(--color-ink)" fillColor="var(--color-accent)" checkColor="#fff" boxSize={24} boxRadius={8} bounce={0.16} strike="none" className="shrink-0" />
}

const RATED = 'pcsc-rated'

/** Máy này đã tự hiện hộp chấm sao chưa (lưu trên trình duyệt; bị chặn lưu thì coi như chưa) */
function ratedBefore(machine: string) {
  try {
    return (JSON.parse(localStorage.getItem(RATED) ?? '[]') as string[]).includes(machine)
  } catch {
    return false
  }
}

function markRated(machine: string) {
  try {
    const list = JSON.parse(localStorage.getItem(RATED) ?? '[]') as string[]
    localStorage.setItem(RATED, JSON.stringify([...list.filter((m) => m !== machine), machine].slice(-50)))
  } catch {
    /* bỏ qua */
  }
}

/** Dòng chữ trên hình 3D: đang ở bước nào */
export function GuideCaption() {
  const guide = useViewer((s) => s.guide)
  const familyId = useViewer((s) => s.familyId)
  const buildId = useViewer((s) => s.buildId)
  const index = useViewer((s) => s.guideIndex)
  const full = useViewer((s) => s.guideFull)
  const setFull = useViewer((s) => s.setGuideFull)
  const hasGpu = useViewer((s) => s.advisedBuild == null || s.advisedBuild.items.some((i) => i.category === 'gpu'))
  const kind = useViewer(s => s.guideKind)
  const task = useViewer(s => s.maintenanceTask)
  const cooler = useViewer(s => s.maintenanceCooler)
  const maintenanceGpu = useViewer(s => s.maintenanceHasGpu)
  const steps = useMemo(() => kind === 'maintenance' ? maintenanceSteps(task, familyId, buildId, maintenanceGpu, cooler) : guideSteps(familyId, buildId, hasGpu), [kind, task, familyId, buildId, hasGpu, maintenanceGpu, cooler])
  if (!guide) return null
  const s = steps[Math.min(index, steps.length - 1)]
  const text = full ? "Overview: all components are assembled" : `Step ${index + 1}/${steps.length}: ${s.title}`
  const pill = 'glass rounded-full bg-glass shadow-[0_6px_24px_rgba(0,0,0,0.10)] ring-1 ring-line backdrop-blur-xl'
  return (
    <div className="pointer-events-none absolute top-4 left-0 z-10 flex w-full flex-wrap items-center justify-center gap-2 px-4">
      <div key={text} aria-live="polite" className={`swap px-4 py-2 text-center text-[14px] font-medium text-ink ${pill}`}>
        {text}
      </div>
      {/* Chuyển giữa "chỉ hiện linh kiện tới bước đang xem" và cả máy đã lắp xong để nhìn toàn cảnh */}
      <button
        onClick={() => setFull(!full)}
        aria-pressed={full}
        className={`pointer-events-auto flex items-center gap-1.5 px-3.5 py-2 text-[14px] font-medium text-link hover:text-ink ${pill} ${press} ${focus}`}
      >
        {full ? <ListNumbers size={15} aria-hidden /> : <Cube size={15} aria-hidden />}
        {full ? "Return to current step" : "Full view"}
      </button>
    </div>
  )
}
