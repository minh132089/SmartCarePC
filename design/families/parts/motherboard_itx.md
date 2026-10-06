# Bo Mini-ITX dùng cho họ 04 và 05

[Ảnh tham chiếu](motherboard_itx.png). Nguồn: [đặc tả họ vỏ, mục 7.1 và 8](../00_families_spec.md), [quy ước Gaming](../../gaming/00_blueprint.md). **Số trong tài liệu là chuẩn dựng; ảnh chỉ minh họa.** Đây là bố trí mô phỏng, không phải mẫu khoan của bo thương mại.

## Kích thước và tọa độ

Đơn vị mm. O = tâm hình vuông trên **mặt lưng PCB**, X hướng từ cạnh I/O sang cạnh ATX24, Y hướng từ cạnh PCIe lên cạnh EPS, Z hướng ra mặt linh kiện. PCB X/Y = −85…85, Z = 0…1,6. PCB **170 × 170 × 1,6**; bao cứng có I/O X = −99…85, Y = −85…85, Z = 0…37,6, tức **184 × 170 × 37,6**, chưa tính bộ gá tản rời. 100 mm = 1 đơn vị cảnh.

Sheet thể hiện bo trống, không lắp CPU, RAM, SSD, GPU hoặc riser. Tọa độ dưới đây là tâm X/Y, trừ nơi ghi miệng/vít; kích thước thân theo X × Y × Z, chiều cao tính từ mặt PCB Z = 1,6.

| Cụm | Vị trí và hình học |
|---|---|
| Bốn lỗ bo | (−75;−75), (−75;75), (75;−75), (75;75); Ø3,4; lưới 150 × 150, cách mép 10; vùng trống bán kính 4 |
| Socket CPU | (−20;12), khung 50 × 50; khung ép, bản lề, cần khóa và dấu hướng CPU. Mốc đáy CPU khi lắp Z = 4,7, mặt IHS Z = 9,2 |
| Bốn lỗ gá tản | (−59;−27), (−59;51), (19;−27), (19;51); Ø3,5, lưới 78 × 78, khác bốn lỗ bắt bo |
| Hai DIMM A/B | (55;6), (65;6); mỗi thân 6 × 142 × 12, chạy dọc Y, chốt hai đầu; rãnh DDR4 lệch tâm. Khi lắp dùng cả hai thanh 133,35 × 42 × 6, gốc đáy RAM Z = 5,5 |
| Một PCIe x16 | (3;−72), thân 89 × 9 × 11, chốt ở đầu +X; mốc đầu riser (3;−72;10,4). Cắm từ phía +Z, chuyển động xuống −Z |
| Một M.2 2280 | Tâm vùng lắp (−12;−48), vùng 80 × 22; miệng (−52;−48), vít (28;−48). SSD đưa vào từ +X về −X rồi hạ xuống; chân đỡ cao 3 trên PCB, mặt đỡ Z = 4,6 |
| Tản VRM | (−65;18), 14 × 55 × 10, đỉnh Z = 11,6; không che lỗ gá tản |
| Tấm đỡ tản rời | 90 × 90, tâm (−20;12), phía −Z; đệm cách điện 0,8. Chiều dày kim loại theo bộ gá Gaming, không suy từ ảnh; không thuộc bao cứng bo |

## Đầu cắm và khóa

Đầu dây trên mặt bo nhận từ phía +Z; I/O ngoài mở về −X. Giữ dấu chân 1 và dải thao tác 3 quanh đầu cắm; không để SMD chiếm vùng gá tản.

