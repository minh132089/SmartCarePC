import { useEffect, useState } from 'react'
import { ArrowClockwise, X } from '@phosphor-icons/react'

const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'
const EVERY = 5 * 60_000

/** File JS chính của bản đang chạy ("/assets/index-xxxx.js"); null khi chạy dev server (không có bản build) */
function runningBundle() {
  return document.querySelector<HTMLScriptElement>('script[type="module"][src*="/assets/index-"]')?.getAttribute('src') ?? null
}

/**
 * Web vừa deploy bản mới mà tab này vẫn chạy bản cũ (nhóm thử nghiệm hay để tab mở cả ngày, rồi báo lỗi đã sửa): mỗi 5 phút và
 * mỗi lần quay lại tab thì đọc lại trang chủ, tên file JS chính khác thì mời tải lại. Không tự tải lại vì sẽ mất cuộc trò chuyện
 * tư vấn đang mở
 */
export function UpdateBanner() {
  const [stale, setStale] = useState(false)
  const [hidden, setHidden] = useState(false)
  useEffect(() => {
    const mine = runningBundle()
    if (!mine) return
    let stop = false
    const check = async () => {
      try {
        const html = await (await fetch('/', { cache: 'no-store' })).text()
        const live = html.match(/\/assets\/index-[\w-]+\.js/)?.[0]
        if (!stop && live && !mine.endsWith(live)) setStale(true)
      } catch {
        /* mất mạng: lần sau thử lại */
      }
    }
    const onVisible = () => document.visibilityState === 'visible' && void check()
    const timer = setInterval(check, EVERY)
    document.addEventListener('visibilitychange', onVisible)
    return () => {
      stop = true
      clearInterval(timer)
      document.removeEventListener('visibilitychange', onVisible)
    }
  }, [])
  if (!stale || hidden) return null
  return (
    <div role="status" className="fixed bottom-4 left-4 z-[70] flex max-w-[calc(100%-2rem)] items-center gap-2 rounded-2xl bg-panel py-2 pr-2 pl-4 text-[14px] text-ink shadow-[0_12px_40px_rgba(0,0,0,0.18)] ring-1 ring-line">
      <span>A newer version of the app is available.</span>
      <button onClick={() => location.reload()} className={`flex shrink-0 items-center gap-1.5 rounded-full bg-accent px-3 py-1.5 font-medium text-white hover:bg-accent-hover ${focus}`}>
        <ArrowClockwise size={14} aria-hidden /> Reload
      </button>
      <button onClick={() => setHidden(true)} aria-label="Dismiss" className={`grid size-8 shrink-0 place-items-center rounded-full text-ink-2 hover:bg-fill ${focus}`}>
        <X size={14} aria-hidden />
      </button>
    </div>
  )
}
