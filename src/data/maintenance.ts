import type { PartId } from './parts'

export interface MaintenanceTask {
  id: string
  title: string
  period: 'monthly' | 'quarterly' | 'yearly'
  periodLabel: string
  difficulty: 'Dễ' | 'Trung bình' | 'Cẩn thận'
  partId?: PartId
  summary: string
  importance: string // Lý do tại sao phải làm
  steps: string[]
  caution?: string
}

export const MAINTENANCE_TASKS: MaintenanceTask[] = [
  // Hàng tháng
  {
    id: 'filter_clean',
    title: 'Vệ sinh lưới lọc bụi nam châm',
    period: 'monthly',
    periodLabel: 'Hàng tháng',
    difficulty: 'Dễ',
    partId: 'case',
    summary: 'Tháo các tấm lưới lọc bụi ở nóc, mặt trước và dưới đáy nguồn để giũ hoặc rửa sạch bụi bẩn.',
    importance: 'Lưới lọc bụi bị bít kín sẽ làm thùng máy bí khí, nhiệt độ CPU và GPU có thể tăng thêm 5–10°C.',
    steps: [
      'Gỡ tấm lưới lọc bụi nam châm ở nóc case bằng cách bóc nhẹ góc cạnh viền.',
      'Rút tấm lưới lọc bụi trượt ở dưới đáy bộ nguồn ra ngoài.',
      'Dùng chổi cọ mềm hoặc vòi nước rửa sạch bụi bám trên lưới (nếu rửa nước phải phơi thật khô mới lắp lại).',
      'Đặt lại tấm lưới vào đúng vị trí nam châm hít.',
    ],
    caution: 'Nếu rửa lưới lọc bằng nước, BẮT BUỘC phải phơi khô 100% trước khi lắp lại vào case để tránh giọt nước rơi vào mạch điện.',
  },
  {
    id: 'temp_check',
    title: 'Đo nhiệt độ CPU và GPU khi tải nặng',
    period: 'monthly',
    periodLabel: 'Hàng tháng',
    difficulty: 'Dễ',
    partId: 'cpu',
    summary: 'Kiểm tra nhiệt độ khi máy nghỉ (Idle) và khi chơi game hoặc render nặng bằng phần mềm đo nhiệt độ chuyên dụng.',
    importance: 'Phát hiện sớm tình trạng quá nhiệt trước khi chip tự ngắt hoặc bị chai keo tản nhiệt.',
    steps: [
      'Tải và mở phần mềm HWMonitor hoặc Core Temp (hoặc MSI Afterburner).',
      'Quan sát nhiệt độ khi máy nghỉ (chỉ mở desktop): mức lý tưởng là từ 35°C đến 45°C.',
      'Bật một tựa game hoặc tác vụ nặng trong 10-15 phút: nhiệt độ CPU và GPU an toàn nên dưới 80°C.',
      'Nếu nhiệt độ vượt quá 90°C–95°C và quạt hú rít liên tục, đó là dấu hiệu cần tra keo hoặc vệ sinh tản nhiệt ngay.',
    ],
  },
  {
    id: 'storage_cleanup',
    title: 'Dọn dẹp rác ổ đĩa C và kiểm tra dung lượng',
    period: 'monthly',
    periodLabel: 'Hàng tháng',
    difficulty: 'Dễ',
    partId: 'ssd',
    summary: 'Xóa các tệp tin tạm, file cập nhật Windows cũ và giữ ổ C luôn trống ít nhất 20% dung lượng.',
    importance: 'Ổ SSD nếu bị đầy trên 85% sẽ bị giảm tốc độ đọc ghi đáng kể và nhanh hao mòn bộ nhớ chip flash.',
    steps: [
      'Mở Start > gõ "Disk Cleanup" > chọn ổ C > tích chọn "Temporary files", "Recycle Bin" và nhấn Clean up system files.',
      'Vào Settings > System > Storage > bật tính năng Storage Sense để Windows tự động dọn rác định kỳ.',
      'Đảm bảo ổ C luôn còn ít nhất 25–30 GB dung lượng trống để Windows tạo bộ nhớ đệm ảo và cập nhật mượt mà.',
    ],
  },

  // 3 - 6 Tháng
  {
    id: 'fan_dusting',
    title: 'Thổi bụi quạt tản nhiệt và khoang máy',
    period: 'quarterly',
    periodLabel: 'Định kỳ 3–6 tháng',
    difficulty: 'Trung bình',
    partId: 'fans',
    summary: 'Dùng bóng thổi bụi hoặc bình khí nén làm sạch cánh quạt case, quạt tản nhiệt CPU và quạt card đồ họa.',
    importance: 'Bụi bám dày trên cánh quạt làm nặng cánh, giảm lưu lượng gió (CFM) và làm mòn trục quay phát tiếng ồn khó chịu.',
    steps: [
      'Tắt máy tính hoàn toàn, rút phích cắm điện AC ra khỏi ổ cắm tường.',
      'Mở nắp kính hoặc nắp hông case máy tính.',
      'DÙNG NGÓN TAY HOẶC TĂM CỐ ĐỊNH CÁNH QUẠT không cho quạt tự do quay vòng khi xịt khí nén.',
      'Xịt khí nén theo từng nhịp ngắn (1-2 giây) để thổi sạch bụi bám trong các kẽ cánh quạt và khe nhôm tản nhiệt.',
      'Dùng cọ mềm quét sạch các hạt bụi còn sót lại rơi xuống đáy thùng máy.',
    ],
    caution: 'TUYỆT ĐỐI KHÔNG để quạt quay tự do với tốc độ cao khi xịt khí nén vì sẽ làm hỏng bạc đạn quạt hoặc motor sinh điện áp ngược làm cháy chân cắm FAN trên bo mạch chủ.',
  },
  {
    id: 'cable_check',
    title: 'Kiểm tra tiếp xúc dây cáp và đầu cắm nguồn',
    period: 'quarterly',
    periodLabel: 'Định kỳ 3–6 tháng',
    difficulty: 'Trung bình',
    partId: 'cables',
    summary: 'Kiểm tra lại độ chắc chắn của các giắc cắm nguồn bo mạch 24-pin, nguồn CPU 8-pin và đặc biệt là cáp nguồn phụ card đồ họa.',
    importance: 'Chân cắm bị lỏng hoặc cắm chưa ngập hết ngàm có thể gây phóng tia lửa điện vi mô (arcing), làm chảy nhựa đầu cắm nguy hiểm.',
    steps: [
      'Rút điện máy tính trước khi chạm tay vào dây dẫn.',
      'Quan sát đầu cắm 12VHPWR (trên card RTX 40 series) hoặc 8-pin PCIe: đảm bảo ngàm khóa nhựa đã kẹp chặt và không có khe hở giữa hai đầu nối.',
      'Kiểm tra dây cáp quạt tản nhiệt có bị vướng hoặc chạm vào cánh quạt quay không.',
      'Dùng dây rút (zip-tie) gom gọn các bó dây lộn xộn để tạo khoảng trống thoáng mát cho luồng gió đối lưu.',
    ],
  },
  {
    id: 'driver_os_update',
    title: 'Cập nhật Driver Card đồ họa và Windows',
    period: 'quarterly',
    periodLabel: 'Định kỳ 3–6 tháng',
    difficulty: 'Dễ',
    partId: 'gpu',
    summary: 'Cập nhật driver đồ họa chính hãng (NVIDIA GeForce Game Ready / AMD Adrenalin) để sửa lỗi và tối ưu hiệu năng game mới.',
    importance: 'Driver cũ thường là thủ phạm gây crash game, lỗi sụt giảm khung hình và màn hình xanh (BSOD).',
    steps: [
      'Mở ứng dụng GeForce Experience / NVIDIA App hoặc AMD Software: Adrenalin Edition.',
      'Vào mục Drivers > nhấn "Check for Updates" > tải và cài đặt bản driver mới nhất.',
      'Khi cài đặt, nên chọn chế độ "Express" hoặc "Clean Installation" để xóa sạch cấu hình lỗi cũ.',
      'Mở Windows Update để kiểm tra các bản vá bảo mật quan trọng của hệ điều hành.',
    ],
  },

  // 1 - 2 Năm
  {
    id: 'repaste_cpu',
    title: 'Tra lại keo tản nhiệt CPU (Thermal Paste Repasting)',
    period: 'yearly',
    periodLabel: 'Định kỳ 1–2 năm',
    difficulty: 'Cẩn thận',
    partId: 'cooler',
    summary: 'Tháo tản nhiệt CPU, lau sạch lớp keo cũ đã khô cứng và bơm lớp keo dẫn nhiệt hiệu năng cao mới.',
    importance: 'Sau 1-2 năm sử dụng liên tục, keo tản nhiệt gốc sẽ bị khô, hóa bột và mất khả năng truyền nhiệt từ CPU sang tản nhiệt.',
    steps: [
      'Bật máy chạy tác vụ nhẹ khoảng 5-10 phút để CPU ấm lên, giúp keo tản nhiệt mềm ra dễ tháo hơn.',
      'Tắt nguồn, rút phích cắm điện AC. Tháo ốc cố định tản nhiệt CPU.',
      'XOAY NHẸ TẢN NHIỆT QUA LẠI SANG HAI BÊN trước khi nhấc lên (tuyệt đối không giật mạnh thẳng đứng, đặc biệt với CPU AMD).',
      'Dùng khăn mềm hoặc giấy ăn cao cấp thấm cồn Isopropyl 90%+ lau sạch hoàn toàn lớp keo cũ trên lưng CPU và mặt đáy tản nhiệt.',
      'Bơm một giọt keo mới cỡ hạt đậu vào chính giữa lưng CPU (khoảng 0.2 - 0.3 ml).',
      'Đặt tản nhiệt cân đối trở lại và siết ốc so le theo hình chữ X.',
    ],
    caution: 'Chỉ dùng cồn Isopropyl (IPA) 90% trở lên, tuyệt đối KHÔNG dùng nước thường hoặc cồn sát trùng y tế 70% chứa nhiều nước để lau CPU.',
  },
  {
    id: 'clean_ram_pcie',
    title: 'Vệ sinh chân tiếp xúc RAM và khe PCIe bằng cồn 90°',
    period: 'yearly',
    periodLabel: 'Định kỳ 1–2 năm',
    difficulty: 'Cẩn thận',
    partId: 'ram',
    summary: 'Tháo thanh RAM và card đồ họa, dùng gôm tẩy mềm hoặc cồn chuyên dụng lau sạch lớp màng oxy hóa trên hàng chân mạ vàng.',
    importance: 'Chân tiếp xúc bị oxy hóa hoặc bám màng ẩm là nguyên nhân số 1 gây hiện tượng quạt quay nhưng màn hình không lên hoặc màn hình xanh.',
    steps: [
      'Tắt máy, rút điện, gạt lẫy khóa khe cắm và nhẹ nhàng rút thanh RAM và card đồ họa ra.',
      'Cầm ở hai mép cạnh mạch PCB, tránh chạm tay trực tiếp vào hàng chân mạ vàng.',
      'Dùng một cục gôm (tẩy) chì mềm màu trắng chà nhẹ dọc theo hàng chân mạ vàng ở cả hai mặt cho đến khi sáng bóng.',
      'Dùng cọ mềm quét sạch vụn gôm rơi ra, hoặc tẩm một chút cồn Isopropyl lau lại một lượt.',
      'Cắm chặt thanh RAM vào khe cho đến khi nghe tiếng "Tách" dứt khoát của lẫy khóa.',
    ],
  },
  {
    id: 'ssd_health_check',
    title: 'Kiểm tra độ suy hao sức khỏe ổ cứng SSD (TBW & SMART)',
    period: 'yearly',
    periodLabel: 'Định kỳ 1–2 năm',
    difficulty: 'Dễ',
    partId: 'ssd',
    summary: 'Dùng công cụ CrystalDiskInfo để đọc chỉ số sức khỏe (Health Status) và tổng dung lượng đã ghi (Total Host Writes).',
    importance: 'Chủ động sao lưu dữ liệu quan trọng trước khi ổ cứng có dấu hiệu hỏng hóc hoặc xuất hiện bad sector.',
    steps: [
      'Tải phần mềm miễn phí CrystalDiskInfo Portable về máy.',
      'Mở ứng dụng và kiểm tra dòng "Health Status": nếu báo "Good 90% - 100%" (Màu xanh dương) là hoàn toàn khỏe mạnh.',
      'Nếu trạng thái báo "Caution" (Màu vàng) hoặc nhiệt độ ổ SSD thường xuyên trên 70°C, hãy sao lưu ngay tài liệu quan trọng sang ổ khác.',
    ],
  },
]

