import { useEffect, useRef, useState } from 'react'
import type { FamilyId } from '../data/families'
import { firstUndone, guideSteps, loadDone } from '../data/guide'
import { useViewer } from '../store'
import { machineOfCard, useAdviceChat, type BuildCard } from './chat/chatStore'
import { Composer, IconButton, ResetPath, Thread } from './chat/ChatParts'

const press = 'transition-transform duration-100 ease-out active:scale-[0.97] motion-reduce:active:scale-100'
const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'

/** Lời kể mẫu: bấm vào thì chép vào ô nhập để người dùng sửa theo ý mình rồi mới gửi */
const EXAMPLES = [
  'Mình là sinh viên, học online và làm Word, Excel. Có khoảng 12 triệu, muốn máy nhỏ gọn để trên bàn.',
  'Mình hay chơi game online với bạn bè, thỉnh thoảng livestream. Ngân sách khoảng 25 triệu, thích máy có kính và đèn.',
  'Mình dựng video cho công việc, cần máy chạy ổn định nhiều giờ liền. Ngân sách trên 40 triệu.',
  'Mình không rành máy tính, chỉ cần máy bền, êm, cả nhà dùng để xem phim và làm giấy tờ.',
]

/**
 * Trang 1: tư vấn chọn máy bằng AI. Người dùng tự kể nhu cầu, trợ lý phân tích, chọn linh kiện từ danh mục
 * và chạy kiểm tra tương thích. Dưới thẻ cấu hình có nút "Xem và lắp máy này" để sang trang 2 (mô hình 3D).
 * Trang này không vẽ cảnh 3D nên không làm nóng máy người dùng.
 */
export function AdvicePage() {
  const { messages, busy, error, send, retry, reset } = useAdviceChat()
  const setPage = useViewer((s) => s.setPage)
  const open = useOpenMachine()
  const [draft, setDraft] = useState('')
  const inputRef = useRef<HTMLTextAreaElement>(null)
  const listRef = useRef<HTMLDivElement>(null)
  const empty = messages.length === 0 && !busy && !error

  // Có tin mới thì cuộn xuống cuối
  useEffect(() => {
    const el = listRef.current
    if (el) el.scrollTop = el.scrollHeight
  }, [messages, busy, error])

  const ask = () => {
    const text = draft
    setDraft('')
    void send(text, {})
  }

  const cardAction = (card: BuildCard) => {
    const machine = machineOfCard(card)
    if (!card.compatible) return null
    if (!machine) return <p className="text-[12.5px] text-ink-2">App chưa có mô hình 3D cho kiểu vỏ này.</p>
    return (
      <button
        onClick={() => open(machine)}
        className={`flex w-full items-center justify-center gap-1.5 rounded-full bg-accent px-4 py-2.5 text-[14px] font-medium text-white hover:bg-accent-hover ${press} ${focus}`}
      >
        Xem và lắp máy này
        <svg width="12" height="12" viewBox="0 0 12 12" aria-hidden>
          <path d="M4.5 2.5 8 6l-3.5 3.5" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>
    )
  }

  return (
    <div className="flex h-full flex-col bg-canvas">
      <header className="flex shrink-0 items-center justify-between gap-3 px-5 pt-5 pb-2 md:px-10">
        <FlowSteps current={1} />
        <div className="flex items-center gap-2">
          {!empty && (
            <IconButton label="Cuộc trò chuyện mới" onClick={reset}>
              <ResetPath />
            </IconButton>
          )}
          <button onClick={() => setPage('build')} className={`hidden text-[14px] font-medium text-accent hover:underline sm:block ${focus}`}>
            Bỏ qua, xem mô hình 3D ›
          </button>
        </div>
      </header>

      {empty ? (
        <div className="min-h-0 flex-1 overflow-y-auto">
          <div className="mx-auto max-w-[720px] px-5 pt-[6vh] pb-12">
            <p className="text-[13px] font-semibold text-ink-2">PC SmartCare · Tư vấn bằng AI</p>
            <h1 className="display mt-1 text-balance">Bạn cần máy tính để làm gì?</h1>
            <p className="mt-3 text-[15px] leading-[1.5] text-ink-2">
              Kể bằng lời của bạn, như đang nói với người bán hàng. Trợ lý AI sẽ phân tích nhu cầu, chọn từng linh kiện và kiểm tra xem chúng lắp chung được không.
            </p>
            <div className="mt-6">
              <Composer
                big
                value={draft}
                onChange={setDraft}
                onSend={ask}
                busy={busy}
                inputRef={inputRef}
                placeholder="Ví dụ: Mình học thiết kế đồ họa, ngân sách khoảng 20 triệu, muốn máy êm và không quá to…"
              />
              <p className="mt-2 px-1 text-[12px] text-ink-3">Nên kể: dùng máy làm gì, ngân sách khoảng bao nhiêu, muốn máy to hay nhỏ.</p>
            </div>

            <p className="mt-8 px-1 text-[13px] font-semibold text-ink-2">Hoặc bắt đầu từ một ví dụ</p>
            <div className="mt-2 grid gap-2 sm:grid-cols-2">
              {EXAMPLES.map((ex) => (
                <button
                  key={ex}
                  onClick={() => {
                    setDraft(ex)
                    inputRef.current?.focus()
                  }}
                  className={`rounded-2xl bg-white px-4 py-3 text-left text-[14px] leading-[1.45] text-ink ring-1 ring-black/[0.06] hover:ring-black/15 ${press} ${focus}`}
                >
                  {ex}
                </button>
              ))}
            </div>

            <button onClick={() => setPage('build')} className={`mt-10 px-1 text-[14px] font-medium text-accent hover:underline sm:hidden ${focus}`}>
              Bỏ qua, xem mô hình 3D ›
            </button>
          </div>
        </div>
      ) : (
        <>
          <div ref={listRef} className="min-h-0 flex-1 overflow-y-auto" aria-live="polite">
            <div className="mx-auto max-w-[760px] space-y-4 px-5 py-6">
              <Thread messages={messages} busy={busy} error={error} onRetry={() => retry({})} cardAction={cardAction} />
            </div>
          </div>
          <div className="shrink-0 border-t border-black/5 bg-canvas px-5 pt-3 pb-3">
            <div className="mx-auto max-w-[760px]">
              <Composer value={draft} onChange={setDraft} onSend={ask} busy={busy} inputRef={inputRef} placeholder="Hỏi thêm hoặc nhờ đổi linh kiện…" />
              <p className="mt-1.5 text-center text-[11px] text-ink-3">Trợ lý AI có thể nhầm. Hãy kiểm tra lại trước khi mua.</p>
            </div>
          </div>
        </>
      )}
    </div>
  )
}

