export type FeedbackRow = {
  id: number
  createdAt: string | null
  participantCode: string | null
  phase: string | null
  machine: string | null
  answers: Record<string, unknown>
}

export function score(v: unknown) {
  const n = typeof v === 'number' ? v : typeof v === 'string' && v.trim() ? Number(v) : NaN
  return Number.isFinite(n) && n >= 1 && n <= 5 ? n : null
}

export function average(values: (number | null)[]) {
  const xs = values.filter((v): v is number => v != null)
  return xs.length ? { mean: xs.reduce((a, b) => a + b, 0) / xs.length, n: xs.length } : null
}

/** English display labels for the old form presets; original feedback records stay unchanged. */
export function occupationLabel(value: string): string {
  const legacy: Record<string, string> = {
    'học sinh': 'School student', 'sinh viên': 'University student',
    'nhân viên văn phòng': 'Office worker', 'công nghệ thông tin': 'Information technology',
    'thiết kế / sáng tạo': 'Design / creative work', 'giáo viên / giảng viên': 'Teacher / lecturer',
    'kinh doanh / tự do': 'Business / self-employed', 'chưa đi làm': 'Not currently employed',
  }
  return legacy[value.trim().toLocaleLowerCase('en')] ?? value.trim()
}

export function participantOf(row: FeedbackRow) {
  const a = row.answers
  return {
    name: typeof a.participant_name === 'string' ? a.participant_name.trim() : '',
    age: typeof a.participant_age === 'number' && Number.isInteger(a.participant_age) && a.participant_age > 0 && a.participant_age <= 120 ? a.participant_age : null,
    occupation: typeof a.participant_occupation === 'string' ? occupationLabel(a.participant_occupation) : '',
  }
}

export function occupationCounts(rows: FeedbackRow[]) {
  const counts = new Map<string, { label: string; count: number }>()
  for (const row of rows) {
    const label = participantOf(row).occupation
    if (!label) continue
    const key = label.toLocaleLowerCase('en').replace(/\s+/g, ' ')
    const item = counts.get(key)
    if (item) item.count++
    else counts.set(key, { label, count: 1 })
  }
  const all = [...counts.values()].sort((a, b) => b.count - a.count || a.label.localeCompare(b.label, 'en'))
  return all.length > 5 ? [...all.slice(0, 5), { label: "Other occupations", count: all.slice(5).reduce((n, r) => n + r.count, 0) }] : all
}

/** 14 ngày lịch kết thúc ở ngày gửi mới nhất, bao gồm cả ngày không có phản hồi. */
export function dailyCounts(rows: FeedbackRow[]) {
  const counts = new Map<string, number>()
  for (const row of rows) {
    const day = row.createdAt?.slice(0, 10)
    if (!day || !/^\d{4}-\d{2}-\d{2}$/.test(day)) continue
    const stamp = Date.parse(day + 'T00:00:00Z')
    if (!Number.isFinite(stamp) || new Date(stamp).toISOString().slice(0, 10) !== day) continue
    counts.set(day, (counts.get(day) ?? 0) + 1)
  }
  const last = [...counts.keys()].sort().at(-1)
  if (!last) return []
  const end = Date.parse(last + 'T00:00:00Z')
  return Array.from({ length: 14 }, (_, i) => {
    const day = new Date(end - (13 - i) * 86_400_000).toISOString().slice(0, 10)
    return { day, label: `${day.slice(8)}/${day.slice(5, 7)}`, count: counts.get(day) ?? 0 }
  })
}
