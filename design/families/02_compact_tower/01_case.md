# 02 — Vỏ compact tower trống

Nguồn: [đặc tả sáu họ](../00_families_spec.md), mục 0, 2, 7.1–7.2 và 8; dáng theo [ảnh concept](../../case-families/02_compact_tower.png). Số dưới đây là chuẩn dựng mô hình, không đo ngược từ ảnh tạo sinh. Đây là thiết kế mô phỏng, chưa phải bản vẽ gia công.

## Kích thước và trục

Đơn vị mm. O ở tâm thân, không tính chân. X từ lưng (−) tới trước (+); Y từ đáy (−) tới nóc (+); Z từ hông sau khay (−) tới hông mở lắp bo (+). Tọa độ ghi `(X; Y; Z)` ở trạng thái đã lắp.

- Thân **210 rộng Z × 380 cao Y × 370 sâu X**: X ±185, Y ±190, Z ±105.
- Bốn chân Ø26 × cao 14, tâm `(±150; −197; ±78)`; tổng đặt bàn **210 × 394 × 370**, chưa cộng đầu vít nhô tối đa 2.
- Vỏ trống có khay, 9 chân đế bo, shroud, gá quạt, lọc, grommet, I/O vỏ và 4 nắp PCIe. Không có linh kiện máy, quạt hay radiator.

## Các phần của vỏ

| Phần | Kích thước, vị trí và cách giữ |
|---|---|
| Cassette trước | X 172…185, Y −181…181, Z −99…99; tấm chính dày 1, cạnh ngoài R8; 6 ngàm và 2 M3 × 6 tại `(182; −170; ±84)`. Plenum X 171…181. |
| Khe thấp | **4 cột × 7 hàng = 28 khe**, mỗi khe 30 theo Z × 5 theo Y, đầu R2,5. Tâm Z = −60/−20/20/60; Y = −154 + 14j, j = 0…6. |
| Hút cạnh trước | Hai khe rộng 6 ở Z ±94, Y −155…145, thông vào plenum; lọc phía trong phủ cả khe cạnh và khe thấp. Không khoét thông gió trên hai nắp hông. |
| Hai hông kín | Mỗi tấm 350 X × 356 Y × 1: X ±175, Y ±178; Z 104…105 và −105…−104. Mỗi nắp 2 vít tay tại X −169, Y ±158; trượt −X rồi nhấc theo ±Z. |
| Nóc | Cửa đục lỗ 270 X × 164 Z, X −145…125, Z ±82; lọc nam châm nằm trong bao. Nóc cố định, không quạt hoặc gá radiator. |
| Đáy | Cửa hút/lọc PSU 124 × 124, tâm X −112, Z −7; lọc kéo về −X. Bốn vít M4 × 8 chìm giữ chân. |
| Khay bo | Dày 1, mặt trước Z −75,5. Phần trên X −181…125, Y −92…188; phần dưới **chỉ** X −39…125, Y −187…−92, tránh khoang PSU. |
| Chân đế bo | 9 M3 cao 6,5, chân tại Z −75,5, đỉnh Z −69. Các tổ hợp X = −161/−49/63 và Y = −58/54/166; lỗ bo tương ứng Ø3,4. Đây là lưới mô phỏng. |
| Cửa CPU | 100 X × 100 Y, tâm `(−49; 104; −75,5)`; không dùng làm lỗ dây. |
| Khoang dây | Z −104…−76,5, sâu 27,5; hai rãnh rộng 24 ở X −152 và 102; 6 đai giữ. |
| Shroud PSU | Mặt trên Y −92, X −181…118, Z −75,5…100, tấm dày 1; 2 M3 × 6 tại `(−145; −92; 89)` và `(90; −92; 89)`. |

## Lưng và gá linh kiện

