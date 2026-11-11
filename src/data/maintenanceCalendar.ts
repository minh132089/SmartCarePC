import { addDays, TASKS, type MaintenanceTask } from './maintenanceSchedule.ts'

export interface CalendarReminder { task: MaintenanceTask; date: string; months: number; link: string; uid: string }
const compact = (s: string) => s.replaceAll('-', '')
const escape = (s: string) => s.replaceAll('\\', '\\\\').replaceAll('\n', '\\n').replaceAll(',', '\\,').replaceAll(';', '\\;')
/** RFC 5545: CRLF và gấp dòng theo số byte UTF-8, không tách đôi ký tự. */
function fold(line: string) {
  const lines: string[] = []; let current = '', bytes = 0
  for (const c of line) {
    const n = new TextEncoder().encode(c).length
    if (bytes + n > 75) { lines.push(current); current = ' '; bytes = 1 }
    current += c; bytes += n
  }
  return [...lines, current].join('\r\n')
}
export function reminderIcs(r: CalendarReminder, now = new Date()) {
  return [
    'BEGIN:VCALENDAR', 'VERSION:2.0', 'PRODID:-//SmartCare PC//Maintenance//EN', 'CALSCALE:GREGORIAN',
    'BEGIN:VEVENT', `UID:${r.uid}@smartcarepc`, `DTSTAMP:${now.toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, '')}`,
    `DTSTART;VALUE=DATE:${compact(r.date)}`, `DTEND;VALUE=DATE:${compact(addDays(r.date, 1))}`,
    `RRULE:FREQ=MONTHLY;INTERVAL=${r.months}`, `SUMMARY:${escape(TASKS[r.task].title)}`,
    `DESCRIPTION:${escape('Open your step-by-step desktop PC guide:\n' + r.link)}`, `URL:${r.link}`,
    'BEGIN:VALARM', 'TRIGGER:-P1D', 'ACTION:DISPLAY', `DESCRIPTION:${escape(TASKS[r.task].title)}`, 'END:VALARM',
    'END:VEVENT', 'END:VCALENDAR', '',
  ].map(fold).join('\r\n')
}
export function googleCalendarUrl(r: CalendarReminder) {
  const q = new URLSearchParams({ action: 'TEMPLATE', text: TASKS[r.task].title,
    dates: `${compact(r.date)}/${compact(addDays(r.date, 1))}`, details: `Open your step-by-step desktop PC guide: ${r.link}`,
    recur: `RRULE:FREQ=MONTHLY;INTERVAL=${r.months}` })
  return `https://calendar.google.com/calendar/render?${q}`
}
