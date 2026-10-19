import { ArrowSquareOut, ImageSquare, MagnifyingGlass } from '@phosphor-icons/react'
import { useState, type ReactNode } from 'react'
import { PARTS, type PartId } from '../../data/parts'
import { totalOf, vnd, vndRange, type BuildCard, type BuildLine } from './chatStore'

const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'

/**
 * Gom linh kiện theo việc chúng làm, để người mới đọc 3 hoặc 4 nhóm ngắn thay vì một danh sách 8 dòng.
 * Loại nào không thuộc nhóm nào thì rơi vào nhóm cuối.
 */
const GROUPS: { title: string; cats: string[] }[] = [
  { title: 'Xử lý và bộ nhớ', cats: ['cpu', 'motherboard', 'ram', 'ssd'] },
  { title: 'Hình ảnh', cats: ['gpu'] },
  { title: 'Làm mát và nguồn điện', cats: ['cooler', 'psu', 'fans'] },
  { title: 'Vỏ máy và phụ kiện', cats: ['case', 'cables', 'tools'] },
]

function groupLines(items: BuildLine[]) {
  const known = new Set(GROUPS.flatMap((g) => g.cats))
  return GROUPS.map((g, i) => ({
    title: g.title,
    lines: items.filter((it) => g.cats.includes(it.category) || (i === GROUPS.length - 1 && !known.has(it.category))),
  })).filter((g) => g.lines.length > 0)
}

/**
 * Bảng thông tin cấu hình sau khi trợ lý chốt: nhu cầu đã hiểu, tổng giá so với ngân sách,
 * rồi linh kiện theo nhóm với ảnh, thông số nên mua, lý do chọn, giá tham khảo và "linh kiện này làm gì".
 */
