import type { PartId } from './parts'
import type { FamilyId } from './families'

export interface UpgradeCategory {
  id: 'ram' | 'ssd' | 'gpu' | 'cpu' | 'psu' | 'cooler'
  partId: PartId
  name: string
  shortDesc: string
  symptoms: string[] // Dấu hiệu khi nào cần nâng cấp
  benefits: string[]
  keySpecs: string[] // Thông số cần để ý
  mistakes: string[] // Sai lầm người mới hay gặp
  steps: {
    title: string
    detail: string
    tip?: string
  }[]
  postInstall: string[]
}

export const UPGRADE_CATEGORIES: UpgradeCategory[] = [
  {
    id: 'ram',
    partId: 'ram',
    name: 'Nâng cấp RAM',
    shortDesc: 'Tăng dung lượng bộ nhớ tạm giúp mở nhiều ứng dụng, duyệt hàng chục tab trình duyệt mượt mà.',
    symptoms: [
      'Máy báo tràn RAM hoặc giật lag khi mở nhiều tab trình duyệt cùng lúc',
      'Chơi game hay làm việc đồ họa bị khựng hình (stuttering) do Windows phải dùng bộ nhớ ảo (Pagefile)',
      'Dung lượng RAM hiện tại chỉ có 8 GB hoặc đang chạy kênh đơn (Single Channel)',
    ],
    benefits: [
      'Đa nhiệm mượt mà, chuyển đổi giữa các ứng dụng tức thì',
      'Kích hoạt Dual-Channel giúp tăng 10–20% hiệu năng chơi game và xử lý đồ họa',
      'Tránh hiện tượng tràn RAM gây văng ứng dụng đột ngột',
    ],
    keySpecs: [
      'Chuẩn RAM: DDR4 hay DDR5 (không thể cắm lẫn vào nhau)',
      'Số khe cắm còn trống trên bo mạch chủ (2 khe hay 4 khe)',
      'Tốc độ Bus (MHz) và độ trễ (CL): nên chọn cùng bus hoặc cùng model với thanh hiện có',
      'Giới hạn dung lượng tối đa của bo mạch chủ và CPU hỗ trợ',
    ],
    mistakes: [
      'Mua RAM DDR5 cắm vào bo mạch chủ hỗ trợ DDR4 (hoặc ngược lại)',
      'Cắm sai khe ưu tiên: nếu bo mạch có 4 khe và dùng 2 thanh, phải cắm vào khe 2 và 4 (khe A2 và B2)',
      'Quên bật tính năng XMP (Intel) hoặc EXPO/DOCP (AMD) trong BIOS khiến RAM chỉ chạy ở xung nhịp gốc thấp',
    ],
    steps: [
      {
        title: 'Bước 1: Tắt nguồn và khử tĩnh điện',
        detail: 'Tắt máy, rút hoàn toàn phích cắm nguồn điện AC. Nhấn giữ nút nguồn trên vỏ case 5 giây để xả hết điện tích dư trong tụ.',
        tip: 'Chạm tay vào phần khung kim loại không sơn của vỏ case để xả tĩnh điện khỏi cơ thể.',
      },
      {
        title: 'Bước 2: Mở nắp hông và xác định khe cắm',
        detail: 'Mở nắp kính hoặc nắp hông case. Tìm khu vực khe RAM bên phải socket CPU trên bo mạch chủ. Gạt lẫy nhựa ở đầu khe cắm ra ngoài.',
      },
      {
        title: 'Bước 3: So khớp rãnh khuyết (khấc khoá)',
        detail: 'Hàng chân RAM có một rãnh khuyết lệch tâm. Hãy so khớp rãnh này với gờ nổi bên trong khe cắm trên bo mạch chủ. Nếu không khớp chiều, hãy quay ngược thanh RAM lại.',
        tip: 'Tuyệt đối không cố ấn nếu chưa đúng chiều vì sẽ làm gãy rãnh khe cắm.',
      },
      {
        title: 'Bước 4: Ấn đều hai đầu để cố định',
        detail: 'Dùng hai ngón tay cái ấn đều và dứt khoát vào hai đầu thanh RAM cho đến khi nghe tiếng "Tách" và lẫy khóa tự động gập vào ôm chặt thanh RAM.',
      },
    ],
    postInstall: [
      'Bật máy và vào BIOS (nhấn liên tục phím Del hoặc F2 lúc vừa khởi động).',
      'Tìm mục XMP (Extreme Memory Profile) hoặc EXPO / DOCP và chuyển sang "Profile 1" để RAM chạy đúng tốc độ định danh.',
      'Khởi động vào Windows, nhấn Ctrl + Shift + Esc mở Task Manager > tab Performance > Memory để kiểm tra dung lượng và tốc độ Speed (MHz).',
    ],
  },
  {
    id: 'ssd',
    partId: 'ssd',
    name: 'Nâng cấp Ổ cứng SSD',
    shortDesc: 'Thay thế HDD cũ hoặc bổ sung thêm SSD M.2 NVMe tốc độ cao để khởi động Windows và tải game siêu tốc.',
    symptoms: [
      'Ổ đĩa C (hệ điều hành) báo đỏ, sắp hết dung lượng trống',
      'Thời gian khởi động máy tính hoặc tải màn chơi game quá lâu (> 1-2 phút)',
      'Máy vẫn đang chạy hệ điều hành trên ổ cứng cơ HDD hoặc SSD SATA đời cũ',
    ],
    benefits: [
      'Tốc độ đọc/ghi nhanh gấp 5–15 lần so với SSD SATA và gấp 30–50 lần so với HDD cơ truyền thống',
      'Khởi động Windows trong vòng 5–10 giây, mở ứng dụng nặng tức thì',
      'Thêm không gian lưu trữ game nặng, video 4K và tài liệu học tập',
    ],
    keySpecs: [
      'Chuẩn kích thước: M.2 2280 (chiều rộng 22mm, dài 80mm - chuẩn phổ biến nhất)',
      'Giao thức: NVMe PCIe (Gen 3, Gen 4 hay Gen 5) hoặc M.2 SATA',
      'Số khe M.2 còn trống trên bo mạch chủ và vị trí gắn miếng tản nhiệt heatsink',
      'Độ bền đọc ghi (TBW) và công nghệ chip nhớ (TLC, QLC)',
    ],
    mistakes: [
      'Quên bóc lớp nilon bảo vệ màu xanh hoặc trong suốt trên miếng thermal pad của tản nhiệt M.2',
      'Lắp nhầm ốc cố định hoặc siết ốc M.2 quá chặt làm cong vênh mạch PCB của SSD',
      'Mua ổ SSD chuẩn M.2 PCIe Gen 5 cắm vào bo mạch chỉ hỗ trợ Gen 3 (vẫn chạy được nhưng bị bóp nghẹt băng thông)',
    ],
    steps: [
      {
        title: 'Bước 1: Chuẩn bị vị trí khe M.2',
        detail: 'Tắt nguồn, rút điện. Xác định khe M.2 trên bo mạch chủ. Nếu khe có sẵn tấm tản nhiệt kim loại (heatsink), hãy dùng tua vít PH2 tháo ốc giữ miếng tản ra.',
      },
      {
        title: 'Bước 2: Cắm SSD vào khe theo góc nghiêng',
        detail: 'Cầm hai cạnh của thanh SSD, đưa phần chân cắm mạ vàng vào khe cắm M.2 theo góc nghiêng khoảng 30 độ so với mặt bo mạch cho đến khi chân ngập hết vào khe.',
        tip: 'Khi cắm đúng, thanh SSD sẽ tự nâng nhẹ ở góc 30 độ.',
      },
      {
        title: 'Bước 3: Đè nhẹ và cố định',
        detail: 'Dùng một ngón tay ấn nhẹ phần đuôi SSD xuống sát chân ốc đệm (standoff) hoặc chốt xoay EZ-Latch. Vặn ốc cố định vừa tay.',
      },
      {
        title: 'Bước 4: Lắp tản nhiệt (nếu có)',
        detail: 'Bóc lớp nilon bảo vệ trên miếng dán tản nhiệt của nắp che, đặt lại vị trí và vặn chặt ốc giữ.',
        tip: 'Đừng quên bóc nilon bảo vệ kẻo miếng dán không truyền được nhiệt sang heatsink kim loại.',
      },
    ],
    postInstall: [
      'Khởi động vào Windows, click chuột phải vào nút Start > chọn Disk Management.',
      'Nếu là ổ mới tinh, Windows sẽ hiện thông báo "Initialize Disk" > chọn chuẩn GPT > nhấn OK.',
      'Click chuột phải vào vùng "Unallocated" màu đen của ổ mới > chọn New Simple Volume > nhấn Next để tạo phân vùng ổ đĩa.',
    ],
  },
  {
    id: 'gpu',
    partId: 'gpu',
    name: 'Nâng cấp Card đồ họa (GPU)',
    shortDesc: 'Yếu tố quan trọng nhất quyết định hiệu năng chơi game 3D, dựng video 4K/8K và chạy mô hình AI.',
    symptoms: [
      'Chơi game bị tụt FPS, giật lag hoặc phải hạ đồ họa xuống mức thấp nhất',
      'Đang sử dụng card đồ họa tích hợp trên CPU (Intel UHD / Radeon Graphics)',
      'Render video hoặc làm hiệu ứng 3D mất quá nhiều thời gian',
    ],
    benefits: [
      'Tăng vọt số khung hình trên giây (FPS), chơi mượt mà ở độ phân giải 1080p, 2K hoặc 4K',
      'Hỗ trợ các công nghệ hiện đại như Ray Tracing chân thực, DLSS / FSR nâng khung hình',
      'Tăng tốc render phần cứng qua GPU Encoder (NVENC, AV1)',
    ],
    keySpecs: [
      'Chiều dài card đồ họa (mm): phải nhỏ hơn khoảng trống tối đa của vỏ case',
      'Công suất nguồn khuyến nghị (PSU Wattage): thường cần từ 550W – 750W trở lên',
      'Chân nguồn phụ: cáp 8-pin PCIe truyền thống hay cáp chuẩn mới 12VHPWR 16-pin',
      'Số khe mở rộng (Slot thickness): card chiếm 2 slot, 2.5 slot hay 3 slot trên nẹp case',
    ],
    mistakes: [
      'Mua card quá dài so với vỏ case khiến không thể lắp vừa hoặc cấn quạt tản trước',
      'Nguồn hiện tại quá yếu (ví dụ nguồn 400W cố gánh card RTX 4070) dẫn đến sập nguồn khi tải nặng',
      'Sau khi lắp xong lại cắm dây màn hình (HDMI/DisplayPort) vào cổng bo mạch chủ thay vì cắm vào cổng card rời',
    ],
    steps: [
      {
        title: 'Bước 1: Tháo card cũ và nẹp chắn khe PCI',
        detail: 'Tắt nguồn, rút phích cắm. Rút các dây cáp màn hình đằng sau. Bóp ngàm rút cáp nguồn phụ PCIe ra khỏi card. Vặn ốc giữ nẹp kim loại sau case.',
      },
      {
        title: 'Bước 2: Mở lẫy khóa khe PCIe',
        detail: 'Ở cuối khe cắm PCIe x16 trên bo mạch chủ có một lẫy khóa nhựa. Dùng ngón tay ấn nhẹ lẫy này xuống để mở khóa. Rút card cũ thẳng đứng lên.',
        tip: 'Nếu bị kẹt, kiểm tra xem lẫy khóa đã mở hoàn toàn chưa, không được giật mạnh.',
      },
      {
        title: 'Bước 3: Lắp card đồ họa mới',
        detail: 'Căn chỉnh khe cắm PCIe và các nẹp phía sau case. Đặt card thẳng góc với khe và ấn đều tay xuống cho đến khi lẫy khóa tự động gập vào kêu "Tách".',
      },
      {
        title: 'Bước 4: Bắt ốc và cắm nguồn phụ',
        detail: 'Vặn chặt ốc nẹp kim loại vào case để card không bị xệ. Cắm đầy đủ các đầu cáp nguồn phụ PCIe hoặc 12VHPWR cho đến khi ngàm khóa ăn khớp hoàn toàn.',
      },
    ],
    postInstall: [
      'CẮM DÂY CÁP MÀN HÌNH (HDMI / DisplayPort) VÀO CỔNG CỦA CARD MỚI (nằm ngang ở phía dưới), TUYỆT ĐỐI KHÔNG cắm vào cổng đứng trên bo mạch chủ.',
      'Khởi động vào Windows, tải phần mềm DDU (Display Driver Uninstaller) để gỡ sạch driver cũ nếu đổi hãng (NVIDIA sang AMD hoặc ngược lại).',
      'Tải và cài đặt bản driver chính thức mới nhất từ trang chủ của nhà sản xuất.',
    ],
  },
  {
    id: 'cpu',
    partId: 'cpu',
    name: 'Nâng cấp Vi xử lý (CPU)',
    shortDesc: 'Trái tim của hệ thống: nâng cao tốc độ tính toán, xử lý logic game, nén file và chạy các tác vụ nặng.',
    symptoms: [
      'CPU luôn hoạt động ở mức 90–100% trong Task Manager khi chơi game hoặc làm việc',
      'Hiện tượng nghẽn cổ chai (CPU bottleneck): Card đồ họa mạnh nhưng CPU yếu kéo theo FPS không cao và trồi sụt',
      'Xử lý tác vụ tính toán, biên dịch mã nguồn (compile), render đồ họa chậm chạp',
    ],
    benefits: [
      'Tăng số nhân / số luồng xử lý, tăng tốc độ xung nhịp đơn nhân và đa nhân',
      'Loại bỏ triệt để điểm nghẽn hiệu năng với card đồ họa cao cấp',
      'Trải nghiệm hệ thống mượt mà, phản hồi ngay lập tức',
    ],
    keySpecs: [
      'Chuẩn chân cắm (Socket): CPU mới phải cùng socket với bo mạch chủ (ví dụ LGA1700 cho Intel Gen 12-14, AM5 cho AMD Ryzen 7000/9000)',
      'Khả năng cấp điện của dàn VRM bo mạch chủ (bo giá rẻ H610/A620 không nên gánh CPU dòng i7/i9 hoặc Ryzen 9)',
      'Công suất tản nhiệt (TDP): CPU mới có nóng hơn CPU cũ nhiều không, tản nhiệt hiện tại có đủ gánh không',
      'Phiên bản BIOS của bo mạch chủ đã hỗ trợ CPU mới chưa',
    ],
    mistakes: [
      'Chưa cập nhật BIOS trước khi tháo CPU cũ ra: bo mạch chủ không nhận diện được CPU đời mới hơn',
      'Lắp ngược chiều tam giác vàng làm cong hoặc gãy các chân tiếp xúc mạ vàng siêu mỏng trong socket',
      'Dùng tản nhiệt cũ giá rẻ cho CPU hiệu năng cao khiến CPU nóng 100°C và bị tụt xung nhịp (thermal throttle)',
    ],
    steps: [
      {
        title: 'Bước 1: Cập nhật BIOS bo mạch chủ (Quan trọng)',
        detail: 'Trước khi tháo CPU cũ, hãy vào trang chủ bo mạch tải bản BIOS mới nhất về USB và tiến hành cập nhật BIOS trong UEFI. Nếu không làm bước này, bo mạch có thể không khởi động được với CPU mới.',
      },
      {
        title: 'Bước 2: Tháo tản nhiệt cũ an toàn',
        detail: 'Tháo các ốc hoặc ngàm của tản nhiệt. Với CPU AMD, hãy bật máy chạy ấm vài phút trước khi tháo, sau đó xoay nhẹ tản nhiệt sang hai bên trước khi nhấc lên để tránh keo dính chặt giật bay cả CPU.',
      },
      {
        title: 'Bước 3: Lau sạch keo và mở socket',
        detail: 'Dùng khăn microfiber tẩm cồn Isopropyl 90% lau sạch lớp keo cũ. Gạt cần khóa socket sang bên và nhấc nắp giữ kim loại lên.',
      },
      {
        title: 'Bước 4: Đặt CPU mới đúng chiều',
        detail: 'Tìm ký hiệu hình tam giác vàng nhỏ ở góc CPU và căn khớp với dấu tam giác trên góc socket. Thả nhẹ CPU thẳng đứng vào lòng socket. KHÔNG được ấn hay đè mạnh.',
        tip: 'Khi hạ cần gạt khóa xuống sẽ cảm thấy lực tì khá chặt, đây là hiện tượng bình thường.',
      },
      {
        title: 'Bước 5: Tra keo tản nhiệt và lắp lại tản',
        detail: 'Bơm một lượng keo tản nhiệt cỡ hạt đậu vào chính giữa lưng CPU, sau đó gắn tản nhiệt trở lại và siết ốc đối xứng theo đường chéo.',
      },
    ],
    postInstall: [
      'Bật máy, lần đầu khởi động sau khi đổi CPU có thể mất 30–60 giây để bo mạch huấn luyện bộ nhớ (Memory Training).',
      'Vào BIOS kiểm tra tên CPU mới đã nhận đúng chưa, bật lại XMP/EXPO cho RAM.',
      'Tải phần mềm HWMonitor hoặc Cinebench để kiểm tra nhiệt độ dưới tải nặng.',
    ],
  },
  {
    id: 'psu',
    partId: 'psu',
    name: 'Nâng cấp Nguồn (PSU)',
    shortDesc: 'Bảo vệ toàn bộ dàn máy với dòng điện sạch, công suất dồi dào và độ bền chuẩn 80 Plus.',
    symptoms: [
      'Máy tính bị sập nguồn tối om hoặc khởi động lại đột ngột khi vừa vào game nặng',
      'Bộ nguồn cũ kêu tiếng rè rè, quạt nguồn hú to hoặc tỏa mùi khét nhẹ',
      'Nguồn hiện tại công suất thấp (350W–450W) hoặc không đủ dây nguồn PCIe để nâng cấp card đồ họa mới',
    ],
    benefits: [
      'Đảm bảo điện áp luôn ổn định (+12V, +5V, +3.3V) giúp kéo dài tuổi thọ mọi linh kiện',
      'Cung cấp đủ đầu cắm PCIe / 12VHPWR cho các thế hệ card đồ họa hiện đại',
      'Tiết kiệm điện năng và giảm tỏa nhiệt nhờ chứng nhận hiệu suất 80 Plus Bronze / Gold',
    ],
    keySpecs: [
      'Kích cỡ nguồn (Form Factor): ATX tiêu chuẩn hay SFX siêu nhỏ (dành cho case Mini ITX)',
      'Công suất thực (Wattage): 550W, 650W, 750W, 850W hay 1000W',
      'Cáp nguồn: Non-modular (dây liền), Semi-modular (tháo rời một phần) hay Full-modular (tháo rời 100%)',
      'Chuẩn ATX 3.0 / PCIe 5.0: có sẵn cáp 12VHPWR chịu tải tức thời gấp 2 lần',
    ],
    mistakes: [
      'Dùng nguồn noname, công suất ảo: khi quá tải không tự ngắt mà phát nổ làm cháy lan mainboard và card đồ họa',
      'Với nguồn Full Modular: dùng cáp của hãng này cắm vào nguồn của hãng khác (sơ đồ chân cắm nguồn khác nhau gây chập cháy ngay lập tức)',
      'Mua nguồn chuẩn ATX to lắp vào case Mini SFF chỉ hỗ trợ chuẩn SFX',
    ],
    steps: [
      {
        title: 'Bước 1: Rút toàn bộ dây nguồn cũ',
        detail: 'Rút dây nguồn 24-pin bo mạch, 8-pin CPU ở góc trên bo mạch, dây PCIe card đồ họa và dây SATA ổ cứng.',
      },
      {
        title: 'Bước 2: Tháo ốc bắt nguồn',
        detail: 'Vặn 4 ốc giữ nguồn ở phía sau lưng case. Nhẹ nhàng rút bộ nguồn cũ và mớ dây ra khỏi khoang chứa.',
      },
      {
        title: 'Bước 3: Lắp nguồn mới vào vị trí',
        detail: 'Đưa nguồn mới vào khoang chứa. Đặt quạt hút gió của nguồn hướng xuống đáy case (nếu case có lưới lọc bụi đáy) để lấy gió mát từ ngoài vào.',
      },
      {
        title: 'Bước 4: Cắm chắc các đầu dây nguồn',
        detail: 'Cắm dây 24-pin, 8-pin CPU, PCIe GPU. Hãy nghe tiếng "Tách" của ngàm khóa để đảm bảo chân cắm ngập hoàn toàn, tránh chập chân tiếp xúc do lỏng lẻo.',
      },
    ],
    postInstall: [
      'Kiểm tra lại công tắc I/O sau lưng nguồn: gạt sang vị trí "I" (Bật).',
      'Bật máy kiểm tra quạt nguồn có quay hoặc chế độ Zero RPM (quạt chỉ quay khi tải nặng).',
    ],
  },
  {
    id: 'cooler',
    partId: 'cooler',
    name: 'Nâng cấp Tản nhiệt & Quạt',
    shortDesc: 'Giúp CPU luôn mát mẻ dưới 70°C, máy chạy êm ái, kéo dài tuổi thọ linh kiện và tối ưu luồng gió.',
    symptoms: [
      'Nhiệt độ CPU khi chơi game hoặc render vượt quá 85–90°C',
      'Quạt tản nhiệt kêu to như động cơ phản lực do phải quay 100% tốc độ liên tục',
      'Đang sử dụng tản nhiệt gốc (Stock cooler) bằng nhôm bé đi kèm CPU',
    ],
    benefits: [
      'Hạ nhiệt độ CPU từ 15–30°C so với tản nhiệt gốc',
      'CPU duy trì được xung nhịp Turbo tối đa ổn định, không bị giảm hiệu năng',
      'Không gian máy tính yên tĩnh, thẩm mỹ đẹp mắt với quạt êm và LED ARGB',
    ],
    keySpecs: [
      'Loại tản: Tản khí dạng tháp (Tower Air Cooler) hay Tản nước AIO (Radiator 240mm / 360mm)',
      'Chiều cao tối đa tản khí (mm): phải thấp hơn khoảng trống tản nhiệt CPU của case',
      'Vị trí gắn két nước AIO: case có lắp vừa két 240mm ở nóc hoặc 360mm ở mặt trước không',
      'Hướng luồng gió: Quạt trước hút vào (Intake), quạt nóc và quạt sau thổi ra (Exhaust)',
    ],
    mistakes: [
      'Quên bóc miếng nilon bảo vệ đế đồng tản nhiệt (lỗi phổ biến khiến CPU lập tức vọt lên 100°C)',
      'Lắp ngược chiều quạt khiến các quạt thổi gió xung đột nhau, tạo luồng khí quẩn trong thùng máy',
      'Cắm nhầm dây quạt tản nhiệt vào cổng `SYS_FAN` thay vì cổng `CPU_FAN`',
    ],
    steps: [
      {
        title: 'Bước 1: Chuẩn bị ngàm bắt mặt sau bo mạch (Backplate)',
        detail: 'Tháo tản cũ, gắn backplate đi kèm của tản nhiệt mới vào mặt sau bo mạch chủ theo đúng socket Intel hoặc AMD.',
      },
      {
        title: 'Bước 2: BÓC MIẾNG NILON ĐẾ ĐỒNG (Cực kỳ quan trọng)',
        detail: 'Mặt đế đồng tiếp xúc với CPU luôn có miếng nilon cảnh báo "Warning: Peel off before use". Hãy bóc sạch miếng này trước khi lắp.',
      },
      {
        title: 'Bước 3: Tra keo tản nhiệt mới',
        detail: 'Lau sạch keo cũ trên mặt lưng CPU. Bơm một lớp keo tản nhiệt mới cỡ hạt đậu xanh vào chính giữa.',
      },
      {
        title: 'Bước 4: Siết ốc cố định và cắm dây CPU_FAN',
        detail: 'Đặt tản nhiệt cân đối lên mặt CPU. Siết ốc đối xứng từng vòng một theo hình chữ X. Cắm dây quạt vào chân cắm có chữ `CPU_FAN` trên bo mạch chủ.',
      },
    ],
    postInstall: [
      'Vào BIOS kiểm tra mục Hardware Monitor: tốc độ vòng quay CPU Fan có báo RPM chuẩn không.',
      'Chạy thử ứng dụng nặng để quan sát nhiệt độ tối đa không vượt quá 75–80°C.',
    ],
  },
]

