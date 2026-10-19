import { useMemo } from 'react'
import { BufferAttribute, BufferGeometry, CatmullRomCurve3, DoubleSide, TubeGeometry, Vector3 } from 'three'
import { FAMILIES, HEADER_Z, MODEL_URL, RAD_FITTING_X, familyPlacement, gpuBehindBoard, rotX, rotate, type Family, type FamilyId } from '../data/families'
import { PUMP_FITTINGS, add, u, type Mm3 } from '../data/gaming'
import { useViewer } from '../store'
import { useOut } from '../data/assembly'
import { Cable, type Route } from './Cables'
import { GlbModel, GlbSpinner } from './Models'
import { Mat, Part } from './Part'

const OPEN = ['panel_open']
const OPEN_CLOSED = ['panel_open', 'panel_closed']
const CLOSED = ['panel_closed']
const PEEL = ['peel_film']
const BLADES = ['blades']
const COOLER_FAN = ['cooler_fan']
const GPU_FANS: Record<string, string[]> = { gpu_280: ['gpu_fan_1', 'gpu_fan_2', 'gpu_fan_3'], gpu_240: ['gpu_fan_1', 'gpu_fan_2'] }
const RAD_FANS: Record<string, string[]> = { aio_240: ['blades_1', 'blades_2'], aio_360: ['blades_1', 'blades_2', 'blades_3'] }

/** Hướng quạt: model quạt thổi theo -x; quay để thổi theo hướng cần. `spin`: xoay thêm (độ) quanh trục quạt để đầu dây ra đúng góc. */
const fanRotation = (fan: { blow: string; spin?: number }): [number, number, number] => {
  const [x, y, z] = FAN_ROT[fan.blow]
  return [x + ((fan.spin ?? 0) * Math.PI) / 180, y, z]
}

const FAN_ROT: Record<string, [number, number, number]> = { '-x': [0, 0, 0], '+y': [0, 0, -Math.PI / 2], '+z': [0, Math.PI / 2, 0] }

/**
 * Lắp ráp một trong 6 họ case từ families.json: vỏ, bo, CPU, tản, RAM, SSD, GPU (cắm thẳng hoặc qua riser),
 * nguồn, quạt, két nước, hub, đế, dây. Số đo theo design/families/00_families_spec.md.
 */
