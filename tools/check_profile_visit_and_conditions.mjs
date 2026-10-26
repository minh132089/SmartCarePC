import assert from 'node:assert/strict'
import { createServer } from 'vite'
import { createElement } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'

const profile = { name: 'Demo user', age: 24, occupation: 'Student' }
const memory = new Map([['pcsc-participant-v1', JSON.stringify(profile)]])
globalThis.localStorage = {
  getItem: (key) => memory.get(key) ?? null,
  setItem: (key, value) => memory.set(key, value),
  removeItem: (key) => memory.delete(key),
}
const server = await createServer({ server: { middlewareMode: true, hmr: false, ws: false }, appType: 'custom' })
try {
  const { useParticipant, needsParticipant } = await server.ssrLoadModule('/src/ui/study/participant.ts')
  const { conditionSummary } = await server.ssrLoadModule('/src/ui/chat/conditionSummary.ts')
  const { ProposalView } = await server.ssrLoadModule('/src/ui/chat/Proposal.tsx')
  const initial = useParticipant.getState()
  assert.deepEqual(initial.profile, profile, 'A returning visitor keeps the saved profile')
  assert.equal(initial.confirmedThisVisit, false, 'Saved details do not bypass confirmation on a new page load')
  initial.save({ ...profile, age: 200 })
  assert.equal(useParticipant.getState().confirmedThisVisit, false, 'Invalid details cannot confirm a visit')
  initial.save(profile)
  assert.equal(useParticipant.getState().confirmedThisVisit, true)
  assert(!('confirmedThisVisit' in JSON.parse(memory.get('pcsc-participant-v1'))), 'Visit confirmation must not persist across reloads')
  useParticipant.getState().edit()
  assert.equal(useParticipant.getState().editing, true, 'Manual editing remains available after confirmation')
  assert(needsParticipant('42df1a96.smartcarepc.pages.dev'))
  assert(!needsParticipant('127.0.0.1'))

  const unknowns = [
    { rule: 'CPU_SUPPORT_LIST', message: 'Chưa đối chiếu CPU Support List và BIOS tối thiểu của đúng mã bo mạch. Tra trang hỗ trợ CPU trước khi mua.' },
    { rule: 'COOLER_MOUNT', message: 'Chưa xác minh tản nhiệt có bộ gá cho socket LGA1700 (gá Intel và AMD khác nhau).' },
    { rule: 'GPU_CLEARANCE', message: 'Chưa đối chiếu chiều dài, bề rộng và độ dày của card với khoảng trống GPU trong vỏ.' },
  ]
  assert.equal(conditionSummary(unknowns[1].message, 'COOLER_MOUNT'), 'Kiểm tra tản có gá cho socket LGA1700.')
  assert.equal(conditionSummary('The cooler mounting kit for AM5 is not verified.', 'COOLER_MOUNT'), 'Confirm the cooler includes a mounting kit for AM5.')
  assert.equal(conditionSummary('Unfamiliar hardware requirement.', 'NEW_RULE'), 'Unfamiliar hardware requirement.')
  assert.equal(conditionSummary('Ask the store to confirm before buying: Check an unusual connector.'), 'Check an unusual connector.')
  const blocker = 'This GPU is too long for the selected case.'
  const conditions = unknowns.map((u) => 'Hỏi cửa hàng xác nhận trước khi mua: ' + u.message)
  const markup = renderToStaticMarkup(createElement(ProposalView, { card: {
    items: [], compatible: false, errors: [], warnings: [], unknowns,
    verdict: { status: 'BLOCKED', technical: 'BLOCKED', purchase: 'READY', blockers: [blocker], conditions },
  } }))
  const [collapsed, details] = markup.split('<details')
  assert(collapsed.includes(blocker), 'Blocking issues must stay fully visible')
  assert(collapsed.includes('Xác nhận bo mạch hỗ trợ CPU và phiên bản BIOS.'))
  assert(collapsed.includes('Kiểm tra tản có gá cho socket LGA1700.'))
  assert(collapsed.includes('Kiểm tra card và đầu cáp nguồn có vừa vỏ.'))
  assert(!collapsed.includes('CPU Support List'), 'Long checks are hidden from the compact summary')
  assert(!/^[^>]*\bopen\b/.test(details), 'Full details are closed by default')
  for (const condition of conditions) assert(details.includes(condition), 'Every original check remains available')
  console.log('PASS: returning profiles, per-page confirmation, validation, short checks, full details and visible blockers.')
} finally {
  await server.close()
}
