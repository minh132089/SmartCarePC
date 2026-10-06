// Chụp app đang chạy (npm run dev, cổng 5173) bằng Chrome headless qua giao thức DevTools.
// Dùng: node tools/shot.mjs <tiền tố ảnh> [bước ...]
// Mỗi bước chụp một ảnh sau khi làm:
//   ""            không làm gì
//   "drag:dx,dy"  kéo chuột trên canvas để xoay camera
//   "wheel:dy"    cuộn để phóng to (âm) / thu nhỏ (dương)
//   còn lại       đoạn JS chạy trong trang (ví dụ bấm nút)
import { spawn } from 'node:child_process'
import { mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const [out, ...steps] = process.argv.slice(2)
const profile = mkdtempSync(join(tmpdir(), 'pcsc-shot-'))
const chrome = spawn('/Applications/Google Chrome.app/Contents/MacOS/Google Chrome', [
  '--headless=new', '--remote-debugging-port=9333', `--user-data-dir=${profile}`,
  '--window-size=1800,1100', '--hide-scrollbars', '--use-angle=metal', 'about:blank'], { stdio: 'ignore' })
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
let target
for (let i = 0; i < 50 && !target; i++) {
  await sleep(200)
  try { target = (await (await fetch('http://127.0.0.1:9333/json')).json()).find((t) => t.type === 'page') } catch {}
}
const ws = new WebSocket(target.webSocketDebuggerUrl)
await new Promise((r) => ws.addEventListener('open', r))
let id = 0
const pending = new Map()
ws.addEventListener('message', (e) => {
  const m = JSON.parse(e.data)
  if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id) }
})
const send = (method, params = {}) => new Promise((r) => { const i = ++id; pending.set(i, r); ws.send(JSON.stringify({ id: i, method, params })) })
await send('Emulation.setDeviceMetricsOverride', { width: 1800, height: 1100, deviceScaleFactor: 1, mobile: false })
await send('Page.navigate', { url: 'http://localhost:5173/' })
await sleep(9000)
const list = steps.length ? steps : ['']
for (let k = 0; k < list.length; k++) {
  const st = list[k]
  if (st.startsWith('drag:')) {
    const [dx, dy] = st.slice(5).split(',').map(Number)
    const x0 = 1100, y0 = 550
    await send('Input.dispatchMouseEvent', { type: 'mousePressed', x: x0, y: y0, button: 'left', clickCount: 1 })
    for (let i = 1; i <= 20; i++) await send('Input.dispatchMouseEvent', { type: 'mouseMoved', x: x0 + dx * i / 20, y: y0 + dy * i / 20, button: 'left', buttons: 1 })
    await send('Input.dispatchMouseEvent', { type: 'mouseReleased', x: x0 + dx, y: y0 + dy, button: 'left', clickCount: 1 })
    await sleep(1500)
  } else if (st.startsWith('wheel:')) {
    await send('Input.dispatchMouseEvent', { type: 'mouseWheel', x: 1100, y: 550, deltaX: 0, deltaY: Number(st.slice(6)) })
    await sleep(1500)
  } else if (st) {
    await send('Runtime.evaluate', { expression: st, awaitPromise: true })
    await sleep(2500)
  }
  const { result } = await send('Page.captureScreenshot', { format: 'png' })
  writeFileSync(`${out}_${k}.png`, Buffer.from(result.data, 'base64'))
  console.log('saved', `${out}_${k}.png`)
}
ws.close()
chrome.kill()
