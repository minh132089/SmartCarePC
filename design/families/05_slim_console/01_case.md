# 05 Slim console — Vỏ trống

Nguồn: [đặc tả sáu họ](../00_families_spec.md), mục 0, 5, 7 và 8; [concept](../../case-families/05_slim_console.png). Đơn vị **mm**; số trong tài liệu là chuẩn dựng mô phỏng, không đo ngược từ ảnh tạo sinh. Sheet chỉ gồm vỏ, panel/lọc, khay, chân đế bo, giá GPU/riser, giá SFX, I/O vỏ, dây AC, nắp PCIe và đế đứng. Không lắp linh kiện điện tử, quạt hoặc radiator.

## Kích thước và hệ trục

O ở tâm thân, không tính đế. X từ lưng (−) ra trước (+), Y từ đáy (−) lên nóc (+), Z từ hông phải (−) sang hông mở (+). Tọa độ ghi `(X; Y; Z)` ở trạng thái lắp; W × H × D tương ứng Z × Y × X. Quy đổi **100 mm = 1 đơn vị cảnh**.

- Thân **100 × 360 × 330**, X ±165, Y ±180, Z ±50; cạnh ngoài R6.
- Đế **140 × 18 × 280**, tâm `(0; −189; 0)`; toàn bộ khi đứng **140 × 378 × 330**.
- Khung thép M5 dày 1,2; panel dày 1, màu graphite `#343639`, roughness 0,55. Mặt trước R8.
- Lưới M2 dày 0,8, `#101216`: lỗ Ø2,4, bước 3,6 so le; mép đặc 6, vùng tránh vít R5. Lỗ phải thông, không dùng chấm sơn giả lỗ.
- Lọc PET dày 0,5, mắt mở 0,35/sợi 0,15; viền nylon 1,5, nằm trong bao thân. Cao su/nhựa `#202329`, dây `#15171B`, kim loại sáng `#AEB4BC`, tiếp điểm `#C6A565`, lưỡi USB-A `#1F6FD1`.

## Các phần và vị trí

| Số trên ảnh | Phần | Bao và mốc lắp |
|---|---|---|
| 1 | Khung | Thân như trên. Nóc, đáy và lưng cố định, 0 vít tháo; không có giá quạt/két |
| 2 | Hai hông | 314 X × 340 Y × 1; X ±157, Y ±170; Z 49…50 và −50…−49. Cửa lỗ X ±142, Y ±155, lọc phía trong. Hai vít tay mỗi nắp tại X −151, Y ±150; trượt −X rồi nhấc ±Z |
| 3 | Cassette trước | X 153…165, Y ±174, Z ±45; 90 Z × 348 Y × 12 X, R8, lưới trừ vùng I/O thấp. Bốn ngàm và 2 M3 × 6 tại `(162; ±159; 0)` ẩn dưới mép |
| 4 | Khay ITX | X −158…43, Y −174…38, Z −37,7…−36,5. Chỉ ở tầng dưới, không che tầng GPU. Cửa CPU 96 × 96 tâm `(−86; −53; −36,5)` |
| 4 | Chân đế bo | 4 M3 cao 6,5, tâm là bốn tổ hợp X −141/9, Y −140/10; từ mặt khay Z −36,5 đến mặt lưng bo dự kiến Z −30. Tương ứng lỗ bo Ø3,4; bo không xuất hiện trong sheet |
| 5 | Ba nắp PCIe | Dọc Y 38…158; dãy Z −17,7…43,26, bước 20,32. Mỗi nắp rộng 18,82, khe giữa 1,5; giữ đủ 3 nắp và 3 vít khi vỏ trống |
| 6 | Giá GPU/riser | X −161…94, Y 34…36, Z −3…15; 2 M3 vào khung. Tai adapter riser cách nhau 100; để trống adapter và dây riser |
| 7 | Giá SFX | Thép chữ L dày 1,2, mặt đỡ AC X 47; khoét thoáng mặt xả và C14. Bốn lỗ vít trục X: `(47; −145/−35; −25/+25)`, đủ bốn tổ hợp, tính từ phép quay R− của nguồn |
| 8 | Gá đế | 2 M4 × 10, trục Y, tại `(−85; −180; 0)` và `(85; −180; 0)`, cách tâm 170. Đế có hai ray dọc X, hai cầu nối X ±85, đệm 1 mỗi bên lòng đỡ |
| 9 | Module trước | X 153…164; các miệng hướng +X tại X 165, Z 0. Chi tiết bên dưới |
| 10 | Dây AC của vỏ | C14 lưng tâm `(−165; −166; 28)`; dây bọc kín dài tâm 450 men đáy Y −171. Đầu C13 góc ở mặt nguồn dự kiến X 47, bao X 22…47; tâm ghép suy ra `(47; −47; 9)`. Không có nguồn trong sheet |

