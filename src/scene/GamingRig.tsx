import { animated, useSpring } from '@react-spring/three'
import { useMemo } from 'react'
import { CatmullRomCurve3, TubeGeometry, Vector3 } from 'three'
import {
  BEHIND,
  BEHIND_RANGE,
  CASE_PARTS,
  G4_DUCT_X,
  G4_MOUTH,
  GPU_POWER,
  GROMMET,
  HEADERS,
  ON_BOARD,
  PCB_TOP,
  PUMP_FITTINGS,
  RAD_FITTINGS,
  TRAY_FRONT,
  add,
  onBoard,
  u,
  type Mm3,
} from '../data/gaming'
import { useViewer } from '../store'
import { useOut } from '../data/assembly'
import { Cable, type Route } from './Cables'
import { GamingModel, GamingSpinner } from './Models'
import { Mat, Part } from './Part'

const W = 220 // bề rộng thùng (mm), để đặt tấm kính
const INSTALLED = ['peel_film'] // màng bảo vệ đế bơm đã bóc khi lắp
const PANEL = ['side_panel'] // nắp kín phía sau khay

/**
 * Máy Gaming dựng theo bản vẽ của Codex: vỏ, bo mạch và linh kiện, tản nước, quạt, nguồn và dây.
 * Mọi số đo nằm trong src/data/gaming.ts (mm), đổi sang đơn vị app bằng u().
 */
export function GamingRig() {
  const coolerOut = useOut('cooler')
  const cablesOut = useOut('cables')
  // Chọn "Dây cáp" thì tháo nắp kín phía sau khay để thấy dây đi sau lưng bo
  const cablesOpen = useViewer((s) => s.selected === 'cables')
  return (
    <>
      <Part id="case">
        <GamingModel name="case" hide={cablesOpen ? PANEL : undefined} />
      </Part>
      <GlassPanel />

      <Part id="motherboard" position={u(onBoard([0, 0, 0]))}>
        <GamingModel name="motherboard" />
      </Part>
      {/* Tấm đỡ bơm nằm sau bo, tâm trùng socket; lắp trước khi bắt bo vào case */}
      <Part id="cooler" position={u(onBoard(ON_BOARD.backplate))} step="cooler">
        <GamingModel name="aio_backplate" />
      </Part>
      <Part id="cpu" position={u(onBoard(ON_BOARD.cpu))} explode={[0, 0, 0.9]} step="cpu">
        <GamingModel name="cpu" />
      </Part>
      {ON_BOARD.ram.map((p, i) => (
        <Part key={i} id="ram" position={u(onBoard(p))} explode={[0, 0, 1.2]} step="ram">
          <GamingModel name="ram" />
        </Part>
      ))}
      <Part id="ssd" position={u(onBoard(ON_BOARD.ssd))} explode={[0, 0, 0.7]} step="ssd">
        <GamingModel name="ssd" />
      </Part>
      <Part id="gpu" position={u(onBoard(ON_BOARD.gpu))} explode={[0, 0, 1.5]} step="gpu">
        <GamingSpinner name="gpu" nodes={GPU_FANS} axis="y" />
      </Part>

      <Part id="cooler" position={u(onBoard(ON_BOARD.pump))} explode={[0, 0, 1.9]} step="cooler">
        <GamingModel name="aio_pump" hide={INSTALLED} />
      </Part>
      <Part id="cooler" position={u(CASE_PARTS.radiator)} explode={[0, 0, 1.6]} step="cooler">
        <GamingSpinner name="aio_radiator" nodes={RAD_FANS} axis="y" />
      </Part>
      {!coolerOut && <AioHoses />}

      {[...CASE_PARTS.frontFans, CASE_PARTS.rearFan].map((p, i) => (
        // Quạt trước tháo ra phía trước, quạt sau ra phía sau; quay theo chiều kim đồng hồ khi nhìn vào mặt hút (+x)
        <Part key={i} id="fans" position={u(p)} explode={[i < 3 ? 0.9 : -0.9, 0, 0]} step="fans">
          <GamingSpinner name={i < 3 ? 'case_fan_argb' : 'case_fan'} nodes={CASE_FAN} axis="x" speed={-4} />
        </Part>
      ))}

      <Part id="psu" position={u(CASE_PARTS.psu)} explode={[0, 0, 2.3]} step="psu">
        <GamingModel name="psu" />
      </Part>

      {!cablesOut && <GamingCables />}
    </>
  )
}

