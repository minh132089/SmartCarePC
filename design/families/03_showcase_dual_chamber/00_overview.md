# 03 Showcase hai khoang — Tổng thể

Nguồn: [đặc tả họ vỏ](../00_families_spec.md), mục 0, 3, 7; [quy ước Gaming](../../gaming/00_blueprint.md); [concept](../../case-families/03_showcase_dual_chamber.png). Ảnh: [00_overview.png](00_overview.png). **Số trong tài liệu là chuẩn dựng mô hình; không đo ngược ảnh tạo sinh.** Đây là thiết kế mô phỏng, chưa phải bản gia công hay xác nhận tương thích sản phẩm thật.

## Kích thước và mốc lắp

Đơn vị mm; 100 mm = 1 đơn vị cảnh. O ở tâm thân, không tính chân. X: lưng (−) → trước (+); Y: đáy (−) → nóc (+); Z: hông kín (−) → kính (+). W × H × D = Z × Y × X. Các tọa độ dưới đây thuộc trạng thái **đã lắp**, không phải vị trí tách lớp trên ảnh.

Thân **300 × 450 × 450**, X/Y ±225, Z ±150; tổng có chân **300 × 470 × 450**. Vách trước Z −36,5, sau −37,7; khoang phụ Z −149…−37,7, khoang trưng bày Z −36,5…146.

| Số / cụm | Kích thước và vị trí lắp |
|---|---|
| 1 Vỏ | Khung dày1,2, panel thép1; bốn chân 50 X × 20 Y × 36 Z, tâm (±180; −235; ±112). Khay mang bo X −221…74. |
| 2 Bo ATX | PCB 244 X × 305 Y × 1,6 Z; B = (−89; 25; −30) tại lưng PCB; linh kiện +Z, I/O −X. Chín chân đế cao6,5. |
| 3 CPU | 37,5 × 37,5 × 4,5; đáy (−70; 84; −25,3), IHS (−70; 84; −20,8). |
| 4 AIO240 | Bơm Ø54 × 48 từ IHS đến Z27,2. Két 277 X × 27 Y × 120 Z, gốc giữa mặt trên (−15; 219; 65); bao X −153,5…123,5, Y192…219, Z5…125. Hai quạt dưới két Y167…192. Hai ống Ø11 dài tâm350, R≥30, đầu két (114; 179; 50/80). Backplate90×90, gá78×78. |
| 5 RAM ×2 | DDR4 133,35 Y × 42 Z × 6 X; gốc đáy A2 (−10; 87; −24,5), B2 (9; 87; −24,5); hai khe còn lại trống. |
| 6 GPU280 | Gốc A = (−81; −111; 86); xoay R−(x;y;z)=(x;z;−y). Thân X −221…59, Y −111…14, Z58,5…113,5; ba quạt đen hướng +Z, chiều dài280 chạy theo X. |
| 7 SSD | Một M.2 2280, 80 X × 22 Y × tối đa3; tâm XY (−54; −37), miệng (−94; −37), vít (−14; −37); cao độ và chân đỡ theo mesh Gaming. |
| 8 PSU ATX | Xoay R+(x;y;z)=(x;−z;y); tâm (−152; −95; −94). Bao X −222…−82, Y −170…−20, Z −137…−51. Hút −Z, xả −X; dây liền ra +X. Nguồn đứng sau khay. |
| 9 Quạt case | Sáu quạt120×120×25 theo bảng dưới; không tính lại hai quạt két. |
| 10 Dây | Nguồn ATX600/EPS650/GPU600, I/O và quạt tối đa650; bó dư sau khay. Rãnh rộng30 tại X54 và rãnh dưới Y−180, tám đai; dây nguồn R≥20, cách cánh≥10. |
| Riser400 | Dải mềm400×70×1,5, đầu đực89×12×1,6; đầu cái110×18×14, hai tai cách100. Đầu bo (−123; −5; −19,6), đầu GPU (−123; −115,2; 61,3). Vòng chùng trong X −172…−76, Y −166…−8, Z −15…55; R≥15, tránh F1. |
| Hub8 | Hộp80 X × 50 Y × 18 Z, tâm (85; −148; −85), tai tổng92, lỗ lệch X±43. Adapter ATX24 nối tiếp25×58×42 tại header, dây cấp hub650. |

