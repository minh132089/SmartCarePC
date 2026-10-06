// Kiểm tra linh kiện / dây lòi ra ngoài vỏ case trong app đang chạy (npm run dev, cổng 5173).
// Dùng: node tools/check_overflow.mjs   (in mỗi máy: mesh nào vượt quá hộp bao của vỏ bao nhiêu mm)
import { spawn } from 'node:child_process'
import { mkdtempSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const profile = mkdtempSync(join(tmpdir(), 'pcsc-check-'))
const chrome = spawn('/Applications/Google Chrome.app/Contents/MacOS/Google Chrome', [
  '--headless=new', '--remote-debugging-port=9334', `--user-data-dir=${profile}`, '--window-size=1600,1000', '--use-angle=metal', 'about:blank'], { stdio: 'ignore' })
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
let target
for (let i = 0; i < 50 && !target; i++) {
  await sleep(200)
  try { target = (await (await fetch('http://127.0.0.1:9334/json')).json()).find((t) => t.type === 'page') } catch {}
}
const ws = new WebSocket(target.webSocketDebuggerUrl)
await new Promise((r) => ws.addEventListener('open', r))
let id = 0
const pending = new Map()
ws.addEventListener('message', (e) => { const m = JSON.parse(e.data); if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id) } })
const send = (method, params = {}) => new Promise((r) => { const i = ++id; pending.set(i, r); ws.send(JSON.stringify({ id: i, method, params })) })
await send('Page.navigate', { url: 'http://localhost:5173/' })
await sleep(8000)
const MACHINES = { standard_tower: 'Tower tiêu chuẩn', compact_tower: 'Tower nhỏ gọn', showcase_dual_chamber: 'Showcase', mini_sff: 'Mini / SFF', full_tower: 'Tower lớn', gaming: 'Gaming' }
const probe = `(() => {
  const sc = window.__scene; sc.updateMatrixWorld(true)
  const shown = (o) => { for (let p = o; p; p = p.parent) if (p.visible === false) return false; return true }
  const box = (obj) => { const mn = [1e9, 1e9, 1e9], mx = [-1e9, -1e9, -1e9]
    obj.traverse((o) => { if (!o.isMesh || !shown(o)) return; const g = o.geometry; if (!g.boundingBox) g.computeBoundingBox(); const bb = g.boundingBox, e = o.matrixWorld.elements
      for (const x of [bb.min.x, bb.max.x]) for (const y of [bb.min.y, bb.max.y]) for (const z of [bb.min.z, bb.max.z]) {
        const w = [e[0]*x+e[4]*y+e[8]*z+e[12], e[1]*x+e[5]*y+e[9]*z+e[13], e[2]*x+e[6]*y+e[10]*z+e[14]]
        for (let i = 0; i < 3; i++) { mn[i] = Math.min(mn[i], w[i]); mx[i] = Math.max(mx[i], w[i]) } } }); return [mn, mx] }
  const [cmn, cmx] = box(sc.getObjectByName('mount_case'))
  const res = []
  sc.traverse((o) => { if (!o.isMesh || !shown(o)) return
    let mount = null; for (let p = o; p; p = p.parent) if (p.name && p.name.startsWith('mount_')) { mount = p.name; break }
    if (!mount || mount === 'mount_case' || mount === 'mount_tools') return
    let [mn, mx] = box(o)
    // hộp bao của vật đang xoay (cánh quạt) phình ra: tính lại theo từng đỉnh cho chính xác
    if (mn.some((v, i) => v < cmn[i]) || mx.some((v, i) => v > cmx[i])) {
      const pos = o.geometry.attributes.position, e = o.matrixWorld.elements
      mn = [1e9, 1e9, 1e9]; mx = [-1e9, -1e9, -1e9]
      for (let k = 0; k < pos.count; k++) { const x = pos.getX(k), y = pos.getY(k), z = pos.getZ(k)
        const w = [e[0]*x+e[4]*y+e[8]*z+e[12], e[1]*x+e[5]*y+e[9]*z+e[13], e[2]*x+e[6]*y+e[10]*z+e[14]]
        if (w[1] > mx[1]) o.userData.peak = w.map((v) => Math.round(v * 100))
        for (let i = 0; i < 3; i++) { mn[i] = Math.min(mn[i], w[i]); mx[i] = Math.max(mx[i], w[i]) } }
    }
    const over = [cmn[0]-mn[0], mx[0]-cmx[0], cmn[1]-mn[1], mx[1]-cmx[1], cmn[2]-mn[2], mx[2]-cmx[2]].map((v) => Math.round(v * 1000) / 10)
    const worst = Math.max(...over)
    // chân ăng-ten bo và công tắc nguồn nhô qua lưng vài mm là đúng thiết kế, chỉ báo khi > 5 mm ở lưng
    if (worst > (over.indexOf(worst) === 0 ? 5 : 0.5)) res.push(mount.slice(6) + ' ' + (o.name || o.geometry.type) + ' vượt ' + worst + ' mm ở ' + ['lưng(-x)','trước(+x)','đáy(-y)','nóc(+y)','hông kín(-z)','hông mở(+z)'][over.indexOf(worst)] + ' [' + mn.map(v=>Math.round(v*100)) + ' … ' + mx.map(v=>Math.round(v*100)) + ']' + (o.userData.peak ? ' đỉnh ' + o.userData.peak : ''))
  })
  return JSON.stringify({ case: [cmn.map((v) => Math.round(v * 100)), cmx.map((v) => Math.round(v * 100))], over: res })
})()`
for (const [key, label] of Object.entries(MACHINES)) {
  await send('Runtime.evaluate', { expression: `[...document.querySelectorAll('button')].find(b=>b.textContent.includes(${JSON.stringify(label)})).click()` })
  await sleep(6000)
  const { result } = await send('Runtime.evaluate', { expression: probe, returnByValue: true })
  const r = JSON.parse(result.result.value ?? '{"over":["(lỗi đọc cảnh)"]}')
  console.log(`== ${key} vỏ ${JSON.stringify(r.case)}`)
  for (const l of r.over) console.log('  ' + l)
}
ws.close()
chrome.kill()
