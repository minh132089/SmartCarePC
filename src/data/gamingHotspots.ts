import type { Hotspot } from './hotspots'
import type { PartId } from './parts'

/**
 * Chú thích "Xem cận cảnh" cho bộ linh kiện Gaming. Toạ độ theo gốc của từng model g_*.glb (đơn vị app,
 * 1 = 100 mm), lấy thẳng từ số đo trong tools/build_gaming.py nên khớp đúng chi tiết trên model.
 */
export function gamingHotspots(id: PartId): Hotspot[] {
  switch (id) {
    case 'motherboard':
      return [
        { at: [0.19, 0.59, 0.07], title: 'Socket CPU', text: 'Nhấc cần gạt bên phải, mở khung thép, đặt CPU theo dấu tam giác ở góc dưới trái rồi hạ khung và cần xuống.' },
        { at: [0.885, 1.36, 0.16], title: 'Khe RAM A2 và B2', text: 'Hai khe có chốt màu xanh. Máy này có 2 thanh RAM nên cắm vào A2 và B2 để chạy kênh đôi.' },
        { at: [-0.34, -0.3, 0.13], title: 'Khe PCIe x16', text: 'Khe dài bọc thép, gần CPU nhất. Card đồ họa cắm vào đây, chốt nhựa ở đầu phải tự khoá lại.' },
        { at: [1.17, 0.4, 0.15], title: 'Cổng nguồn 24 pin', text: 'Cổng lớn nhất trên bo, ở mép phải. Đầu dây có lẫy, ấn đến khi nghe tiếng tách.' },
        { at: [-0.6, 1.46, 0.15], title: 'Cổng nguồn CPU 8 pin', text: 'Ở mép trên, ghi CPU_8. Dùng đầu 4+4 ghi CPU, không dùng đầu 6+2 của card đồ họa.' },
        { at: [0.225, 1.26, 0.1], title: 'AIO_PUMP và CPU_FAN', text: 'Hai đầu 4 chân cạnh nhau. Dây bơm cắm AIO_PUMP, dây hai quạt két (qua dây chia) cắm CPU_FAN.' },
        { at: [-0.05, -0.62, 0.06], title: 'Khe M.2', text: 'Ổ SSD cắm nghiêng vào đây, hạ xuống cọc đỡ bên phải rồi bắt một con vít nhỏ.' },
        { at: [0.95, -1.43, 0.09], title: 'F_PANEL', text: 'Dây nút nguồn, nút reset và đèn của case cắm ở đây. Chữ in trên bo chỉ đúng chỗ từng dây, đèn phải đúng dấu + và −.' },
        { at: [1.13, -1.27, 0.11], title: 'USB 3.0 mặt trước', text: 'Đầu 19 chân có vỏ và một khe khoá. Cắm thẳng, đúng chiều khe, không ép nghiêng.' },
        { at: [0.55, -1.43, 0.09], title: 'JRAINBOW1 (đèn ARGB)', text: 'Đầu 3 chân 5 V cho vòng đèn của quạt trước, có một vị trí bịt kín để không cắm ngược. Không cắm nhầm vào đầu RGB 12 V.' },
        { at: [-1.37, 0.6, 0.2], title: 'Cụm cổng sau', text: 'Quay ra lưng case: USB màu đen/xanh/đỏ theo tốc độ, USB-C, mạng, 2 chân ăng-ten Wi-Fi, HDMI/DP, âm thanh. Màn hình nên cắm vào card đồ họa, không cắm HDMI/DP ở đây.' },
        { at: [0, 0, 0.02], title: 'Lỗ bắt vít bo', text: 'Bo có 9 lỗ như thế này, khớp 9 cọc đỡ trên case. Bắt đủ 9 vít M3, vặn vừa tay.' },
      ]
    case 'cpu':
      return [
        { at: [-0.174, -0.174, 0.013], title: 'Dấu tam giác', text: 'Góc có tam giác vàng phải trùng góc có tam giác trên socket. Đặt nhẹ xuống, không ấn, không trượt.' },
        { at: [0.1875, 0.05, 0.006], title: 'Rãnh định hướng', text: 'Hai rãnh nhỏ ở hai cạnh khớp với hai gờ nhựa trong socket. Sai chiều thì CPU không nằm phẳng được.' },
        { at: [0, 0, 0.046], title: 'Nắp kim loại', text: 'Mặt áp vào đế bơm tản nước. Bôi keo tản nhiệt cỡ hạt đậu vào giữa trước khi lắp bơm.' },
        { at: [0, -0.12, -0.001], title: 'Tiếp điểm vàng', text: 'Mặt dưới là các ô vàng phẳng, rất dễ bẩn. Chỉ cầm CPU ở hai cạnh.' },
      ]
    case 'cooler':
      return [
        { at: [0, -1.1, 0.48], title: 'Bơm', text: 'Khối tròn đặt lên CPU. Bên trong có bơm đẩy nước đi vòng qua két rồi quay về.' },
        { at: [0.39, -0.71, 0.14], title: 'Vít lò xo', text: 'Bốn vít có lò xo giữ bơm. Siết chéo góc, mỗi vít vài vòng, đến khi hết ren thì dừng.' },
        { at: [0.25, -1.1, -0.001], title: 'Màng bảo vệ', text: 'Lớp nilon trong dưới đế đồng. Kéo tai nhỏ để bóc ra trước khi lắp, quên bóc thì CPU sẽ rất nóng.' },
        { at: [0, -0.72, 0.34], title: 'Hai ống nước', text: 'Nối bơm với két. Không gập ống, không kéo căng.' },
        { at: [-1.125, 0.05, -0.525], title: 'Vít ngắn (nóc)', text: '8 vít M3 × 6 giữ két vào nóc case. Đừng nhầm với vít dài của quạt, vít dài có thể đâm thủng két.' },
        { at: [0.6, -0.4, 0.61], title: 'Mũi tên chiều gió', text: 'Quạt két thổi gió lên trên, ra ngoài qua nóc. Mũi tên đúc trên khung quạt chỉ chiều gió.' },
        { at: [-1.125, -0.54, 0.525], title: 'Vít dài (quạt)', text: '8 vít M3 × 30 xuyên qua khung quạt vào két.' },
      ]
    case 'ram':
      return [
        { at: [0, 0.0425, 0.015], title: 'Khấc khoá', text: 'Khe hở lệch khỏi chính giữa hàng chân. Khớp với gờ trong khe RAM nên chỉ cắm được một chiều.' },
        { at: [0.007, -0.3, 0.026], title: 'Chân cắm mạ vàng', text: '288 tiếp điểm, 144 mỗi mặt. Cầm RAM ở hai cạnh, tránh chạm tay vào phần vàng.' },
        { at: [0, 0.66, 0.085], title: 'Hõm giữ chốt', text: 'Hai đầu có hõm nhỏ. Ấn RAM đủ sâu thì chốt hai đầu khe bật vào đây và kêu tách.' },
        { at: [0.031, 0.2, 0.25], title: 'Tấm tản nhiệt', text: 'Vỏ nhôm giúp RAM mát hơn. RAM không cần vít hay dây nào.' },
      ]
    case 'gpu':
      return [
        { at: [-0.42, 0.247, -0.04], title: 'Chân PCIe x16', text: 'Cắm vào khe PCIe bọc thép gần CPU nhất. Mở chốt ở đầu khe trước, ấn đều đến khi chốt tự khoá.' },
        { at: [1.25, 0.185, 1.25], title: 'Ổ nguồn 8 chân', text: 'Cắm đầu 6+2 chân ghi PCIe từ nguồn. Ghép 6 và 2 thành 8 rồi ấn đến khi lẫy giữ.' },
        { at: [0, -0.275, 0.625], title: 'Ba quạt', text: 'Quạt quay mặt xuống đáy máy, hút gió thổi vào khối tản nhiệt.' },
        { at: [-1.41, 0.17, 0.56], title: 'Cổng màn hình', text: '3 cổng DisplayPort và 1 HDMI. Dây màn hình cắm vào đây, không cắm vào cổng trên mainboard.' },
        { at: [-1.42, 0.2, 1.13], title: '2 vít giữ', text: 'Tháo 2 nắp khe PCIe ở lưng case, giữ lại 2 vít để bắt card vào.' },
        { at: [1.08, 0.276, 0.69], title: 'Cửa thoát khí', text: 'Tấm lưng có cửa khoét để gió nóng thoát lên trên.' },
      ]
    case 'ssd':
      return [
        { at: [-0.39, -0.065, 0.02], title: 'Khấc M-key', text: 'Khe nhỏ lệch về một bên, khớp gờ trong khe M.2. Cắm nghiêng khoảng 25° rồi hạ xuống.' },
        { at: [0.4, 0, 0.025], title: 'Vít giữ', text: 'Một vít M2 nhỏ bắt vào rãnh bán nguyệt ở cuối ổ. Vặn vừa đủ, đừng làm cong ổ.' },
        { at: [-0.07, 0, 0.032], title: 'Chip nhớ', text: 'Lưu toàn bộ dữ liệu. SSD M.2 không cần dây nguồn hay dây dữ liệu.' },
      ]
    case 'psu':
      return [
        { at: [-0.71, -0.16, 0.49], title: 'Ổ cắm điện', text: 'Dây điện từ ổ tường cắm vào đây, và chỉ cắm ở bước cuối cùng khi đã lắp xong máy.' },
        { at: [-0.72, 0.14, 0.49], title: 'Công tắc I/O', text: 'Để ở O (tắt) trong lúc lắp. Lắp xong mới gạt sang I.' },
        { at: [0, -0.44, 0], title: 'Quạt hút', text: 'Đặt quạt quay xuống đáy case, nơi có tấm lọc bụi, để nguồn hút gió mát từ dưới lên.' },
        { at: [0.71, -0.13, 0.5], title: 'Ba dây liền', text: 'Dây 24 pin, dây CPU 4+4 và dây PCIe 6+2. Luồn qua lỗ G0 rồi đi sau khay mainboard.' },
        { at: [-0.72, -0.37, -0.69], title: '4 vít sau', text: 'Bốn lỗ xếp lệch nhau, chỉ khớp một chiều. Bắt đủ 4 vít từ ngoài lưng case.' },
      ]
    case 'fans':
      return [
        { at: [0, 0, 0.705], title: 'Mũi tên chiều gió', text: 'Mũi tên trên khung chỉ hướng gió. Ba quạt trước thổi vào trong, quạt sau thổi ra ngoài.' },
        { at: [-0.13, 0, 0.2], title: 'Mặt có nan', text: 'Mặt có bốn nan đỡ là mặt gió thổi ra.' },
        { at: [0.125, 0.6225, 0.6225], title: 'Lỗ vít', text: 'Quạt trước loại 140 mm: bốn lỗ cách nhau 124,5 mm, bắt bằng vít tự ren từ phía trước vào giá quạt.' },
        { at: [0.13, 0.35, 0.55], title: 'Vòng đèn ARGB', text: 'Vòng sáng cyan ở mặt hút. Đèn có dây 3 chân 5 V riêng, cắm nối tiếp sang quạt kế bên rồi về đầu JRAINBOW1 trên bo.' },
        { at: [-0.1, 0.55, -0.71], title: 'Dây 4 chân', text: 'Cắm vào đầu SYS_FAN gần nhất trên mainboard. Đầu cắm có gờ nên chỉ vào một chiều.' },
      ]
    default:
      return []
  }
}
