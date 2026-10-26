/**
 * NGUYÊN TẮC CỨNG CỦA HOẠT CẢNH LẮP MÁY: linh kiện không được đi xuyên qua nhau.
 *
 * Mỗi bước lắp, từng khối của bước đó (mỗi Part có `path`, vd "fans.2") đi thẳng từ chỗ chờ (offset `explode`) vào chỗ
 * lắp. Kiểm tra trên cảnh thật, giống hệt người dùng thấy trong hướng dẫn (khối của các bước sau chưa hiện):
 *  1. Mọi đỉnh của khối đang lắp, đi theo đường đó, không được cắt qua vật nào đang có trong cảnh;
 *  2. Ngược lại, đỉnh của vật trong cảnh không được cắm vào khối đang lắp (bắt trường hợp chân, tấm mỏng).
 * Chỉ được chạm trong SEAT cuối đường đi: phần cắm vào khe, socket hay ốp vào chỗ bắt vít.
 *
 * Chạy: `npm run check:paths` (tools/check_paths.mjs), hoặc trong bản dev gõ `await __checkPaths()` ở console.
 * Đường lắp nằm ở data/installPaths.ts (tìm đường sạch: `await __findPaths(['mini_sff'])`).
 * Đổi model, vị trí lắp hay đường lắp nào cũng phải chạy lại và không còn vi phạm.
 */
import { Box3, DoubleSide, Matrix4, Mesh, Raycaster, Vector3, type Material, type Object3D, type Scene, type Side } from 'three'
import { ASSEMBLY, STEPS, type AssemblyKey } from '../data/assembly'
import { FAMILY_IDS } from '../data/families'
import type { Machine } from '../data/installPaths'
import { useViewer } from '../store'

export type { Machine }
/** Đoạn cuối đường đi được phép chạm (đơn vị app, 1 = 100 mm): 10 mm, đủ cho RAM, card, CPU cắm vào khe */
export const SEAT = 0.1
/** Trong đoạn cắm, sượt qua lớp mỏng hơn mức này (1 mm) là chạm mép khe / khung; bo mạch 1,6 mm vẫn tính là xuyên */
const EDGE = 0.01
/** Trong đoạn cắm, phần xuyên qua vật (vít, chân, gờ) chỉ được chiếm dưới 10% số tia; nhiều hơn là cả thân đi xuyên */
const SEAT_SHARE = 0.1
/** Bỏ qua mặt bị tia chạm lướt (|cos| nhỏ hơn mức này, ~15°): trượt sát mặt tựa, không tính là xuyên */
const GRAZE = 0.26
/** Bộ tìm đường lấy ít mẫu hơn cho nhanh; đường được chọn vẫn phải qua checkPaths với đủ SAMPLES */
const FIND_SAMPLES = 300
/** Số đỉnh lấy mẫu mỗi phía (khối đang lắp / vật trong cảnh) cho mỗi khối */
const SAMPLES = 700
export const MACHINES: Machine[] = [...FAMILY_IDS, 'gaming']
const MOVING_STEPS = ASSEMBLY.map((a) => a.key).filter((k) => k !== 'cables') // dây hiện ra tại chỗ, không bay

export interface Violation {
  machine: Machine
  /** Mã khối, vd "fans.2" */
  path: string
  /** Khối đâm vào vật nào (linh kiện/vật liệu), "ngược:" là vật trong cảnh đâm vào khối */
  hits: string
  /** Bao nhiêu phần đường đi đã qua lúc chạm (0 = chỗ chờ, 1 = chỗ lắp) */
  at: number
  /** Số tia chạm sai / tổng số tia */
  count: string
}

export interface PathOption {
  machine: Machine
  path: string
  /** Offset explode đi từ chỗ lắp ra chỗ chờ */
  explode: [number, number, number]
  /** Số tia chạm sai; 0 = sạch */
  bad: number
  hits: string
}

