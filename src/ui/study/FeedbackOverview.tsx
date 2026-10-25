import { type CSSProperties, type ReactNode } from 'react'
import { ChartLineUp, Star, UserCircle, ChatTeardropText } from '@phosphor-icons/react'
import MagicBento, { type MagicBentoItem } from '../../components/MagicBento'
import { useDark } from '../../theme'
import { average, dailyCounts, occupationCounts, participantOf, score, type FeedbackRow } from './feedbackStats'
import { FEEDBACK } from './questions'
import './feedback.css'

const COLORS = ['#2997ff', '#65b5ff', '#4074bf', '#8a9dbb', '#b4c5dc', '#607085']
const SCALES = FEEDBACK.filter((q) => q.type === 'scale')

function Card({ eyebrow, title, children, note }: { eyebrow?: string; title: string; children: ReactNode; note?: string }) {
  return <section className="feedback-tile">{eyebrow && <p className="feedback-eyebrow">{eyebrow}</p>}<h2>{title}</h2>{children}{note && <p className="feedback-note">{note}</p>}</section>
}

export function FeedbackOverview({ rows }: { rows: FeedbackRow[] }) {
  const dark = useDark()
  const stars = average(rows.map((r) => score(r.answers.stars)))
  const profiles = rows.filter((r) => { const p = participantOf(r); return p.name && p.age && p.occupation }).length
  const distribution = [5, 4, 3, 2, 1].map((n) => ({ label: `${n}`, count: rows.filter((r) => score(r.answers.stars) === n).length }))
  const days = dailyCounts(rows)
  const occupations = occupationCounts(rows)
  const ages = [['Dưới 18', 1, 17], ['18–24', 18, 24], ['25–34', 25, 34], ['35–44', 35, 44], ['45 trở lên', 45, 120]] as const
  const ageCounts = ages.map(([label, lo, hi]) => ({ label, count: rows.filter((r) => { const age = participantOf(r).age; return age != null && age >= lo && age <= hi }).length }))
  const items: MagicBentoItem[] = [
    { id: 'total', content: <Card eyebrow="TỔNG QUAN" title="Phản hồi đã nhận"><ChatTeardropText className="feedback-stat-icon" size={22} aria-hidden /><p className="feedback-number">{rows.length}</p><p className="feedback-note">Theo bộ lọc đang chọn</p></Card> },
    { id: 'rating', content: <Card eyebrow="TRẢI NGHIỆM" title="Điểm đánh giá"><Star className="feedback-stat-icon" size={22} aria-hidden /><p className="feedback-number">{stars ? stars.mean.toFixed(1) : '—'}<span>/5</span></p><p className="feedback-note">{stars ? `${stars.n} phản hồi có chấm sao` : 'Chưa có lượt chấm sao'}</p></Card> },
    { id: 'profiles', content: <Card eyebrow="NGƯỜI THAM GIA" title="Có thông tin hồ sơ"><UserCircle className="feedback-stat-icon" size={22} aria-hidden /><p className="feedback-number">{profiles}<span>/{rows.length}</span></p><p className="feedback-note">Phản hồi có đủ tên, tuổi, nghề nghiệp</p></Card> },
    { id: 'trend', colSpan: 2, rowSpan: 2, content: <Card eyebrow="MỨC ĐỘ THAM GIA" title="Phản hồi theo ngày" note={days.length ? `14 ngày đến ${days.at(-1)!.label}. Mỗi điểm là số phản hồi, không phải số người duy nhất.` : 'Biểu đồ xuất hiện khi có phản hồi ghi ngày gửi.'}><ChartLineUp className="feedback-stat-icon" size={22} aria-hidden /><Trend days={days} /></Card> },
    { id: 'distribution', rowSpan: 2, content: <Card eyebrow="ĐÁNH GIÁ" title="Phân bố số sao" note="Chỉ tính các phản hồi có chấm sao."><Bars data={distribution} suffix=" sao" /></Card> },
    { id: 'questions', colSpan: 2, rowSpan: 2, content: <Card eyebrow="CHẤT LƯỢNG TRẢI NGHIỆM" title="Điểm theo từng câu hỏi" note="Thang 1–5 · số trong ngoặc là lượt trả lời."><div className="feedback-question-bars">{SCALES.map((q) => {
      const a = average(rows.map((r) => score(r.answers[q.key])))
      return <div key={q.key}><div className="feedback-bar-label"><span>{q.text.replace(/^\d+\. /, '')}</span><strong>{a ? `${a.mean.toFixed(2)} (${a.n})` : '—'}</strong></div><div className="feedback-bar-track"><span style={{ transform: `scaleX(${a ? a.mean / 5 : 0})` }} /></div></div>
    })}</div></Card> },
    { id: 'occupations', rowSpan: 2, content: <Card eyebrow="HỒ SƠ" title="Nghề nghiệp" note="Phân bố trong phản hồi có thông tin nghề nghiệp."><Occupations data={occupations} /></Card> },
    { id: 'ages', colSpan: 3, rowSpan: 1, content: <Card eyebrow="HỒ SƠ" title="Nhóm tuổi"><div className="feedback-ages">{ageCounts.map((d) => <div key={d.label}><strong>{d.count}</strong><span>{d.label}</span></div>)}</div></Card> },
  ]

  return <MagicBento className="feedback-bento" items={items} columns={3} rowHeight={192} gap={16} radius={24} theme={dark ? 'dark' : 'light'} glowColor="#2997ff" enableStars={false} enableTilt={false} enableMagnetism={false} clickEffect textAutoHide={false} intro={false} style={{ '--mb-surface': 'var(--color-surface)', '--mb-ink': 'var(--color-ink)', '--mb-muted': 'var(--color-ink-2)', '--mb-shadow': '0 6px 24px -20px rgb(0 0 0 / .18)' } as CSSProperties} />
}

