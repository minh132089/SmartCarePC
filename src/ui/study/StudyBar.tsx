import { useState } from 'react'
import { Flask, NotePencil, Star } from '@phosphor-icons/react'
import { useViewer } from '../../store'
import { collect, type AnswerMap } from './answers'
import { FEEDBACK } from './questions'
import { QuestionList } from './QuestionList'
import { CODE_PATTERN, PHASE_LABEL, newSubmissionId, sendFeedback, useSending, useStudy, type Phase } from './study'
import { Choices, Dialog, focus, PrimaryButton, Question, QuietButton } from './StudyParts'
import { useParticipant } from './participant'

/** Nút nhỏ "Tham gia thử nghiệm" cho người chưa có mã (người dùng bình thường bỏ qua được) */
export function StudyJoinLink({ className = '' }: { className?: string }) {
  const code = useStudy((s) => s.code)
  const open = useStudy((s) => s.open)
  if (code) return null
  return (
    <button onClick={() => open('join')} className={`text-[13px] text-ink-3 hover:text-ink hover:underline ${focus} ${className}`}>
      Taking part in the research study? Enter your code here
    </button>
  )
}

/** Dải nhỏ cho người tham gia: mã, lần đo, và nút gửi phản hồi */
export function StudyBar() {
  const { code, phase, answered, open } = useStudy()
  const { profile, edit } = useParticipant()
  if (!code && !profile) return null
  const sent = answered.includes('feedback')
  return (
    <div className="mt-4 flex flex-wrap items-center gap-1.5 rounded-2xl bg-info-bg px-3 py-2 text-[13px] text-info">
      <button onClick={() => code ? open('join') : edit()} className={`flex items-center gap-1.5 rounded-full font-semibold ${focus}`} title={code ? "Change code or study phase" : "Edit your details"}>
        <Flask size={15} aria-hidden />
        {code ? `Study · ${code} · ${PHASE_LABEL[phase]}` : `${profile!.name} · Edit details`}
      </button>
      {code && profile && <button onClick={edit} className={`rounded-full px-2 text-[12px] hover:underline ${focus}`}>{profile.name} · Edit details</button>}
      <span className="flex-1" />
      <button onClick={() => open('feedback')} className={`flex items-center gap-1 rounded-full px-2.5 py-1 font-medium hover:bg-surface ${focus}`}>
        <NotePencil size={14} aria-hidden />
        {sent ? "Feedback submitted" : "Send feedback"}
      </button>
    </div>
  )
}

/** Các hộp thoại của chế độ thử nghiệm, đặt một lần ở gốc app để dùng được trên cả hai trang */
export function StudyDialogs() {
  const dialog = useStudy((s) => s.dialog)
  if (dialog === 'join') return <JoinDialog />
  if (dialog === 'feedback') return <FeedbackDialog />
  if (dialog === 'rating') return <RatingDialog />
  return null
}

function JoinDialog() {
  const { code, phase, join, leave, open } = useStudy()
  const [value, setValue] = useState(code ?? '')
  const [lan, setLan] = useState<Phase>(phase)
  const ok = CODE_PATTERN.test(value.trim())
  const close = () => open(null)
  return (
    <Dialog
      title="Join the study"
      onClose={close}
      footer={
        <>
          {code && (
            <button onClick={leave} className={`mr-auto text-[13px] font-medium text-bad hover:underline ${focus}`}>
              Leave study mode
            </button>
          )}
          <QuietButton onClick={close}>Cancel</QuietButton>
          <PrimaryButton onClick={() => join(value, lan)} disabled={!ok}>
            {code ? "Save" : "Get started"}
          </PrimaryButton>
        </>
      }
    >
      <p className="text-[14px] leading-[1.5] text-ink-2">
        Thank you for participating. Use the app as usual: ask the assistant to recommend a build, then follow the assembly guide. When finished, select Send feedback to answer seven short questions. Responses are linked to your code and include any profile details you confirmed.
      </p>
      <Question label="Participant code">
        <input
          value={value}
          onChange={(e) => setValue(e.target.value.toUpperCase())}
          maxLength={20}
          placeholder="For example: P07"
          autoCapitalize="characters"
          className={`w-full rounded-xl bg-surface px-3 py-2 text-[15px] tracking-wide text-ink ring-1 ring-line-strong placeholder:text-ink-3 ${focus}`}
        />
        {value && !ok && <p className="mt-1 text-[12.5px] text-bad">Use letters, numbers and hyphens only, up to 20 characters.</p>}
      </Question>
      <Question label="Which study phase is this?">
        <Choices
          name="phase"
          value={[lan]}
          onChange={(v) => setLan(v[0])}
          options={[
            { value: 'before', label: "First use" },
            { value: 'after', label: "After 4 weeks" },
          ]}
        />
      </Question>
    </Dialog>
  )
}

