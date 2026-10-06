# 03 — Vỏ showcase hai khoang trống

Nguồn: [đặc tả tổng](../00_families_spec.md), mục 0, 3, 8; [concept](../../case-families/03_showcase_dual_chamber.png); quy ước Gaming. Số liệu dưới đây là chuẩn dựng mô hình mô phỏng, không phải bản vẽ gia công. Ảnh tạo bằng **imagegen tích hợp**, sửa một lần theo brief.

## Kích thước và tọa độ

Đơn vị mm; **W × H × D = 300 Z × 450 Y × 450 X**. O ở tâm thân; X từ lưng (−) tới trước (+), Y từ đáy (−) tới nóc (+), Z từ hông thép (−) tới hông kính (+). Biên thân X/Y ±225, Z ±150. Tọa độ là trạng thái đã lắp; 100 mm = 1 đơn vị cảnh.

Bốn chân vát 50 X × 36 Z × 20 Y, tâm `(±180; −235; ±112)`; tổng đặt bàn **300 × 470 × 450**, chưa tính đầu vít nhô tối đa 2. Vỏ trống giữ khay, 9 chân đế bo, ống hút bên, gá GPU/riser, gá quạt/két, lọc, I/O vỏ, G0–G6 và 3 nắp PCIe. Không lắp linh kiện, quạt, radiator, hub hoặc cáp riser; không có shroud che sàn khoang kính.

## Panel và khoang

| Phần | Bao / vị trí và cách giữ |
|---|---|
| Kính hông +Z | 435 X × 422 Y × 4; X −215…220, Y ±211, Z 146…150. Cassette dán kính, 2 chốt đáy; 2 M3 × 6 ẩn tại `(−185;214;141)`, `(190;214;141)`. Tháo vít, nhấc khỏi chốt rồi đưa +Z. |
| Kính trước +X | 169 Z × 422 Y × 4; X 221…225, Y ±211, Z −24…145. Hai chốt đáy; 2 M3 × 6 ẩn tại `(218;214;−12/132)`. Tháo ra +X; dải phía −Z bằng thép kín. Góc hai kính nối gioăng 1, không cột dày. |
| Hông thép −Z | Z −150…−149; 2 vít tay tại X −209, Y ±190; trượt −X rồi nhấc −Z. Cửa PSU X −215…−85, Y −170…−20, **130 × 150**. Cửa ống bên X 78…207, Y −112…147, **129 × 259**. Cả hai có lưới/lọc riêng. |
| Nóc | Cửa 370 X × 141 Z: X −190…180, Z −8…133. Cassette 2 M3 × 6 tại `(−213;225;−15/132)` và ngàm trước. Lọc nam châm; giá két bên trong độc lập. |
| Đáy | Cửa 384 X × 130 Z: X −197…187, Z −1…129; lọc kéo +X. Tấm cố định; mỗi chân 1 M4 × 8 chìm. |
| Khay / vách | Mặt trước Z −36,5, sau −37,7; phần mang bo X −221…74. Khoang phụ Z −149…−37,7, sâu 111,3; khoang kính Z −36,5…146, sâu 182,5. |
| Ống hút bên | X 78…207, Y −112…147, Z −148…−35; thành dày 1 ở hai mép X và hai đầu Y. Hai đầu theo Z mở, thông trực tiếp giữa lưới hông và hai gá bên; tách khỏi khoang PSU/dây. Không kéo khay kín ngang lòng ống. |
| Khay bảo trì | Cửa CPU 100 × 100 tâm `(−70;84;−36,5)`. Chín chân đế M3 cao 6,5 tại tổ hợp X = −201/−89/23, Y = −117,5/25/167,5; đỉnh Z −30, lỗ bo Ø3,4. |
| Đường dây | Rãnh dọc rộng 30 tâm X 54, rãnh dưới Y −180, tám đai sau khay. Chừa cửa CPU; lỗ chờ hub ở `(42;−148;−85)` và `(128;−148;−85)`, theo tai cách 86. Hub chưa lắp. |

## Lưng, giá GPU và gá quạt

