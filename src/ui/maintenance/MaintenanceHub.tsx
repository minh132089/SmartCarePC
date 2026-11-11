import { useEffect, useRef, useState } from 'react'
import { FAMILIES, FAMILY_IDS } from '../../data/families'
import { machineOf } from '../../data/guide'
import { addDays, day, EMPTY_ANSWERS, maintenancePlan, TASKS, type Cooler, type MaintenanceAnswers, type MaintenanceTask } from '../../data/maintenance'
import { googleCalendarUrl, reminderIcs } from '../../data/maintenanceCalendar'
import { useViewer } from '../../store'
import { Choices, Dialog, PrimaryButton, Question, QuietButton, focus } from '../study/StudyParts'
import { newSubmissionId, useStudy } from '../study/study'
import { calendarLink, currentPC, flushMaintenanceQueue, recordEvent, startMaintenance, useMaintenance, type MaintenancePC } from './maintenanceState'

const TASK_IDS = Object.keys(TASKS) as MaintenanceTask[]
const THUMB = { standard_tower: '01', compact_tower: '02', showcase_dual_chamber: '03', mini_sff: '04', full_tower: '06' }
const COOLERS: { value: Cooler; label: string }[] = [{ value: 'air', label: 'Tower air cooler' }, { value: 'lowprofile', label: 'Low-profile fan' }, { value: 'aio', label: 'Liquid cooler (AIO)' }, { value: 'unknown', label: "I don’t know" }]
const SYMPTOMS = [{ value: 'noise', label: 'Louder fans' }, { value: 'heat', label: 'Hot or slow under load' }, { value: 'shutdown', label: 'Unexpected shutdowns / restarts' }]
const input = `w-full rounded-xl bg-surface px-3 py-2 text-[14px] text-ink ring-1 ring-line-strong ${focus}`

