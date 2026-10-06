import { useFrame, useThree } from '@react-three/fiber'
import { useEffect, useRef } from 'react'
import { useViewer } from '../store'

/**
 * Cảnh chạy ở chế độ "chỉ vẽ khi cần" (frameloop="demand"): đứng yên thì không tốn GPU.
 * Khối này đánh thức cảnh khi có thay đổi mà three.js không tự biết:
 * - đổi trạng thái app (chọn, rê chuột, tách / lắp, đổi máy...): vẽ tiếp ~0,9 s cho hoạt cảnh chạy hết;
 * - "Bật máy": vẽ liên tục để quạt quay.
 */
export function RenderControl({ watchFps = true }: { watchFps?: boolean }) {
  const invalidate = useThree((s) => s.invalidate)
  const until = useRef(0)
  useEffect(() => {
    const kick = () => {
      until.current = performance.now() + 900
      invalidate()
    }
    kick()
    return useViewer.subscribe(kick)
  }, [invalidate])
  useFrame(() => {
    if (useViewer.getState().powered || performance.now() < until.current) invalidate()
  })
  return watchFps ? <FpsWatch /> : null
}

/**
 * Đo khung hình khi cảnh đang chạy liên tục (kéo xoay, tách lắp, máy đang bật). Trung bình dưới 28 khung/giây
 * trong ~3 giây thì hạ một mức chất lượng (chỉ ở chế độ tự động). Bỏ qua quãng nghỉ của chế độ vẽ khi cần.
 */
function FpsWatch() {
  const acc = useRef({ t: 0, n: 0, last: 0 })
  useFrame(() => {
    const now = performance.now()
    const a = acc.current
    const dt = now - a.last
    a.last = now
    if (dt > 100) return // khung đầu sau quãng nghỉ: không tính
    a.t += dt
    a.n += 1
    if (a.n < 90) return
    const fps = (1000 * a.n) / a.t
    a.t = 0
    a.n = 0
    if (fps < 28) useViewer.getState().lowerQuality()
  })
  return null
}
