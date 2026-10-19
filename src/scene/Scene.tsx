import { Bounds, Environment, Lightformer, OrbitControls } from '@react-three/drei'
import { animated, useSpring } from '@react-spring/three'
import { Canvas, useFrame, useThree } from '@react-three/fiber'
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
import { FAMILIES, familyFloorY, gpuBehindBoard } from '../data/families'
import { Spherical, Vector3 } from 'three'
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
      <GuideCamera back={!!familyId && gpuBehindBoard(FAMILIES[familyId])} />
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

/** Hướng nhìn từ sau-phải, hông có tấm đỡ bo phía sau (đo bằng tia trên Mini / SFF: thấy ~96% card) */
const BACK_VIEW = new Spherical().setFromVector3(new Vector3(0.5, 0.3, -0.85))

/**
 * Bước cắm card của máy có card nằm sau tấm đỡ bo (Mini / SFF): từ góc mặc định, card bị tản, bo và nguồn che gần hết
 * (đo bằng tia: thấy 0–8%). Camera bay vòng quanh máy tới hướng BACK_VIEW, rời bước đó thì quay về góc đang xem trước đó.
 * Chỉ bay lúc đổi bước; giữa chừng người dùng vẫn tự xoay được.
 */
function GuideCamera({ back }: { back: boolean }) {
  const want = useViewer((s) => back && s.guide && !s.guideFull && s.guidePart === 'gpu')
  const controls = useThree((s) => s.controls) as unknown as { object: { position: Vector3 }; target: Vector3; update: () => void } | null
  const invalidate = useThree((s) => s.invalidate)
  const anim = useRef<{ from: Spherical; to: Spherical; t: number } | null>(null)
  /** Góc người dùng đang xem trước khi bay ra sau, để quay về đúng chỗ */
  const saved = useRef<Spherical | null>(null)

  useEffect(() => {
    if (!controls || want === (saved.current !== null)) return
    const from = new Spherical().setFromVector3(controls.object.position.clone().sub(controls.target))
    let to: Spherical
    if (want) {
      saved.current = from.clone()
      to = new Spherical(from.radius, BACK_VIEW.phi, BACK_VIEW.theta)
    } else {
      to = saved.current!
      saved.current = null
    }
    // Đi đường vòng ngắn nhất quanh trục đứng
    let theta = to.theta
    while (theta - from.theta > Math.PI) theta -= 2 * Math.PI
    while (theta - from.theta < -Math.PI) theta += 2 * Math.PI
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    anim.current = { from, to: new Spherical(to.radius, to.phi, theta), t: reduce ? 1 : 0 }
    invalidate()
  }, [want, controls, invalidate])

  useFrame((_, dt) => {
    const a = anim.current
    if (!a || !controls) return
    a.t = Math.min(1, a.t + dt / 0.9)
    const e = a.t < 0.5 ? 2 * a.t * a.t : 1 - (-2 * a.t + 2) ** 2 / 2 // ease-in-out
    const lerp = (x: number, y: number) => x + (y - x) * e
    const p = new Vector3().setFromSpherical(new Spherical(lerp(a.from.radius, a.to.radius), lerp(a.from.phi, a.to.phi), lerp(a.from.theta, a.to.theta)))
    controls.object.position.copy(controls.target).add(p)
    controls.update()
    if (a.t >= 1) anim.current = null
    else invalidate()
  })
  return null
}

/** Chỉ ở bản dev: cho công cụ kiểm tra (tools/shot.mjs, console) đọc cảnh qua window.__scene. */
function DevExpose() {
  const scene = useThree((s) => s.scene)
  const get = useThree((s) => s.get)
  useEffect(() => {
    // __checkPaths: kiểm tra nguyên tắc "linh kiện không đi xuyên qua nhau" (src/scene/pathCheck.ts)
    const checkPaths = (machines?: import('./pathCheck').Machine[]) =>
      import('./pathCheck').then((m) => m.checkPaths(scene, () => get().advance(performance.now(), true), machines))
    const findPaths = (machines?: import('./pathCheck').Machine[], parts?: import('../data/assembly').AssemblyKey[]) =>
      import('./pathCheck').then((m) => m.findPaths(scene, () => get().advance(performance.now(), true), machines, parts))
    Object.assign(window, { __scene: scene, __checkPaths: checkPaths, __findPaths: findPaths })
  }, [scene, get])
  return null
}
