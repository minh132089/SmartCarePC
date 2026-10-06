import { useMemo } from 'react'
import { BufferGeometry, CatmullRomCurve3, TubeGeometry, Vector3 } from 'three'
import { mergeGeometries } from 'three/examples/jsm/utils/BufferGeometryUtils.js'
import type { Build } from '../data/builds'
import { boardLayout, boardOrigin, powerConnectors, rearFanZ } from '../data/layout'
import { useViewer } from '../store'
import { MODEL_SIZE } from './Models'
import { Box, Mat, Part } from './Part'

const T = 0.03 // độ dày vách case, giống Case.tsx
const WIRE_R = 0.014 // bán kính một sợi dây bọc lưới (khoảng 3 mm)

type Vec3 = [number, number, number]
export interface Route {
  points: Vector3[]
  cols: number
  rows: number
  /** Đầu cắm dạng hộp đơn giản. Bỏ trống khi nơi gọi tự vẽ đầu cắm. */
  plug?: { pos: Vec3; size: Vec3 }
  /** Bán kính mỗi sợi, mặc định dây nguồn bọc lưới ~3 mm */
  r?: number
  /** Màu từng sợi, lặp lại theo thứ tự. Mặc định đen. */
  colors?: string[]
  /**
   * Khe sau khay mainboard: ở đoạn đường dây có z nhỏ hơn `below`, mọi sợi bị ép vào khoảng z [min, max]
   * để bó dây dàn thành một lớp phẳng, không xuyên qua khay hay nắp hông.
   */
  flatten?: { below: number; min: number; max: number }
}

const BLACK = '#16171b'

/** Bó nhiều sợi dây chạy song song theo một đường cong. Gộp các sợi cùng màu thành một hình khối cho nhẹ. */
function bundleGeometry({ points, cols, rows, r = WIRE_R, colors = [BLACK], flatten }: Route): Map<string, BufferGeometry> {
  const base = new CatmullRomCurve3(points, false, 'centripetal')
  const n = 64
  // Khung Frenet dùng phép chuyển song song, bó dây không bị xoắn ở chỗ gấp khúc
  const frames = base.computeFrenetFrames(n, false)
  const gap = r * 2.1
  const byColor = new Map<string, BufferGeometry[]>()
  for (let c = 0; c < cols; c++) {
    for (let k = 0; k < rows; k++) {
      const a = (c - (cols - 1) / 2) * gap
      const b = (k - (rows - 1) / 2) * gap
      const pts: Vector3[] = []
      for (let i = 0; i <= n; i++) {
        const c = base.getPointAt(i / n)
        const p = c.clone().addScaledVector(frames.normals[i], a).addScaledVector(frames.binormals[i], b)
        if (flatten && c.z < flatten.below) p.z = Math.min(flatten.max, Math.max(flatten.min, p.z))
        pts.push(p)
      }
      const color = colors[(c * rows + k) % colors.length]
      if (!byColor.has(color)) byColor.set(color, [])
      byColor.get(color)!.push(new TubeGeometry(new CatmullRomCurve3(pts), n * 2, r, 6, false))
    }
  }
  return new Map([...byColor].map(([color, list]) => [color, mergeGeometries(list)!]))
}

/**
 * Đi dây gọn như máy lắp cẩn thận: dây chạy sát vách phải, luồn sau lưng mainboard
 * (khoảng trống BOARD_STANDOFF), chỉ nhô ra ngay cạnh cổng cắm.
 * - 24 pin: từ nguồn áp vào vách, đi thẳng lên dọc mép phải bo, rẽ vào cổng.
 * - Nguồn CPU: vắt qua nóc nguồn áp vào vách, luồn sau bo lên tới mép trên, vòng xuống cổng.
 * - PCIe: từ nguồn đi thẳng lên, áp theo mặt trước card rồi cắm vào cạnh card.
 */