export function FamilyRig({ id }: { id: FamilyId }) {
  const f = FAMILIES[id]
  const P = useMemo(() => familyPlacement(f), [f])
  const coolerOut = useOut('cooler')
  const gpuOut = useOut('gpu')
  const cablesOut = useOut('cables')
  const sideOpen = useViewer((s) => s.sideOpen)
  const cablesOpen = useViewer((s) => s.selected === 'cables')
  // Bước cắm card của máy có card sau tấm đỡ bo: mở luôn tấm hông bên kia (panel_closed) để thấy card
  const gpuSide = useViewer((s) => s.guide && !s.guideFull && s.guidePart === 'gpu') && gpuBehindBoard(f)
  const backOpen = cablesOpen || gpuSide
  const hide = sideOpen ? (backOpen ? OPEN_CLOSED : OPEN) : backOpen ? CLOSED : undefined
  const cooler = f.cooler.kind
  const aio = cooler === 'aio_240' || cooler === 'aio_360'

  return (
    <>
      <Part id="case">
        <GlbModel url={MODEL_URL.case(id)} hide={hide} />
        {f.stand && (
          <group position={u(f.stand.origin)}>
            <GlbModel url={MODEL_URL.stand} />
          </group>
        )}
      </Part>

      <Part id="motherboard" position={u(f.board.origin)}>
        <GlbModel url={MODEL_URL.board[f.board.ff]} />
      </Part>
      {/* Tấm chống cong đi cùng bước lắp tản (explode 0: tách rời thì nằm yên, hướng dẫn thì hiện cùng tản) */}
      <Part id="cooler" position={u(P.backplate)} step="cooler">
        <GlbModel url={MODEL_URL.backplate} />
      </Part>
      <Part id="cpu" position={u(P.cpu)} explode={[0, 0, 0.9]} step="cpu">
        <GlbModel url={MODEL_URL.cpu} />
      </Part>
      {P.ram.map((p, i) => (
        <Part key={i} id="ram" position={u(p)} explode={[0, 0, 1.2]} step="ram">
          <GlbModel url={MODEL_URL.ram} />
        </Part>
      ))}
      <Part id="ssd" position={u(P.ssd)} explode={[0, 0, 0.7]} step="ssd">
        <GlbModel url={MODEL_URL.ssd} />
      </Part>

      {/* Tản CPU: tản khí (quạt quay) hoặc bơm AIO + két + ống */}
      <Part id="cooler" position={u(P.ihs)} explode={[0, 0, 1.9]} step="cooler">
        {aio ? (
          <GlbModel url={MODEL_URL.pump} hide={PEEL} />
        ) : (
          <GlbSpinner url={MODEL_URL.cooler[cooler as 'tower_air_155' | 'low_profile_47']} nodes={COOLER_FAN} axis={cooler === 'tower_air_155' ? 'x' : 'z'} />
        )}
      </Part>
      {f.radiator && (
        <Part id="cooler" position={u(f.radiator.origin)} explode={[0, 1.2, 0]} step="cooler">
          <GlbSpinner url={MODEL_URL.radiator[f.radiator.kind]} nodes={RAD_FANS[f.radiator.kind]} axis="y" />
        </Part>
      )}
      {f.radiator && !coolerOut && <Hoses f={f} ihs={P.ihs} />}

      {/* GPU: R0 cắm thẳng vào khe; R+ / R- dựng qua riser */}
      <Part id="gpu" position={u(f.gpu.origin)} explode={f.riser ? [0, 0, 1.4] : [0, 0, 1.5]} step="gpu">
        <group rotation-x={rotX(f.gpu.rot)}>
          <GlbSpinner url={MODEL_URL.gpu[f.gpu.kind]} nodes={GPU_FANS[f.gpu.kind]} axis="y" />
        </group>
      </Part>
      {f.riser && !gpuOut && <Riser f={f} fingers={P.gpuFingers} />}

      <Part id="psu" position={u(f.psu.center as Mm3)} explode={[0, 0, 1.8]} step="psu">
        <group rotation-x={rotX(f.psu.rot)}>
          <GlbModel url={MODEL_URL.psu[f.psu.kind]} />
        </group>
      </Part>

      {f.fans.map((fan, i) => (
        <Part key={i} id="fans" position={u(fan.at)} explode={fanExplode(fan)} step="fans">
          <group rotation={fanRotation(fan)}>
            <GlbSpinner url={MODEL_URL.fan[fan.model]} nodes={BLADES} axis="x" speed={-4} />
          </group>
        </Part>
      ))}
      <Part id="fans" step="fans">
        {f.hub && (
          <group position={u(f.hub)} rotation-y={Math.PI}>
            <GlbModel url={MODEL_URL.hub} />
          </group>
        )}
      </Part>

      {!cablesOut && <FamilyCables f={f} />}
    </>
  )
}

/** Quạt tháo ra theo hướng ra ngoài vỏ, dọc trục gió (trước / sau, đáy / nóc, hông). */
function fanExplode(fan: { at: Mm3; blow: '-x' | '+y' | '+z' }): [number, number, number] {
  const ax = fan.blow === '-x' ? 0 : fan.blow === '+y' ? 1 : 2
  const d: [number, number, number] = [0, 0, 0]
  d[ax] = Math.sign(fan.at[ax]) * 0.9
  return d
}

const v = (p: number[]) => new Vector3(p[0] / 100, p[1] / 100, p[2] / 100)

/** Hai ống nước Ø11 từ cút trên bơm, đi lên trên bo rồi tới hai đầu ống dưới bình két (phía +x). */
function Hoses({ f, ihs }: { f: Family; ihs: Mm3 }) {
  const tubes = useMemo(() => {
    const R = f.radiator!
    const fx = RAD_FITTING_X[R.kind]
    return PUMP_FITTINGS.map((pf, i) => {
      const a = add(ihs, pf)
      const b = add(R.origin, [fx, -40, i === 0 ? -15 : 15])
      const midY = (a[1] + b[1] - 50) / 2
      const pts: Mm3[] = [a, [a[0], a[1] + 30, a[2] + 4], [(a[0] + b[0]) / 2, midY + 30, (a[2] + b[2]) / 2 + (i === 0 ? -8 : 8)], [b[0] + 12, b[1] - 36, b[2]], [b[0], b[1] - 10, b[2]], b]
      return new TubeGeometry(new CatmullRomCurve3(pts.map(v), false, 'centripetal'), 96, 0.055, 12, false)
    })
  }, [f, ihs])
  return (
    <Part id="cooler">
      {tubes.map((g, i) => (
        <mesh key={i} geometry={g}>
          <Mat color="#202329" metal={0.1} rough={0.75} />
        </mesh>
      ))}
    </Part>
  )
}

