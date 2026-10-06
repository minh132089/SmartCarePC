export type PartId = 'case' | 'motherboard' | 'cpu' | 'cooler' | 'ram' | 'gpu' | 'ssd' | 'psu' | 'fans' | 'cables' | 'tools'

// Mô tả viết cho người không rành công nghệ.
export const PARTS: Record<PartId, { name: string; about: string }> = {
  case: { name: 'Vỏ máy (case)', about: 'Giữ và bảo vệ mọi linh kiện bên trong, có chỗ gắn quạt để lấy gió.' },
  motherboard: {
    name: 'Bo mạch chủ',
    about: 'Tấm mạch lớn nối mọi linh kiện với nhau. Mỗi linh kiện có một khe cắm riêng trên tấm này.',
  },
  cpu: { name: 'CPU (bộ xử lý)', about: 'Bộ não của máy. Nằm ngay dưới tản nhiệt.' },
  cooler: { name: 'Tản nhiệt CPU', about: 'Hút nhiệt từ CPU ra ngoài để máy không bị quá nóng.' },
  ram: { name: 'RAM', about: 'Bộ nhớ tạm. Càng nhiều thì mở được càng nhiều chương trình cùng lúc.' },
  gpu: { name: 'Card đồ họa (GPU)', about: 'Xử lý hình ảnh. Cần cho chơi game, dựng phim và làm 3D.' },
  ssd: { name: 'Ổ SSD', about: 'Nơi lưu hệ điều hành, phần mềm và dữ liệu của bạn.' },
  psu: { name: 'Nguồn (PSU)', about: 'Lấy điện từ ổ cắm rồi chia cho từng linh kiện.' },
  fans: { name: 'Quạt case', about: 'Đưa gió mát vào và đẩy gió nóng ra khỏi thùng máy.' },
  tools: {
    name: 'Đồ nghề lắp máy',
    about: 'Những thứ cần cầm trên tay khi tự lắp: tua vít, dây chống tĩnh điện, keo tản nhiệt, hộp vít, dây rút, kìm cắt và dây điện. Bấm "Tách rời linh kiện" để thấy cả bộ bày trên thảm cạnh máy.',
  },
  cables: {
    name: 'Dây cáp',
    about:
      'Dây nguồn đen to đưa điện tới bo mạch (24 pin), CPU (8 pin) và card đồ họa (PCIe). Dây quạt nhỏ cắm vào các cổng FAN trên bo. Bó dây nhiều màu từ nút nguồn cắm vào cổng F_PANEL ở góc dưới bên phải, dây xanh dẹt là USB mặt trước.',
  },
}