/**
 * Kính hông 430 × 445 × 4 cùng 4 vít tay và đệm cao su ở 4 góc (cách mép 15 mm).
 * Bấm "Tháo kính hông" thì kính trượt ra và mờ dần, giống bước tháo kính trước khi lắp máy.
 * Không nhận chuột để bấm xuyên vào linh kiện bên trong.
 */
function GlassPanel() {
  const sideOpen = useViewer((s) => s.sideOpen)
  const { z, opacity } = useSpring({ z: sideOpen ? 2.5 : 0, opacity: sideOpen ? 0 : 1, config: { tension: 247, friction: 31.4 } })
  const corners = [-1, 1].flatMap((sx) => [-1, 1].map((sy) => [sx * (215 - 15), sy * (222.5 - 15)] as const))
  return (
    <animated.group position-z={z} visible={opacity.to((o) => o > 0.02)}>
      <mesh position={u([0, 0, W / 2 + 2])} raycast={() => null}>
        <boxGeometry args={[4.3, 4.45, 0.04]} />
        <animated.meshStandardMaterial
          color="#11161b"
          transparent
          opacity={opacity.to((o) => o * 0.3)}
          metalness={0}
          roughness={0.02}
          envMapIntensity={0.35}
          depthWrite={false}
        />
      </mesh>
      {corners.map(([x, y], i) => (
        <group key={i} position={u([x, y, W / 2 + 4])} raycast={() => null}>
          <mesh rotation-x={Math.PI / 2}>
            <cylinderGeometry args={[0.05, 0.05, 0.03, 24]} />
            <animated.meshStandardMaterial color="#2a2e35" metalness={0.6} roughness={0.35} transparent opacity={opacity} />
          </mesh>
          <mesh rotation-x={Math.PI / 2} position-z={-0.035}>
            <cylinderGeometry args={[0.04, 0.04, 0.01, 20]} />
            <animated.meshStandardMaterial color="#202329" roughness={0.85} transparent opacity={opacity} />
          </mesh>
        </group>
      ))}
    </animated.group>
  )
}

const GPU_FANS = ['gpu_fan_1', 'gpu_fan_2', 'gpu_fan_3']
const RAD_FANS = ['blades_1', 'blades_2']
const CASE_FAN = ['blades']

/**
 * Hai ống nước bọc lưới Ø11, dài khoảng 350 mm: đi lên từ cút trên bơm, chạy phía sau cụm két
 * (z ≤ -32, không chạm khung quạt), vòng qua đầu két ở x ≥ 146 rồi đi lên vào hai cút dưới bình két.
 */
