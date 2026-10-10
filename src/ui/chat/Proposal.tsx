import { useState, type ReactNode } from 'react'
import { PARTS, type PartId } from '../../data/parts'
import { totalOf, vnd, vndRange, type BuildCard, type BuildLine } from './chatStore'

const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'

/**
 * Bảng thông tin cấu hình sau khi trợ lý chốt: nhu cầu đã hiểu, tổng giá so với ngân sách,
 * rồi từng linh kiện với thông số nên mua, lý do chọn, giá tham khảo và "linh kiện này làm gì".
 */
export function ProposalView({ card, action }: { card: BuildCard; action?: ReactNode }) {
  const total = totalOf(card)
  return (
    <div className="space-y-4">
      <div>
        <p className="text-[13px] font-semibold text-accent">Cấu hình đề xuất cho bạn</p>
        <h2 className="mt-1 text-[22px] leading-tight font-semibold tracking-[-0.02em] text-ink">
          {card.summary || 'Các linh kiện trợ lý đã chọn'}
        </h2>
        <p className={`mt-2 inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[12.5px] font-semibold ${card.compatible ? 'bg-[#eaf7ee] text-[#1d7a3a]' : 'bg-[#fdecec] text-[#b42318]'}`}>
          <svg width="12" height="12" viewBox="0 0 14 14" aria-hidden>
            {card.compatible ? (
              <path d="M3 7.3 5.8 10 11 4" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            ) : (
              <path d="M4 4l6 6M10 4l-6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            )}
          </svg>
          {card.compatible ? 'Các linh kiện lắp chung được' : 'Cấu hình này chưa lắp được'}
        </p>
      </div>

      {total && <Budget min={total.min} max={total.max} budget={card.budgetVnd ?? null} />}

      <ul className="space-y-2">
        {card.items.map((it) => (
          <PartRow key={it.code} line={it} total={total} />
        ))}
      </ul>

      {(card.errors.length > 0 || card.warnings.length > 0) && (
        <ul className="space-y-1.5 rounded-2xl bg-white p-4 text-[13px] leading-snug">
          {card.errors.map((e) => (
            <li key={e.rule + e.message} className="text-[#b42318]">
              {e.message}
            </li>
          ))}
          {card.warnings.map((w) => (
            <li key={w.rule + w.message} className="text-[#7a4b00]">
              Lưu ý: {w.message}
            </li>
          ))}
        </ul>
      )}

      {action}
      {total && (
        <p className="text-[12px] leading-snug text-ink-3">
          Giá do AI ước tính theo thông số phổ thông, chưa gồm màn hình, bàn phím và chuột. Giá thật thay đổi theo cửa hàng và thời điểm, hãy hỏi lại trước khi mua.
        </p>
      )}
    </div>
  )
}

/** Tổng giá so với ngân sách: thanh ngang có khoảng giá ước tính và vạch ngân sách */
function Budget({ min, max, budget }: { min: number; max: number; budget: number | null }) {
  const scale = Math.max(max, budget ?? 0) * 1.08
  const pct = (n: number) => `${Math.min(100, (n / scale) * 100)}%`
  const status =
    budget == null
      ? null
      : max <= budget
        ? { text: `Nằm trong ngân sách ${vnd(budget)}`, tone: 'text-[#1d7a3a]' }
        : min <= budget
          ? { text: `Có thể vượt ngân sách ${vnd(budget)} một chút`, tone: 'text-[#7a4b00]' }
          : { text: `Vượt ngân sách khoảng ${vnd(min - budget)}`, tone: 'text-[#b42318]' }
  return (
    <div className="rounded-2xl bg-white p-4">
      <p className="text-[12px] font-semibold text-ink-2">Tổng ước tính</p>
      <p className="mt-0.5 text-[24px] leading-tight font-semibold tracking-[-0.02em] text-ink">{vndRange(min, max)}</p>
      {budget != null && (
        <>
          <div className="relative mt-3 h-2.5 rounded-full bg-fill" aria-hidden>
            <div className="absolute inset-y-0 left-0 rounded-full bg-accent/25" style={{ width: pct(max) }} />
            <div className="absolute inset-y-0 left-0 rounded-full bg-accent" style={{ width: pct(min) }} />
            <div className="absolute -top-1 -bottom-1 w-0.5 rounded-full bg-ink" style={{ left: pct(budget) }} />
          </div>
          <div className="mt-1.5 flex items-baseline justify-between gap-3 text-[12px]">
            <span className={`font-semibold ${status!.tone}`}>{status!.text}</span>
            <span className="text-ink-3">Vạch đen: ngân sách</span>
          </div>
        </>
      )}
    </div>
  )
}

/** Một linh kiện: loại, thông số nên mua, giá, lý do chọn, và giải thích linh kiện đó làm gì */
function PartRow({ line, total }: { line: BuildLine; total: { min: number; max: number } | null }) {
  const [open, setOpen] = useState(false)
  const part = PARTS[line.category as PartId]
  const price = line.priceMinVnd != null || line.priceMaxVnd != null ? vndRange(line.priceMinVnd ?? line.priceMaxVnd!, line.priceMaxVnd ?? line.priceMinVnd!) : null
  const mid = ((line.priceMinVnd ?? 0) + (line.priceMaxVnd ?? line.priceMinVnd ?? 0)) / 2
  const share = total && mid > 0 ? Math.round((mid / ((total.min + total.max) / 2)) * 100) : null
  return (
    <li className="flex gap-3.5 rounded-2xl bg-white p-4">
      <PartImage line={line} label={part?.name ?? line.category} />
      <div className="min-w-0 flex-1">
        <div className="flex items-baseline justify-between gap-3">
          <span className="text-[12px] font-semibold text-ink-2">
            {part?.name ?? line.category}
            {line.quantity > 1 && ` × ${line.quantity}`}
          </span>
          {price && (
            <span className="shrink-0 text-right text-[13px] font-semibold text-ink">
              {price}
              {share != null && <span className="ml-1 font-normal text-ink-3">· {share}%</span>}
            </span>
          )}
        </div>
        <p className="mt-1 text-[15px] leading-snug font-semibold text-ink">{line.spec || line.name}</p>
        {line.spec && <p className="text-[12.5px] text-ink-3">Loại trong mô hình 3D: {line.name}</p>}
        {line.reason && (
          <p className="mt-2 text-[14px] leading-[1.45] text-ink">
            <span className="text-ink-2">Vì sao chọn: </span>
            {line.reason}
          </p>
        )}
        {part && (
          <>
            <button
              onClick={() => setOpen(!open)}
              aria-expanded={open}
              className={`mt-2 text-[13px] font-medium text-accent hover:underline ${focus}`}
            >
              {open ? 'Ẩn giải thích' : 'Linh kiện này làm gì?'}
            </button>
            <div className="reveal" data-open={open}>
              <div>
                <p className="pt-1.5 text-[13px] leading-[1.45] text-ink-2">{part.about}</p>
              </div>
            </div>
          </>
        )}
      </div>
    </li>
  )
}

/**
 * Ảnh linh kiện. Trợ lý tìm ảnh sản phẩm trên web rồi gửi link ảnh và link trang nguồn; chưa có link,
 * hoặc ảnh không tải được, thì hiện khung "Ảnh mẫu" giữ chỗ.
 */
function PartImage({ line, label }: { line: BuildLine; label: string }) {
  const [failed, setFailed] = useState(false)
  const src = line.imageUrl && !failed ? line.imageUrl : null
  return (
    <div className="w-[88px] shrink-0">
      {src ? (
        <img
          src={src}
          alt={line.spec || line.name}
          loading="lazy"
          referrerPolicy="no-referrer"
          onError={() => setFailed(true)}
          className="aspect-square w-full rounded-xl bg-[#f5f5f7] object-contain"
        />
      ) : (
        <div
          role="img"
          aria-label={`Ảnh mẫu: ${label}`}
          className="grid aspect-square w-full place-items-center rounded-xl border border-dashed border-black/15 bg-[#f5f5f7] text-ink-3"
        >
          <div className="flex flex-col items-center gap-1">
            <svg width="26" height="26" viewBox="0 0 24 24" aria-hidden>
              <rect x="3" y="4.5" width="18" height="15" rx="2.5" fill="none" stroke="currentColor" strokeWidth="1.5" />
              <circle cx="9" cy="10" r="1.8" fill="none" stroke="currentColor" strokeWidth="1.5" />
              <path d="m4 17.5 5-4.5 3.5 3 3-2.5L20 17" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
            </svg>
            <span className="text-[11px] font-medium">Ảnh mẫu</span>
          </div>
        </div>
      )}
      {src && line.sourceUrl && (
        <a
          href={line.sourceUrl}
          target="_blank"
          rel="noreferrer noopener"
          title={`Mở trang nguồn của ảnh: ${hostOf(line.sourceUrl)}`}
          className={`mt-1 block truncate text-center text-[11px] text-ink-3 hover:text-accent hover:underline ${focus}`}
        >
          {hostOf(line.sourceUrl)}
        </a>
      )}
    </div>
  )
}

function hostOf(url: string) {
  try {
    return new URL(url).hostname.replace(/^www\./, '')
  } catch {
    return 'trang web'
  }
}