| Đầu | Tâm (X;Y) | Số chân, thân và khóa |
|---|---|---|
| ATX24 | (80;5) | 24 = 2 × 12; thân 10 × 52 × 13, đỉnh Z = 14,6; ngàm hướng +X, lỗ vuông/vát |
| EPS CPU | (−55;78) | 8 = 2 × 4; thân 18 × 9 × 13, đỉnh Z = 14,6; ngàm hướng +Y, nhận dây CPU 4+4 |
| CPU_FAN | (17;73) | Một hàng 4: GND/12V/tach/PWM; gờ dẫn hướng |
| SYS1 / SYS2 | (−48;−53) / (−25;−53) | Mỗi đầu một hàng 4 như CPU_FAN; nằm trong hình chiếu vùng SSD nên phải kiểm tra khoảng hở khi lắp SSD |
| USB19 | (80;−46) | 19 trên khung 2 × 10, khuyết chân 20; thân 8 × 20 × 9, đỉnh Z = 10,6; vỏ có rãnh khóa; cấp USB-A mặt trước |
| Type-E | (80;58) | 20 tiếp điểm trong hốc có khóa định hướng; thân 8 × 20 × 10, đỉnh Z = 11,6; nhận dây USB-C mặt trước |
| F_PANEL | (60;−80) | 9 trên khung 2 × 5, khuyết 10; thân khoảng 13 × 5 × 7; không dùng chân 9 |
| HD_AUDIO | (−62;−80) | 9 trên khung 2 × 5, khuyết 8; thân khoảng 13 × 5 × 7, lỗ bịt đầu dây khớp chân khuyết |
| Socket / DIMM / x16 / M.2 | Theo bảng trên | LGA 1.200 tiếp điểm quy ước; DDR4 288 mỗi khe; x16 164; M.2 khung 75 vị trí, 67 tiếp điểm và khóa M |

Đầu quạt dùng thân khoảng 10 × 5 × 8, đỉnh Z = 9,6. Không có AIO_PUMP, ARGB, SATA hoặc đầu nguồn GPU trên bo này. Không có nút bấm, lỗ luồn dây hay grommet trên PCB.

F_PANEL nhìn từ +Z, chân 1 ở dưới trái: hàng trên **2 PLED+ / 4 PLED− / 6 PWR / 8 PWR / 10 khuyết**; hàng dưới **1 HDD+ / 3 HDD− / 5 RESET / 7 RESET / 9 không dùng**. Đây là sơ đồ riêng của mô phỏng.

Lá I/O X = −99…−98,2, Y = −79…79, Z = 1,6…37,6; dày 0,8. Tất cả tâm cổng Z = 16, mở ra −X:

- Hai USB-A tại Y = 58 và 39; mỗi cổng 4 tiếp điểm theo quy ước Gaming, lưỡi định hướng. Cửa lá 14,6 theo Y × 6,8 theo Z; lòng cổng 13 × 5.
- Một RJ45 tại Y = 5, 8 tiếp điểm, có rãnh ngàm; cửa lá 17 × 15, lòng cổng 16 × 14.
- Ba jack TRS tại Y = −25/−42/−59, mỗi jack 3 tiếp điểm, lòng Ø3,5. Không thêm HDMI, DisplayPort hoặc antenna.

## Vít và vật liệu

| Nhóm | Số lượng và vị trí |
|---|---|
| Bo → khay | 4 vít M3 × 5 và 4 chân đế M3 cao 6,5, trùng bốn lỗ bo; chân đế nằm Z = −6,5…0 khi lắp |
| SSD → chân đỡ | 1 vít M2 × 2,5 tại (28;−48), 1 chân đỡ cao 3; ảnh chỉ đánh dấu điểm bắt khi chưa lắp SSD |
| Giao diện bộ tản | 4 vít lò xo M3 × 12 của bộ tản qua lưới 78 × 78; không tính là vít bắt bo hoặc vít CPU |

PCB mờ `#16181D`; nhựa cổng `#101216`; khung socket/tản VRM kim loại `#AEB4BC`; lá I/O `#BCC2C8`; tiếp điểm `#C6A565`; lưỡi USB-A `#1F6FD1`; chữ kỹ thuật `#D2D7DD`; nét kích thước `#2164AC`. Không logo, đèn RGB hay chữ trang trí. Cần socket và chốt RAM/PCIe không có vít tháo rời trong bài học.