Cửa nóc: X −140…130, Z −34…34 (**270 × 68**). Hai cửa đáy: X −138…−20 và 20…138, Z ±30 (**118 × 60 mỗi cửa**). Gá M4 cần tai/gân cục bộ; giữ thông vùng cửa, không biến đế thành tấm bịt toàn đáy. Lưới/lọc không dùng vít tháo.

Cửa I/O bo ở lưng X −165, Y −144,5…14,5, Z −28,9…8,1, lòng **159 Y × 37 Z**. Đây là cửa trống; không vẽ các cổng của bo. Rãnh đỡ dành cho lá I/O tại X −165…−164,2. Ba lá PCIe có gờ đỡ riêng: mặt lá GPU dự kiến X −162…−161, không ép chung với mặt lưng X −165. Tai vít GPU tham chiếu tại `(−162; 153; −7,54/12,78)`; vị trí thứ ba tiếp bước 20,32. Vít lá hướng Y sau khi quay R−.

Tâm vít giữ giá GPU vào khung, kích thước khoét riêng cho module C14 và kiểu giữ module chưa được nguồn chốt. Khi dựng phải khớp chi tiết gá/module đã chọn, không lấy nét ảnh làm lưới khoan. Bốn lỗ SFX và cửa I/O ở trên là số tính từ nguồn, không phải đo từ ảnh.

## I/O và đầu dây

| Cổng/nút tại X 165, Z 0 | Y | Miệng / đầu nối và khóa |
|---|---:|---|
| Power | −92 | Ø12; PWR SW 2 chân, không phân cực → F_PANEL 5/7 của bo mô phỏng; không reset |
| USB-A | −119 | 13 Y × 6 Z; 9 tiếp điểm → USB19, khung 20 vị trí thiếu 1 làm khóa. Một nhánh dùng, nhánh còn lại bịt trong module |
| USB-C | −143 | 8,4 Y × 2,6 Z; 24 tiếp điểm → Type-E 20 có khóa định hướng, không ghép USB19 |
| Headphone combo | −160 | Ø3,5; TRRS 4 tiếp điểm → HD_AUDIO 9 trên khung 10, khuyết chân 8 |
| AC ở lưng | −166 | IEC C14 ↔ C13, 3 tiếp điểm L/N/PE; vỏ đầu có khóa hình dạng, dây cách điện kín |

F_PANEL bo có 9 chân trên khung 2 × 5, khuyết chân 10. Không tự thêm reset, HDD LED hoặc đèn RGB vào vỏ. Đầu cắm được mô tả để dựng module/dây vỏ; bo và PSU vẫn vắng mặt.

## G0–G6 trên khay

Tất cả ở Z **−36,5**. Kích thước là lòng mở X × Y; thêm viền EPDM 3, không trừ viền vào lòng mở.

| Mã | Tâm X; Y | Lòng mở | Dùng cho |
|---|---|---|---|
| 05-G0 | 31; −135 | 18 × 56 | Bó PSU vào rãnh cạnh |
| 05-G1 | −121; 29 | 28 × 10 | EPS |
| 05-G2 | 31; −62 | 18 × 58 | ATX24 |
| 05-G3 | −38; 29 | 32 × 10 | CPU_FAN |
| 05-G4 | −68; −166 | 96 × 10 | Riser xuống sau khay, vòng lên khe giữa hai tầng |
| 05-G5 | 2; −166 | 22 × 10 | USB/Type-E/F_PANEL |
| 05-G6 | −143; −166 | 22 × 10 | Audio |

Khoang dây sau khay Z −49…−37,7 sâu **11,3**; xếp dây dẹt một lớp. Bó lớn đi X 25…43. Hành lang trước X 148…162; tại module I/O dành Z −40…−33 cho dây nguồn. Riser đi qua G4 với R≥15, dây nguồn mềm R≥12. Cửa CPU không phải lối đi dây.

## Vít và trạng thái vỏ trống

| Nhóm | Số lượng / loại | Trạng thái |
|---|---|---|
| Panel | 4 vít tay, 2 M3 × 6 trước | Giữ panel hoặc tách cùng panel ở góc 3/4; nguồn chưa chốt ren vít tay |
| PCIe | 3 #6-32 × 6 | Giữ đủ ba nắp; khi lắp GPU tái dùng hai vít |
| Đế | 2 M4 × 10 | Qua tai gá, đầu chìm |
| Giá GPU | 2 M3 | Gắn khung, nguồn chưa chốt chiều dài |
| Bo | 4 chân đế M3 cao 6,5 | Chân đế lắp sẵn; 4 M3 × 5 chỉ dùng khi có bo |
| SFX | 4 M3 × 6 | Lỗ chờ trên giá; vít dùng khi lắp nguồn |
| Khung, lọc, quạt/két | 0 vít tháo khung/lọc; 0 gá quạt/két | Không vẽ quạt hoặc radiator |

## Các hình chiếu

Sáu hình chiếu cùng tỷ lệ, panel và đế ở trạng thái lắp. Chỉ hình 3/4 tách hai hông và cassette trước theo hướng tháo.