const yieldFrame = () => new Promise<void>((r) => {
  const ch = new MessageChannel()
  ch.port1.onmessage = () => r()
  ch.port2.postMessage(0)
})

const shown = (o: Object3D) => {
  for (let p: Object3D | null = o; p; p = p.parent) if (!p.visible) return false
  return true
}
const mountOf = (o: Object3D) => {
  for (let p: Object3D | null = o; p; p = p.parent) if (p.name.startsWith('mount_')) return p
  return null
}

/** Dụng cụ chung: đổi máy, đặt trạng thái bước, chờ hoạt cảnh chạy xong */
function harness(scene: Scene, advance: () => void) {
  const settle = async (n = 70) => {
    for (let i = 0; i < n; i++) {
      advance()
      await yieldFrame()
    }
  }
  const open = async (machine: Machine) => {
    const s = useViewer.getState()
    if (machine === 'gaming') s.setBuild('gaming')
    else s.setFamily(machine)
    // Bước "Chuẩn bị" không đặt hẹn giờ lắp linh kiện, nên bảng hướng dẫn không ghi đè trạng thái lúc đang đo.
    // instantMotion: linh kiện nhảy ngay tới đích; nếu chờ lò xo (chạy theo thời gian thật) sẽ đo nhầm lúc đang bay
    useViewer.setState({ guide: true, guideFull: false, guideIndex: 0, page: 'build', powered: false, instantMotion: true })
    for (let i = 0; i < 40 && !scene.getObjectByName('mount_case'); i++) await settle(5)
    await new Promise((r) => setTimeout(r, 1200))
    await settle(60)
  }
  /** Trạng thái hướng dẫn tại bước `part`: chưa lắp (khối ở chỗ chờ) hoặc đã lắp */
  const at = async (part: AssemblyKey, installed: boolean) => {
    const k = ASSEMBLY.findIndex((a) => a.key === part)
    useViewer.setState({ guidePart: part, step: STEPS - k - (installed ? 1 : 0) })
    await settle(12)
  }
  return { open, at }
}

/** Các khối đang hiện của bước `part`, theo mã khối */
function blocksOf(scene: Scene, part: AssemblyKey): Map<string, Mesh[]> {
  const out = new Map<string, Mesh[]>()
  scene.traverse((o) => {
    if (o.userData.step !== part || !o.userData.path || !shown(o)) return
    const list: Mesh[] = []
    o.traverse((m) => m instanceof Mesh && shown(m) && list.push(m))
    if (list.length) out.set(o.userData.path as string, list)
  })
  return out
}

/** Vật đặc đang có trong cảnh (bỏ sàn, lưới, bóng: những thứ không thuộc nhóm mount_ nào) */
function staticMeshes(scene: Scene, moving: Set<Mesh>): Mesh[] {
  const list: Mesh[] = []
  scene.traverse((o) => {
    if (!(o instanceof Mesh) || moving.has(o) || !shown(o) || !mountOf(o)) return
    const mat = o.material as Material & { opacity?: number }
    if (mat.transparent && (mat.opacity ?? 1) < 0.05) return
    list.push(o)
  })
  return list
}

function samplePoints(meshes: Mesh[], n: number, filter?: (p: Vector3) => boolean): Vector3[] {
  const total = meshes.reduce((a, m) => a + m.geometry.attributes.position.count, 0)
  const stride = Math.max(1, Math.floor(total / n))
  const pts: Vector3[] = []
  let i = 0
  for (const m of meshes) {
    const pos = m.geometry.attributes.position
    for (let j = 0; j < pos.count; j++, i++) {
      if (i % stride) continue
      const p = new Vector3().fromBufferAttribute(pos, j).applyMatrix4(m.matrixWorld)
      if (!filter || filter(p)) pts.push(p)
    }
  }
  return pts
}

