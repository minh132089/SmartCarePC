# 06_full_tower / 01_case — Vỏ trống

Ảnh: [01_case.png](01_case.png). Số liệu theo [đặc tả họ vỏ](../00_families_spec.md), mục 0, 6, 7.3 và 8; đầu cắm/vít theo [Gaming](../../gaming/00_blueprint.md). **Số trong tài liệu là chuẩn; không đo ngược từ ảnh tạo sinh.** Đây là thiết kế mô phỏng.

## Kích thước và tọa độ

Đơn vị mm, 100 mm = 1 đơn vị cảnh. O ở tâm thân, không tính chân. X từ lưng (−) ra trước (+), Y từ đáy lên nóc, Z từ hông kín (−) ra kính (+). Thân **250 Z × 620 Y × 550 X**, biên X ±275, Y ±310, Z ±125. Có chân: **250 × 642 × 550**; đầu vít nhô tối đa 2, vít kính 3 chưa cộng vào bao này.

Case trống giữ khay, 9 chân đế bo, shroud, giá chống võng, G0–G6, ray gá, lọc, I/O, 8 nắp PCIe và 4 chân. Không lắp bo, PSU, GPU, CPU, RAM, SSD, quạt, két hoặc hub.

## Các phần và vị trí đã lắp

| Số | Phần | Bao / vị trí |
|---|---|---|
| 1 | Cassette trước | X 262…275, Y ±299, Z ±115; hai cạnh vát 8 × 8. Lưới X 271…271,8; cửa 184 Z × 588 Y tại Z ±92, Y ±294; lọc X 268,5; 6 ngàm |
| 2 | Kính lớn | 528 X × 588 Y × 4 Z; X ±264, Y ±294, Z 121…125; men viền 10, gioăng 1; tháo +Z |
| 3 | Hông kín | 528 X × 588 Y × 1 Z tại Z −125…−124; trượt −X rồi nhấc −Z |
| 4 | Khay | Mặt trước Z −94,5, dày 1,2; trên: X −271…205, Y −190…307; dưới chỉ X −129…205, Y −307…−190, chừa khoang PSU |
| 4 | 9 chân đế bo | M3 cao 6,5, từ Z −94,5 tới −88. Mọi tổ hợp X = −251/−139/−27 và Y = −62,5/80/222,5; khớp lỗ bo Ø3,4 |
| 4 | Cửa CPU | 100 X × 100 Y, tâm (−120;139;−94,5), tách khỏi lỗ dây |
| 5 | Shroud dài | Mặt trên Y −190, X −271…193, Z −94,5…119: dài 464, tôn 1; vách phía kính, đầu +X hở vùng quạt |
| 5 | Giá chống võng | Đệm tại (−5;−4,2;26); gá cố định vào case, không vẽ GPU trên đệm |
| 6 | 8 nắp PCIe | Nằm ngang, bước 20,32. Mép trên hàng i = 56 − 20,32i, i=0…7; mỗi nắp cao 18,82, khe 1,5. Bao dãy Y −106,56…56 |
| 7 | Nóc và ray | Cửa lưới X −245…215, Z −90…104 = 460 X × 194 Z; cassette ngàm trước, 2 vít sau; ray lệch +Z |
| 8 | Lọc đáy | Cửa PSU 124 X × 124 Z tâm (−202;−310;−18), kéo −X; lưới thụ động X −95…178, Z −75…90 |
| 9 | 4 chân vát | 58 X × 22 Y × 38 Z, tâm (±226;−321;±91), có đệm cao su |
| 10 | Module I/O nóc | X 238…254, Y 296…308, Z −95…95; cửa hướng +Y |

Khoang dây Z −124…−95,7 sâu **28,3**; rãnh nguồn rộng 30 tại X 25, rãnh EPS rộng 26 tại X −245, 8 đai. Gá hub tương lai có tâm (90;70;−110), hai lỗ M3 tại X 47/133, Y 70; để trống. Chi tiết gá cố định không phát sinh vít tháo ngoài bảng dưới.

## Cửa lưng và lỗ gá trống