/** Các cấu hình máy mẫu để người dùng chọn nhanh thử nghiệm */
export interface SystemPreset {
  id: string
  name: string
  familyId?: FamilyId
  tag: string
  specs: {
    cpu: string
    cpuSocket: 'LGA1700' | 'AM5' | 'AM4'
    cpuTdp: number // W
    coolerKind: 'stock' | 'tower_155' | 'aio_240' | 'aio_360' | 'low_47'
    motherboard: string
    formFactor: 'ATX' | 'mATX' | 'ITX'
    ramDdr: 'DDR4' | 'DDR5'
    ramSlots: number
    ramUsedSlots: number
    ramCapacityGb: number
    ramSpeedMhz: number
    gpu: string
    gpuLengthMm: number
    gpuWatts: number
    gpuPsuReq: number
    gpuPciePins: string
    storage: string
    m2SlotsTotal: number
    m2SlotsUsed: number
    caseName: string
    caseMaxGpuMm: number
    caseCoolerHeightMm: number
    caseRadiatorSupport: 'none' | '240' | '360'
    psuWatt: number
    psuType: 'ATX' | 'SFX'
  }
}

export const SYSTEM_PRESETS: SystemPreset[] = [
  {
    id: 'gaming_atx',
    name: 'Máy Gaming Đầy Đủ (ATX)',
    familyId: 'standard_tower',
    tag: 'Phổ biến, dễ nâng cấp',
    specs: {
      cpu: 'Intel Core i5-13400F',
      cpuSocket: 'LGA1700',
      cpuTdp: 65,
      coolerKind: 'tower_155',
      motherboard: 'B760 GAMING DDR4',
      formFactor: 'ATX',
      ramDdr: 'DDR4',
      ramSlots: 4,
      ramUsedSlots: 2,
      ramCapacityGb: 16,
      ramSpeedMhz: 3200,
      gpu: 'GeForce RTX 4060 (2 quạt, 240mm)',
      gpuLengthMm: 240,
      gpuWatts: 115,
      gpuPsuReq: 550,
      gpuPciePins: '1x 8-pin',
      storage: '500 GB NVMe M.2 Gen 4',
      m2SlotsTotal: 2,
      m2SlotsUsed: 1,
      caseName: 'Standard Tower ATX',
      caseMaxGpuMm: 330,
      caseCoolerHeightMm: 160,
      caseRadiatorSupport: '240',
      psuWatt: 550,
      psuType: 'ATX',
    },
  },
  {
    id: 'compact_matx',
    name: 'Máy Thùng Gọn Compact (mATX)',
    familyId: 'compact_tower',
    tag: 'Nhỏ gọn tiết kiệm, giới hạn GPU 240mm',
    specs: {
      cpu: 'Intel Core i3-12100F',
      cpuSocket: 'LGA1700',
      cpuTdp: 58,
      coolerKind: 'tower_155',
      motherboard: 'H610M-K DDR4',
      formFactor: 'mATX',
      ramDdr: 'DDR4',
      ramSlots: 2,
      ramUsedSlots: 1,
      ramCapacityGb: 8,
      ramSpeedMhz: 3200,
      gpu: 'GeForce GTX 1650 (200mm)',
      gpuLengthMm: 200,
      gpuWatts: 75,
      gpuPsuReq: 400,
      gpuPciePins: 'Không cần nguồn phụ',
      storage: '256 GB NVMe M.2 Gen 3',
      m2SlotsTotal: 1,
      m2SlotsUsed: 1,
      caseName: 'Compact Tower mATX',
      caseMaxGpuMm: 245,
      caseCoolerHeightMm: 155,
      caseRadiatorSupport: 'none',
      psuWatt: 450,
      psuType: 'ATX',
    },
  },
  {
    id: 'mini_itx',
    name: 'Máy Tí Hon Mini SFF (ITX)',
    familyId: 'mini_sff',
    tag: 'Siêu nhỏ 11L, nguồn SFX, tản nhiệt thấp',
    specs: {
      cpu: 'AMD Ryzen 5 7600',
      cpuSocket: 'AM5',
      cpuTdp: 65,
      coolerKind: 'low_47',
      motherboard: 'B650I Mini-ITX DDR5',
      formFactor: 'ITX',
      ramDdr: 'DDR5',
      ramSlots: 2,
      ramUsedSlots: 2,
      ramCapacityGb: 16,
      ramSpeedMhz: 5600,
      gpu: 'GeForce RTX 4060 Ti Dual (235mm)',
      gpuLengthMm: 235,
      gpuWatts: 160,
      gpuPsuReq: 550,
      gpuPciePins: '1x 8-pin',
      storage: '1 TB NVMe M.2 Gen 4',
      m2SlotsTotal: 2,
      m2SlotsUsed: 1,
      caseName: 'Mini SFF Sandwich Case',
      caseMaxGpuMm: 245,
      caseCoolerHeightMm: 48,
      caseRadiatorSupport: 'none',
      psuWatt: 600,
      psuType: 'SFX',
    },
  },
  {
    id: 'showcase_beast',
    name: 'Showcase Hồ Cá Đồ Họa (Dual-Chamber)',
    familyId: 'showcase_dual_chamber',
    tag: 'Mặt kính rộng, tản nước AIO',
    specs: {
      cpu: 'Intel Core i7-14700K',
      cpuSocket: 'LGA1700',
      cpuTdp: 125,
      coolerKind: 'aio_240',
      motherboard: 'Z790 AORUS Elite DDR5',
      formFactor: 'ATX',
      ramDdr: 'DDR5',
      ramSlots: 4,
      ramUsedSlots: 2,
      ramCapacityGb: 32,
      ramSpeedMhz: 6000,
      gpu: 'GeForce RTX 4070 Ti SUPER (295mm)',
      gpuLengthMm: 295,
      gpuWatts: 285,
      gpuPsuReq: 750,
      gpuPciePins: '1x 16-pin 12VHPWR',
      storage: '1 TB NVMe M.2 Gen 4',
      m2SlotsTotal: 3,
      m2SlotsUsed: 1,
      caseName: 'Showcase Dual Chamber',
      caseMaxGpuMm: 360,
      caseCoolerHeightMm: 165,
      caseRadiatorSupport: '240',
      psuWatt: 750,
      psuType: 'ATX',
    },
  },
]

