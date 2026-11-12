import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { AnimatePresence, motion, MotionConfig, useReducedMotion } from 'motion/react'
import { AnimateIcon } from '@animate-ui/react/icons'
import GlassSurface from '@/components/GlassSurface'
import { ArrowRight } from '@/components/animate-ui/icons/arrow-right'
import { CircuitBoard } from '@/components/animate-ui/icons/circuit-board'
import { Eye } from '@/components/animate-ui/icons/eye'
import { EyeOff } from '@/components/animate-ui/icons/eye-off'
import { LockKeyhole } from '@/components/animate-ui/icons/lock-keyhole'
import { Mail } from '@/components/animate-ui/icons/mail'

const SESSION_KEY = 'smartcare-session-v2'
const GOOGLE_CLIENT_ID = '666308491003-lasilr6p89lq8b06q6v87tmi2bb29o84.apps.googleusercontent.com'
/** Lò xo không nảy (damping 1): form chỉ hiện ra hay đổi chế độ, không có cú vuốt nào nên không cần nảy */
const SPRING = { type: 'spring', bounce: 0, duration: 0.4 } as const

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

/**
 * Màn đăng nhập / đăng ký: một tấm kính (GlassSurface của React Bits) nằm trên ảnh máy thật.
 * Chrome có khúc xạ thật qua bộ lọc SVG; Safari và Firefox tự lùi về kính mờ.
 */