| Quạt | Tâm (X; Y; Z) | Gió / điều khiển |
|---|---|---|
| F1/F2/F3 đáy | (−132/−10/112; −196,5; 63) | Hút đáy, thổi +Y; PWM vào hub |
| F4/F5 bên | (142; −45/80; −22,5) | Hút hông −Z qua ống riêng, thổi +Z; PWM vào hub |
| F6 sau | (−209,5; 97; 77) | Xả −X; PWM vào hub |
| R1/R2 két | (−75/45; 179,5; 65) | Xả +Y; chia Y vào CPU_FAN, chỉ một nhánh trả tach |

**Tám vòng cyan = 3 đáy + 2 bên + 1 sau + 2 két.** Ba rotor GPU và một rotor PSU là nhóm riêng. Mặt quạt GPU cách kính trong **32,5**; giới hạn GPU330 dài ×130 cao ×60 dày vẫn cách kính30. Nóc chỉ AIO240, tổng dày52. Quạt đáy cách đáy GPU73; quạt két cách đỉnh quạt bên27.

## Panel, cửa, cổng và đường dây

- Kính hông: X −215…220, Y ±211, Z146…150. Kính trước: X221…225, Y ±211, Z −24…145; dải phía −Z bằng thép. Hai kính gặp nhau bằng gioăng1, không cột dày và không hút gió qua kính.
- Hông kín Z −150…−149: cửa PSU X −215…−85/Y −170…−20; cửa cấp gió bên X78…207/Y −112…147. Ống riêng thành dày1 chạy Z −148…−35, không bị khay kín hoặc bó dây bịt.
- Nóc: cửa lưới X −190…180/Z −8…133. Đáy: X −197…187/Z −1…129, lọc kéo +X. Lọc nằm trong bao thân.
- I/O nóc: X188, Y225; power Ø14 tại Z−116, reset Ø4 tại −89, hai USB-A miệng6 X ×13 Z tại −65/−42, combo Ø3,5 tại −17. Module X180…196/Y209…223/Z −128…−9. Không USB-C mặt trước.
- Lưng: lá I/O bo X −225,2…−224,4, Y7…165, Z −28,4…7,6; cửa nới0,5 mỗi cạnh Y/Z. Ba khe PCIe dọc dài Y −113…7, dãy Z55,3…116,26, bước20,32. Hai vị trí cho lá GPU, khoảng thứ ba để trống phía trong. Giá GPU X −221…79/Y −117…−115/Z75…93, đệm tại X45. Không có dãy bảy khe ngang.
- Cửa CPU100×100 tâm (−70; 84; −36,5), không dùng đi dây. PSU không che cửa này. Chân PCIe GPU nhô8, lá cổng nhô1 và đầu vít tối đa3 ngoài bao thân card đã ghi.

Các G ở Z−36,5, lòng mở X×Y, viền EPDM rộng3:

| Lỗ | Tâm X; Y | Lòng mở | Tuyến |
|---|---|---|---|
| 03-G0 | −49; −170 | 60×28 | PSU → rãnh; đầu ATX24 đi qua cạnh60 |
| 03-G1 | −149; 193 | 34×14 | EPS |
| 03-G2 | 54; 79 | 26×65 | ATX24 |
| 03-G3 | −30; 193 | 44×14 | Bơm/quạt két |
| 03-G4 | 54; −20 | 24×30 | GPU8; vòng ở X80…105 tới ổ (44; 14; 67,45), hướng +Y |
| 03-G5 | 6; −145 | 46×14 | USB/F_PANEL |
| 03-G6 | −183; −145 | 40×14 | Audio |

## Đầu nối và vít

Header bo dùng **tọa độ cục bộ theo B**, cộng B để ra toàn máy:

| Đầu | Tâm (X; Y; Z) cục bộ | Chân / khóa |
|---|---|---|
| ATX24 | (117; 40; 14,6) | 2×12, đầu20+4, ngàm; adapter nối tiếp đủ24 đường |
| EPS | (−60; 146; 14,6) | 8 chân, CPU4+4, không tráo GPU6+2 |
| CPU_FAN / AIO_PUMP | (35/10; 126; 9,6) | Mỗi đầu PWM4 có gờ dẫn; quạt két/bơm tương ứng |
| SYS1 | (106; 85; 9,6) | Vỏ4 vị trí, dây hub chỉ PWM/tach/GND; không trả12V về bo |
| SYS2/3/4 | (82; −128; 9,6)/(95; −134; 9,6)/(−96; −135; 9,6) | PWM4, để trống trong cấu hình này |
| USB19 | (113; −127; 10,6) | 19 trên khung20, khóa chân20 |
| F_PANEL | (95; −143; 9,1) | 9 trên khung10, thiếu10; PWR6/8, reset5/7, PLED+2/−4; không thêm HDD LED vỏ |
| HD_AUDIO | (−97; −143; 9,1) | 9 trên khung10, thiếu8 |
| ARGB | (55; −143; 9,1) | +5V/D/khuyết/GND; dây điều khiển hub chỉ DATA/GND |