function cableRoutes(build: Build): Route[] {
  const { w: W, h: H, d: D } = build.case
  const v = (x: number, y: number, z: number) => new Vector3(x, y, z)
  const [ox, oy, oz] = boardOrigin(build)
  const L = boardLayout(build.formFactor)
  const S = L.surface
  const pc = powerConnectors(build.formFactor)
  const psu = build.psu === 'ATX' ? [1.4, 0.86, 1.5] : [1.0, 0.635, 1.25]
  const P = v(-D / 2 + 0.05 + psu[0] / 2, -H / 2 + T + psu[1] / 2, -W / 2 + T + psu[2] / 2)
  const pf = P.x + psu[0] / 2 // mặt trước nguồn, nơi có các cổng cắm dây
  const psuTop = P.y + psu[1] / 2
  const boardRight = ox + L.w / 2
  const boardTop = oy + L.h / 2
  const wall = -W / 2 + T // mặt trong vách phải
  const behind = (wall + oz) / 2 // giữa vách và mặt sau bo: chỗ luồn dây

  const routes: Route[] = []

  // Dây 24 pin: chạy dọc mép phải bo, áp sát vách, gần cổng mới rẽ ra
  const c24 = v(ox + pc.atx24.x, oy + pc.atx24.y, oz + S + 0.12)
  const xr = boardRight + 0.11
  const zr = wall + 0.1
  routes.push({
    points: [
      v(pf + 0.02, P.y + 0.05, P.z),
      v(pf + 0.14, P.y + 0.05, zr),
      v(xr - 0.12, P.y + 0.08, zr),
      v(xr, P.y + 0.3, zr),
      v(xr, c24.y - 0.25, zr),
      v(xr, c24.y, zr + 0.08),
      v(c24.x + 0.1, c24.y, c24.z - 0.02),
    ],
    cols: 6,
    rows: 4,
    plug: { pos: [c24.x + 0.02, c24.y, c24.z - 0.02], size: [0.16, 0.52, 0.16] },
  })

  // Dây nguồn CPU: luồn sau lưng bo, chỉ lộ đoạn dưới đáy bo và đoạn trên mép trên
  const cpu = v(ox + pc.cpu.x, oy + pc.cpu.y, oz + S + 0.1)
  const xc = cpu.x - 0.12
  routes.push({
    points: [
      v(pf + 0.02, P.y + 0.2, P.z - 0.1),
      v(pf - 0.15, psuTop + 0.07, behind + 0.1),
      v(xc, psuTop + 0.12, behind),
      v(xc, boardTop - 0.1, behind),
      v(xc + 0.04, boardTop + 0.08, (behind + cpu.z) / 2),
      v(cpu.x, cpu.y + 0.06, cpu.z),
    ],
    cols: pc.cpu.pins / 2,
    rows: 2,
    plug: {
      pos: [cpu.x, cpu.y + 0.02, cpu.z - 0.02],
      size: pc.cpu.pins === 8 ? [0.22, 0.1, 0.12] : [0.12, 0.12, 0.12],
    },
  })

  // Dây PCIe: từ nguồn đi thẳng lên, áp theo mặt trước card đồ họa
  const gpu = build.gpu
  if (gpu) {
    const s = gpu.length / MODEL_SIZE.gpu[0]
    const thick = MODEL_SIZE.gpu[1] * s
    const gx = ox - L.w / 2 + 0.05 + gpu.length / 2
    const gy = oy + L.pcieY(0) - thick / 2 + 0.08
    const front = oz + S + 0.12 + MODEL_SIZE.gpu[2] * s
    const cx = gx + gpu.length * 0.18
    routes.push({
      points: [
        v(pf + 0.02, P.y + 0.25, P.z + 0.4),
        v(cx, psuTop + 0.12, front + 0.1),
        v(cx, gy - thick / 2 - 0.1, front + 0.1),
        v(cx, gy, front + 0.04),
      ],
      cols: 4,
      rows: 2,
      plug: { pos: [cx, gy, front + 0.02], size: [0.2, 0.1, 0.1] },
    })
  }
  return routes
}

