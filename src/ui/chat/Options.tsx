import { Check } from '@phosphor-icons/react'
import { totalOf, vnd, vndRange, type BuildCard, type Pick, type Tier } from './chatStore'
import { TIER_INFO } from './tiers'

const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'
const press = 'transition-transform duration-100 ease-out active:scale-[0.98] motion-reduce:active:scale-100'

const ORDER: Tier[] = ['cheap', 'balanced', 'quality']

/** Các món đáng so giữa các option; vỏ, nguồn, quạt thường giống nhau nên không đưa vào */
const KEY_PARTS: [string, string][] = [
  ['cpu', 'CPU'],
  ['gpu', 'Card'],
  ['ram', 'RAM'],
  ['ssd', 'SSD'],
]

/**
 * Bảng so sánh ngay dưới câu trả lời: ba phương án (Rẻ, Phù hợp cho bạn, Chất lượng), mỗi phương án tới 3 option để
 * tham khảo (option 1 trợ lý chọn; option 2, 3 cùng linh kiện, sản phẩm khác giá gần). Bấm một option thì bảng chi tiết
 * bên cạnh chuyển sang option đó (nút "Xem và lắp máy này" nằm ở đó).
 */
export function OptionsCanvas({
  options,
  selected,
  onSelect,
}: {
  options: BuildCard[]
  selected: Pick
  onSelect: (pick: Pick) => void
}) {
  const tiers = ORDER.filter((t) => options.some((o) => o.tier === t))
  return (
    <div className="grid gap-2.5 sm:grid-cols-3">
      {tiers.map((t) => (
        <TierColumn
          key={t}
          tier={t}
          builds={options.filter((o) => o.tier === t).sort((a, b) => (a.variant ?? 1) - (b.variant ?? 1))}
          selected={selected.tier === t ? selected.variant : null}
          onSelect={(variant) => onSelect({ tier: t, variant })}
        />
      ))}
    </div>
  )
}

function TierColumn({
  tier,
  builds,
  selected,
  onSelect,
}: {
  tier: Tier
  builds: BuildCard[]
  selected: number | null
  onSelect: (variant: number) => void
}) {
  const info = TIER_INFO[tier]
  // Món chính hiện theo option đang chọn trong cột này (chưa chọn thì option 1)
  const shown = builds.find((b) => (b.variant ?? 1) === (selected ?? 1)) ?? builds[0]
  return (
    <div className={`flex flex-col rounded-2xl bg-surface p-3.5 ${selected != null ? 'ring-2 ring-accent' : 'ring-1 ring-line'}`}>
      <div className="flex items-center justify-between gap-2">
        <span className="text-[14px] font-semibold text-ink">{info.label}</span>
        {tier === 'balanced' && <span className="rounded-full bg-accent px-2 py-0.5 text-[11px] font-medium text-white">Gợi ý</span>}
      </div>
      <p className="mt-0.5 text-[12px] leading-snug text-ink-2">{info.note}</p>

      <div role="radiogroup" aria-label={`Các option của phương án ${info.label}`} className="mt-2.5 space-y-1.5">
        {builds.map((b) => {
          const v = b.variant ?? 1
          const on = selected === v
          const total = totalOf(b)
          const diff = total && b.budgetVnd ? b.budgetVnd - total.max : null
          return (
            <button
              key={v}
              role="radio"
              aria-checked={on}
              onClick={() => onSelect(v)}
              className={`flex w-full items-center justify-between gap-2 rounded-xl px-2.5 py-2 text-left ${press} ${focus} ${
                on ? 'bg-accent text-white' : 'bg-fill text-ink hover:bg-fill-hover'
              }`}
            >
              {/* Cột hẹp: "trợ lý chọn" xuống dòng dưới, "Option 1" và giá luôn giữ một dòng */}
              <span className="flex min-w-0 flex-wrap items-center gap-x-1.5 text-[12.5px] font-medium">
                <span className="flex items-center gap-1.5 whitespace-nowrap">
                  {on && <Check size={12} weight="bold" aria-hidden />}
                  Option {v}
                </span>
                {v === 1 && <span className={`text-[11px] font-normal whitespace-nowrap ${on ? 'text-white/80' : 'text-ink-3'}`}>trợ lý chọn</span>}
              </span>
              <span className="text-right leading-tight">
                {total && <span className="block whitespace-nowrap text-[13px] font-semibold tabular-nums">{vndRange(total.min, total.max)}</span>}
                {diff != null && (
                  <span className={`block whitespace-nowrap text-[11px] tabular-nums ${on ? 'text-white/85' : diff >= 0 ? 'text-ok' : 'text-warn'}`}>
                    {diff >= 0 ? `dư ${vnd(diff)}` : `vượt ${vnd(-diff)}`}
                  </span>
                )}
              </span>
            </button>
          )
        })}
      </div>

      <div className="mt-2.5 space-y-1 border-t border-line pt-2.5">
        <p className="text-[11px] text-ink-3">Option {shown.variant ?? 1} gồm</p>
        {KEY_PARTS.map(([cat, label]) => {
          const line = shown.items.find((i) => i.category === cat)
          if (!line) return null
          return (
            <p key={cat} className="flex gap-1.5 text-[12px] leading-snug">
              <span className="w-[34px] shrink-0 text-ink-3">{label}</span>
              <span className="line-clamp-2 text-ink">{line.productName || line.spec || line.name}</span>
            </p>
          )
        })}
      </div>
    </div>
  )
}
