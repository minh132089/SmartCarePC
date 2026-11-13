import { ArrowLeft } from '@phosphor-icons/react'
import { useViewer } from '../store'

const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'

/**
 * Nút "Back" về màn chọn ba chức năng (Build / Maintain / Upgrade), cho người bấm nhầm chức năng. Tiến độ đang làm (câu
 * tư vấn, bước đã đánh dấu, lần bảo dưỡng dở) vẫn giữ, quay lại là thấy tiếp.
 */
export function BackHome({ className = '' }: { className?: string }) {
  const setPage = useViewer((s) => s.setPage)
  return (
    <button
      onClick={() => setPage('home')}
      aria-label="Back to the start page"
      title="Back to the start page"
      className={`flex shrink-0 items-center gap-1 rounded-full bg-fill py-1.5 pr-3 pl-2 text-[13.5px] font-medium text-ink hover:bg-fill-hover ${focus} ${className}`}
    >
      <ArrowLeft size={15} aria-hidden /> Back
    </button>
  )
}