- Cửa I/O mATX: **159 cao Y × 37 rộng Z**, Y 4,5…163,5, Z −67,9…−30,9; đã nới 0,5 quanh lá I/O. Rãnh kê lá ở X −185,2…−184,4. Case trống chỉ có cửa, không có lá I/O/cổng bo.
- **4 nắp PCIe ngang**, bước 20,32; dãy Y −1…−82,28. Mỗi nắp cao 18,82, khe giữa 1,5. Bao lá theo Z −56,4…63,6. Mặt gá X −182; vít tại Z 58,6, Y −11,16/−31,48/−51,80/−72,12. Có 4 vít #6-32 × 6, không vẽ cổng GPU.
- PSU ATX: bao mặt lắp tại X −182, Y −186…−100, Z −82…68, **150 Z × 86 Y**. Làm cửa và gờ/tai gá giữ lại bốn lỗ, không khoét mất tai vít. Tâm lỗ `(Y; Z)` = `(−180; −76)`, `(−180; 62)`, `(−106; −76)`, `(−116; 56)`, khớp mesh nguồn trong `tools/build_gaming.py`. Không có PSU, C14 hay công tắc nguồn trên case trống.
- Gá trước F1/F2 ở mặt X 170, tâm `(Y; Z)` = `(−82; 12)` và `(43; 12)`; mỗi gá có 4 lỗ lệch ±52,5 theo Y/Z. Khi lắp, tâm quạt ở X 157,5, thân X 145…170.
- Gá sau F3 ở mặt X −182, tâm `(Y; Z) = (105; 30)`; 4 lỗ lệch ±52,5. Khi lắp, tâm quạt X −169,5, thân X −182…−157. Cửa thoáng có viền/gân đỡ; **không vẽ rotor**.
- Gá quạt 120 dùng bước lỗ 105 × 105. Lỗ thông Ø4,5 là lựa chọn bổ sung cho mô hình, dùng vít quạt tự tạo ren Ø5 × 10 khi lắp quạt.

## Lỗ luồn dây

Tất cả nằm trên mặt khay Z −75,5. Kích thước là lòng mở X × Y; viền EPDM rộng 3. Riêng G1/G3 là rãnh chữ U mở tới Y 188, bọc cạnh dày 1, không có thanh cao su bắc ngang miệng.

| Mã | Tâm X; Y | Lòng mở | Dùng cho |
|---|---|---|---|
| 02-G0 | 5; −137 | 60 × 28 | Dây PSU vào sau khay |
| 02-G1 | −109; 182 | 32 × 12, mở trên | EPS |
| 02-G2 | 96; 88 | 24 × 60 | ATX24, quạt trước, I/O đi xuống |
| 02-G3 | −13; 182 | 36 × 12, mở trên | CPU_FAN |
| 02-G4 | 104; −28 | 28 × 28 | Nguồn GPU |
| 02-G5 | 40; −82 | 42 × 12 | USB19, F_PANEL |
| 02-G6 | −141; −82 | 38 × 12 | HD_AUDIO, quạt sau |

## I/O vỏ và đầu dây

Các miệng cổng hướng +Y, tâm X 157, Y 190: power Ø14 tại Z −58; USB-A ×2 tại Z −20/17, mỗi miệng 6 X × 13 Z; jack combo Ø3,5 tại Z 52. Module sâu tối đa 14, X 149…165; nút phẳng bằng nóc. Không reset, USB-C, mic rời hoặc HDD LED.

| Đầu | Tiếp điểm và khóa |
|---|---|
| USB-A ×2 | 9 tiếp điểm mỗi cổng, lưỡi nhựa định hướng; chung cáp USB19, vỏ 2 × 10, 19 tiếp điểm, vị trí 20 bịt khóa. |
| Combo audio | Jack TRRS 4 tiếp điểm; cáp HD_AUDIO 9 tiếp điểm trên khung 2 × 5, vị trí 8 bịt khóa. |
| PWR SW | Đầu 2 vị trí, 2 tiếp điểm, không phân cực; tới F_PANEL chân 6/8. |
| PLED | Đầu 2 vị trí, 2 tiếp điểm, đánh dấu +/−; tới F_PANEL chân 2(+)/4(−), vòng power trắng dịu. |

Dây I/O đen dài 650, đi mép trước → G2 → G5/G6; phần dư sau khay. Chỉ có dây I/O đi kèm vỏ ở sheet này. Các đầu nguồn/quạt của linh kiện chưa lắp không thuộc case trống.

## Vật liệu và vít

Thép sơn nhám **#24262A**, roughness 0,65, metallic 0,25: khung gấp dày 1,2, panel rời 1,0; cạnh ngoài R1 trừ mặt trước R8. Lưới **#101216** dày 0,8, lỗ Ø2,4 bước 3,6 so le, mép đặc 6 và vùng vít bán kính 5. Cao su/nhựa **#202329**; lọc PET dày 0,5, mắt mở 0,35, sợi 0,15, viền nylon 1,5 nằm trong hốc. Kim loại cổng **#AEB4BC**, nhựa **#101216**, tiếp điểm **#C6A565**, lưỡi USB **#1F6FD1**; vòng power **#D2D7DD**, dây **#15171B**. Không kính, logo hoặc RGB.

