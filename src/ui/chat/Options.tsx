import type { CSSProperties } from 'react'
import { Check } from '@phosphor-icons/react'
import SpotlightCard from '@/components/SpotlightCard'
import { useDark } from '../../theme'
import { totalOf, vnd, vndRange, type BuildCard, type Pick, type Tier } from './chatStore'
import { TIER_INFO } from './tiers'

const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'
const press = 'transition-transform duration-100 ease-out active:scale-[0.98] motion-reduce:active:scale-100'

const ORDER: Tier[] = ['cheap', 'balanced', 'quality']

/** Các món đáng so giữa các phương án; vỏ, nguồn, quạt thường giống nhau nên không đưa vào */
const KEY_PARTS: [string, string][] = [
  ['cpu', 'CPU'],
  ['gpu', 'Card'],
  ['ram', 'RAM'],
  ['ssd', 'SSD'],
]

/**
 * Bảng so sánh ngay dưới câu trả lời: ba phương án (Rẻ, Phù hợp cho bạn, Chất lượng), mỗi phương án một build, mỗi
 * thẻ có vệt sáng theo con trỏ (React Bits SpotlightCard). Bấm một phương án thì ngăn chi tiết linh kiện mở ra
 * (nút "Xem và lắp máy này" nằm ở đó).
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
    <div role="radiogroup" aria-label="Ba phương án" className="grid gap-2.5 sm:grid-cols-3">
      {tiers.map((t) => (
        <TierColumn
          key={t}
          tier={t}
          build={options.find((o) => o.tier === t)!}
          selected={selected.tier === t}
          onSelect={() => onSelect({ tier: t, variant: 1 })}
        />
      ))}
    </div>
  )
}

function TierColumn({ tier, build, selected, onSelect }: { tier: Tier; build: BuildCard; selected: boolean; onSelect: () => void }) {
  const info = TIER_INFO[tier]
  const total = totalOf(build)
  const diff = total && build.budgetVnd ? build.budgetVnd - total.max : null
  const dark = useDark()
  return (
    <SpotlightCard
      theme={dark ? 'dark' : 'light'}
      spotlightColor={dark ? '#2997ff' : '#0071e3'}
      intensity={dark ? 0.18 : 0.1}
      spotlightSize={220}
      className={`!rounded-2xl !p-0 ${press} ${selected ? 'ring-2 ring-accent' : ''}`}
      style={
        {
          '--spotlight-card-surface': 'var(--color-surface)',
          '--spotlight-card-shadow': 'none',
        } as CSSProperties
      }
    >
      <button
        role="radio"
        aria-checked={selected}
        onClick={onSelect}
        data-sm-opener
        className={`relative z-[2] flex h-full w-full flex-col rounded-2xl p-3.5 text-left ${focus}`}
      >
        <span className="flex w-full items-center justify-between gap-2">
          <span className="flex items-center gap-1.5 text-[14px] font-semibold text-ink">
            {selected && <Check size={13} weight="bold" className="text-accent" aria-hidden />}
            {info.label}
          </span>
          {tier === 'balanced' && <span className="rounded-full bg-accent px-2 py-0.5 text-[11px] font-medium text-white">Gợi ý</span>}
        </span>
        <span className="mt-0.5 text-[12px] leading-snug text-ink-2">{info.note}</span>

        {total && (
          <span className="mt-2.5 block">
            <span className="block whitespace-nowrap text-[17px] font-semibold text-ink tabular-nums">
              {vndRange(total.min, total.max)}
            </span>
            {diff != null && (
              <span className={`block whitespace-nowrap text-[12px] tabular-nums ${diff >= 0 ? 'text-ok' : 'text-warn'}`}>
                {diff >= 0 ? `dư ${vnd(diff)}` : `vượt ${vnd(-diff)}`}
              </span>
            )}
          </span>
        )}

        <span className="mt-2.5 block w-full space-y-1 border-t border-line pt-2.5">
          {KEY_PARTS.map(([cat, label]) => {
            const line = build.items.find((i) => i.category === cat)
            if (!line) return null
            return (
              <span key={cat} className="flex gap-1.5 text-[12px] leading-snug">
                <span className="w-[34px] shrink-0 text-ink-3">{label}</span>
                <span className="line-clamp-2 text-ink">{line.productName || line.spec || line.name}</span>
              </span>
            )
          })}
        </span>
      </button>
    </SpotlightCard>
  )
}
