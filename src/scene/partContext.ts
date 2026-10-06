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