- Cửa I/O ATX rỗng **37 Z × 159 Y**, Y 6,5…165,5, Z −28,9…8,1; rãnh lá chắn X −225,2…−224,4. Không có cổng bo trên vỏ trống.
- Cửa PSU đứng **86 Z × 150 Y**, mặt gá X −222, Y −170…−20, Z −137…−51. Bốn tai/lỗ giữ lại trên gờ, tâm `(Y;Z)` = `(−26;−131)`, `(−164;−131)`, `(−26;−57)`, `(−158;−67)`, suy từ mesh nguồn ATX qua phép quay R+. Không vẽ ổ C14/công tắc khi chưa có PSU.
- **Ba nắp PCIe dọc** theo Y −113…7; dãy Z 55,3…116,26, bước 20,32, khe giữa nắp 1,5. Gờ kê lá tại X −222…−221; mỗi nắp 1 vít #6-32 × 6. Không thêm bảy khe ngang. Khi lắp GPU tháo hai nắp, giữ trống thể tích khoảng khe thứ ba.
- Giá GPU **300 X × 18 Z × 2 Y**, X −221…79, Y −117…−115, Z 75…93; hai M3 giữ vào khung, đệm nâng tại X 45. Gá adapter riser có hai tai cách 100; tâm đầu khe khi lắp `(−123;−115,2;61,3)`. Giữ chỗ tai, kẹp và vòng cáp R≥15; sheet này chưa lắp cáp/đầu riser. Tâm vít khung của giá chưa được đặc tả tổng chốt, phải khớp tai giá khi dựng, không đo từ ảnh.

Mỗi gá quạt 120 có 4 lỗ theo hình vuông 105 × 105, lệch ±52,5 quanh tâm dưới đây. Lỗ thông Ø4,5 là lựa chọn bổ sung cho mô hình. Không có rotor trong các cửa.

| Gá | Mặt và tâm gá | Vít lắp sau |
|---|---|---|
| F1–F3 đáy | Y −209; `(X;Z)` = `(−132;63)`, `(−10;63)`, `(112;63)` | 12 vít quạt Ø5 × 10; gió +Y |
| F4–F5 bên | Z −35; `(X;Y)` = `(142;−45)`, `(142;80)` | 8 vít Ø5 × 10; gió +Z |
| F6 lưng | X −222; `(Y;Z)` = `(97;77)` | 4 vít Ø5 × 10; gió −X |
| Ray AIO 240 | Y 219; hai nhóm tâm `(X;Z)` = `(−75;65)`, `(45;65)`; mỗi nhóm 4 lỗ 105 × 105, tổng 8 | 8 M3 × 6 và đệm 1; bậc ray hiệu dụng 3. Giá độc lập cassette nóc. |

Giới hạn mô phỏng: GPU dọc 330 dài × 130 cao × 60 dày; AIO nóc chỉ 240, tổng két/quạt 52; không két bên/đáy. GPU mẫu cách mặt trong kính 32,5; dày giới hạn còn 30. Tản khí thay AIO tối đa 150 từ IHS.

## Lỗ luồn dây

Tâm trên Z −36,5; lòng mở X × Y, viền EPDM rộng 3. Không dùng cửa CPU luồn dây.

| Mã | Tâm X; Y | Lòng mở | Chức năng |
|---|---|---|---|
| 03-G0 | −49; −170 | 60 × 28 | Nguồn vào rãnh |
| 03-G1 | −149; 193 | 34 × 14 | EPS |
| 03-G2 | 54; 79 | 26 × 65 | ATX24 |
| 03-G3 | −30; 193 | 44 × 14 | Bơm, quạt nóc |
| 03-G4 | 54; −20 | 24 × 30 | Nguồn GPU vòng phía +X card |
| 03-G5 | 6; −145 | 46 × 14 | USB, F_PANEL |
| 03-G6 | −183; −145 | 40 × 14 | Audio |

## I/O vỏ và đầu dây

