import boardsJson from './boards.json'
import familiesJson from './families.json'
import { add, type Mm3 } from './gaming'

/**
 * Sáu họ case (design/families/00_families_spec.md). Số đo đọc từ families.json và boards.json,
 * cùng nguồn với tools/build_families.py nên model và vị trí lắp trong app luôn khớp nhau.
 * Đơn vị mm, trục app: x lưng → trước, y đáy → nóc, z hông kín → hông mở. Gốc ở tâm thân case.
 */
// Họ slim_console đã bỏ khỏi app theo yêu cầu (dữ liệu và model vẫn giữ trong families.json / build_families.py)
export const FAMILY_IDS = ['standard_tower', 'compact_tower', 'showcase_dual_chamber', 'mini_sff', 'full_tower'] as const
export type FamilyId = (typeof FAMILY_IDS)[number]
export type Rot = 'R0' | 'R+' | 'R-'
export type FF = 'ATX' | 'mATX' | 'ITX'

export interface BoardLayout {
  w: number
  h: number
  cpu: [number, number]
  ram_x: number[]
  ram_y: number
  ram_used: number[]
  pcie: [number, number][]
  m2_center: [number, number]
  holes: [number, number][]
  headers: {
    atx24: [number, number]
    eps: [number, number]
    cpu_fan: [number, number]
    pump?: [number, number]
    sys: [number, number][]
    usb3: [number, number]
    panel: [number, number]
    audio: [number, number]
    argb?: [number, number]
    type_e?: [number, number]
  }
}

/** Bo ATX của bộ Gaming (tools/build_gaming.py), cùng định dạng với boards.json. */
const ATX: BoardLayout = {
  w: 244,
  h: 305,
  cpu: [19, 59],
  ram_x: [70, 79, 89, 98],
  ram_y: 62,
  ram_used: [1, 3],
  pcie: [[-34, -30], [-34, -91], [-34, -132]],
  m2_center: [35, -62],
  holes: [-142.5, 0, 142.5].flatMap((y) => [-112, 0, 112].map((x) => [x, y] as [number, number])),
  headers: {
    atx24: [117, 40], eps: [-60, 146], cpu_fan: [35, 126], pump: [10, 126],
    sys: [[106, 85], [82, -128], [95, -134], [-96, -135]],
    usb3: [113, -127], panel: [95, -143], audio: [-97, -143], argb: [55, -143],
  },
}

export const BOARD_LAYOUT: Record<FF, BoardLayout> = {
  ATX,
  mATX: boardsJson.mATX as unknown as BoardLayout,
  ITX: boardsJson.ITX as unknown as BoardLayout,
}

/** Độ cao (z theo lưng bo) của mặt trên từng loại đầu cắm, giống bộ Gaming. */
export const HEADER_Z = { atx24: 14.6, eps: 14.6, fan: 9.6, panel: 9.1, usb3: 10.6, audio: 9.1, type_e: 11.6 }

interface FanSpec { model: 'fan120' | 'fan140' | 'fan120_argb'; at: Mm3; blow: '-x' | '+y' | '+z'; spin?: number }
export interface Family {
  id: FamilyId
  label: string
  tagline: string
  body: [number, number, number]
  board: { ff: FF; origin: Mm3; io_y: [number, number] }
  cooler: { kind: 'tower_air_155' | 'low_profile_47' | 'aio_240' | 'aio_360' }
  radiator?: { kind: 'aio_240' | 'aio_360'; origin: Mm3 }
  gpu: { kind: 'gpu_280' | 'gpu_240'; origin: Mm3; rot: Rot }
  riser?: { board: Mm3; gpu: Mm3 }
  psu: { kind: 'atx' | 'sfx'; rot: Rot; center?: Mm3 }
  /** Ổ C14 trên lưng case (máy SFX), nối dài vào nguồn bằng dây AC */
  ac_inlet?: Mm3
  hub?: Mm3
  stand?: { origin: Mm3 }
  fans: FanSpec[]
  tray: { z: number; cpu_window: [number, number, number]; rects?: [number, number, number, number][] }
  grommets: Record<string, [number, number, number, number]>
  io: { face: 'x' | 'y'; items: [string, number, number][] }
  side_open?: { type: 'glass' | 'solid' }
}

const RAW = familiesJson as unknown as Record<FamilyId, Omit<Family, 'id'> & { psu: Family['psu'] & { center: Mm3 } }>
export const FAMILIES: Record<FamilyId, Family> = Object.fromEntries(
  FAMILY_IDS.map((id) => [id, { id, ...RAW[id] }]),
) as Record<FamilyId, Family>

/** Phép quay của đặc tả: R+ quay +90° quanh x, R- quay -90° quanh x. */
export const rotate = (r: Rot, [x, y, z]: Mm3): Mm3 => (r === 'R0' ? [x, y, z] : r === 'R+' ? [x, -z, y] : [x, z, -y])
export const rotX = (r: Rot) => (r === 'R0' ? 0 : r === 'R+' ? Math.PI / 2 : -Math.PI / 2)

