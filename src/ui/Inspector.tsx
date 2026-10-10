import { Bounds, Center, Environment, Lightformer, OrbitControls } from '@react-three/drei'
import { useStage } from '../theme'
import { X } from '@phosphor-icons/react'
import { Canvas, useFrame } from '@react-three/fiber'
import { Suspense, useEffect, useMemo, useRef, useState, type ReactNode, type RefObject } from 'react'
import { Matrix4, Quaternion, Vector3, type Group } from 'three'
import { getBuild, type Build } from '../data/builds'
import { hotspotsFor, type Hotspot } from '../data/hotspots'
import { PARTS, type PartId } from '../data/parts'
import { gamingHotspots } from '../data/gamingHotspots'
import { FAMILIES, MODEL_URL, type Family } from '../data/families'
import { familyHotspots } from '../data/familyHotspots'
import { TOOL_HOTSPOTS } from '../data/toolHotspots'
import { AioRadiatorModel, CoolerModel, CpuModel, FanModel, GamingModel, GamingSpinner, GlbModel, GlbSpinner, GpuModel, MotherboardModel, PsuModel, RamModel, SsdModel } from '../scene/Models'
import { useViewer } from '../store'
import { QUALITY } from '../perf/quality'
import { RenderControl } from '../perf/RenderControl'

// Hướng nhìn ban đầu cho từng linh kiện, chọn để mặt có nhiều chi tiết nhất quay về phía người xem
const VIEW: Partial<Record<PartId, [number, number, number]>> = {
  ram: [0.25, 0.35, 1],
  cpu: [0.35, 0.55, 1],
  gpu: [0.35, -0.75, 0.6], // nhìn từ dưới lên để thấy quạt
  ssd: [0.3, 0.6, 1],
  psu: [-1, -0.55, 0.7], // từ phía lưng và đáy: thấy ổ cắm, công tắc và quạt
  cooler: [1, 0.45, 0.9],
  fans: [1, 0.35, 0.5],
  motherboard: [0.2, 0.25, 1],
  tools: [0, 1, 0.75], // nhìn thảm từ trên xuống, số 1 ở góc trên trái như bản vẽ
}

/** Hướng nhìn riêng cho linh kiện của họ case có mặt chi tiết nằm khác bộ Gaming. */
function familyView(f: Family, id: PartId): [number, number, number] | null {
  if (id === 'cooler' && f.cooler.kind === 'tower_air_155') return [1, 0.35, -0.75] // nhìn từ phía đế: thấy đế đồng, ống đồng và quạt
  if (id === 'cooler' && f.cooler.kind === 'low_profile_47') return [1, 0.5, 0.5] // thấy cả lá nhôm lẫn quạt
  return null
}

/** Model riêng của máy Gaming (dựng theo bản vẽ design/gaming). */
function GamingPartModel({ id }: { id: PartId }) {
  switch (id) {
    case 'motherboard':
      return <GamingModel name="motherboard" />
    case 'cpu':
      return <GamingModel name="cpu" />
    case 'ram':
      return <GamingModel name="ram" />
    case 'gpu':
      return <GamingSpinner name="gpu" nodes={['gpu_fan_1', 'gpu_fan_2', 'gpu_fan_3']} axis="y" />
    case 'ssd':
      return <GamingModel name="ssd" />
    case 'psu':
      return <GamingModel name="psu" />
    case 'fans':
      return <GamingSpinner name="case_fan_argb" nodes={['blades']} axis="x" speed={-4} />
    case 'cooler':
      return (
        <>
          <GamingSpinner name="aio_radiator" nodes={['blades_1', 'blades_2']} axis="y" />
          <group position={[0, -1.1, 0]}>
            <GamingModel name="aio_pump" />
          </group>
        </>
      )
    default:
      return null
  }
}

const GPU_FANS = { gpu_280: ['gpu_fan_1', 'gpu_fan_2', 'gpu_fan_3'], gpu_240: ['gpu_fan_1', 'gpu_fan_2'] }
const AIO360_FANS = ['blades_1', 'blades_2', 'blades_3']
const COOLER_FAN = ['cooler_fan']
const FAN_BLADES = ['blades']

/** Model của một họ case (tools/build_families.py), linh kiện dùng chung lấy từ bộ Gaming. */
function FamilyPartModel({ id, f }: { id: PartId; f: Family }) {
  switch (id) {
    case 'motherboard':
      return <GlbModel url={MODEL_URL.board[f.board.ff]} />
    case 'gpu':
      return <GlbSpinner url={MODEL_URL.gpu[f.gpu.kind]} nodes={GPU_FANS[f.gpu.kind]} axis="y" />
    case 'psu':
      return <GlbModel url={MODEL_URL.psu[f.psu.kind]} />
    case 'fans':
      return f.fans[0] ? <GlbSpinner url={MODEL_URL.fan[f.fans[0].model]} nodes={FAN_BLADES} axis="x" speed={-4} /> : null
    case 'cooler': {
      const k = f.cooler.kind
      if (k === 'aio_240') return <GamingPartModel id="cooler" />
      if (k === 'aio_360')
        return (
          <>
            <GlbSpinner url={MODEL_URL.radiator.aio_360} nodes={AIO360_FANS} axis="y" />
            <group position={[0, -1.1, 0]}>
              <GamingModel name="aio_pump" />
            </group>
          </>
        )
      return <GlbSpinner url={MODEL_URL.cooler[k]} nodes={COOLER_FAN} axis={k === 'tower_air_155' ? 'x' : 'z'} />
    }
    default:
      return <GamingPartModel id={id} />
  }
}

