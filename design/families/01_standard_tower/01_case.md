# 01_standard_tower / 01_case — Vỏ trống

Ảnh: [01_case.png](01_case.png). Chuẩn số liệu: [00_families_spec.md](../00_families_spec.md), mục 0, 1 và 8; quy ước đầu cắm/vít theo [Gaming](../../gaming/00_blueprint.md). Đây là kích thước thiết kế mô phỏng. **Số trong tài liệu là chuẩn để dựng, không đo ngược từ ảnh.**

## Kích thước và trục

Đơn vị mm; 100 mm = 1 đơn vị cảnh. O ở tâm thân, không tính chân. X từ lưng −X ra trước +X; Y từ đáy lên nóc; Z từ hông kín −Z ra kính +Z. Thân **230 Z × 460 Y × 430 X**, biên X ±215, Y ±230, Z ±115. Tính chân: **230 × 478 × 430**; chưa cộng đầu vít nhô tối đa 2, riêng vít kính 3.

Case trống giữ khay, chín chân đế bo, shroud, G0–G6, ray gá, lọc, I/O vỏ, bảy nắp PCIe và bốn chân. Không lắp bo, nguồn, GPU, CPU, RAM, SSD, tản, quạt hoặc két.

## Các phần vỏ

| Số trong ảnh | Phần / vị trí đã lắp | Chi tiết |
|---|---|---|
| 1 | Cassette trước: X 201…215, Y ±215, Z −109…89 | 198 rộng × 430 cao × 14 sâu; lưới X 211…211,8; cửa hữu dụng Z −84…82, Y ±200; lọc X 208,5; sáu ngàm |
| 2 | Kính: X ±205, Y ±218, Z 111…115 | 410 × 436 × 4; men đen viền 10, gioăng 1; tháo +Z |
| 3 | Hông kín: X ±205, Y ±218, Z −115…−114 | 410 × 436 × 1, gờ gập; trượt −X rồi nhấc −Z |
| 4 | Khay mặt trước Z −89,5, dày 1 | Phần trên X −211…145, Y −129…227; phần dưới **chỉ** X −69…145, Y −227…−129 để chừa PSU |
| 4 | Chín chân đế M3 cao 6,5 | Mọi tổ hợp X = −191/−79/33, Y = −97,5/45/187,5; từ Z −89,5 tới −83; khớp lưới bo mô phỏng Ø3,4 |
| 4 | Cửa bảo trì CPU | 100 X × 100 Y, tâm (−60;104;−89,5); không phải cửa dây |
| 5 | Che PSU | Mặt trên Y −129, X −211…145, Z −89,5…109, dày 1; vách kính Z 108…109; đầu +X hở |
| 6 | Bảy nắp PCIe ngang ở lưng | Bước 20,32; mép trên hàng i = 21 − 20,32i, i=0…6; nắp cao 18,82, khe cách 1,5; dãy tới Y −121,24 |
| 7 | Nóc cố định | Cửa lưới X −168…142, Z ±88 (310 × 176); lọc nam châm, không gân chữ X |
| 7 | Đáy cố định | Hút PSU 124 X × 124 Z, tâm (−142;−230;−15); lọc kéo về −X |
| 8 | Bốn chân | 42 X × 18 Y × 28 Z; tâm (±175;−239;±85) |
| 9 | Module I/O trước | X 202…213, Z 91…109; cửa cắm ở X 215, ngoài lưới về +Z |

Khoang dây Z −114…−90,5 sâu **23,5**; hai rãnh rộng 26 tại X −185 và 83, sáu đai (hai mỗi rãnh, hai dưới khay). Dây I/O đen dài tối đa 650, đi sau khay; phần dư ở khoang nguồn.

## Cửa lưng và tâm gá

