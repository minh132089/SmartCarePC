import { useEffect, useState } from 'react'
import { useSession } from '../Account'
import { Dialog, PrimaryButton, QuietButton } from '../study/StudyParts'
import { confirmFromEmail, stopFromEmail, useEmailReminders } from './emailReminderStore'

/**
 * Phần "Email reminders" trong hộp Reminders. Chỉ cho người đã đăng nhập, dùng đúng email của tài khoản. Đăng nhập Google
 * thì bật ngay, tài khoản email / mật khẩu thì phải bấm link xác nhận trong thư. Máy chủ chưa cấu hình Gmail (hoặc không với tới) thì không hiện gì.
 */
export function EmailReminderSection() {
  const { available, sub, busy, error, check, enable, disable } = useEmailReminders()
  const session = useSession(s => s.session)
  const openAuth = useSession(s => s.openAuth)
  useEffect(() => { void check() }, [check])
  if (!available) return null
  const mine = sub && session && sub.email === session.email.trim().toLowerCase() ? sub : null
  return <section className="mt-4 rounded-xl bg-surface p-3 ring-1 ring-line">
    <h3 className="font-semibold">Email reminders</h3>
    {!session ? <>
      <p className="mt-1 text-[13px] text-ink-2">Sign in to get an email each time a task is due, for example every 6 months for dust cleaning, with a link straight to the guide.</p>
      <div className="mt-3"><QuietButton onClick={openAuth}>Sign in to turn on</QuietButton></div>
    </> : !mine ? <>
      <p className="mt-1 text-[13px] text-ink-2">Get an email at <strong>{session.email}</strong> each time a task is due, for example every 6 months for dust cleaning, with a link straight to the guide. It keeps going until you turn it off, and your dates update by themselves when you finish or postpone a task.</p>
      <div className="mt-3"><PrimaryButton disabled={busy} onClick={() => void enable(session.email, session.emailProof)}>{busy ? 'Sending…' : 'Email me on these dates'}</PrimaryButton></div>
      <p className="mt-2 text-[12px] text-ink-3">We use your email only for these reminders. Turn them off at any time and we delete it.</p>
    </> : mine.status === 'pending' ? <>
      <p className="mt-1 rounded-xl bg-info-bg p-3 text-[13px] text-info">Almost done: open the email we sent to <strong>{mine.email}</strong> and press “Turn on email reminders”. If you cannot find it, look in Spam.</p>
      <div className="mt-3 flex flex-wrap gap-2"><QuietButton disabled={busy} onClick={() => void enable(mine.email, session.emailProof)}>Send the email again</QuietButton><QuietButton disabled={busy} onClick={() => void disable()}>Cancel</QuietButton></div>
    </> : <>
      <p className="mt-1 rounded-xl bg-ok-bg p-3 text-[13px] text-ok">Email reminders are on for <strong>{mine.email}</strong>.</p>
      <div className="mt-3"><QuietButton disabled={busy} onClick={() => void disable()}>Turn off email reminders</QuietButton></div>
    </>}
    {error && <p role="alert" className="mt-2 text-[13px] text-bad">{error}</p>}
  </section>
}

/** Trang mở từ link trong thư: bật hay tắt nhắc chỉ khi người dùng bấm nút (trình quét link của hộp thư không tự bật được) */
export function EmailLinkDialog({ kind, token, onClose }: { kind: 'confirm' | 'stop'; token: string; onClose: () => void }) {
  const [state, setState] = useState<'ask' | 'busy' | 'done' | 'error'>('ask')
  const [message, setMessage] = useState('')
  const run = async () => {
    setState('busy')
    try {
      if (kind === 'confirm') setMessage(`Email reminders are on for ${await confirmFromEmail(token)}. You will get an email each time a task is due.`)
      else { await stopFromEmail(token); setMessage('Email reminders are off. We deleted your email address from SmartCarePC.') }
      setState('done')
    } catch (e) {
      setMessage((e as { status?: number }).status === 404 ? (kind === 'confirm' ? 'This link has expired. Turn on email reminders again from Maintenance guide → Set reminders.' : 'These reminders are already off.') : (e as Error).message)
      setState('error')
    }
  }
  const title = kind === 'confirm' ? 'Turn on email reminders' : 'Stop email reminders'
  return <Dialog title={title} onClose={onClose} footer={state === 'done' || state === 'error' ? <PrimaryButton onClick={onClose}>Close</PrimaryButton> : <><QuietButton onClick={onClose}>Cancel</QuietButton><PrimaryButton disabled={state === 'busy'} onClick={() => void run()}>{state === 'busy' ? 'Please wait…' : title}</PrimaryButton></>}>
    {state === 'ask' || state === 'busy'
      ? <p className="text-[14px] text-ink-2">{kind === 'confirm' ? 'SmartCarePC will email you each time a maintenance task is due, for example every 6 months for dust cleaning. You can turn this off from any reminder email.' : 'You will no longer get maintenance emails, and we delete your email address. Your plan stays in this browser.'}</p>
      : <p role="status" className={`rounded-xl p-3 text-[14px] ${state === 'done' ? 'bg-ok-bg text-ok' : 'bg-info-bg text-info'}`}>{message}</p>}
  </Dialog>
}
