import { useBounds } from '@react-three/drei'
import { useEffect } from 'react'
import { useViewer } from '../store'
import { GlbModel } from './Models'
import { Part } from './Part'

/**
 * Bộ đồ nghề lắp máy (tools/build_tools.py, bản vẽ design/tools/): thảm 600 × 400 bày trên sàn trước mặt máy.
 * Chỉ hiện khi tách rời linh kiện hoặc khi đang chọn "Đồ nghề lắp máy", để cảnh xem máy thường vẫn gọn.
 * frontX: mặt trước thân máy (đơn vị cảnh), floorY: mặt sàn.
 */
export function ToolKit({ frontX, floorY }: { frontX: number; floorY: number }) {
  const show = useViewer((s) => s.exploded || s.selected === 'tools')
  const bounds = useBounds()
  // Thảm xuất hiện / biến mất thì canh lại khung nhìn cho vừa cả máy lẫn thảm
  useEffect(() => {
    const t = setTimeout(() => bounds.refresh().clip().fit(), 80)
    return () => clearTimeout(t)
  }, [show, bounds])
  if (!show) return null
  // Mép dài của thảm song song mặt trước máy; số 1 (tua vít) ở bên trái khi đứng trước máy nhìn vào
  return (
    <Part id="tools" position={[frontX + 0.5 + 2, floorY + 0.03, 0]}>
      <group rotation-y={Math.PI / 2}>
        <GlbModel url="/models/t_kit.glb" />
      </group>
    </Part>
  )
}
