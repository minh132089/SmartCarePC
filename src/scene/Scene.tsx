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
import { FAMILIES, familyFloorY } from '../data/families'
import { guideView } from '../data/guideViews'
import type { Machine, Vec3 } from '../data/installPaths'
import { Box3, Spherical, Vector3 } from 'three'
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
      <GuideCamera machine={familyId ?? 'gaming'} />
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

/**
 * Bước lắp có hướng nhìn riêng (data/guideViews.ts, vd card sau tấm đỡ của Mini, nguồn dưới tấm che): camera bay vòng
 * quanh máy tới hướng đó, giữ khoảng cách; rời bước thì quay về góc người dùng đang xem trước khi bay.
 * Chỉ bay lúc đổi bước; giữa chừng người dùng vẫn tự xoay được.
 */
function GuideCamera({ machine }: { machine: Machine }) {
  const view = useViewer((s) => (s.guide && !s.guideFull ? guideView(machine, s.guidePart) : null))
  const part = useViewer((s) => s.guidePart)
  const scene = useThree((s) => s.scene)
  const controls = useThree((s) => s.controls) as unknown as { object: { position: Vector3 }; target: Vector3; update: () => void } | null
  const invalidate = useThree((s) => s.invalidate)
  const anim = useRef<{ from: Spherical; to: Spherical; fromT: Vector3; toT: Vector3; t: number } | null>(null)
  /** Góc và điểm nhìn người dùng đang dùng trước khi bay sang hướng riêng, để quay về đúng chỗ */
  const saved = useRef<{ sph: Spherical; target: Vector3 } | null>(null)
  const shown = useRef<Vec3 | null>(null)

  useEffect(() => {
    if (!controls || view === shown.current) return
    shown.current = view
    const from = new Spherical().setFromVector3(controls.object.position.clone().sub(controls.target))
    const fromT = controls.target.clone()
    let to: Spherical
    let toT: Vector3
    if (view) {
      if (!saved.current) saved.current = { sph: from.clone(), target: fromT.clone() }
      const v = new Spherical().setFromVector3(new Vector3(...view))
      to = new Spherical(from.radius, v.phi, v.theta)
      // Dời điểm nhìn 70% về phía linh kiện của bước (vd nguồn ở đáy), vẫn giữ cả vỏ trong khung
      const box = new Box3()
      scene.traverse((o) => {
        if (o.userData.step === part) box.expandByObject(o)
      })
      const base = saved.current.target
      toT = box.isEmpty() ? base.clone() : base.clone().lerp(box.getCenter(new Vector3()), 0.7)
    } else {
      if (!saved.current) return
      to = saved.current.sph
      toT = saved.current.target
      saved.current = null
    }
    // Đi đường vòng ngắn nhất quanh trục đứng
    let theta = to.theta
    while (theta - from.theta > Math.PI) theta -= 2 * Math.PI
    while (theta - from.theta < -Math.PI) theta += 2 * Math.PI
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    anim.current = { from, to: new Spherical(to.radius, to.phi, theta), fromT, toT, t: reduce ? 1 : 0 }
    invalidate()
  }, [view, part, scene, controls, invalidate])

  useFrame((_, dt) => {
    const a = anim.current
    if (!a || !controls) return
    a.t = Math.min(1, a.t + dt / 0.9)
    const e = a.t < 0.5 ? 2 * a.t * a.t : 1 - (-2 * a.t + 2) ** 2 / 2 // ease-in-out
    const lerp = (x: number, y: number) => x + (y - x) * e
    const p = new Vector3().setFromSpherical(new Spherical(lerp(a.from.radius, a.to.radius), lerp(a.from.phi, a.to.phi), lerp(a.from.theta, a.to.theta)))
    controls.target.lerpVectors(a.fromT, a.toT, e)
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
    const checkAssembled = (machines?: import('./pathCheck').Machine[]) =>
      import('./pathCheck').then((m) => m.checkAssembled(scene, () => get().advance(performance.now(), true), machines))
    Object.assign(window, { __scene: scene, __checkPaths: checkPaths, __findPaths: findPaths, __checkAssembled: checkAssembled })
  }, [scene, get])
  return null
}
