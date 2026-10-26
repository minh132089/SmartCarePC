import { useEffect, useId, useRef, useState } from 'react'
import { UserCircle } from '@phosphor-icons/react'
import SlideCommit from '../../components/SlideCommit'
import { needsParticipant, useParticipant, validParticipant, type Participant } from './participant'
import { occupationLabel } from './feedbackStats'
import './participant.css'

const OCCUPATIONS = ["School student", "University student", "Office worker", "Information technology", "Design / creative work", "Teacher / lecturer", "Business / self-employed", "Not currently employed"]

export function ParticipantGate() {
  const { profile, confirmedThisVisit, editing, save } = useParticipant()
  const required = needsParticipant(location.hostname)
  if ((!required || confirmedThisVisit) && !editing) return null
  return <ProfileForm initial={profile} onSave={save} />
}

function ProfileForm({ initial, onSave }: { initial: Participant | null; onSave: (p: Participant) => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const form = useRef<HTMLFormElement>(null)
  const pending = useRef<Participant | null>(null)
  const successTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const title = useId()
  const occupations = useId()
  const [name, setName] = useState(initial?.name ?? '')
  const [age, setAge] = useState(initial ? String(initial.age) : '')
  const [occupation, setOccupation] = useState(occupationLabel(initial?.occupation ?? ''))
  const [confirming, setConfirming] = useState(false)
  const [width, setWidth] = useState(280)
  const value = { name, age: Number(age), occupation }
  const valid = validParticipant(value)

  useEffect(() => {
    const el = dialog.current!
    el.showModal()
    const observer = new ResizeObserver(() => setWidth(Math.min(360, form.current?.clientWidth ?? el.clientWidth - 56)))
    observer.observe(el)
    return () => {
      observer.disconnect()
      el.close()
      if (successTimer.current) clearTimeout(successTimer.current)
    }
  }, [])

  return (
    <dialog ref={dialog} aria-labelledby={title} aria-describedby={`${title}-description`} onCancel={(e) => e.preventDefault()} className="participant-gate">
      <div className="participant-mark"><UserCircle size={28} weight="duotone" aria-hidden /></div>
      <p className="participant-eyebrow">WELCOME TO SMARTCARE PC</p>
      <h2 id={title}>Let's get to know you.</h2>
      <p id={`${title}-description`} className="participant-description">Tell the research team a little about yourself so we can better understand your PC selection and assembly experience.</p>
      <form ref={form} onSubmit={(e) => e.preventDefault()}>
        <fieldset disabled={confirming}>
          <label>Your name<input autoFocus name="name" autoComplete="name" value={name} onChange={(e) => setName(e.target.value)} required minLength={2} maxLength={80} placeholder="Enter your name or preferred name" /></label>
          <div className="participant-fields">
            <label>Age<input name="age" type="number" inputMode="numeric" min={1} max={120} step={1} required value={age} onChange={(e) => setAge(e.target.value)} placeholder="e.g. 21" /></label>
            <label>Occupation<input name="occupation" list={occupations} autoComplete="organization-title" required minLength={2} maxLength={80} value={occupation} onChange={(e) => setOccupation(e.target.value)} placeholder="Choose or enter your occupation" /><datalist id={occupations}>{OCCUPATIONS.map((v) => <option key={v} value={v} />)}</datalist></label>
          </div>
        </fieldset>
        <p className="participant-disclosure">By confirming, you agree to attach your name, age and occupation to feedback sent to the research team. These details are remembered in this browser and sent only when you submit feedback.</p>
        <div className="participant-confirm">
          <SlideCommit width={width} label="Slide to confirm" doneLabel="Confirmed" errorLabel="Check your details" trackColor="#e8e8ed" handleColor="#0071e3" successColor="#22c55e" returnBounce={0.18} landingDip={0} holdMs={0} disabled={!valid}
            onConfirm={() => {
              if (!valid || !form.current?.reportValidity()) throw new Error("Invalid profile details")
              pending.current = value
              setConfirming(true)
            }}
            onDone={() => {
              // Giữ dấu tích và viên xanh đủ lâu để người dùng thấy đã xác nhận trước khi đóng form.
              if (successTimer.current) clearTimeout(successTimer.current)
              successTimer.current = setTimeout(() => {
                if (pending.current) onSave(pending.current)
              }, 1400)
            }}
            onError={() => setConfirming(false)} />
        </div>
        <p className="participant-keyboard">You can also press Tab, then Enter or Space to confirm.</p>
      </form>
    </dialog>
  )
}