| Vùng | Tọa độ / lỗ gá, chỉ để trống trên sheet |
|---|---|
| I/O bo | Lá bo tương lai X −215,2…−214,4, Y 27…185, Z −81,4…−45,4. Cửa nới 0,5 mỗi cạnh Y/Z: **159 Y × 37 Z**, Y 26,5…185,5, Z −81,9…−44,9; có rãnh đỡ riêng |
| PCIe | Lá giữ theo GPU chung: X −212…−211, Z −70,4…49,6; vít trục X ở Z 44,6, Y = 10,84 − 20,32i. Làm cổ gấp từ lưng X −215 tới lá, không dịch mốc bo |
| PSU ATX | Bao lắp X −212…−72, Y −220…−134, Z −90…60; cửa danh nghĩa 150 Z × 86 Y. Gờ gá mặt X −212; bốn tâm (Y;Z) = (−214;−84), (−214;54), (−140;−84), (−150;48), theo mesh nguồn chung |
| Hai quạt trước 140 | Tâm quạt tương lai (187,5;−72;10), (187,5;74;10); mặt gá X 200. Bốn lỗ mỗi cụm: Y = tâm ±62,25, Z = 10 ±62,25, bước 124,5 |
| Quạt sau 120 | Tâm tương lai (−199,5;128;28); mặt gá X −212. Bốn lỗ Y = 128 ±52,5, Z = 28 ±52,5, bước 105 |
| Ray AIO 240 tùy chọn | Gốc mặt trên két (−11,5;224;35); tám vị trí gá X = −124/−19/−4/101, Z = −17,5/87,5. Ray có bậc dày hiệu dụng 3 tại vít; không vẽ két/quạt đã lắp |

Các tọa độ cửa/vít suy ra từ mốc linh kiện dùng lại, không phải mẫu khoan ATX thương mại. Lỗ thông thoáng là khoảng rỗng thật, có viền đỡ; không dùng tấm kín chấm màu để giả lưới.

## G0–G6

Tiền tố đầy đủ **01-G0…01-G6**; tất cả trên khay Z = −89,5. Kích thước là lòng mở X × Y, cộng viền EPDM rộng 3 ra ngoài.

| Mã | Tâm (X;Y;Z) | Lòng mở | Mục đích |
|---|---|---|---|
| G0 | (−32;−174;−89,5) | 60 × 28 | Nguồn vào sau khay; luồn đầu ATX24 qua cạnh 60 |
| G1 | (−139;212;−89,5) | 34 × 12 | EPS qua mép trên bo |
| G2 | (65;102;−89,5) | 24 × 64 | ATX24, dây quạt trước |
| G3 | (−40;212;−89,5) | 42 × 12 | Dây tản/quạt, dùng lại khi thay AIO |
| G4 | (89;−9;−89,5) | 28 × 30 | Nguồn GPU; kẹp ở (92;−25;55), vòng ngoài đầu +X của card |
| G5 | (23;−120;−89,5) | 46 × 12 | USB19 và F_PANEL |
| G6 | (−176;−120;−89,5) | 40 × 12 | HD_AUDIO và quạt sau |

G4 nằm trên khay, không dùng ống nổi trên shroud của Gaming. Khi lắp máy, uốn dây nguồn R ≥20 và giữ cách cánh quạt ≥10.

## I/O và đầu dây thuộc vỏ

Tất cả cổng/nút nhìn ra +X, tâm X=215, Z=101; từ trên xuống:

| Chi tiết | Y tâm / miệng | Đầu nối, khóa |
|---|---|---|
| Power | 178 / Ø14 | PWR SW 2 vị trí, không phân cực; vòng PLED dùng 2 vị trí có +/− |
| Reset | 147 / Ø4 | RESET SW 2 vị trí, không phân cực |
| Headphone | 124 / Ø3,5 | TRRS 4 tiếp điểm; cùng module HD_AUDIO với mic |
| Mic | 103 / Ø3,5 | TRS 3 tiếp điểm |
| Hai USB-A | 69 và 36 / 13 Y × 6 Z | Mỗi cổng 9 tiếp điểm, lưỡi định hướng; chung đầu USB19: khung 2×10, 19 vị trí tiếp xúc, bịt vị trí 20 |

HD_AUDIO dùng khung 2×5, 9 vị trí tiếp xúc, bịt vị trí 8. F_PANEL của bo mô phỏng có 9 chân, khuyết 10: PLED ở 2+/4−, PWR SW 6/8, RESET SW 5/7. Họ 01 không quy định HDD LED rời; không thêm USB-C hay ARGB. EPS/ATX24/GPU không đi kèm case trống. Cửa PSU và I/O bo còn rỗng, không vẽ C14, công tắc PSU hoặc các cổng bo vào tấm lưng.

## Vật liệu và vít

Thép M1 `#24262A`: khung 1,2, panel 1,0, cạnh R1, roughness 0,65/metallic 0,25. Lưới M2 `#101216` dày 0,8: trước lỗ lục giác rộng 3/bước 4; vùng khác Ø2,4/bước 3,6 so le; mép đặc 6, quanh vít bán kính 5. Kính khói `#B8C4CA`, IOR 1,5, roughness 0,08, truyền sáng mục tiêu 75%, men `#101216`. Cao su/nhựa `#202329`; dây `#15171B`; kim loại sáng `#AEB4BC`; tiếp điểm `#C6A565`, lưỡi USB `#1F6FD1`. Lọc PET dày 0,5, mắt mở 0,35/sợi 0,15, viền nylon 1,5; mọi lọc nằm trong bao thân.

