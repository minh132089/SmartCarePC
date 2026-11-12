import { CaretDown, SignOut } from '@phosphor-icons/react'
import { useEffect, useRef, useState } from 'react'
import type { Session } from './AuthPage'

export function AccountMenu({ session, onLogout }: { session: Session; onLogout: () => void }) {
  const [open, setOpen] = useState(false)
  const root = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const close = (event: PointerEvent) => { if (!root.current?.contains(event.target as Node)) setOpen(false) }
    const escape = (event: KeyboardEvent) => { if (event.key === 'Escape') setOpen(false) }
    document.addEventListener('pointerdown', close)
    document.addEventListener('keydown', escape)
    return () => { document.removeEventListener('pointerdown', close); document.removeEventListener('keydown', escape) }
  }, [])
  return (
    <div ref={root} className="account-menu">
      <button className="account-trigger" onClick={() => setOpen(!open)} aria-expanded={open} aria-label="Account" aria-haspopup="true">
        <span className="account-avatar">{(session.name || session.email).slice(0, 1).toUpperCase()}</span>
        <span className="account-trigger-name">{session.name}</span><CaretDown size={13} />
      </button>
      {open && <div className="account-popover">
        <div className="account-details"><span className="account-avatar account-avatar-large">{(session.name || session.email).slice(0, 1).toUpperCase()}</span><span className="account-identity"><strong>{session.name}</strong><span>{session.email}</span></span></div>
        <button className="account-logout" onClick={onLogout}><SignOut size={18} /> Sign out</button>
      </div>}
    </div>
  )
}