export interface DiagnosticIssue {
  id: string
  title: string
  severity: 'urgent' | 'warning' | 'info'
  symptoms: string
  cause: string
  targetPart?: PartId
  steps: {
    order: number
    title: string
    action: string
    note?: string
  }[]
  proTip: string
}

export const DIAGNOSTIC_ISSUES: DiagnosticIssue[] = [
  {
    id: 'no_power',
    title: 'Bấm nút nguồn máy hoàn toàn im lìm, không có đèn hay quạt quay',
    severity: 'urgent',
    symptoms: 'Nhấn nút Power trên vỏ case nhưng không có bất kỳ phản hồi nào, không có tiếng quạt quay, không đèn LED.',
    cause: 'Công tắc nguồn phía sau chưa bật, lỏng phích cắm AC, hỏng nút bấm vỏ case hoặc bộ nguồn (PSU) bị ngắt bảo vệ.',
    targetPart: 'psu',
    steps: [
      {
        order: 1,
        title: 'Kiểm tra công tắc I/O sau lưng bộ nguồn',
        action: 'Hãy nhìn phía sau lưng thùng máy: nút gạt nhỏ trên bộ nguồn phải được nhấn vào phía chữ "I" (Bật), không phải chữ "O" (Tắt).',
        note: 'Rất nhiều người mới thường quên bật công tắc này sau khi di chuyển máy.',
      },
      {
        order: 2,
        title: 'Kiểm tra phích cắm và ổ điện',
        action: 'Cắm lại dây nguồn AC thật chặt vào cả hai đầu (lưng nguồn và ổ cắm tường). Thử cắm sang một ổ cắm điện khác chắc chắn có điện.',
      },
      {
        order: 3,
        title: 'Kiểm tra dây nút nguồn F_PANEL (POWER SW)',
        action: 'Mở nắp hông case, nhìn vào góc dưới cùng bên phải của bo mạch chủ. Đảm bảo đầu cắm 2-pin nhỏ có chữ "POWER SW" đang được cắm chặt vào đúng 2 chân Power của cụm Front Panel.',
      },
      {
        order: 4,
        title: 'Thử kích nguồn bằng đầu tua vít (Loại trừ hỏng nút bấm)',
        action: 'Rút dây POWER SW ra, dùng đầu dẹp kim loại của tua vít chạm nhẹ nối tắt 2 chân cắm Power trên bo mạch trong 1 giây. Nếu máy khởi động ngay, chứng tỏ nút bấm trên vỏ case đã bị đứt dây hoặc hỏng.',
      },
    ],
    proTip: 'Nếu đã thử hết các bước mà bộ nguồn vẫn im lặng, có thể bộ nguồn đã bị kích hoạt chế độ bảo vệ ngắn mạch (Short-circuit protection) do chập điện, hãy rút điện 15 phút rồi thử lại.',
  },
  {
    id: 'no_display',
    title: 'Quạt quay, đèn sáng nhưng màn hình đen báo "No Signal"',
    severity: 'urgent',
    symptoms: 'Thùng máy chạy, quạt CPU và quạt vỏ case quay tít, đèn RGB sáng nhưng màn hình không lên hình, báo "No Signal" rồi tắt ngúm.',
    cause: 'Cắm nhầm dây màn hình vào cổng bo mạch chủ, chân RAM bị lỏng hoặc bám màng ẩm oxy hóa, card đồ họa chưa nhận diện.',
    targetPart: 'ram',
    steps: [
      {
        order: 1,
        title: 'CẮM LẠI CÁP MÀN HÌNH VÀO CỔNG CARD RỜI (Lỗi phổ biến nhất!)',
        action: 'Nếu máy tính có lắp card đồ họa rời (VGA), cáp HDMI/DisplayPort từ màn hình BẮT BUỘC PHẢI CẮM VÀO CỔNG NẰM NGANG PHÍA DƯỚI (của card rời), TUYỆT ĐỐI KHÔNG cắm vào cổng nằm dọc phía trên (của bo mạch chủ).',
        note: 'Nhiều CPU dòng đuôi F của Intel (ví dụ i5-13400F) không có chip đồ họa tích hợp, cắm vào bo mạch chủ sẽ vĩnh viễn không lên hình.',
      },
      {
        order: 2,
        title: 'Vệ sinh và cắm lại thanh RAM',
        action: 'Tắt máy, rút điện. Tháo các thanh RAM ra. Dùng gôm tẩy lau sạch hàng chân vàng. Cắm lại vào đúng khe ưu tiên (khe 2 và 4 tính từ CPU sang phải). Nhấn thật mạnh tay cho đến khi lẫy khóa tự kêu "Tách".',
      },
      {
        order: 3,
        title: 'Thử chạy với 1 thanh RAM duy nhất',
        action: 'Nếu máy có 2 thanh RAM, hãy chỉ cắm đúng 1 thanh vào khe số 2 rồi bật máy thử. Nếu lên hình, đổi thử sang thanh còn lại để tìm xem thanh nào bị lỗi.',
      },
      {
        order: 4,
        title: 'Tháo pin CMOS để Reset BIOS (Clear CMOS)',
        action: 'Rút điện máy tính. Tìm viên pin tròn CR2032 như đồng xu trên bo mạch chủ, tháo viên pin ra ngoài trong 5 phút rồi gắn lại. Việc này sẽ đưa toàn bộ thiết lập BIOS về mặc định an toàn.',
      },
    ],
    proTip: 'Đừng vội hoảng sợ mang ra tiệm sửa. 80% trường hợp màn hình đen trên máy tính tự ráp xuất phát từ việc cắm sai cổng màn hình hoặc RAM chưa ngập chân cắm.',
  },
  {
    id: 'debug_led',
    title: 'Đèn báo lỗi Debug LED trên bo mạch chủ sáng (CPU / DRAM / VGA / BOOT)',
    severity: 'warning',
    symptoms: 'Trên góc bo mạch chủ có cụm 4 đèn LED nhỏ và một trong số các đèn dừng lại ở màu đỏ hoặc vàng cam.',
    cause: 'Tính năng EZ Debug LED báo chính xác phần cứng nào đang gặp trục trặc trong quá trình tự kiểm tra khởi động (POST).',
    targetPart: 'motherboard',
    steps: [
      {
        order: 1,
        title: 'Đèn CPU (Đỏ): Trục trặc nguồn cấp CPU hoặc socket',
        action: 'Kiểm tra cáp nguồn 8-pin CPU ở góc trên cùng bên trái bo mạch chủ đã cắm chặt chưa. Nếu vừa thay CPU mới, có thể bo mạch chủ cần được cập nhật BIOS để hỗ trợ thế hệ CPU này.',
      },
      {
        order: 2,
        title: 'Đèn DRAM (Vàng hoặc Cam): Trục trặc bộ nhớ RAM',
        action: 'Lỏng chân RAM hoặc RAM không tương thích xung nhịp XMP. Tháo RAM ra vệ sinh chân tiếp xúc và cắm lại từng thanh một vào khe A2 (khe thứ 2).',
      },
      {
        order: 3,
        title: 'Đèn VGA (Trắng): Trục trặc nhận diện Card đồ họa',
        action: 'Kiểm tra cáp nguồn phụ PCIe của card đồ họa. Đảm bảo card đồ họa đã được cắm ngập vào khe PCIe x16 và màn hình đã được bật nguồn.',
      },
      {
        order: 4,
        title: 'Đèn BOOT (Xanh lá): Không tìm thấy ổ đĩa khởi động',
        action: 'Phần cứng đã kiểm tra xong hoàn toàn tốt, nhưng bo mạch chủ chưa tìm thấy phân vùng cài Windows trên ổ SSD, hoặc ổ SSD M.2 bị lỏng chân cắm.',
      },
    ],
    proTip: 'Đèn Debug LED là công cụ chẩn đoán chuẩn xác nhất của thợ máy tính. Hãy nhìn kỹ chữ in cạnh chiếc đèn đang sáng để xử lý đúng trọng tâm.',
  },
  {
    id: 'shutdown_gaming',
    title: 'Máy tự tắt phụt hoặc khởi động lại đột ngột khi chơi game / làm việc nặng',
    severity: 'urgent',
    symptoms: 'Khi lướt web, nghe nhạc thì máy chạy bình thường, nhưng hễ vào game 3D nặng hoặc render video là máy tắt phụt tối đen hoặc khởi động lại ngay.',
    cause: 'CPU hoặc GPU bị quá nhiệt vượt ngưỡng an toàn (Thermal Shutdown), hoặc bộ nguồn (PSU) bị sụt áp, quá tải công suất khi card đồ họa ăn điện đỉnh (Power Spike).',
    targetPart: 'psu',
    steps: [
      {
        order: 1,
        title: 'Kiểm tra nhiệt độ khi vừa vào game',
        action: 'Bật HWMonitor quan sát nhiệt độ. Nếu nhiệt độ CPU vọt thẳng lên 100°C chỉ sau 1-2 phút, tản nhiệt đã bị khô keo, chết bơm tản nước AIO, hoặc quên bóc seal nilon mặt đế đồng.',
      },
      {
        order: 2,
        title: 'Kiểm tra tiếng quay của quạt nguồn',
        action: 'Khi máy tắt phụt, hãy sờ tay vào vỏ sau của bộ nguồn: nếu bộ nguồn rất nóng hoặc tỏa mùi khét, quạt nguồn có thể đã bị kẹt bụi hoặc nguồn bị quá tải.',
      },
      {
        order: 3,
        title: 'Tính lại công suất bộ nguồn',
        action: 'Card đồ họa đời mới (RTX 30/40 series) thường có những cú nhảy điện tức thời (Transient Spike) gấp 1.5 - 2 lần công suất danh định. Nếu bạn đang dùng nguồn công suất thấp, nguồn sẽ tự nhảy mạch ngắt để bảo vệ.',
      },
    ],
    proTip: 'Nếu máy tính tắt phụt ngay lập tức mà không hiện màn hình xanh BSOD, 90% nguyên nhân là do BỘ NGUỒN hoặc QUÁ NHIỆT ngắt bảo vệ phần cứng.',
  },
  {
    id: 'bsod_crash',
    title: 'Máy bị Màn hình xanh chết chóc (Blue Screen of Death - BSOD)',
    severity: 'warning',
    symptoms: 'Đang sử dụng thì màn hình chuyển sang màu xanh dương kèm mã lỗi như `MEMORY_MANAGEMENT`, `IRQL_NOT_LESS_OR_EQUAL` hoặc `WHEA_UNCORRECTABLE_ERROR`.',
    cause: 'Xung đột driver, ép xung RAM (XMP) không ổn định, lỗi file hệ thống Windows hoặc ổ SSD bị suy hao.',
    targetPart: 'ram',
    steps: [
      {
        order: 1,
        title: 'Tạm thời tắt cấu hình XMP trong BIOS',
        action: 'Khởi động máy nhấn phím Del vào BIOS, tìm mục XMP / EXPO và chuyển về "Disabled". Nếu sau khi tắt XMP máy chạy ổn định không còn bị màn hình xanh, chứng tỏ bus RAM đang bị xung đột với vi điều khiển IMC của CPU.',
      },
      {
        order: 2,
        title: 'Quét và sửa lỗi file hệ điều hành Windows',
        action: 'Nhấn nút Start > gõ "cmd" > chuột phải chọn "Run as administrator" > gõ lệnh `sfc /scannow` rồi nhấn Enter để Windows tự tìm và vá file hỏng.',
      },
      {
        order: 3,
        title: 'Cài lại driver card đồ họa sạch bằng DDU',
        action: 'Tải công cụ DDU (Display Driver Uninstaller) vào chế độ Safe Mode gỡ sạch driver màn hình cũ, sau đó tải bản driver mới nhất từ trang chủ cài đặt lại.',
      },
    ],
    proTip: 'Hãy ghi lại hoặc chụp ảnh dòng mã lỗi chữ in hoa ở góc dưới màn hình xanh (Stop Code). Mã lỗi này sẽ chỉ đích danh linh kiện hay driver nào đang phát sinh lỗi.',
  },
  {
    id: 'loud_noise',
    title: 'Nhiệt độ quá nóng (> 90°C), quạt hú ầm ĩ như động cơ máy bay',
    severity: 'warning',
    symptoms: 'Thùng máy tỏa hơi nóng hầm hập qua khe kính, quạt tản nhiệt quay 100% công suất kêu vù vù liên tục dù không làm gì nặng.',
    cause: 'Lớp keo tản nhiệt bị khô, quạt case lắp sai hướng tạo luồng gió quẩn, hoặc bơm tản nước AIO bị bọt khí/hỏng motor.',
    targetPart: 'cooler',
    steps: [
      {
        order: 1,
        title: 'Kiểm tra hướng luồng gió của các quạt (Airflow)',
        action: 'Mặt có thanh nan đỡ motor là mặt THỔI GIÓ RA. Đảm bảo quạt mặt trước là HÚT GIÓ VÀO (Intake), quạt phía sau và nóc là THỔI GIÓ RA (Exhaust). Nếu tất cả các quạt đều thổi ra hoặc đều hút vào, hơi nóng sẽ bị ứ đọng.',
      },
      {
        order: 2,
        title: 'Kiểm tra và tra lại keo tản nhiệt CPU',
        action: 'Nếu đã hơn 1 năm chưa thay keo, hãy tháo tản nhiệt CPU ra vệ sinh và tra lại keo tản nhiệt xịn (độ dẫn nhiệt > 8.5 W/mK như MX-4, NT-H1).',
      },
      {
        order: 3,
        title: 'Kiểm tra bơm tản nhiệt nước AIO (nếu dùng tản nước)',
        action: 'Đặt ngón tay nhẹ lên hai ống nước hoặc mặt block CPU khi máy đang bật: bạn phải cảm nhận được độ rung nhẹ êm ái của dòng nước lưu chuyển. Nếu block CPU nóng ran mà ống nước mát lạnh, motor bơm đã bị hỏng.',
      },
    ],
    proTip: 'Để case máy tính cách tường ít nhất 10–15 cm và không đặt thùng máy trực tiếp trên thảm lông dày để nguồn ở đáy có thể lấy gió mát dễ dàng.',
  },
]

