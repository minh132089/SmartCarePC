# 02 Compact tower — Tổng thể

Nguồn: [đặc tả họ vỏ](../00_families_spec.md), mục 0, 2, 7; [quy ước Gaming](../../gaming/00_blueprint.md). Ảnh: [00_overview.png](00_overview.png). **Số trong tài liệu là chuẩn dựng mô hình; không đo ngược ảnh tạo sinh.** Đây là kích thước mô phỏng, không phải bản gia công.

## Kích thước và tọa độ lắp

Đơn vị mm; 100 mm = 1 đơn vị cảnh. O ở tâm thân, không tính chân. X: lưng (−) → trước (+); Y: đáy (−) → nóc (+); Z: hông kín (−) → hông mở (+). W × H × D = Z × Y × X. Tọa độ dưới đây là trạng thái đã lắp, không phải vị trí tách lớp trong ảnh.

Thân **210 × 380 × 370**, biên X ±185, Y ±190, Z ±105; tổng đặt bàn **210 × 394 × 370**. Hai hông thép kín, mặt trước bo R8.

| Số / cụm | Kích thước và vị trí |
|---|---|
| 1 Vỏ | Khung 1,2; panel 1. Hai hông 350 X × 356 Y; mặt ngoài Z ±105. Chân Ø26 × 14, tâm (±150; −197; ±78). |
| 2 Bo mATX | 244 X × 244 Y × 1,6 Z; gốc lưng PCB B = (−49; 54; −69), mặt linh kiện +Z. Chín chân đế cao 6,5 trên khay Z −75,5. |
| 3 CPU | 37,5 × 37,5 × 4,5; đáy (−49; 104; −64,3), mặt IHS (−49; 104; −59,8). |
| 4 Tản khí | Bao 90 X × 120 Y × 155 Z từ IHS: X −94…−4, Y 44…164, Z −59,8…95,2. Một quạt 120 × 120 × 25 tâm (−26,5; 104; 30,2), thổi −X; bốn heatpipe Ø6; backplate 90 × 90, đệm cách điện 0,8. |
| 5 RAM ×2 | DDR4 133,35 Y × 42 Z × 6 X. Gốc đáy A2 (30; 94; −63,5), B2 (49; 94; −63,5); A1/B1 để trống. |
| 6 GPU | Thân 240 X × 45 Y × 120 Z; gốc A = (−61; −26,7; −54,4), không xoay. Bao X −181…59, Y −49,2…−4,2, Z −54,4…65,6. Hai rotor Ø90 hướng xuống −Y; cắm trực tiếp x16, không riser. |
| 7 SSD | Một M.2 2280, 80 X × 22 Y × tối đa 3. Tâm XY (−14; −29), miệng (−54; −29), vít (26; −29); cao độ/đế giữ theo Gaming. |
| 8 PSU ATX | 140 X × 86 Y × 150 Z; tâm (−112; −143; −7). Hút −Y, xả −X; AC phía lưng, dây liền ra +X. Shroud trên Y −92, X −181…118, Z −75,5…100. |
| 9 Quạt case | F1 trước dưới (157,5; −82; 12); F2 trước trên (157,5; 43; 12); F3 sau (−169,5; 105; 30). Cả ba 120 × 120 × 25, thổi −X, lần lượt SYS1/2/3. **Không quạt nóc, không két.** |
| 10 Dây | Đen, đi sau khay Z −104…−76,5, sâu 27,5; hai rãnh rộng 24 tại X −152/102 và sáu đai. Dây nguồn uốn R≥20, cách cánh quạt ≥10. |

GPU có chân PCIe nhô thêm 8 về −Z, lá cổng tới X −182; đầu nguồn tâm **(44; −13,95; 65,6)**, cửa +Z. Thân cần ba khoảng khe trống dù lá chỉ bắt hai khe. Lưng có bốn vị trí PCIe bước 20,32, mép trên Y −1, mép dưới −82,28; còn hai nắp khi lắp GPU.

## Panel, cửa và đường dây

