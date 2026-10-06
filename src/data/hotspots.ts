import type { CoolerKind } from './builds'
import type { PartId } from './parts'

/**
 * Chấm chú thích cho chế độ "Xem cận cảnh". Toạ độ theo gốc của từng model (đơn vị app, 1 = 100 mm),
 * đo bằng tools/find_features.py và ảnh lưới khi xử lý model. Lời giải thích viết cho người mới,
 * để họ đối chiếu với linh kiện thật đang cầm trên tay.
 */
export interface Hotspot {
  at: [number, number, number]
  title: string
  text: string
}

/** Linh kiện có chế độ xem cận cảnh (vỏ case và dây nguồn thì không cần). */
export const INSPECTABLE: PartId[] = ['motherboard', 'cpu', 'cooler', 'ram', 'gpu', 'ssd', 'psu', 'fans', 'tools']

type Variant = 'heatsink' | 'plain'

export function hotspotsFor(
  id: PartId,
  opts: { ram?: Variant; board?: 'ITX' | 'mATX' | 'ATX'; cooler?: CoolerKind } = {},
): Hotspot[] {
  switch (id) {
    case 'ram': {
      // Khấc khoá đo trên từng model: RAM có tản nhiệt lệch tâm -0,151, RAM thường khoảng -0,107
      const notch = opts.ram === 'plain' ? -0.107 : -0.151
      const top = opts.ram === 'plain' ? 0.3 : 0.5
      return [
        {
          at: [0, notch, 0.01],
          title: 'Khấc khoá',
          text: 'Khe hở nhỏ lệch khỏi chính giữa hàng chân. Nó khớp với gờ trong khe RAM, nên RAM chỉ cắm được một chiều. Cắm không vào thì xoay ngược lại, đừng cố ấn.',
        },
        {
          at: [0, 0.38, 0.015],
          title: 'Chân cắm mạ vàng',
          text: 'Hàng tiếp điểm cắm xuống khe. Cầm RAM ở hai cạnh bên, tránh chạm tay vào phần vàng này.',
        },
        {
          at: [0, 0.64, 0.08],
          title: 'Rãnh chốt',
          text: 'Hai đầu có khuyết lõm. Khi ấn RAM đủ sâu, lẫy nhựa ở hai đầu khe bật vào đây và kêu "tách".',
        },
        {
          at: [0.03, 0.1, top * 0.7],
          title: opts.ram === 'plain' ? 'Chip nhớ' : 'Tấm tản nhiệt',
          text:
            opts.ram === 'plain'
              ? 'Các con chip đen là nơi lưu dữ liệu tạm. RAM thường để lộ chip ra ngoài.'
              : 'Lớp vỏ kim loại giúp RAM mát hơn. RAM loại này cao hơn, nên kiểm tra tản nhiệt CPU có bị cấn không.',
        },
      ]
    }
    case 'cpu':
      return [
        {
          at: [-0.18, 0.175, 0.016],
          title: 'Dấu tam giác',
          text: 'Góc có tam giác vàng phải trùng với góc có dấu tam giác trên socket. Đặt CPU nhẹ xuống, không ấn, không trượt.',
        },
        {
          at: [0, 0, 0.045],
          title: 'Nắp kim loại',
          text: 'Mặt này tiếp xúc với đế tản nhiệt. Bôi một lượng keo tản nhiệt cỡ hạt đậu vào giữa trước khi lắp tản.',
        },
        {
          at: [0.19, -0.1, 0.006],
          title: 'Đế mạch xanh',
          text: 'Mặt dưới có chân tiếp xúc rất dễ hỏng. Chỉ cầm CPU ở hai cạnh.',
        },
      ]
    case 'gpu':
      return [
        {
          at: [-0.6, 0, 0.24],
          title: 'Chân cắm PCIe',
          text: 'Cắm vào khe PCIe dài nhất, gần CPU nhất. Nhớ mở lẫy nhựa ở đầu khe trước, ấn đều tay đến khi lẫy tự khoá.',
        },
        {
          at: [-1.38, 0, 0.66],
          title: 'Tấm chắn và cổng xuất hình',
          text: 'Tấm kim loại bắt vít vào lưng case. Cắm dây màn hình vào đây, không cắm vào cổng trên mainboard.',
        },
        {
          at: [0.5, 0, 1.32],
          title: 'Cổng nguồn PCIe',
          text: 'Cắm dây PCIe từ nguồn vào đây. Thiếu dây này card không chạy, máy vẫn lên nhưng không có hình.',
        },
        {
          at: [0.3, -0.27, 0.66],
          title: 'Quạt tản nhiệt',
          text: 'Quạt nằm ở mặt dưới, hướng xuống phía nguồn. Để trống khoảng dưới card cho gió lưu thông.',
        },
      ]
    case 'ssd':
      return [
        {
          at: [-0.39, 0, 0.012],
          title: 'Đầu cắm M.2 có khấc',
          text: 'Cắm nghiêng khoảng 30 độ vào khe M.2, khấc chỉ khớp một chiều, rồi ấn phần đuôi xuống.',
        },
        {
          at: [0.39, 0, 0.012],
          title: 'Rãnh vít',
          text: 'Nửa vòng tròn ở đuôi. Dùng con vít nhỏ đi kèm mainboard để giữ SSD nằm phẳng.',
        },
        {
          at: [0.05, 0, 0.026],
          title: 'Chip nhớ',
          text: 'Mặt có chip quay lên trên. Nếu mainboard có tấm tản nhiệt M.2, nhớ bóc lớp nilon ở mặt dưới tấm tản.',
        },
      ]
    case 'psu':
      return [
        {
          at: [0.7, 0.05, 0.1],
          title: 'Cổng cắm dây',
          text: 'Mỗi ổ ghi sẵn tên (CPU, PCIe, 24-pin). Cắm dây vào nguồn trước khi đặt nguồn vào case sẽ dễ hơn.',
        },
        {
          at: [-0.7, -0.15, 0.35],
          title: 'Ổ điện và công tắc',
          text: 'Quay ra lưng máy. Nhớ bật công tắc sang "I" sau khi lắp xong.',
        },
        {
          at: [0, 0.41, 0],
          title: 'Quạt nguồn',
          text: 'Nếu đáy case có lỗ thông gió, lắp quạt úp xuống để nguồn hút gió mát từ dưới lên.',
        },
      ]
    case 'cooler':
      // Tản nước: xem cận cảnh hiện két tản ở trên, bơm ở dưới (toạ độ theo cách xếp trong Inspector)
      if (opts.cooler === 'aio')
        return [
          { at: [0, -1.1, 0.43], title: 'Bơm', text: 'Khối tròn đặt lên CPU, thay chỗ của tản khí. Bên trong có bơm đẩy nước đi vòng qua két.' },
          { at: [0.33, -1.1, 0.03], title: 'Đế đồng', text: 'Áp lên CPU. Bóc lớp nilon bảo vệ trước khi lắp, và kiểm tra đã có keo tản nhiệt chưa.' },
          { at: [-1.05, -0.08, 0.6], title: 'Két tản nhiệt', text: 'Gắn lên nóc hoặc mặt trước case. Nước nóng từ bơm chảy qua két, được quạt làm mát rồi quay về bơm qua hai ống.' },
          { at: [0.59, -0.33, 0.3], title: 'Quạt két', text: 'Gắn nóc thì cho quạt thổi gió lên, ra ngoài. Cắm dây quạt vào cổng CPU_FAN hoặc AIO_PUMP trên mainboard.' },
        ]
      if (opts.cooler && opts.cooler !== 'tower')
        return [
          { at: [0, 0, 0.02], title: 'Đế tiếp xúc', text: 'Mặt này áp lên CPU. Tản loại này thường có sẵn một lớp keo xám, không cần bôi thêm.' },
          { at: [0, 0, 0.45], title: 'Quạt úp', text: 'Quạt thổi gió thẳng xuống CPU và các linh kiện xung quanh.' },
          { at: [0.4, 0.3, 0.2], title: 'Lá tản nhiệt', text: 'Nhận nhiệt từ đế rồi toả ra khi có gió thổi qua.' },
        ]
      return [
        {
          at: [0, 0, 0.03],
          title: 'Đế tiếp xúc',
          text: 'Mặt này áp lên CPU. Nếu có lớp nilon bảo vệ thì phải bóc ra trước khi lắp.',
        },
        {
          at: [0, 0.3, 0.9],
          title: 'Lá tản nhiệt',
          text: 'Nhiệt từ CPU truyền lên đây rồi được quạt thổi đi.',
        },
        {
          at: [0.42, 0, 0.95],
          title: 'Quạt',
          text: 'Mũi tên trên khung quạt chỉ hướng gió. Nên để gió thổi từ mặt trước ra phía lưng case.',
        },
      ]
    case 'fans':
      return [
        {
          at: [0.1, 0, 0],
          title: 'Mặt hút gió',
          text: 'Mặt thấy rõ cánh quạt là mặt hút gió. Mặt có thanh đỡ động cơ là mặt thổi gió ra.',
        },
        {
          at: [0, 0.44, 0.44],
          title: 'Lỗ bắt vít',
          text: 'Bốn góc có lỗ để bắt vào case. Quạt trước hút gió vào, quạt sau và quạt nóc thổi gió ra.',
        },
      ]
    case 'motherboard': {
      const dy = opts.board === 'mATX' ? -0.33 : 0
      if (opts.board === 'ITX')
        return [
          { at: [0.07, 0.17, 0.08], title: 'Socket CPU', text: 'Nơi đặt CPU. Mở cần gạt, đặt CPU đúng dấu tam giác rồi hạ cần xuống.' },
          { at: [0.58, 0.06, 0.14], title: 'Khe RAM', text: 'Mở lẫy hai đầu, dóng khấc trên RAM với gờ trong khe rồi ấn thẳng xuống.' },
          { at: [0.84, 0.45, 0.1], title: 'Cổng nguồn 24 pin', text: 'Cổng lớn nhất trên bo. Có lẫy nhựa: ấn đến khi nghe tiếng tách.' },
          { at: [-0.35, 0.8, 0.1], title: 'Cổng nguồn CPU', text: 'Cổng nhỏ ở góc trên. Quên cắm cổng này thì máy không lên.' },
          { at: [-0.75, -0.1, 0.25], title: 'Cụm cổng sau', text: 'Quay ra lưng case: USB, mạng, âm thanh.' },
        ]
      return [
        { at: [0.19, 0.59 + dy, 0.06], title: 'Socket CPU', text: 'Nơi đặt CPU. Mở cần gạt, đặt CPU đúng dấu tam giác rồi hạ cần xuống.' },
        { at: [0.84, 0.62 + dy, 0.12], title: 'Khe RAM', text: 'Có 2 thanh thì cắm khe thứ 2 và thứ 4 tính từ CPU để chạy kênh đôi. Sách hướng dẫn của bo có ghi rõ.' },
        { at: [-0.34, -0.3 + dy, 0.08], title: 'Khe PCIe x16', text: 'Khe dài nhất, gần CPU nhất. Card đồ họa cắm vào đây.' },
        { at: [1.17, 0.4 + dy, 0.1], title: 'Cổng nguồn 24 pin', text: 'Cổng lớn nhất trên bo. Có lẫy nhựa: ấn đến khi nghe tiếng tách.' },
        { at: [-0.6, 1.46 + dy, 0.1], title: 'Cổng nguồn CPU 8 pin', text: 'Nằm ở góc trên bên trái. Quên cắm cổng này thì máy không lên.' },
        { at: [0.35, -0.62 + dy, 0.05], title: 'Khe M.2', text: 'Ổ SSD M.2 cắm ở đây, cố định bằng một con vít nhỏ.' },
      ]
    }
    default:
      return []
  }
}
