import { create } from 'zustand'
import { FAMILY_IDS, type FamilyId } from '../../data/families'
import { machineOf, loadDone, saveDone } from '../../data/guide'
import { addDays, addMonths, day, EMPTY_ANSWERS, maintenancePlan, maintenanceSteps, TASKS, type Cooler, type MaintenanceAnswers, type MaintenanceTask } from '../../data/maintenance'
import { useViewer } from '../../store'
import { newSubmissionId, useStudy, type Phase } from '../study/study'

const KEY = 'pcsc-maintenance-v1'
const QUEUE = 'pcsc-maintenance-queue-v1'
export interface MaintenancePC { machine: FamilyId | 'gaming'; cooler: Cooler; hasGpu: boolean }
export type EventType = 'reminder_set' | 'calendar_added' | 'reminder_shown' | 'reminder_snoozed' | 'opened_from_calendar' | 'maintenance_started' | 'step_done' | 'maintenance_completed' | 'maintenance_abandoned'
export interface MaintenanceEvent { eventId: string; eventType: EventType; task: MaintenanceTask; participantCode: string; phase: Phase; machine: string; occurredAt: string; data: Record<string, unknown> }
interface Run { id: string; task: MaintenanceTask; machine: string; completed: boolean; study: { code: string | null; phase: Phase }; alone: Record<string, boolean> }
interface Saved { pc: MaintenancePC | null; answers: MaintenanceAnswers | null; dates: Partial<Record<MaintenanceTask, string>>; completed: Partial<Record<MaintenanceTask, string>>; run: Run | null }
export function readStored<T>(key: string, fallback: T): T {
  try { const s = localStorage.getItem(key); return s ? JSON.parse(s) as T : fallback } catch { return fallback }
}
function writeStored(key: string, value: unknown) {
  try { localStorage.setItem(key, JSON.stringify(value)); return true } catch { return false }
}
const initial = (): Saved => {
  const fallback: Saved = { pc: null, answers: null, dates: {}, completed: {}, run: null }
  const saved = readStored<Saved>(KEY, fallback)
  if (!saved || !saved.dates || !saved.completed || (saved.pc && ![...FAMILY_IDS, 'gaming'].includes(saved.pc.machine))) return fallback
  return { ...fallback, ...saved }
}
interface MaintenanceState extends Saved {
  dialog: 'choose' | 'reminders' | null
  pending: MaintenanceTask
  source: 'calendar' | 'guide'
  storageWarning: boolean
  queueProblem: boolean
  open: (task?: MaintenanceTask, source?: 'calendar' | 'guide') => void
  close: () => void
  rememberPC: (pc: MaintenancePC) => void
  schedule: (answers: MaintenanceAnswers, dates: Record<MaintenanceTask, string>) => void
  snooze: (task: MaintenanceTask) => void
  finish: (task: MaintenanceTask, who: string, stillHot: boolean, symptoms?: string[]) => void
}
export const useMaintenance = create<MaintenanceState>((set, get) => ({
  ...initial(), dialog: null, pending: 'dust', source: 'guide', queueProblem: false, storageWarning: false,
  open: (pending = 'dust', source = 'guide') => set({ dialog: 'choose', pending, source }),
  close: () => set({ dialog: null }),
  rememberPC: (pc) => { set({ pc }); persist() },
  schedule: (answers, dates) => {
    const s = get(), plan = maintenancePlan(answers)
    // Giữ câu trả lời lần đầu trong sự kiện, không ghi đè mốc trước của người tham gia.
    set({ answers, dates }); persist()
    for (const task of Object.keys(TASKS) as MaintenanceTask[]) recordEvent('reminder_set', task, { answers, never_serviced: ['never', 'unknown'].includes(answers.lastCleaned), scheduledFor: dates[task], dueNow: plan[task].due, intervalMonths: plan[task].months, firstSetup: !s.answers })
  },
  snooze: (task) => { const date = addDays(day(), 7); set({ dates: { ...get().dates, [task]: date } }); persist(); recordEvent('reminder_snoozed', task, { scheduledFor: date }) },
  finish: (task, who, stillHot, symptoms) => {
    const s = get(), interval = task === 'dust' && s.answers?.environment.length ? 3 : TASKS[task].months
    const answers = { ...(s.answers ?? EMPTY_ANSWERS), ...(task === 'dust' ? { lastCleanedDate: day(), lastCleaned: 'recent' as const, cleanedButStillHot: stillHot } : {}), ...(symptoms ? { symptoms, cleanedButStillHot: symptoms.some(x => x === 'heat' || x === 'shutdown') && (s.answers?.cleanedButStillHot ?? false) } : {}), ...(task === 'paste' ? { paste: 'changed' as const, lastPasteDate: day(), cleanedButStillHot: false } : {}) }
    const dates = { ...s.dates, [task]: addMonths(day(), interval) }
    if (answers && (task === 'check' || task === 'dust')) {
      const plan = maintenancePlan(answers)
      if (task === 'check' && plan.dust.due) dates.dust = day()
      if (plan.paste.due) dates.paste = day()
    }
    recordRun('maintenance_completed', { who, stillHot, ...(symptoms ? { symptoms, cleanedButStillHot: symptoms.some(x => x === 'heat' || x === 'shutdown') && (s.answers?.cleanedButStillHot ?? false) } : {}) })
    set({ completed: { ...s.completed, [task]: day() }, dates, answers, run: s.run ? { ...s.run, completed: true } : null }); persist()
  },
}))
function persist() { const { pc, answers, dates, completed, run } = useMaintenance.getState(); if (!writeStored(KEY, { pc, answers, dates, completed, run })) useMaintenance.setState({ storageWarning: true }) }
export const maintenanceKey = (pc: MaintenancePC, task: MaintenanceTask) => `maint-${task}-${pc.machine}-${pc.cooler}-${pc.hasGpu ? 'gpu' : 'igpu'}`
export function currentPC(): MaintenancePC {
  const v = useViewer.getState()
  return { machine: v.familyId ?? 'gaming', cooler: machineOf(v.familyId, v.buildId).cooler, hasGpu: v.advisedBuild == null || v.advisedBuild.items.some((i) => i.category === 'gpu') }
}
export function startMaintenance(pc: MaintenancePC, task: MaintenanceTask) {
  const state = useMaintenance.getState(), key = maintenanceKey(pc, task)
  const viewer = useViewer.getState()
  if (viewer.guide && viewer.guideKind === 'maintenance' && state.run && !state.run.completed && state.run.machine !== key) recordRun('maintenance_abandoned', { stepIndex: viewer.guideIndex, completedSteps: Object.keys(state.run.alone).length })
  const study = useStudy.getState()
  const resume = state.run && !state.run.completed && state.run.task === task && state.run.machine === key && state.run.study.code === study.code && state.run.study.phase === study.phase
  const run: Run = resume ? state.run! : { id: newSubmissionId(), task, machine: key, completed: false, study: { code: study.code, phase: study.phase }, alone: {} }
  if (!resume) saveDone(key, new Set())
  useMaintenance.setState({ pc, run, dialog: null }); persist()
  const v = useViewer.getState()
  // Cùng máy đang xem (vd máy vừa chọn ở trang tư vấn) thì giữ nguyên, không xoá cấu hình đã chọn (advisedBuild):
  // quay lại hướng dẫn lắp vẫn đúng các món của cấu hình đó (máy không card rời không hiện bước lắp card)
  const same = (v.familyId ?? 'gaming') === pc.machine && (pc.machine !== 'gaming' || v.buildId === 'gaming')
  if (!same) { if (pc.machine === 'gaming') v.setBuild('gaming'); else v.setFamily(pc.machine) }
  const steps = maintenanceSteps(task, pc.machine === 'gaming' ? null : pc.machine, 'gaming', pc.hasGpu, pc.cooler)
  const done = loadDone(key), first = steps.findIndex(s => !done.has(s.id))
  useViewer.setState({ page: 'build', guide: true, guideKind: 'maintenance', maintenanceTask: task, maintenanceRunId: run.id, maintenanceCooler: pc.cooler, maintenanceHasGpu: pc.hasGpu, maintenanceLift: false, guideIndex: first < 0 ? 0 : first, guidePart: null, guideFull: false, exploded: false, step: 0, powered: false, selected: null, inspecting: null, sideOpen: false })
  recordRun('maintenance_started', { resumed: !!resume, cooler: pc.cooler })
}
export function recordStep(stepId: string, alone: boolean) {
  const run = useMaintenance.getState().run
  if (!run || run.completed) return
  useMaintenance.setState({ run: { ...run, alone: { ...run.alone, [stepId]: alone } } }); persist()
  recordRun('step_done', { stepId, alone })
}
export function recordRun(type: EventType, data: Record<string, unknown>) {
  const run = useMaintenance.getState().run
  if (!run || run.completed || !run.study.code) return
  const current = useStudy.getState()
  if (current.code !== run.study.code || current.phase !== run.study.phase) return
  recordEvent(type, run.task, { runId: run.id, ...data })
}
export function recordEvent(eventType: EventType, task: MaintenanceTask, data: Record<string, unknown> = {}) {
  const { code, phase } = useStudy.getState()
  if (!code) return
  const s = useMaintenance.getState()
  const event: MaintenanceEvent = { eventId: newSubmissionId(), eventType, task, participantCode: code, phase, machine: s.pc?.machine ?? (useViewer.getState().advisedBuild ? currentPC().machine : 'unselected'), occurredAt: new Date().toISOString(), data }
  const queue = readStored<MaintenanceEvent[]>(QUEUE, [])
  // Tránh cắt bỏ âm thầm người không làm xong; báo rõ nếu trình duyệt không thể lưu.
  if (!writeStored(QUEUE, [...queue, event])) {
    volatileQueue.push(event)
    useMaintenance.setState({ queueProblem: true })
  }
  void flushMaintenanceQueue()
}
const volatileQueue: MaintenanceEvent[] = []
let flushing = false
export async function flushMaintenanceQueue() {
  const code = useStudy.getState().code
  if (!code || flushing) return
  flushing = true
  try {
    const delivered = new Set<string>()
    while (useStudy.getState().code === code) {
      const event = [...readStored<MaintenanceEvent[]>(QUEUE, []), ...volatileQueue].find(e => e.participantCode === code && !delivered.has(e.eventId))
      if (!event) break
      try {
        const r = await fetch('/api/maintenance/events', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(event), signal: AbortSignal.timeout(8000), keepalive: true })
        if (!r.ok) break
        delivered.add(event.eventId)
        writeStored(QUEUE, readStored<MaintenanceEvent[]>(QUEUE, []).filter(e => e.eventId !== event.eventId))
        const i = volatileQueue.findIndex(e => e.eventId === event.eventId); if (i >= 0) volatileQueue.splice(i, 1)
      } catch { break }
    }
  } finally { flushing = false }
}
export function calendarLink(task: MaintenanceTask) {
  const url = new URL(location.origin + '/')
  url.searchParams.set('baotri', task)
  const { code, phase } = useStudy.getState()
  if (code) { url.searchParams.set('thunghiem', code); if (phase === 'after') url.searchParams.set('lan', 'sau') }
  return url.toString()
}
if (typeof window !== 'undefined') {
  window.addEventListener('online', () => void flushMaintenanceQueue())
  useStudy.subscribe(() => void flushMaintenanceQueue())
}

// Rời bài giữa chừng vẫn là dữ liệu nghiên cứu; không loại những người chưa hoàn thành.
let lastAbandonedRun = ''
function abandoned() {
  const v = useViewer.getState(), run = useMaintenance.getState().run
  if (!v.guide || v.guideKind !== 'maintenance' || !run || run.completed || lastAbandonedRun === run.id) return
  lastAbandonedRun = run.id
  recordRun('maintenance_abandoned', { stepIndex: v.guideIndex, completedSteps: Object.keys(run.alone).length })
}
useViewer.subscribe((next, prev) => {
  if (prev.guide && prev.guideKind === 'maintenance' && (!next.guide || next.page !== 'build' || next.guideKind !== 'maintenance')) {
    const run = useMaintenance.getState().run
    if (run && !run.completed && lastAbandonedRun !== run.id) {
      lastAbandonedRun = run.id
      recordRun('maintenance_abandoned', { stepIndex: prev.guideIndex, completedSteps: Object.keys(run.alone).length })
    }
  }
  if (!prev.guide && next.guide && next.guideKind === 'maintenance') lastAbandonedRun = ''
})
if (typeof window !== 'undefined') window.addEventListener('pagehide', abandoned)
