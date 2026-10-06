import { BOARD_LAYOUT, type Family } from './families'
import { gamingHotspots } from './gamingHotspots'
import type { Hotspot } from './hotspots'
import type { PartId } from './parts'

/**
 * Chú thích "Xem cận cảnh" cho 6 họ case. Linh kiện dùng chung model Gaming (CPU, RAM, SSD, GPU 280, PSU ATX, AIO 240)
 * lấy lại chú thích Gaming, chỉ sửa câu nào phụ thuộc cách lắp. Linh kiện mới đọc toạ độ từ boards.json và
 * tools/build_families.py. Đơn vị app (1 = 100 mm), theo gốc của từng model.
 */
const mm = (x: number, y: number, z: number): [number, number, number] => [x / 100, y / 100, z / 100]

/** Đổi lời giải thích của một chú thích có sẵn theo tiêu đề. */
function retext(spots: Hotspot[], fix: Record<string, string>): Hotspot[] {
  return spots.map((s) => (fix[s.title] ? { ...s, text: fix[s.title] } : s))
}

function boardSpots(f: Family): Hotspot[] {
  if (f.board.ff === 'ATX') {
    const argb = f.fans.some((x) => x.model === 'fan120_argb')
    const aio = f.cooler.kind.startsWith('aio')
    return gamingHotspots('motherboard').filter((s) => (argb || !s.title.startsWith('JRAINBOW')) && (aio || !s.title.startsWith('AIO_PUMP')))
  }
  const L = BOARD_LAYOUT[f.board.ff]
  const itx = f.board.ff === 'ITX'
  const io = (L as unknown as { io: { x: [number, number]; y: [number, number] } }).io
  const ramX = L.ram_used.reduce((a, i) => a + L.ram_x[i], 0) / L.ram_used.length
  const [px, py] = L.pcie[0]
  const spots: Hotspot[] = [
    { at: mm(L.cpu[0], L.cpu[1], 7), title: 'Socket CPU', text: 'Nhấc cần gạt, mở khung thép, đặt CPU theo dấu tam giác ở góc rồi hạ khung và cần xuống.' },
    {
      at: mm(ramX, L.ram_y + 74, 16),
      title: itx ? 'Hai khe RAM' : 'Khe RAM A2 và B2',
      text: itx ? 'Bo nhỏ chỉ có 2 khe, cắm cả hai để chạy kênh đôi. Ấn đều hai đầu đến khi chốt bật.' : 'Bo có 4 khe. Máy này 2 thanh nên cắm vào hai khe có chốt màu xanh (A2 và B2).',
    },
    {
      at: mm(px, py, 13),
      title: 'Khe PCIe x16',
      text: f.riser ? 'Đầu cáp riser cắm vào đây, card đồ họa nằm ở chỗ khác và nối qua cáp này.' : 'Khe dài bọc thép. Card đồ họa cắm vào đây, chốt ở đầu khe tự khoá lại.',
    },
    { at: mm(L.headers.atx24[0], L.headers.atx24[1], 15), title: 'Cổng nguồn 24 pin', text: 'Cổng lớn nhất trên bo. Đầu dây có lẫy, ấn đến khi nghe tiếng tách.' },
    { at: mm(L.headers.eps[0], L.headers.eps[1], 15), title: 'Cổng nguồn CPU 8 pin', text: 'Ở mép trên, ghi CPU_8. Dùng đầu 4+4 ghi CPU, không dùng đầu 6+2 của card đồ họa.' },
    { at: mm(L.headers.cpu_fan[0], L.headers.cpu_fan[1], 10), title: 'CPU_FAN', text: 'Đầu 4 chân cho quạt của tản CPU. Đầu cắm có gờ nên chỉ vào một chiều.' },
    { at: mm(L.m2_center[0], L.m2_center[1], 6), title: 'Khe M.2', text: 'Ổ SSD cắm nghiêng vào khe, hạ xuống cọc đỡ ở cuối rồi bắt một con vít nhỏ.' },
    { at: mm(L.headers.panel[0], L.headers.panel[1], 9), title: 'F_PANEL', text: 'Dây nút nguồn, nút reset và đèn của case cắm ở đây. Đèn phải đúng dấu + và −.' },
    { at: mm(L.headers.usb3[0], L.headers.usb3[1], 11), title: 'USB 3.0 mặt trước', text: 'Đầu 19 chân có vỏ và một khe khoá. Cắm thẳng, đúng chiều khe, không ép nghiêng.' },
  ]
  if (L.headers.type_e) spots.push({ at: mm(L.headers.type_e[0], L.headers.type_e[1], 11), title: 'Đầu USB-C mặt trước', text: 'Đầu nhỏ có gờ khoá cho cổng USB-C trên vỏ. Cắm thẳng, đúng chiều gờ.' })
  spots.push(
    { at: mm(io.x[0], (io.y[0] + io.y[1]) / 2, 20), title: 'Cụm cổng sau', text: 'USB, mạng và âm thanh quay ra lưng case. Màn hình cắm vào card đồ họa, không cắm ở đây.' },
    { at: mm(L.holes[0][0], L.holes[0][1], 2), title: 'Lỗ bắt vít bo', text: `Bo có ${L.holes.length} lỗ, khớp ${L.holes.length} cọc đỡ trên case. Bắt đủ vít M3, vặn vừa tay.` },
  )
  return spots
}

