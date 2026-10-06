import { Suspense, useMemo } from 'react'
import { CatmullRomCurve3, TubeGeometry, Vector3 } from 'three'
import type { Build } from '../data/builds'
import { boardLayout, boardOrigin } from '../data/layout'
import { useViewer } from '../store'
import { AioRadiatorModel } from './Models'
import { Box, Mat, Part } from './Part'

const T = 0.03 // độ dày vách case, giống Case.tsx
const RAD = { length: 2.44, width: 1.29, depth: 0.32 } // két 240 mm kèm quạt, đo khi xử lý model
const TUBE_R = 0.055 // ống cao su bọc lưới, khoảng 11 mm

/** Vị trí két tản dưới nóc case và điểm đầu ống ở bơm, theo cùng hệ toạ độ của thùng máy. */
function aioLayout(build: Build) {
  const { w: W, h: H, d: D } = build.case
  const top = H / 2 - T - 0.02
  // Két bắt đầu ngay trước quạt sau, nằm lệch về phía mặt kính để tránh RAM và nắp cổng I/O
  const x = -D / 2 + 0.35 + RAD.length / 2
  const z = Math.min(0.25, W / 2 - RAD.width / 2 - 0.08)
  const [ox, oy, oz] = boardOrigin(build)
  const L = boardLayout(build.formFactor)
  const pump = new Vector3(ox + L.socket.x, oy + L.socket.y, oz + L.surface + 0.065)
  return { top, x, z, pump }
}

/** Hai ống nước: từ nóc bơm đi lên, vòng tới đầu két phía trước thùng máy. */
function tubeGeometries(build: Build) {
  const { top, x, z, pump } = aioLayout(build)
  const end = x + RAD.length / 2 - 0.14
  const bottom = top - RAD.depth
  return [-0.09, 0.09].map((d) => {
    const pts = [
      new Vector3(pump.x + d, pump.y + 0.3, pump.z + 0.25),
      new Vector3(pump.x + d, pump.y + 0.62, pump.z + 0.32),
      new Vector3(end - 0.35 + d, bottom - 0.32, z - 0.3),
      new Vector3(end + d * 0.5, bottom - 0.04, z - 0.42 + d),
    ]
    return new TubeGeometry(new CatmullRomCurve3(pts, false, 'centripetal'), 64, TUBE_R, 10, false)
  })
}

/**
 * Tản nhiệt nước AIO: bơm nằm trên CPU (vẽ trong Board.tsx cùng linh kiện "cooler"),
 * két tản treo dưới nóc case, hai ống nối bơm với két. Cùng mã linh kiện "cooler" nên bấm vào đâu cũng sáng cả bộ.
 */
export function AioLoop({ build }: { build: Build }) {
  const exploded = useViewer((s) => s.exploded)
  const { top, x, z } = aioLayout(build)
  const tubes = useMemo(() => tubeGeometries(build), [build])
  return (
    <>
      <Part id="cooler" position={[x, top, z]} explode={[0, 0, 1.6]} order={2}>
        <Suspense fallback={<Box size={[RAD.length, RAD.depth, RAD.width]} position={[0, -RAD.depth / 2, 0]} color="#1a1b1f" />}>
          <AioRadiatorModel />
        </Suspense>
      </Part>
      {/* Khi tách rời linh kiện, két và bơm rời nhau nên ống được ẩn đi, giống dây nguồn */}
      {!exploded && (
        <Part id="cooler">
          {tubes.map((g, i) => (
            <mesh key={i} geometry={g}>
              <Mat color="#141518" metal={0.1} rough={0.7} />
            </mesh>
          ))}
        </Part>
      )}
    </>
  )
}