export interface MaintenanceTool {
  id: string
  name: string
  spec: string
  purpose: string
  tip: string
}

export const MAINTENANCE_TOOLS: MaintenanceTool[] = [
  {
    id: 'screwdriver',
    name: 'Tua vít 4 cạnh PH2 từ tính',
    spec: 'Đầu vít Phillips cỡ PH2, thân dài 100–150 mm',
    purpose: 'Mở được 95% các loại ốc trong vỏ case máy tính: ốc nắp hông, ốc nguồn, ốc quạt, ốc tản nhiệt và ốc card đồ họa.',
    tip: 'Nên chọn loại có đầu hít nam châm (từ tính) để tránh làm rơi ốc lọt vào các khe hẹp dưới bo mạch chủ.',
  },
  {
    id: 'air_blower',
    name: 'Bóng thổi bụi cao su / Bình xịt khí nén',
    spec: 'Áp lực khí vừa phải, không sinh tĩnh điện',
    purpose: 'Thổi bay các hạt bụi bám sâu trong các khe nhôm tản nhiệt, khe cắm RAM và chân socket mà tay không chạm tới được.',
    tip: 'Xịt theo từng nhịp dứt khoát 1–2 giây. Không dốc ngược bình khí nén vì sẽ phun ra khí hóa lỏng cực lạnh làm đọng sương.',
  },
  {
    id: 'brush',
    name: 'Cọ quét mềm chống tĩnh điện (ESD Brush)',
    spec: 'Sợi lông nhân tạo siêu mềm, cán nhựa dẫn điện tĩnh',
    purpose: 'Quét sạch màng bụi bám dính trên cánh quạt, lưng card đồ họa và các tụ điện trên bo mạch chủ.',
    tip: 'Không dùng chổi sơn lông cứng hoặc chổi nhựa thông thường vì dễ làm bung các linh kiện dán bề mặt (SMD).',
  },
  {
    id: 'thermal_paste',
    name: 'Keo tản nhiệt hiệu năng cao (Thermal Paste)',
    spec: 'Độ dẫn nhiệt > 8.5 W/mK (như Arctic MX-4, Noctua NT-H1)',
    purpose: 'Lấp đầy các khe hở siêu vi mô giữa nắp lưng kim loại của CPU và đế đồng tản nhiệt, giúp truyền nhiệt tối đa.',
    tip: 'Chỉ cần một lượng bằng hạt đậu xanh (khoảng 0.2ml). Tra quá nhiều keo sẽ bị tràn ra mạch ngoài gây bẩn.',
  },
  {
    id: 'alcohol_ipa',
    name: 'Cồn Isopropyl Alcohol (IPA) 90% trở lên',
    spec: 'Độ tinh khiết cao, bay hơi nhanh trong 10 giây',
    purpose: 'Lau sạch keo tản nhiệt cũ và màng oxy hóa trên chân tiếp xúc mạ vàng của RAM và card đồ họa.',
    tip: 'TUYỆT ĐỐI KHÔNG dùng cồn y tế 70% thông thường vì chứa 30% nước, nước đọng lại trong khe mạch sẽ làm chập cháy khi bật máy.',
  },
  {
    id: 'microfiber_cloth',
    name: 'Khăn vải sợi Microfiber không xơ',
    spec: 'Chất liệu sợi siêu nhỏ, mềm mịn, không rụng sợi',
    purpose: 'Lau sạch mặt lưng CPU, kính cường lực hông case và các bề mặt kim loại sáng bóng.',
    tip: 'Không dùng giấy vệ sinh thông thường vì vụn giấy và xơ gỗ sẽ bám đầy vào chân socket.',
  },
]