Tất cả nằm trên **nóc**, X 188, Y 225, hướng +Y: power Ø14 tại Z −116; reset Ø4 tại −89; hai USB-A miệng 6 X × 13 Z tại −65/−42; combo TRRS Ø3,5 tại −17. Module X 180…196, Y 209…223, Z −128…−9. Không USB-C, mic riêng hoặc HDD LED. Power có vòng cyan.

| Đầu dây I/O | Tiếp điểm / khóa |
|---|---|
| USB19 | Vỏ 2 × 10, 19 tiếp điểm, bịt vị trí 20; hai USB-A mỗi cổng 9 tiếp điểm, lưỡi định hướng. |
| HD_AUDIO | Vỏ 2 × 5, 9 tiếp điểm, bịt vị trí 8; jack TRRS có 4 tiếp điểm. |
| PWR SW / RESET SW | Mỗi đầu 2 tiếp điểm, không phân cực; F_PANEL chân 6/8 và 5/7. |
| PLED | 2 tiếp điểm, có +/−; F_PANEL 2(+)/4(−). |

Dây I/O đen dài 650, men mép trước vào khoang phụ, ra G5/G6; buộc dư sau khay, không nằm trong ống hút. Đầu ATX/EPS/GPU, PWM/ARGB thuộc linh kiện lắp sau.

## Vật liệu và vít

Thép nhám **#24262A**, khung 1,2, panel 1,0, roughness 0,65, metallic 0,25, cạnh R1. Lưới **#101216** dày 0,8, lỗ Ø2,4 bước 3,6 so le; mép đặc 6, quanh vít R5. Kính khói **#B8C4CA**, dày 4, IOR 1,5, roughness 0,08, truyền sáng mục tiêu 75%, men viền **#101216** rộng 10, gioăng 1. Cao su/nhựa **#202329**; lọc PET 0,5, mắt mở 0,35, sợi 0,15, viền nylon 1,5 nằm trong hốc. Kim loại cổng **#AEB4BC**, nhựa **#101216**, tiếp điểm **#C6A565**, lưỡi USB **#1F6FD1**, dây **#15171B**, PLED **#22D3EE**.

Vít giữ vỏ: kính 4 M3 × 6; hông thép 2 vít tay #6-32 × 6; nóc 2 M3 × 6; giá GPU 2 M3 × 6; PCIe 3 #6-32 × 6; chân 4 M4 × 8. Tổng **17 vít**, cộng riêng 9 chân đế bo. Khung, lưng, đáy và lọc: 0 vít tháo. Vít lắp sau: bo 9 M3 × 5; PSU 4 #6-32 × 6; hub 2 M3; quạt/két theo bảng. Hai vít lá GPU tái dùng từ nắp PCIe.

## Các hướng nhìn

| View | Hướng nhìn và nội dung |
|---|---|
| FRONT +X | Nhìn −X; +Z bên trái. Kính trước trái, dải khoang phụ kín bên phải. |
| BACK −X | Nhìn +X; +Z bên phải. Cửa PSU đứng bên trái, I/O gần vách giữa, gá quạt phía trên phải, ba khe dọc phía dưới. |
| LEFT +Z | Nhìn −Z; trước +X bên phải. Qua kính thấy khay, G0–G6, giá GPU, hai gá bên, ba gá đáy. |
| RIGHT −Z | Nhìn +Z; trước +X bên trái. Cửa hút bên cao ở trái; cửa hút PSU thấp ở phải. |
| TOP +Y | Nhìn −Y; +X xuống dưới, +Z sang phải. Ray/lọc về phía kính, I/O ở mép trước của khoang kín. |
| BOTTOM −Y | Nhìn +Y; +X lên trên, +Z sang phải. Lọc ba gá đáy về phía kính, bốn chân/vít. |
| 3/4 | Từ +X/+Y/+Z; tháo panel theo mũi tên để thấy khay, ống bên, giá và các cửa gá trống. |

## Người mới cần chú ý

