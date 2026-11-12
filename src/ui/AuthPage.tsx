import { ArrowRight, CheckCircle, Cpu, Eye, EyeSlash, Sparkle } from '@phosphor-icons/react'
import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'

const SESSION_KEY = 'smartcare-session-v2'

export type Session = { email: string; name: string }

export function loadSession(): Session | null {
  try {
    const raw = localStorage.getItem(SESSION_KEY)
    return raw ? (JSON.parse(raw) as Session) : null
  } catch {
    return null
  }
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY)
}

export function AuthPage({ onLogin, onClose }: { onLogin: (session: Session) => void; onClose?: () => void }) {
  const googleButton = useRef<HTMLDivElement>(null)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [remember, setRemember] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [register, setRegister] = useState(false)
  const [confirmPassword, setConfirmPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const changeMode = (next: boolean) => { if (busy) return; setRegister(next); setError(''); setNotice(''); setPassword(''); setConfirmPassword(''); setShowPassword(false) }

  useEffect(() => {
    let cancelled = false
    const initialize = () => {
      if (cancelled || !googleButton.current || !window.google) return
      window.google.accounts.id.initialize({
        client_id: '666308491003-lasilr6p89lq8b06q6v87tmi2bb29o84.apps.googleusercontent.com',
        callback: async ({ credential }) => {
          try {
            const response = await fetch('/api/auth/google', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ credential }) })
            if (!response.ok) throw new Error('authentication failed')
            const session = await response.json() as Session
            if (remember) localStorage.setItem(SESSION_KEY, JSON.stringify(session))
            onLogin(session)
          } catch { setError('Không thể đăng nhập bằng Google. Vui lòng kiểm tra máy chủ và thử lại.') }
        },
      })
      googleButton.current.replaceChildren()
      window.google.accounts.id.renderButton(googleButton.current, { theme: 'outline', size: 'large', text: 'signin_with', width: 320 })
    }
    if (window.google) initialize()
    else {
      const script = document.createElement('script')
      script.src = 'https://accounts.google.com/gsi/client'
      script.async = true
      script.onload = initialize
      script.onerror = () => { if (!cancelled) setError('Không tải được đăng nhập Google. Vui lòng thử lại.') }
      document.head.appendChild(script)
      return () => { cancelled = true; script.remove() }
    }
    return () => { cancelled = true }
  }, [onLogin, remember])

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    const normalizedEmail = email.trim().toLowerCase()
    if (!normalizedEmail || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
      setError('Vui lòng nhập địa chỉ email hợp lệ.')
      return
    }
    if (password.length < 8 || new TextEncoder().encode(password).length > 72) {
      setError('Mật khẩu cần ít nhất 8 ký tự và không quá 72 byte.')
      return
    }
    if (register && password !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.')
      return
    }
    setBusy(true)
    try {
      const response = await fetch(`/api/auth/${register ? 'register' : 'login'}`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: normalizedEmail, password }) })
      if (!response.ok) {
        setError(response.status === 409 ? 'Email đã được đăng ký.' : response.status === 401 ? 'Email hoặc mật khẩu không chính xác.' : 'Không thể xử lý yêu cầu. Vui lòng thử lại.')
        return
      }
      if (register) {
        setRegister(false)
        setPassword('')
        setConfirmPassword('')
        setNotice('Đăng ký thành công. Vui lòng đăng nhập bằng tài khoản của bạn.')
        return
      }
      const session = await response.json() as Session
      if (remember) localStorage.setItem(SESSION_KEY, JSON.stringify(session))
      onLogin(session)
    } catch { setError('Không kết nối được máy chủ. Vui lòng thử lại sau.') }
    finally { setBusy(false) }
  }

  return (
    <main className="auth-page">
      <div className="auth-layout">
        <section className="auth-story" aria-label="Giới thiệu PC SmartCare">
          <div className="auth-brand"><span className="auth-brand-icon"><Cpu size={21} weight="bold" /></span><span>PC <strong>SmartCare</strong></span></div>
          <div className="auth-story-copy">
            <div className="auth-eyebrow"><Sparkle size={14} weight="fill" /> KHÁM PHÁ THẾ GIỚI PC</div>
            <h1>Chiếc PC của bạn.<br /><span>Hành trình của bạn.</span></h1>
            <p>Từ lựa chọn linh kiện đến lắp ráp hoàn chỉnh — mọi thứ bắt đầu từ đây.</p>
          </div>
          <div className="auth-visual"><img src="/hero/advice-showcase-dark.jpg" alt="Mô hình máy tính PC với các linh kiện bên trong" /><span className="auth-visual-label"><span className="auth-status" /> TƯƠNG TÁC 3D <span> / </span> LẮP MÁY DỄ DÀNG</span></div>
          <div className="auth-benefits"><span><CheckCircle size={17} /> Tư vấn thông minh</span><span><CheckCircle size={17} /> Hướng dẫn trực quan</span></div>
          <div className="auth-story-footer">PC SMARTCARE <span>•</span> DÀNH CHO NGƯỜI YÊU CÔNG NGHỆ</div>
        </section>
        <section className="auth-form-panel" aria-label={register ? 'Đăng ký' : 'Đăng nhập'}>
          <div className="auth-mobile-brand"><span className="auth-brand-icon"><Cpu size={19} weight="bold" /></span> PC <strong>SmartCare</strong></div>
          <div className="auth-form-content">
            <div className="auth-form-kicker"><span /> TÀI KHOẢN PC SMARTCARE</div>
            <h2>{register ? 'Tạo tài khoản' : 'Chào mừng trở lại'}<span>.</span></h2>
            <p className="auth-intro">{register ? 'Bắt đầu hành trình xây dựng chiếc PC của riêng bạn.' : 'Đăng nhập để bắt đầu khám phá và tự tin xây dựng chiếc PC của riêng bạn.'}</p>
            <div className="auth-tabs" role="group" aria-label="Chọn hình thức tài khoản"><button type="button" aria-pressed={!register} onClick={() => changeMode(false)}>Đăng nhập</button><button type="button" aria-pressed={register} onClick={() => changeMode(true)}>Đăng ký</button></div>
            <div ref={googleButton} className="auth-google" />
            <div className="auth-divider"><span />hoặc tiếp tục với email<span /></div>
            <form onSubmit={submit} className="auth-form" noValidate>
              <label>Email<input value={email} onChange={(e) => { setEmail(e.target.value); setError('') }} type="email" autoComplete="email" placeholder="ban@example.com" /></label>
              <label>Mật khẩu<span className="auth-password"><input value={password} onChange={(e) => { setPassword(e.target.value); setError('') }} type={showPassword ? 'text' : 'password'} autoComplete={register ? 'new-password' : 'current-password'} placeholder={register ? 'Tối thiểu 8 ký tự' : 'Nhập mật khẩu của bạn'} /><button type="button" onClick={() => setShowPassword(!showPassword)} aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} aria-pressed={showPassword}>{showPassword ? <EyeSlash size={19} /> : <Eye size={19} />}</button></span></label>
              {register && <label>Xác nhận mật khẩu<input value={confirmPassword} onChange={(e) => { setConfirmPassword(e.target.value); setError('') }} type="password" autoComplete="new-password" placeholder="Nhập lại mật khẩu" /></label>}
              {error && <p role="alert" className="auth-error">{error}</p>}
              {notice && <p role="status" className="auth-notice">{notice}</p>}
              {!register && <label className="auth-remember"><input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} /> Ghi nhớ đăng nhập trên thiết bị này</label>}
              <button type="submit" disabled={busy} className="auth-submit">{busy ? 'Đang xử lý…' : register ? 'Tạo tài khoản' : 'Đăng nhập'} <ArrowRight size={18} /></button>
              <p className="auth-demo">{register ? 'Bạn đã có tài khoản?' : 'Bạn chưa có tài khoản?'} <button type="button" onClick={() => changeMode(!register)}>{register ? 'Đăng nhập' : 'Đăng ký ngay'}</button></p>
              {onClose && <button type="button" onClick={onClose} className="auth-skip">Để sau, quay lại xem trang</button>}
            </form>
          </div>
          <p className="auth-footer">© {new Date().getFullYear()} PC SmartCare <span>Được tạo cho người yêu công nghệ.</span></p>
        </section>
      </div>
    </main>
  )
}

declare global {
  interface Window {
    google?: { accounts: { id: { initialize: (config: { client_id: string; callback: (response: { credential: string }) => void }) => void; renderButton: (element: HTMLElement, options: { theme: string; size: string; text: string; width: number }) => void } } }
  }
}
