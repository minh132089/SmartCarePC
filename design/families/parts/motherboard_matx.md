# Motherboard mATX — bo cho compact_tower

Ảnh: [motherboard_matx.png](motherboard_matx.png). Nguồn: [đặc tả họ vỏ, mục 7.1](../00_families_spec.md), [quy ước Gaming](../../gaming/00_blueprint.md) và hình socket/khóa trong `tools/build_gaming.py`. **Số trong tài liệu là chuẩn dựng; không đo ngược từ ảnh.** Lưới lỗ là lựa chọn mô phỏng, không phải mẫu khoan thương mại. Bo trống, chưa lắp CPU, RAM, SSD, GPU hoặc dây.

## Kích thước và hệ tọa độ

Đơn vị mm; 100 mm = 1 đơn vị cảnh. Gốc O ở tâm **mặt lưng PCB**. X hướng từ cạnh I/O (−) sang cạnh ATX24 (+); Y từ cạnh PCIe (−) lên EPS (+); Z từ lưng PCB ra mặt linh kiện. PCB **244 X × 244 Y × 1,6 Z**, X/Y = ±122, Z = 0…1,6. Bao cứng gồm I/O: X = −136,2…122, Y = ±122, Z = 0…37,6, tức **258,2 × 244 × 37,6**. Bộ gá tản sau bo ghi riêng, không cộng vào bao này.

Khi lắp họ 02: B = (−49;54;−69); điểm toàn máy = B + điểm cục bộ, không xoay/lật gương. Bảng dưới ghi tâm X/Y; kích thước theo X × Y × cao trên PCB, trừ nơi ghi khác.

## Socket, khe và lỗ

| Chi tiết | Vị trí / bao | Hình học cần giữ |
|---|---|---|
| Socket LGA | (0;50), thân 50 × 50; khung ép tới Z = 7 | Lòng 38 × 38; 1.200 tiếp điểm quy ước tại Z = 4,65; bản lề phía +Y, cần gạt phía +X, tam giác góc −X/−Y, hai gờ khóa CPU. Cần gạt nhô ngoài thân theo Gaming |
| Mốc CPU, chưa lắp | Đáy (0;50;4,7), IHS Z = 9,2 | CPU chung 37,5 × 37,5 × 4,5; giữ nguyên socket mô phỏng |
| DIMM A1/A2/B1/B2 | X = 70/79/89/98, Y = 40; mỗi khe 6 × 142 × 12 | Bốn khe DDR4, 288 tiếp điểm/khe; rãnh lệch tâm, chốt hai đầu. Hai thanh sau này lắp A2/B2, đáy thanh Z = 5,5 |
| PCIe 1 | (−34;−61), 89 × 9 × 11 | x16, 164 tiếp điểm, gia cường bạc; khóa ở +X, vách khóa kế thừa Gaming |
| PCIe 2 | (−34;−101), cùng bao | Vỏ đen, cùng dạng dài và chốt; để trống, không suy ra số lane điện |
| M.2 2280 | Vùng trống tâm (35;−83), 80 × 22 | Miệng (−5;−83), mở +X; thân X = −11…−5, Y = −94…−72, Z = 1,6…7,2; khe nhận Z = 4,2…5,8. M-key, 67 tiếp điểm trên khung 75 vị trí |
| Chân đỡ SSD | (75;−83); Ø4, cao 3 trên PCB | Mặt đỡ Z = 4,6; ren M2. Chỉ vẽ footprint SSD, không thêm ổ cứng |
| Tản VRM | (−53;50), 16 × 60 × 16 | Rãnh nhôm đơn giản, đỉnh Z = 17,6; không lấn lỗ gá tản |
| H1–H3 | (−112;112), (0;112), (112;112) | Ba lỗ bo Ø3,4 |
| H4–H6 | (−112;0), (0;0), (112;0) | Ba lỗ bo Ø3,4, H5 đúng gốc O |
| H7–H9 | (−112;−112), (0;−112), (112;−112) | Ba lỗ bo Ø3,4 |
| Bốn lỗ gá tản | (−39;11), (39;11), (−39;89), (39;89) | Ø3,5, bước 78 × 78, không cộng vào chín lỗ bo |
| Vùng tấm đỡ tản | X = ±45, Y = 5…95, phía −Z | Footprint 90 × 90 nét đứt; tấm thuộc bộ tản, đệm cách điện 0,8 theo đặc tả họ vỏ; không lắp trên bo trống |

