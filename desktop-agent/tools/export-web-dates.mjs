// Chạy đúng hàm addDays / addMonths của web (src/data/maintenanceSchedule.ts) để app so kết quả, không viết luật thứ hai khác đi.
// Chạy: node desktop-agent/tools/export-web-dates.mjs  (Node 22.6+ đọc thẳng file .ts)
import { writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { addDays, addMonths } from '../../src/data/maintenanceSchedule.ts'

const starts = ['2026-01-31', '2026-02-28', '2028-02-29', '2026-03-31', '2026-05-31', '2026-08-31', '2026-10-30', '2026-11-28', '2026-12-31', '2027-06-15', '2024-12-01']
const months = [-18, -12, -6, 1, 3, 6, 30]
const days = [7, -7, 1, 31, 366]
const cases = []
for (const s of starts) {
  for (const n of months) cases.push({ fn: 'addMonths', day: s, n, result: addMonths(s, n) })
  for (const n of days) cases.push({ fn: 'addDays', day: s, n, result: addDays(s, n) })
}
const out = fileURLToPath(new URL('../tests/SmartCarePC.Helper.Tests/fixtures/web-dates.json', import.meta.url))
writeFileSync(out, JSON.stringify(cases, null, 1) + '\n')
console.log(`${cases.length} cases -> ${out}`)
