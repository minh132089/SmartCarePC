import { useState } from 'react'
import { Flask, NotePencil, Star } from '@phosphor-icons/react'
import { useViewer } from '../../store'
import { collect, type AnswerMap } from './answers'
import { BASELINE, FEEDBACK, FOLLOWUP, type Lang } from './questions'
import { QuestionList } from './QuestionList'
import { CODE_PATTERN, PHASE_LABEL, newSubmissionId, pendingForm, sendFeedback, useSending, useStudy, type Phase } from './study'
import { Choices, Dialog, focus, LangToggle, PrimaryButton, Question, QuietButton } from './StudyParts'
import { tr, useFormLang } from './lang'
import { useMaintenance } from '../maintenance/maintenanceState'
import { useParticipant, validParticipant, type Participant } from './participant'

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

/** Dải nhỏ cho người tham gia: mã, lần đo, và nút mở form của lần đo (hoặc form phản hồi chung khi đã trả lời) */
export function StudyBar() {
  const { code, phase, answered, open } = useStudy()
  if (!code) return null
  const form = pendingForm(phase, answered)
  const sent = answered.includes('feedback')
  return (
    <div className="mt-4 flex flex-wrap items-center gap-1.5 rounded-2xl bg-info-bg px-3 py-2 text-[13px] text-info">
      <button onClick={() => open('join')} className={`flex items-center gap-1.5 rounded-full font-semibold ${focus}`} title="Change code or study phase">
        <Flask size={15} aria-hidden />
        {`Study · ${code} · ${PHASE_LABEL[phase]}`}
      </button>
      <span className="flex-1" />
      <button onClick={() => open(form ?? 'feedback')} className={`flex items-center gap-1 rounded-full px-2.5 py-1 font-medium hover:bg-surface ${focus} ${form ? 'bg-surface' : ''}`}>
        <NotePencil size={14} aria-hidden />
        {form === 'baseline' ? "Answer the first-use questions" : form === 'followup' ? "Answer the end-of-study questions" : sent ? "Feedback submitted" : "Send feedback"}
      </button>
    </div>
  )
}

/** Các hộp thoại của chế độ thử nghiệm, đặt một lần ở gốc app để dùng được trên cả hai trang */
export function StudyDialogs() {
  const dialog = useStudy((s) => s.dialog)
  if (dialog === 'join') return <JoinDialog />
  if (dialog === 'baseline') return <StudyForm form="baseline" />
  if (dialog === 'followup') return <StudyForm form="followup" />
  if (dialog === 'feedback') return <FeedbackDialog />
  if (dialog === 'rating') return <RatingDialog />
  return null
}

