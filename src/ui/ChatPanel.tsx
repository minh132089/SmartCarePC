import { useEffect, useMemo, useRef, useState } from 'react'
import { getBuild } from '../data/builds'
import { FAMILIES } from '../data/families'
import { guideSteps } from '../data/guide'
import { useViewer } from '../store'
import { useAssemblyChat } from './chat/chatStore'
import { Bubble, ChatIcon, Composer, IconButton, ResetPath, Thread } from './chat/ChatParts'

const GREETING =
  'Chào bạn, đang lắp máy mà gặp chỗ nào khó, lạ hay lo làm hỏng thì cứ hỏi mình. Mình biết bạn đang ở bước nào nên chỉ cần kể chuyện gì đang xảy ra.'

/** Những chỗ người mới hay vướng khi tự lắp */
const SUGGESTIONS = [
  'CPU không nằm phẳng trong socket',
  'RAM ấn mãi không vào khe',
  'Không biết cắm dây nút nguồn vào đâu',
  'Bật máy quạt quay nhưng không lên hình',
]

/**
 * Khung chat "Hỏi khi lắp máy" nổi trên trang hướng dẫn (mô hình 3D).
 * Gửi kèm máy đang xem và bước hướng dẫn đang mở để trợ lý trả lời đúng chỗ người dùng đang vướng.
 */
export function ChatPanel() {
  const familyId = useViewer((s) => s.familyId)
  const buildId = useViewer((s) => s.buildId)
  const machine = familyId ? FAMILIES[familyId].label : getBuild(buildId).label
  const guide = useViewer((s) => s.guide)
  const guideIndex = useViewer((s) => s.guideIndex)
  const steps = useMemo(() => guideSteps(familyId, buildId), [familyId, buildId])
  const step = guide ? steps[Math.min(guideIndex, steps.length - 1)] : null
  const stepText = step ? `Bước ${steps.indexOf(step) + 1}/${steps.length}: ${step.title}` : null
  const suggestions = step ? [`Bước "${step.title}" mình cần chú ý gì?`, ...SUGGESTIONS.slice(0, 3)] : SUGGESTIONS
  const ctx = { buildCode: familyId ?? buildId, guideStep: stepText }

  const { messages, busy, error, send, retry, reset } = useAssemblyChat()
  const [open, setOpen] = useState(false)
  const [draft, setDraft] = useState('')
  const listRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    if (!open) return
    inputRef.current?.focus()
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false)
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open])

  // Có tin mới thì cuộn xuống cuối
  useEffect(() => {
    const el = listRef.current
    if (el) el.scrollTop = el.scrollHeight
  }, [messages, busy, error, open])

  const ask = (text: string) => {
    setDraft('')
    void send(text, ctx)
  }

  if (!open) {
    return (
      <button
        onClick={() => setOpen(true)}
        aria-label="Mở khung hỏi đáp khi lắp máy"
        className="absolute right-4 bottom-20 z-30 flex items-center gap-2 rounded-full bg-ink p-3.5 text-[15px] font-medium text-white shadow-[0_8px_28px_rgba(0,0,0,0.22)] transition-transform duration-100 active:scale-[0.96] md:bottom-5 md:py-3 md:pr-5 md:pl-4"
      >
        <ChatIcon />
        <span className="hidden md:inline">Hỏi khi lắp máy</span>
      </button>
    )
  }

  return (
    <section
      role="dialog"
      aria-label="Hỏi khi lắp máy"
      className="chat-pop fixed inset-x-2 top-12 bottom-2 z-50 flex flex-col overflow-hidden rounded-[24px] bg-[#f7f7f9] shadow-[0_18px_60px_rgba(0,0,0,0.22)] ring-1 ring-black/5 md:absolute md:inset-auto md:right-4 md:bottom-4 md:h-[min(620px,calc(100%-32px))] md:w-[390px]"
    >
      <header className="flex items-center gap-3 border-b border-black/5 bg-white/80 px-4 py-3 backdrop-blur-xl">
        <div className="grid size-9 shrink-0 place-items-center rounded-full bg-ink text-white">
          <ChatIcon />
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-[15px] leading-tight font-semibold">Hỏi khi lắp máy</p>
          <p className="truncate text-[12px] text-ink-2">{stepText ? `${machine} · ${stepText}` : `Đang xem: ${machine}`}</p>
        </div>
        <IconButton label="Cuộc trò chuyện mới" onClick={reset} disabled={messages.length === 0 && !busy}>
          <ResetPath />
        </IconButton>
        <IconButton label="Đóng" onClick={() => setOpen(false)}>
          <path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
        </IconButton>
      </header>

      <div ref={listRef} className="min-h-0 flex-1 space-y-3 overflow-y-auto px-4 py-4" aria-live="polite">
        <Bubble role="assistant">{GREETING}</Bubble>
        {messages.length === 0 && (
          <div className="flex flex-wrap gap-2 pt-1">
            {suggestions.map((s) => (
              <button
                key={s}
                onClick={() => ask(s)}
                className="rounded-full bg-white px-3 py-1.5 text-left text-[13px] text-accent ring-1 ring-black/8 transition-colors duration-150 hover:bg-[#eef5fe] active:scale-[0.98]"
              >
                {s}
              </button>
            ))}
          </div>
        )}
        <Thread messages={messages} busy={busy} error={error} onRetry={() => retry(ctx)} />
      </div>

      <div className="border-t border-black/5 bg-white px-3 pt-3 pb-2">
        <Composer value={draft} onChange={setDraft} onSend={() => ask(draft)} busy={busy} placeholder="Nhập câu hỏi của bạn…" inputRef={inputRef} />
        <p className="mt-1.5 text-center text-[11px] text-ink-3">Trợ lý AI có thể nhầm. Hãy kiểm tra lại trước khi mua hoặc mở máy.</p>
      </div>
    </section>
  )
}
