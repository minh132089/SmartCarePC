import { useFrame } from '@react-three/fiber'
import { useViewer } from '../store'
import { Suspense, useRef } from 'react'
import type { Group } from 'three'
import { FanModel } from './Models'
import { Box, Mat } from './Part'

/** Quạt case: model thật, trong lúc tải thì hiện quạt dựng bằng khối. Viền đèn vẽ thêm khi có màu nhấn. */
export function Fan({
  size,
  position,
  accent,
  spinning = true,
}: {
  size: number
  position: [number, number, number]
  accent?: string | null
  spinning?: boolean
}) {
  return (
    <group position={position}>
      <Suspense fallback={<ProceduralFan size={size} position={[0, 0, 0]} spinning={spinning} />}>
        <FanModel size={size} spinning={spinning} />
      </Suspense>
      {accent && (
        <mesh rotation={[0, Math.PI / 2, 0]} position={[0.11 * size, 0, 0]}>
          <torusGeometry args={[size * 0.44, 0.012, 8, 48]} />
          <Mat color={accent} glow={accent} />
        </mesh>
      )}
    </group>
  )
}

function ProceduralFan({
  size,
  position,
  spinning = true,
}: {
  size: number
  position: [number, number, number]
  spinning?: boolean
}) {
  const blades = useRef<Group>(null)
  useFrame((_, dt) => {
    if (spinning && blades.current && useViewer.getState().powered) blades.current.rotation.x += Math.min(dt, 0.1) * 4
  })
  const t = 0.25
  const bar = size * 0.07
  const half = size / 2 - bar / 2
  return (
    <group position={position}>
      <Box size={[t, bar, size]} position={[0, half, 0]} color="#1b1d22" />
      <Box size={[t, bar, size]} position={[0, -half, 0]} color="#1b1d22" />
      <Box size={[t, size, bar]} position={[0, 0, half]} color="#1b1d22" />
      <Box size={[t, size, bar]} position={[0, 0, -half]} color="#1b1d22" />
      <group ref={blades} userData={{ spinner: true }}>
        <mesh rotation={[0, 0, Math.PI / 2]}>
          <cylinderGeometry args={[size * 0.16, size * 0.16, t * 0.8, 24]} />
          <Mat color="#2a2d33" />
        </mesh>
        {Array.from({ length: 7 }, (_, i) => (
          <group key={i} rotation={[(i / 7) * Math.PI * 2, 0, 0]}>
            <Box size={[0.02, size * 0.3, size * 0.15]} position={[0, size * 0.28, 0]} rotation={[0, 0.4, 0]} color="#3a3e46" />
          </group>
        ))}
      </group>
    </group>
  )
}
