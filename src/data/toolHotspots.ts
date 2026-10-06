import type { Hotspot } from './hotspots'

/**
 * Chú thích "Xem cận cảnh" cho bộ đồ nghề trên thảm (t_kit.glb). Toạ độ theo gốc thảm (tâm mặt trên),
 * đơn vị cảnh (1 = 100 mm), lấy từ bảng bố trí trong design/tools/00_overview.md.
 */
export const TOOL_HOTSPOTS: Hotspot[] = [
  {
    at: [-1.6, 0.34, -1.4],
    title: '1. Tua vít đổi đầu',
    text: 'Đầu PH2 to dùng cho vít bo, vít nguồn, vít quạt. Đầu PH1 nhỏ để rời bên cạnh, chỉ dùng cho con vít M.2 của ổ SSD. Đầu có nam châm giữ vít.',
  },
  {
    at: [1.3, 0.22, -1.35],
    title: '2. Dây chống tĩnh điện',
    text: 'Bước đầu tiên: rút điện, đeo vòng vào cổ tay cho miếng kim loại chạm da, kẹp cá sấu vào phần sắt không sơn của khung case. Tháo ra trước khi cắm điện thử máy.',
  },
  {
    at: [-1.9, 0.14, -0.45],
    title: '3. Keo tản nhiệt',
    text: 'Bóp một chấm cỡ hạt đậu vào giữa nắp CPU ngay trước khi đặt tản. Nếu đế tản đã có sẵn keo thì không bôi thêm. Thẻ gạt chỉ dùng khi hướng dẫn của tản yêu cầu dàn mỏng.',
  },
  {
    at: [0.3, 0.2, -0.2],
    title: '4. Hộp vít',
    text: 'Tám ngăn: vít bo M3, cọc đỡ bo, vít M.2, vít nguồn và nắp khe PCIe, vít quạt, vít két nước ngắn và dài, đầu tuýp 5 mm để vặn cọc đỡ. Lấy đúng ngăn cho từng bước, đừng ép vít khác loại.',
  },
  {
    at: [-1.85, 0.05, 0.8],
    title: '5. Dây rút và dây dán',
    text: 'Gom gọn bó dây sau khay bo. Dây rút siết vừa tay rồi cắt phần thừa; dây dán gai tháo ra dùng lại được.',
  },
  {
    at: [0.25, 0.1, 1.35],
    title: '6. Dây nguồn điện',
    text: 'Đầu C13 (ba lỗ) cắm vào ổ C14 ở lưng nguồn, phích hai chân tròn cắm ổ tường. Đây là thứ cắm cuối cùng, khi máy đã lắp xong và đóng nắp.',
  },
  {
    at: [2.25, 0.1, -0.2],
    title: '7. Ăng-ten Wi-Fi',
    text: 'Chỉ máy có Wi-Fi trên bo (máy Gaming) mới cần. Vặn hai ăng-ten vào hai cổng ren ở cụm cổng sau rồi dựng đứng lên.',
  },
  {
    at: [2.05, 0.08, 1.3],
    title: '8. Kìm cắt sát',
    text: 'Cắt đuôi dây rút sát đầu khoá cho khỏi cấn tay. Giữ mỏ kìm tránh xa dây điện và dây quạt.',
  },
]
