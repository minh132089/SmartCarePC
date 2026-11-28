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

/** Một lần bấm Start guide trên thông báo của app khay được tính là dẫn tới bảo dưỡng nếu làm xong trong vòng 7 ngày */
export const AGENT_COMPLETION_DAYS = 7
type AgentSource = 'schedule' | 'health'
/**
 * Đếm sự kiện của app khay Windows (spec mục 7) để trả lời Q3: nhắc theo triệu chứng có làm người ta bắt tay làm nhiều
 * hơn nhắc theo lịch không. Đếm trên mọi sự kiện, kể cả task 'none' (vd ổ đầy, RAM đầy không có bài hướng dẫn).
 * Mỗi lần làm xong (maintenance_completed) chỉ gán cho một lần bấm: lần bấm gần nhất trước đó của cùng người, cùng mốc,
 * cùng việc, trong vòng 7 ngày. Không gán thì không tính, để không đếm một lần làm cho nhiều lần bấm.
 */
export function agentStats(all: MaintenanceRow[]) {
  const rows = [...all].sort((a, b) => a.occurredAt.localeCompare(b.occurredAt) || a.id - b.id)
  const who = (e: MaintenanceRow) => `${e.participantCode}:${e.phase}`
  const linked = new Map<string, Set<string>>()
  for (const e of rows) if (e.eventType === 'agent_linked') linked.set(e.phase, (linked.get(e.phase) ?? new Set()).add(e.participantCode))
  const people = new Set(rows.filter(e => e.eventType === 'agent_linked').map(e => e.participantCode))
  const blank = () => ({ shown: 0, clicked: 0, snoozed: 0, completed: 0 })
  const by: Record<AgentSource, ReturnType<typeof blank>> = { schedule: blank(), health: blank() }
  const rules = new Map<string, number>()
  const clicks: { e: MaintenanceRow; source: AgentSource; used: boolean }[] = []
  const src = (e: MaintenanceRow): AgentSource | null => e.data.source === 'schedule' || e.data.source === 'health' ? e.data.source : null
  for (const e of rows) {
    if (e.eventType === 'agent_reminder_shown') by.schedule.shown++
    else if (e.eventType === 'health_alert_shown') {
      by.health.shown++
      const rule = typeof e.data.rule === 'string' ? e.data.rule : 'unknown'
      rules.set(rule, (rules.get(rule) ?? 0) + 1)
    } else if (e.eventType === 'agent_snoozed') { const s = src(e); if (s) by[s].snoozed++ }
    else if (e.eventType === 'agent_notification_clicked') {
      const s = src(e)
      if (s) { by[s].clicked++; if (e.task !== 'none') clicks.push({ e, source: s, used: false }) }
    } else if (e.eventType === 'maintenance_completed') {
      const at = Date.parse(e.occurredAt)
      const click = clicks.filter(c => !c.used && who(c.e) === who(e) && c.e.task === e.task && Date.parse(c.e.occurredAt) <= at && at - Date.parse(c.e.occurredAt) <= AGENT_COMPLETION_DAYS * 86_400_000).at(-1)
      if (click) { click.used = true; by[click.source].completed++ }
    }
  }
  return {
    people: people.size,
    byPhase: [...linked.entries()].map(([phase, codes]) => ({ phase: PHASE_LABEL[phase as Phase] ?? phase, people: codes.size })),
    sources: (['schedule', 'health'] as const).map(s => ({ source: s, ...by[s] })),
    rules: [...rules.entries()].sort((a, b) => b[1] - a[1]),
  }
}