Chừa bán kính 4 quanh mỗi lỗ bo và dải 3 quanh đầu cắm; linh kiện SMD chỉ bổ sung ngoài vùng gá tản. Không sao chép tản chipset của bo ATX vào vùng M.2/header của bo này.

## Đầu cắm và I/O

Z trong bảng là cao độ đỉnh đầu cắm tính từ lưng PCB. Đầu dây tiếp cận từ +Z, chuyển động cắm về −Z; SSD tiếp cận miệng M.2 từ +X. Vỏ nguồn có hốc vuông/vát và ngàm, mọi header có dấu chân 1.

| Đầu | Tâm (X;Y), Z đỉnh | Số chân, khóa / bao |
|---|---|---|
| ATX24 | (117;30), 14,6 | 2 × 12; 10 × 52 × 13; trục dài Y, ngàm hướng +X, giữ bao cứng đã chốt |
| EPS CPU | (−60;115), 14,6 | 2 × 4; 18 × 9 × 13; nhận 4+4, ngàm +Y |
| CPU_FAN | (25;115), 9,6 | 1 × 4, thân khoảng 10 × 5 × 8; GND/12 V/tach/PWM, gờ dẫn hướng |
| SYS_FAN1/2/3 | (106;79) / (81;−99) / (−96;−103), 9,6 | Mỗi đầu 1 × 4 như CPU_FAN; lần lượt quạt trước dưới/trước trên/sau |
| USB19 | (113;−92), 10,6 | 19 chân trên 2 × 10, khuyết chân 20; thân 8 × 20 × 9, dài Y, vỏ có khóa |
| F_PANEL | (94;−113), 9,1 | 9 chân trên 2 × 5, khuyết chân 10; bao phụ chọn 13 × 5 × 7,5 |
| HD_AUDIO | (−97;−113), 9,1 | 9 chân trên 2 × 5, khuyết chân 8; bao phụ chọn 13 × 5 × 7,5 |

Không có Type-E, AIO_PUMP, ARGB, SYS4 hoặc SATA. Bo không có nút bấm hay grommet đi dây; chúng thuộc vỏ.

Lá I/O dày 0,8 tại **X = −136,2…−135,4**, Y = −49…109, Z = 1,6…37,6. Tất cả cổng mở −X, tâm Z = 16. Chọn đuôi thân cổng tới X = −118, chừa vùng H4; kích thước cửa dưới đây là bổ sung cho mô hình.

| Cổng ngoài | Tâm Y | Cửa theo Y × Z / tiếp điểm |
|---|---|---|
| USB-A ×2 | 88; 69 | 13 × 5; mỗi cổng 4 tiếp điểm theo quy ước Gaming, lưỡi xanh |
| RJ45 ×1 | 35 | 16 × 14, có hốc ngàm; 8 tiếp điểm |
| Jack TRS ×3 | 5; −12; −29 | Lỗ Ø3,5; mỗi cổng 3 tiếp điểm |

Không thêm HDMI, DP, anten, USB-C hay nút trên lá I/O. Sáu cửa phải là hốc thật; mặt +Z chỉ thấy nóc thân cổng.

F_PANEL nhìn từ +Z, cột chạy theo +X, chân 1 góc dưới trái; sơ đồ riêng của mô phỏng:

| | Cột 1 | Cột 2 | Cột 3 | Cột 4 | Cột 5 |
|---|---|---|---|---|---|
| Hàng +Y | 2 PLED+ | 4 PLED− | 6 PWR | 8 PWR | 10 khuyết |
| Hàng −Y | 1 HDD+ | 3 HDD− | 5 RESET | 7 RESET | 9 không dùng |

Nút không phân cực; đèn có cực. Họ 02 chỉ nối chức năng hiện có trên vỏ, không tự thêm reset hoặc đèn ổ đĩa.

## Vít và vật liệu

| Nhóm | Số lượng / vị trí |
|---|---|
| Bo → khay | 9 vít M3 × 5, khớp H1–H9; 9 chân đế M3 cao 6,5 thuộc case, khay tại Z cục bộ −6,5 |
| SSD | 1 vít M2 × 2,5 tại (75;−83), chân đỡ cao 3 lắp sẵn |
| Tản CPU, bộ riêng | 4 vít lò xo M3 × 12 tại bốn lỗ gá 78 × 78; không phải vít lắp CPU |

