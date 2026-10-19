import type { Tier } from './chatStore'

/** Tên và một câu cho từng phương án; thứ tự hiện là Rẻ, Phù hợp cho bạn (gợi ý chính), Chất lượng */
export const TIER_INFO: Record<Tier, { label: string; note: string }> = {
  cheap: { label: 'Rẻ', note: 'Tiết kiệm nhất mà vẫn đủ dùng' },
  balanced: { label: 'Phù hợp cho bạn', note: 'Cân bằng giá và chất lượng' },
  quality: { label: 'Chất lượng', note: 'Mạnh và bền hơn, có thể vượt ngân sách một chút' },
}