Hub có 8 cổng PWM4 và 8 ARGB3, dùng6 PWM +8 ARGB; port1 duy nhất trả tach. Nguồn hub lấy12V/5V/GND từ adapter ATX24, nhánh thiết kế ≥3A ở12V và ≥2,5A ở5V, bảo vệ từng rail; **không nối +5V bo vào rail đèn hub**. Mỗi vòng ≤0,25A, mỗi motor case ≤0,2A. Hai ống AIO là ống nước, dây điện bơm đi sát ống rồi vào G3.

LGA có tiếp điểm phẳng, tam giác/rãnh khóa (1.200 tiếp điểm là quy ước mô hình); DDR4 288/rãnh lệch; x16 164/chốt khóa; M.2 M-key67 trên khung75. GPU8 dùng6+2 và ngàm. GPU xuất3 DP20 +1 HDMI19. I/O bo giữ mesh hiện tại: HDMI19, DP20, sáu USB-A (hai USB2/4 tiếp điểm, bốn USB3/9), RJ45/8, USB-C/24, hai RP-SMA, hai TRS/3, cổng quang, hai nút BIOS/CMOS. USB-C sau bo không phải header Type-E; không thêm Type-E. I/O vỏ có hai USB3/9 và TRRS/4. C14 nguồn có3 tiếp điểm ghép C13, công tắc ở mặt AC −X.

| Cụm giữ | Số lượng / loại / mốc |
|---|---|
| Bo | 9 M3×5 +9 chân đế M3 cao6,5, lỗØ3,4 khớp mesh ATX hiện tại |
| Bơm | 4 M3×12 lò xo, tại (−70±39; 84±39), trụcZ; backplate có đệm cách điện |
| Két / quạt két | 8 M3×6 từ ray nóc /8 M3×30 từ dưới quạt; mỗi tâm quạt có4 lỗ lệch±52,5 theo X/Z; 16 đệm1, bậc ray hiệu dụng3 |
| Sáu quạt case | 24 vítØ5×10; bốn lỗ bước105×105 mỗi quạt, trong mặt vuông góc trục gió |
| GPU / nắp còn lại | 2 +1 vít #6-32×6; hai vít GPU tái dùng từ nắp tháo |
| Giá GPU / hub | Mỗi cụm2 M3; hub tại X42/128, Y−148; chiều dài vít giá/hub chưa chốt trong nguồn |
| PSU | 4 #6-32×6 khớp mesh ATX đã xoay; mặt AC X−222 |
| SSD | 1 M2×2,5, chân đỡ cao3 |
| Kính hông | 2 M3×6 ẩn ở (−185/190; 214; 141) +2 chốt đáy; không xuyên mặt kính |
| Kính trước | 2 M3×6 ở (218; 214; −12/132) +2 chốt đáy |
| Nóc | 2 M3×6 ở (−213; 225; −15/132) +ngàm trước; giá két độc lập |
| Nắp kín | 2 vít tay tại X−209, Y±190; trượt −X rồi nhấc |
| Chân | 4 M4×8 chìm tại bốn tâm chân |

Lưng, đáy cố định và lọc không có vít tháo. Lỗ bo/PSU giữ đúng mesh dùng lại, không suy ra mẫu khoan thương mại từ hình. Vít trang trí của cụm kín không phải điểm tháo.

## Vật liệu và cách đọc sheet

Thép nhám **#24262A**, roughness0,65/metallic0,25; lưới **#101216** dày0,8, lỗØ2,4 bước3,6 so le. Kính khói **#B8C4CA** dày4, IOR1,5, roughness0,08, truyền sáng mục tiêu75%, viền men **#101216** rộng10/gioăng1. Cao su **#202329**, dây **#15171B**, PCB **#16181D**, kim loại **#AEB4BC**, tiếp điểm **#C6A565**, cyan **#22D3EE**. Lọc PET0,5/cassette nylon1,5. Vòng đèn đứng yên, cánh quay riêng; không logo.