| Vùng | Mốc để dựng |
|---|---|
| I/O bo ATX | Lá tương lai X −275,2…−274,4, Y 62…220, Z −86,4…−50,4. Cửa nới 0,5 mỗi cạnh: **159 Y × 37 Z**, Y 61,5…220,5, Z −86,9…−49,9; có rãnh đỡ riêng |
| PCIe | Mặt lá X −272…−271, Z −75,4…44,6; vít trục X ở Z 39,6, Y = 45,84 − 20,32i, i=0…7; gờ gấp nối về lưng X −275 |
| PSU | Cửa danh nghĩa 150 Z × 86 Y, Z −93…57, Y −294…−208; gờ gá X −272. Bốn tâm (Y;Z): (−288;−87), (−288;51), (−214;−87), (−224;45) |
| 4 quạt trước 140 | Tâm quạt tương lai (247,5;−219/−73/73/219;0); mặt gá X 260; mỗi cụm 4 lỗ Y = tâm ±62,25, Z ±62,25, bước 124,5 |
| Quạt sau 140 | Tâm tương lai (−259,5;149;40); mặt gá X −272; 4 lỗ Y = 149 ±62,25, Z = 40 ±62,25 |
| Ray két 360 | Gốc mặt trên két tương lai (−35;304;40). Ba tâm gá X −155/−35/85, Z 40; mỗi tâm 4 điểm lệch ±52,5 theo X/Z: **12 lỗ**, bậc ray dày hiệu dụng 3 |

Nóc hỗ trợ két 360 hoặc 240 rộng 120, tổng két/quạt 52; không 420/280 và không két trước. Bao két 360 tương lai X −233,5…163,5, Y 277…304, Z −20…100. Bo vẫn ATX, B=(−139;80;−88). Giới hạn mô phỏng: GPU dài 450, dày 65, nhô Z 135; tản khí thay AIO cao 180 từ IHS. Các mẫu lỗ là quy ước mô phỏng theo linh kiện giữ lại, không phải mẫu khoan thương mại.

## Lỗ dây

Mã đầy đủ **06-G0…06-G6**. Tất cả nằm trên khay Z −94,5; kích thước là lòng mở X × Y, viền EPDM rộng 3.

| Mã | Tâm (X;Y) | Lòng mở | Đường đi |
|---|---|---|---|
| G0 | (−91;−248) | 60 × 28 | PSU vào sau khay, luồn ATX24 qua cạnh 60 |
| G1 | (−199;252) | 34 × 14 | EPS trên bo |
| G2 | (10;133) | 28 × 68 | ATX24 |
| G3 | (−94;252) | 48 × 14 | Bơm/quạt két |
| G4 | (52;31) | 30 × 34 | GPU; kẹp tại (40;45;65) |
| G5 | (−42;−91) | 46 × 14 | USB19, F_PANEL |
| G6 | (−235;−91) | 42 × 14 | HD_AUDIO |

G4 là lỗ trên khay, không dùng ống che nổi của Gaming. Dây I/O dài 650 đi sau khay; dây nguồn chỉ thêm khi lắp máy, uốn R≥20, cách cánh quạt ≥10.

## I/O và khóa đầu cắm

Trên mặt Y=310, hàng X=246: reset Ø4 tại Z −84; USB-A tại Z −42/+42 (miệng 6 X × 13 Z, 9 tiếp điểm/cổng); power Ø14 tại Z 0; combo TRRS Ø3,5 tại Z 84 (4 tiếp điểm). HDD LED Ø1,5 tại (261;310;0); vòng PLED trắng `#D2D7DD`. Không USB-C, jack mic riêng hoặc ARGB.

- Hai USB-A dùng một đầu **USB19**, vỏ 2×10, bịt vị trí 20, có gờ định hướng.
- Audio dùng **HD_AUDIO 9**, vỏ 2×5, bịt vị trí 8.
- PWR SW và RESET SW: mỗi đầu 2 vị trí, không phân cực. PLED và HDD LED: mỗi đầu 2 vị trí, ghi +/−.
- Theo F_PANEL mô phỏng: PWR SW 6/8; RESET SW 5/7; PLED 2+/4−; HDD LED 1+/3−; chân 9 không dùng, khuyết 10.

Case trống chỉ có đầu dây I/O vỏ. Cửa I/O bo và PSU rỗng, không có cổng bo, ổ C14 hoặc công tắc nguồn trên tấm lưng.

## Vật liệu và vít