function JoinDialog() {
  const { code, phase, answered, join, leave, open } = useStudy()
  const { lang, setLang } = useFormLang()
  const [value, setValue] = useState(code ?? '')
  const [lan, setLan] = useState<Phase>(phase)
  const ok = CODE_PATTERN.test(value.trim())
  const close = () => open(null)
  return (
    <Dialog
      title={tr(lang, "Join the study", 'Tham gia nghiên cứu')}
      onClose={close}
      footer={
        <>
          {code && (
            <button onClick={leave} className={`mr-auto text-[13px] font-medium text-bad hover:underline ${focus}`}>
              {tr(lang, "Leave study mode", 'Thoát chế độ nghiên cứu')}
            </button>
          )}
          <QuietButton onClick={close}>{tr(lang, "Cancel", 'Huỷ')}</QuietButton>
          <PrimaryButton onClick={() => join(value, lan)} disabled={!ok}>
            {code && !pendingForm(phase, answered) ? tr(lang, "Save", 'Lưu') : tr(lang, "Get started", 'Bắt đầu')}
          </PrimaryButton>
        </>
      }
    >
      <LangToggle lang={lang} onChange={setLang} />
      <p className="text-[14px] leading-[1.5] text-ink-2">
        {tr(
          lang,
          "Thank you for taking part. First you answer a few short questions, before using any guide. Then use the app as usual. On the last day of the study, open the app again, choose “End of study” below and answer the final questions. Responses are saved with your participant code and the name and email of your account. Only the research team can see them.",
          'Cảm ơn bạn đã tham gia. Trước tiên bạn trả lời vài câu hỏi ngắn, trước khi dùng bất kỳ hướng dẫn nào. Sau đó dùng app như bình thường. Vào ngày cuối của nghiên cứu, mở lại app, chọn “Cuối nghiên cứu” bên dưới và trả lời các câu hỏi cuối. Câu trả lời được lưu cùng mã người tham gia và tên, email tài khoản của bạn. Chỉ nhóm nghiên cứu xem được.',
        )}
      </p>
      <Question label={tr(lang, "Participant code", 'Mã người tham gia')}>
        <input
          value={value}
          onChange={(e) => setValue(e.target.value.toUpperCase())}
          maxLength={20}
          placeholder={tr(lang, "For example: P07", 'Ví dụ: P07')}
          autoCapitalize="characters"
          className={`w-full rounded-xl bg-surface px-3 py-2 text-[15px] tracking-wide text-ink ring-1 ring-line-strong placeholder:text-ink-3 ${focus}`}
        />
        {value && !ok && <p className="mt-1 text-[12.5px] text-bad">{tr(lang, "Use letters, numbers and hyphens only, up to 20 characters.", 'Chỉ dùng chữ, số và dấu gạch ngang, tối đa 20 ký tự.')}</p>}
      </Question>
      <Question label={tr(lang, "Which study phase is this?", 'Đây là lần nào của nghiên cứu?')}>
        <Choices
          name="phase"
          value={[lan]}
          onChange={(v) => setLan(v[0])}
          options={[
            { value: 'before', label: tr(lang, "First use", 'Lần đầu') },
            { value: 'after', label: tr(lang, "End of study", 'Cuối nghiên cứu') },
          ]}
        />
      </Question>
    </Dialog>
  )
}

/**
 * Form của hai lần đo. Lần đầu (baseline) hiện ngay sau khi nhập mã, trước mọi gợi ý và hướng dẫn, để làm mức nền.
 * Cuối nghiên cứu (followup) hỏi lại y nguyên ba nhóm câu lõi để lấy hiệu sau − trước. Câu nào cũng bỏ qua được.
 * Người có máy bàn: gửi xong form lần đầu thì mở luôn bước chọn máy và đặt lời nhắc bảo dưỡng.
 */