Hình tách lớp nhìn từ +X/+Y/+Z cho hướng lắp và đủ BOM. FRONT +X nhìn−X: kính trước/dải khoang phụ; BACK −X nhìn+X: I/O, F6, ba PCIe dọc, AC; LEFT/OPEN +Z nhìn−Z: máy đã lắp qua kính; RIGHT/CLOSED −Z nhìn+Z: nắp, cửa hút và khoang dây được minh họa mở; TOP +Y nhìn−Y: lưới/két/I/O; BOTTOM −Y nhìn+Y: ba vị trí quạt, lọc, chân. Các hình chiếu dùng cùng tỷ lệ danh nghĩa; chi tiết đầu nối/vít được phóng đại.

### Người mới cần thấy

1. Kính trước không hút gió. Hai quạt bên lấy khí qua hông kín và ống riêng; ba quạt đáy hút lên, quạt sau và nóc xả.
2. PSU đứng sau bo, không có nguồn hoặc shroud dưới ba quạt đáy. Dây dư và hub nằm ở khoang phụ.
3. GPU dọc vẫn dài ngang theo X; mặt ba quạt quay ra kính. Cần riser400 và giá đỡ, không cắm card xuyên bo.
4. Có6 quạt case và2 quạt két, tổng8 vòng cyan. PWM và ARGB là hai đường riêng; hub cần nguồn từ adapter ATX24.
5. Lắp CPU, RAM, SSD và gá bơm trước GPU; giữ hai đầu ống nối thật, không kẹp dây khi đóng kính. Khe hút giữa GPU và kính phải giữ32,5.

### Kiểm tra ảnh và sai khác còn lại

Ảnh dùng imagegen tích hợp, đã sửa một lượt theo brief. Hình chính có đủ8 quạt cyan, GPU ba quạt dựng dọc, riser, hub, PSU sau khay; có sáu khung hình chiếu. Những chi tiết sau phải dựng theo số ở trên:

- FRONT còn vẽ thừa I/O trên dải đứng; I/O vỏ chỉ ở **nóc**, X188/Y225. Các số176/124 trên ảnh là chia vùng minh họa, không thay biên kính Z−24…145 hay vách Z−36,5.
- Sáu inset chưa đồng nhất tỷ lệ thực dù tiêu đề ghi “same scale”. LEFT còn thấy mặt quạt sau và mặt quạt nóc/đáy theo phối cảnh; BOTTOM giản lược ba cửa quạt. Không dùng inset làm ảnh nền để đo mesh.
- PSU trong hình tách lớp vẫn chưa phân biệt rõ hai mặt vuông góc: hút −Z, AC/công tắc −X. RIGHT đã thể hiện quạt nguồn phía hông; đó là hướng đúng.
- Kính hông bị thêm điểm giữ ở đáy giống vít xuyên kính. Thực tế dùng2 chốt đáy và2 vít ẩn phía trên; kính trước cũng vậy. Kích thước/hướng tách kính chỉ mang tính trình bày.
- CPU mang nhãn thừa “LGA1700/AM5”; bỏ nhãn này khi dựng, chỉ dùng socket mô phỏng chung đã chọn. Không suy ra khả năng tương thích socket thật. Pump/CPU tách lệch để nhìn thấy, mốc lắp phải đồng tâm (−70;84).
- Bảng vít chưa ghi riêng vít nắp PCIe thứ ba; lấy bảng trong tệp này. Khóa, tiếp điểm, backplate, nhánh adapter ATX24 và một số lỗ nhỏ còn giản lược. Icon M.2 ở dải dưới chưa rõ M-key; giữ đúng mesh SSD chung.

Prompt sử dụng, bản rút gọn: “03 showcase dual-chamber PC assembly reference, body300W×450H×450D, total470H; large exploded three-quarter assembly and six orthographic views. ATX, AIO240, two DDR4, one M.2, vertical GPU280 with400mm riser, upright PSU behind tray, powered8-port hub. Exactly3 bottom+2 side+1 rear+2 radiator cyan fans. White background, graphite materials, blue dimensions, keyed connectors, G0–G6 and screw schedule, no logos.” Lượt sửa yêu cầu sửa FRONT/TOP, mặt hút PSU, tách CPU và hai kính, sửa G0 và icon M.2; các sai khác còn lại đã liệt kê phía trên.
