import { animated, useSpring } from '@react-spring/three'
import { Html } from '@react-three/drei'
import type { ThreeEvent } from '@react-three/fiber'
import { useState, type ReactNode } from 'react'
import { PARTS, type PartId } from '../data/parts'
import { PartContext, usePartState } from './partContext'
import { useViewer } from '../store'
import { isOut, type AssemblyKey } from '../data/assembly'

type Vec3 = [number, number, number]

const MAX_ORDER = 5 // thứ tự tháo lớn nhất, xem prop order

/**
 * Một linh kiện có thể bấm chọn được.
 * Nhóm ngoài mang tên `mount_<id>`, trùng quy ước đặt tên Empty trong Blender,
 * nên sau này thay khối tạm bằng model thật mà không phải đổi vị trí.
 */
export function Part({
  id,
  position = [0, 0, 0],
  explode = [0, 0, 0],
  order = 0,
  step,
  children,
}: {
  id: PartId
  position?: Vec3
  explode?: Vec3
  /** Thứ tự tháo ra khi tách rời (0 = tháo đầu tiên). Lắp lại thì theo thứ tự ngược. */
  order?: number
  /** Bước lắp của linh kiện (data/assembly.ts). Có thì tách / lắp lần lượt từng bước thay vì cùng lúc. */
  step?: AssemblyKey
  children: ReactNode
}) {
  const exploded = useViewer((s) => s.exploded)
  const out = useViewer((s) => (step ? isOut(s.step, step) : s.exploded))
  const select = useViewer((s) => s.select)
  const hover = useViewer((s) => s.hover)
  const [over, setOver] = useState(false)
  // Lò xo không nảy (damping 1, response 0,4 s), các linh kiện tháo lần lượt cách nhau 60 ms
  const reduce = typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const spring = useSpring({
    offset: out ? explode : [0, 0, 0],
    delay: reduce || step ? 0 : (exploded ? order : MAX_ORDER - order) * 60,
    config: { tension: 247, friction: 31.4 },
    immediate: reduce,
  })

  const onOver = (e: ThreeEvent<PointerEvent>) => {
    e.stopPropagation()
    setOver(true)
    hover(id)
    document.body.style.cursor = 'pointer'
  }
  const onOut = () => {
    setOver(false)
    hover(null)
    document.body.style.cursor = 'auto'
  }
  const onClick = (e: ThreeEvent<MouseEvent>) => {
    e.stopPropagation()
    select(id)
  }

  return (
    <group name={`mount_${id}`} position={position}>
      <animated.group position={spring.offset as unknown as Vec3} onPointerOver={onOver} onPointerOut={onOut} onClick={onClick}>
        <PartContext.Provider value={id}>{children}</PartContext.Provider>
        {over && (
          <Html center distanceFactor={9} style={{ pointerEvents: 'none' }}>
            <div className="glass tip whitespace-nowrap rounded-full bg-glass px-3 py-1 text-[12px] font-medium text-ink shadow-[0_2px_10px_rgba(0,0,0,0.12)] ring-1 ring-line backdrop-blur-md">
              {PARTS[id].name}
            </div>
          </Html>
        )}
      </animated.group>
    </group>
  )
}

/** Vật liệu tự sáng lên khi linh kiện đang được chọn hoặc rê chuột. */
export function Mat({
  color,
  metal = 0.3,
  rough = 0.55,
  glow,
}: {
  color: string
  metal?: number
  rough?: number
  glow?: string
}) {
  const { selected, hovered } = usePartState()
  const emissive = selected ? '#0071e3' : hovered ? '#2997ff' : (glow ?? '#000000')
  const intensity = selected ? 0.55 : hovered ? 0.25 : glow ? 2.6 : 0 // đèn RGB đủ sáng để vượt ngưỡng toả sáng
  return <meshStandardMaterial color={color} metalness={metal} roughness={rough} emissive={emissive} emissiveIntensity={intensity} />
}

export function Box({
  size,
  position = [0, 0, 0],
  rotation = [0, 0, 0],
  color,
  metal,
  rough,
  glow,
}: {
  size: Vec3
  position?: Vec3
  rotation?: Vec3
  color: string
  metal?: number
  rough?: number
  glow?: string
}) {
  return (
    <mesh position={position} rotation={rotation}>
      <boxGeometry args={size} />
      <Mat color={color} metal={metal} rough={rough} glow={glow} />
    </mesh>
  )
}
