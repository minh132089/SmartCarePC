import { useGLTF } from '@react-three/drei'
import { useFrame } from '@react-three/fiber'
import { useEffect, useMemo } from 'react'
import { Box3, Color, Group, Mesh, MeshStandardMaterial, Vector3, type Object3D } from 'three'
import type { BuildId, CoolerKind, FormFactor } from '../data/builds'
import { usePartState } from './partContext'
import { useViewer } from '../store'

/*
 * Model thật tải từ Sketchfab (giấy phép CC BY), đã xử lý bằng tools/process_model.py:
 * xoá logo hãng, xoay theo hướng của app, gốc toạ độ ở mặt áp vào bo mạch / tâm khối,
 * đơn vị 1 = 100 mm. Danh sách tác giả nằm trong src/data/credits.ts.
 */
const URL = {
  gpu: '/models/gpu.glb',
  psu: '/models/psu_atx.glb',
  fan: '/models/fan.glb',
  ssd: '/models/ssd.glb',
  cpu: '/models/cpu.glb',
}
const BOARD_URL: Record<FormFactor, string> = {
  ITX: '/models/motherboard_itx.glb',
  mATX: '/models/motherboard_matx.glb',
  ATX: '/models/motherboard_atx.glb',
}
const COOLER_URL: Record<CoolerKind, string> = {
  tower: '/models/cooler_tower.glb',
  lowprofile: '/models/cooler_lowprofile.glb',
  stock: '/models/cooler_stock.glb',
  aio: '/models/aio_pump.glb',
}
const AIO_RADIATOR_URL = '/models/aio_radiator.glb'
const RAM_URL = { heatsink: '/models/ram_red.glb', plain: '/models/ram_green.glb' }
const CASE_IDS: BuildId[] = ['office', 'study', 'gaming', 'workstation']
const caseUrl = (id: BuildId) => `/models/case_${id}.glb`

// Kích thước gốc của từng model sau khi xử lý (theo trục x, y, z của app)
const NATIVE = {
  gpu: [2.8, 0.53, 1.32],
  psu: [1.4, 0.81, 1.46],
} as const

/** Đổi màu tự sáng của vật liệu. Vật liệu three.js vốn được thiết kế để sửa trực tiếp. */
function applyHighlight(materials: MeshStandardMaterial[], selected: boolean, hovered: boolean) {
  const color = new Color(selected ? '#0071e3' : '#2997ff')
  for (const m of materials) {
    if (!m.userData.baseEmissive) {
      m.userData.baseEmissive = m.emissive.clone()
      m.userData.baseIntensity = m.emissiveIntensity
    }
    if (selected || hovered) {
      m.emissive.copy(color)
      m.emissiveIntensity = selected ? 0.45 : 0.2
    } else {
      m.emissive.copy(m.userData.baseEmissive)
      m.emissiveIntensity = m.userData.baseIntensity
    }
  }
}

/** Nhân bản cảnh và vật liệu để mỗi linh kiện tự sáng lên độc lập khi được chọn. */
function useHighlightedClone(scene: Object3D) {
  const { selected, hovered } = usePartState()
  const { root, materials } = useMemo(() => {
    const root = scene.clone(true)
    const materials: MeshStandardMaterial[] = []
    root.traverse((o) => {
      if (o instanceof Mesh) {
        const m = (o.material as MeshStandardMaterial).clone()
        o.material = m
        materials.push(m)
      }
    })
    return { root, materials }
  }, [scene])

  useEffect(() => applyHighlight(materials, selected, hovered), [selected, hovered, materials])

  return root
}

function Model({ url, scale = [1, 1, 1] }: { url: string; scale?: readonly [number, number, number] }) {
  const { scene } = useGLTF(url)
  const root = useHighlightedClone(scene)
  return <primitive object={root} scale={scale as [number, number, number]} />
}

/** Mainboard theo cỡ bo. Mặt sau bo nằm ở z = 0, linh kiện hướng về +z. */
export const MotherboardModel = ({ ff }: { ff: FormFactor }) => <Model url={BOARD_URL[ff]} />

/** CPU: đế xanh và nắp kim loại, dựng bằng tools/build_parts.py. */
export const CpuModel = () => <Model url={URL.cpu} />

