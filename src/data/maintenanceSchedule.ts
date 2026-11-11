export type MaintenanceTask = 'dust' | 'paste' | 'check'
export type Cooler = 'air' | 'lowprofile' | 'aio' | 'unknown'
export const TASKS: Record<MaintenanceTask, { title: string; hint: string; months: number }> = {
  dust: { title: 'Clean out dust', hint: 'Every 3–6 months · open the case', months: 6 },
  paste: { title: 'Replace thermal paste', hint: 'Every 2–3 years, or when needed', months: 30 },
  check: { title: 'Quick health check', hint: 'Every month · 5 minutes, case closed', months: 1 },
}
export interface MaintenanceAnswers {
  lastCleaned: 'never' | 'unknown' | 'recent' | 'six-twelve' | 'over-year'
  lastCleanedDate?: string
  age: 'under-one' | 'one-two' | 'two-three' | 'over-three'
  environment: string[]
  symptoms: string[]
  paste: 'never' | 'changed' | 'unknown'
  lastPasteDate?: string
  cleanedButStillHot: boolean
}
export const EMPTY_ANSWERS: MaintenanceAnswers = { lastCleaned: 'unknown', age: 'under-one', environment: [], symptoms: [], paste: 'unknown', cleanedButStillHot: false }
export const day = (date = new Date()) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
export const fromDay = (date: string) => new Date(`${date}T12:00:00`)
export function addDays(date: string, n: number) { const d = fromDay(date); d.setDate(d.getDate() + n); return day(d) }
export function addMonths(date: string, n: number) {
  const d = fromDay(date), original = d.getDate()
  d.setDate(1); d.setMonth(d.getMonth() + n)
  d.setDate(Math.min(original, new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate()))
  return day(d)
}
export function nextWeekend(today: string) {
  const weekday = fromDay(today).getDay()
  return addDays(today, weekday === 6 ? 1 : weekday === 0 ? 6 : 6 - weekday)
}
export interface Recommendation { due: boolean; date: string; months: number; reason: string }
/** Luật tính hạn dùng ngày địa phương; không đưa hạn về sớm chỉ để phục vụ thử nghiệm. */
export function maintenancePlan(a: MaintenanceAnswers, today = day()): Record<MaintenanceTask, Recommendation> {
  const months = a.environment.length ? 3 : 6
  const never = a.lastCleaned === 'never' || a.lastCleaned === 'unknown'
  const last = a.lastCleanedDate || addMonths(today, a.lastCleaned === 'recent' ? -6 : a.lastCleaned === 'six-twelve' ? -12 : -18)
  const dustDate = addMonths(last, months)
  const dustDue = never || dustDate <= today || a.symptoms.length > 0
  // Lần thay keo gần nhất đã quá 30 tháng thì đến hạn ngay (không hẹn vào một ngày đã qua)
  const pasteReview = addMonths(a.lastPasteDate || today, 30)
  const pasteOverdue = a.paste === 'changed' && !!a.lastPasteDate && pasteReview <= today
  const pasteDue = ((a.age === 'two-three' || a.age === 'over-three') && a.paste === 'never') || a.cleanedButStillHot || pasteOverdue
  return {
    dust: { due: dustDue, date: dustDue ? nextWeekend(today) : dustDate, months,
      reason: `${never ? 'The last clean is unknown or your PC has never been cleaned.' : a.symptoms.length ? 'You reported a change in how your PC runs.' : dustDue ? 'Your last clean is outside the suggested interval.' : 'Your last clean is still within the suggested interval.'} ${months === 3 ? 'Your environment calls for cleaning every 3 months.' : 'Plan to clean every 6 months.'}` },
    paste: { due: pasteDue, date: pasteDue ? nextWeekend(today) : pasteReview, months: 30,
      reason: pasteDue ? a.cleanedButStillHot ? 'Heat or shutdowns continue after dust cleaning. Check cooling contact and consider fresh paste.' : pasteOverdue ? 'The paste was last replaced more than 2½ years ago.' : 'Your PC is over 2 years old and its paste has never been replaced.' : a.paste === 'unknown' ? 'Paste history is unknown. Check temperatures first; age alone does not confirm a paste problem.' : 'There is no current reason to replace paste early. Review it in 2–3 years.' },
    check: { due: a.symptoms.length > 0, date: a.symptoms.length ? nextWeekend(today) : addMonths(today, 1), months: 1, reason: 'A short monthly check helps you notice new noise, dust or instability.' },
  }
}

