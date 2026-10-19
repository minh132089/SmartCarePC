import { create } from 'zustand'
import type { AssemblyKey } from './data/assembly'
import type { BuildId } from './data/builds'
import type { FamilyId } from './data/families'
import type { PartId } from './data/parts'
import { detectQuality, LOWER, loadPref, savePref, type QualityLevel, type QualityPref } from './perf/quality'

interface ViewerState {
  /** Trang đang mở: tư vấn chọn máy trước, rồi tới trang hướng dẫn (mô hình 3D) */
  page: 'advice' | 'build'
  setPage: (page: 'advice' | 'build') => void
  buildId: BuildId
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
  page: 'advice',
  setPage: (page) => set({ page }),
  buildId: 'gaming',
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
  guideIndex: 0,
  // Mở hay đóng hướng dẫn đều đưa máy về trạng thái lắp đủ (exploded = false): đóng thì máy tự lắp lại từng bước
  setGuide: (guide) => set({ guide, guideFull: false, guidePart: null, exploded: false, selected: null, inspecting: null }),
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
  setBuild: (buildId) => set({ buildId, familyId: null, selected: null, exploded: false, step: 0, inspecting: null }),
  setFamily: (familyId) => set({ familyId, selected: null, exploded: false, step: 0, inspecting: null }),
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
