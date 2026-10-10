import { useEffect, useRef, useState, type ReactNode } from 'react'
import { getBuild } from '../data/builds'
import { FAMILIES } from '../data/families'
import { useViewer } from '../store'

type Msg = { role: 'user' | 'assistant'; content: string }

const GREETING =
  'Chào bạn, mình là trợ lý của PC SmartCare. Bạn cứ hỏi về chọn máy, lắp ráp, nâng cấp hay máy bị nóng, chậm, ồn. Không cần biết thuật ngữ đâu.'

const SUGGESTIONS = [
  'Máy đang xem hợp để làm gì?',
  'Mình có khoảng 15 triệu, nên chọn máy nào?',
  'Máy mình dạo này nóng và ồn, phải làm sao?',
  'Lắp nguồn ATX vào vỏ Mini được không?',
]

/** Gửi tối đa từng này tin gần nhất, đủ để trợ lý nhớ mạch chuyện mà không quá dài */
const HISTORY = 20

/**
 * Khung chat tư vấn. Gọi /api/chat của backend (Vite chuyển tiếp sang cổng 8083), backend mới giữ khoá AI.
 * Hội thoại nằm ở đây nên đóng khung chat rồi mở lại vẫn còn.
 */
export function ChatPanel() {
  const familyId = useViewer((s) => s.familyId)
  const buildId = useViewer((s) => s.buildId)
  const machine = familyId ? FAMILIES[familyId].label : getBuild(buildId).label
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState<Msg[]>([])
  const [draft, setDraft] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const abort = useRef<AbortController | null>(null)
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

  async function send(text: string, history = messages) {
    const content = text.trim()
    if (!content || busy) return
    const next: Msg[] = [...history, { role: 'user', content }]
    setMessages(next)
    setDraft('')
    setError(null)
    setBusy(true)
    const ctrl = new AbortController()
    abort.current = ctrl
    try {
      const res = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ buildCode: familyId ?? buildId, messages: next.slice(-HISTORY) }),
        signal: ctrl.signal,
      })
      const data = await res.json().catch(() => null)
      if (!res.ok) throw new Error(data?.detail ?? (res.status >= 500 ? OFFLINE : 'Trợ lý chưa trả lời được, bạn thử lại nhé.'))
      setMessages([...next, { role: 'assistant', content: data.reply }])
    } catch (e) {
      if (ctrl.signal.aborted) return
      setError(e instanceof TypeError ? OFFLINE : (e as Error).message)
    } finally {
      if (abort.current === ctrl) {
        abort.current = null
        setBusy(false)
      }
    }
  }

  function reset() {
    abort.current?.abort()
    abort.current = null
    setBusy(false)
    setMessages([])
    setError(null)
    inputRef.current?.focus()
  }

  /** Lỗi thì bỏ câu hỏi vừa gửi ra khỏi hội thoại rồi gửi lại đúng câu đó */
  function retry() {
    const last = messages.at(-1)
    if (last?.role === 'user') send(last.content, messages.slice(0, -1))
  }

  if (!open) {
    return (
      <button
        onClick={() => setOpen(true)}
        aria-label="Hỏi trợ lý tư vấn"
        className="absolute right-4 bottom-20 z-30 flex items-center gap-2 rounded-full bg-ink p-3.5 text-[15px] font-medium text-white shadow-[0_8px_28px_rgba(0,0,0,0.22)] transition-transform duration-100 active:scale-[0.96] md:bottom-5 md:py-3 md:pr-5 md:pl-4"
      >
        <ChatIcon />
        <span className="hidden md:inline">Hỏi trợ lý</span>
      </button>
    )
  }

  return (
    <section
      role="dialog"
      aria-label="Trợ lý tư vấn"
      className="chat-pop fixed inset-x-2 top-12 bottom-2 z-50 flex flex-col overflow-hidden rounded-[24px] bg-[#f7f7f9] shadow-[0_18px_60px_rgba(0,0,0,0.22)] ring-1 ring-black/5 md:absolute md:inset-auto md:right-4 md:bottom-4 md:h-[min(620px,calc(100%-32px))] md:w-[390px]"
    >
      <header className="flex items-center gap-3 border-b border-black/5 bg-white/80 px-4 py-3 backdrop-blur-xl">
        <div className="grid size-9 shrink-0 place-items-center rounded-full bg-ink text-white">
          <ChatIcon />
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-[15px] leading-tight font-semibold">Trợ lý tư vấn</p>
          <p className="truncate text-[12px] text-ink-2">Đang xem: {machine}</p>
        </div>
        <IconButton label="Cuộc trò chuyện mới" onClick={reset} disabled={messages.length === 0 && !busy}>
          <path d="M3 8a5 5 0 1 0 1.5-3.5M3 2.5V5h2.5" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
        </IconButton>
        <IconButton label="Đóng" onClick={() => setOpen(false)}>
          <path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
        </IconButton>
      </header>

      <div ref={listRef} className="min-h-0 flex-1 space-y-3 overflow-y-auto px-4 py-4" aria-live="polite">
        <Bubble role="assistant">{GREETING}</Bubble>
        {messages.length === 0 && (
          <div className="flex flex-wrap gap-2 pt-1">
            {SUGGESTIONS.map((s) => (
              <button
                key={s}
                onClick={() => send(s)}
                className="rounded-full bg-white px-3 py-1.5 text-left text-[13px] text-accent ring-1 ring-black/8 transition-colors duration-150 hover:bg-[#eef5fe] active:scale-[0.98]"
              >
                {s}
              </button>
            ))}
          </div>
        )}
        {messages.map((m, i) => (
          <Bubble key={i} role={m.role}>
            {m.role === 'assistant' ? <Rich text={m.content} /> : m.content}
          </Bubble>
        ))}
        {busy && (
          <div className="flex w-fit gap-1 rounded-2xl rounded-bl-md bg-white px-3.5 py-3 ring-1 ring-black/5" aria-label="Trợ lý đang trả lời">
            {[0, 1, 2].map((d) => (
              <span key={d} className="typing size-1.5 rounded-full bg-ink-3" style={{ animationDelay: `${d * 160}ms` }} />
            ))}
          </div>
        )}
        {error && (
          <div className="rounded-2xl bg-[#fff4e5] px-3.5 py-3 text-[13px] leading-snug text-[#7a4b00] ring-1 ring-[#f5d9a8]">
            {error}
            <button onClick={retry} className="mt-1.5 block font-semibold text-accent">
              Thử lại
            </button>
          </div>
        )}
      </div>

      <form
        className="border-t border-black/5 bg-white px-3 pt-3 pb-2"
        onSubmit={(e) => {
          e.preventDefault()
          send(draft)
        }}
      >
        <div className="flex items-end gap-2 rounded-[20px] bg-canvas px-3 py-1.5 ring-1 ring-black/5 focus-within:ring-accent/50">
          <textarea
            ref={inputRef}
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => {
              // Enter gửi, Shift+Enter xuống dòng. Bỏ qua lúc đang gõ dấu tiếng Việt (bộ gõ chưa chốt chữ)
              if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
                e.preventDefault()
                send(draft)
              }
            }}
            rows={1}
            maxLength={2000}
            placeholder="Nhập câu hỏi của bạn…"
            aria-label="Câu hỏi"
            className="field-sizing-content max-h-28 min-h-[30px] flex-1 resize-none bg-transparent py-1 text-[15px] leading-snug outline-none placeholder:text-ink-3"
          />
          <button
            type="submit"
            disabled={!draft.trim() || busy}
            aria-label="Gửi"
            className="mb-0.5 grid size-8 shrink-0 place-items-center rounded-full bg-accent text-white transition-[opacity,transform] duration-100 active:scale-[0.94] disabled:opacity-30"
          >
            <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden>
              <path d="M7 12V2M2.5 6.5 7 2l4.5 4.5" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </button>
        </div>
        <p className="mt-1.5 text-center text-[11px] text-ink-3">Trợ lý AI có thể nhầm. Hãy kiểm tra lại trước khi mua hoặc mở máy.</p>
      </form>
    </section>
  )
}

