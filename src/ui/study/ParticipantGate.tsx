import { useEffect, useId, useRef, useState } from 'react'
import { UserCircle } from '@phosphor-icons/react'
import SlideCommit from '../../components/SlideCommit'
import { needsParticipant, useParticipant, validParticipant, type Participant } from './participant'
import './participant.css'

const OCCUPATIONS = ['Học sinh', 'Sinh viên', 'Nhân viên văn phòng', 'Công nghệ thông tin', 'Thiết kế / sáng tạo', 'Giáo viên / giảng viên', 'Kinh doanh / tự do', 'Chưa đi làm']

export function ParticipantGate() {
  const { profile, editing, save } = useParticipant()
  const required = needsParticipant(location.hostname)
  if ((!required || profile) && !editing) return null
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
  const [occupation, setOccupation] = useState(initial?.occupation ?? '')
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
      <p className="participant-eyebrow">CHÀO MỪNG ĐẾN SMARTCARE PC</p>
      <h2 id={title}>Làm quen một chút nhé.</h2>
      <p id={`${title}-description`} className="participant-description">Cho nhóm biết bạn là ai để hiểu rõ hơn phản hồi về trải nghiệm chọn và lắp máy.</p>
      <form ref={form} onSubmit={(e) => e.preventDefault()}>
        <fieldset disabled={confirming}>
          <label>Tên của bạn<input autoFocus name="name" autoComplete="name" value={name} onChange={(e) => setName(e.target.value)} required minLength={2} maxLength={80} placeholder="Nhập tên hoặc tên bạn muốn dùng" /></label>
          <div className="participant-fields">
            <label>Tuổi<input name="age" type="number" inputMode="numeric" min={1} max={120} step={1} required value={age} onChange={(e) => setAge(e.target.value)} placeholder="Ví dụ: 21" /></label>
            <label>Nghề nghiệp<input name="occupation" list={occupations} autoComplete="organization-title" required minLength={2} maxLength={80} value={occupation} onChange={(e) => setOccupation(e.target.value)} placeholder="Chọn hoặc nhập nghề nghiệp" /><datalist id={occupations}>{OCCUPATIONS.map((v) => <option key={v} value={v} />)}</datalist></label>
          </div>
        </fieldset>
        <p className="participant-disclosure">Khi xác nhận, bạn đồng ý đính kèm tên, tuổi và nghề nghiệp vào phản hồi gửi cho nhóm nghiên cứu. Thông tin được ghi nhớ trên trình duyệt này và chỉ gửi lên khi bạn gửi phản hồi.</p>
        <div className="participant-confirm">
          <SlideCommit width={width} label="Kéo để xác nhận" doneLabel="Đã xác nhận" errorLabel="Kiểm tra lại thông tin" trackColor="#e8e8ed" handleColor="#0071e3" successColor="#22c55e" returnBounce={0.18} landingDip={0} holdMs={0} disabled={!valid}
            onConfirm={() => {
              if (!valid || !form.current?.reportValidity()) throw new Error('Thông tin chưa hợp lệ')
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
        <p className="participant-keyboard">Bạn cũng có thể dùng Tab rồi Enter hoặc phím cách để xác nhận.</p>
      </form>
    </dialog>
  )
}
