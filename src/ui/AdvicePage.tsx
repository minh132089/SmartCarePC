import { useEffect, useRef, useState, type MouseEvent, type RefObject } from 'react'
import { ArrowCounterClockwise, CaretRight, DiceFive, SpeakerHigh, SpeakerSlash } from '@phosphor-icons/react'
import CardNav, { type CardNavItem, type CardNavLink } from '@/components/CardNav'
import MicroSlats from '@/components/MicroSlats'
import PixelSwap from '@/components/PixelSwap'
import StaggeredMenu from '@/components/StaggeredMenu'
import TechText from '@/components/TechText'
import type { FamilyId } from '../data/families'
import { firstUndone, guideSteps, loadDone } from '../data/guide'
import { useViewer } from '../store'
import { useDark } from '../theme'
import { machineOfCard, useAdviceChat, type BuildCard, type Pick } from './chat/chatStore'
import { OptionsCanvas } from './chat/Options'
import { TIER_INFO } from './chat/tiers'
import { Composer, IconButton, Thread } from './chat/ChatParts'
import { ProposalView } from './chat/Proposal'
import { playRandom, useSound } from './sound/sfx'
import { useStudy } from './study/study'
import { StudyBar } from './study/StudyBar'

const press = 'transition-transform duration-100 ease-out active:scale-[0.97] motion-reduce:active:scale-100'
const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'

/**
 * Lời kể mẫu cho nút ngẫu nhiên cạnh nút gửi: chép vào ô nhập để người dùng sửa theo ý mình rồi mới gửi. label là chữ
 * ngắn dùng trong thanh CardNav.
 */