/** Mở trang hướng dẫn với máy đã chọn và bật luôn hướng dẫn lắp, tới bước còn dở */
function useOpenMachine() {
  const s = useViewer()
  return (id: FamilyId | 'gaming') => {
    if (id === 'gaming') s.setBuild('gaming')
    else s.setFamily(id)
    const steps = guideSteps(id === 'gaming' ? null : id, 'gaming')
    s.setGuideIndex(firstUndone(steps, loadDone(id)))
    s.setGuide(true)
    s.setPage('build')
  }
}

/** Thanh hai bước ở đầu trang: 1 Tư vấn, 2 Lắp máy. Dùng chung cho cả hai trang. */
export function FlowSteps({ current }: { current: 1 | 2 }) {
  const setPage = useViewer((s) => s.setPage)
  const items = [
    { n: 1, label: 'Tư vấn chọn máy', short: 'Tư vấn', page: 'advice' as const },
    { n: 2, label: 'Hướng dẫn lắp máy', short: 'Lắp máy', page: 'build' as const },
  ]
  return (
    <nav aria-label="Các bước" className="flex items-center gap-2 text-[13px]">
      {items.map((it, i) => {
        const on = it.n === current
        return (
          <div key={it.n} className="flex items-center gap-2">
            {i > 0 && <span className="h-px w-5 bg-black/15" aria-hidden />}
            <button
              onClick={() => setPage(it.page)}
              aria-current={on ? 'step' : undefined}
              className={`flex items-center gap-1.5 rounded-full py-1 pr-3 pl-1 ${focus} ${on ? 'bg-white font-semibold text-ink' : 'text-ink-2 hover:text-ink'}`}
            >
              <span className={`grid size-5 place-items-center rounded-full text-[11px] font-semibold ${on ? 'bg-accent text-white' : 'bg-fill text-ink-2'}`}>
                {it.n}
              </span>
              <span className="whitespace-nowrap sm:hidden">{it.short}</span>
              <span className="hidden whitespace-nowrap sm:inline">{it.label}</span>
            </button>
          </div>
        )
      })}
    </nav>
  )
}
