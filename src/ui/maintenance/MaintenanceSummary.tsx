import { TASKS, type MaintenanceTask } from '../../data/maintenance'
import type { FeedbackRow } from '../study/feedbackStats'
import { AGENT_COMPLETION_DAYS, agentStats, emailStats, maintenanceGroups } from './maintenanceStats'
import type { MaintenanceEvent } from './maintenanceState'
import { QuietButton } from '../study/StudyParts'

export interface MaintenanceRow extends MaintenanceEvent { id: number; createdAt: string }
export function MaintenanceSummary({ rows, feedback, error, download }: { rows: MaintenanceRow[] | null; feedback: FeedbackRow[]; error: string | null; download?: () => void }) {
  const groups = maintenanceGroups(rows ?? [], feedback)
  const agent = agentStats(rows ?? [])
  const email = emailStats(rows ?? [])
  return <section className="mt-6 rounded-2xl bg-panel p-4 ring-1 ring-line">
    <div className="flex flex-wrap items-center justify-between gap-3"><h2 className="text-[18px] font-semibold">Maintenance study</h2>{download && <QuietButton onClick={download}>Download maintenance CSV</QuietButton>}</div>
    <p className="mt-2 text-[13px] text-ink-2">Unique participants per phase, grouped by their first-use maintenance baseline. Calendar exports / clicks do not confirm an event was saved or a notification delivered. People who stop remain included.</p>
    <p className="mt-1 text-[12px] text-ink-3">Technical background comes from each participant's first-use form. Baseline maintenance history is kept in reminder_set events. During the study, reminders are simulated: they appear the next time the participant opens the app after setting them (marked “simulated” in the event details).</p>
    {error && <p role="alert" className="mt-3 text-[13px] text-bad">{error}</p>}
    {!error && rows && !rows.length && <p className="mt-3 text-[14px] text-ink-3">No maintenance study events yet.</p>}
    {groups.length > 0 && <div className="mt-4 overflow-x-auto"><table className="w-full text-left text-[12px]"><thead><tr>{['Group', 'People', 'Set reminder', 'Calendar export / click', 'Opened from calendar', 'Opened from email', 'Started', 'Completed', 'Left a guide unfinished', 'Steps alone'].map(h => <th key={h} className="border-b border-line px-2 py-2 font-semibold">{h}</th>)}</tr></thead><tbody>{groups.map(g => <tr key={g.label}>{[g.label, g.participants, g.set, g.calendar, g.opened, g.email, g.started, g.completed, g.abandoned, `${g.alone}/${g.steps}`].map((v, i) => <td key={i} className="border-b border-line px-2 py-3">{v}</td>)}</tr>)}</tbody></table></div>}
    {rows && rows.length > 0 && <div className="mt-6">
      <h3 className="text-[15px] font-semibold">Email reminders</h3>
      <p className="mt-1 text-[13px] text-ink-2">Guides opened from a reminder email: <strong>{email.opened}</strong> by {email.people} {email.people === 1 ? 'participant' : 'participants'}. Led to completed maintenance (same task, ≤ {AGENT_COMPLETION_DAYS} days): <strong>{pct(email.completed, email.opened)}</strong>.</p>
      <p className="mt-1 text-[12px] text-ink-3">Emails sent are not recorded here: the server never stores an email address next to a participant code. Only signed-in users who confirmed their address get these emails.</p>
    </div>}
    {rows && rows.length > 0 && <AgentSummary stats={agent} />}
    {rows && rows.length > 0 && <details className="mt-4 text-[13px]"><summary className="cursor-pointer font-medium">View {rows.length} recorded events</summary><div className="mt-2 max-h-72 overflow-auto"><table className="w-full text-left"><thead><tr>{['Participant / phase', 'Task', 'Event', 'When', 'Work outcome'].map(h => <th key={h} className="px-2 py-2">{h}</th>)}</tr></thead><tbody>{rows.map(e => <tr key={e.eventId}><td className="px-2 py-2">{e.participantCode} / {e.phase}</td><td className="px-2">{TASKS[e.task as MaintenanceTask]?.title ?? e.task}</td><td className="px-2">{e.eventType}</td><td className="px-2">{e.occurredAt}</td><td className="px-2">{typeof e.data.alone === 'boolean' ? e.data.alone ? 'Alone' : 'With help' : String(e.data.who ?? '')}</td></tr>)}</tbody></table></div></details>}
  </section>
}

const SOURCE_LABEL = { schedule: 'On schedule (planned date)', health: 'From a PC symptom (health check)' }
const pct = (n: number, of: number) => of ? `${n} (${Math.round(n / of * 100)}%)` : String(n)
/** App khay Windows: nhắc theo lịch so với nhắc theo triệu chứng (Q3) */
function AgentSummary({ stats }: { stats: ReturnType<typeof agentStats> }) {
  return <div className="mt-6">
    <h3 className="text-[15px] font-semibold">Desktop helper app (Windows)</h3>
    <p className="mt-1 text-[13px] text-ink-2">People who linked the app: <strong>{stats.people}</strong>{stats.byPhase.length > 0 && ` (${stats.byPhase.map(p => `${p.phase}: ${p.people}`).join(' · ')})`}. Installing the app is optional, so these counts cover only people who chose to.</p>
    {stats.people === 0 && !stats.sources.some(s => s.shown) ? <p className="mt-2 text-[13px] text-ink-3">No desktop app events yet.</p> : <>
      <div className="mt-3 overflow-x-auto"><table className="w-full text-left text-[12px]"><thead><tr>{['Reminder type', 'Shown', 'Start guide clicked', 'Snoozed 1 week', `Led to completed maintenance (≤ ${AGENT_COMPLETION_DAYS} days)`].map(h => <th key={h} className="border-b border-line px-2 py-2 font-semibold">{h}</th>)}</tr></thead>
        <tbody>{stats.sources.map(s => <tr key={s.source}>{[SOURCE_LABEL[s.source], s.shown, pct(s.clicked, s.shown), pct(s.snoozed, s.shown), pct(s.completed, s.clicked)].map((v, i) => <td key={i} className="border-b border-line px-2 py-3">{v}</td>)}</tr>)}</tbody></table></div>
      {stats.rules.length > 0 && <p className="mt-2 text-[12px] text-ink-3">Health alerts by rule: {stats.rules.map(([rule, n]) => `${rule} ${n}`).join(' · ')}</p>}
      <p className="mt-1 text-[12px] text-ink-3">Click and snooze rates are out of reminders shown. Completion is out of clicks: each completed maintenance counts once, for the latest click on the same task by the same participant in the {AGENT_COMPLETION_DAYS} days before it. Alerts with no guide (low disk space, full memory) cannot lead to maintenance. Guides opened from a desktop notification are also counted above as “Opened from calendar”.</p>
    </>}
  </div>
}