- Mặt trước: cassette X 172…185, Y ±181, Z ±99. **28 khe = 4 cột × 7 hàng**; tâm Z −60/−20/20/60, Y = −154 + 14j (j=0…6); mỗi khe 30 Z × 5 Y, đầu R2,5. Hai khe cạnh rộng 6 tại Z ±94, Y −155…145; plenum X 171…181, có lọc chung.
- Nóc: lỗ thoáng X −145…125, Z ±82; lọc nam châm. I/O hướng +Y, X 157, Y 190: power Ø14 Z −58; USB-A ×2, miệng 6 X × 13 Z tại Z −20/17; combo audio Ø3,5 Z 52. Không reset, USB-C hay mic rời.
- Đáy: cửa/lọc PSU 124 × 124 tâm X −112, Z −7. Lưng: I/O bo, F3, bốn PCIe, C14 và công tắc PSU. Lá I/O bo ở X −185,2…−184,4, Y 5…163, Z −67,4…−31,4; cửa nới 0,5 mỗi cạnh Y/Z.
- Khay dày 1: phần trên X −181…125, Y −92…188; phần dưới chỉ X −39…125, Y −187…−92 để tránh PSU. Cửa bảo trì CPU 100 × 100 tâm (−49; 104; −75,5), không dùng luồn dây.

Các G nằm trên Z −75,5; kích thước là lòng mở X × Y, viền cao su rộng 3, riêng G1/G3 là rãnh U mở lên mép Y188, viền cạnh dày 1.

| Lỗ | Tâm X; Y | Lòng mở | Tuyến |
|---|---|---|---|
| 02-G0 | 5; −137 | 60 × 28 | PSU → sau khay; đầu ATX24 đi qua cạnh 60 |
| 02-G1 | −109; 182 | 32 × 12 | EPS, luồn qua mép trước khi bắt bo |
| 02-G2 | 96; 88 | 24 × 60 | ATX24, hai quạt trước, dây I/O đi xuống |
| 02-G3 | −13; 182 | 36 × 12 | CPU_FAN |
| 02-G4 | 104; −28 | 28 × 28 | GPU8; vòng dây ở X >64 rồi trở về ổ +Z |
| 02-G5 | 40; −82 | 42 × 12 | USB19, F_PANEL |
| 02-G6 | −141; −82 | 38 × 12 | HD_AUDIO, quạt sau |

## Đầu cắm và vít

Tọa độ header sau đây là **cục bộ theo B**, không phải toàn máy; cao độ giữ theo mục 0/7 của đặc tả họ vỏ.

| Đầu | Tâm X; Y | Chân và khóa |
|---|---|---|
| ATX24 | 117; 30 | 24, 2×12; đầu 20+4, ngàm giữ |
| EPS8 | −60; 115 | 8, 2×4; đầu CPU 4+4, khác khóa GPU6+2 |
| CPU_FAN | 25; 115 | PWM4 có gờ dẫn |
| SYS1/2/3 | 106;79 / 81;−99 / −96;−103 | Mỗi đầu PWM4; không hub |
| USB19 | 113; −92 | 19 trên khung 20, một vị trí khóa |
| F_PANEL | 94; −113 | 9 trên khung 10, thiếu chân10; chỉ dùng PWR SW chân6/8, PLED +2/−4 |
| HD_AUDIO | −97; −113 | 9 trên khung10, thiếu chân8 |
| GPU power | Theo tọa độ toàn máy ở trên | 8, đầu6+2, ngàm; không tráo với EPS |

CPU dùng tiếp điểm LGA phẳng, tam giác/rãnh định hướng; 1.200 tiếp điểm là quy ước mô hình Gaming. DDR4 288 tiếp điểm/rãnh lệch; PCIe x16 164/chốt giữ; M.2 M-key 67 tiếp điểm trên khung75. Không header AIO_PUMP, ARGB hay Type-E. I/O bo: 2 USB-A USB2 (4 tiếp điểm/cổng), RJ45 (8), 3 TRS (3/jack). GPU: 3 DP (20/cổng), HDMI (19). I/O vỏ: USB3 ×2 (9/cổng), TRRS (4). C14 có 3 tiếp điểm, ghép C13; mọi đầu có hình khóa tương ứng.

| Cụm giữ | Số lượng, loại; mốc |
|---|---|
| Bo | 9 M3 × 5 + 9 chân đế M3 cao6,5; lỗ Ø3,4 tại B + các tổ hợp X/Y = −112/0/112 |
| Tản | 4 M3 × 12 lò xo; lưới78×78 quanh CPU; quạt dùng 2 kẹp thép Ø1,2 |
| Quạt case | 12 vít Ø5 × 10; mỗi quạt4 lỗ bước105×105 quanh tâm trên mặt YZ |
| GPU / nắp PCIe còn lại | 2 + 2 vít #6-32 × 6; hai vít GPU tái dùng từ hai nắp đã tháo |
| PSU | 4 #6-32 × 6, theo mesh PSU chung; mặt AC lùi3 so với lưng |
| SSD | 1 M2 × 2,5 và chân đỡ cao3 |
| Hai hông | 4 vít tay #6-32 × 6; mỗi bên tại X −169, Y ±158; trượt −X rồi nhấc ra |
| Cassette trước | 2 M3 × 6 tại (182; −170; ±84) + 6 ngàm |
| Shroud | 2 M3 × 6 tại (−145/90; −92; 89) |
| Chân | 4 M4 × 8, chìm tại tâm từng chân |

