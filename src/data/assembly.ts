import { useViewer } from '../store'

/**
 * Thứ tự lắp máy (bo mạch và vỏ đã nằm sẵn). "Tách rời" tháo ngược từ bước cuối về bước đầu,
 * "Lắp lại" lắp xuôi từ bước đầu, mỗi lần một bước.
 */
export const ASSEMBLY = [
  { key: 'psu', install: 'Lắp nguồn vào case', remove: 'Tháo nguồn' },
  { key: 'cpu', install: 'Đặt CPU vào socket', remove: 'Nhấc CPU ra' },
  { key: 'ram', install: 'Cắm RAM', remove: 'Rút RAM' },
  { key: 'ssd', install: 'Lắp ổ SSD M.2', remove: 'Tháo ổ SSD' },
  { key: 'cooler', install: 'Bôi keo, lắp tản nhiệt', remove: 'Tháo tản nhiệt' },
  { key: 'gpu', install: 'Cắm card đồ họa', remove: 'Rút card đồ họa' },
  { key: 'fans', install: 'Bắt quạt case', remove: 'Tháo quạt case' },
  { key: 'cables', install: 'Cắm và gom dây', remove: 'Rút dây cáp' },
] as const

export type AssemblyKey = (typeof ASSEMBLY)[number]['key']
export const STEPS = ASSEMBLY.length
/** Mỗi bước chờ bước trước chạy xong (lò xo ~0,45 s) rồi mới bắt đầu. */
export const STEP_MS = 620

const index = (k: AssemblyKey) => ASSEMBLY.findIndex((s) => s.key === k) + 1

/** Bước k đang ở ngoài máy khi số bước đã tháo (`step`) đủ để tới nó: tháo từ bước cuối. */
export const isOut = (step: number, k: AssemblyKey) => step >= STEPS - index(k) + 1

export function useOut(k: AssemblyKey) {
  return useViewer((s) => isOut(s.step, k))
}
