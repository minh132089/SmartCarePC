import { useEffect, useRef, useState } from 'react'
import { ArrowCounterClockwise, ArrowRight, CaretRight, SpeakerHigh, SpeakerSlash, X } from '@phosphor-icons/react'
import DecryptedText from '@/components/DecryptedText'
import DotGrid from '@/components/DotGrid'
import type { FamilyId } from '../data/families'
import { firstUndone, guideSteps, loadDone } from '../data/guide'
import { useViewer } from '../store'
import { machineOfCard, useAdviceChat, type BuildCard, type Pick } from './chat/chatStore'
import { OptionsCanvas } from './chat/Options'
import { TIER_INFO } from './chat/tiers'
import { Composer, IconButton, Thread } from './chat/ChatParts'
import { ProposalView } from './chat/Proposal'
import { StudyBar, StudyJoinLink } from './study/StudyBar'
import { startAmbient, stopAmbient, useSound } from './sound/sfx'
import { useDark } from '../theme'

const press = 'transition-transform duration-100 ease-out active:scale-[0.97] motion-reduce:active:scale-100'
const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'

/** Lời kể mẫu: bấm vào thì chép vào ô nhập để người dùng sửa theo ý mình rồi mới gửi */
const EXAMPLES = [
  'Mình là sinh viên, có khoảng 12 triệu, cần máy nhỏ gọn để học online và làm Word, Excel.',
  'Mình chơi game online và thỉnh thoảng livestream, có khoảng 25 triệu, thích máy có kính.',
  'Mình dựng video cho công việc, máy phải chạy ổn định nhiều giờ, ngân sách trên 40 triệu.',
]

/** Mặc định xem build trợ lý chọn của phương án "Phù hợp cho bạn" */
const DEFAULT_PICK: Pick = { tier: 'balanced', variant: 1 }

/** Trợ lý phải tìm giá ở nhiều cửa hàng nên trả lời mất khoảng 1 đến 2 phút: báo cho người dùng đang làm gì */
const BUSY_HINTS = [
  'Đang đọc nhu cầu của bạn…',
  'Đang tìm giá ở các cửa hàng…',
  'Đang so giá và kiểm tra các linh kiện lắp chung được không…',
  'Sắp xong, đang tổng hợp lại…',
]

/**
 * Trang 1: tư vấn chọn máy bằng AI. Người dùng tự kể nhu cầu, trợ lý phân tích, chọn linh kiện từ danh mục
 * và chạy kiểm tra tương thích. Dưới thẻ cấu hình có nút "Xem và lắp máy này" để sang trang 2 (mô hình 3D).
 * Trang này không vẽ cảnh 3D nên không làm nóng máy người dùng.
 */
