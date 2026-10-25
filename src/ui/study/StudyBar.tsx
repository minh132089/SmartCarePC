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
      Bạn tham gia thử nghiệm của nhóm nghiên cứu? Nhập mã ở đây
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
      <button onClick={() => code ? open('join') : edit()} className={`flex items-center gap-1.5 rounded-full font-semibold ${focus}`} title={code ? 'Đổi mã hoặc lần đo' : 'Sửa thông tin của bạn'}>
        <Flask size={15} aria-hidden />
        {code ? `Thử nghiệm · ${code} · ${PHASE_LABEL[phase]}` : `${profile!.name} · Sửa thông tin`}
      </button>
      {code && profile && <button onClick={edit} className={`rounded-full px-2 text-[12px] hover:underline ${focus}`}>{profile.name} · Sửa thông tin</button>}
      <span className="flex-1" />
      <button onClick={() => open('feedback')} className={`flex items-center gap-1 rounded-full px-2.5 py-1 font-medium hover:bg-surface ${focus}`}>
        <NotePencil size={14} aria-hidden />
        {sent ? 'Đã gửi phản hồi' : 'Gửi phản hồi'}
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
      title="Tham gia thử nghiệm"
      onClose={close}
      footer={
        <>
          {code && (
            <button onClick={leave} className={`mr-auto text-[13px] font-medium text-bad hover:underline ${focus}`}>
              Thoát chế độ thử nghiệm
            </button>
          )}
          <QuietButton onClick={close}>Huỷ</QuietButton>
          <PrimaryButton onClick={() => join(value, lan)} disabled={!ok}>
            {code ? 'Lưu' : 'Bắt đầu'}
          </PrimaryButton>
        </>
      }
    >
      <p className="text-[14px] leading-[1.5] text-ink-2">
        Cảm ơn bạn tham gia. Hãy dùng web app như bình thường: nhờ trợ lý chọn cấu hình, rồi làm theo hướng dẫn lắp máy. Dùng xong, bấm
        "Gửi phản hồi" để trả lời 7 câu ngắn. Câu trả lời gắn với mã; thông tin bạn đã xác nhận (nếu có) cũng được đính kèm.
      </p>
      <Question label="Mã người tham gia">
        <input
          value={value}
          onChange={(e) => setValue(e.target.value.toUpperCase())}
          maxLength={20}
          placeholder="Ví dụ: P07"
          autoCapitalize="characters"
          className={`w-full rounded-xl bg-surface px-3 py-2 text-[15px] tracking-wide text-ink ring-1 ring-line-strong placeholder:text-ink-3 ${focus}`}
        />
        {value && !ok && <p className="mt-1 text-[12.5px] text-bad">Mã chỉ gồm chữ, số, gạch ngang, tối đa 20 ký tự.</p>}
      </Question>
      <Question label="Đây là lần nào?">
        <Choices
          name="phase"
          value={[lan]}
          onChange={(v) => setLan(v[0])}
          options={[
            { value: 'before', label: 'Lần đầu' },
            { value: 'after', label: 'Sau 4 tuần' },
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
      title="Phản hồi về web app"
      onClose={close}
      footer={
        <>
          {failed && <p className="mr-auto text-[13px] text-bad">Chưa gửi được, kiểm tra mạng rồi bấm Gửi lại.</p>}
          <QuietButton onClick={close}>Để sau</QuietButton>
          <PrimaryButton onClick={submit} disabled={busy || empty}>
            {busy ? 'Đang gửi…' : failed ? 'Gửi lại' : 'Gửi'}
          </PrimaryButton>
        </>
      }
    >
      <p className="mb-5 text-[14px] leading-[1.5] text-ink-2">Khoảng 2 phút. Không có câu trả lời đúng hay sai, bạn có thể bỏ qua câu nào cũng được.</p>
      <QuestionList questions={FEEDBACK} answers={a} onChange={setA} />
    </Dialog>
  )
}

const STAR_WORDS = ['', 'Rất tệ', 'Chưa tốt', 'Tạm được', 'Tốt', 'Rất tốt']

/** Chấm 1 đến 5 sao. Dùng radio thật để đọc được bằng trình đọc màn hình và chọn được bằng phím mũi tên */
function Stars({ value, onChange }: { value: number; onChange: (v: number) => void }) {
  return (
    <div>
      <div className="flex gap-1" role="radiogroup" aria-label="Số sao">
        {[1, 2, 3, 4, 5].map((n) => (
          <label key={n} className="relative cursor-pointer">
            <input
              type="radio"
              name="stars"
              checked={value === n}
              onChange={() => onChange(n)}
              aria-label={`${n} sao, ${STAR_WORDS[n]}`}
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
      <p className="mt-1 h-5 text-[13px] text-ink-2">{value ? STAR_WORDS[value] : 'Chọn số sao'}</p>
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
      title="Bạn đã lắp xong cả máy!"
      onClose={close}
      footer={
        <>
          {failed && <p className="mr-auto text-[13px] text-bad">Chưa gửi được, kiểm tra mạng rồi bấm Gửi lại.</p>}
          <QuietButton onClick={close}>Để sau</QuietButton>
          <PrimaryButton onClick={submit} disabled={busy || stars === 0}>
            {busy ? 'Đang gửi…' : failed ? 'Gửi lại' : 'Gửi đánh giá'}
          </PrimaryButton>
        </>
      }
    >
      <p className="text-[14px] leading-[1.5] text-ink-2">Bạn thấy trải nghiệm chọn và lắp máy với web app thế nào?</p>
      <div className="mt-3">
        <Stars value={stars} onChange={setStars} />
      </div>
      <p className="mt-5 mb-1 text-[13px] text-ink-3">Nếu có thêm 2 phút, trả lời vài câu dưới đây giúp nhóm làm app tốt hơn. Bỏ qua câu nào cũng được.</p>
      <QuestionList questions={FEEDBACK} answers={a} onChange={setA} />
    </Dialog>
  )
}