| Nhóm | Số / loại | Tâm hoặc cách giữ |
|---|---|---|
| P1 kính | 4 vít tay #6-32 × 8 + 4 đệm | X ±190, Y ±203, trục Z |
| P2 hông kín | 2 vít tay #6-32 × 6 | X −199, Y ±198, đầu −Z |
| P3 cassette | 2 M3 × 6 + 6 ngàm | (213;−207;−98), (213;−207;78), trục X |
| P4 shroud | 2 M3 × 6 | (−170;−129;98), (115;−129;98), trục Y |
| P5 nắp PCIe | 7 #6-32 × 6 | Một vít/nắp; đủ bảy khi trống |
| P6 chân | 4 M4 × 8 chìm | Một vít tại mỗi tâm chân, trục Y |
| M1 bo, dành khi lắp | 9 M3 × 5; 9 chân đế đã có | Lưới tại mục các phần vỏ |
| M2 quạt, dành khi lắp | 8 trước + 4 sau, Ø5 × 10 | Bốn điểm mỗi khung quạt |
| M3 nguồn, dành khi lắp | 4 #6-32 × 6 | Bốn lỗ bất đối xứng ở bảng gá |
| M4 AIO tùy chọn | 8 M3 × 6 ngắn | Hai cụm ray 105; quạt–két dùng 8 M3 × 30 thuộc bộ AIO |

Vỏ trống có **21 vít giữ tháo được và 9 chân đế**, không cộng vít linh kiện chưa lắp. Khung/lưng/nóc/đáy cố định, lọc nam châm: 0 vít tháo.

## Các hướng nhìn và điểm người mới cần thấy

FRONT +X nhìn −X: I/O bên trái hình. BACK −X nhìn +X: cửa I/O bo bên trái, bảy nắp và cửa PSU. LEFT/OPEN +Z nhìn −Z: kính, khay, trước ở phải hình. RIGHT/CLOSED −Z nhìn +Z: thép kín, trước ở trái. TOP +Y nhìn −Y: trước ở dưới, kính ở phải. BOTTOM −Y nhìn +Y: trước ở trên, kính ở phải. Sáu hình chiếu yêu cầu cùng tỷ lệ; góc 3/4 từ +X/+Y/+Z tách hai hông và cassette theo hướng tháo.

- Đỡ kính khi tháo đủ bốn vít; vít có đệm, không ép kim loại trực tiếp lên kính.
- Mặt trước là lưới hút gió; các vòng gá trống không phải quạt đã lắp.
- Nguồn sẽ nằm dưới shroud, hút riêng qua lọc đáy; không kéo khay xuyên qua khoang nguồn.
- Case trống có bảy nắp PCIe. Khi lắp GPU mới tháo hai nắp và dùng lại hai vít; thân GPU vẫn cần ba khoảng khe.
- G0–G6 là bảy lỗ riêng; cửa CPU 100 × 100 chỉ để bảo trì. Luồn dây sau khay trước khi đóng hông kín.

## Kiểm tra ảnh

Ảnh tạo bằng imagegen tích hợp, dùng concept họ 01 làm tham chiếu dáng/vật liệu; đã sửa một lượt theo brief. Có đủ sáu mặt, một góc 3/4, bảy nắp PCIe, G0–G6 và không có linh kiện lắp trong case. **Ảnh chưa đạt hoàn toàn yêu cầu bản vẽ cùng tỷ lệ.** Các sai lệch còn lại:

- Sáu hình chiếu bị co ngang khác nhau: chiều 430 ở hông không bằng 430 ở nóc theo cùng tỷ lệ pixel. Chỉ dùng số ghi, không lấy tỷ lệ ảnh làm kích thước.
- TOP vẫn đặt đường **310 nằm ngang và 176 thẳng đứng**; phải đổi lại: 310 dọc X, 176 ngang Z. Các ô ray 240 chỉ minh họa; dùng tám tâm gá trong bảng.
- FRONT: một số leader lệch nút/cổng, thiếu dòng USB-A tại Y=69; power thật là nút lớn trên cùng. Vẫn cần đủ hai USB-A ở Y=69 và 36 theo bảng. Dòng tọa độ phụ dưới cửa I/O lưng bị lỗi chữ; cửa chuẩn là 159 Y × 37 Z tại X≈−215.
- G0 được thấy qua shroud làm mờ ở góc 3/4: lỗ nằm trên **khay Z=−89,5**, không khoét vào vách ngoài shroud. Chi tiết ngàm, lỗ/vít nhỏ và đường đi dây bị giản lược; dựng theo các bảng tọa độ và số lượng ở trên.