Thép M1 `#24262A`, roughness 0,65/metallic 0,25, khung 1,2/panel 1,0, cạnh R1 trừ vát trước. Lưới M2 `#101216` dày 0,8: trước lục giác rộng 3/bước 4; vùng khác Ø2,4/bước 3,6 so le; mép đặc 6, vùng vít bán kính 5. Kính `#B8C4CA` dày 4, IOR 1,5, roughness 0,08, truyền sáng mục tiêu 75%, viền men `#101216`. Cao su/nhựa `#202329`, dây `#15171B`, kim loại `#AEB4BC`, tiếp điểm `#C6A565`, lưỡi USB `#1F6FD1`. Lọc PET dày 0,5, mắt 0,35/sợi 0,15, viền nylon 1,5, nằm trong bao thân.

| Nhóm | Số / loại | Vị trí tâm |
|---|---|---|
| P1 kính | 4 vít tay #6-32 × 8, 4 đệm | X ±249, Y ±279, trục Z |
| P2 hông kín | 2 vít tay #6-32 × 6 | X −258, Y ±274, đầu −Z |
| P3 trước | 2 M3 × 6 và 6 ngàm | (273;−302;±98) |
| P4 nóc | 2 M3 × 6 và ngàm trước | (−261;310;±103) |
| P5 shroud | 2 M3 × 6 | (−225/158;−190;108) |
| P6 PCIe | 8 #6-32 × 6 | Một vít/nắp, theo bảng lưng |
| P7 chân | 4 M4 × 8 chìm | Một vít mỗi chân, trục Y |
| M, dành khi lắp | Bo 9 M3 × 5; quạt trước 16 + sau 4 vít Ø5 × 10; PSU 4 #6-32 × 6 | Theo tâm gá trên |
| M, dành khi lắp | Két 12 M3 × 6; quạt–két 12 M3 × 30; hub 2 M3 | Vít dài thuộc AIO, không dùng bắt két vào nóc |

Tổng case trống: **24 vít tháo được, 9 chân đế bo, 4 đệm kính**. Khung/khay/lưng/đáy cố định và lọc: 0 vít tháo. Không cộng vít của linh kiện chưa lắp.

## Hướng nhìn và điểm người mới cần thấy

FRONT +X nhìn −X: lưới, +Z bên trái. BACK −X nhìn +X: cửa I/O, gá quạt, 8 nắp, cửa PSU; +Z bên phải. LEFT/OPEN +Z nhìn −Z: kính và khay, trước ở phải. RIGHT/CLOSED −Z nhìn +Z: thép kín, trước ở trái. TOP +Y nhìn −Y: ray/lọc/I/O, trước ở dưới, kính bên phải. BOTTOM −Y nhìn +Y: cửa hút/chân, trước ở trên, kính bên phải. Sáu mặt cùng tỷ lệ; 3/4 từ +X/+Y/+Z tách hai hông và cassette theo hướng tháo.

- Đỡ kính khi tháo bốn vít có đệm; không ép đầu kim loại trực tiếp lên kính.
- Case trống đủ 8 nắp PCIe. Khi lắp GPU mới tháo 2 nắp; thân GPU dày vẫn cần 3 khoảng khe bên trong.
- Bốn cụm gá trước là cho quạt 140; ba cụm ray nóc là cho quạt két 120. Trên sheet này chỉ có lỗ gá.
- PSU nằm dưới shroud và hút riêng qua đáy. Khay phải chừa khoang nguồn ở phía sau.
- G0–G6 là 7 lỗ dây riêng; cửa CPU 100 × 100 chỉ để bảo trì. Vỏ lớn này vẫn dùng bo ATX.

## Kiểm tra ảnh và sai lệch

Ảnh dùng imagegen tích hợp, đã sửa một lượt theo brief. Có sáu mặt và một phối cảnh case trống, nhưng **chưa đạt hoàn toàn yêu cầu kỹ thuật**:

- BACK ghi PCIe ×8 nhưng số hàng/nhãn không khớp, nhảy từ 6 tới 8; chưa thể dùng ảnh để xác nhận đủ tám nắp. Dựng đúng **8 nắp và 8 vít** theo bước 20,32 trong bảng.
- G0 chưa có nhãn rõ và hình lỗ vẫn gần mép shroud, thay vì thấp tại Y −248. Lỗ đúng nằm trên khay, sau vách shroud; số 60 × 28 cạnh G5 không phải kích thước G5. G5 phải là 46 × 14.
- Nóc/đáy đã đúng hướng, nhưng sáu mặt chưa cùng tỷ lệ pixel. Các lỗ ray 360 và chín chân đế chưa được thể hiện đủ, rõ và đúng vị trí; dựng theo bảng tọa độ. Nhãn 7 trong ảnh sửa thành “Roof”, vẫn bao gồm ray két 360 bên trong.
- TOP còn vài dấu tròn phụ; lấy số lượng và vị trí I/O theo bảng. Hình đầu dây USB/audio chưa thể hiện chính xác lỗ bịt và đầu cái; nhãn KEY 20/KEY 8 là chỉ dẫn khóa cần dựng.
- Phối cảnh còn panel rời dư phía trước và phía gần kính; case chỉ có **một kính, một hông thép, một cassette trước, một cassette nóc**. Vị trí tách lớp không phải tọa độ lắp. Lỗ vít/ngàm nhỏ và nét lưới chỉ minh họa.