function StudyForm({ form }: { form: 'baseline' | 'followup' }) {
  const { open, markAnswered, setOwnPc, ownPc } = useStudy()
  const saveProfile = useParticipant((s) => s.save)
  const { lang, setLang } = useFormLang()
  const [a, setA] = useState<AnswerMap>({})
  const busy = useSending((s) => s.sending)
  const [failed, setFailed] = useState(false)
  const [submissionId] = useState(newSubmissionId)
  // Ở form cuối, câu bảo dưỡng chỉ dành cho người có máy bàn (hoặc chưa rõ vì trả lời form đầu trên máy khác)
  const questions = form === 'baseline' ? BASELINE : FOLLOWUP.filter((q) => q.key !== 'maint_done' || ownPc == null || ownPc === 'desktop')
  const { answers, empty } = collect(questions, a)
  // Tuổi và nghề đi theo cặp (máy chủ từ chối nếu chỉ có một hoặc sai dạng): thiếu hay sai một ô thì không gửi cả hai
  const profile = { age: answers.participant_age, occupation: answers.participant_occupation }
  const profileOk = validParticipant(profile)
  if (!profileOk) { delete answers.participant_age; delete answers.participant_occupation }
  const close = () => !busy && open(null)

  const submit = async () => {
    const r = await sendFeedback({ machine: null, answers: { form, form_lang: lang, ...answers } }, submissionId)
    if (r === 'failed') return setFailed(true)
    if (profileOk) saveProfile(profile as Participant)
    markAnswered(form)
    if (form === 'baseline' && typeof answers.own_pc === 'string') setOwnPc(answers.own_pc)
    open(null)
    if (form === 'baseline' && answers.own_pc === 'desktop') useMaintenance.getState().open('dust')
  }

  return (
    <Dialog
      title={form === 'baseline' ? tr(lang, "Before you start", 'Trước khi bắt đầu') : tr(lang, "End of study", 'Cuối nghiên cứu')}
      onClose={close}
      footer={
        <>
          {failed && <p className="mr-auto text-[13px] text-bad">{tr(lang, "Could not send. Check your connection and select Send again.", 'Chưa gửi được. Kiểm tra kết nối rồi bấm Gửi lại.')}</p>}
          <QuietButton onClick={close}>{tr(lang, "Later", 'Để sau')}</QuietButton>
          <PrimaryButton onClick={submit} disabled={busy || empty}>
            {busy ? tr(lang, "Sending…", 'Đang gửi…') : failed ? tr(lang, "Send again", 'Gửi lại') : tr(lang, "Send", 'Gửi')}
          </PrimaryButton>
        </>
      }
    >
      <LangToggle lang={lang} onChange={setLang} />
      <p className="mb-5 text-[14px] leading-[1.5] text-ink-2">
        {form === 'baseline'
          ? tr(lang, "About 3 minutes. Please answer before you use the app's advice or guides, so we can compare with your answers at the end of the study. There are no right or wrong answers, and you can skip any question.", 'Khoảng 3 phút. Bạn trả lời trước khi dùng phần tư vấn hay hướng dẫn của app, để cuối nghiên cứu so sánh với câu trả lời lúc đó. Không có đúng hay sai, câu nào cũng bỏ qua được.')
          : tr(lang, "About 4 minutes. Some questions are the same as on your first day; answer them as things are for you now, not as you answered before.", 'Khoảng 4 phút. Một số câu giống hôm đầu; bạn trả lời theo đúng hiện tại, không cần nhớ lần trước đã chọn gì.')}
      </p>
      <QuestionList questions={questions} answers={a} onChange={setA} lang={lang} />
    </Dialog>
  )
}

/** Form phản hồi chung: bảy câu cũ (questions.ts). Gửi kèm máy đang xem. */
function FeedbackDialog() {
  const { open, markAnswered } = useStudy()
  const { lang, setLang } = useFormLang()
  const machine = useViewer((s) => s.familyId ?? s.buildId)
  const [a, setA] = useState<AnswerMap>({})
  const busy = useSending((s) => s.sending)
  const [failed, setFailed] = useState(false)
  const [submissionId] = useState(newSubmissionId)
  const { answers, empty } = collect(FEEDBACK, a)
  // Đang gửi thì không cho đóng (đóng rồi mở lại giữa chừng dễ gửi trùng)
  const close = () => !busy && open(null)

  const submit = async () => {
    const r = await sendFeedback({ machine, answers: { form: 'general', form_lang: lang, ...answers } }, submissionId)
    if (r === 'failed') return setFailed(true)
    markAnswered('feedback')
    open(null)
  }

  return (
    <Dialog
      title={tr(lang, "App feedback", 'Góp ý cho app')}
      onClose={close}
      footer={
        <>
          {failed && <p className="mr-auto text-[13px] text-bad">{tr(lang, "Could not send. Check your connection and select Send again.", 'Chưa gửi được. Kiểm tra kết nối rồi bấm Gửi lại.')}</p>}
          <QuietButton onClick={close}>{tr(lang, "Later", 'Để sau')}</QuietButton>
          <PrimaryButton onClick={submit} disabled={busy || empty}>
            {busy ? tr(lang, "Sending…", 'Đang gửi…') : failed ? tr(lang, "Send again", 'Gửi lại') : tr(lang, "Send", 'Gửi')}
          </PrimaryButton>
        </>
      }
    >
      <LangToggle lang={lang} onChange={setLang} />
      <p className="mb-5 text-[14px] leading-[1.5] text-ink-2">{tr(lang, "About two minutes. There are no right or wrong answers, and every question is optional.", 'Khoảng hai phút. Không có đúng hay sai, câu nào cũng bỏ qua được.')}</p>
      <QuestionList questions={FEEDBACK} answers={a} onChange={setA} lang={lang} />
    </Dialog>
  )
}

