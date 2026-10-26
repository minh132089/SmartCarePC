import type { Tier } from './chatStore'

/** Tên và một câu cho từng phương án; nhãn gợi ý lấy theo thẻ chính backend trả về. */
export const TIER_INFO: Record<Tier, { label: string; note: string }> = {
  cheap: { label: "Budget", note: "The lowest-cost suitable build, at least 10% below your budget" },
  balanced: { label: "Best fit", note: "A balance of cost and requirements, within your budget" },
  quality: { label: "Upgraded", note: "A specific upgrade, up to VND 3 million above your budget" },
}