const EXAMPLES = [
  { label: 'Sinh viên, 12 triệu', text: 'Mình là sinh viên, có khoảng 12 triệu, cần máy nhỏ gọn để học online và làm Word, Excel.' },
  { label: 'Chơi game, 25 triệu', text: 'Mình chơi game online và thỉnh thoảng livestream, có khoảng 25 triệu, thích máy có kính.' },
  { label: 'Dựng video, 40 triệu', text: 'Mình dựng video cho công việc, máy phải chạy ổn định nhiều giờ, ngân sách khoảng 40 triệu.' },
  { label: 'Văn phòng, 10 triệu', text: 'Mình cần máy cho văn phòng nhỏ, chạy phần mềm kế toán và in ấn, khoảng 10 triệu, càng êm càng tốt.' },
  { label: 'Thiết kế, 20 triệu', text: 'Mình học thiết kế đồ họa, dùng Photoshop và Illustrator, có khoảng 20 triệu, muốn máy êm và không quá to.' },
  { label: 'Lập trình, 18 triệu', text: 'Mình là lập trình viên, hay mở nhiều tab và chạy Docker, có khoảng 18 triệu, cần nhiều RAM để sau này nâng cấp.' },
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
 *
 * Giao diện dùng React Bits: màn giới thiệu (CardNav, nền MicroSlats, chữ TechText) chuyển sang khung chat bằng
 * PixelSwap khi người dùng gửi câu đầu tiên; xong chuyển cảnh thì gỡ hẳn màn giới thiệu để nền không chạy nữa.
 * Chi tiết linh kiện mở trong ngăn StaggeredMenu.
 */
export function AdvicePage() {
  const { messages, busy, stage, error, send, retry, reset } = useAdviceChat()
  const setPage = useViewer((s) => s.setPage)
  const open = useOpenMachine()
  const dark = useDark()
  const { muted, toggle: toggleSound } = useSound()
  const [draft, setDraft] = useState('')
  const introInputRef = useRef<HTMLTextAreaElement>(null)
  const chatInputRef = useRef<HTMLTextAreaElement>(null)
  const listRef = useRef<HTMLDivElement>(null)
  const empty = messages.length === 0 && !busy && !error
  // Chuyển cảnh sang khung chat xong thì gỡ màn giới thiệu (nền WebGL, chữ canvas) cho máy nhẹ
  const [introGone, setIntroGone] = useState(!empty)
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
  // Ngăn chi tiết linh kiện (StaggeredMenu), mở bằng nút "Xem chi tiết linh kiện" hoặc bấm một phương án
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

  // Thẻ trong hội thoại chỉ mở ngăn chi tiết; nút "Xem và lắp máy này" chỉ nằm trong ngăn để không lặp hai nơi
  const cardAction = (_c: BuildCard, index: number) => (
    <button
      data-sm-opener
      onClick={() => {
        setPicked({ index, latest })
        setSheet(true)
      }}
      aria-pressed={index === shown}
      className={`rounded-full px-4 py-2 text-[14px] font-medium ${press} ${focus} ${
        index === shown ? 'bg-ink text-on-ink' : 'bg-fill text-ink hover:bg-fill-hover'
      }`}
    >
      Xem chi tiết linh kiện
    </button>
  )

  const soundButton = (
    <IconButton
      label={muted ? 'Bật âm thanh' : 'Tắt âm thanh'}
      onClick={toggleSound}
      icon={muted ? <SpeakerSlash size={16} aria-hidden /> : <SpeakerHigh size={16} aria-hidden />}
    />
  )

  const chat = (
    <div className="flex h-full flex-col bg-canvas">
      <header className="flex shrink-0 items-center justify-between gap-3 px-5 pt-5 pb-2 md:px-10">
        <FlowSteps current={1} />
        <div className="flex items-center gap-2">
          {soundButton}
          <IconButton label="Cuộc trò chuyện mới" onClick={reset} icon={<ArrowCounterClockwise size={16} aria-hidden />} />
          <button onClick={() => setPage('build')} className={`hidden text-[14px] font-medium text-link hover:underline sm:block ${focus}`}>
            Bỏ qua, xem mô hình 3D ›
          </button>
        </div>
      </header>
      <div className="shrink-0 px-5 md:px-10">
        <StudyBar />
      </div>
      <div ref={listRef} className="min-h-0 flex-1 overflow-y-auto" aria-live="polite">
        <div className="mx-auto max-w-[820px] space-y-4 px-5 py-6">
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
                    setSheet(true)
                  }}
                />
              ) : null
            }
          />
        </div>
      </div>
      <div className="shrink-0 border-t border-line bg-canvas px-5 pt-3 pb-3">
        <div className="mx-auto max-w-[820px]">
          <Composer value={draft} onChange={setDraft} onSend={ask} busy={busy} inputRef={chatInputRef} placeholder="Hỏi thêm hoặc nhờ đổi linh kiện…" />
          <p className="mt-1.5 text-center text-[11px] text-ink-3">Trợ lý AI có thể nhầm. Hãy kiểm tra lại trước khi mua.</p>
        </div>
      </div>
    </div>
  )

  return (
    <div className="flex h-full flex-col bg-canvas">
      <PixelSwap
        className="min-h-0 flex-1"
        style={{ aspectRatio: 'auto' }}
        active={!empty}
        trigger="none"
        pattern="bottom-to-top"
        pixelSize={56}
        pixelRadius={18}
        duration={520}
        pixelDuration={380}
        randomness={0.35}
        // Không chạy hiệu ứng hiện dần lúc mở trang: máy yếu chạy chậm làm tiêu đề mấy giây sau mới thấy
        intro={false}
        onComplete={(toChat) => {
          setIntroGone(toChat)
          if (toChat) chatInputRef.current?.focus()
        }}
        firstContent={
          empty || !introGone ? (
            <Intro
              dark={dark}
              draft={draft}
              setDraft={setDraft}
              ask={ask}
              busy={busy}
              inputRef={introInputRef}
              soundButton={soundButton}
            />
          ) : null
        }
        secondContent={chat}
      />

      {card && (
        <StaggeredMenu
          isFixed
          position="right"
          open={sheet}
          onOpenChange={setSheet}
          hideToggleWhenClosed
          panelLabel="chi tiết linh kiện"
          menuText="Linh kiện"
          closeText="Đóng"
          displaySocials={false}
          displayItemNumbering={false}
          colors={dark ? ['#16253a', '#0a4fa3'] : ['#dcebfd', '#0071e3']}
          accentColor={dark ? '#2997ff' : '#0071e3'}
          menuButtonColor={dark ? '#f5f5f7' : '#1d1d1f'}
          openMenuButtonColor={dark ? '#f5f5f7' : '#1d1d1f'}
          className="[--sm-panel-bg:var(--color-panel)] [--sm-width:clamp(400px,44vw,560px)]"
        >
          <div key={`${shown}-${card.tier ?? ''}-${card.variant ?? 1}`} className="text-ink">
            {card.tier && (
              <p data-sm-item className="mb-2 text-[13px] font-semibold text-accent">
                Phương án: {TIER_INFO[card.tier].label}
              </p>
            )}
            <ProposalView card={card} action={buildButton(card)} />
          </div>
        </StaggeredMenu>
      )}
    </div>
  )
}

/** Mục trong thanh CardNav ở màn giới thiệu */
const NAV_ITEMS: CardNavItem[] = [
  {
    label: 'Tư vấn',
    description: 'Kể nhu cầu, trợ lý chọn linh kiện và so giá ở các cửa hàng.',
    links: [
      ...EXAMPLES.slice(0, 3).map((ex) => ({ label: ex.label, href: '#' })),
    ],
  },
  {
    label: 'Lắp máy 3D',
    description: 'Xem mô hình máy và làm theo hướng dẫn từng bước.',
    links: [
      { label: 'Xem mô hình 3D', href: '#' },
      { label: 'Hướng dẫn lắp máy', href: '#' },
    ],
  },
  {
    label: 'Nghiên cứu',
    description: 'Dành cho người tham gia thử nghiệm của nhóm.',
    links: [{ label: 'Nhập mã tham gia', href: '#' }],
  },
]

/**
 * Màn giới thiệu: thanh CardNav, nền MicroSlats phản ứng theo con trỏ, chữ "Create your own PC" bằng TechText,
 * ô chat ngay dưới. Lớp phủ mờ giữa màn giữ chữ dễ đọc trên nền sóng.
 */
