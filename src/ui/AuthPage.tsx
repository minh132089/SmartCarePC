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
          } catch { setError('Could not sign in with Google. Check that the server is running and try again.') }
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
      script.onerror = () => { if (!cancelled) setError('Could not load Google sign-in. Please try again.') }
      document.head.appendChild(script)
      return () => { cancelled = true; script.remove() }
    }
    return () => { cancelled = true }
  }, [onLogin, remember])

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
    <main className="auth-page">
      <div className="auth-layout">
        <section className="auth-story" aria-label="About SmartCare PC">
          <div className="auth-brand"><span className="auth-brand-icon"><Cpu size={21} weight="bold" /></span><span>PC <strong>SmartCare</strong></span></div>
          <div className="auth-story-copy">
            <div className="auth-eyebrow"><Sparkle size={14} weight="fill" /> EXPLORE THE WORLD OF PCS</div>
            <h1>Your PC.<br /><span>Your journey.</span></h1>
            <p>From choosing parts to a finished build, it all starts here.</p>
          </div>
          <div className="auth-visual"><img src="/hero/advice-showcase-dark.jpg" alt="A PC model with its components visible inside" /><span className="auth-visual-label"><span className="auth-status" /> INTERACTIVE 3D <span> / </span> EASY ASSEMBLY</span></div>
          <div className="auth-benefits"><span><CheckCircle size={17} /> Smart advice</span><span><CheckCircle size={17} /> Visual guides</span></div>
          <div className="auth-story-footer">PC SMARTCARE <span>•</span> FOR PEOPLE WHO LOVE TECH</div>
        </section>
        <section className="auth-form-panel" aria-label={register ? 'Create account' : 'Sign in'}>
          <div className="auth-mobile-brand"><span className="auth-brand-icon"><Cpu size={19} weight="bold" /></span> PC <strong>SmartCare</strong></div>
          <div className="auth-form-content">
            <div className="auth-form-kicker"><span /> SMARTCARE PC ACCOUNT</div>
            <h2>{register ? 'Create an account' : 'Welcome back'}<span>.</span></h2>
            <p className="auth-intro">{register ? 'Start building a PC of your own.' : 'Sign in to explore and build your own PC with confidence.'}</p>
            <div className="auth-tabs" role="group" aria-label="Sign in or create an account"><button type="button" aria-pressed={!register} onClick={() => changeMode(false)}>Sign in</button><button type="button" aria-pressed={register} onClick={() => changeMode(true)}>Create account</button></div>
            <div ref={googleButton} className="auth-google" />
            <div className="auth-divider"><span />or continue with email<span /></div>
            <form onSubmit={submit} className="auth-form" noValidate>
              <label>Email<input value={email} onChange={(e) => { setEmail(e.target.value); setError('') }} type="email" autoComplete="email" placeholder="you@example.com" /></label>
              <label>Password<span className="auth-password"><input value={password} onChange={(e) => { setPassword(e.target.value); setError('') }} type={showPassword ? 'text' : 'password'} autoComplete={register ? 'new-password' : 'current-password'} placeholder={register ? 'At least 8 characters' : 'Enter your password'} /><button type="button" onClick={() => setShowPassword(!showPassword)} aria-label={showPassword ? 'Hide password' : 'Show password'} aria-pressed={showPassword}>{showPassword ? <EyeSlash size={19} /> : <Eye size={19} />}</button></span></label>
              {register && <label>Confirm password<input value={confirmPassword} onChange={(e) => { setConfirmPassword(e.target.value); setError('') }} type="password" autoComplete="new-password" placeholder="Enter your password again" /></label>}
              {error && <p role="alert" className="auth-error">{error}</p>}
              {notice && <p role="status" className="auth-notice">{notice}</p>}
              {!register && <label className="auth-remember"><input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} /> Keep me signed in on this device</label>}
              <button type="submit" disabled={busy} className="auth-submit">{busy ? 'Working…' : register ? 'Create account' : 'Sign in'} <ArrowRight size={18} /></button>
              <p className="auth-demo">{register ? 'Already have an account?' : 'New here?'} <button type="button" onClick={() => changeMode(!register)}>{register ? 'Sign in' : 'Create an account'}</button></p>
              {onClose && <button type="button" onClick={onClose} className="auth-skip">Not now, back to the page</button>}
            </form>
          </div>
          <p className="auth-footer">© {new Date().getFullYear()} PC SmartCare <span>Made for people who love tech.</span></p>
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
