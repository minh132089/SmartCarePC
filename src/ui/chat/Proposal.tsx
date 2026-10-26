import { ArrowSquareOut, ImageSquare } from '@phosphor-icons/react'
import { useState, type ReactNode } from 'react'
import { PARTS, type PartId } from '../../data/parts'
import { compatText, quantityLabel, totalOf, vnd, vndRange, type BuildCard, type BuildLine, type Issue, type Verdict } from './chatStore'
import { VERDICT_INFO } from './verdict'
import { conditionSummary } from './conditionSummary'

const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'

/**
 * Gom linh kiện theo việc chúng làm, để người mới đọc 3 hoặc 4 nhóm ngắn thay vì một danh sách 8 dòng.
 * Loại nào không thuộc nhóm nào thì rơi vào nhóm cuối.
 */
const GROUPS: { title: string; cats: string[] }[] = [
  { title: "Processing and memory", cats: ['cpu', 'motherboard', 'ram', 'ssd'] },
  { title: "Graphics", cats: ['gpu'] },
  { title: "Cooling and power", cats: ['cooler', 'psu', 'fans'] },
  { title: "Case and accessories", cats: ['case', 'cables', 'tools'] },
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
      <h2 data-sm-item className="text-[22px] leading-[1.2] font-semibold tracking-[-0.02em] text-balance text-ink">
        {card.summary || "Recommended build"}
      </h2>
      <p data-sm-item className={`mt-1.5 text-[14px] font-medium ${card.compatible ? 'text-ok' : 'text-bad'}`}>{compatText(card).long}</p>

      {card.verdict && <VerdictBox verdict={card.verdict} issues={[...card.warnings, ...(card.unknowns ?? [])]} />}

      {total && <Total min={total.min} max={total.max} budget={card.budgetVnd ?? null} />}

      <div className="mt-6 space-y-6">
        {groupLines(card.items).map((g) => (
          <section key={g.title} data-sm-item>
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
              Note: {w.message}
            </li>
          ))}
        </ul>
      )}
      {!card.verdict && <IssueGroup title="Confirm before buying" issues={card.unknowns} />}
      <IssueGroup title="Recommended" issues={card.recommendations} />

      {action && (
        <div data-sm-item className="mt-6">
          {action}
        </div>
      )}
      {total && (
        <p className="mt-3 text-[12px] leading-snug text-ink-3">
          Store prices were checked during the search and may have changed. Unverified prices are references from Google; open the store link to confirm. Estimated prices are based on general specifications. Monitor, keyboard and mouse are not included.
        </p>
      )}
    </div>
  )
}

/** Tổng giá và một câu so với ngân sách. Không vẽ thanh tiến độ: con số và câu chữ đủ rõ cho người mới. */
function Total({ min, max, budget }: { min: number; max: number; budget: number | null }) {
  let verdict: { text: string; tone: string } | null = null
  if (budget != null) {
    if (max <= budget) verdict = { text: `Within your budget of ${vnd(budget)}, with at least ${vnd(budget - max)}.`, tone: 'text-ok' }
    else if (min <= budget) verdict = { text: `Budget ${vnd(budget)}: may exceed it by up to ${vnd(max - budget)} at the upper price estimate.`, tone: 'text-warn' }
    else verdict = { text: `Exceeds your budget of ${vnd(budget)} by approximately ${vnd(min - budget)}.`, tone: 'text-bad' }
  }
  return (
    <div className="mt-5">
      <p className="text-[13px] text-ink-2">Estimated total</p>
      <p className="text-[30px] leading-tight font-semibold tracking-[-0.025em] text-ink tabular-nums">{vndRange(min, max)}</p>
      {verdict && <p className={`mt-1 text-[14px] leading-snug ${verdict.tone}`}>{verdict.text}</p>}
    </div>
  )
}

