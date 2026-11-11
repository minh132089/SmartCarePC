import assert from 'node:assert/strict'
import { maintenancePlan, addMonths } from '../src/data/maintenanceSchedule.ts'
import { reminderIcs, googleCalendarUrl } from '../src/data/maintenanceCalendar.ts'
const today = '2026-10-29'
const base = { lastCleaned: 'recent', lastCleanedDate: '2026-08-29', age: 'under-one', environment: [], symptoms: [], paste: 'unknown', cleanedButStillHot: false }
assert.equal(maintenancePlan({ ...base, lastCleaned: 'never', lastCleanedDate: undefined }, today).dust.due, true)
assert.equal(maintenancePlan({ ...base, environment: ['pet'] }, today).dust.months, 3)
assert.equal(maintenancePlan({ ...base, environment: ['pet'] }, today).dust.date, '2026-11-29')
assert.equal(maintenancePlan(base, today).dust.due, false)
assert.equal(maintenancePlan(base, today).dust.date, '2027-02-28')
assert.equal(maintenancePlan({ ...base, age: 'over-three', paste: 'never', symptoms: ['heat'] }, today).paste.due, true)
assert.equal(maintenancePlan({ ...base, age: 'over-three', paste: 'unknown', symptoms: ['heat'] }, today).paste.due, false)
assert.equal(maintenancePlan({ ...base, cleanedButStillHot: true }, today).paste.due, true)
// Thay keo lần cuối đã quá 30 tháng: đến hạn ngay, ngày hẹn không nằm trong quá khứ
const oldPaste = maintenancePlan({ ...base, paste: 'changed', lastPasteDate: '2023-01-01' }, '2026-11-11')
assert.equal(oldPaste.paste.due, true)
assert.ok(oldPaste.paste.date > '2026-11-11')
assert.equal(maintenancePlan({ ...base, paste: 'changed', lastPasteDate: '2026-01-01' }, '2026-11-11').paste.due, false)
assert.equal(addMonths('2026-01-31', 1), '2026-02-28')
const reminder = { task: 'dust', date: '2026-10-31', months: 3, uid: 'sample-event', link: 'https://ai.smartcarepc.pages.dev/?baotri=dust&thunghiem=P01' }
const ics = reminderIcs(reminder, new Date('2026-10-29T10:00:00Z'))
assert.ok(ics.includes('DTSTART;VALUE=DATE:20261031\r\nDTEND;VALUE=DATE:20261101'))
assert.ok(ics.includes('RRULE:FREQ=MONTHLY;INTERVAL=3'))
assert.ok(ics.includes('TRIGGER:-P1D'))
assert.ok(!ics.replaceAll('\r\n', '').includes('\n'))
assert.ok(ics.split('\r\n').every(line => Buffer.byteLength(line) <= 75))
assert.ok(ics.replaceAll('\r\n ', '').includes(reminder.link))
const google = new URL(googleCalendarUrl(reminder))
assert.equal(google.searchParams.get('dates'), '20261031/20261101')
assert.equal(google.searchParams.get('recur'), 'RRULE:FREQ=MONTHLY;INTERVAL=3')
console.log('Maintenance scheduling and calendar checks passed.')