/** Tản nhiệt theo loại. Gốc ở giữa đế tiếp xúc CPU. */
export const CoolerModel = ({ kind }: { kind: CoolerKind }) => <Model url={COOLER_URL[kind]} />

/** Thanh RAM. Chân cắm ở z = 0, thân RAM hướng về +z. */
export const RamModel = ({ heatsink }: { heatsink: boolean }) => <Model url={heatsink ? RAM_URL.heatsink : RAM_URL.plain} />

/** Ổ SSD M.2 nằm áp mặt bo, đầu cắm ở phía -x. */
export const SsdModel = () => <Model url={URL.ssd} />

/**
 * Bọc bộ phận quay (cánh quạt) trong một nút xoay đặt đúng tâm hình khối của nó.
 * Công cụ nén có thể dời gốc toạ độ của node, nên không tin vào gốc node mà tính tâm từ hình khối.
 */
function makeSpinner(obj: Object3D | undefined): Object3D | null {
  if (!obj?.parent) return null
  if (obj.parent.userData.spinner) return obj.parent
  obj.updateMatrix()
  const box = new Box3()
  obj.traverse((o) => {
    if (o instanceof Mesh) {
      o.geometry.computeBoundingBox()
      box.union(o.geometry.boundingBox!.clone().applyMatrix4(o === obj ? obj.matrix : o.matrixWorld))
    }
  })
  const center = box.getCenter(new Vector3())
  const pivot = new Group()
  pivot.userData.spinner = true
  pivot.position.copy(center)
  obj.parent.add(pivot)
  pivot.add(obj)
  obj.position.sub(center)
  return pivot
}

/** Quạt chỉ quay khi người dùng "Bật máy"; tắt máy thì đứng yên để cảnh không phải vẽ liên tục. */
const running = () => useViewer.getState().powered
const step = (dt: number) => Math.min(dt, 0.1)

function spin(o: Object3D, dt: number) {
  if (running()) o.rotation.x += step(dt) * 4
}

/** Quạt vuông, mặt quạt hướng theo trục x. `size` là cạnh quạt (1.2 = 120 mm). Cánh quạt quay. */
export function FanModel({ size, spinning = true }: { size: number; spinning?: boolean }) {
  const { scene } = useGLTF(URL.fan)
  const root = useHighlightedClone(scene)
  const blades = useMemo(() => makeSpinner(root.getObjectByName('blades')), [root])
  useFrame((_, dt) => {
    if (spinning && blades) spin(blades, dt)
  })
  return <primitive object={root} scale={[size, size, size]} />
}

/** Card đồ họa. Gốc ở giữa mép chân cắm, thân card hướng về +z, quạt úp xuống. */
export function GpuModel({ length }: { length: number }) {
  const s = length / NATIVE.gpu[0]
  return <Model url={URL.gpu} scale={[s, s, s]} />
}

/** Nguồn, co giãn theo kích thước chuẩn ATX hoặc SFX. */
export function PsuModel({ size }: { size: readonly [number, number, number] }) {
  const scale = [size[0] / NATIVE.psu[0], size[1] / NATIVE.psu[1], size[2] / NATIVE.psu[2]] as const
  return <Model url={URL.psu} scale={scale} />
}

/** Vỏ case dựng bằng tools/build_cases.py. Để trống mặt hông phía kính, app tự vẽ kính hoặc nắp. */
export const CaseModel = ({ id }: { id: BuildId }) => <Model url={caseUrl(id)} />

/** Két tản nước 240 mm kèm 2 quạt. Gốc ở giữa mặt trên, két treo xuống dưới, cánh quạt quay. */
export function AioRadiatorModel({ spinning = true }: { spinning?: boolean }) {
  const { scene } = useGLTF(AIO_RADIATOR_URL)
  const root = useHighlightedClone(scene)
  const blades = useMemo(
    () => ['blades_1', 'blades_2'].map((n) => makeSpinner(root.getObjectByName(n))).filter(Boolean) as Object3D[],
    [root],
  )
  useFrame((_, dt) => {
    if (spinning) for (const b of blades) spinAround(b, dt)
  })
  return <primitive object={root} />
}