/** Form phản hồi: 5 câu chấm điểm 1 đến 5 và 2 câu tự luận (questions.ts). Gửi kèm máy đang xem. */
function FeedbackDialog() {
  const { open, markAnswered } = useStudy()
  const machine = useViewer((s) => s.familyId ?? s.buildId)
  const [a, setA] = useState<AnswerMap>({})
  const busy = useSending((s) => s.sending)
  const [failed, setFailed] = useState(false)
  const [submissionId] = useState(newSubmissionId)
  const { answers, empty } = collect(FEEDBACK, a)
  // Đang gửi thì không cho đóng (đóng rồi mở lại giữa chừng dễ gửi trùng)
  const close = () => !busy && open(null)

  const submit = async () => {
    const r = await sendFeedback({ machine, answers }, submissionId)
    if (r === 'failed') return setFailed(true)
    markAnswered('feedback')
    open(null)
  }

  return (
    <Dialog
      title="App feedback"
      onClose={close}
      footer={
        <>
          {failed && <p className="mr-auto text-[13px] text-bad">Could not send. Check your connection and select Send again.</p>}
          <QuietButton onClick={close}>Later</QuietButton>
          <PrimaryButton onClick={submit} disabled={busy || empty}>
            {busy ? "Sending…" : failed ? "Send again" : "Send"}
          </PrimaryButton>
        </>
      }
    >
      <p className="mb-5 text-[14px] leading-[1.5] text-ink-2">About two minutes. There are no right or wrong answers, and every question is optional.</p>
      <QuestionList questions={FEEDBACK} answers={a} onChange={setA} />
    </Dialog>
  )
}

const STAR_WORDS = ['', "Very poor", "Poor", "Fair", "Good", "Excellent"]

/** Chấm 1 đến 5 sao. Dùng radio thật để đọc được bằng trình đọc màn hình và chọn được bằng phím mũi tên */
function Stars({ value, onChange }: { value: number; onChange: (v: number) => void }) {
  return (
    <div>
      <div className="flex gap-1" role="radiogroup" aria-label="Star rating">
        {[1, 2, 3, 4, 5].map((n) => (
          <label key={n} className="relative cursor-pointer">
            <input
              type="radio"
              name="stars"
              checked={value === n}
              onChange={() => onChange(n)}
              aria-label={`${n} stars, ${STAR_WORDS[n]}`}
              className="peer absolute inset-0 cursor-pointer opacity-0"
            />
            <Star
              size={36}
              weight={n <= value ? 'fill' : 'regular'}
              aria-hidden
              className={`rounded-lg transition-colors duration-150 peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-accent ${
                n <= value ? 'text-[#f5a524]' : 'text-ink-3 hover:text-ink-2'
              }`}
            />
          </label>
        ))}
      </div>
      <p className="mt-1 h-5 text-[13px] text-ink-2">{value ? STAR_WORDS[value] : "Choose a rating"}</p>
    </div>
  )
}

/**
 * Hiện khi người dùng đánh dấu xong mọi bước lắp (mọi người dùng, không cần mã thử nghiệm): chấm sao trải nghiệm,
 * dưới là các câu phản hồi cũ (questions.ts), câu nào cũng bỏ qua được. Gửi chung một lần, kèm máy đang xem.
 */
function RatingDialog() {
  const { open, markAnswered } = useStudy()
  const machine = useViewer((s) => s.familyId ?? s.buildId)
  const [stars, setStars] = useState(0)
  const [a, setA] = useState<AnswerMap>({})
  const busy = useSending((s) => s.sending)
  const [failed, setFailed] = useState(false)
  const [submissionId] = useState(newSubmissionId)
  const { answers } = collect(FEEDBACK, a)
  // Đang gửi thì không cho đóng (đóng rồi mở lại giữa chừng dễ gửi trùng)
  const close = () => !busy && open(null)

  const submit = async () => {
    const r = await sendFeedback({ machine, answers: { stars, source: 'guide_done', ...answers } }, submissionId)
    // Không tới được máy chủ và không cất được trên máy: giữ form và câu trả lời để thử lại
    if (r === 'failed') return setFailed(true)
    markAnswered('feedback')
    open(null)
  }

  return (
    <Dialog
      title="Your PC is assembled!"
      onClose={close}
      footer={
        <>
          {failed && <p className="mr-auto text-[13px] text-bad">Could not send. Check your connection and select Send again.</p>}
          <QuietButton onClick={close}>Later</QuietButton>
          <PrimaryButton onClick={submit} disabled={busy || stars === 0}>
            {busy ? "Sending…" : failed ? "Send again" : "Submit rating"}
          </PrimaryButton>
        </>
      }
    >
      <p className="text-[14px] leading-[1.5] text-ink-2">How was your experience choosing and assembling a PC with the app?</p>
      <div className="mt-3">
        <Stars value={stars} onChange={setStars} />
      </div>
      <p className="mt-5 mb-1 text-[13px] text-ink-3">Have two more minutes? Your answers help the team improve the app. Every question is optional.</p>
      <QuestionList questions={FEEDBACK} answers={a} onChange={setA} />
    </Dialog>
  )
}
