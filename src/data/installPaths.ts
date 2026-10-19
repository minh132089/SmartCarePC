import type { FamilyId } from './families'
import table from './installPaths.json'

export type Machine = FamilyId | 'gaming'
export type Vec3 = [number, number, number]

/**
 * Đường lắp của từng khối (offset `explode` từ chỗ lắp ra chỗ chờ, đơn vị app), theo máy và mã khối "<bước>.<thứ tự>".
 * Bảng nằm trong installPaths.json, do `npm run check:paths -- --fix` ghi: mỗi đường ở đây đã qua kiểm tra "linh kiện
 * không đi xuyên qua nhau" (src/scene/pathCheck.ts). [0, 0, 0] = không có đường thẳng sạch nên khối hiện ngay tại chỗ.
 * Khối nào không có ở đây dùng offset mặc định viết trong FamilyRig / GamingRig, và cũng phải qua kiểm tra đó.
 */
export const INSTALL_PATHS = table as unknown as Partial<Record<Machine, Record<string, Vec3>>>

export function installPath(machine: Machine, key: string, fallback: Vec3): Vec3 {
  return INSTALL_PATHS[machine]?.[key] ?? fallback
}