/** Danh mục các linh kiện nâng cấp mục tiêu */
export interface UpgradeTargetOption {
  id: string
  category: 'ram' | 'ssd' | 'gpu' | 'cpu' | 'psu' | 'cooler'
  name: string
  costEstimate: string
  impact: string
  specChanges: Partial<SystemPreset['specs']> & {
    // metadata để kiểm tra tương thích
    reqSocket?: 'LGA1700' | 'AM5' | 'AM4'
    reqDdr?: 'DDR4' | 'DDR5'
    reqPsuWatt?: number
    reqGpuLengthMm?: number
    reqCaseCoolerMm?: number
    reqRadSupport?: '240' | '360'
    reqPsuType?: 'ATX' | 'SFX'
    needsM2Slot?: boolean
    needsRamSlot?: boolean
  }
}

export const UPGRADE_OPTIONS: UpgradeTargetOption[] = [
  // RAM
  {
    id: 'ram_add_8gb_ddr4',
    category: 'ram',
    name: 'Thêm 1 thanh 8 GB DDR4 3200 MHz (Lên 16 GB)',
    costEstimate: '~450.000 ₫',
    impact: 'Kích hoạt Dual-Channel, mượt mà các tác vụ văn phòng và game cơ bản',
    specChanges: {
      ramCapacityGb: 16,
      ramUsedSlots: 2,
      reqDdr: 'DDR4',
      needsRamSlot: true,
    },
  },
  {
    id: 'ram_up_32gb_ddr4',
    category: 'ram',
    name: 'Nâng lên bộ Kit 32 GB (2x16GB) DDR4 3200 MHz',
    costEstimate: '~1.400.000 ₫',
    impact: 'Làm việc đa nhiệm, mở 100 tab Chrome, render video Premiere Pro mượt mà',
    specChanges: {
      ramCapacityGb: 32,
      ramUsedSlots: 2,
      reqDdr: 'DDR4',
    },
  },
  {
    id: 'ram_up_32gb_ddr5',
    category: 'ram',
    name: 'Nâng lên bộ Kit 32 GB (2x16GB) DDR5 6000 MHz',
    costEstimate: '~2.600.000 ₫',
    impact: 'Băng thông cực cao cho hệ thống đời mới, tối ưu cho CPU Intel Gen 13/14 và AMD Ryzen 7000/9000',
    specChanges: {
      ramCapacityGb: 32,
      ramSpeedMhz: 6000,
      ramUsedSlots: 2,
      reqDdr: 'DDR5',
    },
  },
  // SSD
  {
    id: 'ssd_add_1tb_gen4',
    category: 'ssd',
    name: 'Gắn thêm 1 TB NVMe M.2 PCIe Gen 4 (Tốc độ ~5000 MB/s)',
    costEstimate: '~1.600.000 ₫',
    impact: 'Mở rộng gấp đôi không gian lưu trữ game và ứng dụng, tốc độ tải thần tốc',
    specChanges: {
      storage: '+ 1 TB NVMe M.2 PCIe Gen 4',
      needsM2Slot: true,
    },
  },
  {
    id: 'ssd_add_2tb_gen4',
    category: 'ssd',
    name: 'Gắn thêm 2 TB NVMe M.2 PCIe Gen 4 (Tốc độ ~7000 MB/s)',
    costEstimate: '~3.200.000 ₫',
    impact: 'Lưu trữ thoải mái các tựa game AAA dung lượng 100GB+ và thư viện video 4K',
    specChanges: {
      storage: '+ 2 TB NVMe M.2 PCIe Gen 4',
      needsM2Slot: true,
    },
  },
  // GPU
  {
    id: 'gpu_rtx4060_2fan',
    category: 'gpu',
    name: 'Nâng cấp lên GeForce RTX 4060 8GB (2 quạt, dài 240mm)',
    costEstimate: '~7.800.000 ₫',
    impact: 'Chiến mượt mọi game 1080p Ultra Settings, hỗ trợ DLSS 3, ăn điện cực ít',
    specChanges: {
      gpu: 'GeForce RTX 4060 8GB (2 quạt)',
      gpuLengthMm: 240,
      gpuWatts: 115,
      gpuPsuReq: 550,
      reqGpuLengthMm: 240,
      reqPsuWatt: 500,
    },
  },
  {
    id: 'gpu_rtx4070_3fan',
    category: 'gpu',
    name: 'Nâng cấp lên GeForce RTX 4070 SUPER 12GB (3 quạt, dài 305mm)',
    costEstimate: '~16.500.000 ₫',
    impact: 'Quái vật chiến game 2K/4K max setting, Ray Tracing xuất sắc, 12GB VRAM',
    specChanges: {
      gpu: 'GeForce RTX 4070 SUPER 12GB (3 quạt)',
      gpuLengthMm: 305,
      gpuWatts: 220,
      gpuPsuReq: 650,
      reqGpuLengthMm: 305,
      reqPsuWatt: 650,
    },
  },
  {
    id: 'gpu_rtx4070_mini_2fan',
    category: 'gpu',
    name: 'Nâng cấp lên GeForce RTX 4070 12GB Bản Mini 2 quạt (Dài 242mm)',
    costEstimate: '~14.500.000 ₫',
    impact: 'Sức mạnh cực lớn trong thân hình nhỏ gọn, gắn vừa hầu hết case Mini ITX',
    specChanges: {
      gpu: 'GeForce RTX 4070 12GB Dual Mini',
      gpuLengthMm: 242,
      gpuWatts: 200,
      gpuPsuReq: 650,
      reqGpuLengthMm: 242,
      reqPsuWatt: 650,
    },
  },
  // CPU
  {
    id: 'cpu_i7_14700k',
    category: 'cpu',
    name: 'Nâng cấp lên Intel Core i7-14700K (20 nhân, 28 luồng)',
    costEstimate: '~10.200.000 ₫',
    impact: 'Xử lý đa nhiệm và render cực mạnh, xung nhịp lên tới 5.6 GHz',
    specChanges: {
      cpu: 'Intel Core i7-14700K',
      cpuTdp: 125,
      reqSocket: 'LGA1700',
      reqPsuWatt: 700,
      reqRadSupport: '240',
    },
  },
  {
    id: 'cpu_ryzen7_7800x3d',
    category: 'cpu',
    name: 'Nâng cấp lên AMD Ryzen 7 7800X3D (Vua chơi game 3D V-Cache)',
    costEstimate: '~10.800.000 ₫',
    impact: 'Vô địch hiệu năng gaming với bộ nhớ đệm 3D V-Cache khổng lồ 96MB, tiêu thụ điện thấp',
    specChanges: {
      cpu: 'AMD Ryzen 7 7800X3D',
      cpuTdp: 120,
      reqSocket: 'AM5',
      reqPsuWatt: 600,
    },
  },
  // PSU
  {
    id: 'psu_750w_gold',
    category: 'psu',
    name: 'Nâng cấp Nguồn 750W 80 Plus Gold ATX 3.0 (PCIe 5.0 12VHPWR)',
    costEstimate: '~2.500.000 ₫',
    impact: 'Cung cấp năng lượng thoải mái cho RTX 4070/4080, chuẩn ATX 3.0 chống sập nguồn',
    specChanges: {
      psuWatt: 750,
      reqPsuType: 'ATX',
    },
  },
  {
    id: 'psu_sfx_750w_gold',
    category: 'psu',
    name: 'Nâng cấp Nguồn SFX 750W 80 Plus Gold (Cho Case Mini ITX)',
    costEstimate: '~3.400.000 ₫',
    impact: 'Nguồn chuẩn SFX tí hon nhưng công suất khủng 750W cho dàn Mini SFF',
    specChanges: {
      psuWatt: 750,
      reqPsuType: 'SFX',
    },
  },
  // Cooler
  {
    id: 'cooler_aio_240',
    category: 'cooler',
    name: 'Nâng cấp Tản nhiệt nước AIO 240 mm (Két làm mát 2 quạt 120mm)',
    costEstimate: '~1.800.000 ₫',
    impact: 'Giữ CPU luôn dưới 70°C, mặt block nước gắn LED ARGB cực đẹp',
    specChanges: {
      coolerKind: 'aio_240',
      reqRadSupport: '240',
    },
  },
  {
    id: 'cooler_tower_dual',
    category: 'cooler',
    name: 'Nâng cấp Tản nhiệt khí tháp đôi 2 tháp nhôm (Cao 157mm)',
    costEstimate: '~850.000 ₫',
    impact: 'Hiệu năng tản nhiệt cực mạnh, không lo rò rỉ nước, độ bền vĩnh cửu',
    specChanges: {
      coolerKind: 'tower_155',
      reqCaseCoolerMm: 157,
    },
  },
]