/** Hộp bao theo ma trận thế giới hiện có (không gọi setFromObject vì nó tính lại ma trận, xoá phần dời tạm) */
const boxOf = (meshes: Mesh[]) =>
  meshes.reduce((b, m) => {
    if (!m.geometry.boundingBox) m.geometry.computeBoundingBox()
    return b.union(m.geometry.boundingBox!.clone().applyMatrix4(m.matrixWorld))
  }, new Box3())

/** Quét khối `moving` (đang ở chỗ chờ) đi theo d vào chỗ lắp. Trả về chỗ chạm sai, null nếu sạch. */
function sweep(statics: Mesh[], moving: Mesh[], d: Vector3, samples = SAMPLES): Omit<Violation, 'machine' | 'path'> | null {
  // Bắn tia trúng cả mặt sau tam giác, nếu không tia đi từ trong vật ra sẽ lọt
  const sides = new Map<Material, Side>()
  for (const m of [...statics, ...moving]) {
    for (const mat of ([] as Material[]).concat(m.material)) {
      if (!sides.has(mat)) sides.set(mat, mat.side)
      mat.side = DoubleSide
    }
  }
  const len = d.length()
  const dir = d.clone().normalize()
  const rc = new Raycaster()
  rc.near = 1e-4
  rc.far = len
  const bad = new Map<string, { n: number; at: number }>()
  /** Xuyên qua trọn trong đoạn cắm (vít, chân, gờ đi qua lỗ khi bắt vào); chỉ sai nếu chiếm nhiều tia */
  const seatPass = new Map<string, { n: number; at: number }>()
  let rays = 0
  const name = (o: Object3D) => {
    const mount = mountOf(o)
    const mat = ([] as Material[]).concat((o as Mesh).material)[0]
    return `${mount ? mount.name.slice(6) : '?'}/${mat?.name || o.name}`
  }
  const normal = new Vector3()
  /**
   * Một tia theo đường đi. Theo dõi tia đi vào / đi ra từng vật (theo pháp tuyến mặt bị trúng):
   * - vào rồi ra khỏi cùng một vật = xuyên qua: ngoài đoạn cắm luôn sai; trong đoạn cắm chỉ là phần nhô (vít, chân, gờ)
   *   nếu ít tia bị vậy (SEAT_SHARE), còn nhiều tia = cả thân đi xuyên (vd tấm chống cong đi xuyên bo mạch);
   * - chỉ vào (cắm vào khe, ốp vào chỗ bắt vít): chỉ được trong đoạn cắm SEAT;
   * - chỉ ra (điểm bắt đầu nằm sẵn trong vật): được nếu ở đoạn cắm, hoặc rất sát điểm đầu (mặt tiếp xúc trùng nhau).
   */
  const test = (from: Vector3, ray: Vector3, targets: Mesh[], label: (o: Object3D) => string) => {
    rc.set(from, ray)
    rays++
    const inSeat = (t: number) => t >= len - SEAT
    const entered = new Map<Object3D, number>()
    let wrong: { o: Object3D; t: number; soft?: boolean } | null = null
    for (const hit of rc.intersectObjects(targets, false)) {
      if (!hit.face) continue
      normal.copy(hit.face.normal).transformDirection(hit.object.matrixWorld)
      const cos = normal.dot(ray)
      // Tia lướt gần song song mặt (< ~15°): khối đang trượt sát mặt nó tựa vào, không phải đi xuyên
      if (Math.abs(cos) < GRAZE) continue
      const into = cos < 0
      if (into) {
        if (!entered.has(hit.object)) entered.set(hit.object, hit.distance)
        continue
      }
      const t0 = entered.get(hit.object)
      if (t0 !== undefined) {
        // Sượt qua một lớp mỏng hơn EDGE trong đoạn cắm: chạm mép khe / khung socket, không tính
        if (inSeat(t0) && hit.distance - t0 < EDGE) {
          entered.delete(hit.object)
          continue
        }
        wrong = { o: hit.object, t: t0, soft: inSeat(t0) } // vào rồi ra: xuyên qua
        break
      }
      if (!inSeat(hit.distance) && hit.distance > 0.02) {
        wrong = { o: hit.object, t: hit.distance } // bắt đầu nằm trong vật rồi đi ra ngoài đoạn cắm
        break
      }
    }
    if (!wrong) {
      for (const [o, t] of entered) if (!inSeat(t)) wrong = !wrong || t < wrong.t ? { o, t } : wrong
    }
    if (!wrong) return
    const key = label(wrong.o)
    const into = wrong.soft ? seatPass : bad
    const b = into.get(key) ?? { n: 0, at: 1 }
    b.n++
    b.at = Math.min(b.at, wrong.t / len)
    into.set(key, b)
  }
  // 1. Đỉnh của khối đi theo d
  for (const p of samplePoints(moving, samples)) test(p, dir, statics, name)
  // 2. Đỉnh vật trong cảnh nằm trong vùng quét, đi theo -d so với khối
  const box = boxOf(moving)
  box.union(box.clone().translate(d)).expandByScalar(0.02)
  const back = dir.clone().negate()
  for (const p of samplePoints(statics, samples * 30, (p) => box.containsPoint(p)).slice(0, samples)) {
    test(p, back, moving, (o) => `reverse: ${name(o)}`)
  }
  sides.forEach((side, mat) => (mat.side = side))
  const soft = [...seatPass.values()].reduce((a, b) => a + b.n, 0)
  if (soft > rays * SEAT_SHARE) seatPass.forEach((v, k) => bad.set(`${k} (insertion segment)`, v))
  if (!bad.size) return null
  const worst = [...bad.entries()].sort((a, b) => b[1].n - a[1].n)
  const total = worst.reduce((a, [, b]) => a + b.n, 0)
  return {
    hits: worst.slice(0, 3).map(([k, b]) => `${k} ×${b.n}`).join(', '),
    at: Math.round(Math.min(...worst.map(([, b]) => b.at)) * 100) / 100,
    count: `${total}/${rays}`,
  }
}