1. Hai kính không hút gió; hai quạt bên nhận gió qua ống riêng từ hông thép đối diện.
2. Nguồn đặt đứng ở khoang sau bo, không nằm dưới sàn kính; cửa CPU phải tiếp cận được.
3. GPU dựng mặt quạt ra kính cần riser và giá đỡ; ba khe dọc không đồng nghĩa lắp thêm card cạnh GPU dày 55.
4. Kính dùng chốt đáy và vít ẩn trên cassette; đỡ kính khi tháo, không khoan vít xuyên mặt kính.
5. Gá trống không phải quạt. Tờ này chưa có quạt, két hay linh kiện nào.

## Sai khác còn lại trong ảnh

- Đủ sáu mặt và một phối cảnh, nhưng các hình chiếu chưa đồng tỷ lệ tuyệt đối; không đo kích thước từ pixel.
- RIGHT đã đảo đúng phía hai vùng lưới nhưng **nhãn/kích thước bị hoán đổi**: cửa cao bên trái phải là side-bank 129 × 259; cửa thấp bên phải phải là PSU 130 × 150.
- TOP đặt I/O trải quá rộng về khoang kính; tâm chính xác phải theo bảng X 188/Z −116…−17. BOTTOM vẽ lọc gần giữa hơn vị trí chuẩn Z −1…129. Mặt lưng I/O, PSU và gá quạt chưa đúng tọa độ định lượng.
- Dãy PCIe có nét gân/nắp gây khó đếm, nhất là phối cảnh; dựng đúng **3 nắp và 3 vít**, không chép mọi dải đứng trong ảnh. Phối cảnh vẫn còn trụ góc dày và chưa tách rõ hông thép; bỏ trụ đặc tại góc hai kính, dựng khoang phụ và ống rỗng theo bảng.
- Mũi tên ở hai gá bên chưa diễn tả đúng chiều +Z; không hiểu thành hút qua kính. Số chân đế, chốt, đai, tâm lỗ gá và tai riser chưa thể hiện đủ để đếm. Nét vòng phía sau lưới không đại diện rotor.
- Bảng vít ảnh bị hỏng dòng nóc và lặp PCIe; dùng tổng 17 vít ở trên. Hình đầu dây chỉ minh họa, PLED không có lưỡi USB; số tiếp điểm và vị trí bịt lấy từ bảng đầu dây.

Các số trong đặc tả tổng và file này là nguồn chuẩn; ảnh dùng để đọc hình dáng và bố cục.


<details>
<summary>Lệnh imagegen đã dùng (bản đầu và lần sửa duy nhất)</summary>