export function MaintenanceEntry({ className = '' }: { className?: string }) {
  return <button onClick={() => useMaintenance.getState().open()} className={`rounded-full bg-fill px-4 py-2 text-[14px] font-medium text-link hover:bg-fill-hover ${focus} ${className}`}>Maintenance guide</button>
}
/** Cùng kiểu hộp thoại với khảo sát, không thu thông tin liên hệ. */
export function MaintenanceHub() {
  const dialog = useMaintenance(s => s.dialog)
  const pending = useMaintenance(s => s.pending)
  const pc = useMaintenance(s => s.pc)
  const source = useMaintenance(s => s.source)
  const dates = useMaintenance(s => s.dates)
  const close = useMaintenance(s => s.close)
  const open = useMaintenance(s => s.open)
  const snooze = useMaintenance(s => s.snooze)
  const studyDialog = useStudy(s => s.dialog)
  const guide = useViewer(s => s.guide && s.page === 'build')
  const advised = useViewer(s => s.advisedBuild)
  const [today, setToday] = useState(day)
  const opened = useRef(false)
  useEffect(() => {
    void flushMaintenanceQueue()
    const onFocus = () => { setToday(day()); void flushMaintenanceQueue() }
    window.addEventListener('focus', onFocus)
    const timer = setInterval(onFocus, 60_000)
    if (!opened.current) {
      opened.current = true
      const q = new URLSearchParams(location.search), task = q.get('baotri')
      if (TASK_IDS.includes(task as MaintenanceTask)) {
        q.delete('baotri'); history.replaceState(null, '', location.pathname + (q.size ? '?' + q : '') + location.hash)
        const t = task as MaintenanceTask
        recordEvent('opened_from_calendar', t)
        useViewer.getState().setPage('build')
        if (useMaintenance.getState().pc) startMaintenance(useMaintenance.getState().pc!, t)
        else open(t, 'calendar')
      }
    }
    return () => { window.removeEventListener('focus', onFocus); clearInterval(timer) }
  }, [open])
  const due = TASK_IDS.find(task => dates[task] && dates[task]! <= today)
  const shown = useRef('')
  const code = useStudy(s => s.code), phase = useStudy(s => s.phase)
  useEffect(() => {
    if (!due || guide || dialog || studyDialog || !code) return
    const id = `${code}:${phase}:${due}:${dates[due]}`
    if (shown.current === id) return
    shown.current = id
    recordEvent('reminder_shown', due, { scheduledFor: dates[due] })
  }, [due, dates, guide, dialog, studyDialog, code, phase])
  return <>
    {due && !guide && !dialog && !studyDialog && <div className="fixed right-4 bottom-4 z-40 max-w-[350px] rounded-2xl bg-panel p-4 text-ink shadow-xl ring-1 ring-line" role="status">
      <p className="font-semibold">{due === 'dust' ? 'Time to clean your PC' : `Time for: ${TASKS[due].title.toLowerCase()}`}</p>
      <p className="mt-1 text-[13px] text-ink-2">Your planned date: {dates[due]}. Work at a time that is safe for you.</p>
      <p className="mt-1 text-[12px] text-ink-3">Calendar events must be rescheduled in your calendar.</p><div className="mt-3 flex flex-wrap gap-2"><PrimaryButton onClick={() => pc ? startMaintenance(pc, due) : open(due)}>Start guide</PrimaryButton><QuietButton onClick={() => snooze(due)}>Remind me in 1 week</QuietButton></div>
    </div>}
    {dialog === 'choose' && !studyDialog && <ChoosePC key={pending} initial={advised ? currentPC() : pc} task={pending} fromCalendar={source === 'calendar'} advised={!!advised} onClose={close} />}
    {dialog === 'reminders' && !studyDialog && pc && <ReminderSetup pc={pc} onClose={close} />}
  </>
}
function CoolerPicture({ kind }: { kind: Cooler }) {
  return <svg viewBox="0 0 120 76" className="h-16 w-full text-ink-2" aria-hidden="true" fill="none" stroke="currentColor" strokeWidth="2">
    {kind === 'air' ? <><rect x="40" y="9" width="44" height="53" rx="5" /><path d="M45 20h34M45 29h34M45 38h34M45 47h34M45 56h34M50 62v5h25v-5" /><circle cx="59" cy="36" r="16" /><path d="m59 21 4 12 10 9-14-3-13 3 9-10Z" /></> : kind === 'lowprofile' ? <><rect x="22" y="46" width="78" height="13" rx="4" /><ellipse cx="60" cy="35" rx="34" ry="15" /><path d="m60 21 4 12 22 7-26-2-24 2 20-8Z" /></> : kind === 'aio' ? <><rect x="60" y="8" width="47" height="58" rx="4" /><circle cx="83" cy="24" r="12" /><circle cx="83" cy="50" r="12" /><rect x="10" y="32" width="25" height="25" rx="7" /><path d="M35 38c20-32 27-23 27-23M35 48c18 22 29 9 29 9" /></> : <><circle cx="60" cy="36" r="25" /><text x="60" y="45" textAnchor="middle" fill="currentColor" stroke="none" fontSize="28">?</text></>}
  </svg>
}
function ChoosePC({ initial, task: firstTask, advised, fromCalendar, onClose }: { initial: MaintenancePC | null; task: MaintenanceTask; advised: boolean; fromCalendar: boolean; onClose: () => void }) {
  const [pc, setPC] = useState<MaintenancePC>(initial ?? { machine: 'standard_tower', cooler: 'unknown', hasGpu: true })
  const [selected, setSelected] = useState(!!initial)
  const [laptop, setLaptop] = useState(false)
  const [task, setTask] = useState(firstTask)
  const remember = useMaintenance(s => s.rememberPC)
  const modelCooler = machineOf(pc.machine === 'gaming' ? null : pc.machine, 'gaming').cooler
  return <Dialog title="Care for your PC" onClose={onClose} footer={<><QuietButton onClick={onClose}>Cancel</QuietButton><PrimaryButton disabled={laptop || !selected} onClick={() => { remember(pc); useMaintenance.setState({ dialog: 'reminders' }) }}>Set reminders</PrimaryButton><PrimaryButton disabled={laptop || !selected} onClick={() => startMaintenance(pc, task)}>Start guide</PrimaryButton></>}>
    <p className="text-[14px] text-ink-2">Short, practical desktop PC guides. Your progress stays in this browser.</p>
    {advised ? <p className="mt-4 rounded-xl bg-info-bg p-3 text-[14px] text-info">Using the PC you selected from your recommendations.</p> : <>
      <Question label="Which one looks most like your PC?" hint="Choose the closest desktop shape. The 3D view is a reference, not an exact service manual.">
        <div className="grid grid-cols-3 gap-2" role="radiogroup" aria-label="Your desktop PC style">
          {[...FAMILY_IDS, 'gaming' as const].map(id => <button key={id} role="radio" aria-checked={!laptop && selected && pc.machine === id} onClick={() => { setPC({ ...pc, machine: id }); setSelected(true); setLaptop(false) }} className={`rounded-xl bg-surface p-2 text-[12px] ring-1 ${selected && !laptop && pc.machine === id ? 'ring-2 ring-accent' : 'ring-line'} ${focus}`}>
            {id === 'gaming' ? <svg viewBox="0 0 120 100" className="aspect-square w-full text-ink-2" aria-hidden="true"><rect x="28" y="8" width="60" height="82" rx="5" fill="none" stroke="currentColor" strokeWidth="3" /><path d="m39 17 36 0v57H39z" fill="var(--color-fill)" /><circle cx="57" cy="35" r="11" fill="none" stroke="currentColor" /><path d="M39 57h36M38 95v-5m42 5v-5" stroke="currentColor" /></svg> : <img src={`/families/${THUMB[id]}_${id}.jpg`} alt="" className="aspect-square w-full rounded-lg object-contain" />}
            {id === 'gaming' ? 'Gaming tower' : FAMILIES[id].label}
          </button>)}
        </div>
        <label className="mt-3 flex items-center gap-2 text-[14px]"><input type="checkbox" checked={laptop} onChange={e => setLaptop(e.target.checked)} /> I use a laptop</label>
      </Question>
      {laptop ? <p className="mt-3 rounded-xl bg-info-bg p-3 text-[14px] text-info">This guide is for desktop PCs. Laptop access and batteries require a model-specific service guide.</p> : <Question label="What cooler does it have?">
        <div className="grid grid-cols-2 gap-2">{COOLERS.map(c => <button key={c.value} aria-pressed={pc.cooler === c.value} onClick={() => setPC({ ...pc, cooler: c.value })} className={`rounded-xl bg-surface p-2 text-[12px] ring-1 ${pc.cooler === c.value ? 'ring-2 ring-accent' : 'ring-line'} ${focus}`}><CoolerPicture kind={c.value} />{c.label}</button>)}</div>
        {pc.cooler === 'unknown' && <p className="mt-2 text-[13px] text-ink-2">We will explain a tower air cooler first. After shutting down and unplugging, compare your cooler with these pictures before loosening anything.</p>}
        {pc.cooler !== 'unknown' && pc.cooler !== modelCooler && <p className="mt-2 text-[13px] text-info">This case reference shows a different cooler. Written instructions follow your chosen cooler; the 3D cooler is illustrative.</p>}
        <label className="mt-3 flex items-center gap-2 text-[14px]"><input type="checkbox" checked={pc.hasGpu} onChange={e => setPC({ ...pc, hasGpu: e.target.checked })} /> My PC has a separate graphics card</label>
      </Question>}
    </>}
    {!laptop && <Question label={fromCalendar ? 'Your calendar opened this guide' : 'What would you like to do?'}><div className="space-y-2">{TASK_IDS.map(id => <label key={id} className="flex cursor-pointer gap-3 rounded-xl bg-surface p-3 ring-1 ring-line"><input type="radio" name="maintenance-task" checked={task === id} onChange={() => setTask(id)} /><span><strong className="block text-[14px]">{TASKS[id].title}</strong><span className="text-[12px] text-ink-2">{TASKS[id].hint}</span></span></label>)}</div></Question>}
  </Dialog>
}
export function Symptoms({ value, onChange }: { value: string[]; onChange: (s: string[]) => void }) {
  return <Choices name="maintenance-symptoms" multi value={value.length ? value : ['none']} options={[...SYMPTOMS, { value: 'none', label: 'None' }]} onChange={v => onChange(v.includes('none') && !value.includes('none') && v.at(-1) === 'none' ? [] : v.filter(x => x !== 'none'))} />
}
function ReminderSetup({ pc, onClose }: { pc: MaintenancePC; onClose: () => void }) {
  const saved = useMaintenance(s => s.answers), schedule = useMaintenance(s => s.schedule), dates = useMaintenance(s => s.dates)
  const [a, setA] = useState<MaintenanceAnswers>(saved ?? EMPTY_ANSWERS)
  const [picks, setPicks] = useState<Partial<Record<MaintenanceTask, string>>>(() => ({ ...dates }))
  const storageWarning = useMaintenance(s => s.storageWarning)
  const [savedNow, setSavedNow] = useState(false)
  const [answered, setAnswered] = useState(() => new Set(saved ? ['lastCleaned', 'age', 'paste', 'symptoms'] : []))
  const today = day(), plan = maintenancePlan(a, today)
  const chosen = Object.fromEntries(TASK_IDS.map(t => [t, picks[t] || plan[t].date])) as Record<MaintenanceTask, string>
  const valid = ['lastCleaned', 'age', 'paste', 'symptoms'].every(k => answered.has(k)) && TASK_IDS.every(t => /^\d{4}-\d{2}-\d{2}$/.test(chosen[t]) && (!plan[t].due || chosen[t] >= addDays(today, 1) && chosen[t] <= addDays(today, 14)))
  const update = (v: Partial<MaintenanceAnswers>) => { setA({ ...a, ...v }); setPicks({}); setAnswered(new Set([...answered, ...Object.keys(v)])) }
  const exportCalendar = (task: MaintenanceTask, method: 'google' | 'ics') => {
    const r = { task, date: dates[task]!, months: plan[task].months, link: calendarLink(task), uid: newSubmissionId() }
    recordEvent('calendar_added', task, { method, scheduledFor: r.date, action: method === 'google' ? 'opened' : 'downloaded' })
    if (method === 'google') window.open(googleCalendarUrl(r), '_blank', 'noopener,noreferrer')
    else {
      const url = URL.createObjectURL(new Blob([reminderIcs(r)], { type: 'text/calendar;charset=utf-8' }))
      const link = document.createElement('a'); link.href = url; link.download = `smartcarepc-${task}.ics`; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000)
    }
  }
  return <Dialog title="Plan your PC maintenance" onClose={onClose} footer={<><QuietButton onClick={onClose}>{savedNow ? 'Close' : 'Cancel'}</QuietButton>{!savedNow && <PrimaryButton disabled={!valid} onClick={() => { schedule(a, chosen); setSavedNow(true) }}>Save reminders</PrimaryButton>}</>}>
    {savedNow ? <>
      <p className="rounded-xl bg-ok-bg p-3 text-[14px] text-ok">{storageWarning ? 'Saved for this tab only: browser storage is unavailable.' : 'Reminders saved in this browser.'} Add them to your calendar to be reminded when the app is closed.</p>
      <p className="mt-3 text-[13px] text-ink-2">Google opens a draft: save it and check its notification settings. Calendar files include a reminder 1 day before; your calendar must allow notifications.</p>
      {TASK_IDS.map(t => <section key={t} className="mt-4 rounded-xl bg-surface p-3 ring-1 ring-line"><h3 className="font-semibold">{TASKS[t].title}</h3><p className="mt-1 text-[13px] text-ink-2">{dates[t]} · repeats every {plan[t].months} months</p><div className="mt-3 flex flex-wrap gap-2"><QuietButton onClick={() => exportCalendar(t, 'google')}>Add to Google Calendar ↗</QuietButton><QuietButton onClick={() => exportCalendar(t, 'ics')}>Download .ics</QuietButton></div></section>)}
    </> : <>
      <p className="text-[13px] text-ink-2">No email or phone number needed. Dates are suggestions; stop and seek help if you are unsure about a physical task.</p>
      <Question label="When was your PC last cleaned?">
        <Choices name="last-clean" value={answered.has('lastCleaned') ? [a.lastCleaned] : []} options={[{ value: 'never', label: 'Never' }, { value: 'unknown', label: 'I don’t remember' }, { value: 'recent', label: 'Less than 6 months ago' }, { value: 'six-twelve', label: '6–12 months ago' }, { value: 'over-year', label: 'Over a year ago' }]} onChange={v => update({ lastCleaned: v[0], lastCleanedDate: undefined })} />
        {!['never', 'unknown'].includes(a.lastCleaned) && <label className="mt-3 block text-[13px] text-ink-2">Exact date, if known<input type="date" max={today} value={a.lastCleanedDate ?? ''} onChange={e => update({ lastCleanedDate: e.target.value || undefined })} className={`${input} mt-1`} /><span className="mt-1 block text-[12px]">Otherwise, we use the oldest end of the selected range.</span></label>}
      </Question>
      <Question label="How old is your PC?"><Choices name="pc-age" value={answered.has('age') ? [a.age] : []} options={[{ value: 'under-one', label: 'Under 1 year' }, { value: 'one-two', label: '1–2 years' }, { value: 'two-three', label: 'Over 2–3 years' }, { value: 'over-three', label: 'Over 3 years' }]} onChange={v => update({ age: v[0] })} /></Question>
      <Question label="What is its environment like?" hint="Select all that apply; leave empty if none apply."><Choices name="environment" multi value={a.environment} options={[{ value: 'pet', label: 'Pets' }, { value: 'carpet', label: 'Carpet' }, { value: 'smoke', label: 'Smoke' }, { value: 'dust', label: 'Dusty / near a road' }, { value: 'all-day', label: 'Runs all day' }]} onChange={v => update({ environment: v })} /></Question>
      <Question label="Have you noticed any of these symptoms?"><Symptoms value={a.symptoms} onChange={v => update({ symptoms: v, cleanedButStillHot: v.some(x => x === 'heat' || x === 'shutdown') && a.cleanedButStillHot })} />{a.symptoms.some(s => s === 'heat' || s === 'shutdown') && <label className="mt-3 flex gap-2 text-[13px]"><input type="checkbox" checked={a.cleanedButStillHot} onChange={e => update({ cleanedButStillHot: e.target.checked })} /> Heat or shutdowns have continued after dust cleaning</label>}</Question>
      <Question label="Has its CPU thermal paste ever been replaced?"><Choices name="paste-history" value={answered.has('paste') ? [a.paste] : []} options={[{ value: 'never', label: 'Never' }, { value: 'changed', label: 'Yes' }, { value: 'unknown', label: 'I don’t know' }]} onChange={v => update({ paste: v[0] })} />{a.paste === 'changed' && <label className="mt-2 block text-[13px]">Replacement date, if known<input type="date" max={today} value={a.lastPasteDate ?? ''} onChange={e => update({ lastPasteDate: e.target.value || undefined })} className={`${input} mt-1`} /></label>}</Question>
      <p className="mt-5 text-[14px] font-semibold">Your suggested plan</p>{!valid && <p className="mt-1 text-[12px] text-ink-3">Answer the cleaning history, PC age, symptoms and paste history before saving. Dates below update as you answer.</p>}
      {TASK_IDS.map(t => <section key={t} className="mt-2 rounded-xl bg-surface p-3 ring-1 ring-line"><h3 className="text-[14px] font-semibold">{TASKS[t].title}{plan[t].due ? ' · due now' : ''}</h3><p className="mt-1 text-[13px] text-ink-2">{plan[t].reason}</p>{plan[t].due ? <label className="mt-2 block text-[13px]">Choose a day in the next 1–14 days<input type="date" aria-label={`Date for ${TASKS[t].title}`} min={addDays(today, 1)} max={addDays(today, 14)} value={chosen[t]} onChange={e => setPicks({ ...picks, [t]: e.target.value })} className={`${input} mt-1`} /></label> : <p className="mt-2 text-[13px]">Next: {chosen[t]}</p>}</section>)}
      <p className="mt-3 text-[12px] text-ink-3">PC reference: {pc.machine}. Future dates follow your maintenance history. We do not shorten intervals for the study.</p>
    </>}
  </Dialog>
}
