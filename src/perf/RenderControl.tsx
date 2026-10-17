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
  // Khung trước có tự xin vẽ tiếp không: nếu có thì khoảng cách tới khung này là thời gian vẽ thật, không phải quãng nghỉ
  const chained = useRef(false)
  const fps = useRef({ t: 0, n: 0, last: 0 })
  useEffect(() => {
    const kick = () => {
      until.current = performance.now() + 900
      invalidate()
    }
    kick()
    return useViewer.subscribe(kick)
  }, [invalidate])
  useFrame(() => {
    const now = performance.now()
    if (watchFps) watch(fps.current, now, chained.current)
    chained.current = useViewer.getState().powered || now < until.current
    if (chained.current) invalidate()
  })
  return null
}

/**
 * Đo khung hình khi cảnh đang chạy liên tục (kéo xoay, tách lắp, máy đang bật). Trung bình dưới 28 khung/giây
 * trong ~2 giây thì hạ một mức chất lượng (chỉ ở chế độ tự động). Đo theo thời gian chứ không theo số khung,
 * để máy chỉ chạy được 8–10 khung/giây cũng được hạ sau 2 giây thay vì phải chịu giật gần 10 giây.
 * Bỏ qua quãng nghỉ của chế độ vẽ khi cần: khung trước không tự xin vẽ tiếp và cách hơn 250 ms.
 */
function watch(a: { t: number; n: number; last: number }, now: number, chained: boolean) {
  const dt = now - a.last
  a.last = now
  // Khung đầu sau quãng nghỉ, tab đang ẩn, hoặc vừa quay lại tab (trình duyệt dừng vẽ khi tab ẩn): không tính
  if ((!chained && dt > 250) || dt > 1000 || document.hidden) return
  a.t += dt
  a.n += 1
  if (a.t < 2000 || a.n < 8) return
  const fps = (1000 * a.n) / a.t
  a.t = 0
  a.n = 0
  if (fps < 28) useViewer.getState().lowerQuality()
}