function PartModel({ id, build, family }: { id: PartId; build: Build; family: Family | null }) {
  if (id === 'tools') return <GlbModel url="/models/t_kit.glb" />
  if (family) return <FamilyPartModel id={id} f={family} />
  if (build.id === 'gaming') return <GamingPartModel id={id} />
  switch (id) {
    case 'ram':
      return <RamModel heatsink={build.gpu !== null} />
    case 'cpu':
      return <CpuModel />
    case 'gpu':
      return <GpuModel length={2.8} />
    case 'ssd':
      return <SsdModel />
    case 'psu':
      return <PsuModel size={build.psu === 'ATX' ? [1.4, 0.86, 1.5] : [1.0, 0.635, 1.25]} />
    case 'cooler':
      if (build.cooler === 'aio')
        return (
          <>
            <AioRadiatorModel />
            <group position={[0, -1.1, 0]}>
              <CoolerModel kind="aio" />
            </group>
          </>
        )
      return <CoolerModel kind={build.cooler} />
    case 'fans':
      return <FanModel size={1.2} />
    case 'motherboard':
      return <MotherboardModel ff={build.formFactor} />
    default:
      return null
  }
}

/**
 * Xoay một số linh kiện về tư thế quen thuộc như ảnh trên hộp.
 * RAM: chiều dài (y) nằm ngang, chiều cao (z) dựng đứng, mặt chip (x) quay về phía người xem, chân vàng ở dưới.
 */
function useOrientation(id: PartId) {
  return useMemo(() => {
    if (id !== 'ram') return new Quaternion()
    const m = new Matrix4().set(0, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1)
    return new Quaternion().setFromRotationMatrix(m)
  }, [id])
}

/**
 * Mỗi khung hình: đổi toạ độ 3D của từng chấm ra vị trí trên màn hình rồi dời nút HTML tương ứng.
 * Chấm là nút HTML bình thường nằm đè lên canvas, không phải khung HTML gắn trong cảnh 3D.
 */
function DotProjector({ anchor, spots, dots }: { anchor: RefObject<Group | null>; spots: Hotspot[]; dots: RefObject<(HTMLElement | null)[]> }) {
  const v = useMemo(() => new Vector3(), [])
  useFrame(({ camera, size }) => {
    const g = anchor.current
    if (!g) return
    spots.forEach((s, i) => {
      const el = dots.current[i]
      if (!el) return
      v.set(...s.at)
      g.localToWorld(v)
      v.project(camera)
      const visible = v.z < 1
      el.style.opacity = visible ? '1' : '0'
      el.style.transform = `translate(${(v.x * 0.5 + 0.5) * size.width}px, ${(-v.y * 0.5 + 0.5) * size.height}px) translate(-12px, -50%)`
    })
  })
  return null
}

function InspectScene({ id, build, family, spots, dots }: {
  id: PartId
  build: Build
  family: Family | null
  spots: Hotspot[]
  dots: RefObject<(HTMLElement | null)[]>
}) {
  const [spin, setSpin] = useState(true)
  const stageColors = useStage()
  const orientation = useOrientation(id)
  const anchor = useRef<Group>(null)
  return (
    <>
      <color attach="background" args={[stageColors.inspector]} />
      <ambientLight intensity={0.9} />
      <directionalLight position={[5, 8, 6]} intensity={2.2} />
      <directionalLight position={[-6, 3, -4]} intensity={0.9} />
      <Environment resolution={256}>
        <Lightformer intensity={5} position={[0, 6, 4]} scale={[10, 4, 1]} />
        <Lightformer intensity={3} position={[-6, 2, 2]} rotation-y={Math.PI / 2} scale={[8, 4, 1]} />
        <Lightformer intensity={2.5} position={[6, 2, -2]} rotation-y={-Math.PI / 2} scale={[8, 4, 1]} />
      </Environment>
      <Bounds fit clip observe margin={1.35}>
        <Center>
          <group ref={anchor} quaternion={orientation}>
            <PartModel id={id} build={build} family={family} />
          </group>
        </Center>
      </Bounds>
      <DotProjector anchor={anchor} spots={spots} dots={dots} />
      {/* Tự xoay chậm để thấy mọi mặt; người dùng chạm vào là dừng, để họ tự xem */}
      <OrbitControls makeDefault autoRotate={spin} autoRotateSpeed={0.7} enablePan={false} onStart={() => setSpin(false)} />
    </>
  )
}

export function Inspector() {
  const inspecting = useViewer((s) => s.inspecting)
  // key: đổi linh kiện thì khung được tạo lại, chú thích đang chọn tự về mặc định
  return inspecting ? <InspectorPanel key={inspecting} inspecting={inspecting} /> : null
}

