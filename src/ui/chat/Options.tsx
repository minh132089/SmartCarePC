import { Check } from '@phosphor-icons/react'
import { totalOf, vnd, vndRange, type BuildCard, type Tier } from './chatStore'
import { TIER_INFO } from './tiers'

const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'
const press = 'transition-transform duration-100 ease-out active:scale-[0.98] motion-reduce:active:scale-100'

const ORDER: Tier[] = ['cheap', 'balanced', 'quality']

/** Các món đáng so giữa ba phương án; vỏ, nguồn, quạt thường giống nhau nên không đưa vào */
const KEY_PARTS: [string, string][] = [
  ['cpu', 'CPU'],
  ['gpu', 'Card'],
  ['ram', 'RAM'],
  ['ssd', 'SSD'],
]

/**
 * Bảng so sánh ba phương án ngay dưới câu trả lời: tổng giá so với ngân sách và các món chính của từng phương án.
 * Bấm một phương án thì bảng chi tiết bên cạnh chuyển sang phương án đó (nút "Xem và lắp máy này" nằm ở đó).
 */
export function OptionsCanvas({
  options,
  selected,
  onSelect,
}: {
  options: BuildCard[]
  selected: Tier
  onSelect: (tier: Tier) => void
}) {
  const list = ORDER.map((t) => options.find((o) => o.tier === t)).filter(Boolean) as BuildCard[]
  return (
    <div role="radiogroup" aria-label="Ba phương án cấu hình" className="grid gap-2.5 sm:grid-cols-3">
      {list.map((o) => (
        <OptionCard key={o.tier} option={o} on={o.tier === selected} onSelect={() => onSelect(o.tier as Tier)} />
      ))}
    </div>
  )
}

function OptionCard({ option, on, onSelect }: { option: BuildCard; on: boolean; onSelect: () => void }) {
  const tier = option.tier as Tier
  const info = TIER_INFO[tier]
  const total = totalOf(option)
  const budget = option.budgetVnd ?? null
  // So với ngân sách: lấy giá cao của khoảng (an toàn cho người mua)
  const diff = total && budget ? budget - total.max : null
  return (
    <button
      role="radio"
      aria-checked={on}
      onClick={onSelect}
      className={`flex flex-col rounded-2xl p-3.5 text-left ${press} ${focus} ${
        on ? 'bg-surface ring-2 ring-accent' : 'bg-surface ring-1 ring-line hover:ring-line-strong'
      }`}
    >
      <span className="flex items-center justify-between gap-2">
        <span className="text-[14px] font-semibold text-ink">{info.label}</span>
        {tier === 'balanced' && (
          <span className="rounded-full bg-accent px-2 py-0.5 text-[11px] font-medium text-white">Gợi ý</span>
        )}
      </span>
      <span className="mt-0.5 text-[12px] leading-snug text-ink-2">{info.note}</span>
      {total && <span className="mt-2.5 text-[17px] font-semibold text-ink tabular-nums">{vndRange(total.min, total.max)}</span>}
      {diff != null && (
        <span className={`text-[12px] tabular-nums ${diff >= 0 ? 'text-ok' : 'text-warn'}`}>
          {diff >= 0 ? `Dưới ngân sách ${vnd(diff)}` : `Vượt ngân sách ${vnd(-diff)}`}
        </span>
      )}
      <span className="mt-2.5 space-y-1 border-t border-line pt-2.5">
        {KEY_PARTS.map(([cat, label]) => {
          const line = option.items.find((i) => i.category === cat)
          if (!line) return null
          return (
            <span key={cat} className="flex gap-1.5 text-[12px] leading-snug">
              <span className="w-[34px] shrink-0 text-ink-3">{label}</span>
              <span className="line-clamp-2 text-ink">{line.productName || line.spec || line.name}</span>
            </span>
          )
        })}
      </span>
      <span className={`mt-3 flex items-center justify-center gap-1 rounded-full px-3 py-1.5 text-[12.5px] font-medium ${on ? 'bg-accent text-white' : 'bg-fill text-ink'}`}>
        {on && <Check size={12} weight="bold" aria-hidden />}
        {on ? 'Đang xem' : 'Xem phương án này'}
      </span>
    </button>
  )
}