## Các hình chiếu và sai lệch ảnh

| View | Nội dung và hướng chuẩn |
|---|---|
| FRONT +X nhìn −X | Cạnh ATX24/USB19/Type-E; +Z sang trái, +Y lên trên |
| BACK −X nhìn +X | Lá I/O với đúng sáu miệng cổng; +Z sang phải, +Y lên trên |
| LEFT +Z nhìn −Z | Mặt linh kiện; +X sang phải, +Y lên trên |
| RIGHT −Z nhìn +Z | Mặt hàn và dấu vùng backplate; +X sang trái, +Y lên trên |
| TOP +Y nhìn −Y | Cạnh EPS, chiều cao thân cổng; +X xuống dưới, +Z sang phải |
| BOTTOM −Y nhìn +Y | Cạnh x16/F_PANEL/audio; +X lên trên, +Z sang phải |
| 3/4 | Chiều sâu socket, chốt DIMM/x16, phần nhô I/O; không dùng để đo |

Ảnh đã sửa một lượt bằng công cụ imagegen tích hợp. Còn các sai lệch phải bỏ qua khi dựng:

- TOP/BOTTOM còn trình bày nằm ngang, chưa xoay theo quy ước X/Z trong bảng; tỷ lệ các cạnh chỉ gần đúng. Ở RIGHT, mũi tên sang trái ghi **−X** là sai, phải là **+X**.
- Trên mặt linh kiện, Type-E bị lặp/sai vị trí: đầu ở phía trên cạnh phải mang nhãn 07, còn nhãn 12 xuất hiện phía dưới ATX24. Chỉ dựng **một Type-E tại (80;58)**; ATX24 duy nhất tại (80;5), USB19 tại (80;−46). Chú giải dưới bị lặp số 12; dùng tên đầu và bảng tọa độ để phân biệt.
- Lỗ gá tản, socket, VRM và đầu SYS được minh họa lệch tâm/kích thước; chữ tọa độ ở sơ đồ lỗ tản thiếu tâm (−20;12). Dùng bốn tọa độ đã chốt, không đo hình. Ký hiệu 12 trên lỗ backplate phải hiểu là lỗ gá tản, không phải Type-E.
- Số chân nhỏ và vị trí chân khuyết trong hình chưa đủ chính xác, nhất là USB19/F_PANEL/HD_AUDIO. Dùng bảng số chân và pinout ở trên. Ảnh 3/4 có hai miệng USB-A sát nhau theo chiều ngang, không khớp hai tâm Y đã chốt; lấy BACK và bảng I/O làm chuẩn.

## Người mới cần thấy

1. Bốn lỗ bắt bo và bốn lỗ gá tản là hai nhóm riêng; không đặt chân đế tùy ý dưới PCB.
2. Hai khe đều nhận DDR4 nguyên cỡ. Đặt đúng rãnh khóa, không thu nhỏ RAM cho vừa ITX.
3. GPU của họ 04/05 nối qua riser tại x16; nhả chốt trước khi rút. Không có khe PCIe thứ hai.
4. USB-C mặt trước đi vào Type-E; USB-A mặt trước đi vào USB19. EPS CPU 4+4 khác dây GPU 6+2.
5. SSD M.2 vào miệng trước, hạ xuống chân đỡ rồi bắt một vít; kiểm tra đầu SYS phía dưới vùng SSD khi dựng lắp ráp.

Prompt đã dùng, rút gọn: “MINI-ITX / MOTHERBOARD, white engineering sheet, realistic black PCB, blue dimensions; six same-scale orthographic views plus one 3/4; 170 × 170 × 1.6, four board mounts, two DDR4, one x16/riser, empty M.2, USB19 and Type-E; exact local coordinates and connector keys; no brands.” Lượt sửa tập trung nhãn hướng, phân biệt USB19/Type-E và số chân; giới hạn còn lại được liệt kê ở trên.