/** Kiểm tra đường lắp hiện tại của mọi khối; trả về danh sách vi phạm (rỗng = đạt) */
export async function checkPaths(scene: Scene, advance: () => void, machines: Machine[] = MACHINES): Promise<Violation[]> {
  const h = harness(scene, advance)
  const out: Violation[] = []
  const posOf = (blocks: Map<string, Mesh[]>) =>
    new Map([...blocks].map(([k, ms]) => [k, new Vector3().setFromMatrixPosition(ms[0].matrixWorld)]))
  for (const machine of machines) {
    await h.open(machine)
    for (const part of MOVING_STEPS) {
      await h.at(part, true)
      const endPos = posOf(blocksOf(scene, part))
      await h.at(part, false)
      const start = blocksOf(scene, part)
      const startPos = posOf(start)
      const statics = staticMeshes(scene, new Set([...start.values()].flat()))
      for (const [path, meshes] of start) {
        const end = endPos.get(path)
        if (!end) continue
        const d = end.clone().sub(startPos.get(path)!)
        if (d.length() < 1e-4) continue
        const v = sweep(statics, meshes, d)
        if (v) out.push({ machine, path, ...v })
      }
    }
  }
  useViewer.setState({ instantMotion: false })
  useViewer.getState().setGuide(false)
  return out
}

/** Dời tạm các mesh của khối theo d (chỉ ma trận thế giới, không vẽ lại) trong lúc chạy fn, rồi trả về chỗ cũ */
function fromOutside<T>(meshes: Mesh[], d: Vector3, fn: () => T): T {
  const saved = meshes.map((m) => m.matrixWorld.clone())
  const t = new Matrix4().makeTranslation(d.x, d.y, d.z)
  meshes.forEach((m) => m.matrixWorld.premultiply(t))
  try {
    return fn()
  } finally {
    meshes.forEach((m, i) => m.matrixWorld.copy(saved[i]))
  }
}