function AioHoses() {
  const tubes = useMemo(() => {
    const pump = onBoard(ON_BOARD.pump)
    return PUMP_FITTINGS.map((f, i) => {
      const a = add(pump, f)
      const b = add(CASE_PARTS.radiator, RAD_FITTINGS[i])
      const dz = i === 0 ? -7 : 7
      const pts: Mm3[] = [
        a,
        [a[0], 178, a[2] - 2],
        [-40, 196 + dz * 0.4, -44 + dz * 0.6],
        [70, 196, -42 + dz * 0.6],
        [146, 186, -10 + dz],
        [150, 168, b[2] - 6],
        [b[0] + 6, 168, b[2]],
        [b[0], 182, b[2]],
        b,
      ]
      const curve = new CatmullRomCurve3(pts.map(v), false, 'centripetal')
      return new TubeGeometry(curve, 128, 0.055, 12, false)
    })
  }, [])
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

const v = (p: number[]) => new Vector3(p[0] / 100, p[1] / 100, p[2] / 100)
const behind = (x: number, y: number): Mm3 => [x, y, BEHIND]
const front = (x: number, y: number, dz = 0): Mm3 => [x, y, TRAY_FRONT + dz]

/** Mặt khay phía kính: dây nằm sát khay khi phải đi một đoạn ngắn tới lỗ luồn. */
const onTray = (x: number, y: number): Mm3 => [x, y, -99]

type PlugKind = 'atx24' | 'eps8' | 'pcie8' | 'pwm4' | 'usb3' | 'audio' | 'pair' | 'argb'
interface Plug {
  kind: PlugKind
  /** tâm mặt đáy đầu cắm (mm, toạ độ thùng) */
  at: Mm3
  /** hướng ngàm khoá */
  latch?: 'x' | '-x' | 'y' | '-y'
}

/** Kích thước vỏ đầu cắm (x, y, z) theo bản vẽ 10 / cable kit. */
const PLUG_SIZE: Record<PlugKind, Mm3> = {
  atx24: [11, 52, 20],
  eps8: [19, 11, 20],
  pcie8: [19, 11, 20],
  pwm4: [10, 5, 12],
  usb3: [7, 19, 11],
  audio: [13, 6, 9],
  pair: [5, 2.5, 9],
  argb: [10.2, 5, 9],
}
const LATCH_AT: Record<NonNullable<Plug['latch']>, (s: Mm3) => Mm3> = {
  x: ([sx, , sz]) => [sx / 2 + 0.8, 0, sz / 2 - 4],
  '-x': ([sx, , sz]) => [-sx / 2 - 0.8, 0, sz / 2 - 4],
  y: ([, sy, sz]) => [0, sy / 2 + 0.8, sz / 2 - 4],
  '-y': ([, sy, sz]) => [0, -sy / 2 - 0.8, sz / 2 - 4],
}
/** Đường ghép của đầu cắm nhiều phần: ATX 20+4, EPS 4+4, PCIe 6+2. */
const SEAM: Partial<Record<PlugKind, Mm3>> = { atx24: [0, 26 - 8.4, 0], eps8: [0, 0, 0], pcie8: [4.2, 0, 0] }

/** Đầu cắm: vỏ nhựa đen, đường ghép và ngàm khoá; dây đi ra ở mặt trên. */
function PlugMesh({ kind, at, latch }: Plug) {
  const size = PLUG_SIZE[kind]
  const [sx, sy, sz] = size
  const seam = SEAM[kind]
  return (
    <group position={u([at[0], at[1], at[2] + sz / 2])}>
      <mesh>
        <boxGeometry args={u(size)} />
        <Mat color="#101216" metal={0.05} rough={0.6} />
      </mesh>
      {seam && (
        <mesh position={u(seam)}>
          <boxGeometry args={u(kind === 'atx24' ? [sx + 0.2, 0.6, sz + 0.2] : [0.6, sy + 0.2, sz + 0.2])} />
          <Mat color="#050506" rough={0.9} />
        </mesh>
      )}
      {latch && (
        <mesh position={u(LATCH_AT[latch](size))}>
          <boxGeometry args={u(latch === 'x' || latch === '-x' ? [1.6, 5, 9] : [5, 1.6, 9])} />
          <Mat color="#101216" metal={0.05} rough={0.6} />
        </mesh>
      )}
    </group>
  )
}

/** Tâm mặt đáy đầu cắm trên một đầu cắm của bo (z tính từ lưng bo). */
const onHeader = (h: Mm3, base: number): Mm3 => onBoard([h[0], h[1], base])
const top = (p: Plug): Mm3 => [p.at[0], p.at[1], p.at[2] + PLUG_SIZE[p.kind][2]]

/**
 * Dây đi gọn theo bản vẽ: từ nguồn qua G0 ra sau khay, chạy trong khe sau khay,
 * chỉ chui ra ở lỗ luồn gần đầu cắm (G1, G2, G3, G5, G6) rồi cắm thẳng vào đầu cắm.
 * Dây nguồn GPU đi trong khoang nguồn, lên trong ống che G4, ra ở miệng G4 cạnh card.
 */
function gamingWiring(): { routes: Route[]; plugs: Plug[]; splitter: Mm3; joints: Mm3[] } {
  const routes: Route[] = []
  const plugs: Plug[] = []
  const power = (points: Mm3[], cols: number, rows: number): Route => ({ points: points.map(v), cols, rows, r: 0.01 })
  const signal = (points: Mm3[], cols = 2, rows = 2): Route => ({ points: points.map(v), cols, rows, r: 0.008 })
  const plug = (p: Plug) => {
    plugs.push(p)
    return p
  }

  const exit = CASE_PARTS.psuExit
  const { G0, G1, G3, G5, G6 } = GROMMET
  const intoG0: Mm3[] = [exit, [exit[0] + 18, exit[1] + 4, exit[2] - 12], [G0[0] - 4, G0[1] - 2, -80], front(G0[0], G0[1]), behind(G0[0], G0[1] + 4)]

  // 24 pin: G0 → rãnh dọc phía trước khay → G2 → cổng ATX (ngàm phía +x)
  const atx = plug({ kind: 'atx24', at: onHeader(HEADERS.atx24, HEADERS.atx24[2]), latch: 'x' })
  routes.push(power([...intoG0, behind(0, -150), behind(60, -70), behind(60, 92), front(60, 100), [48, 104, -70], top(atx)], 12, 2))

  // EPS 8 pin (4+4): G0 → rãnh dọc phía sau khay → G1 ngay trên cổng → cổng CPU (ngàm phía +y)
  const eps = plug({ kind: 'eps8', at: onHeader(HEADERS.eps, HEADERS.eps[2]), latch: 'y' })
  routes.push(power([...intoG0, behind(-110, -150), behind(-205, -60), behind(-205, 160), behind(G1[0], G1[1] - 6), front(G1[0], G1[1]), [eps.at[0], eps.at[1] + 12, -64], top(eps)], 4, 2))

  // PCIe 6+2: ở trong khoang nguồn, lên trong ống che G4, ra ở miệng rồi cắm vào ổ trên mép card (miệng +z)
  const pcie = plug({ kind: 'pcie8', at: add(onBoard(ON_BOARD.gpu), GPU_POWER), latch: 'y' })
  routes.push(power([exit, [0, -200, 25], [G4_DUCT_X - 4, -170, 25], [G4_DUCT_X, -110, 25], [G4_DUCT_X, -10, 25], [G4_MOUTH[0] - 2, G4_MOUTH[1] + 4, G4_MOUTH[2] + 4], [64, 24, 66], top(pcie)], 4, 2))

  // Đầu cắm 1 × 4 (quạt, bơm) trùm lên 4 chân
  const pwm = (h: Mm3) => plug({ kind: 'pwm4', at: onHeader(h, PCB_TOP + 1.2) })

  // Bơm: dây đi dọc ống nước một đoạn rồi xuống thẳng đầu AIO_PUMP ngay bên dưới
  const pumpRoot = add(onBoard(ON_BOARD.pump), [14, 42, 30])
  const pp = pwm(HEADERS.pump)
  routes.push(signal([pumpRoot, [pumpRoot[0] - 2, 176, -60], [pp.at[0] + 6, pp.at[1] + 10, -72], top(pp)]))

  // Hai quạt két: men cạnh két phía bo, vào G3, gặp nhau ở bộ chia Y sau khay; một dây từ bộ chia ra lại G3 tới CPU_FAN
  const splitter = behind(G3[0], G3[1] - 10)
  for (const sx of [-1, 1]) {
    const stub = add(CASE_PARTS.radiator, [sx * 100, -29, -66])
    const mid: Mm3[] = sx > 0 ? [[40, 205, -34], [-40, 208, -40]] : [[-88, 210, -60]]
    routes.push(signal([stub, ...mid, [G3[0] + sx * 8, 220, -92], front(G3[0] + sx * 8, G3[1]), behind(G3[0] + sx * 6, G3[1] - 4), splitter]))
  }
  const cf = pwm(HEADERS.cpuFan)
  routes.push(signal([splitter, behind(G3[0] + 10, G3[1] - 2), front(G3[0] + 12, G3[1], 1), [cf.at[0] - 2, cf.at[1] + 14, -76], top(cf)]))

  // Ba quạt trước nằm sát mặt khay tới lỗ luồn. Trên: vào G2, ra lại mép trên G2 tới SYS_FAN1.
  // Giữa: vào G2, xuống sau khay, ra G5. Dưới: vào thẳng G5.
  const [f3, f2, f1] = CASE_PARTS.frontFans
  const stubOf = (f: Mm3): Mm3 => add(f, [-10, 55, -86]) // góc ra dây của quạt 140
  const s1 = pwm(HEADERS.sys1)
  const a1 = stubOf(f1)
  routes.push(signal([a1, [a1[0] - 12, a1[1] - 20, -96], onTray(150, 128), onTray(68, 122), front(62, 118), behind(60, 124), front(58, 134, 1), [s1.at[0] + 10, s1.at[1] - 4, -80], top(s1)]))
  const s2 = pwm(HEADERS.sys2)
  const a2 = stubOf(f2)
  routes.push(signal([a2, [a2[0] - 12, a2[1] - 6, -96], onTray(150, 92), onTray(68, 88), front(60, 88), behind(55, 80), behind(30, -60), behind(G5[0] - 8, G5[1] + 2), front(G5[0] - 8, G5[1], 1), [s2.at[0], s2.at[1] - 14, -82], top(s2)]))
  const s3 = pwm(HEADERS.sys3)
  const a3 = stubOf(f3)
  routes.push(signal([a3, [a3[0] - 12, a3[1] - 10, -96], onTray(150, -70), onTray(52, -100), front(G5[0] + 18, G5[1]), behind(G5[0] + 12, G5[1] - 4), front(G5[0] + 4, G5[1] + 2, 1), [s3.at[0] + 2, s3.at[1] - 12, -82], top(s3)]))

  // Quạt sau: vào G1, xuống rãnh sau khay, ra G6 tới SYS_FAN4
  const s4 = pwm(HEADERS.sys4)
  const rs = add(CASE_PARTS.rearFan, [-10, 45, -76])
  routes.push(signal([rs, [-196, 208, -94], front(G1[0] - 10, G1[1]), behind(G1[0] - 14, G1[1] - 6), behind(-212, 140), behind(-212, -70), behind(G6[0] - 8, G6[1] - 2), front(G6[0] - 8, G6[1], 1), [s4.at[0], s4.at[1] - 14, -82], top(s4)]))

  // Cụm cổng trên nóc: dây dọc mặt khay vào G2, xuống sau khay, ra G5 (USB, nút/đèn) và G6 (âm thanh)
  const io: Mm3 = [190, 222, -30]
  const down: Mm3[] = [io, [186, 206, -94], onTray(150, 112), onTray(70, 104), front(63, 104), behind(58, 96), behind(40, -40)]
  const usb = plug({ kind: 'usb3', at: onHeader(HEADERS.usb3, PCB_TOP + 1.6) })
  routes.push(signal([...down, behind(G5[0] + 14, G5[1] - 2), front(G5[0] + 14, G5[1], 1), [usb.at[0] + 2, usb.at[1] - 16, -80], top(usb)], 4, 2))
  // F_PANEL: bốn đầu 1 × 2 riêng (HDD LED, RESET, PLED, PWR SW) trên đúng cặp chân
  const fp = onBoard([HEADERS.panel[0], HEADERS.panel[1], PCB_TOP + 1.6])
  const pairs: Mm3[] = [
    [fp[0] - 3.81, fp[1] - 1.27, fp[2]],
    [fp[0] + 1.27, fp[1] - 1.27, fp[2]],
    [fp[0] - 3.81, fp[1] + 1.27, fp[2]],
    [fp[0] + 1.27, fp[1] + 1.27, fp[2]],
  ]
  for (const at of pairs) plug({ kind: 'pair', at })
  routes.push(signal([...down, behind(G5[0] - 2, G5[1] - 2), front(G5[0] - 2, G5[1], 1), [fp[0] - 2, fp[1] - 14, -82], [fp[0] - 1.27, fp[1], fp[2] + 9]], 4, 2))
  const au = plug({ kind: 'audio', at: onHeader(HEADERS.audio, PCB_TOP + 1.6) })
  routes.push(signal([...down, behind(-60, -128), behind(G6[0] + 10, G6[1] - 2), front(G6[0] + 10, G6[1], 1), [au.at[0] + 4, au.at[1] - 14, -82], top(au)], 3, 2))

  // ARGB 5 V, đủ chiều dài thật: mỗi quạt có đuôi IN 450 mm chạy về cụm nối sau khay, tại đó tách nhánh OUT 80 mm;
  // dây nối dài 200 mm từ OUT quạt trên sang IN quạt giữa, rồi giữa sang dưới; dây 650 mm từ JRAINBOW1 lên IN quạt trên.
  // Phần dây dư nằm thành một khúc võng sau khay; fitLength() tìm độ võng để chiều dài đường tâm đúng số đo.
  const argb = (points: Mm3[]): Route => ({ points: points.map(v), cols: 3, rows: 1, r: 0.006 })
  const nodeTop = behind(150, 140)
  const nodeMid = behind(150, 0)
  const nodeLow = behind(60, -110)
  const outTop = behind(175, 105)
  const outMid = behind(175, -35)
  const outLow = behind(85, -135)
  const t1 = add(stubOf(f1), [0, -6, 6])
  const t2 = add(stubOf(f2), [0, -6, 6])
  const t3 = add(stubOf(f3), [0, -6, 6])
  const runs: [string, Mm3[], number, number, Mm3][] = [
    ['IN quạt trên', [t1, [t1[0] - 12, t1[1] - 20, -96], onTray(150, 128), onTray(70, 126), front(64, 124), behind(66, 130), nodeTop], 450, 6, [-1, 0, 0]],
    ['IN quạt giữa', [t2, [t2[0] - 12, t2[1] - 10, -96], onTray(150, 96), onTray(70, 92), front(64, 92), behind(70, 60), nodeMid], 450, 6, [-1, 0, 0]],
    ['IN quạt dưới', [t3, [t3[0] - 12, t3[1] - 10, -96], onTray(150, -74), onTray(56, -98), front(G5[0] + 20, G5[1] + 2), behind(40, -108), nodeLow], 450, 6, [-1, 0.3, 0]],
    ['OUT quạt trên', [nodeTop, outTop], 80, 1, [1, 0, 0]],
    ['OUT quạt giữa', [nodeMid, outMid], 80, 1, [1, 0, 0]],
    ['OUT quạt dưới', [nodeLow, outLow], 80, 1, [1, 0, 0]],
    ['Nối dài trên → giữa', [outTop, behind(165, 50), nodeMid], 200, 1, [-1, 0, 0]],
    ['Nối dài giữa → dưới', [outMid, behind(110, -70), nodeLow], 200, 1, [-0.7, 0.7, 0]],
  ]
  const rb = plug({ kind: 'argb', at: onHeader(HEADERS.argb, PCB_TOP + 1.6) })
  runs.push(['JRAINBOW1 → IN quạt trên', [top(rb), [rb.at[0] + 4, rb.at[1] - 14, -82], front(G5[0] + 6, G5[1], 1), behind(G5[0] + 6, G5[1] - 3), behind(120, -90), behind(130, 80), nodeTop], 650, 4, [-1, 0, 0]])
  const argbLengths: Record<string, number> = {}
  for (const [name, pts, len, at, dir] of runs) {
    const fitted = fitLength(pts, len, at, dir)
    argbLengths[name] = Math.round(pathLength(fitted))
    routes.push(argb(fitted))
  }
  if (import.meta.env.DEV) {
    // Tự kiểm tra: mọi điểm trên đường tâm dây ARGB phải nằm trong lòng case (tôn 3 mm) và trước nắp kín
    let outside = 0
    for (const r of routes.slice(-runs.length)) {
      const c = new CatmullRomCurve3(r.points, false, 'centripetal')
      for (const q of c.getSpacedPoints(400)) {
        const [x, y, z] = [q.x * 100, q.y * 100, q.z * 100]
        if (Math.abs(x) > 222 || Math.abs(y) > 232 || z < -109 || z > 107) outside++
      }
    }
    console.debug('ARGB lengths (mm)', JSON.stringify(argbLengths), 'points outside case:', outside)
  }

  // Đầu nối ARGB 3 tiếp điểm / 4 vị trí: cổ dây ở mỗi quạt, cụm IN tại mỗi nút, cụm OUT + dây nối dài, nắp bịt OUT cuối
  const joints: Mm3[] = [t1, t2, t3, nodeTop, nodeMid, nodeLow, outTop, outMid, outLow]

  return { routes, plugs, splitter, joints }
}

/** Chiều dài đường tâm (mm) của dây đi qua các điểm, theo cùng đường cong dùng để dựng dây. */
function pathLength(pts: Mm3[]) {
  return new CatmullRomCurve3(pts.map((p) => new Vector3(...p)), false, 'centripetal').getLength()
}

/**
 * Thêm phần dây dư để chiều dài đường tâm bằng `target` mm, mọi phần dư nằm sau khay:
 * - dư ít (dưới một vòng bán kính 20): một khúc võng giữa điểm `at - 1` và `at`, lệch theo `dir`;
 * - dư nhiều: cuộn tròn bán kính 20…45 mm đi qua điểm `at`, tâm cuộn lệch theo `dir` (giữ bán kính uốn ≥ 20).
 * Độ võng / bán kính tìm bằng chia đôi. Dây đã đủ dài thì giữ nguyên.
 */
function fitLength(pts: Mm3[], target: number, at: number, dir: Mm3): Mm3[] {
  const base = pathLength(pts)
  if (base >= target) return pts
  const n = Math.hypot(...dir)
  const d: Mm3 = [dir[0] / n, dir[1] / n, 0]
  const solve = (make: (x: number) => Mm3[], hi: number) => {
    let lo = 0
    for (let k = 0; k < 40; k++) {
      const m = (lo + hi) / 2
      if (pathLength(make(m)) < target) lo = m
      else hi = m
    }
    return make(hi)
  }
  if (target - base < 2 * Math.PI * 20) {
    const a = pts[at - 1]
    const b = pts[at]
    return solve((m) => [...pts.slice(0, at), [(a[0] + b[0]) / 2 + d[0] * m, (a[1] + b[1]) / 2 + d[1] * m, (a[2] + b[2]) / 2], ...pts.slice(at)], 80)
  }
  const p = pts[at]
  const turns = Math.ceil((target - base) / (2 * Math.PI * 45))
  const coil = (r: number): Mm3[] => {
    const c: Mm3 = [p[0] + d[0] * r, p[1] + d[1] * r, p[2]]
    const a0 = Math.atan2(p[1] - c[1], p[0] - c[0])
    const steps = Math.round(turns * 12)
    const ring: Mm3[] = []
    for (let i = 1; i < steps; i++) {
      const t = a0 + (i / steps) * (turns * 2 * Math.PI - 0.35)
      ring.push([c[0] + r * Math.cos(t), c[1] + r * Math.sin(t), p[2]])
    }
    return [...pts.slice(0, at + 1), ...ring, ...pts.slice(at + 1)]
  }
  return solve((r) => coil(Math.max(20, r)), 45)
}

/** Khe sau khay: bó dây ép thành lớp phẳng, tâm sợi trong BEHIND_RANGE. */
const FLAT = { below: -1.03, min: BEHIND_RANGE[0] / 100, max: BEHIND_RANGE[1] / 100 }

function GamingCables() {
  const { routes, plugs, splitter, joints } = useMemo(() => {
    const w = gamingWiring()
    return { ...w, routes: w.routes.map((r) => ({ ...r, flatten: FLAT })) }
  }, [])
  return (
    <Part id="cables">
      {routes.map((r, i) => (
        <Cable key={i} route={r} />
      ))}
      {plugs.map((p, i) => (
        <PlugMesh key={i} {...p} />
      ))}
      {joints.map((j, i) => (
        // Đầu nối ARGB dẹt: dày 3 mm để nằm gọn trong khe sau khay
        <mesh key={`j${i}`} position={u(j)}>
          <boxGeometry args={u([10, 5, 3])} />
          <Mat color="#202329" rough={0.6} />
        </mesh>
      ))}
      {/* Bộ chia Y của hai quạt két, dán sau khay */}
      <mesh position={u(splitter)}>
        <boxGeometry args={u([12, 8, 3])} />
        <Mat color="#101216" rough={0.6} />
      </mesh>
    </Part>
  )
}