function Intro({
  dark,
  draft,
  setDraft,
  ask,
  busy,
  inputRef,
  soundButton,
}: {
  dark: boolean
  draft: string
  setDraft: (v: string) => void
  ask: () => void
  busy: boolean
  inputRef: RefObject<HTMLTextAreaElement | null>
  soundButton: React.ReactNode
}) {
  const setPage = useViewer((s) => s.setPage)
  const setGuide = useViewer((s) => s.setGuide)
  const openStudy = useStudy((s) => s.open)
  const fill = (text: string) => {
    setDraft(text)
    inputRef.current?.focus()
  }
  /** Nút ngẫu nhiên: lấy một lời kể mẫu khác với chữ đang có trong ô */
  const random = () => {
    playRandom()
    const others = EXAMPLES.filter((ex) => ex.text !== draft)
    fill(others[Math.floor(Math.random() * others.length)].text)
  }
  const onLink = (link: CardNavLink, _item: CardNavItem, event: MouseEvent<HTMLElement>) => {
    event.preventDefault()
    const example = EXAMPLES.find((ex) => ex.label === link.label)
    if (example) fill(example.text)
    else if (link.label === 'Xem mô hình 3D') setPage('build')
    else if (link.label === 'Hướng dẫn lắp máy') {
      setGuide(true)
      setPage('build')
    } else if (link.label === 'Nhập mã tham gia') openStudy('join')
  }

  return (
    <div className="relative h-full overflow-hidden bg-canvas">
      <div aria-hidden className="absolute inset-0">
        <MicroSlats
          preset="swell"
          color={dark ? '#0a4fa3' : '#c4daf5'}
          glintColor={dark ? '#7cb8ff' : '#0071e3'}
          backgroundColor={dark ? '#111113' : '#f5f5f7'}
          slatWidth={9}
          slatHeight={22}
          gap={3}
          cursorStrength={1.2}
          cursorSize={50}
        />
      </div>
      {/* Lớp phủ: giữa màn (chỗ chữ và ô chat) gần như màu nền, ra mép mới thấy rõ sóng */}
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0"
        style={{
          background:
            'radial-gradient(ellipse 62% 72% at 50% 50%, var(--color-canvas) 0%, color-mix(in srgb, var(--color-canvas) 88%, transparent) 45%, color-mix(in srgb, var(--color-canvas) 20%, transparent) 100%)',
        }}
      />

      <div className="relative flex h-full flex-col overflow-y-auto">
        <div className="relative z-20 shrink-0 px-4 pt-4">
          <CardNav
            logo={<span className="text-[15px] font-semibold tracking-[-0.01em]">SmartCare PC</span>}
            logoAlt="SmartCare PC"
            items={NAV_ITEMS}
            theme={dark ? 'dark' : 'light'}
            surface="glass"
            accentColor={dark ? '#2997ff' : '#0071e3'}
            maxWidth={820}
            cardHeight={150}
            cta={{ label: 'Bắt đầu', onClick: () => inputRef.current?.focus() }}
            onLinkClick={onLink}
            ariaLabel="Điều hướng"
          />
        </div>
        <div className="absolute top-5 right-5 z-30 hidden xl:block">{soundButton}</div>

        <div className="mx-auto flex w-full max-w-[760px] flex-1 flex-col justify-center px-5 pt-6 pb-10 md:px-10">
          <p className="text-center font-mono text-[12px] tracking-[0.18em] text-ink-2 uppercase">SmartCare PC · trợ lý build máy</p>
          <h1 className="mt-2 text-ink">
            <span className="sr-only">Create your own PC</span>
            <div aria-hidden className="h-[clamp(84px,13vw,150px)] w-full">
              <TechText
                text="Create your own PC"
                fontFamily="-apple-system, BlinkMacSystemFont, 'SF Pro Display', system-ui, sans-serif"
                fontWeight={700}
                fontSize={120}
                letterSpacing={-0.04}
                color="currentColor"
                accentColor={dark ? '#2997ff' : '#0071e3'}
                intro="none"
                draggable={false}
              />
            </div>
          </h1>
          <div className="mt-6">
            <Composer
              big
              value={draft}
              onChange={setDraft}
              onSend={ask}
              busy={busy}
              inputRef={inputRef}
              placeholder="Ví dụ: mình học thiết kế, có khoảng 20 triệu, muốn máy êm và không quá to"
              extra={
                <button
                  type="button"
                  onClick={random}
                  aria-label="Điền một gợi ý ngẫu nhiên"
                  title="Gợi ý ngẫu nhiên"
                  className={`grid size-10 shrink-0 place-items-center rounded-full bg-fill text-ink-2 hover:bg-fill-hover hover:text-ink ${press} ${focus}`}
                >
                  <DiceFive size={18} aria-hidden />
                </button>
              }
            />
          </div>

          <div className="mt-4 flex justify-end xl:hidden">{soundButton}</div>
        </div>
      </div>
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