```text
Use case: infographic-diagram. Create ONE high resolution landscape product reference sheet for a 3D modeller, white background, realistic matte charcoal steel #24262A, smoked glass #B8C4CA, thin blue #2164AC dimensions, crisp SHORT English labels, numbered callouts, no logos. Title "03 SHOWCASE DUAL CHAMBER / 01 CASE". Subtitle "EMPTY CHASSIS / mm". Use attached concept for silhouette, materials and two adjoining glass walls ONLY; remove ALL PC components, fans, radiator, hoses, hub and riser cable. Keep only chassis, panels, empty mounting rails, standoffs, I/O cables, filters, GPU support and THREE vertical PCIe blank covers.

Layout: six clearly separated orthographic views at SAME mm scale in two aligned rows on left 65% of sheet; ONE large exploded 3/4 on right 35%. Row1 FRONT +X, BACK -X, LEFT +Z; row2 TOP +Y, BOTTOM -Y, RIGHT -Z. Each view distinctly labelled. Front/back W300 H450; sides D450 H450; top/bottom W300 D450. Overall body 300 W x450 H x450 D, feet20 and total height470 dimensioned separately. Side glass front edge at image right in LEFT; front at image LEFT in RIGHT. FRONT glass portion on image left, opaque secondary chamber on right. BACK secondary chamber on image left. TOP front at bottom, BOTTOM front at top. Four chamfered feet50 X x36 Z x20 H centers X+/-180 Z+/-112.

Coordinates body center O: X rear(-225) to front(+225), Y bottom(-225) to roof(+225), Z steel side(-150) to glass side(+150). Assembled glass panels in all orthographic views; only 3/4 panels exploded with arrows. Broad LEFT glass435x422x4, X-215..220 Y+/-211 Z146..150. FRONT glass169x422x4, X221..225 Z-24..145 Y+/-211, remainder front solid steel hiding secondary chamber. Two glasses meet at thin sealed corner, NO thick pillar. Each glass bonded to slim metal cassette with2 bottom pins and2 hidden upper M3x6 screws, no through-glass screws. Steel RIGHT side has TWO separate filtered vents: PSU130x150 at X-215..-85 Y-170..-20 (rear low); side-bank129x259 at X78..207 Y-112..147 (front tall). Two captive rear thumbscrews at X-209 Y+/-190.

Internal partition face Z-36.5, back -37.7, secondary bay111.3 deep, main bay182.5 deep. Motherboard tray only X-221..74, nine brass M3 H6.5 standoffs at X=-201,-89,23 crossed with Y=-117.5,25,167.5. CPU access100x100 center X-70 Y84. Real separate hollow SIDE INTAKE DUCT at X78..207 Y-112..147 extending Z-148..-35, 1mm walls along edges, open both ends, NEVER a solid tray across duct! At duct main-chamber end exactly TWO EMPTY120mm mounts with centers(X,Y)=(142,-45),(142,80), mount plane Z-35, 105mm square hole pitch. Show arrows air from steel side -Z through isolated duct to +Z, NO intake through front glass.

Roof vent370x141 at X-190..180 Z-8..133 with removable filter and cassette2M3x6 at rear X-213 Z-15/132, Y225. Independent inner240 radiator rails remain on frame, NO radiator; eight holes105mm squares around(X,Z)=(-75,65),(45,65) at Y219, rail effective thickness3. Bottom filtered vent384x130 X-197..187 Z-1..129, filter pulls FRONT +X. Exactly THREE EMPTY120 floor mounts at X=-132,-10,112 Z63, mounting plane Y-209, holes105square. Rear one EMPTY120 vent center Y97 Z77, plane X-222,105square four holes. No rotors anywhere.

BACK: empty ATX I/O aperture159 high x37 wide at Y6.5..165.5 Z-28.9..8.1, shield seat X-225.2..-224.4, no ports installed. Empty upright PSU opening86 wide Z x150 high Y at X-222, Y-170..-20 Z-137..-51,4 retaining tabs/holes, no PSU/C14/switch. EXACTLY THREE tall VERTICAL PCIe blank plates below rear fan, Y-113..7, Z55.3..116.26, pitch20.32, three retention screws along top, NO horizontal slot bank. GPU support rail300x18x2 along X-221..79,Y-117..-115,Z75..93,2M3 screws, raised rubber support at X45. Riser adapter mounting ears100 apart, empty, no adapter/cable installed. No PSU shroud over showcase floor.

3/4 from +X/+Y/+Z: rear left front right, exploded left glass toward+Z, front glass toward+X, steel side toward-Z, roof cassette slightly+Y. Thin blue removal arrows. Clearly expose motherboard tray/G holes, hollow side duct, THREE empty floor mounts, TWO empty side mounts, ONE rear mount, empty roof rails, GPU support and rear3 vertical blanks. No additional perspective/invented miniature case views. Numbered leaders:1 Side glass,2 Front glass,3 Side intake duct,4 Tray,5 GPU support,6 PCIe x3,7 Filters,8 Top I/O,9 Feet.

Grommets on tray Z-36.5, EPDM rim3: G0 center(-49,-170)60x28; G1(-149,193)34x14; G2(54,79)26x65; G3(-30,193)44x14; G4(54,-20)24x30; G5(6,-145)46x14; G6(-183,-145)40x14. Label each exact G0..G6. Cable channel30 wide centered X54, lower channel Y-180,8 straps in secondary bay.

Top I/O on SOLID secondary bay roof X188 Y225: power14 Z-116, reset4 Z-89,2 USB-A6x13 Z-65/-42,ONE combo3.5 Z-17. No USB-C or mic. Cyan power ring only. Small connector detail strip: USB19 2x10 with pin20 blocked, HD AUDIO9 2x5 with pin8 blocked, PWR SW2, RESET SW2, PLED2 +/-. All wires black.
Compact fastener schedule: "GLASS 4 M3x6 / STEEL SIDE 2 THUMB / ROOF 2 M3x6 / GPU SUPPORT 2 M3x6 / PCIe 3 UNC6-32x6 / FEET 4 M4x8 / STANDOFFS 9 M3 H6.5". Note "Future mounts: case fans6x120 / roof AIO240 only". Materials steel frame1.2 panels1.0, mesh0.8 with2.4 holes pitch3.6, glass4 with black10mm border. Prioritize six complete matching orthographic views, empty chassis, correct3 vertical blank plates, fully open intake duct and legible concise labels. Do not fill unused space with fake text.
```

