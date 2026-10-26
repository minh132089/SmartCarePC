import { useEffect, useMemo, useRef, useState } from 'react'
import { ArrowCounterClockwise, ChatCircleDots, X } from '@phosphor-icons/react'
import { getBuild } from '../data/builds'
import { FAMILIES } from '../data/families'
import { guideChatContext, guideSteps } from '../data/guide'
import { useViewer } from '../store'
import { useAssemblyChat } from './chat/chatStore'
import { Bubble, Composer, IconButton, Thread } from './chat/ChatParts'

const GREETING =
  "Need help while building? Describe what looks unfamiliar or difficult. I can see your current assembly step and help you continue safely."

/** Những chỗ người mới hay vướng khi tự lắp */
const SUGGESTIONS = [
  "The CPU does not sit flat in its socket",
  "The RAM will not fit into the slot",
  "Where do I connect the power button cable?",
  "The fans spin, but there is no display",
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
  const advised = useViewer((s) => s.advisedBuild)
  const hasGpu = advised == null || advised.items.some((i) => i.category === 'gpu')
  const steps = useMemo(() => guideSteps(familyId, buildId, hasGpu), [familyId, buildId, hasGpu])
  const step = guide ? steps[Math.min(guideIndex, steps.length - 1)] : null
  const stepText = step ? `Step ${Math.min(guideIndex, steps.length - 1) + 1}/${steps.length}: ${step.title}` : null
  const suggestions = step ? [`What should I watch for in the step: ${step.title}?`, ...SUGGESTIONS.slice(0, 3)] : SUGGESTIONS
  const ctx = { buildCode: familyId ?? buildId, guideStep: guideChatContext(familyId, buildId, guide ? guideIndex : null, advised) }

  const { messages, busy, stage, error, send, retry, reset } = useAssemblyChat()
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
        aria-label="Open assembly help"
        className="absolute right-4 bottom-20 z-30 flex items-center gap-2 rounded-full bg-ink p-3.5 text-[15px] font-medium text-on-ink shadow-[0_8px_28px_rgba(0,0,0,0.22)] transition-transform duration-100 active:scale-[0.96] md:bottom-5 md:py-3 md:pr-5 md:pl-4"
      >
        <ChatCircleDots size={19} aria-hidden />
        <span className="hidden md:inline">Assembly help</span>
      </button>
    )
  }

  return (
    <section
      role="dialog"
      aria-label="Assembly help"
      className="chat-pop fixed inset-x-2 top-12 bottom-2 z-50 flex flex-col overflow-hidden rounded-[24px] bg-panel shadow-[0_18px_60px_rgba(0,0,0,0.22)] ring-1 ring-line md:absolute md:inset-auto md:right-4 md:bottom-4 md:h-[min(620px,calc(100%-32px))] md:w-[390px]"
    >
      <header className="flex items-center gap-3 border-b border-line bg-glass px-4 py-3 backdrop-blur-xl">
        <div className="grid size-9 shrink-0 place-items-center rounded-full bg-ink text-on-ink">
          <ChatCircleDots size={19} aria-hidden />
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-[15px] leading-tight font-semibold">Assembly help</p>
          <p className="truncate text-[12px] text-ink-2">{stepText ? `${machine} · ${stepText}` : `Viewing: ${machine}`}</p>
        </div>
        <IconButton label="New conversation" onClick={reset} disabled={messages.length === 0 && !busy} icon={<ArrowCounterClockwise size={16} aria-hidden />} />
        <IconButton label="Close" onClick={() => setOpen(false)} icon={<X size={16} aria-hidden />} />
      </header>

      <div ref={listRef} className="min-h-0 flex-1 space-y-3 overflow-y-auto px-4 py-4" aria-live="polite">
        <Bubble role="assistant">{GREETING}</Bubble>
        {messages.length === 0 && (
          <div className="flex flex-wrap gap-2 pt-1">
            {suggestions.map((s) => (
              <button
                key={s}
                onClick={() => ask(s)}
                className="rounded-full bg-surface px-3 py-1.5 text-left text-[13px] text-link ring-1 ring-line transition-colors duration-150 hover:bg-info-bg active:scale-[0.98]"
              >
                {s}
              </button>
            ))}
          </div>
        )}
        <Thread messages={messages} busy={busy} stage={stage} error={error} onRetry={() => retry(ctx)} />
      </div>

      <div className="border-t border-line bg-surface px-3 pt-3 pb-2">
        <Composer value={draft} onChange={setDraft} onSend={() => ask(draft)} busy={busy} placeholder="Type your question…" inputRef={inputRef} />
        <p className="mt-1.5 text-center text-[11px] text-ink-3">AI can make mistakes. Verify advice before buying or working inside a PC.</p>
      </div>
    </section>
  )
}
