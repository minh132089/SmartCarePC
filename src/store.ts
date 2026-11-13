import { create } from 'zustand'
import type { AssemblyKey } from './data/assembly'
import type { BuildId } from './data/builds'
import type { FamilyId } from './data/families'
import type { PartId } from './data/parts'
import type { Cooler, MaintenanceTask } from './data/maintenance'
import type { BuildCard } from './ui/chat/chatStore'
import { detectQuality, LOWER, loadPref, savePref, type QualityLevel, type QualityPref } from './perf/quality'

export type Page = 'home' | 'advice' | 'build' | 'upgrade'

interface ViewerState {
  /**
   * Trang đang mở. home: chọn một trong ba hướng (lắp máy mới, bảo dưỡng, nâng cấp). advice: tư vấn chọn máy mới,
   * build: mô hình 3D kèm hướng dẫn lắp / bảo dưỡng, upgrade: tư vấn nâng cấp máy đang có.
   */
  page: Page
  setPage: (page: Page) => void
  buildId: BuildId
  /** Cấu hình người dùng thực sự chọn từ tư vấn; null khi xem máy mẫu độc lập. */
  advisedBuild: BuildCard | null
  setAdvisedBuild: (card: BuildCard) => void
  /** Họ case đang xem; null = máy Gaming đầy đủ (buildId) */
  familyId: FamilyId | null
  exploded: boolean
  /** Số bước đã tháo (0 = máy nguyên, STEPS = tháo hết); chạy dần tới đích theo `exploded` */
  step: number
  setStep: (n: number) => void
  sideOpen: boolean
  selected: PartId | null
  hovered: PartId | null
  inspecting: PartId | null
  /** "Bật máy": quạt quay, cảnh vẽ liên tục. Tắt thì cảnh đứng yên và chỉ vẽ lại khi có thay đổi. */
  powered: boolean
  togglePower: () => void
  /** Đang mở hướng dẫn lắp từng bước: bảng bên trái thành danh sách bước, hình 3D lắp theo bước đang xem */
  guide: boolean
  guideKind: 'assembly' | 'maintenance'
  maintenanceTask: MaintenanceTask
  maintenanceCooler: Cooler
  maintenanceRunId: string
  maintenanceHasGpu: boolean
  maintenanceLift: boolean
  /** Bước đang xem trong hướng dẫn (chỉ số trong guideSteps) */
  guideIndex: number
  /**
   * Linh kiện được lắp ở bước đang xem. Trong hướng dẫn, linh kiện của các bước sau chưa hiện:
   * tới bước nào thì linh kiện của bước đó mới xuất hiện (xem Part.tsx).
   */
  guidePart: AssemblyKey | null
  /** "Xem toàn cảnh": hiện cả máy đã lắp đủ thay cho trạng thái của bước đang xem */
  guideFull: boolean
  /** Chỉ dùng khi kiểm tra đường lắp (scene/pathCheck.ts): linh kiện nhảy ngay tới đích, không chạy lò xo */
  instantMotion: boolean
  setGuidePart: (part: AssemblyKey | null) => void
  setGuideFull: (on: boolean) => void
  setGuide: (on: boolean) => void
  setGuideIndex: (i: number) => void
  /** Lựa chọn chất lượng của người dùng và mức đang dùng thực tế */
  qualityPref: QualityPref
  quality: QualityLevel
  /** Mức tự đoán lúc mở trang, mốc cho chế độ tự động */
  detected: QualityLevel
  setQualityPref: (p: QualityPref) => void
  /** Chế độ tự động: hạ một mức khi đo thấy khung hình thấp */
  lowerQuality: () => void
  setBuild: (id: BuildId) => void
  setFamily: (id: FamilyId) => void
  toggleExploded: () => void
  toggleSide: () => void
  select: (id: PartId | null) => void
  hover: (id: PartId | null) => void
  inspect: (id: PartId | null) => void
}

export const useViewer = create<ViewerState>((set) => ({
  page: 'home',
  setPage: (page) => set({ page }),
  buildId: 'gaming',
  advisedBuild: null,
  setAdvisedBuild: (advisedBuild) => set({ advisedBuild }),
  familyId: null,
  exploded: false,
  step: 0,
  sideOpen: true,
  selected: null,
  hovered: null,
  inspecting: null,
  powered: false,
  togglePower: () => set((s) => ({ powered: !s.powered })),
  guide: false,
  guideKind: 'assembly',
  maintenanceTask: 'dust',
  maintenanceCooler: 'unknown',
  maintenanceLift: false,
  maintenanceHasGpu: true,
  maintenanceRunId: '',
  guideIndex: 0,
  // Mở hay đóng hướng dẫn đều đưa máy về trạng thái lắp đủ (exploded = false): đóng thì máy tự lắp lại từng bước
  // Bước 1 của hướng dẫn lắp là tháo nắp hông: mở hướng dẫn thì nắp mở sẵn (vừa xem bảo dưỡng hay vừa bấm đóng nắp thì nắp
  // còn đóng, trong khi màn hướng dẫn trước đây không có nút mở lại)
  setGuide: (guide) => set({ guide, guideKind: 'assembly', maintenanceLift: false, step: 0, guideFull: false, guidePart: null, exploded: false, selected: null, inspecting: null, ...(guide ? { sideOpen: true } : {}) }),
  // Chọn một bước thì thoát toàn cảnh để hình quay về đúng bước đó
  setGuideIndex: (guideIndex) => set({ guideIndex, guideFull: false }),
  guidePart: null,
  guideFull: false,
  instantMotion: false,
  setGuidePart: (guidePart) => set({ guidePart }),
  setGuideFull: (guideFull) => set({ guideFull }),
  ...initialQuality(),
  setQualityPref: (qualityPref) => {
    savePref(qualityPref)
    set((s) => ({ qualityPref, quality: qualityPref === 'auto' ? s.detected : qualityPref }))
  },
  lowerQuality: () => set((s) => (s.qualityPref === 'auto' && LOWER[s.quality] ? { quality: LOWER[s.quality]! } : {})),
  setBuild: (buildId) => set({ buildId, familyId: null, advisedBuild: null, selected: null, exploded: false, step: 0, inspecting: null }),
  setFamily: (familyId) => set({ familyId, advisedBuild: null, selected: null, exploded: false, step: 0, inspecting: null }),
  toggleExploded: () => set((s) => ({ exploded: !s.exploded })),
  setStep: (step) => set({ step }),
  toggleSide: () => set((s) => ({ sideOpen: !s.sideOpen })),
  select: (selected) => set({ selected }),
  hover: (hovered) => set({ hovered }),
  inspect: (inspecting) => set(inspecting ? { inspecting, selected: inspecting } : { inspecting: null }),
}))

function initialQuality() {
  const detected = detectQuality()
  const qualityPref = loadPref()
  return { detected, qualityPref, quality: qualityPref === 'auto' ? detected : qualityPref }
}