/**
 * Card nằm sau tấm đỡ bo mạch (kiểu "sandwich", nối với bo bằng cáp riser): nhìn từ hông có bo thì bị tản, bo và nguồn
 * che gần hết, phải lắp và xem từ hông bên kia. Hiện chỉ có Mini / SFF; Showcase dùng riser nhưng card nằm sát kính trước.
 */
export const gpuBehindBoard = (f: Family) => !!f.riser && f.gpu.origin[2] < f.board.origin[2]

/** Gốc linh kiện trên bo (tâm bo + toạ độ cục bộ, z theo lưng bo). */
export const onFamilyBoard = (f: Family, p: Mm3): Mm3 => add(f.board.origin, p)

/** Vị trí lắp tính sẵn cho một họ. */
export function familyPlacement(f: Family) {
  const L = BOARD_LAYOUT[f.board.ff]
  const ob = (x: number, y: number, z: number) => onFamilyBoard(f, [x, y, z])
  const cpu = ob(L.cpu[0], L.cpu[1], 4.7)
  const ihs = ob(L.cpu[0], L.cpu[1], 9.2)
  const ram = L.ram_used.map((i) => ob(L.ram_x[i], L.ram_y, 5.5))
  const ssd = ob(L.m2_center[0], L.m2_center[1], 3.5)
  // ổ nguồn 8 chân trên GPU (theo gốc GPU) và tâm dải chân PCIe
  const gpuSocket: Mm3 = f.gpu.kind === 'gpu_280' ? [125, 18.55, 125] : [105, 12.75, 120]
  const gpuFingers: Mm3 = f.gpu.kind === 'gpu_280' ? [-42, 24.7, 0] : [-22, 19.7, 0]
  const psuCenter = f.psu.center as Mm3
  const psuExitLocal: Mm3 = f.psu.kind === 'atx' ? [72, -13, 50] : [52, -13, 35]
  return {
    L,
    cpu,
    ihs,
    ram,
    ssd,
    backplate: ob(L.cpu[0], L.cpu[1], 0),
    gpuSocket: add(f.gpu.origin, rotate(f.gpu.rot, gpuSocket)),
    gpuSocketNormal: rotate(f.gpu.rot, [0, 0, 1]),
    gpuFingers: add(f.gpu.origin, rotate(f.gpu.rot, gpuFingers)),
    psuExit: add(psuCenter, rotate(f.psu.rot, psuExitLocal)),
    header: (h: [number, number], z: number) => ob(h[0], h[1], z),
  }
}

export const MODEL_URL = {
  case: (id: FamilyId) => `/models/f_case_${id}.glb`,
  board: { ATX: '/models/g_motherboard.glb', mATX: '/models/f_motherboard_matx.glb', ITX: '/models/f_motherboard_itx.glb' } as Record<FF, string>,
  cpu: '/models/g_cpu.glb',
  ram: '/models/g_ram.glb',
  ssd: '/models/g_ssd.glb',
  backplate: '/models/g_aio_backplate.glb',
  pump: '/models/g_aio_pump.glb',
  gpu: { gpu_280: '/models/g_gpu.glb', gpu_240: '/models/f_gpu_240_dual.glb' },
  psu: { atx: '/models/g_psu.glb', sfx: '/models/f_psu_sfx_100.glb' },
  cooler: { tower_air_155: '/models/f_tower_air_155.glb', low_profile_47: '/models/f_low_profile_47.glb' },
  radiator: { aio_240: '/models/g_aio_radiator.glb', aio_360: '/models/f_aio_360.glb' },
  fan: { fan120: '/models/f_fan120.glb', fan140: '/models/f_fan140.glb', fan120_argb: '/models/f_fan120_argb.glb' },
  riser: '/models/f_riser_ends.glb',
  stand: '/models/f_console_stand.glb',
  hub: '/models/f_fan_hub_8.glb',
}

/** Đầu ống nước trên két (theo gốc két). */
export const RAD_FITTING_X = { aio_240: 129, aio_360: 189 }

/** Mặt sàn (đơn vị app) dưới chân / đế của họ case. */
export function familyFloorY(id: FamilyId) {
  const raw = familiesJson as unknown as Record<string, { body: number[]; feet?: { at: number[]; size: number[]; shape: string }[]; stand?: { origin: number[] } }>
  const f = raw[id]
  if (f.stand) return (f.stand.origin[1] - 9) / 100
  const feet = f.feet ?? []
  const low = feet.length ? Math.min(...feet.map((t) => t.at[1] - (t.shape === 'round' ? t.size[1] : t.size[1]) / 2)) : -f.body[1] / 2
  return low / 100
}