/** Dải riser mềm rộng 70 mm từ đầu cắm trên khe bo tới khe nhận card, cùng hai đầu cứng. */
function Riser({ f, fingers }: { f: Family; fingers: Mm3 }) {
  const R = f.riser!
  const n = rotate(f.gpu.rot, [0, 0, 1]) // hướng thân card tính từ mép chân cắm
  const slotOrigin = add(fingers, [-n[0] * 14, -n[1] * 14, -n[2] * 14])
  const geo = useMemo(() => {
    const top: Mm3 = add(R.board, [0, 0, 14])
    const back: Mm3 = add(slotOrigin, [-n[0] * 6, -n[1] * 6, -n[2] * 6])
    const via = Object.values(f.grommets).find((g) => g[2] >= 90) // lỗ riser trên khay (G4 ở SFF / slim)
    const pts: Mm3[] = [top, add(top, [0, 0, 16])]
    if (via && back[2] < f.tray.z) {
      pts.push([via[0], via[1], f.tray.z + 10], [via[0], via[1], f.tray.z - 8])
    } else {
      pts.push([(top[0] + back[0]) / 2, (top[1] + back[1]) / 2, Math.max(top[2], back[2]) + 18])
    }
    pts.push(add(back, [-n[0] * 20, -n[1] * 20, -n[2] * 20]), back)
    return ribbon(pts.map(v), 0.7)
  }, [R, f, n, slotOrigin])
  return (
    <Part id="gpu">
      <mesh geometry={geo}>
        <meshStandardMaterial color="#1b1d22" metalness={0.3} roughness={0.6} side={DoubleSide} />
      </mesh>
      <group position={u(R.board)}>
        <GlbModel url={MODEL_URL.riser} hide={RISER_SLOT} />
      </group>
      <group position={u(slotOrigin)} rotation-x={rotX(f.gpu.rot)}>
        <GlbModel url={MODEL_URL.riser} hide={RISER_MALE} />
      </group>
    </Part>
  )
}
const RISER_SLOT = ['riser_slot']
const RISER_MALE = ['riser_male']

/** Dải phẳng rộng `w` (đơn vị app) theo đường cong, bề rộng nằm dọc trục x (theo chiều dài khe PCIe). */
function ribbon(points: Vector3[], w: number) {
  const curve = new CatmullRomCurve3(points, false, 'centripetal')
  const n = 64
  const pos: number[] = []
  const idx: number[] = []
  for (let i = 0; i <= n; i++) {
    const p = curve.getPointAt(i / n)
    pos.push(p.x - w / 2, p.y, p.z, p.x + w / 2, p.y, p.z)
    if (i < n) {
      const a = i * 2
      idx.push(a, a + 1, a + 2, a + 1, a + 3, a + 2)
    }
  }
  const g = new BufferGeometry()
  g.setAttribute('position', new BufferAttribute(new Float32Array(pos), 3))
  g.setIndex(idx)
  g.computeVertexNormals()
  return g
}

/**
 * Dây gọn theo lỗ luồn của từng họ: từ ổ ra dây của nguồn vào khe sau khay, chạy sau khay,
 * chui ra ở lỗ gần đầu cắm nhất rồi cắm thẳng vào đầu cắm.
 */
function FamilyCables({ f }: { f: Family }) {
  const routes = useMemo(() => familyRoutes(f), [f])
  return (
    <Part id="cables">
      {routes.map((r, i) => (
        <Cable key={i} route={r} />
      ))}
    </Part>
  )
}

