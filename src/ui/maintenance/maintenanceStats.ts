import type { FeedbackRow } from '../study/feedbackStats'
import type { MaintenanceRow } from './MaintenanceSummary'

/** Đếm người duy nhất theo nhóm, giữ cả người chưa bắt đầu hoặc dừng giữa chừng. */
export function maintenanceGroups(events: MaintenanceRow[], feedback: FeedbackRow[]) {
  const baseline = new Map<string, string>()
  for (const e of [...events].sort((a, b) => a.id - b.id)) {
    if (e.phase === 'before' && e.eventType === 'reminder_set' && typeof e.data.never_serviced === 'boolean' && !baseline.has(e.participantCode)) baseline.set(e.participantCode, e.data.never_serviced ? 'Never serviced / unsure' : 'Previously serviced')
  }
  const people = new Map<string, { phase: string; baseline: string; background: string; events: MaintenanceRow[] }>()
  for (const e of [...events].sort((a, b) => a.id - b.id)) {
    const key = `${e.participantCode}:${e.phase}`
    const f = feedback.find(r => r.participantCode === e.participantCode && r.phase === e.phase)
    const p = people.get(key) ?? { phase: e.phase, baseline: baseline.get(e.participantCode) ?? 'Baseline unknown', background: String(f?.answers.technical_background ?? 'Not recorded'), events: [] }
    p.events.push(e); people.set(key, p)
  }
  const groups = new Map<string, { label: string; participants: number; set: number; calendar: number; opened: number; started: number; completed: number; abandoned: number; alone: number; steps: number }>()
  for (const p of people.values()) {
    const label = `${p.phase === 'before' ? 'First use' : 'After 4 weeks'} · ${p.baseline} · Technical background: ${p.background}`
    const g = groups.get(label) ?? { label, participants: 0, set: 0, calendar: 0, opened: 0, started: 0, completed: 0, abandoned: 0, alone: 0, steps: 0 }
    const has = (type: string) => p.events.some(e => e.eventType === type)
    g.participants++; g.set += +has('reminder_set'); g.calendar += +has('calendar_added'); g.opened += +has('opened_from_calendar'); g.started += +has('maintenance_started'); g.completed += +has('maintenance_completed'); g.abandoned += +has('maintenance_abandoned')
    const steps = new Map<string, MaintenanceRow>()
    for (const e of p.events) if (e.eventType === 'step_done') steps.set(`${e.data.runId}:${e.data.stepId}`, e)
    g.steps += steps.size; g.alone += [...steps.values()].filter(e => e.data.alone === true).length
    groups.set(label, g)
  }
  return [...groups.values()]
}
