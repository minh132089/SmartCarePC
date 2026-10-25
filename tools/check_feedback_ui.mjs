import assert from 'node:assert/strict'
import { createServer } from 'vite'

// Bộ nhớ riêng cho kiểm tra; không ghi vào hồ sơ trình duyệt hoặc database thật.
const memory = new Map()
globalThis.localStorage = {
  getItem: (k) => memory.get(k) ?? null,
  setItem: (k, v) => memory.set(k, v),
  removeItem: (k) => memory.delete(k),
}
const server = await createServer({ server: { middlewareMode: true, hmr: false, ws: false }, appType: 'custom' })
try {
  const { validParticipant, needsParticipant, useParticipant } = await server.ssrLoadModule('/src/ui/study/participant.ts')
  const { score, dailyCounts, occupationCounts } = await server.ssrLoadModule('/src/ui/study/feedbackStats.ts')
  const { sendFeedback, flushQueue } = await server.ssrLoadModule('/src/ui/study/study.ts')
  const profile = { name: 'Nguyễn Minh', age: 21, occupation: 'Sinh viên' }
  assert(validParticipant(profile))
  for (const p of [null, {}, { ...profile, age: 0 }, { ...profile, age: 121 }, { ...profile, age: 20.5 }, { ...profile, age: '21' }, { ...profile, name: ' ' }, { ...profile, occupation: '' }]) assert(!validParticipant(p))
  for (const host of ['smartcarepc.pages.dev', 'preview.smartcarepc.pages.dev']) assert(needsParticipant(host))
  for (const host of ['localhost', '127.0.0.1', '192.168.1.5', 'pages.dev.attacker.test']) assert(!needsParticipant(host))
  assert.equal(score(''), null)
  assert.equal(score('5'), 5)
  assert.equal(score(6), null)
  const row = (id, createdAt, occupation) => ({ id, createdAt, participantCode: null, phase: null, machine: null, answers: occupation ? { participant_occupation: occupation } : {} })
  const rows = [row(1, '2026-10-23 09:00:00', 'Sinh viên'), row(2, '2026-10-25 10:00:00', 'sinh viên'), row(3, 'bad', null), row(4, '2026-02-31 00:00:00', null)]
  assert.deepEqual(occupationCounts(rows), [{ label: 'Sinh viên', count: 2 }])
  const days = dailyCounts(rows)
  assert.equal(days.length, 14)
  assert.equal(days.at(-1).count, 1)
  assert.equal(days.at(-2).count, 0)
  assert.equal(days.reduce((n, d) => n + d.count, 0), 2)
  assert.deepEqual(dailyCounts([]), [])
  let sent
  globalThis.fetch = async (_url, init) => { sent = JSON.parse(init.body); return new Response('{}', { status: 201 }) }
  useParticipant.getState().save(profile)
  assert.equal(await sendFeedback({ answers: { stars: 5 }, machine: 'gaming' }, 'profile-check-1234'), 'sent')
  assert.equal(sent.answers.participant_name, profile.name)
  assert.equal(sent.answers.participant_age, 21)
  assert.equal(sent.answers.participant_occupation, 'Sinh viên')
  assert.equal(sent.answers.submission_id, 'profile-check-1234')
  globalThis.fetch = async () => new Response('{}', { status: 400 })
  assert.equal(await sendFeedback({ answers: { stars: 4 } }, 'invalid-check-1234'), 'failed')
  assert(!memory.has('pcsc-feedback-queue'))
  globalThis.fetch = async () => { throw new Error('offline') }
  assert.equal(await sendFeedback({ answers: { stars: 4 } }, 'offline-check-1234'), 'queued')
  useParticipant.getState().save({ name: 'Người khác', age: 30, occupation: 'Giáo viên' })
  globalThis.fetch = async (_url, init) => { sent = JSON.parse(init.body); return new Response('{}', { status: 201 }) }
  await flushQueue()
  assert.equal(sent.answers.participant_name, profile.name, 'Hàng đợi phải giữ đúng hồ sơ tại lúc gửi')
  assert.equal(sent.answers.submission_id, 'offline-check-1234')
  assert(!memory.has('pcsc-feedback-queue'))
  useParticipant.setState({ profile: null })
  assert.equal(await sendFeedback({ answers: { stars: 3 } }, 'local-check-1234'), 'sent')
  assert(!('participant_name' in sent.answers), 'Local và dữ liệu cũ được gửi không cần hồ sơ')
  console.log('PASS: Cloudflare/local gate, profile validation, chart aggregation, feedback payload, offline queue, invalid data and legacy feedback.')
} finally {
  await server.close()
}