function InspectorPanel({ inspecting }: { inspecting: PartId }) {
  const inspect = useViewer((s) => s.inspect)
  const buildId = useViewer((s) => s.buildId)
  const familyId = useViewer((s) => s.familyId)
  const quality = useViewer((s) => s.quality)
  const family = familyId ? FAMILIES[familyId] : null
  const [active, setActive] = useState<number | null>(null)
  const dots = useRef<(HTMLElement | null)[]>([])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && inspect(null)
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [inspect])

  const build = getBuild(buildId)
  const spots = inspecting === 'tools'
    ? TOOL_HOTSPOTS
    : family
    ? familyHotspots(family, inspecting)
    : build.id === 'gaming'
      ? gamingHotspots(inspecting)
      : hotspotsFor(inspecting, {
          ram: build.gpu ? 'heatsink' : 'plain',
          board: build.formFactor,
          cooler: build.cooler,
        })
  const dir = (family && familyView(family, inspecting)) || VIEW[inspecting] || [1, 0.5, 1]
  const len = Math.hypot(...dir)
  const cam: [number, number, number] = [(dir[0] / len) * 8, (dir[1] / len) * 8, (dir[2] / len) * 8]

  return (
    <div className="inspector absolute inset-0 z-20 flex flex-col bg-panel" role="dialog" aria-label={`Xem cận cảnh ${PARTS[inspecting].name}`}>
      <div className="flex items-start justify-between gap-4 px-6 pt-5">
        <div>
          <p className="text-[13px] font-semibold text-ink-2">Xem cận cảnh</p>
          <h2 className="text-[24px] leading-tight font-semibold tracking-[-0.02em]">{PARTS[inspecting].name}</h2>
        </div>
        <button
          onClick={() => inspect(null)}
          aria-label="Đóng"
          className="grid size-9 shrink-0 place-items-center rounded-full bg-fill text-ink-2 transition-transform duration-100 hover:text-ink active:scale-[0.94]"
        >
          <X size={16} aria-hidden />
        </button>
      </div>

      <div className="relative min-h-0 flex-1">
        <Canvas key={inspecting} camera={{ position: cam, fov: 32 }} dpr={QUALITY[quality].dpr} frameloop="demand">
          <RenderControl watchFps={false} />
          <Suspense fallback={null}>
            <InspectScene id={inspecting} build={build} family={family} spots={spots} dots={dots} />
          </Suspense>
        </Canvas>
        {spots.map((s, i) => (
          <button
            key={s.title}
            ref={(el) => {
              dots.current[i] = el
            }}
            onClick={() => setActive(i)}
            aria-label={s.title}
            style={{ opacity: 0 }}
            className="group absolute top-0 left-0 flex items-center gap-1.5 whitespace-nowrap will-change-transform"
          >
            <span
              className={`grid size-6 place-items-center rounded-full text-[12px] font-semibold shadow-[0_2px_8px_rgba(0,0,0,0.18)] ring-1 transition-colors duration-150 ${
                active === i ? 'bg-accent text-white ring-accent' : 'bg-surface text-link ring-line-strong'
              }`}
            >
              {i + 1}
            </span>
            <span
              className={`glass rounded-full bg-glass px-2.5 py-0.5 text-[12px] font-medium text-ink shadow-[0_2px_8px_rgba(0,0,0,0.12)] ring-1 ring-line backdrop-blur-md transition-opacity duration-150 ${
                active === i ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'
              }`}
            >
              {s.title}
            </span>
          </button>
        ))}
        <p className="pointer-events-none absolute bottom-3 left-0 w-full text-center text-[12px] text-ink-3">
          Kéo để xoay, cuộn để phóng to. Bấm số để xem chi tiết.
        </p>
      </div>

      <ol className="grid max-h-[38%] shrink-0 gap-2 overflow-y-auto px-5 pb-5 sm:grid-cols-2">
        {spots.map((s, i) => (
          <li key={s.title}>
            <SpotCard n={i + 1} active={active === i} onPick={() => setActive(i)} title={s.title}>
              {s.text}
            </SpotCard>
          </li>
        ))}
      </ol>
    </div>
  )
}

function SpotCard({ n, title, active, onPick, children }: { n: number; title: string; active: boolean; onPick: () => void; children: ReactNode }) {
  return (
    <button
      onClick={onPick}
      className={`flex w-full gap-3 rounded-2xl bg-surface p-3.5 text-left ring-1 transition-colors duration-150 ${
        active ? 'ring-accent' : 'ring-transparent hover:ring-line-strong'
      }`}
    >
      <span className={`grid size-6 shrink-0 place-items-center rounded-full text-[12px] font-semibold ${active ? 'bg-accent text-white' : 'bg-fill text-link'}`}>
        {n}
      </span>
      <span>
        <span className="block text-[15px] font-medium text-ink">{title}</span>
        <span className="mt-0.5 block text-[13px] leading-[1.45] text-ink-2">{children}</span>
      </span>
    </button>
  )
}