function gpuSpots(f: Family): Hotspot[] {
  const fanText = f.gpu.rot === 'R0' ? 'Quạt quay mặt xuống đáy máy, hút gió thổi vào khối tản nhiệt.' : 'Card dựng qua cáp riser, quạt quay về phía tấm lưới hoặc kính để hút gió mát.'
  const pcieText = f.riser ? 'Cắm vào đầu cái của cáp riser, ấn đều đến khi chốt khoá. Đầu kia của cáp cắm vào khe x16 trên bo.' : 'Cắm vào khe PCIe bọc thép gần CPU nhất. Mở chốt trước, ấn đều đến khi chốt tự khoá.'
  if (f.gpu.kind === 'gpu_280') return retext(gamingHotspots('gpu'), { 'Ba quạt': fanText, 'Chân PCIe x16': pcieText })
  return [
    { at: mm(-22, 19.7, -4), title: 'Chân PCIe x16', text: pcieText },
    { at: mm(105, 12.75, 120), title: 'Ổ nguồn 8 chân', text: 'Cắm đầu 6+2 chân ghi PCIe từ nguồn. Ghép 6 và 2 thành 8 rồi ấn đến khi lẫy giữ.' },
    { at: mm(-60, -25, 60), title: 'Hai quạt', text: fanText },
    { at: mm(-122, 12.7, 56), title: 'Cổng màn hình', text: '3 cổng DisplayPort và 1 HDMI. Dây màn hình cắm vào đây, không cắm vào cổng trên mainboard.' },
    { at: mm(-122, 15.5, 113), title: '2 vít giữ', text: 'Hai tai trên lá cổng bắt vít vào khe PCIe ở lưng case.' },
    { at: mm(89, 22.5, 62), title: 'Cửa thoát khí', text: 'Tấm lưng có cửa khoét để gió nóng thoát ra.' },
  ]
}

function psuSpots(f: Family): Hotspot[] {
  if (f.psu.kind === 'atx') {
    const fix: Record<string, string> = {}
    if (f.psu.rot !== 'R0') fix['Quạt hút'] = 'Nguồn đứng sau khay bo, quạt quay về tấm lưới bên hông để hút gió mát.'
    if (f.id === 'showcase_dual_chamber') fix['Ba dây liền'] = 'Dây 24 pin, dây CPU 4+4 và dây PCIe 6+2 đi trong khoang sau rồi chui qua các lỗ cao su ra khoang kính.'
    return retext(gamingHotspots('psu'), fix)
  }
  return [
    { at: mm(-52, -9, 43), title: 'Ổ cắm điện', text: 'Trong máy nhỏ, ổ này nối bằng dây nối dài tới ổ cắm ở lưng case. Chỉ cắm điện khi đã lắp xong.' },
    { at: mm(-53, 17, 43), title: 'Công tắc I/O', text: 'Để ở O (tắt) trong lúc lắp. Lắp xong mới gạt sang I.' },
    { at: mm(0, -33, 0), title: 'Quạt 92 mm', text: 'Nguồn SFX nhỏ hơn ATX nhiều. Quạt hút gió qua tấm lưới bên cạnh.' },
    { at: mm(58, -13, 35), title: 'Ba dây liền', text: 'Dây 24 pin, dây CPU 4+4 và dây PCIe 6+2 liền thân nguồn, gom gọn sát vách.' },
    { at: mm(-50.5, 25, 55), title: '4 lỗ vít', text: 'Bốn lỗ M3 để bắt nguồn vào giá chữ L trong case.' },
  ]
}