```text
Correct this reference sheet once, keep same title, white/blue style and six orthographic views plus one exploded perspective. Important mechanical corrections:
1 BACK must have exactly THREE vertical PCIe blank covers, not four. Three separate tall strips Y-113..7, bank Z55.3..116.26 with one screw each at top. They are on image RIGHT half, below rear empty fan. Eliminate all extra vertical/horizontal blanks. Same exactly3 in perspective.
2 BACK upright empty PSU aperture must be on image LEFT lower half in secondary chamber: 86mm wide Z x150mm high Y, from Z-137..-51 Y-170..-20. This is a TALL rectangle, not wide landscape opening. Remove the current bottom-wide PSU opening. ATX I/O aperture159high x37wide belongs above and right of PSU, near central partition (Z-28.9..8.1,Y6.5..165.5). Rear empty120 fan upper RIGHT at Z77 Y97, no visible fan rotor. Rear lower right can be solid perforated steel, not a second PSU bay.
3 RIGHT -Z orthographic is viewed from opposite side, so FRONT is image LEFT. Move tall129x259 side intake vent to LEFT upper/middle, small130x150 PSU vent to RIGHT lower. Captive two rear screws on image RIGHT. Mirror the current vent placement while preserving correct short labels.
4 TOP +Y has front at BOTTOM and glass +Z at RIGHT: vent occupies RIGHT side and solid secondary chamber strip LEFT side. Place ALL FIVE top I/O controls in a HORIZONTAL row along FRONT/BOTTOM on solid roof at X188,Y225, spanning Z-116,-89,-65,-42,-17 (power/reset/USB/USB/audio left to right). Not a vertical line along depth. Remove all controls from FRONT vertical panel: front opaque right strip plain, no buttons or ports. BOTTOM front UP,+Z RIGHT; bottom vent on RIGHT showcase chamber, not centrally through closed chamber.
5 Main views must be same physical scale: front/back300wide x450high; left/right450wide x450high; top/bottom300wide x450deep. Front/back same width. Feet20 separate.
6 Exploded perspective: NO thick corner pillar between glass front and glass side; thin sealed seam only. No fans/radiator/hardware. Remove rear steel side panel outward -Z in this SAME exploded perspective so its two vents and secondary bay are visible; do not add another perspective. GPU support stays300x18x2 with pad at forward end. Hollow SIDE INTAKE DUCT connects rear-side intake through separated113mm depth channel to exactly2 EMPTY120 mounts beside tray. Three empty bottom mounts, one rear empty mount, eight roof radiator holes.
7 Side-bank airflow arrows from -Z toward +Z should point into showcase bay, not through front glass.
8 Numbered legend7 must read "Filters (top / bottom / side)" not rear: rear fan exhaust has no filter. Make connector footer clear: USB19 2x10 pin20 blocked; HD AUDIO9 2x5 pin8 blocked; PWR SW2; RESET SW2; PLED2 +/-. No blue USB-style tongues on PLED.
Keep G0..G6 seven holes, dimensions and screw schedule. Update SIDE DUCT table geometry to "X78..207 / Y-112..147 / Z-148..-35". Retain overall300x450x450 body, total470. No extra views, logos, electronic components, or illegible filler.
```

</details>