| Nhãn | Hướng nhìn / nội dung |
|---|---|
| FRONT +X | Nhìn −X; +Z bên trái ảnh. Lưới hẹp, bốn nút/cổng thấp, thân 100 × 360 và đế rộng 140 |
| BACK −X | Nhìn +X; +Z bên phải. Ba nắp dọc phía trên, cửa I/O trống phía dưới, C14 sát đáy |
| LEFT / OPEN +Z | Nhìn −Z; trước +X bên phải. Hông đục lỗ chính, hai vít mép sau; “OPEN” là tên phía tháo, panel vẫn lắp |
| RIGHT / CLOSED −Z | Nhìn +Z; trước +X bên trái. Hông đục lỗ đối diện, hai vít sau đảo trái/phải đúng hướng nhìn |
| TOP +Y | Nhìn −Y; +X xuống dưới, +Z sang phải. Cửa 270 × 68 và cassette trước |
| BOTTOM −Y | Nhìn +Y; +X lên trên, +Z sang phải. Hai cửa 118 × 60, hai điểm M4 cách 170 và khe giữa ray đế |
| 3/4 | Từ +X/+Y/+Z, tách hai hông và mặt trước; thấy khay thấp, 4 chân đế, cửa CPU, G0–G6, giá GPU/riser và SFX trống |

## Người mới cần chú ý

1. Ba khe PCIe dọc ở tầng trên; tầng dưới dành bo ITX. Khi lắp máy cần riser, không cắm GPU xuyên qua bo.
2. Hông là thép đục lỗ có lọc, không phải kính; +Z là mặt hút chính. Vỏ này không có quạt case hay radiator.
3. Đế chống đổ phải bắt đủ hai M4. Gá đế không được bịt kín cửa thoáng đáy.
4. Đặt ngang phải tháo đế, gắn bốn chân 12 × 12 × 8 ở `(±137; ±148; −54)`, keo định vị, 0 vít; mặt hút +Z hướng lên. Mặt bàn Z −58, tổng 108 Z × 360 Y × 330 X. Tư thế này không thêm thành view thứ tám của sheet.
5. USB-C dùng Type-E; đầu ATX đi theo cạnh dài G0/G2. Không nhét đầu dây dày vào khoang sau khay 11,3 hoặc luồn qua cửa CPU.

## Kiểm tra ảnh và lệnh tạo

Ảnh được tạo bằng **imagegen tích hợp**, đã tạo lại một lần theo brief. Bản cuối đủ sáu hình chiếu, một góc 3/4 mở vỏ, ba nắp PCIe dọc và bảng G0–G6. Ảnh vẫn có các sai lệch dưới đây; dùng số và hướng ở tài liệu này để dựng:

- Tỷ lệ pixel giữa các view chưa hoàn toàn đồng nhất; view trước/lưng hơi hẹp, mặt nóc/đáy nhỏ hơn. Chữ 100/140 ở mặt trước bị đảo mô tả: **100 là thân, 140 là đế**. Nhãn 310 ở LEFT chỉ vùng lỗ, không phải panel 340.
- Mũi tên FRONT dưới BOTTOM đang chỉ xuống; đúng phải **lên trên**. Hình nóc/đáy giản lược ray đế, chưa thể hiện rõ đủ hai lỗ M4 cách 170. Không suy ra vị trí lỗ từ các chấm vít trên ảnh.
- Khay ở hình 3/4 đã đứng nhưng bị thu ngắn và thiếu cửa CPU/G0–G6. Inset khay có chân đế thừa và nhãn sai: góc trên phía trước phải là **G3**, đáy giữa phía trước phải là **G5**. Chỉ dùng **4 chân đế** đúng tọa độ, không sao chép sáu chấm màu vàng trong inset. Bảng G0–G6 bằng chữ trên ảnh đúng số.
- Ổ AC trong góc 3/4 còn bị đặt ở phía trước; đúng phải ở lưng **X −165**. Đầu tự do bên giá SFX là C13; không thêm C14 thứ hai ở mặt trước.
- Các hình đầu USB là ký hiệu minh họa, có hình trông như đầu đực; trên mặt vỏ phải dựng **ổ cái**. Kích thước cửa I/O 159 × 37 và vị trí bốn cổng trước theo bảng, không theo tỷ lệ hình.

**Prompt dùng để tạo, tóm tắt:** “05 SLIM CONSOLE / 01 CASE; EMPTY CHASSIS; graphite perforated enclosure 100 W × 360 H × 330 D; six labelled orthographic views at one scale plus an exploded 3/4; narrow mesh front, three upper vertical PCIe blanks, lower vertical ITX tray, G0–G6, empty SFX/GPU brackets, two M4 stand mounts, front I/O and AC extension; white background, thin blue dimensions, short English callouts, no installed components.” Lần sửa yêu cầu khay đứng, đồng nhất tỷ lệ, nóc/đáy đúng trục, C14 ở lưng và đầu PWR SW hai chân hình chữ nhật; các điểm còn chưa đạt đã nêu ở trên.