<details>
<summary>Prompt sửa ảnh cuối — imagegen tích hợp</summary>

Đầu vào là sheet lần đầu, được tạo từ concept họ 01 và các thông số của tài liệu này.

```text
Edit this reference sheet ONCE, retaining its six-view grid plus single exploded 3/4, realistic empty black case, dimension style and all correct geometry. Make these targeted drafting corrections:
1. Add the missing G0 opening and label in the lower forward motherboard tray, center (-32,-174,-89.5), clear aperture60x28. In exploded perspective make PSU shroud translucent enough to see the actual G0 rubber slot behind it and an unobstructed leader. Keep G1/G3 at top, G2/G4 forward, G5/G6 below. Exactly seven G codes G0,G1,G2,G3,G4,G5,G6, each readable. CPU service opening100x100 stays separate.
2. Delete the garbled text under LEFT view near its lower right edge. Replace only with crisp numbered callout "6 PCIe x7" pointing to the seven separate covers in BACK. Preserve exactly seven covers and seven screws, all seven present in exploded view too.
3. FRONT dimension leader labels must point across to the ACTUAL front I/O buttons/ports on viewer LEFT, NOT to mesh border on viewer right. Labels may stay in whitespace beside the front image; route leaders clearly. Power14 at Y178, reset4 at147, headphone3.5 at124, mic3.5 at103, USB-A13x6 at69 and36, all X215,Z101. No USB-C.
4. BACK I/O opening callout is "159 x 37" (height Y159,width Z37), NEVER 59x159. Keep aperture Y26.5..185.5, Z-81.9..-44.9 at rear -X. It is an empty opening, no motherboard ports. PSU opening empty150x86.
5. TOP drawing orientation: front DOWN, glass RIGHT. Roof vent is LONG along vertical X, 310 in X, and 176 in horizontal Z. Place 310 dimension vertically alongside vent and176 horizontally across vent. Do not mark 310 across entire case width. Roof overall230 horizontal x430 vertical. Optional 240 rail should depict two dashed 120 mounting SQUARES end to end vertically along X, not two circles side by side across Z. Their centers X-71.5/+48.5,Z35, holes +/-52.5 X/Z. Label "240 rail / EMPTY". No fan blades or hubs, no radiator installed.
6. BOTTOM orientation is front UP, back DOWN, glass RIGHT; preserve four feet and rear PSU filter124x124, back pull arrow DOWN.
7. Footer USB label exactly "USB 19 / blocked 20". Remove the erroneous "(USB 2.0)" text. Draw its female housing2x10 with19 cavities and exactly cavity20 blocked. HD AUDIO female2x5 with9 cavities and cavity8 blocked (second row fourth position if first row1,3,5,7,9 and second2,4,6,8,10), show9/10 at last column. PWR SW, RESET SW, PLED two-position plugs unchanged; PLED +/-.
8. Mounting-points footer exact short rows: "Front 2 x 140 / 8 holes (Y -72, +74; Z 10; pitch 124.5)", "Rear 1 x 120 / 4 holes (Y 128; Z 28; pitch 105)", "PSU 4 holes / opening 150 x 86", "Roof 240 rail (X -71.5, +48.5; Z 35; pitch 105)".
9. Number8 "Feet" leader to one foot. Preserve numbered1Mesh,2Glass,3Steel panel,4Tray/9 standoffs,5PSU shroud,6PCIex7,7Filters,8Feet,9FrontI/O.
10. Six orthographic views MUST be at identical scale: their width230 and depth430 measured in image must share same mm/pixel. Elevations body460 plus feet18. LEFT/RIGHT430-wide views must visibly be1.87times width of FRONT/BACK230-wide views. Widen third column or shrink first2 columns' case drawings to achieve uniform scale; allow whitespace. TOP/BOTTOM230x430 same scale. No cropping. Keep all six labels and 3/4 view.
Body230W x460H x430D, feet18,total478. Exactly one sheet; no logo; NO installed board, PSU, GPU, CPU, RAM, SSD, fans, cooler or radiator. Keep case-specific front I/O wires only. English short text, no gibberish.
```

</details>