export function ProposalView({ card, action }: { card: BuildCard; action?: ReactNode }) {
  const total = totalOf(card)
  return (
    <div>
      <h2 className="text-[22px] leading-[1.2] font-semibold tracking-[-0.02em] text-balance text-ink">
        {card.summary || 'Cấu hình trợ lý đã chọn'}
      </h2>
      <p className={`mt-1.5 text-[14px] font-medium ${card.compatible ? 'text-ok' : 'text-bad'}`}>
        {card.compatible ? 'Các linh kiện lắp chung được.' : 'Cấu hình này chưa lắp được, xem lỗi bên dưới.'}
      </p>

      {total && <Total min={total.min} max={total.max} budget={card.budgetVnd ?? null} />}

      <div className="mt-6 space-y-6">
        {groupLines(card.items).map((g) => (
          <section key={g.title}>
            <h3 className="px-1 text-[15px] font-semibold text-ink">{g.title}</h3>
            <ul className="mt-2 space-y-5 rounded-2xl bg-surface p-4">
              {g.lines.map((it) => (
                <PartRow key={it.code} line={it} total={total} />
              ))}
            </ul>
          </section>
        ))}
      </div>

      {(card.errors.length > 0 || card.warnings.length > 0) && (
        <ul className="mt-6 space-y-2 text-[14px] leading-[1.45]">
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

      {action && <div className="mt-6">{action}</div>}
      {total && (
        <p className="mt-3 text-[12px] leading-snug text-ink-3">
          Giá cửa hàng là giá lúc trợ lý tìm, có thể đã đổi. Dòng ghi "ước tính" là trợ lý ước theo thông số phổ thông. Chưa gồm màn hình, bàn phím và chuột.
        </p>
      )}
    </div>
  )
}

/** Tổng giá và một câu so với ngân sách. Không vẽ thanh tiến độ: con số và câu chữ đủ rõ cho người mới. */
function Total({ min, max, budget }: { min: number; max: number; budget: number | null }) {
  let verdict: { text: string; tone: string } | null = null
  if (budget != null) {
    if (max <= budget) verdict = { text: `Nằm trong ngân sách ${vnd(budget)}, còn dư ít nhất ${vnd(budget - max)}.`, tone: 'text-ok' }
    else if (min <= budget) verdict = { text: `Ngân sách ${vnd(budget)}: có thể vượt tới ${vnd(max - budget)} nếu mua ở mức giá cao.`, tone: 'text-warn' }
    else verdict = { text: `Vượt ngân sách ${vnd(budget)} khoảng ${vnd(min - budget)}.`, tone: 'text-bad' }
  }
  return (
    <div className="mt-5">
      <p className="text-[13px] text-ink-2">Tổng ước tính</p>
      <p className="text-[30px] leading-tight font-semibold tracking-[-0.025em] text-ink tabular-nums">{vndRange(min, max)}</p>
      {verdict && <p className={`mt-1 text-[14px] leading-snug ${verdict.tone}`}>{verdict.text}</p>}
    </div>
  )
}

/** Một linh kiện: ảnh, loại, thông số nên mua, giá, lý do chọn, và giải thích linh kiện đó làm gì */
function PartRow({ line, total }: { line: BuildLine; total: { min: number; max: number } | null }) {
  const [open, setOpen] = useState(false)
  const part = PARTS[line.category as PartId]
  const label = `${part?.name ?? line.category}${line.quantity > 1 ? ` × ${line.quantity}` : ''}`
  const lo = line.priceMinVnd ?? line.priceMaxVnd
  const hi = line.priceMaxVnd ?? line.priceMinVnd
  const share = total && lo != null && hi != null ? Math.round(((lo + hi) / (total.min + total.max)) * 100) : null
  return (
    <li className="flex gap-3.5">
      <PartImage line={line} label={label} />
      <div className="min-w-0 flex-1">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <p className="text-[12px] text-ink-2">{label}</p>
            {/* Tên sản phẩm thật nếu đã tìm được; chưa có thì hiện thông số nên tìm mua. Không hiện tên mẫu trong danh mục 3D. */}
            <p className="text-[15px] leading-snug font-semibold text-ink">{line.productName || line.spec || part?.name}</p>
            {line.productName && line.spec && <p className="mt-0.5 text-[12.5px] leading-snug text-ink-3">{line.spec}</p>}
          </div>
          {lo != null && hi != null && (
            <div className="shrink-0 text-right">
              <p className="text-[13px] font-semibold text-ink tabular-nums">{vndRange(lo, hi)}</p>
              <p className="text-[12px] text-ink-3 tabular-nums">
                {line.productName ? 'giá cửa hàng' : 'ước tính'}
                {share != null && `, ${share}%`}
              </p>
            </div>
          )}
        </div>
        {line.reason && <p className="mt-1.5 text-[14px] leading-[1.45] text-ink-2">{line.reason}</p>}
        {line.sourceUrl && (
          <a
            href={line.sourceUrl}
            target="_blank"
            rel="noreferrer noopener"
            className={`mt-2 inline-flex max-w-full items-center gap-1.5 rounded-full bg-fill px-3 py-1.5 text-[13px] font-medium text-ink hover:bg-fill-hover ${focus}`}
          >
            <span className="truncate">Xem ở {line.shop || hostOf(line.sourceUrl)}</span>
            <ArrowSquareOut size={14} aria-hidden className="shrink-0" />
          </a>
        )}
        {/* Trợ lý chưa tìm được sản phẩm thật: vẫn cho một chỗ bắt đầu mua, là trang tìm theo đúng thông số (không phải link sản phẩm) */}
        {!line.sourceUrl && (line.spec || part) && (
          <a
            href={shoppingSearch(line.spec || part!.name)}
            target="_blank"
            rel="noreferrer noopener"
            className={`mt-2 inline-flex max-w-full items-center gap-1.5 rounded-full bg-fill px-3 py-1.5 text-[13px] font-medium text-ink hover:bg-fill-hover ${focus}`}
          >
            <MagnifyingGlass size={14} aria-hidden className="shrink-0" />
            <span className="truncate">Tìm mua theo thông số này</span>
            <ArrowSquareOut size={14} aria-hidden className="shrink-0" />
          </a>
        )}
        {part && (
          <>
            <button
              onClick={() => setOpen(!open)}
              aria-expanded={open}
              className={`mt-1.5 text-[13px] font-medium text-link hover:underline ${focus}`}
            >
              {open ? 'Ẩn giải thích' : 'Linh kiện này làm gì?'}
            </button>
            <div className="reveal" data-open={open}>
              <div>
                <p className="pt-1 text-[13px] leading-[1.45] text-ink-2">{part.about}</p>
              </div>
            </div>
          </>
        )}
      </div>
    </li>
  )
}

/**
 * Ảnh sản phẩm trợ lý tìm được trên web. Chưa có ảnh, hoặc ảnh không tải được, thì hiện khung "Ảnh mẫu" giữ chỗ.
 * Link mua nằm dưới phần chữ (PartRow).
 */
function PartImage({ line, label }: { line: BuildLine; label: string }) {
  const [failed, setFailed] = useState(false)
  const src = line.imageUrl && !failed ? line.imageUrl : null
  return (
    <div className="w-[72px] shrink-0">
      {src ? (
        <img
          src={src}
          alt={line.spec || line.name}
          loading="lazy"
          referrerPolicy="no-referrer"
          onError={() => setFailed(true)}
          className="aspect-square w-full rounded-xl bg-canvas object-contain"
        />
      ) : (
        <div
          role="img"
          aria-label={`Ảnh mẫu: ${label}`}
          className="grid aspect-square w-full place-items-center rounded-xl border border-dashed border-line-strong bg-canvas text-ink-3"
        >
          <div className="flex flex-col items-center gap-0.5">
            <ImageSquare size={22} aria-hidden />
            <span className="text-[11px] font-medium">Ảnh mẫu</span>
          </div>
        </div>
      )}
    </div>
  )
}

/** Trang Google Shopping (khu vực Việt Nam) tìm sẵn theo thông số, cho dòng chưa có sản phẩm thật */
function shoppingSearch(query: string) {
  return `https://www.google.com/search?tbm=shop&hl=vi&gl=vn&q=${encodeURIComponent(query)}`
}

function hostOf(url: string) {
  try {
    return new URL(url).hostname.replace(/^www\./, '')
  } catch {
    return 'trang web'
  }
}