export function AuthPage({ onLogin, onClose, required = false }: { onLogin: (session: Session) => void; onClose?: () => void; required?: boolean }) {
  const googleButton = useRef<HTMLDivElement>(null)
  const reduce = useReducedMotion()
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
        client_id: GOOGLE_CLIENT_ID,
        callback: async ({ credential }) => {
          try {
            const response = await fetch('/api/auth/google', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ credential }) })
            if (!response.ok) throw new Error('authentication failed')
            const session = await response.json() as Session
            if (remember) localStorage.setItem(SESSION_KEY, JSON.stringify(session))
            onLogin(session)
          } catch { setError('Could not sign in with Google. Check that the server is running and try again.') }
        },
      })
      googleButton.current.replaceChildren()
      // Nút của Google có bề rộng cố định: lấy đúng bề rộng ô chứa để không tràn trên điện thoại
      const width = Math.min(400, Math.max(200, Math.floor(googleButton.current.clientWidth)))
      window.google.accounts.id.renderButton(googleButton.current, { theme: 'filled_black', size: 'large', text: register ? 'signup_with' : 'continue_with', shape: 'pill', width })
    }
    if (window.google) initialize()
    else {
      const script = document.createElement('script')
      script.src = 'https://accounts.google.com/gsi/client'
      script.async = true
      script.onload = initialize
      script.onerror = () => { if (!cancelled) setError('Could not load Google sign-in. Please try again.') }
      document.head.appendChild(script)
      return () => { cancelled = true; script.remove() }
    }
    return () => { cancelled = true }
  }, [onLogin, remember, register])

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    const normalizedEmail = email.trim().toLowerCase()
    if (!normalizedEmail || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
      setError('Enter a valid email address.')
      return
    }
    if (password.length < 8 || new TextEncoder().encode(password).length > 72) {
      setError('Your password needs at least 8 characters and no more than 72 bytes.')
      return
    }
    if (register && password !== confirmPassword) {
      setError('The passwords do not match.')
      return
    }
    setBusy(true)
    try {
      const response = await fetch(`/api/auth/${register ? 'register' : 'login'}`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: normalizedEmail, password }) })
      if (!response.ok) {
        setError(response.status === 409 ? 'This email is already registered.' : response.status === 401 ? 'Incorrect email or password.' : 'Something went wrong. Please try again.')
        return
      }
      if (register) {
        setRegister(false)
        setPassword('')
        setConfirmPassword('')
        setNotice('Account created. Sign in with your new account.')
        return
      }
      const session = await response.json() as Session
      if (remember) localStorage.setItem(SESSION_KEY, JSON.stringify(session))
      onLogin(session)
    } catch { setError('Could not reach the server. Please try again later.') }
    finally { setBusy(false) }
  }

  return (
    <MotionConfig reducedMotion="user" transition={SPRING}>
      <main className="auth-page">
        <img className="auth-backdrop" src="/hero/advice-showcase-dark.jpg" alt="" aria-hidden />
        <motion.div
          className="auth-card-wrap"
          // Tấm kính "hiện hình": phóng nhẹ và hết mờ cùng lúc, thay vì chỉ hiện dần (giảm chuyển động thì chỉ hiện dần)
          initial={reduce ? { opacity: 0 } : { opacity: 0, scale: 0.97, filter: 'blur(8px)' }}
          // filter phải về 'none' khi xong: còn filter (kể cả blur(0px)) thì khung này thành "backdrop root" và tấm kính bên trong
          // không nhìn thấy ảnh phía sau nữa
          animate={reduce ? { opacity: 1 } : { opacity: 1, scale: 1, filter: 'blur(0px)', transitionEnd: { filter: 'none' } }}
        >
          <GlassSurface width="100%" height="auto" borderRadius={28} borderWidth={0.04} distortionScale={-90} frostBlur={22} backgroundOpacity={0.4} blur={10} brightness={45} saturation={1.3} className="auth-card">
            <div className="auth-card-body">
              <AnimateIcon animateOnHover asChild>
                <div className="auth-brand"><CircuitBoard size={20} aria-hidden /> PC SmartCare</div>
              </AnimateIcon>

              <h1>{register ? 'Create an account' : 'Sign in'}</h1>
              <p className="auth-lead">
                {required
                  ? 'You need an account to use the app. Your name and email are saved with any feedback you send, and only the research team can see them.'
                  : register ? 'All you need is an email and a password of 8 characters or more.' : 'Use your Google account or the email you registered with.'}
              </p>

              <div className="auth-segment" role="group" aria-label="Sign in or create an account">
                {[false, true].map((mode) => (
                  <button key={String(mode)} type="button" aria-pressed={register === mode} onClick={() => changeMode(mode)}>
                    {register === mode && <motion.span layoutId="auth-segment-pill" className="auth-segment-pill" aria-hidden />}
                    <span className="relative">{mode ? 'Create account' : 'Sign in'}</span>
                  </button>
                ))}
              </div>

              <div ref={googleButton} className="auth-google" />
              <div className="auth-divider"><span />or with email<span /></div>

              <form onSubmit={submit} className="auth-form" noValidate>
                <Field label="Email" icon={(on) => <Mail size={17} animate={on} aria-hidden />}>
                  {(focus) => <input {...focus} value={email} onChange={(e) => { setEmail(e.target.value); setError('') }} type="email" autoComplete="email" placeholder="you@example.com" />}
                </Field>
                <Field
                  label="Password"
                  icon={(on) => <LockKeyhole size={17} animate={on} aria-hidden />}
                  trailing={
                    <button type="button" className="auth-reveal" onClick={() => setShowPassword(!showPassword)} aria-label={showPassword ? 'Hide password' : 'Show password'} aria-pressed={showPassword}>
                      {showPassword ? <EyeOff size={18} animateOnHover aria-hidden /> : <Eye size={18} animateOnHover aria-hidden />}
                    </button>
                  }
                >
                  {(focus) => <input {...focus} value={password} onChange={(e) => { setPassword(e.target.value); setError('') }} type={showPassword ? 'text' : 'password'} autoComplete={register ? 'new-password' : 'current-password'} placeholder={register ? 'At least 8 characters' : 'Your password'} />}
                </Field>
                <AnimatePresence initial={false}>
                  {register && (
                    <motion.div key="confirm" className="auth-collapse" initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }}>
                      <Field label="Confirm password" icon={(on) => <LockKeyhole size={17} animate={on} aria-hidden />}>
                        {(focus) => <input {...focus} value={confirmPassword} onChange={(e) => { setConfirmPassword(e.target.value); setError('') }} type="password" autoComplete="new-password" placeholder="Type it again" />}
                      </Field>
                    </motion.div>
                  )}
                </AnimatePresence>

                {error && <p role="alert" className="auth-error">{error}</p>}
                {notice && <p role="status" className="auth-notice">{notice}</p>}
                {!register && <label className="auth-remember"><input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} /> Keep me signed in on this device</label>}

                <AnimateIcon animateOnHover asChild>
                  <button type="submit" disabled={busy} className="auth-submit">
                    {busy ? 'One moment…' : register ? 'Create account' : 'Sign in'}
                    {!busy && <ArrowRight size={18} aria-hidden />}
                  </button>
                </AnimateIcon>
              </form>

              {onClose && <button type="button" onClick={onClose} className="auth-skip">Not now</button>}
            </div>
          </GlassSurface>
        </motion.div>
      </main>
    </MotionConfig>
  )
}

type FocusProps = { onFocus: () => void; onBlur: () => void }

/** Một ô nhập có icon ở đầu: icon chạy hoạt ảnh khi ô đang được chọn, để thấy rõ mình đang gõ vào đâu */
function Field({ label, icon, trailing, children }: { label: string; icon: (active: boolean) => ReactNode; trailing?: ReactNode; children: (focus: FocusProps) => ReactNode }) {
  const [active, setActive] = useState(false)
  return (
    <label className="auth-field">
      <span className="auth-label">{label}</span>
      <span className="auth-input" data-active={active || undefined}>
        <span className="auth-input-icon">{icon(active)}</span>
        {children({ onFocus: () => setActive(true), onBlur: () => setActive(false) })}
        {trailing}
      </span>
    </label>
  )
}

declare global {
  interface Window {
    google?: { accounts: { id: { initialize: (config: { client_id: string; callback: (response: { credential: string }) => void }) => void; renderButton: (element: HTMLElement, options: { theme: string; size: string; text: string; shape?: string; width: number }) => void } } }
  }
}