<details>
<summary>Prompt sửa cuối — imagegen tích hợp</summary>

Đầu vào: sheet đầu tạo từ concept `design/case-families/06_full_tower.png` và số liệu trên.

```text
Correct this existing sheet ONCE, keeping the realistic empty charcoal chassis, white/blue technical style, six orthographic views plus single exploded 3/4, body250W x620H x550D, feet22. Reflow the lower half to fit corrected top/bottom orientation. Prioritize these exact corrections:
1. TOP must be a VERTICAL rectangle250wide x550tall, front at BOTTOM, glass at RIGHT. Rotate its current horizontal drawing90degrees clockwise. Three empty120 mounting squares run vertically along X, offset RIGHT towardglass; overall250 horizontal,550 vertical. Roof mesh460vertical x194horizontal. BOTTOM also vertical250wide x550tall, front at TOP, glass RIGHT; rear PSU filter124x124 at BOTTOM, rear-pull arrow downward. Both same scale as elevations. All six views exact shared mm/pixel; FRONT/BACK250x620,LEFT/RIGHT550x620. Expand canvas if needed, do not compress dimensions.
2. I/O: exactly TWO USB-A ports, ONE power, ONE reset, ONE combo audio, ONE HDD LED. Top atX246: Z-84 resetdiam4, Z-42 USB-A, Z0 powerdiam14, Z42 USB-A, Z84 audio3.5. HDD LED1.5 atX261,Z0. In TOP with glass right, across bottom strip left-to-right is reset,USB-A,power,USB-A,audio, with tiny LED below power. Same counts in enlarged numbered10 detail: REMOVE third USB port and surplus audio/button. Draw each USB mouth6X x13Z with blue tongue. No USB-C. Do not put ports on front mesh.
3. BACK: EXACTLY EIGHT horizontal PCIe blanks, numbered tiny1,2,3,4,5,6,7,8 down their right margin, each with ONE screw. Keep empty I/O slot and empty PSU opening. Eight blanks topY56 to bottomY-106.56, pitch20.32; no lost eighth blank. Add dimensions I/O159high x37wide and PSU150wide x86high.
4. G0 currently appears at shroud top, WRONG. It is on lower forward tray atX-91,Y-248,Z-94.5, opening60x28. Show shroud partially ghosted in 3/4 to reveal G0 well BELOW shroud topY-190. Keep G1/G3 top, G2/G4 forward, G5/G6 below CPU area; CPU opening100x100 separate. Clearly dimension G0 and CPU opening. Nine standoffs only, atX-251,-139,-27 andY-62.5,80,222.5, not arbitrary added brass studs.
5. Connector footer: replace erroneous "USB 2.0 (x2)" with "USB 19 / KEY 20"; female2x10 housing19 contacts with cavity20 BLOCKED. "HD AUDIO 9 / KEY 8",2x5 with cavity8 BLOCKED. PWR SW2,RESET SW2,PLED2+/-,HDD LED2+/-. All are case wires650. Do not add PSU connectors.
6. Reserved mounting footer replace front/rear screw types: "Front 16 x 5x10", "Rear 4 x 5x10", "Pitch 124.5". NO M3 front/rear fan screws. Roof12xM3x6 stays correct. Case empty, all fans and radiator absent. All circle guides are dashed EMPTY mounts, no hubs/blades.
7. Exploded3/4: closed solid panel must be displaced on opposite side of tray, toward-Z (far side/back of object), NOT in front of mesh. Glass displaced toward+Z (near side), mesh cassette toward+X, roof upward. Preserve long shroud464, four feet, empty frame.
8. Use short readable English labels, no malformed words. Keep exact 24 case screws in schedule4+2+2+2+2+8+4,9standoffs. Overall and panel dimensions, numbered legend1..10. No logos. No installed board, GPU, PSU, RAM, CPU, cooler, fans, radiator or hub.
```

</details>
