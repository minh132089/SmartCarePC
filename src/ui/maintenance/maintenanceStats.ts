import type { FeedbackRow } from '../study/feedbackStats'
import { TECH_GROUP } from '../study/questions'
import { PHASE_LABEL, type Phase } from '../study/study'
import type { MaintenanceRow } from './MaintenanceSummary'

/** Đếm người duy nhất theo nhóm, giữ cả người chưa bắt đầu hoặc dừng giữa chừng. */
export function maintenanceGroups(all: MaintenanceRow[], feedback: FeedbackRow[]) {
  // Sự kiện dùng app (task 'none') không thuộc phễu bảo dưỡng
  const events = all.filter(e => e.task !== 'none')
  // Nền kỹ thuật lấy từ form lần đầu (tech_bg) của cùng mã, dùng cho cả hai lần đo
  const background = (code: string) => {
    const b = feedback.filter(r => r.participantCode === code && r.answers.form === 'baseline').sort((x, y) => y.id - x.id)[0]
    const g = typeof b?.answers.tech_bg === 'string' ? TECH_GROUP[b.answers.tech_bg] : undefined
    return g === 'non_tech' ? 'No' : g === 'tech' ? 'Yes' : 'Not recorded'
  }
  const baseline = new Map<string, string>()
  for (const e of [...events].sort((a, b) => a.id - b.id)) {
    if (e.phase === 'before' && e.eventType === 'reminder_set' && typeof e.data.never_serviced === 'boolean' && !baseline.has(e.participantCode)) baseline.set(e.participantCode, e.data.never_serviced ? 'Never serviced / unsure' : 'Previously serviced')
  }
  const people = new Map<string, { phase: string; baseline: string; background: string; events: MaintenanceRow[] }>()
  for (const e of [...events].sort((a, b) => a.id - b.id)) {
    const key = `${e.participantCode}:${e.phase}`
    const p = people.get(key) ?? { phase: e.phase, baseline: baseline.get(e.participantCode) ?? 'Baseline unknown', background: background(e.participantCode), events: [] }
    p.events.push(e); people.set(key, p)
  }
  const groups = new Map<string, { label: string; participants: number; set: number; calendar: number; opened: number; started: number; completed: number; abandoned: number; alone: number; steps: number }>()
  for (const p of people.values()) {
    const label = `${PHASE_LABEL[p.phase as Phase] ?? p.phase} · ${p.baseline} · Technical background: ${p.background}`
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
