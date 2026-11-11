import assert from 'node:assert/strict'
import { createServer } from 'vite'
const memory = new Map()
globalThis.localStorage = { getItem: k => memory.get(k) ?? null, setItem: (k, v) => memory.set(k, v), removeItem: k => memory.delete(k) }
let requests = [], online = true
const tick = () => new Promise(r => setTimeout(r, 20))
globalThis.fetch = async (url, init) => {
  requests.push({ url, ...JSON.parse(init.body) })
  if (!online) throw new Error('offline')
  return new Response('{}', { status: 201 })
}
const server = await createServer({ server: { middlewareMode: true, hmr: false, ws: false }, appType: 'custom' })
try {
  const { useStudy } = await server.ssrLoadModule('/src/ui/study/study.ts')
  const { useViewer } = await server.ssrLoadModule('/src/store.ts')
  const m = await server.ssrLoadModule('/src/ui/maintenance/maintenanceState.ts')
  const { maintenanceSteps, maintenanceContext } = await server.ssrLoadModule('/src/data/maintenance.ts')
  const { loadDone, saveDone } = await server.ssrLoadModule('/src/data/guide.ts')
  const { maintenanceGroups } = await server.ssrLoadModule('/src/ui/maintenance/maintenanceStats.ts')
  const pc = { machine: 'mini_sff', cooler: 'lowprofile', hasGpu: false }
  m.startMaintenance(pc, 'dust'); m.recordStep('safe', true); await tick()
  m.useMaintenance.getState().snooze('dust')
  const { addDays, day } = await server.ssrLoadModule('/src/data/maintenance.ts')
  assert.equal(m.useMaintenance.getState().dates.dust, addDays(day(), 7))
  assert.equal(requests.length, 0, 'No maintenance network traffic outside study mode')
  for (const machine of ['standard_tower', 'compact_tower', 'showcase_dual_chamber', 'mini_sff', 'full_tower', null]) {
    for (const task of ['dust', 'paste', 'check']) {
      const steps = maintenanceSteps(task, machine, 'gaming', true)
      assert(steps.length >= 3); assert.equal(new Set(steps.map(s => s.id)).size, steps.length)
      if (task === 'check') assert(steps.every(s => !s.panelOpen && !s.coolerOut))
      assert(steps.every(s => s.actions.length && s.why && s.check))
      assert(steps.every((s, i) => maintenanceContext(steps, i, machine ?? 'gaming', task).length <= 2000))
    }
  }
  assert(!maintenanceSteps('dust', 'mini_sff', 'gaming', false).some(s => s.id === 'gpu'))
  assert(maintenanceSteps('paste', null, 'gaming', true, 'aio').some(s => s.actions.some(a => a.includes('Leave the radiator installed'))))
  useStudy.getState().join('P01', 'before'); await tick(); requests = []
  online = false
  m.startMaintenance(pc, 'dust'); m.recordStep('safe', false); await tick()
  const queued = JSON.parse(memory.get('pcsc-maintenance-queue-v1'))
  assert.equal(queued.length, 2); assert.equal(queued[1].data.alone, false)
  assert(queued.every(e => e.participantCode === 'P01' && e.phase === 'before'))
  const ids = queued.map(e => e.eventId)
  useStudy.getState().leave(); const count = requests.length
  await m.flushMaintenanceQueue(); assert.equal(requests.length, count, 'Do not retry study data outside study mode')
  online = true; useStudy.getState().join('P01', 'before'); await tick(); await m.flushMaintenanceQueue(); await tick()
  assert.deepEqual(JSON.parse(memory.get('pcsc-maintenance-queue-v1')), [])
  assert(ids.every(id => requests.some(r => r.eventId === id)))
  useViewer.getState().setGuide(false); await tick()
  assert(requests.some(r => r.eventType === 'maintenance_abandoned'))
  m.startMaintenance(pc, 'check'); m.recordStep('noise', true)
  m.useMaintenance.getState().finish('check', 'with-help', false, ['heat']); await tick()
  assert(requests.some(r => r.eventType === 'maintenance_completed' && r.data.who === 'with-help'), JSON.stringify({requests, queue: memory.get('pcsc-maintenance-queue-v1')}))
  const completedRun = m.useMaintenance.getState().run.id
  saveDone(m.maintenanceKey(pc, 'check'), new Set(['noise']))
  const before = requests.length
  useViewer.getState().setGuide(false); await tick()
  assert.equal(requests.length, before, 'Completed work must not be marked abandoned')
  const rows = requests.filter(r => r.eventType !== undefined).map((r, i) => ({ ...r, id: i + 1 }))
  const groups = maintenanceGroups(rows, [])
  assert.equal(groups.length, 1); assert.equal(groups[0].participants, 1); assert.equal(groups[0].completed, 1); assert.equal(groups[0].abandoned, 1)
  const paired = maintenanceGroups([{ ...rows[0], id: 1, eventType: 'reminder_set', phase: 'before', data: { never_serviced: true } }, { ...rows[0], id: 2, eventType: 'reminder_set', phase: 'after', data: { never_serviced: false } }], [])
  assert.equal(paired.length, 2); assert(paired.every(g => g.label.includes('Never serviced / unsure')), 'Keep the original never-serviced cohort after four weeks')
  m.startMaintenance(pc, 'check'); await tick()
  assert.notEqual(m.useMaintenance.getState().run.id, completedRun)
  assert.equal(useViewer.getState().maintenanceRunId, m.useMaintenance.getState().run.id)
  assert.equal(loadDone(m.maintenanceKey(pc, 'check')).size, 0, 'A new maintenance visit must start a fresh checklist')
  useStudy.getState().leave(); requests = []
  const workingStorage = globalThis.localStorage
  globalThis.localStorage = { getItem() { throw new Error('blocked') }, setItem() { throw new Error('blocked') } }
  m.startMaintenance(pc, 'dust'); m.recordStep('safe', true); await tick()
  assert.equal(m.useMaintenance.getState().storageWarning, true)
  assert.equal(m.useMaintenance.getState().run.alone.safe, true)
  assert.equal(requests.length, 0)
  globalThis.localStorage = workingStorage
  console.log('Maintenance guide, research-only events, offline replay, independence and abandonment checks passed.')
} finally { await server.close() }
