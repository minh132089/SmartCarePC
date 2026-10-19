import type { AssemblyKey } from './assembly'
import type { Machine, Vec3 } from './installPaths'

/**
 * Hướng nhìn riêng cho bước lắp mà góc mặc định bị che (hướng từ tâm máy ra camera, độ dài không quan trọng).
 * Mỗi hướng chọn bằng đo tia: cách nhìn thấy linh kiện của bước đó nhiều nhất, cả ở chỗ chờ lẫn khi đã lắp.
 * Tới bước đó camera bay vòng quanh máy sang hướng này; rời bước thì quay về góc người dùng đang xem trước đó.
 */
export const GUIDE_VIEWS: Partial<Record<Machine, Partial<Record<AssemblyKey, Vec3>>>> = {
  // Card nằm sau tấm đỡ bo (sandwich): nhìn từ sau-phải, hông có tấm đỡ phía sau; thấy ~96% (góc mặc định 0–8%).
  // Nguồn của Mini nằm ngay hông mở nên góc mặc định đã thấy 99%.
  mini_sff: { gpu: [0.5, 0.3, -0.85] },
  // Nguồn nằm trong ngăn dưới có tấm che: góc mặc định thấy 0%. Nhìn từ mặt lưng, hơi thấp (cũng là phía bắt vít nguồn)
  standard_tower: { psu: [-0.92, 0.15, -0.38] }, // thấy 64%
  compact_tower: { psu: [-0.92, 0.15, -0.38] }, // 66%
  showcase_dual_chamber: { psu: [-1, 0.15, 0] }, // 79%
  full_tower: { psu: [-0.92, 0.15, -0.38] }, // 62%
  gaming: { psu: [-1, 0.15, 0] }, // 76%
}

export const guideView = (machine: Machine, part: AssemblyKey | null): Vec3 | null =>
  (part && GUIDE_VIEWS[machine]?.[part]) || null
