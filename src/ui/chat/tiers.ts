import type { Tier } from './chatStore'

/** Tên và một câu cho từng phương án; thứ tự hiện là Rẻ, Phù hợp cho bạn (gợi ý chính), Chất lượng */
export const TIER_INFO: Record<Tier, { label: string; note: string }> = {
  cheap: { label: 'Rẻ', note: 'Tiết kiệm nhất mà vẫn đủ dùng, rẻ hơn ngân sách ít nhất 10%' },
  balanced: { label: 'Phù hợp cho bạn', note: 'Sát ngân sách; option 2, 3 có thể hơn 1 đến 2 triệu' },
  quality: { label: 'Chất lượng', note: 'Mạnh và bền hơn, vượt ngân sách tối đa 3 triệu' },
}