function coolerSpots(f: Family): Hotspot[] {
  switch (f.cooler.kind) {
    case 'aio_240':
      return gamingHotspots('cooler')
    case 'aio_360':
      return gamingHotspots('cooler').map((s) => {
        if (s.title === 'Vít ngắn (nóc)') return { ...s, at: mm(-172.5, 5, -52.5), text: '12 vít M3 × 6 giữ két 360 vào nóc case. Đừng nhầm với vít dài của quạt.' }
        if (s.title === 'Vít dài (quạt)') return { ...s, at: mm(-172.5, -54, 52.5), text: '12 vít M3 × 30 xuyên qua khung ba quạt vào két.' }
        if (s.title === 'Mũi tên chiều gió') return { ...s, at: mm(120, -40, 61) }
        return s
      })
    case 'tower_air_155':
      return [
        { at: mm(0, -21, 1), title: 'Đế đồng', text: 'Mặt áp lên CPU. Bóc miếng nilon dưới đế, bôi keo tản nhiệt cỡ hạt đậu lên CPU trước khi lắp.' },
        { at: mm(39, 39, 12.5), title: 'Vít lò xo', text: 'Bốn vít có lò xo. Siết chéo góc, mỗi vít vài vòng, đến khi hết ren thì dừng.' },
        { at: mm(-12, 22, 156), title: 'Ống đồng', text: 'Bốn ống dẫn nhiệt từ đế lên khối lá nhôm.' },
        { at: mm(-40, 30, 100), title: 'Khối lá nhôm', text: 'Cao 155 mm. Case phải đủ rộng để đóng nắp hông, kiểm tra trước khi mua.' },
        { at: mm(35, 0, 90), title: 'Quạt 120 mm', text: 'Thổi gió xuyên qua lá nhôm về phía lưng case. Dây quạt cắm vào CPU_FAN.' },
        { at: mm(8, -58, 140), title: 'Kẹp quạt', text: 'Hai kẹp thép giữ quạt vào khối lá. Bóp nhẹ kẹp để tháo quạt khi lắp RAM.' },
      ]
    case 'low_profile_47':
      return [
        { at: mm(0, -21, 1), title: 'Đế đồng', text: 'Mặt áp lên CPU. Bóc miếng nilon dưới đế, bôi keo tản nhiệt cỡ hạt đậu lên CPU trước khi lắp.' },
        { at: mm(39, 39, 12.5), title: 'Vít lò xo', text: 'Bốn vít nằm trong hốc ở bốn góc, thò tuốc nơ vít xuống để siết chéo góc.' },
        { at: mm(46, 30, 12), title: 'Ống đồng', text: 'Ống dẫn nhiệt nằm ngang trong lá nhôm.' },
        { at: mm(-40, 46, 20), title: 'Lá nhôm thấp', text: 'Cả bộ chỉ cao 47 mm, vừa với case nhỏ và case mỏng.' },
        { at: mm(0, 0, 47), title: 'Quạt 92 mm', text: 'Thổi gió xuống CPU và bo. Dây quạt cắm vào CPU_FAN.' },
      ]
  }
}

function fanSpots(f: Family): Hotspot[] {
  const fan = f.fans[0]
  if (!fan) return []
  const size = fan.model === 'fan140' ? 140 : 120
  const k = size / 140
  const hub = f.hub ? 'Cắm vào hub quạt, mỗi quạt một cổng PWM có số.' : 'Cắm vào đầu SYS_FAN gần nhất trên mainboard.'
  return gamingHotspots('fans')
    .filter((s) => fan.model === 'fan120_argb' || !s.title.startsWith('Vòng đèn'))
    .map((s) => {
      const at = [s.at[0], s.at[1] * k, s.at[2] * k] as [number, number, number]
      if (s.title === 'Mũi tên chiều gió') return { ...s, at, text: 'Mũi tên trên khung chỉ hướng gió. Quạt trước và đáy hút gió vào, quạt sau và nóc thổi ra.' }
      if (s.title === 'Lỗ vít') return { ...s, at, text: `Quạt ${size} mm: bốn lỗ cách nhau ${size === 140 ? '124,5' : '105'} mm, bắt bằng vít tự ren.` }
      if (s.title === 'Vòng đèn ARGB') return { ...s, at, text: `Vòng sáng ở mặt hút. Dây đèn 3 chân 5 V ${f.hub ? 'cắm vào hàng ARGB của hub' : 'cắm nối tiếp về đầu ARGB trên bo'}.` }
      if (s.title === 'Dây 4 chân') return { ...s, at, text: `${hub} Đầu cắm có gờ nên chỉ vào một chiều.` }
      return { ...s, at }
    })
}

export function familyHotspots(f: Family, id: PartId): Hotspot[] {
  switch (id) {
    case 'motherboard':
      return boardSpots(f)
    case 'cpu':
      return retext(gamingHotspots('cpu'), {
        'Nắp kim loại': f.cooler.kind.startsWith('aio') ? 'Mặt áp vào đế bơm tản nước. Bôi keo tản nhiệt cỡ hạt đậu vào giữa trước khi lắp bơm.' : 'Mặt áp vào đế đồng của tản khí. Bôi keo tản nhiệt cỡ hạt đậu vào giữa trước khi lắp tản.',
      })
    case 'ram':
    case 'ssd':
      return gamingHotspots(id)
    case 'gpu':
      return gpuSpots(f)
    case 'psu':
      return psuSpots(f)
    case 'cooler':
      return coolerSpots(f)
    case 'fans':
      return fanSpots(f)
    default:
      return []
  }
}
