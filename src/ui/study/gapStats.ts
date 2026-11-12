import type { FeedbackRow } from './feedbackStats'
import { score } from './feedbackStats'
import { CONFIDENCE_KEYS, DECISION_CORRECT, DECISION_KEYS, EXTERNAL_SOURCES, INDEPENDENCE_KEYS, TECH_GROUP } from './questions'

/**
 * Gap effect: ghép form lần đầu (baseline) với form cuối (followup) của cùng một mã người tham gia, rồi lấy sau − trước.
 * Ba chỉ số lõi giống hệt nhau ở hai lần: tự tin (trung bình 4 câu, 1–5), tự làm (trung bình 4 câu, 1–5),
 * quyết định (số câu tình huống trả lời đúng, 0–4; "Tôi không biết" và bỏ trống tính 0).
 * Thêm: số nguồn "người khác" thường hỏi (nhân viên cửa hàng, diễn đàn/mạng, bạn bè/người nhà), 0–3.
 */
export type Measure = 'confidence' | 'independence' | 'decisions' | 'sources'
export const MEASURES: { key: Measure; label: string; range: string; better: 'up' | 'down' }[] = [
  { key: 'confidence', label: "Confidence (Q1)", range: "mean of 4 items, 1–5", better: 'up' },
  { key: 'independence', label: "Doing it yourself (Q2)", range: "mean of 4 items, 1–5", better: 'up' },
  { key: 'decisions', label: "Decision quality", range: "correct answers, 0–4", better: 'up' },
  { key: 'sources', label: "Other people asked (Q2)", range: "shop staff, online, friends/family, 0–3", better: 'down' },
]
export type Group = 'non_tech' | 'tech' | 'unknown'
export const GROUP_LABEL: Record<Group, string> = { non_tech: "No technical background", tech: "Technical background", unknown: "Background not recorded" }

const mean = (xs: number[]) => (xs.length ? xs.reduce((a, b) => a + b, 0) / xs.length : null)
const median = (xs: number[]) => {
  if (!xs.length) return null
  const s = [...xs].sort((a, b) => a - b), m = Math.floor(s.length / 2)
  return s.length % 2 ? s[m] : (s[m - 1] + s[m]) / 2
}

export function measuresOf(a: Record<string, unknown>): Record<Measure, number | null> {
  const conf = CONFIDENCE_KEYS.map((k) => score(a[k])).filter((v): v is number => v != null)
  const ind = INDEPENDENCE_KEYS.map((k) => score(a[k])).filter((v): v is number => v != null)
  const answeredDecisions = DECISION_KEYS.filter((k) => typeof a[k] === 'string')
  const sources = Array.isArray(a.sources) ? a.sources.filter((v): v is string => typeof v === 'string') : null
  return {
    confidence: mean(conf),
    independence: mean(ind),
    decisions: answeredDecisions.length ? DECISION_KEYS.filter((k) => a[k] === DECISION_CORRECT[k]).length : null,
    sources: sources ? sources.filter((s) => EXTERNAL_SOURCES.includes(s)).length : null,
  }
}

export interface Person {
  code: string
  group: Group
  techBg: string | null
  ownPc: string | null
  baselineAt: string | null
  followupAt: string | null
  before: Record<Measure, number | null> | null
  after: Record<Measure, number | null> | null
  maintDone: string | null
  /** Form lần đầu gửi sau lần dùng app đầu tiên: mức nền có thể đã bị hướng dẫn ảnh hưởng */
  baselineLate: boolean
  usage: { advice: number; upgrade: number; buildSteps: number; buildCompleted: number; maintenanceStarted: number; maintenanceCompleted: number }
}

export interface UsageRow { participantCode: string; eventType: string; task: string; occurredAt: string }

const latest = (rows: FeedbackRow[], form: string) => rows.filter((r) => r.answers.form === form).sort((a, b) => b.id - a.id)[0]

export function people(feedback: FeedbackRow[], events: UsageRow[]): Person[] {
  const codes = new Set<string>()
  for (const r of feedback) if (r.participantCode && (r.answers.form === 'baseline' || r.answers.form === 'followup')) codes.add(r.participantCode)
  return [...codes].sort().map((code) => {
    const mine = feedback.filter((r) => r.participantCode === code)
    const b = latest(mine, 'baseline'), f = latest(mine, 'followup')
    const techBg = typeof b?.answers.tech_bg === 'string' ? b.answers.tech_bg : null
    const ev = events.filter((e) => e.participantCode === code)
    const count = (t: string) => ev.filter((e) => e.eventType === t).length
    const firstUse = ev.filter((e) => e.task === 'none').map((e) => e.occurredAt).sort()[0]
    // Server ghi createdAt (giờ máy chủ, không múi giờ) còn occurredAt là ISO: so theo thời điểm khi đọc được cả hai
    const late = !!(b?.createdAt && firstUse && Date.parse(b.createdAt) > Date.parse(firstUse) + 60_000)
    return {
      code,
      group: techBg ? TECH_GROUP[techBg] ?? 'unknown' : 'unknown',
      techBg,
      ownPc: typeof b?.answers.own_pc === 'string' ? b.answers.own_pc : null,
      baselineAt: b?.createdAt ?? null,
      followupAt: f?.createdAt ?? null,
      before: b ? measuresOf(b.answers) : null,
      after: f ? measuresOf(f.answers) : null,
      maintDone: typeof f?.answers.maint_done === 'string' ? f.answers.maint_done : null,
      baselineLate: late,
      usage: {
        advice: count('advice_asked'),
        upgrade: count('upgrade_asked'),
        buildSteps: new Set(ev.filter((e) => e.eventType === 'build_step_done').map((e) => JSON.stringify(e))).size,
        buildCompleted: count('build_guide_completed'),
        maintenanceStarted: count('maintenance_started'),
        maintenanceCompleted: count('maintenance_completed'),
      },
    }
  })
}

export interface GapCell { n: number; before: number | null; after: number | null; change: number | null; median: number | null; better: number; same: number; worse: number }

/** Một ô bảng: chỉ tính người có cả hai lần đo của chỉ số đó */
export function gap(ps: Person[], m: Measure): GapCell {
  const pairs = ps.flatMap((p) => (p.before?.[m] != null && p.after?.[m] != null ? [[p.before[m]!, p.after[m]!] as const] : []))
  const diffs = pairs.map(([b, a]) => a - b)
  const dir = MEASURES.find((x) => x.key === m)!.better === 'up' ? 1 : -1
  return {
    n: pairs.length,
    before: mean(pairs.map(([b]) => b)),
    after: mean(pairs.map(([, a]) => a)),
    change: mean(diffs),
    median: median(diffs),
    better: diffs.filter((d) => d * dir > 0).length,
    same: diffs.filter((d) => d === 0).length,
    worse: diffs.filter((d) => d * dir < 0).length,
  }
}
