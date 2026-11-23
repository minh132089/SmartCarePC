import { createContext, useContext } from 'react'
import type { PartId } from '../data/parts'
import { useViewer } from '../store'

export const PartContext = createContext<PartId | null>(null)

/** Trạng thái chọn / rê chuột của linh kiện đang bao quanh. */
export function usePartState() {
  const id = useContext(PartContext)
  const selected = useViewer((s) => id !== null && s.selected === id)
  const hovered = useViewer((s) => id !== null && s.hovered === id)
  return { selected, hovered }
}

/** Hướng dẫn bảo dưỡng: máy đã tắt và rút điện thì mọi đèn RGB trên mô hình tắt theo (trừ bước làm khi máy chạy, vd nghe quạt) */
export function useLightsOff() {
  return useViewer((s) => s.guide && s.guideKind === 'maintenance' && !s.powered)
}