Tổng 46 vít giữ, 9 chân đế bo và 1 chân đỡ SSD; không tính vít trang trí trong cụm kín. Nóc, đáy, lưng cố định và lọc nam châm không có vít tháo.

## Vật liệu và cách đọc ảnh

Thép nhám **#24262A** (roughness0,65, metallic0,25); lưới **#101216**, dày0,8, lỗ Ø2,4 bước3,6 so le; cao su/nhựa **#202329**; dây **#15171B**. Kim loại linh kiện **#AEB4BC**, PCB **#16181D**, tiếp điểm **#C6A565**; USB xanh **#1F6FD1**, PLED trắng **#D2D7DD**. Lọc PET dày0,5 trong cassette nylon1,5. Không kính, RGB hay logo.

Phối cảnh tách lớp từ +X/+Y/+Z thể hiện hướng đưa linh kiện vào. FRONT +X: mặt đặc/khe hút; BACK −X: cổng, F3, PCIe và AC; LEFT/OPEN +Z: nội thất đã lắp sau khi tháo hông; RIGHT/CLOSED −Z: hông thép; TOP +Y: lỗ thoáng/I/O; BOTTOM −Y: lọc nguồn/chân. Inset sau khay chỉ đường dây; hình đầu cắm và bảng vít là chi tiết phóng đại.

### Người mới cần thấy

1. Phải tháo hông thép mới nhìn thấy linh kiện. Bo mATX không phải bo ATX thu nhỏ.
2. Hai quạt trước hút qua khe thấp/khe cạnh; một quạt sau xả. Nóc chỉ thoát thụ động. Quạt CPU/GPU/PSU không tính vào ba quạt case.
3. GPU240 nằm ngang, hai quạt hướng xuống, cắm thẳng vào x16. Lắp SSD trước GPU.
4. PSU vẫn là ATX, hút qua đáy riêng. Dây đi sau khay, không luồn qua cửa bảo trì CPU.
5. Tản155 cách hông trong8,8; giới hạn tản160. GPU mẫu cách quạt trước86; giới hạn GPU280 × 120 × 50 còn46. Đây là khoảng hở danh nghĩa, cần kiểm tra va chạm khi dựng mesh.

### Kiểm tra ảnh

Ảnh được tạo bằng imagegen tích hợp và sửa một lượt theo brief. Có đủ sáu mặt chiếu, hình tách lớp, hai RAM, GPU ngang, ba quạt case và nóc không quạt. Những sai khác còn lại:

- Mũi tên **155 mm** trên tản chưa bám đúng trục +Z từ IHS; mũi tên **8,8 mm** đang chỉ gần RAM thay vì mặt trong hông. Dựng theo bao tản và Z104 của hông trong; không đo theo hai đường này.
- Chú giải màu dây dưới inset ghi nhầm **GPU8 → G3**. G3 chỉ CPU_FAN; nguồn GPU đi G4. Dòng G0 trong ảnh mất tọa độ (5; −137); dùng bảng ở trên.
- BACK chưa thể hiện rõ hai vị trí đã thay nắp bằng lá GPU có cổng. Khi dựng chỉ giữ hai nắp trống, hai vít lá GPU và ba khoảng khe trống bên trong.
- Shroud bị minh họa quá sát PSU; phải đặt mặt trên Y−92 theo bảng. Backplate, khóa đầu cắm, số tiếp điểm và một số lỗ/vít nhỏ bị giản lược; không suy ra hình học chính xác từ inset.

Prompt tóm tắt: “Compact tower assembly reference, 210 W × 380 H × 370 D mm; opaque steel sides; mATX, tower air155, horizontal dual-fan GPU240, ATX PSU, two front120 + one rear120, no top fan; exploded three-quarter assembly, six orthographic views, cable routes, keyed connectors and screw schedule; white background, blue dimensions, realistic graphite, no logos.”