export function AdvicePage() {
  const { messages, busy, stage, error, send, retry, reset } = useAdviceChat()
  const setPage = useViewer((s) => s.setPage)
  const open = useOpenMachine()
  const [draft, setDraft] = useState('')
  const inputRef = useRef<HTMLTextAreaElement>(null)
  const listRef = useRef<HTMLDivElement>(null)
  const empty = messages.length === 0 && !busy && !error
  const dark = useDark()
  const { muted, toggle: toggleSound } = useSound()
  // Nhạc nền chỉ ở màn giới thiệu; người dùng bắt đầu chat (hoặc rời trang) thì tắt
  useEffect(() => {
    if (!empty) return
    startAmbient()
    return stopAmbient
  }, [empty])
  // Cấu hình đang xem chi tiết: mặc định là cấu hình mới nhất trợ lý đưa ra
  const latest = messages.findLastIndex((m) => m.build)
  // Chọn xem cấu hình cũ; trợ lý đưa cấu hình mới (latest đổi) thì tự quay về cấu hình mới
  const [picked, setPicked] = useState<{ index: number; latest: number } | null>(null)
  const shown = picked && picked.latest === latest && messages[picked.index]?.build ? picked.index : latest
  // Tin có ba phương án: phương án đang xem theo từng tin (mặc định "Phù hợp cho bạn")
  const [picks, setPicks] = useState<Record<number, Pick>>({})
  const pick = picks[shown] ?? DEFAULT_PICK
  const card =
    shown >= 0
      ? (messages[shown].options?.find((o) => o.tier === pick.tier && (o.variant ?? 1) === pick.variant) ?? messages[shown].build!)
      : null
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
      <>
        <button
          onClick={() => open(machine)}
          className={`flex w-full items-center justify-center gap-1.5 rounded-full bg-accent px-4 py-2.5 text-[14px] font-medium text-white hover:bg-accent-hover ${press} ${focus}`}
        >
          Xem và lắp máy này
          <CaretRight size={13} aria-hidden />
        </button>
        {c.modelNote && <p className="mt-2 text-[12.5px] leading-[1.45] text-ink-2">{c.modelNote}</p>}
      </>
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
        index === shown ? 'bg-fill text-ink lg:bg-ink lg:text-on-ink' : 'bg-fill text-ink hover:bg-fill-hover'
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
          <IconButton
            label={muted ? 'Bật âm thanh' : 'Tắt âm thanh'}
            onClick={toggleSound}
            icon={muted ? <SpeakerSlash size={16} aria-hidden /> : <SpeakerHigh size={16} aria-hidden />}
          />
          {!empty && (
            <IconButton label="Cuộc trò chuyện mới" onClick={reset} icon={<ArrowCounterClockwise size={16} aria-hidden />} />
          )}
          <button onClick={() => setPage('build')} className={`hidden text-[14px] font-medium text-link hover:underline sm:block ${focus}`}>
            Bỏ qua, xem mô hình 3D ›
          </button>
        </div>
      </header>
      <div className="shrink-0 px-5 md:px-10">
        <StudyBar />
      </div>

      {empty ? (
        <div className="relative min-h-0 flex-1">
          {/* Nền chấm phản ứng theo con trỏ (React Bits DotGrid). Chỉ có ở màn giới thiệu: người dùng gửi câu đầu tiên là gỡ hẳn. */}
          <div aria-hidden className="absolute inset-0">
            <DotGrid
              dotSize={4}
              gap={22}
              baseColor={dark ? '#3a3b43' : '#c6c6ce'}
              activeColor={dark ? '#2997ff' : '#0071e3'}
              proximity={130}
              shockRadius={260}
              shockStrength={4}
              glow={0.6}
              fade={0.9}
            />
          </div>
          <div className="relative h-full overflow-y-auto">
            <div className="mx-auto flex min-h-full max-w-[720px] flex-col justify-center px-5 pt-8 pb-12 md:px-10">
              <p className="text-center font-mono text-[12px] tracking-[0.18em] text-ink-3 uppercase">SmartCare PC · trợ lý build máy</p>
              <h1 className="mt-3 text-center font-mono text-[clamp(2.25rem,1.4rem+3.6vw,4.25rem)] leading-[1.05] font-semibold tracking-[-0.03em] text-ink">
                <span className="sr-only">Create your own PC</span>
                <span aria-hidden>
                  <DecryptedText
                    text="Create your own PC"
                    animateOn="inViewHover"
                    sequential
                    revealDirection="start"
                    speed={45}
                    maxIterations={14}
                    characters="01<>/[]{}#$%&*+=ABCDEF"
                    encryptedClassName="text-accent"
                  />
                </span>
              </h1>
              <p className="mx-auto mt-4 max-w-[48ch] text-center text-[16px] leading-[1.5] text-ink-2">
                Kể nhu cầu bằng lời của bạn. Trợ lý sẽ chọn linh kiện, so giá ở các cửa hàng và kiểm tra chúng có lắp chung được không.
              </p>
              <div className="mt-7">
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
                      className={`group flex w-full items-start gap-2.5 rounded-xl px-1 py-2 text-left text-[15px] leading-[1.45] text-ink hover:text-link ${focus}`}
                    >
                      <ArrowRight size={14} aria-hidden className="mt-[5px] shrink-0 text-ink-3 group-hover:text-link" />
                      {ex}
                    </button>
                  </li>
                ))}
              </ul>

              <button onClick={() => setPage('build')} className={`mt-8 self-start px-1 text-[14px] font-medium text-link hover:underline sm:hidden ${focus}`}>
                Bỏ qua, xem mô hình 3D ›
              </button>
              <StudyJoinLink className="mt-8 block px-1 text-left" />
            </div>
          </div>
        </div>
      ) : (
        <div className="flex min-h-0 flex-1">
          <div className="flex min-w-0 flex-1 flex-col">
            <div ref={listRef} className="min-h-0 flex-1 overflow-y-auto" aria-live="polite">
              <div className="mx-auto max-w-[760px] space-y-4 px-5 py-6">
                <Thread
                  messages={messages}
                  busy={busy}
                  stage={stage}
                  error={error}
                  onRetry={() => retry({})}
                  cardAction={cardAction}
                  compactCards
                  busyHints={BUSY_HINTS}
                  optionsView={(m, i) =>
                    m.options ? (
                      <OptionsCanvas
                        options={m.options}
                        selected={picks[i] ?? DEFAULT_PICK}
                        onSelect={(p) => {
                          setPicks((all) => ({ ...all, [i]: p }))
                          setPicked({ index: i, latest })
                          setSheet(true) // màn hình hẹp: mở bảng chi tiết; màn rộng bảng đã nằm bên phải
                        }}
                      />
                    ) : null
                  }
                />
              </div>
            </div>
            <div className="shrink-0 border-t border-line bg-canvas px-5 pt-3 pb-3">
              <div className="mx-auto max-w-[760px]">
                <Composer value={draft} onChange={setDraft} onSend={ask} busy={busy} inputRef={inputRef} placeholder="Hỏi thêm hoặc nhờ đổi linh kiện…" />
                <p className="mt-1.5 text-center text-[11px] text-ink-3">Trợ lý AI có thể nhầm. Hãy kiểm tra lại trước khi mua.</p>
              </div>
            </div>
          </div>
          {/* Màn hình rộng: bảng chi tiết luôn nằm bên phải khi đã có cấu hình */}
          {card && (
            <aside key={`${shown}-${card.tier ?? ''}-${card.variant ?? 1}`} aria-label="Chi tiết cấu hình" className="swap hidden w-[440px] shrink-0 overflow-y-auto border-l border-line bg-panel px-5 py-6 lg:block">
              {card.tier && <p className="mb-2 text-[13px] font-semibold text-accent">Phương án: {TIER_INFO[card.tier].label}</p>}
              <ProposalView card={card} action={buildButton(card)} />
            </aside>
          )}
        </div>
      )}

      {/* Màn hình hẹp: bảng chi tiết phủ kín màn hình */}
      {sheet && card && (
        <div role="dialog" aria-label="Chi tiết cấu hình" className="chat-pop fixed inset-0 z-50 overflow-y-auto bg-panel lg:hidden">
          <div className="sticky top-0 z-10 flex items-center justify-between border-b border-line bg-panel/90 px-5 py-3 backdrop-blur-xl">
            <span className="text-[15px] font-semibold">Chi tiết linh kiện</span>
            <IconButton label="Đóng" onClick={() => setSheet(false)} icon={<X size={16} aria-hidden />} />
          </div>
          <div className="mx-auto max-w-[560px] px-5 py-5">
            {card.tier && <p className="mb-2 text-[13px] font-semibold text-accent">Phương án: {TIER_INFO[card.tier].label}</p>}
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
            {i > 0 && <span className="h-px w-5 bg-line-strong" aria-hidden />}
            <button
              onClick={() => setPage(it.page)}
              aria-current={on ? 'step' : undefined}
              className={`flex items-center gap-1.5 rounded-full py-1 pr-3 pl-1 ${focus} ${on ? 'bg-surface font-semibold text-ink' : 'text-ink-2 hover:text-ink'}`}
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