export interface CompatibilityCheckResult {
  status: 'ok' | 'warning' | 'error'
  headline: string
  details: string[]
  advice: string
  powerStatus?: {
    estimatedTotalWatts: number
    recommendedPsu: number
    currentPsu: number
    isPsuSufficient: boolean
  }
}

/** Hàm kiểm tra tương thích giữa cấu hình hiện tại và linh kiện muốn nâng cấp */
export function checkUpgradeCompatibility(
  current: SystemPreset['specs'],
  option: UpgradeTargetOption,
): CompatibilityCheckResult {
  const issues: string[] = []
  const warnings: string[] = []
  const changes = option.specChanges

  // 1. Kiểm tra RAM
  if (changes.reqDdr && changes.reqDdr !== current.ramDdr) {
    issues.push(
      `Không tương thích chuẩn RAM: Bo mạch chủ hiện tại dùng ${current.ramDdr}, trong khi linh kiện bạn chọn là ${changes.reqDdr}. Hai chuẩn này khác nhau về chân cắm vật lý và điện áp, không thể lắp chung.`,
    )
  }
  if (changes.needsRamSlot) {
    const slotsFree = current.ramSlots - current.ramUsedSlots
    if (slotsFree <= 0) {
      issues.push(
        `Không còn khe RAM trống: Bo mạch chủ hiện tại chỉ có ${current.ramSlots} khe và đã cắm đủ ${current.ramUsedSlots}/${current.ramSlots} thanh. Muốn nâng cấp, bạn phải tháo thanh cũ ra thay thế bằng bộ kit dung lượng lớn hơn.`,
      )
    }
  }

  // 2. Kiểm tra SSD M.2
  if (changes.needsM2Slot) {
    const m2Free = current.m2SlotsTotal - current.m2SlotsUsed
    if (m2Free <= 0) {
      warnings.push(
        `Bo mạch chủ đã dùng hết ${current.m2SlotsUsed}/${current.m2SlotsTotal} khe M.2 NVMe. Để gắn thêm, bạn cần chuyển dữ liệu sang ổ mới để thay thế, hoặc mua thêm card chuyển đổi PCIe-to-M.2 (nếu case còn khe mở rộng), hoặc chọn loại SSD 2.5" SATA.`,
      )
    }
  }

  // 3. Kiểm tra GPU
  if (changes.reqGpuLengthMm) {
    if (changes.reqGpuLengthMm > current.caseMaxGpuMm) {
      issues.push(
        `Card đồ họa quá dài (${changes.reqGpuLengthMm} mm) so với khoảng trống tối đa của vỏ case (${current.caseMaxGpuMm} mm). Card sẽ bị cấn vào quạt trước hoặc khung vỏ case, không thể nhét vừa!`,
      )
    } else if (changes.reqGpuLengthMm > current.caseMaxGpuMm - 15) {
      warnings.push(
        `Chiều dài card (${changes.reqGpuLengthMm} mm) rất sát với giới hạn case (${current.caseMaxGpuMm} mm). Bạn sẽ cần khéo léo khi luồn card vào khoang máy và chú ý luồng dây nguồn.`,
      )
    }
  }

  // 4. Kiểm tra CPU & Socket
  if (changes.reqSocket && changes.reqSocket !== current.cpuSocket) {
    issues.push(
      `Không tương thích chân cắm CPU (Socket): Bo mạch chủ hiện tại dùng socket ${current.cpuSocket}, còn CPU muốn nâng cấp dùng socket ${changes.reqSocket}. Bạn bắt buộc phải thay cả Bo mạch chủ nếu muốn lên CPU này.`,
    )
  }

  // 5. Kiểm tra Tản nhiệt & Kích thước Case
  if (changes.reqCaseCoolerMm && changes.reqCaseCoolerMm > current.caseCoolerHeightMm) {
    issues.push(
      `Tản nhiệt khí cao ${changes.reqCaseCoolerMm} mm, vượt quá giới hạn nắp hông case (${current.caseCoolerHeightMm} mm). Bạn sẽ không thể đóng được nắp kính hông case!`,
    )
  }
  if (changes.reqRadSupport) {
    if (current.caseRadiatorSupport === 'none') {
      issues.push(
        `Vỏ case hiện tại (${current.caseName}) là dạng siêu nhỏ gọn, không có vị trí bắt két nước tản nhiệt AIO. Hãy chọn tản nhiệt khí thích hợp.`,
      )
    } else if (changes.reqRadSupport === '360' && current.caseRadiatorSupport === '240') {
      issues.push(
        `Vỏ case chỉ hỗ trợ két tản nước tối đa 240 mm, không thể gắn vừa két tản nhiệt nước 360 mm (3 quạt).`,
      )
    }
  }

  // 6. Kiểm tra Kích thước Nguồn (ATX vs SFX)
  if (changes.reqPsuType && changes.reqPsuType !== current.psuType) {
    issues.push(
      `Không vừa khung nguồn: Vỏ case yêu cầu nguồn chuẩn ${current.psuType}, trong khi nguồn bạn chọn là chuẩn ${changes.reqPsuType}. Nguồn ATX to không thể nhét vừa khoang nguồn SFX của case Mini ITX.`,
    )
  }

  // 7. Ước tính Công suất Nguồn (PSU Wattage calculation)
  const targetGpuWatts = changes.gpuWatts ?? current.gpuWatts
  const targetCpuWatts = changes.cpuTdp ?? current.cpuTdp
  const targetPsuWatt = changes.psuWatt ?? current.psuWatt
  // 100W cho main, ram, quạt, ổ đĩa
  const estimatedPeakSystemWatts = targetCpuWatts + targetGpuWatts + 100
  // Hệ số headroom an toàn 1.3
  const recommendedPsuWatts = Math.ceil((estimatedPeakSystemWatts * 1.3) / 50) * 50

  const isPsuSufficient = targetPsuWatt >= (changes.reqPsuWatt ?? recommendedPsuWatts)

  if (!isPsuSufficient) {
    if (targetPsuWatt < estimatedPeakSystemWatts) {
      issues.push(
        `Công suất nguồn quá thấp: Nguồn hiện tại (${targetPsuWatt} W) thấp hơn cả công suất tiêu thụ tối đa ước tính (${estimatedPeakSystemWatts} W). Máy chắc chắn sẽ bị sập nguồn khi tải nặng hoặc chơi game!`,
      )
    } else {
      warnings.push(
        `Nguồn hiện tại (${targetPsuWatt} W) sát mức giới hạn khuyến nghị (${recommendedPsuWatts} W). Dù có thể chạy được nhưng nguồn sẽ phải làm việc ở 90-100% công suất, quạt kêu to và dễ bị sập khi card đồ họa ăn điện đột ngột (transient power spike).`,
      )
    }
  }

  // Tổng hợp kết quả
  if (issues.length > 0) {
    return {
      status: 'error',
      headline: 'Không tương thích! Cần điều chỉnh linh kiện',
      details: [...issues, ...warnings],
      advice:
        'Bạn không nên mua linh kiện này ngay vì có xung đột phần cứng. Hãy xem các mục cảnh báo màu đỏ ở trên và cân nhắc phương án thay thế phù hợp hơn.',
      powerStatus: {
        estimatedTotalWatts: estimatedPeakSystemWatts,
        recommendedPsu: recommendedPsuWatts,
        currentPsu: targetPsuWatt,
        isPsuSufficient,
      },
    }
  }

  if (warnings.length > 0) {
    return {
      status: 'warning',
      headline: 'Tương thích, nhưng cần lưu ý kỹ trước khi lắp!',
      details: warnings,
      advice:
        'Linh kiện có thể lắp được vào máy, nhưng bạn cần chú ý các điều kiện về không gian case hoặc công suất nguồn để hệ thống vận hành bền bỉ nhất.',
      powerStatus: {
        estimatedTotalWatts: estimatedPeakSystemWatts,
        recommendedPsu: recommendedPsuWatts,
        currentPsu: targetPsuWatt,
        isPsuSufficient,
      },
    }
  }

  return {
    status: 'ok',
    headline: 'Hoàn toàn tương thích 100%!',
    details: [
      'Chân cắm và socket vật lý hoàn toàn khớp với bo mạch chủ.',
      'Kích thước vừa vặn trong không gian vỏ case, không bị cấn linh kiện khác.',
      `Bộ nguồn hiện tại (${targetPsuWatt} W) dư dả công suất vận hành an toàn (khuyến nghị ${recommendedPsuWatts} W).`,
    ],
    advice:
      'Tuyệt vời! Bạn có thể tự tin đặt mua linh kiện này. Hãy xem cẩm nang hướng dẫn tháo lắp bên dưới để tự tay nâng cấp an toàn.',
    powerStatus: {
      estimatedTotalWatts: estimatedPeakSystemWatts,
      recommendedPsu: recommendedPsuWatts,
      currentPsu: targetPsuWatt,
      isPsuSufficient: true,
    },
  }
}
