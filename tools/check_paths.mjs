// Kiểm tra nguyên tắc cứng của hoạt cảnh lắp máy: linh kiện không được đi xuyên qua nhau (src/scene/pathCheck.ts).
// Cần dev server đang chạy (npm run dev, cổng 5173). Dùng: npm run check:paths [-- mini_sff gaming ...]
// In từng vi phạm; còn vi phạm thì thoát mã 1.
// Thêm --find để tìm đường lắp sạch cho từng khối (in gợi ý cho data/installPaths.ts) thay vì kiểm tra;
// --parts=gpu,fans để chỉ tìm cho các bước đó.
// --fix: kiểm tra, tìm đường sạch cho từng khối vi phạm, ghi vào src/data/installPaths.json rồi kiểm tra lại.
import { spawn } from 'node:child_process'
import { mkdtempSync, readFileSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const find = process.argv.includes('--find')
const fix = process.argv.includes('--fix')
const TABLE = new URL('../src/data/installPaths.json', import.meta.url)
const partsArg = process.argv.find((a) => a.startsWith('--parts='))
const parts = partsArg ? JSON.stringify(partsArg.slice(8).split(',')) : 'undefined'
const machines = process.argv.slice(2).filter((a) => !a.startsWith('--'))
const profile = mkdtempSync(join(tmpdir(), 'pcsc-paths-'))
// Cổng ngẫu nhiên để không nối nhầm vào Chrome của lần chạy trước còn sót
const PORT = 9400 + Math.floor(Math.random() * 500)
const chrome = spawn('/Applications/Google Chrome.app/Contents/MacOS/Google Chrome', [
  '--headless=new', `--remote-debugging-port=${PORT}`, `--user-data-dir=${profile}`,
  '--window-size=1600,1000', '--use-angle=metal', 'about:blank'], { stdio: 'ignore' })
// Ctrl+C hay lỗi giữa chừng: vẫn tắt Chrome
for (const sig of ['SIGINT', 'SIGTERM']) process.on(sig, () => { chrome.kill('SIGKILL'); process.exit(130) })
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
let target
for (let i = 0; i < 50 && !target; i++) {
  await sleep(200)
  try { target = (await (await fetch(`http://127.0.0.1:${PORT}/json`)).json()).find((t) => t.type === 'page') } catch {}
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
const evaluate = async (expression) => {
  const m = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true })
  if (m.error) throw new Error(`Chrome: ${m.error.message}`)
  return m.result
}

let code = 2
try {
  await send('Emulation.setDeviceMetricsOverride', { width: 1600, height: 1000, deviceScaleFactor: 1, mobile: false })
  const openPage = async () => {
    await send('Page.navigate', { url: 'http://localhost:5173/' })
    await sleep(6000)
    // Mở trang mô hình 3D để có cảnh và hàm __checkPaths (chỉ có ở bản dev)
    await evaluate(`[...document.querySelectorAll('button')].find((b) => b.textContent.includes('xem mô hình 3D'))?.click()`)
    for (let i = 0; i < 60; i++) {
      if ((await evaluate('typeof window.__checkPaths')).result.value === 'function') break
      await sleep(500)
    }
  }
  await openPage()
  const check = async (list) => {
    const r = await evaluate(`window.__checkPaths(${list ? JSON.stringify(list) : 'undefined'})`)
    if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description ?? 'lỗi trong trang')
    return r.result.value
  }
  if (fix) {
    const t1 = Date.now()
    const found = await check(machines.length ? machines : undefined)
    console.log(`Kiểm tra: ${found.length} khối vi phạm`)
    const table = JSON.parse(readFileSync(TABLE, 'utf8'))
    const byMachine = new Map()
    for (const v of found) {
      if (!byMachine.has(v.machine)) byMachine.set(v.machine, new Set())
      byMachine.get(v.machine).add(v.path)
    }
    for (const [machine, paths] of byMachine) {
      const steps = [...new Set([...paths].map((p) => p.split('.')[0]))]
      const r = await evaluate(`window.__findPaths(${JSON.stringify([machine])}, ${JSON.stringify(steps)})`)
      if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description ?? 'lỗi trong trang')
      table[machine] ??= {}
      for (const path of paths) {
        // Sạch; dài nhất (đi từ ngoài vào) trước; cùng độ dài thì ưu tiên ra hông mở (+z)
        const ok = r.result.value.filter((o) => o.path === path && o.bad === 0)
        const score = (o) => -Math.hypot(...o.explode) - (o.explode[0] === 0 && o.explode[1] === 0 && o.explode[2] > 0 ? 0.3 : 0)
        ok.sort((a, b) => score(a) - score(b))
        const pick = ok[0]?.explode ?? [0, 0, 0]
        table[machine][path] = pick
        console.log(`  ${machine.padEnd(22)} ${path.padEnd(9)} -> [${pick.join(', ')}]${ok.length ? '' : '  (không có đường sạch: hiện tại chỗ)'}`)
      }
    }
    writeFileSync(TABLE, JSON.stringify(table, null, 2) + '\n')
    console.log(`Đã ghi ${TABLE.pathname} (${Math.round((Date.now() - t1) / 1000)} s). Kiểm tra lại...`)
    await sleep(3000)
    await openPage()
  }
  const t0 = Date.now()
  const arg = machines.length ? JSON.stringify(machines) : 'undefined'
  if (find) {
    const r = await evaluate(`window.__findPaths(${arg === 'undefined' ? 'undefined' : arg}, ${parts})`)
    if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description ?? 'lỗi trong trang')
    const by = new Map()
    for (const o of r.result.value) {
      const k = `${o.machine} ${o.path}`
      if (!by.has(k)) by.set(k, [])
      by.get(k).push(o)
    }
    // Ưu tiên: sạch, rồi ra hông mở (+z), rồi ngắn nhất
    const rank = (o) => o.bad * 100 + (o.explode[0] === 0 && o.explode[1] === 0 && o.explode[2] > 0 ? 0 : 1) * 10 + Math.hypot(...o.explode)
    for (const [k, list] of by) {
      list.sort((a, b) => rank(a) - rank(b))
      console.log(`${k.padEnd(32)} ${list.slice(0, 3).map((o) => `[${o.explode.join(', ')}]${o.bad ? ` bad=${o.bad} (${o.hits})` : ''}`).join('  |  ')}`)
    }
    console.log(`(${Math.round((Date.now() - t0) / 1000)} s)`)
    code = 0
    throw null
  }
  const list = await check(machines.length ? machines : undefined)
  console.log(`Đã kiểm tra ${machines.length ? machines.join(', ') : 'cả 6 máy'} trong ${Math.round((Date.now() - t0) / 1000)} s`)
  if (!list.length) {
    console.log('ĐẠT: không linh kiện nào đi xuyên qua vật khác khi lắp.')
    code = 0
  } else {
    console.log(`VI PHẠM (${list.length}):`)
    for (const v of list) console.log(`  ${v.machine.padEnd(22)} ${v.path.padEnd(9)} chạm ở ${Math.round(v.at * 100)}% đường đi, ${v.count} tia: ${v.hits}`)
    code = 1
  }
} catch (e) {
  if (e) console.error('Không chạy được kiểm tra:', e.message)
} finally {
  ws.close()
  chrome.kill('SIGKILL')
}
process.exit(code)
