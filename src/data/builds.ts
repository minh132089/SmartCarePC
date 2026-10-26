// Đơn vị trong cảnh 3D: 1 = 100 mm.

export type FormFactor = 'ATX' | 'mATX' | 'ITX'
export type CoolerKind = 'lowprofile' | 'stock' | 'tower' | 'aio'
export type BuildId = 'office' | 'study' | 'gaming' | 'workstation'

// Kích thước chuẩn của bo mạch chủ. Đây là chuẩn chung của ngành, không thuộc hãng nào.
export const BOARDS: Record<FormFactor, { w: number; h: number; ramSlots: number; pcieSlots: number; label: string }> = {
  ATX: { w: 2.44, h: 3.05, ramSlots: 4, pcieSlots: 3, label: 'ATX (305 × 244 mm)' },
  mATX: { w: 2.44, h: 2.44, ramSlots: 4, pcieSlots: 2, label: 'Micro-ATX (244 × 244 mm)' },
  ITX: { w: 1.7, h: 1.7, ramSlots: 2, pcieSlots: 1, label: 'Mini-ITX (170 × 170 mm)' },
}

export interface Build {
  id: BuildId
  label: string
  tagline: string
  caseName: string
  formFactor: FormFactor
  case: { w: number; h: number; d: number; glass: boolean; body: string; front: string }
  cooler: CoolerKind
  ramSticks: number
  gpu: { length: number; height: number; thick: number; fans: number } | null
  psu: 'ATX' | 'SFX'
  frontFans: { count: number; size: number }
  rearFan: number
  accent: string | null
}

export const BUILDS: Build[] = [
  {
    id: 'office',
    label: "Office",
    tagline: "Compact and quiet, with enough power for documents, email and video calls.",
    caseName: "Compact case",
    formFactor: 'ITX',
    case: { w: 1.7, h: 2.9, d: 3.2, glass: false, body: '#d9dde2', front: '#c3c8cf' },
    cooler: 'lowprofile',
    ramSticks: 2,
    gpu: null,
    psu: 'SFX',
    frontFans: { count: 0, size: 1.2 },
    rearFan: 0.8,
    accent: null,
  },
  {
    id: 'study',
    label: "Study",
    tagline: "A balanced PC for online classes and coursework, with room to upgrade.",
    caseName: "Basic mid-tower",
    formFactor: 'mATX',
    case: { w: 2.0, h: 4.0, d: 3.9, glass: false, body: '#2b2f36', front: '#23262c' },
    cooler: 'stock',
    ramSticks: 2,
    gpu: null,
    psu: 'ATX',
    frontFans: { count: 1, size: 1.2 },
    rearFan: 1.2,
    accent: null,
  },
  {
    id: 'gaming',
    label: 'Gaming',
    tagline: "Built for graphics, with plenty of cooling and a glass side panel.",
    caseName: "Glass-panel mid-tower",
    formFactor: 'ATX',
    case: { w: 2.2, h: 4.7, d: 4.5, glass: true, body: '#16181d', front: '#101216' },
    cooler: 'aio',
    ramSticks: 2,
    gpu: { length: 2.8, height: 1.25, thick: 0.55, fans: 3 },
    psu: 'ATX',
    frontFans: { count: 3, size: 1.4 },
    rearFan: 1.2,
    accent: '#22d3ee',
  },
  {
    id: 'workstation',
    label: "Creative work",
    tagline: "For video editing, 3D work and demanding tasks. A large case with plenty of memory and airflow.",
    caseName: 'Full-tower',
    formFactor: 'ATX',
    case: { w: 2.4, h: 5.4, d: 5.0, glass: false, body: '#3a3f47', front: '#30343b' },
    cooler: 'tower',
    ramSticks: 4,
    gpu: { length: 3.0, height: 1.35, thick: 0.6, fans: 3 },
    psu: 'ATX',
    frontFans: { count: 2, size: 1.4 },
    rearFan: 1.4,
    accent: null,
  },
]

export const getBuild = (id: BuildId) => BUILDS.find((b) => b.id === id)!
