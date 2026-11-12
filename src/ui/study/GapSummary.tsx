import { Fragment } from 'react'
import type { FeedbackRow } from './feedbackStats'
import { GROUP_LABEL, MEASURES, gap, people, type Group, type Person, type UsageRow } from './gapStats'

/**
 * Bảng gap effect cho nhóm nghiên cứu: mỗi chỉ số × mỗi nhóm nền kỹ thuật, chỉ tính người có đủ hai lần đo.
 * Bên dưới là từng người (mã, nhóm, trước → sau, đã dùng phần nào của app) để kiểm tra lại số tổng.
 */
const fmt = (v: number | null, digits = 2) => (v == null ? '—' : v.toFixed(digits))
const signed = (v: number | null) => (v == null ? '—' : `${v > 0 ? '+' : ''}${v.toFixed(2)}`)
const GROUPS: (Group | 'all')[] = ['all', 'non_tech', 'tech', 'unknown']
const MAINT: Record<string, string> = { reminder: "After the app's reminder", problem: "Because of a problem", other: "Another reason", no: "Not yet" }

export function GapSummary({ feedback, events }: { feedback: FeedbackRow[]; events: UsageRow[] }) {
  const ps = people(feedback, events)
  const paired = ps.filter((p) => p.before && p.after)
  const late = ps.filter((p) => p.baselineLate).length
  const owners = ps.filter((p) => p.ownPc === 'desktop')
  const maint = Object.entries(MAINT).map(([k, label]) => ({ label, count: owners.filter((p) => p.maintDone === k).length }))
  return (
    <section className="mt-6 rounded-2xl bg-panel p-4 ring-1 ring-line">
      <h2 className="text-[18px] font-semibold">Gap effect (first use → end of study)</h2>
      <p className="mt-2 text-[13px] text-ink-2">
        Each participant is compared with their own first-use answers. Only people with both answers for a measure are counted. {ps.length} participants have a first-use or end-of-study form; {paired.length} have both.
        {late > 0 && ` ${late} sent the first-use form after already using the app, so their baseline may be affected by the guidance.`}
      </p>
      {ps.length === 0 ? (
        <p className="mt-3 text-[14px] text-ink-3">No first-use or end-of-study forms yet.</p>
      ) : (
        <>
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-[12.5px] tabular-nums">
              <thead>
                <tr>{["Measure", "Group", "People", "Before", "After", "Mean change", "Median change", "Better / same / worse"].map((h) => <th key={h} className="border-b border-line px-2 py-2 font-semibold">{h}</th>)}</tr>
              </thead>
              <tbody>
                {MEASURES.map((m) => (
                  <Fragment key={m.key}>
                    {GROUPS.map((g, i) => {
                      const cell = gap(g === 'all' ? ps : ps.filter((p) => p.group === g), m.key)
                      if (g !== 'all' && cell.n === 0) return null
                      return (
                        <tr key={g} className={i === 0 ? 'font-medium' : 'text-ink-2'}>
                          <td className="border-b border-line px-2 py-2">{i === 0 ? <>{m.label}<span className="block text-[11px] font-normal text-ink-3">{m.range}{m.better === 'down' ? ', lower is better' : ''}</span></> : ''}</td>
                          <td className="border-b border-line px-2 py-2">{g === 'all' ? "Everyone" : GROUP_LABEL[g]}</td>
                          <td className="border-b border-line px-2 py-2">{cell.n}</td>
                          <td className="border-b border-line px-2 py-2">{fmt(cell.before)}</td>
                          <td className="border-b border-line px-2 py-2">{fmt(cell.after)}</td>
                          <td className="border-b border-line px-2 py-2">{signed(cell.change)}</td>
                          <td className="border-b border-line px-2 py-2">{signed(cell.median)}</td>
                          <td className="border-b border-line px-2 py-2">{cell.n ? `${cell.better} / ${cell.same} / ${cell.worse}` : '—'}</td>
                        </tr>
                      )
                    })}
                  </Fragment>
                ))}
              </tbody>
            </table>
          </div>
          <p className="mt-2 text-[12px] text-ink-3">Q4: compare the change between the two background groups, and check the “Before” column. If the technical group already starts near the top of the scale, it cannot rise much, so the comparison is not fair. There is no control group, so report changes as signs, not proof of cause.</p>

          <h3 className="mt-5 text-[14px] font-semibold">Q3: desktop owners who serviced their PC during the study</h3>
          <p className="mt-1 text-[12.5px] text-ink-2">{owners.length} participants said they have a desktop PC. Answers from the end-of-study form (the reminder funnel is in the maintenance table below).</p>
          <div className="mt-2 flex flex-wrap gap-2 text-[12.5px]">{maint.map((m) => <span key={m.label} className="rounded-full bg-fill px-3 py-1">{m.label}: <strong>{m.count}</strong></span>)}</div>

          <details className="mt-5 text-[12.5px]">
            <summary className="cursor-pointer font-medium">Each participant ({ps.length})</summary>
            <div className="mt-2 overflow-x-auto">
              <table className="w-full text-left tabular-nums">
                <thead><tr>{["Code", "Background", "Confidence", "Doing it yourself", "Decisions", "Others asked", "Used in the app", "Notes"].map((h) => <th key={h} className="border-b border-line px-2 py-2 font-semibold">{h}</th>)}</tr></thead>
                <tbody>{ps.map((p) => <PersonRow key={p.code} p={p} />)}</tbody>
              </table>
            </div>
          </details>
        </>
      )}
    </section>
  )
}

function PersonRow({ p }: { p: Person }) {
  const pair = (m: (typeof MEASURES)[number]['key'], digits = 2) => {
    const b = p.before?.[m] ?? null, a = p.after?.[m] ?? null
    return `${fmt(b, digits)} → ${fmt(a, digits)}${b != null && a != null ? ` (${signed(a - b)})` : ''}`
  }
  const u = p.usage
  const used = [u.advice && `advice ${u.advice}`, u.buildSteps && `build steps ${u.buildSteps}`, u.buildCompleted && 'build finished', u.upgrade && `upgrade ${u.upgrade}`, u.maintenanceStarted && `maintenance started ${u.maintenanceStarted}`, u.maintenanceCompleted && 'maintenance finished'].filter(Boolean).join(' · ')
  const notes = [!p.before && 'no first-use form', !p.after && 'no end-of-study form', p.baselineLate && 'first-use form sent after using the app'].filter(Boolean).join('; ')
  return (
    <tr>
      <td className="border-b border-line px-2 py-2 font-semibold">{p.code}</td>
      <td className="border-b border-line px-2 py-2">{GROUP_LABEL[p.group]}{p.ownPc === 'desktop' ? ' · desktop owner' : ''}</td>
      <td className="border-b border-line px-2 py-2">{pair('confidence')}</td>
      <td className="border-b border-line px-2 py-2">{pair('independence')}</td>
      <td className="border-b border-line px-2 py-2">{pair('decisions', 0)}</td>
      <td className="border-b border-line px-2 py-2">{pair('sources', 0)}</td>
      <td className="border-b border-line px-2 py-2">{used || 'nothing recorded'}</td>
      <td className="border-b border-line px-2 py-2 text-ink-3">{notes}</td>
    </tr>
  )
}