function familyRoutes(f: Family): Route[] {
  const P = familyPlacement(f)
  const L = P.L
  const tz = f.tray.z
  const bz = tz - 7 // khe sau khay
  const fz = tz + 2.5 // vừa ra khỏi lỗ phía trước khay
  const G = f.grommets
  const g = (k: string) => G[k] ?? G.G2
  const front = (k: string): Mm3 => [g(k)[0], g(k)[1], fz]
  const behind = (k: string): Mm3 => [g(k)[0], g(k)[1], bz]
  const near = (p: Mm3) =>
    Object.keys(G).reduce((best, k) => (Math.hypot(G[k][0] - p[0], G[k][1] - p[1]) < Math.hypot(G[best][0] - p[0], G[best][1] - p[1]) ? k : best), Object.keys(G)[0])
  const routes: Route[] = []
  // Sau khay: mọi dây đi theo đường vuông góc, gom về một "trục dây" dọc cạnh hàng lỗ phía trước bo (cột G2),
  // mỗi dây một làn lệch nhau vài mm để thành bó gọn thay vì cắt chéo nhau.
  const spineX = (G.G2 ?? G[Object.keys(G)[0]])[0]
  let lane = 0
  const trunk = (p: Mm3, q: Mm3): Mm3[] => {
    const n = lane++
    // trục dây nằm giữa hai đầu thì dùng trục chung; nằm ngoài thì đi dọc ở đầu gần trục nhất (không vòng xa rồi quay lại)
    const sx = Math.min(Math.max(spineX, Math.min(p[0], q[0])), Math.max(p[0], q[0])) + ((n % 6) - 2.5) * 3
    const z = bz + ((n % 3) - 1) * 1.2
    if (Math.abs(p[1] - q[1]) < 20) return [[q[0], q[1], z]]
    // góc cua vát 14 mm để đường cong không vọt quá điểm góc (sát nóc / đáy)
    const sy = Math.sign(q[1] - p[1])
    const sx2 = Math.sign(q[0] - sx) || 1
    return [[sx, p[1], z], [sx, q[1] - sy * 14, z], [sx + sx2 * 14, q[1], z], [q[0], q[1], z]]
  }
  const power = (pts: Mm3[], cols: number, rows: number): Route => ({ points: pts.map(v), cols, rows, r: 0.01, plug: undefined })
  const signal = (pts: Mm3[], cols = 2, rows = 2): Route => ({ points: pts.map(v), cols, rows, r: 0.008 })
  const plug = (at: Mm3, size: Mm3): Route['plug'] => ({ pos: u([at[0], at[1], at[2] + size[2] / 2]), size: u(size) })

  const exit = P.psuExit
  const startBehind = exit[2] < tz // nguồn đã nằm sau khay (khoang sau)
  const fromPsu = (k: string): Mm3[] =>
    startBehind
      ? [exit, [exit[0] + 10, exit[1], bz], ...trunk([exit[0] + 10, exit[1], bz], behind(k))]
      : [exit, [exit[0] + 18, exit[1], exit[2] - 10], [g('G0')[0], g('G0')[1], fz + 8], front('G0'), behind('G0'), ...trunk(behind('G0'), behind(k))]
  const toHeader = (k: string, h: Mm3, lift = 20): Mm3[] => [front(k), [h[0], h[1], h[2] + lift + 10], [h[0], h[1], h[2] + lift]]
  /** Từ một điểm trong lòng máy: lùi thẳng về sát mặt khay, đi sát khay theo hai đoạn vuông góc tới lỗ k. */
  const alongTray = (p: Mm3, k: string): Mm3[] => {
    const hz = fz + 3
    const [gx, gy] = [g(k)[0], g(k)[1]]
    const sy = Math.sign(gy - p[1])
    const sx = Math.sign(gx - p[0]) || 1
    const turn: Mm3[] = Math.abs(gy - p[1]) > 20 && Math.abs(gx - p[0]) > 20 ? [[p[0], gy - sy * 14, hz], [p[0] + sx * 14, gy, hz]] : [[p[0], gy, hz]]
    return [[p[0], p[1], Math.max(hz, Math.min(p[2], hz + 6))], [p[0], p[1], hz], ...turn, [gx, gy, hz], front(k)]
  }

  // 24 pin
  const atx = P.header(L.headers.atx24, HEADER_Z.atx24)
  routes.push({ ...power([...fromPsu('G2'), ...toHeader('G2', atx)], 12, 2), plug: plug(atx, [11, 52, 20]) })
  // EPS 8 pin
  const eps = P.header(L.headers.eps, HEADER_Z.eps)
  routes.push({ ...power([...fromPsu('G1'), ...toHeader('G1', eps)], 4, 2), plug: plug(eps, [19, 11, 20]) })
  // PCIe 6+2 tới ổ trên card
  const gs = P.gpuSocket
  const gn = P.gpuSocketNormal
  const gpuEnd: Mm3[] = [front('G4'), add(gs, [gn[0] * 14 + 36, gn[1] * 14, gn[2] * 14]), add(gs, [gn[0] * 14 + 12, gn[1] * 14, gn[2] * 14]), add(gs, [gn[0] * 14, gn[1] * 14, gn[2] * 14])]
  routes.push({ points: [...fromPsu('G4'), ...gpuEnd].map(v), cols: 4, rows: 2, r: 0.01, plug: { pos: u(add(gs, [gn[0] * 7, gn[1] * 7, gn[2] * 7])), size: u(gn[2] !== 0 ? [19, 11, 14] : gn[1] !== 0 ? [19, 14, 11] : [14, 11, 19]) } })

  // Đầu cắm nhỏ ngồi trên chân header (kích thước mm: ngang x, dọc y, cao z)
  const FAN_PLUG: Mm3 = [10.5, 6, 8]

  // Quạt case: vào lỗ gần nhất, sau khay, ra lỗ gần đầu SYS_FAN (hoặc vào hub nếu có)
  const sys = L.headers.sys
  const trayFront = f.tray.rects ? Math.max(...f.tray.rects.map((r) => r[2])) : null
  f.fans.forEach((fan, i) => {
    const corner: Mm3 = add(fan.at, fan.blow === '-x' ? [-10, 50, -55] : fan.blow === '+y' ? [50, 10, -55] : [50, 50, -10])
    const target = f.hub ? f.hub : P.header(sys[i % sys.length], HEADER_Z.fan)
    const kin = near(corner)
    const kout = near(target)
    // Quạt trước nằm trước mép khay: dây vòng qua mép khay ra sau ngay, không vắt qua lòng máy
    const wrapX = trayFront !== null && fan.blow === '-x' && fan.at[0] > trayFront ? trayFront + 8 : null
    const pts: Mm3[] =
      wrapX !== null
        ? [corner, [wrapX, corner[1], corner[2]], [wrapX, corner[1], bz], ...trunk([wrapX, corner[1], bz], behind(kout))]
        : [corner, ...alongTray(corner, kin), behind(kin), ...trunk(behind(kin), behind(kout))]
    if (f.hub) pts.push([target[0], target[1], bz])
    else pts.push(...toHeader(kout, target, 14))
    routes.push({ ...signal(pts), plug: f.hub ? undefined : plug(target, FAN_PLUG) })
  })
  if (f.hub) {
    const hub: Mm3 = [f.hub[0], f.hub[1], bz]
    // hub → SYS_FAN1 (điều khiển PWM)
    const s1 = P.header(sys[0], HEADER_Z.fan)
    const k = near(s1)
    routes.push({ ...signal([hub, ...trunk(hub, behind(k)), ...toHeader(k, s1, 14)]), plug: plug(s1, FAN_PLUG) })
    // hub → đầu ARGB 5 V trên bo (điều khiển đèn)
    if (L.headers.argb) {
      const ar = P.header(L.headers.argb, HEADER_Z.fan)
      const ka = near(ar)
      routes.push({ ...signal([[hub[0] + 10, hub[1] - 6, bz], ...trunk([hub[0] + 10, hub[1] - 6, bz], behind(ka)), ...toHeader(ka, ar, 14)], 3, 1), plug: plug(ar, [8, 5, 8]) })
    }
    // nguồn cho hub: nhánh 4 dây từ nguồn chạy sau khay tới cổng nguồn của hub
    routes.push(power([...fromPsu(near(hub)), [hub[0] - 14, hub[1] - 25, bz], hub], 4, 1))
  }

  // Tản CPU tới đầu cắm trên bo (đoạn ngắn trên mặt bo)
  const cf = P.header(L.headers.cpu_fan, HEADER_Z.fan)
  if (f.cooler.kind.startsWith('aio')) {
    // bơm → AIO_PUMP (hoặc CPU_FAN nếu bo không có đầu riêng)
    const pumpH = L.headers.pump ? P.header(L.headers.pump, HEADER_Z.fan) : cf
    const root = add(P.ihs, [14, 42, 30])
    routes.push({ ...signal([root, [root[0], root[1] + 8, root[2] - 6], [pumpH[0], pumpH[1] + 10, pumpH[2] + 24], [pumpH[0], pumpH[1], pumpH[2] + 14]]), plug: plug(pumpH, FAN_PLUG) })
    // dây chung của các quạt két (qua dây chia) → CPU_FAN: từ góc két đi dọc nóc rồi thả xuống
    if (f.radiator) {
      const R = f.radiator.origin
      const half = f.radiator.kind === 'aio_360' ? 198 : 138
      const a: Mm3 = [R[0] - half + 15, R[1] - 45, R[2] - 55]
      const top = Math.min(R[1] - 60, cf[1] + 120)
      routes.push({ ...signal([a, [a[0], top, a[2] + 4], [cf[0], top, cf[2] + 30], [cf[0], cf[1] + 10, cf[2] + 22], [cf[0], cf[1], cf[2] + 14]]), plug: plug(cf, FAN_PLUG) })
    }
  } else {
    const root: Mm3 = add(P.ihs, f.cooler.kind === 'tower_air_155' ? [30, 55, 60] : [40, 40, 30])
    routes.push({ ...signal([root, [root[0], root[1] + 8, root[2] - 6], [cf[0], cf[1] + 10, cf[2] + 24], [cf[0], cf[1], cf[2] + 14]]), plug: plug(cf, FAN_PLUG) })
  }

  // Cụm cổng trên vỏ: USB-A → USB 3.0, USB-C → đầu Type-E, nút / đèn → F_PANEL, jack → HD_AUDIO
  const kinds = new Set(f.io.items.map((i) => i[0]))
  const io = f.io.items[0]
  const ioPt: Mm3 = f.io.face === 'x' ? [f.body[2] / 2 - 14, io[1], io[2]] : [io[1] - 6, f.body[1] / 2 - 14, io[2]]
  const kin = near(ioPt)
  const down: Mm3[] = [ioPt, ...alongTray(ioPt, kin), behind(kin)]
  const kio = G.G5 ? 'G5' : kin
  if (kinds.has('usb')) {
    const usb = P.header(L.headers.usb3, HEADER_Z.usb3)
    routes.push({ ...signal([...down, ...trunk(behind(kin), behind(kio)), ...toHeader(kio, usb, 16)], 4, 2), plug: plug(usb, [10, 22, 9]) })
  }
  if (kinds.has('usbc') && L.headers.type_e) {
    const te = P.header(L.headers.type_e, HEADER_Z.type_e)
    routes.push({ ...signal([...down, ...trunk(behind(kin), behind(kio)), ...toHeader(kio, te, 16)], 2, 1), plug: plug(te, [7, 18, 8]) })
  }
  const panel = P.header(L.headers.panel, HEADER_Z.panel)
  routes.push({ ...signal([...down, ...trunk(behind(kin), behind(kio)), ...toHeader(kio, panel, 14)], 4, 2), plug: plug(panel, [13, 6, 8]) })
  if (G.G6 && (kinds.has('jack') || kinds.has('mic'))) {
    const audio = P.header(L.headers.audio, HEADER_Z.audio)
    routes.push({ ...signal([...down, ...trunk(behind(kin), behind('G6')), ...toHeader('G6', audio, 14)], 3, 2), plug: plug(audio, [13, 6, 8]) })
  }
  // Dây điện AC nối dài (máy SFX): ổ C14 ở lưng case → đầu C13 bẻ góc cắm vào ổ trên nguồn
  if (f.ac_inlet && f.psu.kind === 'sfx') {
    const ac = f.ac_inlet
    const c14 = add(f.psu.center as Mm3, rotate(f.psu.rot, [-50, -9, 43]))
    const pts: Mm3[] = [[ac[0] + 14, ac[1], ac[2]], [c14[0] - 25, ac[1], ac[2]], [c14[0] - 22, c14[1], c14[2]], [c14[0] - 14, c14[1], c14[2]]]
    const size = rotate(f.psu.rot, [14, 20, 26]).map(Math.abs) as Mm3
    routes.push({ points: pts.map(v), cols: 1, rows: 1, r: 0.035, plug: { pos: u([c14[0] - 7, c14[1], c14[2]]), size: u(size) } })
  }
  return routes
}
