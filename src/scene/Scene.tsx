import { Bounds, Environment, Lightformer, OrbitControls } from '@react-three/drei'
import { animated, useSpring } from '@react-spring/three'
import { Canvas, useThree } from '@react-three/fiber'
import { useEffect, useRef, useState, type ReactNode } from 'react'
import { getBuild } from '../data/builds'
import { boardOrigin } from '../data/layout'
import { useViewer } from '../store'
import { QUALITY } from '../perf/quality'
import { RenderControl } from '../perf/RenderControl'
import { BoardAssembly } from './Board'
import { AioLoop } from './Aio'
import { Cables } from './Cables'
import { Case } from './Case'
import { Effects, StudioFloor } from './Stage'
import { useStage } from '../theme'
import { GamingRig } from './GamingRig'
import { FamilyRig } from './FamilyRig'
import { FAMILIES, familyFloorY } from '../data/families'
import { ToolKit } from './ToolKit'

export function Scene() {
  const buildId = useViewer((s) => s.buildId)
  const familyId = useViewer((s) => s.familyId)
  const select = useViewer((s) => s.select)
  const build = getBuild(buildId)
  const { h } = build.case
  const boardPos = boardOrigin(build)
  const floorY = familyId ? familyFloorY(familyId) : -h / 2 - 0.07
  const quality = useViewer((s) => s.quality)
  const stage = useStage()
  // Đang xem cận cảnh (khung này bị che kín) hoặc khung đã cuộn ra khỏi màn hình: dừng vẽ hẳn
  const covered = useViewer((s) => s.inspecting !== null)
  const wrap = useRef<HTMLDivElement>(null)
  const [onScreen, setOnScreen] = useState(true)
  useEffect(() => {
    const el = wrap.current
    if (!el) return
    const io = new IntersectionObserver(([e]) => setOnScreen(e.isIntersecting))
    io.observe(el)
    return () => io.disconnect()
  }, [])

  return (
    <div ref={wrap} className="absolute inset-0">
    <Canvas
      camera={{ position: [5.2, 3, 10], fov: 38 }}
      dpr={QUALITY[quality].dpr}
      frameloop={covered || !onScreen ? 'never' : 'demand'}
      onPointerMissed={() => select(null)}
    >
      <RenderControl />
      {/* Màu nền trùng token --color-stage của khung 3D để sàn tan vào nền, không thấy đường chân trời */}
      <color attach="background" args={[stage.bg]} />
      {/* Sương mù chỉ để sàn ở xa tan vào nền; bắt đầu đủ xa để không phủ lên thùng máy kể cả khi camera lùi ra */}
      <fog attach="fog" args={[stage.bg, 22, 46]} />
      <ambientLight intensity={0.9} />
      <directionalLight position={[6, 9, 8]} intensity={2.2} />
      <directionalLight position={[-6, 4, -4]} intensity={0.8} />
      {/* Đèn bên trong thùng máy, giống đèn LED của case thật */}
      <pointLight position={[0, 1, 0.6]} intensity={6} distance={6} decay={1.5} />
      <Environment resolution={256}>
        <Lightformer intensity={5} position={[0, 6, 4]} scale={[10, 4, 1]} />
        <Lightformer intensity={3} position={[-6, 2, 2]} rotation-y={Math.PI / 2} scale={[8, 4, 1]} />
        <Lightformer intensity={2.5} position={[6, 2, -2]} rotation-y={-Math.PI / 2} scale={[8, 4, 1]} />
      </Environment>

      <Bounds key={familyId ?? buildId} fit clip observe margin={1.25}>
        <SwapIn>
          {familyId ? (
            <FamilyRig id={familyId} />
          ) : build.id === 'gaming' ? (
            // Máy Gaming dùng bộ linh kiện dựng theo bản vẽ riêng (design/gaming)
            <GamingRig />
          ) : (
            <>
              <Case build={build} />
              <group position={boardPos}>
                <BoardAssembly build={build} />
              </group>
              <Cables build={build} />
              {build.cooler === 'aio' && <AioLoop build={build} />}
            </>
          )}
        </SwapIn>
        <ToolKit frontX={familyId ? FAMILIES[familyId].body[2] / 200 : build.id === 'gaming' ? 2.25 : build.case.d / 2} floorY={floorY} />
      </Bounds>

      <StudioFloor y={floorY} />
      <Effects />
      {import.meta.env.DEV && <DevExpose />}
      <OrbitControls makeDefault enablePan={false} minDistance={3} maxDistance={20} />
    </Canvas>
    </div>
  )
}

/**
 * Đổi loại máy: thùng máy mới nhô lên từ thấp hơn một chút và lớn dần từ 97%,
 * bằng lò xo không nảy. Bounds được tạo lại theo loại máy nên khối này chạy lại mỗi lần đổi.
 */
function SwapIn({ children }: { children: ReactNode }) {
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const { y, s } = useSpring({
    from: { y: reduce ? 0 : -0.15, s: reduce ? 1 : 0.97 },
    to: { y: 0, s: 1 },
    config: { tension: 247, friction: 31.4 },
  })
  return (
    <animated.group position-y={y} scale={s}>
      {children}
    </animated.group>
  )
}

/** Chỉ ở bản dev: cho công cụ kiểm tra (tools/shot.mjs, console) đọc cảnh qua window.__scene. */
function DevExpose() {
  const scene = useThree((s) => s.scene)
  useEffect(() => {
    Object.assign(window, { __scene: scene })
  }, [scene])
  return null
}