Vít đang giữ vỏ: **4 vít tay #6-32 × 6** của hai hông; **2 M3 × 6** cassette; **2 M3 × 6** shroud; **4 #6-32 × 6** nắp PCIe; **4 M4 × 8** chân. Tổng **16 vít giữ**, cộng riêng **9 chân đế M3**. Khung, nóc, đáy, lưng cố định và lọc dùng **0 vít tháo**. Vít lắp sau: bo 9 M3 × 5, quạt 12 vít Ø5 × 10, PSU 4 #6-32 × 6; không cộng vào vỏ trống.

## Nội dung các hướng nhìn

| View | Hướng nhìn và chi tiết cần thấy |
|---|---|
| FRONT +X | Nhìn −X, +Z ở trái: 28 khe thấp, hai khe cạnh, mặt trước R8. |
| BACK −X | Nhìn +X, +Z ở phải: cửa I/O rỗng, gá quạt, 4 nắp PCIe, cửa PSU và các tai vít. |
| LEFT +Z | Nhìn −Z, +X ở phải: hông thép kín, hai vít phía sau. |
| RIGHT −Z | Nhìn +Z, +X ở trái: hông thép kín đối diện, hai vít phía sau. |
| TOP +Y | Nhìn −Y, +X xuống dưới, +Z sang phải: lọc/lỗ nóc và đúng bốn vị trí I/O. |
| BOTTOM −Y | Nhìn +Y, +X lên trên, +Z sang phải: lọc PSU, 4 chân và vít chìm. |
| 3/4 | Từ +X/+Y/+Z; hai hông và cassette tách theo hướng tháo; thấy khay, G0–G6, chân đế, shroud và gá quạt trống. |

## Người mới cần chú ý

1. Hai hông đều là thép kín; chỉ thấy bên trong sau khi tháo nắp.
2. Mặt trước đặc vẫn hút gió qua khe thấp và hai khe cạnh, cần giữ đường thông vào khoang trước.
3. Có đúng 4 nắp PCIe; vỏ dùng bo mATX và vẫn chứa nguồn ATX.
4. G1/G3 mở lên mép khay để đưa đầu dây qua trước khi bắt bo; cửa CPU không phải lối dây.
5. Nóc chỉ thoát thụ động. Giới hạn mô phỏng: GPU dài 280, cao 120, dày 50; tản cao 160 tính từ IHS, không hỗ trợ radiator.

## Kiểm tra ảnh

Ảnh tạo bằng công cụ imagegen tích hợp, đã sửa một lần theo brief. Đủ sáu mặt chính và một phối cảnh tháo panel; hai hông kín, 28 khe thấp và 4 nắp PCIe. Các sai khác còn lại cần bỏ qua khi dựng:

- Mũi tên +Z của FRONT, +X của RIGHT và +X của BOTTOM còn ngược. Dùng hướng chiếu trong bảng trên. Các hình chính chưa hoàn toàn đồng tỷ lệ; kích thước cửa lọc đáy vẽ chưa vuông đúng 124 × 124 và chưa đúng vị trí định lượng.
- Dải chú giải lỗ dây dưới cùng có nhãn lặp/nhầm: G2 bị ghi G1, G6 bị lặp, G0 có kích thước sai. Tâm G1/G3 phải ở Y **182**, còn **188** là mép mở của khay. Dùng bảng G0–G6 phía trên, không chép chú giải ảnh.
- Inset gá trước ghi “M4 holes”, không đúng loại vít quạt Ø5 × 10 đã chọn. Gá, lỗ, vít nhỏ và chín chân đế trong phối cảnh chỉ minh họa; dựng theo tâm và số lượng trong đặc tả. Phần nóc cố định chưa thể hiện đầy đủ dưới lọc đang tách.
- Inset I/O bị ghi “FRONT I/O” nhưng cụm này nằm trên **nóc**, hướng +Y. PWR SW không phân cực; PLED phải là **+/−**, ảnh thể hiện dấu chưa đúng. Hình đầu USB19 không thể hiện đủ 19 lỗ rõ ràng; khóa pin20 và khóa pin8 của audio lấy theo bảng đầu dây.
- Kích thước tấm hông, cửa CPU, cửa I/O và vị trí lọc chỉ đọc từ số liệu, không suy từ tỷ lệ ảnh. Sáu đai dây, ngàm cassette và các vít nhỏ chưa được thể hiện đủ rõ để đếm.

Các số trong file này và đặc tả tổng là nguồn chuẩn; ảnh dùng để đọc dáng và bố cục.

<details>
<summary>Lệnh tạo ảnh (imagegen tích hợp, gồm một lần sửa)</summary>

