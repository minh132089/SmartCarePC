import { useEffect, useRef, useState } from 'react'
import { ASSEMBLY, STEP_MS, STEPS } from '../data/assembly'
import { useViewer } from '../store'

/**
 * Chạy tách / lắp lần lượt từng bước theo thứ tự lắp máy (data/assembly.ts) và hiện tên bước đang làm.
 * Bấm lại giữa chừng thì quay đầu ngay từ bước hiện tại.
 */
export function AssemblySteps() {
  const exploded = useViewer((s) => s.exploded)
  const step = useViewer((s) => s.step)
  const setStep = useViewer((s) => s.setStep)
  const target = exploded ? STEPS : 0

  useEffect(() => {
    if (step === target) return
    const dir = target > step ? 1 : -1
    // Bước đầu tiên chạy ngay khi bấm, các bước sau chờ bước trước xong
    const first = (dir > 0 && step === 0) || (dir < 0 && step === STEPS)
    const t = setTimeout(() => setStep(step + dir), first ? 0 : STEP_MS)
    return () => clearTimeout(t)
  }, [step, target, setStep])

  let text: string | null = null
  if (exploded && step > 0) {
    text = step === STEPS ? 'Đã tháo xong. Bấm "Lắp lại" để lắp theo đúng thứ tự.' : `Tháo bước ${step}/${STEPS}: ${ASSEMBLY[STEPS - step].remove}`
  } else if (!exploded && step < STEPS) {
    const n = STEPS - step // bước vừa lắp vào
    text = `Lắp bước ${n}/${STEPS}: ${ASSEMBLY[n - 1].install}`
  }
  // Bước lắp cuối cùng (dây cáp) vẫn hiện thêm một lúc rồi mới ẩn
  const [hold, setHold] = useState(false)
  const prev = useRef(step)
  useEffect(() => {
    const was = prev.current
    prev.current = step
    if (exploded || step !== 0 || was !== 1) return
    setHold(true)
    const t = setTimeout(() => setHold(false), 1600)
    return () => clearTimeout(t)
  }, [exploded, step])
  if (!exploded && step === 0 && !hold) text = null
  if (!text) return null
  return (
    <div className="pointer-events-none absolute top-4 left-0 z-10 flex w-full justify-center" aria-live="polite">
      <div key={text} className="swap glass rounded-full bg-white/80 px-4 py-2 text-[14px] font-medium text-ink shadow-[0_6px_24px_rgba(0,0,0,0.10)] ring-1 ring-black/5 backdrop-blur-xl">
        {text}
      </div>
    </div>
  )
}