/** Đoạn ngắn thử khi không ra hẳn ngoài vỏ được (đơn vị app): 60, 40, 25, 15 mm */
const SHORT = [0.6, 0.4, 0.25, 0.15]

/** Hướng thử khi tìm đường lắp: ra hông mở (+z) là chính, rồi các hướng thẳng và chéo khác */
const CANDIDATES: [number, number, number][] = [
  [0, 0, 1], [-1, 0, 0], [1, 0, 0], [0, 1, 0], [0, -1, 0], [0, 0, -1],
  [0, 1, 1], [0, -1, 1], [1, 0, 1], [-1, 0, 1], [-1, 1, 0], [1, 1, 0], [0, 1, -1], [-1, 0, -1],
]

/**
 * Tìm đường lắp thẳng không xuyên qua vật nào cho từng khối: đặt khối ở chỗ lắp, thử từng hướng với độ dài vừa đủ để
 * ra hẳn khỏi vỏ (thêm 0,25), quét từ chỗ lắp ra. Kết quả dùng để điền data/installPaths.ts.
 */
export async function findPaths(
  scene: Scene,
  advance: () => void,
  machines: Machine[] = MACHINES,
  parts: AssemblyKey[] = MOVING_STEPS,
): Promise<PathOption[]> {
  const h = harness(scene, advance)
  const res: PathOption[] = []
  for (const machine of machines) {
    await h.open(machine)
    const caseBox = new Box3().setFromObject(scene.getObjectByName('mount_case')!)
    for (const part of parts) {
      await h.at(part, true)
      const blocks = blocksOf(scene, part)
      const statics = staticMeshes(scene, new Set([...blocks.values()].flat()))
      for (const [path, meshes] of blocks) {
        const box = boxOf(meshes)
        for (const c of CANDIDATES) {
          const dir = new Vector3(...c).normalize()
          let exit = 0.2
          while (exit < 6 && box.clone().translate(dir.clone().multiplyScalar(exit)).intersectsBox(caseBox)) exit += 0.05
          // Ra hẳn ngoài vỏ trước; không sạch thì thử đoạn ngắn: hiện gần chỗ lắp rồi trượt vào
          for (const len of [exit + 0.25, ...SHORT.filter((l) => l < exit)]) {
            const d = dir.clone().multiplyScalar(len)
            // Đo đúng như checkPaths: dời tạm khối ra chỗ chờ rồi quét vào chỗ lắp
            // Sàng nhanh với ít mẫu; trông sạch thì kiểm lại đủ mẫu như checkPaths mới tính là sạch
            const v = fromOutside(meshes, d, () =>
              sweep(statics, meshes, d.clone().negate(), FIND_SAMPLES) ?? sweep(statics, meshes, d.clone().negate()),
            )
            const bad = v ? Number(v.count.split('/')[0]) : 0
            res.push({
              machine,
              path,
              explode: d.toArray().map((x) => Math.round(x * 100) / 100) as [number, number, number],
              bad,
              hits: v?.hits ?? '',
            })
            if (!bad) break // dài nhất mà sạch là đủ cho hướng này
          }
        }
      }
    }
  }
  useViewer.setState({ instantMotion: false })
  useViewer.getState().setGuide(false)
  return res
}

/** Khối bỏ qua khi soát chồng nhau lúc đã lắp: tấm chống cong (trụ đi xuyên lỗ bo mạch là đúng) */
const OVERLAP_SKIP = new Set(['cooler.0'])

export interface Overlap {
  machine: Machine
  /** Khối bị vật khác cắm ngang qua thân khi máy đã lắp đủ (mã khối hoặc tên linh kiện) */
  block: string
  hits: string
  /** Số tia từ đỉnh về tâm khối đi xuyên trọn một vật khác / tổng số tia */
  count: string
}