```text
Use case: infographic-diagram. Create ONE high-resolution landscape product reference drawing sheet for a 3D modeller, title "02 COMPACT TOWER / 01 CASE", subtitle "EMPTY CHASSIS - DIMENSIONS IN mm". White background, realistic matte charcoal powder-coated steel #24262A, thin blue #2164AC dimension lines, crisp short English labels, numbered callouts, no brands, no watermarks. Reference silhouette: compact low rectangular PC tower with two completely solid steel sides, flat front with R8 rounded vertical edges, four columns of low horizontal slots, perforated top and four short cylindrical rubber feet.

EXACTLY SIX separated labelled orthographic views at identical mm scale, aligned in two rows, plus ONE larger exploded 3/4 perspective on right. First row FRONT, BACK, LEFT; second row TOP, BOTTOM, RIGHT. Do not omit any. Front/back are 210 wide x380 high; left/right 370 wide x380 high; top/bottom 210 wide x370 deep. Body excludes 14 high feet; total height394. Body W210 x H380 x D370. Main dimensions 210,380,370 and separate 14 feet dimension. Axes: O body center, X rear(-) to front(+), Y bottom(-) to top(+), Z closed side(-) to access side(+). Full labels: "FRONT +X", "BACK -X", "LEFT +Z", "RIGHT -Z", "TOP +Y", "BOTTOM -Y", "3/4 - PANELS REMOVED". FRONT has +Z at image left; BACK +Z at image right; LEFT front at image right; RIGHT front at image left; TOP front at image bottom and +Z right; BOTTOM front at image top and +Z right. All six orthographics assembled panels, completely opaque solid side panels, not transparent. Show internals only in exploded perspective; small isolated connector details below perspective allowed, no additional perspective.

Geometry: X +/-185 Y +/-190 Z +/-105. Front cassette X172..185 Y-181..181 Z-99..99, steel1mm, R8 edges. EXACTLY 28 pill slots arranged FOUR columns SEVEN rows, each30 wide x5 high R2.5, column Z=-60,-20,20,60; row Y=-154,-140,-126,-112,-98,-84,-70. TWO narrow vertical side intake openings 6mm wide at Z+/-94, from Y-155 to145, subtly recessed along front edges. Filter behind low and side intakes. Front attached six clips plus2 M3x6 at X182 Y-170 Z+/-84. Both side panels350 depth x356 height x1mm, at Z+/-105, two captive thumbscrews EACH at rear X-169 Y+/-158. Slide rear then lift outward, show arrows in 3/4.
Top perforated vent X-145..125 Z-82..82:270x164, holes2.4 pitch3.6 staggered,6mm solid rim, flat magnetic filter, no roof fans or radiator mounts. Top I/O at X157 Y190: circular flush power14mm Z-58, two USB-A ports6x13 at Z-20 and17 blue tongues, ONE3.5mm combo audio jack at Z52. No reset, USB-C or separate mic.
Bottom PSU intake/filter124x124 centered X-112 Z-7, slide filter rear. FOUR feet diameter26 height14 at X+/-150 Z+/-78, one recessed M4x8 each.
BACK empty apertures: vertical mATX I/O cutout159 high x37 wide Y4.5..163.5 Z-67.9..-30.9 (no actual motherboard ports), one120mm rear fan vent at Y105 Z30 with square105mm mounting centers (no rotor or fan), exactly FOUR horizontal removable PCIe blank plates below vent. PCIe pitch20.32, total bank Y-1..-82.28, Z-56.4..63.6, exactly4 retention screws at Y-11.16,-31.48,-51.8,-72.12 Z58.6. Empty ATX PSU rectangular aperture150x86 at rear bottom Y-186..-100 Z-82..68,4 mount holes, no PSU, no C14, no switch. Rear aperture collars recessed to X-182, I/O shield seat X-185.2..-184.4.

3/4 exploded from +X/+Y/+Z: clearly show empty steel frame and tray with nine brass standoffs, PSU shroud, rubber edged cable openings, front fan mounting rails, front cassette displaced +X and both SOLID steel side panels displaced +/-Z with arrows. No motherboard, graphics card, PSU, CPU, cooler, RAM, SSD, fan blades, fans or radiator anywhere. Front supports exactly2x120mm EMPTY mounts centers X170 Y-82/43 Z12, four holes each105mm square. Rear120mm EMPTY mount. Nine standoffs M3 height6.5 on tray at Z-75.5, X=-161,-49,63 combined Y=-58,54,166. CPU service opening100x100 center X-49 Y104. Tray top region X-181..125 Y-92..188, lower region ONLY X-39..125 Y-187..-92, so no tray through PSU bay. Shroud top Y-92 X-181..118 Z-75.5..100,2M3x6.
Rubber cable openings all labelled G0 G1 G2 G3 G4 G5 G6 with fine leaders in perspective: G0 center(5,-137)60x28; G1(-109,182)32x12 OPEN-TOP U notch; G2(96,88)24x60; G3(-13,182)36x12 OPEN-TOP U notch; G4(104,-28)28x28; G5(40,-82)42x12; G6(-141,-82)38x12. Coordinates X,Y on tray. Cable bay27.5mm deep behind tray with2 vertical24mm channels,6 straps.

Concise numbered legend: "1 Solid sides", "2 Front intake", "3 Top filter", "4 I/O", "5 Tray / G0-G6", "6 PSU shroud", "7 PCIe x4", "8 Bottom filter", "9 Feet". Small fastener table distinguish fitted chassis screws from future component fasteners: "PANELS: 4 thumb + 2 M3x6", "SHROUD: 2 M3x6", "PCIe: 4 x 6-32x6", "FEET: 4 M4x8", "STANDOFFS: 9 M3 / H6.5", "FIT LATER: board 9 M3x5; fans 12 x 5x10; PSU 4 x 6-32x6". No loose screws floating over object.
Small readable case I/O cable end details: "USB 19 - blocked pin20" (2x10 socket one keyed blocked position), "HD AUDIO 9 - blocked pin8" (2x5 one blocked), "PWR SW 2" and "PLED 2 +/-". Power ring soft white, no RGB. Bottom note "No fans / No radiator / No hardware installed". Prioritize correct six views, exact4x7 front slots,4 PCIe blank covers, four feet, both opaque steel sides, empty chassis and readable English. No decorative fake text.
```

