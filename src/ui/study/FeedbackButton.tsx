import { ChatCircleText } from '@phosphor-icons/react'
import { useStudy } from './study'
import { focus } from './StudyParts'

/**
 * Nút góp ý luôn có trên mọi trang, cho mọi người dùng (không cần mã nghiên cứu, không cần làm xong chức năng nào).
 * compact: chỉ có biểu tượng (bảng bên trái trang 3D hẹp). Còn lại thì màn hẹp chỉ hiện biểu tượng, màn rộng có chữ.
 */
export function FeedbackButton({ compact = false, className = '' }: { compact?: boolean; className?: string }) {
  const open = useStudy((s) => s.open)
  return (
    <button
      onClick={() => open('feedback')}
      aria-label="Send feedback"
      title="Send feedback"
      className={`flex shrink-0 items-center gap-1.5 rounded-full bg-fill py-1.5 text-[13.5px] font-medium text-ink hover:bg-fill-hover ${compact ? 'px-2' : 'px-2 sm:px-3'} ${focus} ${className}`}
    >
      <ChatCircleText size={16} aria-hidden />
      {!compact && <span className="hidden sm:inline">Feedback</span>}
    </button>
  )
}
