import type { ReactNode } from 'react'
import { Fade } from '@animate-ui/react/primitives/effects/fade'
import { Slide } from '@animate-ui/react/primitives/effects/slide'

/**
 * Hiện dần và trượt nhẹ từ dưới lên khi xuất hiện (Animate UI Slide + Fade). Chỉ chạy lúc gắn vào, không chạy lại khi
 * cập nhật. Khi hệ thống bật "giảm chuyển động", Animate UI tự bỏ phần trượt và chỉ giữ hiện dần.
 * delay tính bằng mili-giây, dùng để các phần tử nối nhau chứ không nhảy ra cùng lúc.
 */
export function Rise({ children, delay = 0, offset = 14, className }: { children: ReactNode; delay?: number; offset?: number; className?: string }) {
  return (
    <Slide direction="up" offset={offset} delay={delay} className={className}>
      <Fade delay={delay} className={className}>
        {children}
      </Fade>
    </Slide>
  )
}