/**
 * Máy đã lắp đủ thì cũng không được có vật cắm ngang qua thân linh kiện (vd tấm đỡ bo đi xuyên quạt nóc).
 * Từ mỗi đỉnh của một khối bắn tia về tâm khối: tia vào rồi ra khỏi một vật khác nghĩa là vật đó nằm chắn ngang trong
 * thân khối. Cắm đúng (chân RAM trong khe, CPU trong socket) chỉ có tia đi ra khỏi khe nên không tính; phần nhô nhỏ
 * (vít, chân) được dưới SEAT_SHARE số tia.
 */
export async function checkAssembled(scene: Scene, advance: () => void, machines: Machine[] = MACHINES): Promise<Overlap[]> {
  const h = harness(scene, advance)
  const out: Overlap[] = []
  for (const machine of machines) {
    await h.open(machine)
    useViewer.setState({ guideFull: true, step: 0 })
    for (let i = 0; i < 12; i++) {
      advance()
      await yieldFrame()
    }
    const blocks = new Map<string, Mesh[]>()
    scene.traverse((o) => {
      // Chỉ linh kiện gọn có đường lắp. Bo mạch, dây, riser, ống nước trải dài cả vỏ nên tia về tâm cắt qua mọi thứ;
      // tấm chống cong có trụ xuyên lỗ bo mạch theo thiết kế
      const key = o.userData.path as string | undefined
      if (!key || OVERLAP_SKIP.has(key) || !shown(o)) return
      const list = blocks.get(key) ?? []
      o.traverse((m) => m instanceof Mesh && shown(m) && list.push(m))
      if (list.length) blocks.set(key, list)
    })
    for (const [block, meshes] of blocks) {
      const own = new Set(meshes)
      const statics = staticMeshes(scene, own)
      const center = boxOf(meshes).getCenter(new Vector3())
      const v = inside(statics, meshes, center)
      if (v) out.push({ machine, block, ...v })
    }
    useViewer.setState({ guideFull: false })
  }
  useViewer.setState({ instantMotion: false })
  useViewer.getState().setGuide(false)
  return out
}

function inside(statics: Mesh[], meshes: Mesh[], center: Vector3): { hits: string; count: string } | null {
  const sides = new Map<Material, Side>()
  for (const m of statics) {
    for (const mat of ([] as Material[]).concat(m.material)) {
      if (!sides.has(mat)) sides.set(mat, mat.side)
      mat.side = DoubleSide
    }
  }
  const rc = new Raycaster()
  const normal = new Vector3()
  const bad = new Map<string, number>()
  let rays = 0
  for (const p of samplePoints(meshes, SAMPLES)) {
    const dir = center.clone().sub(p)
    const len = dir.length()
    if (len < 0.01) continue
    dir.normalize()
    rc.set(p, dir)
    rc.near = 1e-4
    rc.far = len
    rays++
    const entered = new Set<Object3D>()
    for (const hit of rc.intersectObjects(statics, false)) {
      if (!hit.face) continue
      const cos = normal.copy(hit.face.normal).transformDirection(hit.object.matrixWorld).dot(dir)
      if (Math.abs(cos) < GRAZE) continue
      if (cos < 0) entered.add(hit.object)
      else if (entered.has(hit.object)) {
        const mount = mountOf(hit.object)
        const mat = ([] as Material[]).concat((hit.object as Mesh).material)[0]
        const key = `${mount ? mount.name.slice(6) : '?'}/${mat?.name || hit.object.name}`
        bad.set(key, (bad.get(key) ?? 0) + 1)
        break
      }
    }
  }
  sides.forEach((side, mat) => (mat.side = side))
  const total = [...bad.values()].reduce((a, b) => a + b, 0)
  if (total <= rays * SEAT_SHARE) return null
  const worst = [...bad.entries()].sort((a, b) => b[1] - a[1])
  return { hits: worst.slice(0, 3).map(([k, n]) => `${k} ×${n}`).join(', '), count: `${total}/${rays}` }
}