PCB `#16181D` nhám 0,8; nhựa `#101216` nhám 0,55; kim loại bạc `#AEB4BC`, metallic 0,85/nhám 0,4; lá I/O `#BCC2C8`; tiếp điểm `#C6A565`; lưỡi USB `#1F6FD1`; chữ bo `#D2D7DD`. Nét kích thước `#2164AC`; dấu A2/B2 `#22D3EE` không phát sáng. Không logo hoặc chữ trang trí.

## Các hướng chiếu

Sáu hình vuông góc cùng tỷ lệ danh nghĩa; phối cảnh 3/4 chỉ giải thích chiều sâu.

| View | Hướng nhìn và nội dung |
|---|---|
| FRONT +X | Nhìn −X, +Z ở trái: cạnh ATX24/USB19, độ dày PCB và chiều cao đầu cắm |
| BACK −X | Nhìn +X, +Z ở phải: sáu cửa I/O, lá 158 × 36 |
| LEFT / COMPONENT FACE +Z | Nhìn −Z, +X sang phải: socket, bốn DIMM, hai PCIe, M.2 và lưới lỗ |
| RIGHT / SOLDER FACE −Z | Nhìn +Z, +X sang trái: lưng PCB, mối hàn và footprint tấm đỡ 90 × 90 |
| TOP +Y | Nhìn −Y, +X xuống dưới, +Z sang phải: cạnh EPS/CPU_FAN, vị trí I/O nhô |
| BOTTOM −Y | Nhìn +Y, +X lên trên, +Z sang phải: cạnh F_PANEL/HD_AUDIO, mặt cắt bo |
| 3/4 | Một góc nhìn nghiêng của cùng bo trống: chiều cao khe, cần socket và lá I/O |

## Người mới cần thấy

1. Chín lỗ giữ bo và bốn lỗ gá tản là hai nhóm riêng. Chân đế chỉ đặt dưới H1–H9.
2. CPU theo tam giác/rãnh khóa; cần socket giữ CPU, không dùng vít để lắp CPU.
3. Hai RAM vào A2/B2. Cả bốn khe vẫn giữ đúng khóa DDR4.
4. SSD vào M.2 trước khi lắp GPU ở PCIe 1; khe PCIe 2 để trống. Nhả chốt trước khi rút GPU.
5. EPS 4+4 dành cho CPU; USB19 và HD_AUDIO có vị trí khuyết khác nhau. Đèn F_PANEL cần đúng cực.

## Tạo ảnh và kiểm tra

Dùng công cụ imagegen tích hợp. Prompt chính: “One brand-free mATX product reference sheet, realistic black PCB and silver metal on white, thin blue dimensions; six aligned orthographic views plus one 3/4; 244 × 244 × 1.6 PCB; nine mounting holes, four empty DDR4 slots, two PCIe, empty M.2; exact local coordinates, keyed headers, six rear I/O openings and screw schedule.” Các tọa độ và yêu cầu view trong tài liệu được đưa vào prompt chi tiết.

Đã tạo một lượt và sửa một lượt theo brief. Prompt sửa yêu cầu chỉnh sáu hướng chiếu cùng tỷ lệ, lật đúng mặt lưng chỉ có mối hàn, đưa gốc O về H5, sửa vị trí socket/khe/header và số chân.

Ảnh cuối có đủ sáu nhãn hướng chiếu và một phối cảnh, chín lỗ H1–H9, bốn DIMM, hai PCIe và sáu cửa I/O. **Sai lệch còn lại:** TOP/BOTTOM được xoay nằm ngang, chưa theo chiều trục và tỷ lệ chung đã quy định; các view cạnh là minh họa chiều cao, chưa khớp hoàn toàn hình học. Vị trí socket, lỗ gá tản và PCIe trên mặt chính còn lệch so với tọa độ in cạnh chúng; SYS3 vẫn bị vẽ cạnh HD_AUDIO thay vì phía trên. Dấu O nằm hơi dưới H5; phải đặt đúng tâm H5 trên mặt lưng PCB. Inset F_PANEL vẽ đủ mười tiếp điểm dù chú thích chân 10 khuyết: khi dựng phải bỏ chân 10. Các hàng chân ATX/EPS/quạt/USB/audio bị giản lược, phải dựng đúng bảng số chân và khóa ở trên. Không dùng khoảng cách pixel, hình mối hàn hoặc số chân nhìn thấy trong ảnh làm dữ liệu hình học.