const STAR_WORDS = { en: ['', "Very poor", "Poor", "Fair", "Good", "Excellent"], vi: ['', 'Rất tệ', 'Tệ', 'Tạm được', 'Tốt', 'Rất tốt'] }

/** Chấm 1 đến 5 sao. Dùng radio thật để đọc được bằng trình đọc màn hình và chọn được bằng phím mũi tên */
function Stars({ value, onChange, lang }: { value: number; onChange: (v: number) => void; lang: Lang }) {
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
              aria-label={`${n} stars, ${STAR_WORDS[lang][n]}`}
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
      <p className="mt-1 h-5 text-[13px] text-ink-2">{value ? STAR_WORDS[lang][value] : tr(lang, "Choose a rating", 'Chọn số sao')}</p>
    </div>
  )
}

/**
 * Hiện khi người dùng đánh dấu xong mọi bước lắp (mọi người dùng, không cần mã thử nghiệm): chấm sao trải nghiệm,
 * dưới là các câu phản hồi cũ (questions.ts), câu nào cũng bỏ qua được. Gửi chung một lần, kèm máy đang xem.
 */
function RatingDialog() {
  const { open, markAnswered } = useStudy()
  const { lang, setLang } = useFormLang()
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
    const r = await sendFeedback({ machine, answers: { form: 'post_build', form_lang: lang, stars, source: 'guide_done', ...answers } }, submissionId)
    // Không tới được máy chủ và không cất được trên máy: giữ form và câu trả lời để thử lại
    if (r === 'failed') return setFailed(true)
    markAnswered('feedback')
    open(null)
  }

  return (
    <Dialog
      title={tr(lang, "Your PC is assembled!", 'Bạn đã lắp xong máy!')}
      onClose={close}
      footer={
        <>
          {failed && <p className="mr-auto text-[13px] text-bad">{tr(lang, "Could not send. Check your connection and select Send again.", 'Chưa gửi được. Kiểm tra kết nối rồi bấm Gửi lại.')}</p>}
          <QuietButton onClick={close}>{tr(lang, "Later", 'Để sau')}</QuietButton>
          <PrimaryButton onClick={submit} disabled={busy || stars === 0}>
            {busy ? tr(lang, "Sending…", 'Đang gửi…') : failed ? tr(lang, "Send again", 'Gửi lại') : tr(lang, "Submit rating", 'Gửi đánh giá')}
          </PrimaryButton>
        </>
      }
    >
      <LangToggle lang={lang} onChange={setLang} />
      <p className="text-[14px] leading-[1.5] text-ink-2">{tr(lang, "How was your experience choosing and assembling a PC with the app?", 'Trải nghiệm chọn và lắp máy với app của bạn thế nào?')}</p>
      <div className="mt-3">
        <Stars value={stars} onChange={setStars} lang={lang} />
      </div>
      <p className="mt-5 mb-1 text-[13px] text-ink-3">{tr(lang, "Have two more minutes? Your answers help the team improve the app. Every question is optional.", 'Còn hai phút nữa không? Câu trả lời giúp nhóm cải thiện app. Câu nào cũng bỏ qua được.')}</p>
      <QuestionList questions={FEEDBACK} answers={a} onChange={setA} lang={lang} />
    </Dialog>
  )
}
