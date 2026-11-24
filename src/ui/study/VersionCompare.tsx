import type { FeedbackRow } from './feedbackStats'
import { V2_SINCE, average, score, versionOf } from './feedbackStats'
import { SCALES } from './FeedbackOverview'
import { MEASURES, gap, people, type UsageRow } from './gapStats'

/**
 * So chất lượng giữa các phiên bản app (v1 / v2, xem appVersion.ts): số phản hồi, điểm sao, điểm từng câu hỏi và gap effect
 * (trước → sau của cùng một người, tính riêng trong từng bản). Người có lần đo đầu ở bản này và lần đo cuối ở bản khác không
 * được ghép đôi ở bản nào: so gap effect giữa hai bản cần mỗi người đo cả hai lần trên cùng một bản.
 */
const fmt = (v: number | null | undefined, digits = 2) => (v == null ? '—' : v.toFixed(digits))
const signed = (v: number | null) => (v == null ? '—' : `${v > 0 ? '+' : ''}${v.toFixed(2)}`)

export function VersionCompare({ feedback, events }: { feedback: FeedbackRow[]; events: UsageRow[] }) {
  const versions = [...new Set(feedback.map(versionOf))].sort()
  if (versions.length < 2) {
    return (
      <section className="mt-6 rounded-2xl bg-panel p-4 ring-1 ring-line">
        <h2 className="text-[18px] font-semibold">Version comparison</h2>
        <p className="mt-2 text-[13px] text-ink-2">
          All {feedback.length} responses so far are from {versions[0] ?? 'one version'}. The comparison appears once responses from another
          version arrive. New responses are tagged with the app version.
        </p>
      </section>
    )
  }
  const byVersion = new Map(versions.map((v) => [v, feedback.filter((r) => versionOf(r) === v)]))
  const scaleRows = SCALES.map(({ q, prefix }) => ({
    label: prefix + q.text.replace(/^\d+\.\s*/, ''),
    cells: versions.map((v) => average(byVersion.get(v)!.map((r) => score(r.answers[q.key])))),
  })).filter((row) => row.cells.some(Boolean))
  const gapRows = MEASURES.map((m) => ({
    label: m.label,
    better: m.better,
    cells: versions.map((v) => gap(people(byVersion.get(v)!, events), m.key)),
  }))
  const first = versions[0]
  const last = versions.at(-1)!
  return (
    <section className="mt-6 rounded-2xl bg-panel p-4 ring-1 ring-line">
      <h2 className="text-[18px] font-semibold">Version comparison ({versions.join(' vs ')})</h2>
      <p className="mt-2 text-[13px] text-ink-2">
        Responses without a version tag sent before {V2_SINCE} count as v1, later ones as v2. Gap effect is computed inside each version: a
        participant is paired only when both of their forms come from the same version.
      </p>
      <div className="mt-3 overflow-x-auto">
        <table className="w-full min-w-[560px] text-[13px]">
          <thead>
            <tr className="text-left text-ink-3">
              <th className="py-1.5 pr-3 font-medium">Measure</th>
              {versions.map((v) => <th key={v} className="py-1.5 pr-3 font-medium">{v}</th>)}
              <th className="py-1.5 font-medium">{last} − {first}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            <tr>
              <td className="py-1.5 pr-3">Responses</td>
              {versions.map((v) => <td key={v} className="py-1.5 pr-3 tabular-nums">{byVersion.get(v)!.length}</td>)}
              <td />
            </tr>
            {[{ label: 'Average star rating', cells: versions.map((v) => average(byVersion.get(v)!.map((r) => score(r.answers.stars)))) }, ...scaleRows].map((row) => {
              const a = row.cells[0], b = row.cells.at(-1)
              return (
                <tr key={row.label}>
                  <td className="py-1.5 pr-3">{row.label}</td>
                  {row.cells.map((c, i) => <td key={i} className="py-1.5 pr-3 tabular-nums">{c ? `${fmt(c.mean)} (${c.n})` : '—'}</td>)}
                  <td className="py-1.5 tabular-nums">{a && b ? signed(b.mean - a.mean) : '—'}</td>
                </tr>
              )
            })}
            {gapRows.map((row) => {
              const a = row.cells[0].change, b = row.cells.at(-1)!.change
              return (
                <tr key={row.label}>
                  <td className="py-1.5 pr-3">Gap effect: {row.label} ({row.better === 'up' ? 'higher is better' : 'lower is better'})</td>
                  {row.cells.map((c, i) => <td key={i} className="py-1.5 pr-3 tabular-nums">{c.n ? `${signed(c.change)} (${c.n} paired)` : '—'}</td>)}
                  <td className="py-1.5 tabular-nums">{a != null && b != null ? signed(b - a) : '—'}</td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
      <p className="mt-2 text-[12px] text-ink-3">Brackets: number of answers (or paired participants for gap effect). Small groups move a lot: read differences with the counts.</p>
    </section>
  )
}