function Bars({ data, suffix = '' }: { data: { label: string; count: number }[]; suffix?: string }) {
  const max = Math.max(1, ...data.map((d) => d.count))
  return <div className="feedback-bars">{data.map((d) => <div key={d.label}><div className="feedback-bar-label"><span>{d.label}{suffix}</span><strong>{d.count}</strong></div><div className="feedback-bar-track"><span style={{ transform: `scaleX(${d.count / max})` }} /></div></div>)}</div>
}

function Trend({ days }: { days: ReturnType<typeof dailyCounts> }) {
  if (!days.length) return <div className="feedback-chart-empty">Chưa có dữ liệu theo ngày</div>
  const max = Math.max(1, ...days.map((d) => d.count))
  const points = days.map((d, i) => `${36 + i * 35},${160 - d.count / max * 130}`).join(' ')
  return <figure className="feedback-trend"><svg viewBox="0 0 515 205" role="img" aria-label={`Số phản hồi trong 14 ngày, tổng ${days.reduce((n, d) => n + d.count, 0)} phản hồi`}>
    {[0, 1, 2].map((n) => <g key={n}><line x1="36" x2="491" y1={30 + n * 65} y2={30 + n * 65} stroke="currentColor" opacity=".1" /><text x="25" y={34 + n * 65} textAnchor="end">{Number((max * (1 - n / 2)).toFixed(1))}</text></g>)}
    <polygon points={`36,160 ${points} 491,160`} fill="#2997ff" opacity=".1" /><polyline points={points} fill="none" stroke="#2997ff" strokeWidth="3" strokeLinejoin="round" />
    {days.map((d, i) => <g key={d.day}><circle cx={36 + i * 35} cy={160 - d.count / max * 130} r="4" fill="#2997ff"><title>{d.day}: {d.count} phản hồi</title></circle>{[0, 4, 8, 13].includes(i) && <text x={36 + i * 35} y="190" textAnchor="middle">{d.label}</text>}</g>)}
  </svg><figcaption className="sr-only">{days.map((d) => `${d.day}: ${d.count}`).join('; ')}</figcaption></figure>
}

function Occupations({ data }: { data: ReturnType<typeof occupationCounts> }) {
  const total = data.reduce((n, d) => n + d.count, 0)
  if (!total) return <div className="feedback-chart-empty">Chưa có thông tin nghề nghiệp</div>
  const circumference = 2 * Math.PI * 42
  return <div className="feedback-occupations"><svg viewBox="0 0 120 120" role="img" aria-label={`${total} phản hồi có nghề nghiệp`}>
    {data.map((d, i) => { const length = d.count / total * circumference; const start = data.slice(0, i).reduce((n, item) => n + item.count, 0) / total * circumference; return <circle key={d.label} cx="60" cy="60" r="42" fill="none" stroke={COLORS[i]} strokeWidth="13" strokeDasharray={`${length} ${circumference - length}`} strokeDashoffset={-start} transform="rotate(-90 60 60)"><title>{d.label}: {d.count}</title></circle> })}
    <text x="60" y="61" textAnchor="middle" className="feedback-donut-total">{total}</text><text x="60" y="76" textAnchor="middle">phản hồi</text>
  </svg><ul>{data.map((d, i) => <li key={d.label}><i style={{ background: COLORS[i] }} aria-hidden /><span>{d.label}</span><strong>{d.count}</strong></li>)}</ul></div>
}