export function Cable({ route }: { route: Route }) {
  const geometries = useMemo(() => [...bundleGeometry(route)], [route])
  return (
    <>
      {geometries.map(([color, g]) => (
        <mesh key={color} geometry={g}>
          <Mat color={color} metal={0.1} rough={0.65} />
        </mesh>
      ))}
      {route.plug && <Box size={route.plug.size} position={route.plug.pos} color="#0e0f12" metal={0.05} rough={0.7} />}
    </>
  )
}

/**
 * Dây tín hiệu: dây quạt về các cổng quạt trên bo, dây nút nguồn / đèn (front panel) và dây USB mặt trước.
 * Cùng cách đi dây gọn: áp sát vách phải, luồn sau bo, chỉ nhô ra ở mép bo ngay cạnh cổng.
 */
function signalRoutes(build: Build): Route[] {
  const { w: W, h: H, d: D } = build.case
  const v = (x: number, y: number, z: number) => new Vector3(x, y, z)
  const [ox, oy, oz] = boardOrigin(build)
  const L = boardLayout(build.formFactor)
  const pc = powerConnectors(build.formFactor)
  const hz = oz + L.surface + 0.06 // mặt cổng cắm trên bo
  const wall = -W / 2 + T
  const zw = wall + 0.03 // sát vách phải
  const behind = (wall + oz) / 2 // khe sau lưng bo
  const edge = { top: oy + L.h / 2, bottom: oy - L.h / 2, right: ox + L.w / 2 }
  // Cổng trên bo (toạ độ thế giới). Vị trí chung chung theo cách bố trí phổ biến của mainboard.
  const at = (x: number, y: number) => v(ox + x, oy + y, hz)
  const topY = L.h / 2 - 0.07
  const header = {
    cpuFan: at(L.socket.x + 0.42, topY),
    pump: at(L.socket.x + 0.62, topY),
    sysRear: at(L.socket.x - 0.35, topY),
    sysFront: (i: number) => at(L.w / 2 - 0.07, pc.atx24.y - 0.7 - i * 0.22),
    usb3: at(L.w / 2 - 0.07, -L.h / 2 + 0.33),
    panel: at(L.w / 2 - 0.3, -L.h / 2 + 0.07),
  }
  const fan = (points: Vector3[], plugAt: Vector3, vertical = false): Route => ({
    points,
    cols: 2,
    rows: 2,
    r: 0.009,
    plug: { pos: [plugAt.x, plugAt.y, plugAt.z], size: vertical ? [0.05, 0.08, 0.05] : [0.08, 0.05, 0.05] },
  })
  const routes: Route[] = []

  // Quạt sau: từ góc trên của khung quạt, vắt qua mép trên bo vào cổng SYS_FAN1
  const rf = build.rearFan
  const rear = v(-D / 2 + T + 0.14, H / 2 - 0.3 - rf / 2, rearFanZ(build))
  const hr = header.sysRear
  routes.push(
    fan(
      [
        v(rear.x + 0.12, rear.y + rf / 2 - 0.08, rear.z - rf / 2 + 0.08),
        v((rear.x + hr.x) / 2, edge.top + 0.1, (rear.z - rf / 2 + hr.z) / 2),
        v(hr.x, edge.top + 0.06, hr.z + 0.04),
        v(hr.x, hr.y + 0.04, hr.z + 0.02),
      ],
      hr,
    ),
  )

  // Quạt trước: về vách phải, luồn sau bo, nhô ra ở mép phải vào các cổng SYS_FAN
  for (let i = 0; i < build.frontFans.count; i++) {
    const s = build.frontFans.size
    const f = v(D / 2 - 2 * T - 0.14, H / 2 - 0.3 - s / 2 - i * (s + 0.1), 0)
    const h = header.sysFront(i)
    routes.push(
      fan(
        [
          v(f.x - 0.12, f.y - s / 2 + 0.08, -s / 2 + 0.08),
          v(f.x - 0.22, f.y - s / 2 + 0.08, zw),
          v(edge.right + 0.12, h.y, zw),
          v(edge.right + 0.05, h.y, behind + 0.06),
          v(h.x + 0.04, h.y, h.z),
        ],
        h,
        true,
      ),
    )
  }

  // Quạt tản nhiệt CPU (và bơm nếu là tản nước) về cổng CPU_FAN / AIO_PUMP cạnh socket
  const socket = v(ox + L.socket.x, oy + L.socket.y, hz)
  const cf = header.cpuFan
  if (build.cooler === 'aio') {
    // Dây quạt két đi từ đầu két phía sau xuống mép trên bo; dây bơm ngắn từ cạnh bơm sang
    const radX = -D / 2 + 0.35 + 0.2
    const radBottom = H / 2 - T - 0.02 - 0.32
    const radZ = Math.min(0.25, W / 2 - 1.29 / 2 - 0.08) - 0.5
    routes.push(fan([v(radX, radBottom, radZ), v(cf.x - 0.2, edge.top + 0.12, (radZ + cf.z) / 2), v(cf.x, cf.y + 0.05, cf.z + 0.02)], cf))
    const ph = header.pump
    routes.push(fan([v(socket.x + 0.3, socket.y + 0.2, socket.z + 0.3), v(ph.x, ph.y - 0.15, ph.z + 0.15), v(ph.x, ph.y + 0.04, ph.z + 0.02)], ph))
  } else {
    const lift = build.cooler === 'tower' ? 0.9 : 0.35
    routes.push(fan([v(socket.x + 0.45, socket.y + 0.35, socket.z + lift), v(cf.x, cf.y - 0.1, cf.z + 0.2), v(cf.x, cf.y + 0.04, cf.z + 0.02)], cf))
  }

  // Dây mặt trước: nút nguồn / reset / đèn (bó dây nhiều màu) và USB (dây dẹt xanh),
  // từ cụm nút trên nóc chạy xuống sát vách phải, vòng qua đáy bo vào cổng ở góc dưới bên phải
  const btn = v(D / 2 - 0.35, H / 2 - T - 0.02, 0.2)
  const drop = (hx: number, hy: number, fromZ: number, below: boolean) => [
    v(btn.x, btn.y, fromZ),
    v(D / 2 - 0.45, H / 2 - 0.3, zw),
    v(D / 2 - 0.5, (below ? edge.bottom - 0.12 : hy) + 0.02, zw),
    below ? v(hx + 0.12, edge.bottom - 0.1, behind) : v(edge.right + 0.12, hy, behind),
    below ? v(hx, hy - 0.05, hz + 0.02) : v(hx + 0.05, hy, hz + 0.02),
  ]
  const p = header.panel
  routes.push({
    points: drop(p.x, p.y, btn.z, true),
    cols: 6,
    rows: 1,
    r: 0.006,
    colors: ['#c62828', '#f2f2f2', '#1b1b1b', '#2e7d32', '#1e5bb8', '#e0b100'],
    plug: { pos: [p.x, p.y, p.z], size: [0.22, 0.06, 0.05] },
  })
  const u = header.usb3
  routes.push({
    points: drop(u.x, u.y, btn.z - 0.3, false),
    cols: 4,
    rows: 1,
    r: 0.012,
    colors: ['#2f6fd6'],
    plug: { pos: [u.x, u.y, u.z], size: [0.06, 0.2, 0.06] },
  })
  return routes
}

/** Dây nguồn và dây tín hiệu trong thùng máy. Khi tách rời linh kiện thì dây được rút ra (ẩn đi). */
export function Cables({ build }: { build: Build }) {
  const exploded = useViewer((s) => s.exploded)
  const routes = useMemo(() => [...cableRoutes(build), ...signalRoutes(build)], [build])
  if (exploded) return null
  return (
    <Part id="cables">
      {routes.map((r, i) => (
        <Cable key={i} route={r} />
      ))}
    </Part>
  )
}