/** Một linh kiện: ảnh, loại, thông số nên mua, giá, lý do chọn, và giải thích linh kiện đó làm gì */
function PartRow({ line, total }: { line: BuildLine; total: { min: number; max: number } | null }) {
  const [open, setOpen] = useState(false)
  const part = PARTS[line.category as PartId]
  const label = `${part?.name ?? line.category}${quantityLabel(line)}`
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
                {!line.productName ? "estimated" : line.priceChecked === false ? "reference price, not verified on the product page" : "store website price"}
                {share != null && `, ${share}%`}
              </p>
            </div>
          )}
        </div>
        {line.reason && <p className="mt-1.5 text-[14px] leading-[1.45] text-ink-2">{line.reason}</p>}
        <OfferFacts line={line} />
        {line.sourceUrl && (
          <a
            href={line.sourceUrl}
            target="_blank"
            rel="noreferrer noopener"
            className={`mt-2 inline-flex max-w-full items-center gap-1.5 rounded-full bg-fill px-3 py-1.5 text-[13px] font-medium text-ink hover:bg-fill-hover ${focus}`}
          >
            <span className="truncate">View at {line.shop || hostOf(line.sourceUrl)}</span>
            <ArrowSquareOut size={14} aria-hidden className="shrink-0" />
          </a>
        )}
        {/* App tự tìm sản phẩm, không bắt người dùng tự tìm: không có trang bán còn hàng thì nói rõ, không đưa trang tìm kiếm */}
        {!line.sourceUrl && <p className="mt-2 text-[13px] text-ink-3">No in-stock product page has been found for this component.</p>}
        {part && (
          <>
            <button
              onClick={() => setOpen(!open)}
              aria-expanded={open}
              className={`mt-1.5 text-[13px] font-medium text-link hover:underline ${focus}`}
            >
              {open ? "Hide explanation" : "What does this component do?"}
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
 * Ảnh từ trang sản phẩm. Chưa có ảnh, hoặc ảnh không tải được, thì ghi rõ chưa có ảnh.
 * Link mua nằm dưới phần chữ (PartRow).
 */
function PartImage({ line, label }: { line: BuildLine; label: string }) {
  const [failedSrc, setFailedSrc] = useState<string | null>(null)
  const src = line.imageUrl && line.imageUrl !== failedSrc ? line.imageUrl : null
  return (
    <div className="w-[72px] shrink-0">
      {src ? (
        <img
          src={src}
          alt={line.productName || line.name}
          loading="lazy"
          referrerPolicy="no-referrer"
          onError={() => setFailedSrc(src)}
          className="aspect-square w-full rounded-xl bg-canvas object-contain"
        />
      ) : (
        <div
          role="img"
          aria-label={`Image unavailable: ${label}`}
          className="grid aspect-square w-full place-items-center rounded-xl border border-dashed border-line-strong bg-canvas text-ink-3"
        >
          <div className="flex flex-col items-center gap-0.5">
            <ImageSquare size={22} aria-hidden />
            <span className="text-[11px] font-medium">Image unavailable</span>
          </div>
        </div>
      )}
    </div>
  )
}

/** Trang tìm kiếm trên web cửa hàng (GEARVN) theo thông số, cho dòng hiếm hoi chưa có sản phẩm thật; không qua Google */
/** Một nhóm ghi chú theo mức của bộ luật (Cần xác minh, Gợi ý); không có thì không hiện */
/** Trạng thái phương án (backend tính theo docs/pc-build-verifier): điểm đang chặn, rồi việc cần làm trước khi mua */
function VerdictBox({ verdict, issues }: { verdict: Verdict; issues: Issue[] }) {
  const info = VERDICT_INFO[verdict.status]
  const conditions = verdict.conditions.map((message) => ({
    message,
    summary: conditionSummary(message, issues.find((i) => message.includes(i.message))?.rule),
  }))
  const shortened = conditions.some((c) => c.summary !== c.message)
  const vietnamese = verdict.conditions.some((c) => /[ĂăÂâĐđÊêÔôƠơƯư\u1ea0-\u1ef9]/u.test(c))
  return (
    <div data-sm-item className="mt-4 rounded-xl bg-fill px-3.5 py-3">
      <p className="flex items-center gap-2 text-[13px] font-semibold text-ink-2">
        Status
        <span className={`rounded-full px-2 py-0.5 text-[11.5px] font-medium ${info.tone}`}>{info.label}</span>
      </p>
      {verdict.blockers.length > 0 && (
        <ul className="mt-1.5 list-disc space-y-1 pl-4 text-[13.5px] leading-[1.45] text-bad">
          {verdict.blockers.map((b) => (
            <li key={b}>{b}</li>
          ))}
        </ul>
      )}
      {verdict.conditions.length > 0 && (
        <>
          <p className="mt-2 text-[12.5px] font-medium text-ink-2">Before you buy</p>
          <ul className="mt-1 list-disc space-y-1 pl-4 text-[13.5px] leading-[1.45] text-ink-2">
            {conditions.map((c) => (
              <li key={c.message}>{c.summary}</li>
            ))}
          </ul>
          {shortened && (
            <details className="mt-2 text-[12.5px] leading-[1.45] text-ink-2">
              <summary className={`w-fit cursor-pointer rounded-sm font-medium text-link ${focus}`}>
                {vietnamese ? 'View details' : 'View details'}
              </summary>
              <ul className="mt-2 list-disc space-y-2 pl-4">
                {conditions.map((c) => <li key={c.message}>{c.message}</li>)}
              </ul>
            </details>
          )}
        </>
      )}
    </div>
  )
}

function IssueGroup({ title, issues }: { title: string; issues?: Issue[] | null }) {
  if (!issues?.length) return null
  return (
    <div className="mt-4 rounded-xl bg-fill px-3.5 py-3">
      <p className="text-[13px] font-semibold text-ink-2">{title}</p>
      <ul className="mt-1.5 list-disc space-y-1 pl-4 text-[13.5px] leading-[1.45] text-ink-2">
        {issues.map((i) => (
          <li key={i.rule + i.message}>{i.message}</li>
        ))}
      </ul>
    </div>
  )
}

/** Còn hàng / bảo hành / giờ kiểm tra giá của món đã chọn (engine so giá đọc từ trang cửa hàng) */
function OfferFacts({ line }: { line: BuildLine }) {
  const facts: ReactNode[] = []
  if (line.inStock === true) facts.push(<span key="s" className="text-ok">In stock</span>)
  if (line.inStock === false) facts.push(<span key="s" className="text-bad">Reported out of stock</span>)
  if (line.warrantyMonths) facts.push(<span key="w">Warranty {line.warrantyMonths % 12 === 0 ? `${line.warrantyMonths / 12} years` : `${line.warrantyMonths} months`}</span>)
  const at = checkedText(line.checkedAt)
  if (at) facts.push(<span key="t">Price checked {at}</span>)
  const others = line.compare ?? []
  if (!facts.length && !others.length) return null
  return (
    <div className="mt-2 space-y-1.5">
      {facts.length > 0 && (
        <p className="flex flex-wrap gap-x-2 text-[12.5px] text-ink-3">
          {facts.map((f, i) => (
            <span key={i} className="flex gap-x-2">
              {i > 0 && <span aria-hidden>·</span>}
              {f}
            </span>
          ))}
        </p>
      )}
      {others.length > 0 && (
        <div className="rounded-xl bg-fill px-3 py-2">
          <p className="text-[12px] font-medium text-ink-2">Same product at other stores{line.quantity > 1 ? " (per item)" : ''}</p>
          <ul className="mt-1 space-y-0.5">
            {others.map((o) => (
              <li key={o.url} className="flex items-center justify-between gap-3 text-[13px]">
                <a href={o.url} target="_blank" rel="noreferrer noopener" className={`min-w-0 truncate text-link hover:underline ${focus}`}>
                  {o.shop}
                  {o.inStock === false && <span className="text-ink-3"> (out of stock)</span>}
                </a>
                <span className="shrink-0 tabular-nums text-ink">{vnd(o.priceVnd)}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

/** "lúc 17:40" nếu trong hôm nay, "lúc 17:40 ngày 19/10" nếu cũ hơn */
function checkedText(iso?: string | null) {
  if (!iso) return null
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return null
  const time = d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })
  return d.toDateString() === new Date().toDateString() ? `at ${time}` : `at ${time} on ${d.getDate()}/${d.getMonth() + 1}`
}


function hostOf(url: string) {
  try {
    return new URL(url).hostname.replace(/^www\./, '')
  } catch {
    return 'trang web'
  }
}
