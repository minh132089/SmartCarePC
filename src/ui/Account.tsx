import { useEffect } from 'react'
import { SignIn } from '@phosphor-icons/react'
import { create } from 'zustand'
import { AccountMenu } from './AccountMenu'
import { AuthPage, clearSession, loadSession, type Session } from './AuthPage'

/**
 * Tài khoản là tuỳ chọn: ai chưa đăng nhập vẫn dùng đủ mọi trang. Màn đăng nhập mở dạng lớp phủ phía trên trang đang xem
 * (không đổi trang), nên form đang điền, cuộc trò chuyện với trợ lý hay buổi hướng dẫn bảo dưỡng đều còn nguyên khi đóng.
 * Phiên chỉ nằm ở trình duyệt, server không kiểm tra đăng nhập khi gọi các API khác.
 */
interface SessionState {
  session: Session | null
  authOpen: boolean
  login: (session: Session) => void
  logout: () => void
  openAuth: () => void
  closeAuth: () => void
}

export const useSession = create<SessionState>((set) => ({
  session: loadSession(),
  authOpen: false,
  login: (session) => set({ session, authOpen: false }),
  logout: () => {
    clearSession()
    set({ session: null })
  },
  openAuth: () => set({ authOpen: true }),
  closeAuth: () => set({ authOpen: false }),
}))

/**
 * Góc tài khoản trên mỗi trang: đã đăng nhập thì hiện menu (tên, email, đăng xuất), chưa thì hiện nút "Đăng nhập".
 * hideSignIn dùng ở trang mô hình 3D để không có nút dễ bấm nhầm khi đang giữa buổi hướng dẫn.
 */
export function AccountSlot({ hideSignIn = false }: { hideSignIn?: boolean }) {
  const session = useSession((s) => s.session)
  const logout = useSession((s) => s.logout)
  const openAuth = useSession((s) => s.openAuth)
  if (session) return <AccountMenu session={session} onLogout={logout} />
  if (hideSignIn) return null
  return (
    <button
      onClick={openAuth}
      className="flex items-center gap-1.5 rounded-full bg-fill px-3 py-1.5 text-[13.5px] font-medium text-ink hover:bg-fill-hover focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
    >
      <SignIn size={15} aria-hidden /> Sign in
    </button>
  )
}

/**
 * Website công khai (Cloudflare Pages) bắt đăng nhập trước khi dùng, để phản hồi gắn được với người gửi.
 * localhost và demo qua mạng LAN thì không bắt, cho dễ thử.
 */
export function needsLogin(hostname: string) {
  return hostname.toLowerCase().endsWith('.pages.dev')
}

/**
 * Lớp phủ đăng nhập / đăng ký. Esc hoặc nút "Để sau" đóng lại, đăng nhập xong cũng tự đóng.
 * Trên website công khai mà chưa đăng nhập thì lớp phủ luôn mở và không có nút đóng.
 */
export function AuthOverlay() {
  const session = useSession((s) => s.session)
  const required = !session && needsLogin(location.hostname)
  const open = useSession((s) => s.authOpen) || required
  const login = useSession((s) => s.login)
  const close = useSession((s) => s.closeAuth)
  useEffect(() => {
    if (!open || required) return
    const onKey = (event: KeyboardEvent) => { if (event.key === 'Escape') close() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, required, close])
  if (!open) return null
  return (
    <div role="dialog" aria-modal="true" aria-label="Sign in or create an account" className="fixed inset-0 z-[200] overflow-y-auto bg-[#0b0d14]">
      <AuthPage onLogin={login} onClose={required ? undefined : close} required={required} />
    </div>
  )
}
