import assert from 'node:assert/strict'
import { createServer } from 'vite'

const memory = new Map()
globalThis.localStorage = { getItem: (k) => memory.get(k) ?? null, setItem: (k, v) => memory.set(k, v) }
const server = await createServer({ server: { middlewareMode: true, hmr: false, ws: false }, appType: 'custom' })
try {
  const { guideSteps, guideChatContext } = await server.ssrLoadModule('/src/data/guide.ts')
  const { machineOfCard } = await server.ssrLoadModule('/src/ui/chat/chatStore.ts')
  const { FAMILY_IDS } = await server.ssrLoadModule('/src/data/families.ts')
  const { useViewer } = await server.ssrLoadModule('/src/store.ts')
  const line = (category, code, name = code) => ({ category, code, name, quantity: 1 })
  const card = { items: [line('case', 'case_standard_tower'), line('cpu', 'cpu_igpu', 'CPU có iGPU')], compatible: true, errors: [], warnings: [] }
  for (const id of [...FAMILY_IDS, 'gaming']) {
    assert.equal(machineOfCard({ ...card, items: [line('case', `case_${id}`)] }), id)
  }
  assert.equal(machineOfCard({ ...card, items: [line('case', 'unknown_case')] }), null)
  useViewer.getState().setFamily('standard_tower')
  useViewer.getState().setAdvisedBuild(card)
  assert.equal(useViewer.getState().advisedBuild, card)
  const noGpu = guideSteps('standard_tower', 'gaming', false)
  assert(!noGpu.some((s) => s.id === 'gpu'))
  assert(noGpu.find((s) => s.id === 'finish').actions.some((s) => s.includes('trên bo mạch chủ')))
  const ramIndex = noGpu.findIndex((s) => s.id === 'ram')
  const ctx = guideChatContext('standard_tower', 'gaming', ramIndex, card)
  assert(ctx.includes('Không có card rời'))
  assert(ctx.includes('khe thứ 2 và thứ 4'))
  assert(ctx.includes('Thao tác chuẩn:'))
  assert(ctx.includes('CPU có iGPU'))
  assert(ctx.length <= 2000)
  assert(guideChatContext('mini_sff', 'gaming', null, null).includes('Mini-ITX chỉ có 2 khe'))
  assert(guideSteps('standard_tower', 'gaming', true).some((s) => s.id === 'gpu'))
  useViewer.getState().setFamily('mini_sff')
  assert.equal(useViewer.getState().advisedBuild, null, 'Chọn máy mẫu khác phải bỏ cấu hình tư vấn cũ')
  useViewer.getState().setAdvisedBuild(card)
  useViewer.getState().setBuild('gaming')
  assert.equal(useViewer.getState().advisedBuild, null)
  console.log('PASS: case/model mapping, selected build, iGPU steps, monitor port, RAM slots and canonical AI context.')
} finally {
  await server.close()
}