const OFFLINE = 'Chưa kết nối được máy chủ. Hãy bật backend (cổng 8083) rồi thử lại.'

function Bubble({ role, children }: { role: Msg['role']; children: ReactNode }) {
  return role === 'user' ? (
    <div className="ml-auto w-fit max-w-[85%] rounded-2xl rounded-br-md bg-accent px-3.5 py-2 text-[15px] leading-snug whitespace-pre-wrap text-white">{children}</div>
  ) : (
    <div className="w-fit max-w-[92%] rounded-2xl rounded-bl-md bg-white px-3.5 py-2.5 text-[15px] leading-[1.45] text-ink ring-1 ring-black/5">{children}</div>
  )
}

/** Hiện gạch đầu dòng, danh sách đánh số và **chữ đậm** trong câu trả lời. Không dùng innerHTML. */
function Rich({ text }: { text: string }) {
  const blocks: ReactNode[] = []
  let list: { ordered: boolean; items: string[] } | null = null
  const flush = () => {
    if (!list) return
    const Tag = list.ordered ? 'ol' : 'ul'
    blocks.push(
      <Tag key={blocks.length} className={`my-1 space-y-1 pl-5 ${list.ordered ? 'list-decimal' : 'list-disc'}`}>
        {list.items.map((it, i) => (
          <li key={i}>{inline(it)}</li>
        ))}
      </Tag>,
    )
    list = null
  }
  for (const raw of text.split('\n')) {
    const line = raw.trim()
    const bullet = /^[-*•]\s+(.*)$/.exec(line)
    const num = /^\d+[.)]\s+(.*)$/.exec(line)
    const item = bullet ?? num
    if (item) {
      const ordered = !bullet
      if (list && list.ordered !== ordered) flush()
      list ??= { ordered, items: [] }
      list.items.push(item[1])
      continue
    }
    flush()
    if (line) blocks.push(<p key={blocks.length} className="my-1 first:mt-0 last:mb-0">{inline(line.replace(/^#+\s*/, ''))}</p>)
  }
  flush()
  return <>{blocks}</>
}

function inline(s: string): ReactNode[] {
  return s.split(/(\*\*[^*]+\*\*)/g).map((part, i) =>
    part.startsWith('**') && part.endsWith('**') && part.length > 4 ? (
      <strong key={i} className="font-semibold">
        {part.slice(2, -2)}
      </strong>
    ) : (
      part
    ),
  )
}

function IconButton({ label, onClick, disabled, children }: { label: string; onClick: () => void; disabled?: boolean; children: ReactNode }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      title={label}
      className="grid size-8 shrink-0 place-items-center rounded-full bg-fill text-ink-2 transition-transform duration-100 hover:text-ink active:scale-[0.94] disabled:opacity-40"
    >
      <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden>
        {children}
      </svg>
    </button>
  )
}

function ChatIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden>
      <path
        d="M3 4.5A2.5 2.5 0 0 1 5.5 2h7A2.5 2.5 0 0 1 15 4.5v5a2.5 2.5 0 0 1-2.5 2.5H8l-3.2 2.6c-.33.27-.8.03-.8-.4V12h.5A1.5 1.5 0 0 1 3 10.5z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinejoin="round"
      />
      <circle cx="6.5" cy="7" r=".9" fill="currentColor" />
      <circle cx="9" cy="7" r=".9" fill="currentColor" />
      <circle cx="11.5" cy="7" r=".9" fill="currentColor" />
    </svg>
  )
}