```text
Edit this technical sheet, keeping its white background, charcoal realistic steel, blue dimensions, six orthographic views at equal scale, and ONE exploded 3/4. Correct these mechanical errors precisely:
1. BACK and exploded rear panel must have EXACTLY FOUR horizontal PCIe blanking plates and FOUR retention screws, not five or six. Four total strips, obvious count. Remove extras. Bank Y -1 to -82.28.
2. FRONT must retain exactly 4 columns x7 rows=28 slots, same count on detached cassette. No change to external solid side panels.
3. G1 and G3 on tray are OPEN-TOP U-shaped notches, cut DOWN FROM TOP EDGE at Y188 to176, not closed rounded holes. Show these notches both in perspective and small tray inset. Label all seven G0 G1 G2 G3 G4 G5 G6 with leaders on actual tray. CPU opening remains100x100. Tray lower rear portion must be completely absent: lower region Y-187..-92 exists only X-39..125. Correct tray inset L shape: rear X-181 is left, front X125 right, lower left cut away. G0 at X5 Y-137 in lower RIGHT extension. G1 X-109 and G3 X-13 are on top edge. G2 X96 Y88, G4 X104 Y-28, G5 X40 Y-82, G6 X-141 Y-82.
4. Exploded front frame has exactly TWO empty circular 120mm fan mounts, no third small circular cutout underneath. All openings truly empty, no fans. Centers at Y-82 and43, Z12. Show four mount holes per fan at105mm square pitch. No roof fan/radiator.
5. BOTTOM orientation: front +X is UP, rear -X DOWN; +Z RIGHT. Put124x124 PSU intake/filter near REAR, towards BOTTOM of bottom-view rectangle, not centered. Filter center X-112 Z-7. Four feet corners unchanged. TOP front +X DOWN, +Z RIGHT, I/O bottom row power at LEFT, USB, USB, audio at RIGHT.
6. FRONT +Z arrow must point LEFT, BACK +Z RIGHT. RIGHT view +X points LEFT, LEFT view +X RIGHT. BOTTOM +X arrow UP. Side views assembled solid steel. Body width210 height380 depth370, feet14 total394. Six views SAME scale; front/back widths identical.
7. The lifted perforated sheet in 3/4 must be labelled magnetic TOP FILTER only; steel roof stays fixed. Rear I/O is an EMPTY aperture, bottom PSU is EMPTY aperture; no electronic hardware.
8. Small connector illustrations have BLACK wires, no rainbow. USB19 housing2x10 with19 open sockets and pin20 visibly blocked. HD AUDIO housing2x5 with9 sockets and pin8 blocked. PWR SW2, PLED2 +/-.
Keep main title and fastener schedule. Replace redundant small FRONT/REAR/TOP mini views in bottom strip with useful readable tray drawing and G dimensions, giving room for correct geometry. Only six MAIN external orthographic views and one perspective. Numbered legend1 Solid sides;2 Front intake;3 Top filter;4 I/O;5 Tray/G0-G6;6 PSU shroud;7 PCIe x4;8 Bottom filter;9 Feet. No logo or illegible text.
```

</details>
