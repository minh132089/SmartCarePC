import { useEffect, useState, type ReactNode, type Ref } from 'react'
import { ArrowUp, Check, X } from '@phosphor-icons/react'
import { PARTS, type PartId } from '../../data/parts'
import { compatText, totalOf, vndRange, type BuildCard, type Msg } from './chatStore'

/** Bong bóng tin nhắn: người dùng bên phải màu xanh, trợ lý bên trái màu trắng */
export function Bubble({ role, children }: { role: Msg['role']; children: ReactNode }) {
  return role === 'user' ? (
    <div className="ml-auto w-fit max-w-[85%] rounded-2xl rounded-br-md bg-accent px-3.5 py-2 text-[15px] leading-snug whitespace-pre-wrap text-white">{children}</div>
  ) : (
    <div className="w-fit max-w-[92%] rounded-2xl rounded-bl-md bg-surface px-3.5 py-2.5 text-[15px] leading-[1.45] text-ink ring-1 ring-line">{children}</div>
  )
}

/** Cả hội thoại: tin nhắn, thẻ cấu hình, ba chấm đang trả lời và hộp báo lỗi */
export function Thread({
  messages,
  busy,
  stage,
  error,
  onRetry,
  cardAction,
  compactCards,
  busyHints,
  optionsView,
}: {
  messages: Msg[]
  busy: boolean
  /** Bước backend báo đang làm; có thì hiện thay cho busyHints */
  stage?: string | null
  error: string | null
  onRetry: () => void
  /** Nút gắn dưới thẻ cấu hình, vd "Xem và lắp máy này" ở trang tư vấn (index là vị trí tin trong hội thoại) */
  cardAction?: (card: BuildCard, index: number) => ReactNode
  /** Thẻ gọn (chỉ kết quả và tổng giá) khi danh sách linh kiện đã có ở bảng chi tiết bên cạnh */
  compactCards?: boolean
  /** Dòng trạng thái lần lượt hiện khi chờ lâu (trang tư vấn phải tìm giá ở nhiều cửa hàng, mất cả phút) */
  busyHints?: string[]
  /** Vẽ thay cho thẻ cấu hình khi tin có nhiều phương án (bảng so sánh ở trang tư vấn); trả null thì dùng thẻ thường */
  optionsView?: (m: Msg, index: number) => ReactNode | null
}) {
  return (
    <>
      {messages.map((m, i) => (
        <div key={i} className="space-y-2">
          <Bubble role={m.role}>{m.role === 'assistant' ? <Rich text={m.content} /> : m.content}</Bubble>
          {(m.options && optionsView?.(m, i)) ??
            (m.build &&
            (compactCards ? (
              <CompactCard card={m.build} action={cardAction?.(m.build, i)} />
            ) : (
              <BuildCardView card={m.build} action={cardAction?.(m.build, i)} />
            )))}
        </div>
      ))}
      {busy && <BusyBubble hints={busyHints} stage={stage} />}
      {error && (
        <div className="rounded-2xl bg-warn-bg px-3.5 py-3 text-[13px] leading-snug text-warn ring-1 ring-warn-line">
          {error}
          <button onClick={onRetry} className="mt-1.5 block font-semibold text-link">
            Thử lại
          </button>
        </div>
      )}
    </>
  )
}

/** Mốc thời gian (giây) đổi sang dòng trạng thái kế tiếp */
const HINT_AT = [0, 6, 20, 50]

/** Ba chấm "đang trả lời", kèm dòng trạng thái đổi theo thời gian chờ nếu có */
function BusyBubble({ hints, stage }: { hints?: string[]; stage?: string | null }) {
  const [step, setStep] = useState(0)
  useEffect(() => {
    if (!hints?.length) return
    const timers = HINT_AT.slice(1, hints.length).map((s, i) => setTimeout(() => setStep(i + 1), s * 1000))
    return () => timers.forEach(clearTimeout)
  }, [hints])
  const hint = stage ?? hints?.[Math.min(step, hints.length - 1)]
  return (
    <div className="flex w-fit items-center gap-2.5 rounded-2xl rounded-bl-md bg-surface px-3.5 py-3 ring-1 ring-line" role="status" aria-label={hint ?? 'Trợ lý đang trả lời'}>
      <span className="flex gap-1" aria-hidden>
        {[0, 1, 2].map((d) => (
          <span key={d} className="typing size-1.5 rounded-full bg-ink-3" style={{ animationDelay: `${d * 160}ms` }} />
        ))}
      </span>
      {hint && (
        <span key={hint} className="swap text-[13px] text-ink-2">
          {hint}
        </span>
      )}
    </div>
  )
}

/** Thẻ gọn trong hội thoại: lắp chung được không, số linh kiện, tổng giá. Chi tiết nằm ở bảng bên cạnh. */
function CompactCard({ card, action }: { card: BuildCard; action?: ReactNode }) {
  const total = totalOf(card)
  return (
    <div className="max-w-[92%] rounded-2xl bg-surface p-3.5 ring-1 ring-line sm:max-w-[420px]">
      <p className={`text-[13px] font-semibold ${card.compatible ? 'text-ok' : 'text-bad'}`}>
        {compatText(card).short}
      </p>
      <p className="mt-0.5 text-[14px] text-ink">
        {card.items.length} linh kiện{total && `, ước tính ${vndRange(total.min, total.max)}`}
      </p>
      {action && <div className="mt-3">{action}</div>}
    </div>
  )
}

