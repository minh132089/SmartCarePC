import { useEffect, useRef, useState, type FormEvent } from 'react'
import { animate, AnimatePresence, motion, MotionConfig, useReducedMotion } from 'motion/react'
import { AnimateIcon } from '@animate-ui/react/icons'
import GlassSurface from '@/components/GlassSurface'
import GradientASCII from '@/components/GradientASCII'
import { ArrowRight } from '@/components/animate-ui/icons/arrow-right'
import { CircuitBoard } from '@/components/animate-ui/icons/circuit-board'
import { Eye } from '@/components/animate-ui/icons/eye'
import { EyeOff } from '@/components/animate-ui/icons/eye-off'

const SESSION_KEY = 'smartcare-session-v2'
const GOOGLE_CLIENT_ID = '666308491003-lasilr6p89lq8b06q6v87tmi2bb29o84.apps.googleusercontent.com'
/** Lò xo không nảy (damping 1): chỉ là đổi chế độ / đổi ảnh, không có cú vuốt nào nên không cần nảy */
const SPRING = { type: 'spring', bounce: 0, duration: 0.4 } as const
/**
 * Ba ảnh Unsplash (xem public/auth/CREDITS.txt), mỗi ảnh ứng với một việc app làm được.
 * hue là sắc màu chính của ánh đèn trong ảnh (đo trên ảnh: xanh 230, đỏ cam 5, xanh 217); nền trang và thẻ đổi theo nó.
 */
const SLIDES = [
  { src: '/auth/build.jpg', hue: 230, caption: 'Choose parts that fit,\nthen build it step by step' },
  { src: '/auth/maintain.jpg', hue: 8, caption: 'Keep it clean and cool,\nwith a reminder when it is due' },
  { src: '/auth/upgrade.jpg', hue: 217, caption: 'Know which upgrade\nis worth the money' },
]
const SLIDE_MS = 5000
/** Nền ASCII chạy phần mở màn (2,4 giây trong GradientASCII) được một lúc thì thẻ form mới hiện ra */
const CARD_DELAY = 1.8

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
 * Màn đăng nhập / đăng ký: thẻ tối hai cột. Bên trái là ảnh máy thật đổi dần (ẩn trên điện thoại), bên phải là form.
 * Nút "Back to website" là một viên kính (GlassSurface của React Bits) nổi trên ảnh.
 */
