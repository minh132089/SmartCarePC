import { useState } from 'react'
import { Flask, NotePencil } from '@phosphor-icons/react'
import { useViewer } from '../../store'
import { collect, type AnswerMap } from './answers'
import { FEEDBACK } from './questions'
import { QuestionList } from './QuestionList'
import { CODE_PATTERN, PHASE_LABEL, sendFeedback, useStudy, type Phase } from './study'
import { Choices, Dialog, focus, PrimaryButton, Question, QuietButton } from './StudyParts'

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
  if (!code) return null
  const sent = answered.includes('feedback')
  return (
    <div className="mt-4 flex flex-wrap items-center gap-1.5 rounded-2xl bg-info-bg px-3 py-2 text-[13px] text-info">
      <button onClick={() => open('join')} className={`flex items-center gap-1.5 rounded-full font-semibold ${focus}`} title="Đổi mã hoặc lần đo">
        <Flask size={15} aria-hidden />
        Thử nghiệm · {code} · {PHASE_LABEL[phase]}
      </button>
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
        "Gửi phản hồi" để trả lời 7 câu ngắn. Câu trả lời chỉ gắn với mã của bạn, app không thu tên, email hay số điện thoại.
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
  const [busy, setBusy] = useState(false)
  const { answers, empty } = collect(FEEDBACK, a)

  const submit = async () => {
    setBusy(true)
    await sendFeedback({ machine, answers })
    markAnswered('feedback')
    setBusy(false)
    open(null)
  }

  return (
    <Dialog
      title="Phản hồi về web app"
      onClose={() => open(null)}
      footer={
        <>
          <QuietButton onClick={() => open(null)}>Để sau</QuietButton>
          <PrimaryButton onClick={submit} disabled={busy || empty}>
            Gửi
          </PrimaryButton>
        </>
      }
    >
      <p className="mb-5 text-[14px] leading-[1.5] text-ink-2">Khoảng 2 phút. Không có câu trả lời đúng hay sai, bạn có thể bỏ qua câu nào cũng được.</p>
      <QuestionList questions={FEEDBACK} answers={a} onChange={setA} />
    </Dialog>
  )
}