/** Thẻ cấu hình: từng linh kiện theo loại, kết quả "lắp chung được không", lỗi và lưu ý */
export function BuildCardView({ card, action }: { card: BuildCard; action?: ReactNode }) {
  return (
    <div className="max-w-[92%] overflow-hidden rounded-2xl bg-surface ring-1 ring-line sm:max-w-[420px]">
      <div className={`flex items-center gap-2 px-3.5 py-2.5 text-[13px] font-semibold ${card.compatible ? 'bg-ok-bg text-ok' : 'bg-bad-bg text-bad'}`}>
        {card.compatible ? <Check size={14} aria-hidden /> : <X size={14} aria-hidden />}
        {compatText(card).short}
      </div>
      <ul className="divide-y divide-line px-3.5">
        {card.items.map((it) => (
          <li key={it.code} className="flex items-baseline justify-between gap-3 py-2 text-[13px]">
            <span className="shrink-0 text-ink-2">{PARTS[it.category as PartId]?.name ?? it.category}</span>
            <span className="text-right font-medium text-ink">
              {it.productName || it.spec || it.name}
              {it.quantity > 1 && <span className="text-ink-2"> × {it.quantity}</span>}
            </span>
          </li>
        ))}
      </ul>
      {totalOf(card) && (
        <div className="flex items-baseline justify-between gap-3 border-t border-line px-3.5 py-2.5 text-[13px]">
          <span className="text-ink-2">Tổng ước tính</span>
          <span className="font-semibold text-ink">{vndRange(totalOf(card)!.min, totalOf(card)!.max)}</span>
        </div>
      )}
      {(card.errors.length > 0 || card.warnings.length > 0) && (
        <ul className="space-y-1.5 border-t border-line px-3.5 py-2.5 text-[12.5px] leading-snug">
          {card.errors.map((e) => (
            <li key={e.rule + e.message} className="text-bad">
              {e.message}
            </li>
          ))}
          {card.warnings.map((w) => (
            <li key={w.rule + w.message} className="text-warn">
              Lưu ý: {w.message}
            </li>
          ))}
        </ul>
      )}
      {action && <div className="border-t border-line p-3">{action}</div>}
    </div>
  )
}

/** Ô nhập câu hỏi: Enter gửi, Shift+Enter xuống dòng */
export function Composer({
  value,
  onChange,
  onSend,
  busy,
  placeholder,
  big,
  inputRef,
  extra,
}: {
  value: string
  onChange: (v: string) => void
  onSend: () => void
  busy: boolean
  placeholder: string
  /** Ô lớn nhiều dòng cho trang tư vấn */
  big?: boolean
  inputRef?: Ref<HTMLTextAreaElement>
  /** Nút thêm đặt ngay trước nút gửi (vd gợi ý ngẫu nhiên) */
  extra?: ReactNode
}) {
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        onSend()
      }}
      className={`flex items-end gap-2 ring-1 ring-line focus-within:ring-accent/50 ${big ? 'rounded-[24px] bg-surface p-2.5 pl-5' : 'rounded-[20px] bg-canvas px-3 py-1.5'}`}
    >
      <textarea
        ref={inputRef}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={(e) => {
          // Bỏ qua lúc đang gõ dấu tiếng Việt (bộ gõ chưa chốt chữ)
          if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
            e.preventDefault()
            onSend()
          }
        }}
        rows={1}
        maxLength={2000}
        placeholder={placeholder}
        aria-label="Câu hỏi"
        className={`field-sizing-content flex-1 resize-none bg-transparent leading-snug outline-none placeholder:text-ink-3 ${
          big ? 'max-h-48 min-h-10 py-[9px] text-[16px]' : 'max-h-28 min-h-[30px] py-1 text-[15px]'
        }`}
      />
      {extra}
      <button
        type="submit"
        disabled={!value.trim() || busy}
        aria-label="Gửi"
        className={`grid shrink-0 place-items-center rounded-full bg-accent text-white transition-[opacity,transform] duration-100 active:scale-[0.94] disabled:opacity-30 ${big ? 'size-10' : 'mb-0.5 size-8'}`}
      >
        <ArrowUp size={15} aria-hidden />
      </button>
    </form>
  )
}

/** Hiện gạch đầu dòng, danh sách đánh số và **chữ đậm** trong câu trả lời. Không dùng innerHTML. */
export function Rich({ text }: { text: string }) {
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

/** Nút tròn chỉ có icon (đóng, cuộc trò chuyện mới...); `icon` là một icon Phosphor */
export function IconButton({ label, onClick, disabled, icon }: { label: string; onClick: () => void; disabled?: boolean; icon: ReactNode }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      title={label}
      className="grid size-8 shrink-0 place-items-center rounded-full bg-fill text-ink-2 transition-transform duration-100 hover:text-ink active:scale-[0.94] disabled:opacity-40"
    >
      {icon}
    </button>
  )
}
