import { useEffect, useRef, useState } from 'react'
import type { FamilyId } from '../data/families'
import { firstUndone, guideSteps, loadDone } from '../data/guide'
import { useViewer } from '../store'
import { machineOfCard, useAdviceChat, type BuildCard } from './chat/chatStore'
import { Composer, IconButton, ResetPath, Thread } from './chat/ChatParts'
import { ProposalView } from './chat/Proposal'

const press = 'transition-transform duration-100 ease-out active:scale-[0.97] motion-reduce:active:scale-100'
const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'

/** Lời kể mẫu: bấm vào thì chép vào ô nhập để người dùng sửa theo ý mình rồi mới gửi */
const EXAMPLES = [
  'Mình là sinh viên, có khoảng 12 triệu, cần máy nhỏ gọn để học online và làm Word, Excel.',
  'Mình chơi game online và thỉnh thoảng livestream, có khoảng 25 triệu, thích máy có kính.',
  'Mình dựng video cho công việc, máy phải chạy ổn định nhiều giờ, ngân sách trên 40 triệu.',
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
  // Cấu hình đang xem chi tiết: mặc định là cấu hình mới nhất trợ lý đưa ra
  const latest = messages.findLastIndex((m) => m.build)
  // Chọn xem cấu hình cũ; trợ lý đưa cấu hình mới (latest đổi) thì tự quay về cấu hình mới
  const [picked, setPicked] = useState<{ index: number; latest: number } | null>(null)
  const shown = picked && picked.latest === latest && messages[picked.index]?.build ? picked.index : latest
  const card = shown >= 0 ? messages[shown].build! : null
  // Màn hình hẹp: bảng chi tiết mở thành trang phủ kín, bấm "Xem chi tiết linh kiện" mới hiện
  const [sheet, setSheet] = useState(false)

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

  /** Nút "Xem và lắp máy này" (chỉ khi cấu hình lắp được và app có mô hình 3D cho vỏ đó) */
  const buildButton = (c: BuildCard) => {
    const machine = machineOfCard(c)
    if (!c.compatible) return null
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

  // Thẻ trong hội thoại chỉ mở bảng chi tiết; nút "Xem và lắp máy này" chỉ nằm trong bảng để không lặp hai nơi
  const cardAction = (_c: BuildCard, index: number) => (
    <button
      onClick={() => {
        setPicked({ index, latest })
        setSheet(true)
      }}
      aria-pressed={index === shown}
      className={`rounded-full px-4 py-2 text-[14px] font-medium ${press} ${focus} ${
        index === shown ? 'bg-fill text-ink lg:bg-ink lg:text-white' : 'bg-fill text-ink hover:bg-[#dcdce1]'
      }`}
    >
      Xem chi tiết linh kiện
    </button>
  )

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
          <div className="mx-auto grid max-w-[1120px] gap-10 px-5 pt-8 pb-12 md:grid-cols-[minmax(0,1fr)_minmax(0,0.8fr)] md:items-center md:px-10 md:pt-14">
            <div>
              <h1 className="display text-balance">Bạn cần máy tính để làm gì?</h1>
              <p className="mt-3 max-w-[46ch] text-[16px] leading-[1.5] text-ink-2">
                Kể nhu cầu bằng lời của bạn. Trợ lý sẽ chọn linh kiện, ước tính giá và kiểm tra chúng có lắp chung được không.
              </p>
              <div className="mt-6">
                <Composer
                  big
                  value={draft}
                  onChange={setDraft}
                  onSend={ask}
                  busy={busy}
                  inputRef={inputRef}
                  placeholder="Ví dụ: mình học thiết kế, có khoảng 20 triệu, muốn máy êm và không quá to"
                />
              </div>

              <p className="mt-7 px-1 text-[13px] text-ink-2">Chưa biết kể thế nào? Bấm một ví dụ rồi sửa lại cho đúng ý bạn.</p>
              <ul className="mt-2 space-y-1">
                {EXAMPLES.map((ex) => (
                  <li key={ex}>
                    <button
                      onClick={() => {
                        setDraft(ex)
                        inputRef.current?.focus()
                      }}
                      className={`group flex w-full items-start gap-2.5 rounded-xl px-1 py-2 text-left text-[15px] leading-[1.45] text-ink hover:text-accent ${focus}`}
                    >
                      <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden className="mt-[5px] shrink-0 text-ink-3 group-hover:text-accent">
                        <path d="M2 7h9M7.5 3.5 11 7l-3.5 3.5" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
                      </svg>
                      {ex}
                    </button>
                  </li>
                ))}
              </ul>

              <button onClick={() => setPage('build')} className={`mt-8 px-1 text-[14px] font-medium text-accent hover:underline sm:hidden ${focus}`}>
                Bỏ qua, xem mô hình 3D ›
              </button>
            </div>

            {/* Ảnh chụp chính mô hình 3D của app (máy Showcase), để người dùng thấy trước thứ mình sẽ lắp. Từ màn hình vừa trở lên. */}
            <img
              src="/hero/advice-showcase.jpg"
              alt="Mô hình 3D một chiếc máy tính mặt kính trong app, nhìn thấy card đồ họa, tản nước và quạt bên trong"
              width={1200}
              height={919}
              className="hidden aspect-[1200/919] w-full rounded-[28px] bg-[#fafafb] object-cover md:block"
            />
          </div>
        </div>
      ) : (
        <div className="flex min-h-0 flex-1">
          <div className="flex min-w-0 flex-1 flex-col">
            <div ref={listRef} className="min-h-0 flex-1 overflow-y-auto" aria-live="polite">
              <div className="mx-auto max-w-[760px] space-y-4 px-5 py-6">
                <Thread messages={messages} busy={busy} error={error} onRetry={() => retry({})} cardAction={cardAction} compactCards />
              </div>
            </div>
            <div className="shrink-0 border-t border-black/5 bg-canvas px-5 pt-3 pb-3">
              <div className="mx-auto max-w-[760px]">
                <Composer value={draft} onChange={setDraft} onSend={ask} busy={busy} inputRef={inputRef} placeholder="Hỏi thêm hoặc nhờ đổi linh kiện…" />
                <p className="mt-1.5 text-center text-[11px] text-ink-3">Trợ lý AI có thể nhầm. Hãy kiểm tra lại trước khi mua.</p>
              </div>
            </div>
          </div>
          {/* Màn hình rộng: bảng chi tiết luôn nằm bên phải khi đã có cấu hình */}
          {card && (
            <aside key={shown} aria-label="Chi tiết cấu hình" className="swap hidden w-[440px] shrink-0 overflow-y-auto border-l border-black/5 bg-[#f0f0f3] px-5 py-6 lg:block">
              <ProposalView card={card} action={buildButton(card)} />
            </aside>
          )}
        </div>
      )}

      {/* Màn hình hẹp: bảng chi tiết phủ kín màn hình */}
      {sheet && card && (
        <div role="dialog" aria-label="Chi tiết cấu hình" className="chat-pop fixed inset-0 z-50 overflow-y-auto bg-[#f0f0f3] lg:hidden">
          <div className="sticky top-0 z-10 flex items-center justify-between border-b border-black/5 bg-[#f0f0f3]/90 px-5 py-3 backdrop-blur-xl">
            <span className="text-[15px] font-semibold">Chi tiết linh kiện</span>
            <IconButton label="Đóng" onClick={() => setSheet(false)}>
              <path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
            </IconButton>
          </div>
          <div className="mx-auto max-w-[560px] px-5 py-5">
            <ProposalView card={card} action={buildButton(card)} />
          </div>
        </div>
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