export const SAFETY_RULES: string[] = [
  'LUÔN RÚT HOÀN TOÀN DÂY NGUỒN AC và nhấn giữ nút nguồn trên vỏ case 5 giây để xả hết điện tích dư trong tụ trước khi chạm vào linh kiện.',
  'KHỬ TĨNH ĐIỆN CHO BẢN THÂN: Chạm tay vào phần khung kim loại không sơn của thùng máy, hoặc đeo vòng chống tĩnh điện trước khi cầm vào bo mạch.',
  'CỐ ĐỊNH CÁNH QUẠT KHI XỊT BỤI: Dùng ngón tay hoặc tăm giữ cánh quạt không quay tít khi xịt khí nén, tránh làm hỏng motor bạc đạn và điện áp ngược.',
  'KHÔNG BAO GIỜ DÙNG MÁY HÚT BỤI GIA ĐÌNH CÔNG SUẤT LỚN: Đầu hút nhựa cọ xát với không khí tạo ra luồng tĩnh điện hàng nghìn vôn làm chết chip vi mạch trong nháy mắt.',
  'CẦM LINH KIỆN Ở CÁC MÉP BÌA MẠCH: Tránh chạm trực tiếp ngón tay dính mồ hôi hoặc dầu mỡ vào các chân tiếp xúc mạ vàng hay mặt chip trần.',
]