export function AuthPage({ onLogin, onClose, required = false }: { onLogin: (session: Session) => void; onClose?: () => void; required?: boolean }) {
  const googleButton = useRef<HTMLDivElement>(null)
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [agree, setAgree] = useState(false)
  const [remember, setRemember] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [register, setRegister] = useState(false)
  const [busy, setBusy] = useState(false)
  const [slide, setSlide] = useState(0)
  const hue = SLIDES[slide].hue
  const reduce = useReducedMotion()
  const changeMode = (next: boolean) => { if (busy) return; setRegister(next); setError(''); setNotice(''); setPassword(''); setShowPassword(false) }

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
      window.google.accounts.id.renderButton(googleButton.current, { theme: 'filled_black', size: 'large', text: register ? 'signup_with' : 'signin_with', shape: 'rectangular', width, locale: 'en' })
    }
    if (window.google) initialize()
    else {
      const script = document.createElement('script')
      script.src = 'https://accounts.google.com/gsi/client?hl=en'
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
    if (register && !firstName.trim()) {
      setError('Enter your first name.')
      return
    }
    if (!normalizedEmail || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
      setError('Enter a valid email address.')
      return
    }
    if (password.length < 8 || new TextEncoder().encode(password).length > 72) {
      setError('Your password needs at least 8 characters and no more than 72 bytes.')
      return
    }
    if (register && !agree) {
      setError('Tick the box to agree before creating your account.')
      return
    }
    setBusy(true)
    try {
      const body = register ? { email: normalizedEmail, password, firstName: firstName.trim(), lastName: lastName.trim() } : { email: normalizedEmail, password }
      const response = await fetch(`/api/auth/${register ? 'register' : 'login'}`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
      if (!response.ok) {
        setError(response.status === 409 ? 'This email is already registered.' : response.status === 401 ? 'Incorrect email or password.' : 'Something went wrong. Please try again.')
        return
      }
      if (register) {
        setRegister(false)
        setPassword('')
        setNotice('Account created. Log in with your new account.')
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
      {/* Nền ASCII và thẻ ngả theo màu đèn của ảnh đang hiện, chuyển màu chậm cùng lúc ảnh mờ dần sang ảnh mới */}
      <main className="auth-page">
        <AsciiBackdrop hue={hue} />
        <motion.div
          className="auth-card"
          style={{ backgroundColor: `hsl(${hue} 14% 13%)` }}
          // Thẻ trượt từ dưới đáy màn hình lên như một tấm sheet, không mờ dần, không phóng to. Lò xo nảy rất nhẹ
          // khi chạm chỗ, cho cảm giác có quán tính của cú đẩy lên. Giảm chuyển động thì chỉ hiện dần.
          initial={reduce ? { opacity: 0 } : { y: '110vh' }}
          animate={reduce ? { opacity: 1 } : { y: 0 }}
          transition={reduce ? { duration: 0.3, delay: 0 } : { type: 'spring', bounce: 0.14, duration: 0.85, delay: CARD_DELAY }}
        >
          <Showcase index={slide} onChange={setSlide} onClose={onClose} />

          <section className="auth-form-side" aria-label={register ? 'Create an account' : 'Log in'}>
            <div className="auth-mobile-brand"><CircuitBoard size={20} aria-hidden /> PC SmartCare</div>
            <h1>{register ? 'Create an account' : 'Welcome back'}</h1>
            <p className="auth-switch">
              {register ? 'Already have an account?' : 'New to PC SmartCare?'}{' '}
              <button type="button" onClick={() => changeMode(!register)}>{register ? 'Log in' : 'Create an account'}</button>
            </p>
            {required && <p className="auth-required">Log in to use the app. Your name and email are saved with feedback you send, and only the research team can see them.</p>}

            <form onSubmit={submit} className="auth-form" noValidate>
              <AnimatePresence initial={false}>
                {register && (
                  <motion.div key="names" className="auth-collapse" initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }}>
                    <div className="auth-row">
                      <input aria-label="First name" value={firstName} onChange={(e) => { setFirstName(e.target.value); setError('') }} autoComplete="given-name" maxLength={50} placeholder="First name" />
                      <input aria-label="Last name" value={lastName} onChange={(e) => { setLastName(e.target.value); setError('') }} autoComplete="family-name" maxLength={50} placeholder="Last name" />
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
              <input aria-label="Email" value={email} onChange={(e) => { setEmail(e.target.value); setError('') }} type="email" autoComplete="email" placeholder="Email" />
              <span className="auth-password">
                <input aria-label="Password" value={password} onChange={(e) => { setPassword(e.target.value); setError('') }} type={showPassword ? 'text' : 'password'} autoComplete={register ? 'new-password' : 'current-password'} placeholder={register ? 'Create a password (8+ characters)' : 'Enter your password'} />
                <button type="button" onClick={() => setShowPassword(!showPassword)} aria-label={showPassword ? 'Hide password' : 'Show password'} aria-pressed={showPassword}>
                  {showPassword ? <EyeOff size={18} animateOnHover aria-hidden /> : <Eye size={18} animateOnHover aria-hidden />}
                </button>
              </span>

              {register
                ? <label className="auth-check"><input type="checkbox" checked={agree} onChange={(e) => { setAgree(e.target.checked); setError('') }} /> <span>I agree that my name and email are saved with the feedback I send. Only the research team can see them.</span></label>
                : <label className="auth-check"><input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} /> <span>Keep me logged in on this device</span></label>}

              {error && <p role="alert" className="auth-error">{error}</p>}
              {notice && <p role="status" className="auth-notice">{notice}</p>}

              <AnimateIcon animateOnHover asChild>
                <button type="submit" disabled={busy} className="auth-submit">
                  {busy ? 'One moment…' : register ? 'Create account' : 'Log in'}
                  {!busy && <ArrowRight size={17} aria-hidden />}
                </button>
              </AnimateIcon>
            </form>

            <div className="auth-divider"><span />{register ? 'Or register with' : 'Or log in with'}<span /></div>
            <div ref={googleButton} className="auth-google" />
          </section>
        </motion.div>
      </main>
    </MotionConfig>
  )
}

/** Bảng màu của nền ASCII theo một sắc màu: xanh đậm, gần trắng, nhạt, sáng (giống bảng mặc định của React Bits) */
function palette(h: number) {
  const at = (dh: number) => (Math.round(h + dh) % 360 + 360) % 360
  return [`hsl(${at(0)} 75% 47%)`, `hsl(${at(0)} 85% 86%)`, `hsl(${at(-22)} 100% 82%)`, `hsl(${at(-30)} 92% 60%)`]
}

/**
 * Nền Gradient ASCII của React Bits phủ cả trang. Đổi ảnh thì sắc màu trượt dần sang màu mới (đi đường ngắn trên
 * vòng màu, 0,9 giây), khớp với lúc ảnh mờ dần. Lần đầu mở có phần mở màn của chính GradientASCII.
 */
function AsciiBackdrop({ hue }: { hue: number }) {
  const [shown, setShown] = useState(hue)
  const current = useRef(hue)
  useEffect(() => {
    const from = current.current
    const to = from + ((((hue - from) % 360) + 540) % 360) - 180
    const controls = animate(from, to, { duration: 0.9, ease: 'easeInOut', onUpdate: (v) => { current.current = v; setShown(Math.round(v)) } })
    return () => controls.stop()
  }, [hue])
  return (
    <div className="auth-ascii" style={{ backgroundColor: `hsl(${((shown % 360) + 360) % 360} 40% 6%)` }} aria-hidden>
      <GradientASCII colors={palette(shown)} shape="horizon" cellSize={16} glyphOpacity={0.8} gradientOpacity={0.85} clickEffect="wave" />
    </div>
  )
}

/** Cột ảnh bên trái: ba ảnh đổi dần mỗi 5 giây, chấm bên dưới để chọn ảnh. Rê chuột vào thì dừng đổi. */
function Showcase({ index, onChange, onClose }: { index: number; onChange: (index: number) => void; onClose?: () => void }) {
  const reduce = useReducedMotion()
  const [paused, setPaused] = useState(false)
  useEffect(() => {
    if (paused) return
    const timer = setTimeout(() => onChange((index + 1) % SLIDES.length), SLIDE_MS)
    return () => clearTimeout(timer)
  }, [index, paused, onChange])
  const slide = SLIDES[index]

  return (
    <aside className="auth-showcase" onPointerEnter={() => setPaused(true)} onPointerLeave={() => setPaused(false)}>
      <AnimatePresence initial={false}>
        <motion.img
          key={slide.src}
          src={slide.src}
          alt=""
          className="auth-showcase-img"
          initial={reduce ? { opacity: 0 } : { opacity: 0, scale: 1.04 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.9, ease: [0.22, 1, 0.36, 1] }}
        />
      </AnimatePresence>
      <div className="auth-showcase-top">
        <AnimateIcon animateOnHover asChild>
          <span className="auth-logo"><CircuitBoard size={22} aria-hidden /> PC SmartCare</span>
        </AnimateIcon>
        {onClose && (
          <AnimateIcon animateOnHover asChild>
            <button type="button" onClick={onClose} className="auth-back">
              <GlassSurface width="auto" height={34} borderRadius={17} borderWidth={0.12} distortionScale={-60} frostBlur={10} backgroundOpacity={0.18} className="auth-back-glass">
                <span className="auth-back-label">Back to website <ArrowRight size={14} aria-hidden /></span>
              </GlassSurface>
            </button>
          </AnimateIcon>
        )}
      </div>
      <div className="auth-showcase-bottom">
        <AnimatePresence mode="wait" initial={false}>
          <motion.p key={index} className="auth-caption" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -6 }} transition={{ duration: 0.35 }}>
            {slide.caption}
          </motion.p>
        </AnimatePresence>
        <div className="auth-dots" role="group" aria-label="Choose a picture">
          {SLIDES.map((s, i) => (
            <button key={s.src} type="button" aria-label={`Picture ${i + 1} of ${SLIDES.length}`} aria-pressed={i === index} onClick={() => onChange(i)}>
              <span />
            </button>
          ))}
        </div>
      </div>
    </aside>
  )
}

declare global {
  interface Window {
    google?: { accounts: { id: { initialize: (config: { client_id: string; callback: (response: { credential: string }) => void }) => void; renderButton: (element: HTMLElement, options: { theme: string; size: string; text: string; shape?: string; width: number; locale?: string }) => void } } }
  }
}