function spinAround(o: Object3D, dt: number) {
  if (running()) o.rotation.y += step(dt) * 4 // quạt két nằm ngang, trục quay là trục đứng
}

export const MODEL_SIZE = NATIVE

for (const url of [AIO_RADIATOR_URL, ...Object.values(URL), ...Object.values(BOARD_URL), ...Object.values(COOLER_URL), ...Object.values(RAM_URL), ...CASE_IDS.map(caseUrl)]) {
  useGLTF.preload(url)
}

/*
 * Bộ linh kiện riêng cho máy Gaming, dựng bằng tools/build_gaming.py theo bản vẽ của Codex (design/gaming/).
 * Toạ độ gốc của từng model ghi trong docstring của hàm tương ứng trong script.
 */
const G = (name: string) => `/models/g_${name}.glb`
const GAMING_PARTS = ['case', 'motherboard', 'cpu', 'aio_pump', 'aio_backplate', 'aio_radiator', 'ram', 'gpu', 'ssd', 'psu', 'case_fan', 'case_fan_argb'] as const
type GamingPart = (typeof GAMING_PARTS)[number]

/** Model tĩnh của bộ Gaming. `hide`: tên các node cần ẩn (ví dụ màng bảo vệ đã bóc, nắp hông đã tháo). */
export function GamingModel({ name, hide = NONE }: { name: GamingPart; hide?: readonly string[] }) {
  const { scene } = useGLTF(G(name))
  const root = useHighlightedClone(scene)
  useEffect(() => setHidden(root, hide), [root, hide])
  return <primitive object={root} />
}

const NONE: readonly string[] = []

function setHidden(root: Object3D, names: readonly string[]) {
  root.traverse((o) => {
    if (o.name && o !== root) o.visible = !names.includes(o.name)
  })
}

/** Model có bộ phận quay: các node `nodes` quay quanh trục `axis` (cánh quạt). */
export function GamingSpinner({
  name,
  nodes,
  axis,
  speed = 4,
  spinning = true,
}: {
  name: GamingPart
  nodes: string[]
  axis: 'x' | 'y'
  /** rad/s, đổi dấu để đổi chiều quay */
  speed?: number
  spinning?: boolean
}) {
  const { scene } = useGLTF(G(name))
  const root = useHighlightedClone(scene)
  const spinners = useMemo(
    () => nodes.map((n) => makeSpinner(root.getObjectByName(n))).filter(Boolean) as Object3D[],
    [root, nodes],
  )
  useFrame((_, dt) => {
    if (spinning) for (const s of spinners) turn(s, axis, dt * speed)
  })
  return <primitive object={root} />
}

function turn(o: Object3D, axis: 'x' | 'y' | 'z', angle: number) {
  if (running()) o.rotation[axis] += Math.sign(angle) * Math.min(Math.abs(angle), 0.8)
}

/*
 * Linh kiện của 6 họ case, dựng bằng tools/build_families.py (tiền tố f_) hoặc dùng lại của bộ Gaming (g_).
 * `url` là đường dẫn đầy đủ trong public/models.
 */
/** Model tĩnh theo đường dẫn. `hide`: tên node cần ẩn. */
export function GlbModel({ url, hide = NONE }: { url: string; hide?: readonly string[] }) {
  const { scene } = useGLTF(url)
  const root = useHighlightedClone(scene)
  useEffect(() => setHidden(root, hide), [root, hide])
  return <primitive object={root} />
}

/** Model có node quay (cánh quạt) theo đường dẫn. */
export function GlbSpinner({
  url,
  nodes,
  axis,
  speed = 4,
  hide = NONE,
}: {
  url: string
  nodes: readonly string[]
  axis: 'x' | 'y' | 'z'
  speed?: number
  hide?: readonly string[]
}) {
  const { scene } = useGLTF(url)
  const root = useHighlightedClone(scene)
  const spinners = useMemo(
    () => nodes.map((n) => makeSpinner(root.getObjectByName(n))).filter(Boolean) as Object3D[],
    [root, nodes],
  )
  useEffect(() => setHidden(root, hide), [root, hide])
  useFrame((_, dt) => {
    for (const s of spinners) turn(s, axis, dt * speed)
  })
  return <primitive object={root} />
}

for (const name of GAMING_PARTS) useGLTF.preload(G(name))
