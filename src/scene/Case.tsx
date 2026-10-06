import { animated, useSpring } from '@react-spring/three'
import type { Build } from '../data/builds'
import { Suspense } from 'react'
import { rearFanZ } from '../data/layout'
import { useViewer } from '../store'
import { Fan } from './Fan'
import { CaseModel, PsuModel } from './Models'
import { Box, Part } from './Part'

const T = 0.03 // độ dày tấm vỏ

export function Case({ build }: { build: Build }) {
  const { w: W, h: H, d: D, glass, body } = build.case
  const sideOpen = useViewer((s) => s.sideOpen)
  const panelZ = W / 2 - T / 2
  const { z, opacity } = useSpring({ z: sideOpen ? panelZ + 2.5 : panelZ, opacity: sideOpen ? 0 : 1 })

  return (
    <>
      <Part id="case">
        <Suspense fallback={<CaseFallback build={build} />}>
          <CaseModel id={build.id} />
        </Suspense>
      </Part>

      {glass ? (
        // Mặt kính: không nhận chuột để bấm xuyên qua vào linh kiện bên trong
        <mesh position={[0, 0, W / 2 - 0.01]} raycast={() => null}>
          <boxGeometry args={[D - 0.08, H - 0.08, 0.012]} />
          <meshStandardMaterial color="#9fb4c8" transparent opacity={0.12} metalness={0.1} roughness={0.05} depthWrite={false} />
        </mesh>
      ) : (
        // Nắp hông kim loại: trượt ra khi bấm "Mở nắp hông"
        <animated.mesh
          position-z={z}
          visible={opacity.to((o) => o > 0.02)}
          raycast={() => null}
        >
          <boxGeometry args={[D, H, T]} />
          <animated.meshStandardMaterial color={body} transparent opacity={opacity} metalness={0.5} roughness={0.5} />
        </animated.mesh>
      )}

      <Part id="fans">
        {Array.from({ length: build.frontFans.count }, (_, i) => {
          const s = build.frontFans.size
          return <Fan key={i} size={s} position={[D / 2 - T * 2 - 0.14, H / 2 - 0.3 - s / 2 - i * (s + 0.1), 0]} accent={build.accent} />
        })}
        <Fan size={build.rearFan} position={[-D / 2 + T + 0.14, H / 2 - 0.3 - build.rearFan / 2, rearFanZ(build)]} accent={build.accent} />
      </Part>

      <Psu build={build} />
    </>
  )
}

/** Vỏ dựng bằng khối, hiện trong lúc model case đang tải. */
function CaseFallback({ build }: { build: Build }) {
  const { w: W, h: H, d: D, body, front } = build.case
  return (
    <>
        <Box size={[D, T, W]} position={[0, -H / 2 + T / 2, 0]} color={body} metal={0.5} rough={0.5} />
      <Box size={[D, T, W]} position={[0, H / 2 - T / 2, 0]} color={body} metal={0.5} rough={0.5} />
      <Box size={[T, H, W]} position={[-D / 2 + T / 2, 0, 0]} color={body} metal={0.5} rough={0.5} />
      <Box size={[D, H, T]} position={[0, 0, -W / 2 + T / 2]} color={body} metal={0.5} rough={0.5} />
      <Box size={[T * 2, H, W]} position={[D / 2 - T, 0, 0]} color={front} metal={0.3} rough={0.6} />
      {build.accent && (
        <Box size={[0.01, H * 0.7, 0.04]} position={[D / 2 + 0.006, 0, W / 2 - 0.2]} color={build.accent} glow={build.accent} />
      )}
      {/* Nút nguồn */}
      <mesh position={[D / 2 - 0.35, H / 2 + 0.01, 0]}>
        <cylinderGeometry args={[0.07, 0.07, 0.02, 24]} />
        <meshStandardMaterial color="#9aa1ab" metalness={0.8} roughness={0.3} />
      </mesh>
      {/* Chân đế */}
      {[-1, 1].flatMap((sx) =>
        [-1, 1].map((sz) => (
          <Box key={`${sx}${sz}`} size={[0.3, 0.06, 0.3]} position={[sx * (D / 2 - 0.3), -H / 2 - 0.03, sz * (W / 2 - 0.25)]} color="#0f1115" />
        )),
      )}
    </>
  )
}

function Psu({ build }: { build: Build }) {
  const { w: W, h: H, d: D } = build.case
  const size = (build.psu === 'ATX' ? [1.4, 0.86, 1.5] : [1.0, 0.635, 1.25]) as [number, number, number]
  const [dx, dy, dz] = size
  const fallback = (
    <>
      <Box size={[dx, dy, dz]} color="#202329" metal={0.5} rough={0.5} />
      <Box size={[dx * 0.6, dy * 0.5, 0.005]} position={[0, 0, dz / 2 + 0.003]} color="#3a3e46" />
    </>
  )
  return (
    <Part id="psu" position={[-D / 2 + 0.05 + dx / 2, -H / 2 + T + dy / 2, -W / 2 + T + dz / 2]} explode={[0, 0, 1.8]} order={5}>
      <Suspense fallback={fallback}>
        <PsuModel size={size} />
      </Suspense>
    </Part>
  )
}
