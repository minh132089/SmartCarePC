import type { Tier } from './chatStore'

/** Tên và một câu cho từng phương án; nhãn gợi ý lấy theo thẻ chính backend trả về. */
export const TIER_INFO: Record<Tier, { label: string; note: string }> = {
  cheap: { label: 'Rẻ', note: 'Tiết kiệm nhất mà vẫn đủ dùng, rẻ hơn ngân sách ít nhất 10%' },
  balanced: { label: 'Phù hợp cho bạn', note: 'Cân bằng chi phí và nhu cầu, trong ngân sách' },
  quality: { label: 'Chất lượng', note: 'Có nâng cấp cụ thể, vượt ngân sách tối đa 3 triệu' },
}
