# 04 / cooler — Tản nhiệt nước AIO 240

Ảnh: [04_cooler.png](04_cooler.png). Dựng theo [00_blueprint.md](00_blueprint.md), mục 1–5 và 7. Đơn vị mm; 100 mm = 1 đơn vị cảnh. Số đo dưới đây là mốc dựng, không đo ngược từ ảnh. Đây là bộ linh kiện chung cho mô phỏng.

## Kích thước và vị trí

Trục toàn máy: X từ lưng ra trước, Y từ đáy lên nóc, Z từ vách kín ra kính. Các chi tiết nhỏ chưa có trong blueprint được ghi là **chọn thêm**.

| Chi tiết | Kích thước và cách đặt |
|---|---|
| Bơm tròn | Ø54 × cao 48, trục bơm theo Z. Tâm X = −69, Y = 121,5, trùng CPU; mặt đáy áp vào mặt tản nhiệt CPU, mặt nắp hướng kính. |
| Đế tiếp xúc | **Chọn thêm:** đĩa đồng Ø42, nằm trong chiều cao bơm; màng bảo vệ trong có tai bóc. |
| Vòng cyan | **Chọn thêm:** dải cao 2 quanh mép nắp, đường kính ngoài 54. Vật liệu màu, không thêm dây đèn. |
| Gá trước | Bốn tai quanh đáy bơm; tâm lỗ tạo hình vuông 78 × 78, tâm trùng bơm. **Chọn thêm:** thép dày 2, đầu tai rộng 12, mép bo tròn. |
| Tấm đỡ sau bo | 90 × 90; bốn trụ liền có lỗ ren M3 bên trong, cùng lưới 78 × 78. **Chọn thêm:** tấm dày 2, đệm cách điện dày 1, trụ Ø6; chỉnh chiều cao trụ khớp gá và vít S2. Không thêm vít giữ trụ. |
| Két | 277 theo X × 120 theo Z × 27 theo Y. Giới hạn X = −140…137, Z = −24…96, Y = 204…231. Hai vùng quạt có tâm X = −61,5 và 58,5; Z = 36. |
| Hai quạt | Mỗi chiếc 120 × 120 × 25, tâm cùng hai vùng két, Y = 179…204. Mặt có nan đỡ motor hướng lên két; gió đi theo +Y. Cụm lắp kín dày 52, hở nóc 4. Ảnh tách quạt chỉ để thấy cách ráp. |
| Hai ống | Mỗi ống dài khoảng 350 theo đường tâm, Ø ngoài 11, bọc sợi đen. Tổng bốn đầu nối: hai ở bơm, hai ở két. Uốn cong mềm, không cắt qua RAM hoặc cánh quạt. |
| Đầu nối và cổng nạp | **Chọn thêm:** hai đầu ống trên bình đầu két phía +X, lệch ±15 quanh Z = 36, cút tại bơm quay về phía két. Cổng nạp có nắp Ø10 trên bình đầu, tránh đầu ống và lỗ vít. Cổng này bịt kín, không có thao tác tháo trong bài học. |

Két lệch về kính, không đè lên mép bo hoặc RAM. Giữ ít nhất 10 mm tới kính sau khi tính mép khung. Không dùng vị trí tách rời trên ảnh làm vị trí lắp thật.

## Vật liệu và màu

| Vùng | Vật liệu / màu |
|---|---|
| Vỏ bơm, khung két, gá | Nhựa hoặc kim loại sơn mờ `#16181D` |
| Lá két, cánh và khung quạt | Nhôm đen / nhựa đen `#101216`; lá két mỏng, sát nhau |
| Ống và dây điện | Cao su bọc sợi đen `#202329`, đầu cắm nhựa `#101216` |
| Điểm nhấn | Cyan `#22D3EE`, chỉ vòng bơm |
| Đế đồng | Đồng hơi bóng `#B87333` |
| Vít, lò xo, vòng đệm | Thép `#929AA3`; gá sau sơn đen, đệm cách điện `#30343B` |

## Đầu cắm và dây

Mọi đầu PWM có một hàng bốn vị trí, bước chân **chọn thêm 2,54**; dựng gờ dẫn hướng và dấu chân 1. Đánh số theo dấu chân 1 trên cặp đầu khớp nhau, tránh đảo thứ tự khi đổi mặt nhìn. Thứ tự: **1 GND, 2 +12 V, 3 TACH (báo tốc độ), 4 PWM**.

| Đầu / dây | Chân và vị trí |
|---|---|
| Bơm → AIO_PUMP | Đầu dây cái 1 × 4, đủ bốn tiếp điểm; cắm header đực tại (10; 126) cục bộ bo, tức (−78; 188,5) toàn máy. Dây dài 350, ra ở cạnh bơm gần cút ống. Tải bơm chọn dưới 1 A. |
| Hai dây quạt | Mỗi quạt có đầu cái 1 × 4 đủ tiếp điểm, dây ra góc khung gần rãnh két. Cắm vào hai nhánh đực của bộ chia Y. |
| Y → CPU_FAN | Đầu chung cái 1 × 4 đủ tiếp điểm; header đực tại (35; 126) cục bộ bo, tức (−53; 188,5) toàn máy. Đuôi Y dài 300, mỗi nhánh dài 150. Tổng tải hai quạt chọn dưới 1 A. |
| Nhánh A của Y | Vỏ bốn vị trí, đủ bốn chân đực 1–4; chỉ nhánh này trả tín hiệu tốc độ. |
| Nhánh B của Y | Vỏ bốn vị trí, chỉ ba chân đực ở **1, 2, 4**. Vị trí 3 để trống; vẫn có chân PWM số 4. |

Dây bơm giấu trong vỏ bọc dọc một ống nước, chỉ lộ khoảng 20 ở gốc. Dây quạt men rãnh khung két; bộ chia nằm sau khay. Đi qua G3 tại (−65; 224), vòng qua mép trên bo, không xuyên PCB. Đoạn cuối lộ 20–30 tới AIO_PUMP, 20–35 tới CPU_FAN. Hình đầu cắm dùng đoạn ngắn và ký hiệu ngắt để dễ nhìn; hai ống nước vẫn hiện đầy đủ. Không có dây SATA, RGB/ARGB hay bộ điều khiển rời.

## Lỗ và vít

| Mã | Số lượng / loại | Vị trí và hướng lắp |
|---|---|---|
| S2 | 4 vít M3 × 12 có lò xo | Bốn lỗ xuyên trên tai gá, Ø3,4 **chọn thêm**, đối diện bốn lỗ ren M3 trong trụ đỡ. Tâm cục bộ bo: (−20; 20), (58; 20), (−20; 98), (58; 98). Tâm toàn máy: (−108; 82,5), (−30; 82,5), (−108; 160,5), (−30; 160,5). Vít đi từ phía kính về bo, theo −Z. |
| S3 | 8 vít M3 × 6 + 8 đệm dày 1 | Từ nóc xuống tám lỗ ren mặt trên két, theo −Y. Qua giá nóc dày 1 và đệm 1, ren ăn khoảng 4. |
| S4 | 8 vít M3 × 30 + 8 đệm dày 1 | Từ dưới xuyên bốn góc mỗi quạt lên tám lỗ ren mặt dưới két, theo +Y. Qua khung quạt 25 và đệm 1, ren ăn khoảng 4. |

Lưới két/quạt: X = **−114; −9; 6; 111**, kết hợp mỗi giá trị với Z = **−16,5; 88,5**, thành tám điểm trên mỗi mặt két. Mặt trên ở Y = 231, mặt dưới ở Y = 204. Mỗi quạt có bốn lỗ xuyên, bước tâm 105 × 105; Ø lỗ quạt **chọn thêm 4,5**. Két có 8 lỗ ren trên và 8 lỗ ren dưới, không nối thành lỗ xuyên qua đường nước.

Tổng bộ này: **20 vít giữ, 16 vòng đệm, 4 lò xo trên S2**. Không cộng bốn trụ liền thành vít. Chiều dài vít tính dưới đầu; dùng đầu chữ thập. Ren M3 là lựa chọn cho mô hình này.

## Người mới cần nhìn thấy

1. Hai quạt nằm dưới két và thổi lên nóc; nhìn nan đỡ motor và mũi tên để đặt đúng mặt.
2. Vít dài S4 đi xuyên quạt; vít ngắn S3 giữ két vào nóc. Đổi nhầm vít dài có thể chạm đường nước.
3. Bóc màng đế bơm, đặt keo mỏng lên CPU, rồi siết bốn vít lò xo luân phiên theo đường chéo.
4. Bơm cắm AIO_PUMP; hai quạt qua Y cắm CPU_FAN. Chỉ một nhánh Y có chân báo tốc độ.
5. Hai ống luôn nối bơm với két; giữ dây điện gọn, tránh cánh quạt. Không mở nắp nạp nước trong bài học.

## Tạo ảnh

Dùng công cụ imagegen tích hợp. Lệnh gốc và lệnh sửa một lần được lưu bên dưới; dùng cùng nhau để tái tạo bản cuối.


**Sai khác còn lại của ảnh sau một lần sửa:** nhóm S4 vẫn chỉ vẽ 6 vít dài và 6 vòng đệm dù nhãn ghi 8; khi dựng phải tạo đủ **8 vít + 8 đệm**. Hình nghiêng nhánh B của Y còn thừa chân; dùng sơ đồ mặt đầu có ô 3 trống và bảng chân ở trên để dựng **chỉ chân 1, 2, 4**. Những điểm này chưa đạt yêu cầu hình ảnh, không được sao chép vào mesh.

<details>
<summary>Lệnh imagegen gốc và lệnh sửa</summary>

```text
Use case: infographic-diagram.
Create ONE landscape high-resolution product reference sheet for a 3D modeller, titled "04 / COOLER", subtitle "240 mm AIO · Dimensions in mm". White/light grey background, realistic dark materials, soft studio light, thin blue dimension lines, navy short readable English labels, cyan accent #22d3ee. Brand-free, no logos, no watermark. This is a detailed exploded assembly reference for a generic gaming PC cooler.

COMPOSITION: large 3/4 isometric hero occupies left/center 70%; two smaller technical detail panels at right: "PUMP / UNDERSIDE" and "CONNECTORS". Bottom organized hardware strip. Generous whitespace. Numbered small callouts 1 Pump, 2 Hoses, 3 Radiator, 4 Fans, 5 Mount, 6 Wiring. All parts fully in frame.

HERO: exactly ONE radiator 277 long x120 wide x27 thick, with realistic dense black fins, two 120mm mounting bays, 8 threaded holes visible on top perimeter. Each bay has a 105x105 hole grid. Capped sealed fill port clearly visible on end tank with label "Fill port · sealed". Exactly TWO black sleeved coolant hoses, each approximately 350 long, outer diameter 11, curved gently and attached at BOTH ends: two metal swivel elbows on one round pump, two fittings on one radiator end tank. No disconnected hose ends. Pump cylinder diameter 54 x48 tall, matte charcoal #16181d, plain top and thin cyan accent ring, no text or logo on pump. Four-ear mounting bracket on pump base, exactly four through holes on 78x78 square. Show separate 90x90 rear backplate behind pump with exactly four integral threaded posts, no extra fasteners. Cyan is a material accent, no RGB wires.
Exactly TWO 120x120x25 fans pulled off BELOW radiator, lying in same plane as radiator with generous clear exploded gap, side by side aligned to the two radiator bays. Four visible corner holes per fan and realistic seven-blade rotor, rear support struts on upper exhaust face toward radiator. Blue dashed assembly alignment lines. Two prominent cyan arrows point UP from fan intakes through radiator toward roof, label "Airflow UP". Tiny ghost roof reference line ABOVE radiator, not a third object/fan. Dimension radiator 277,120,27; one fan 120x120x25; assembled stack note "27 + 25 = 52". No case or motherboard hero.

PUMP / UNDERSIDE small inset: view of same pump underside, smooth copper contact plate diameter 42, protective film with peel tab, bracket holes 78x78 with dimensions. Label "Peel film". Same pump shown as detail, not an additional component. Backplate 90x90 label in hero.

CONNECTORS inset: short tidy black cable ends only, with diagram breaks indicating hidden full wire lengths. Separate pump female 1x4 socket labeled "AIO_PUMP". Show its keyed guide rails and tiny pin-1 triangle, all FOUR contact openings. Beside it depict a clear Y splitter: female 1x4 motherboard plug labeled "CPU_FAN", splitting into TWO male fan branches, both with keyed 4-position housings. Branch A has 4 actual pins, label "A: 4 pins"; Branch B has pins only at positions 1,2,4, position 3 empty, label "B: no tach". Diagram a keyed female 4-contact fan cable at each corresponding branch. Small precise pin legend "1 GND | 2 12V | 3 TACH | 4 PWM". Make missing tach on B unambiguous. No SATA, RGB, USB or extra pump controller. Wiring in hero shows only short ends, pump cable otherwise hidden alongside hose sleeve.

BOTTOM HARDWARE STRIP: three spacious separate labeled groups, exact rendered screw counts, do not scatter additional screw objects around hero.
Group S2 "PUMP · 4 x M3x12": exactly FOUR identical spring-loaded pump screws arranged one row of four. Each includes a coil spring, all countable.
Group S4 "FAN · 8 x M3x30": exactly EIGHT long Phillips screws arranged two rows of four; exactly one thin washer beside EACH screw, 8 washers total. Label "1 mm washers".
Group S3 "ROOF · 8 x M3x6": exactly EIGHT visibly short Phillips screws arranged two rows of four; exactly one thin washer beside EACH screw, 8 washers total. Label "1 mm washers".
TOTAL exactly 20 loose screw objects (4 spring, 8 long, 8 short), 16 washers. Hero screw holes remain empty, with no duplicate fastener drawings in insets. Labels "Long: through fans" and "Short: roof to radiator". Thin blue leaders associate groups with intended holes. Make long versus short unmistakable.
Avoid dense prose, branding, extra fans, extra hoses, incorrect screw counts, illegible microtext, dramatic dark background. Artwork must be a clean realistic modeling sheet, not a cartoon or wireframe.
```

```text
Correct this cooler reference sheet, preserving its layout, readable dimensions, white background, charcoal materials, cyan pump ring, two sleeved hoses, one radiator, sealed fill port, and exactly two detached fans. Do not add logos.
Make these exact local corrections:
1. Completely redraw S4 hardware group as TWO ROWS, exactly FOUR long M3x30 screws per row = EIGHT total. Beside each screw exactly ONE washer = EIGHT washers total. Use a clean 4-column grid with enough space. Existing six screws and excess washers are wrong.
2. S3 group: TWO ROWS of FOUR short M3x6 screws = EIGHT total, with exactly ONE washer per screw = EIGHT total. Remove every extra washer. S2 retains exactly FOUR spring screws. All three groups total 20 screws, 16 washers.
3. Fan orientation: both detached fans have their upper faces pointing toward radiator. Add four clear fixed rear motor support struts spanning frame to motor over the blades on each fan, making these the exhaust faces. Retain the upward airflow arrows and all four mounting holes on each fan. Only two fans.
4. Backplate has four integral cylindrical posts with a visible threaded FEMALE M3 bore on each top. Replace current male screw studs; these posts receive the four spring screws from front.
5. Y splitter: branch A must show FOUR exposed male pins, inline. Branch B must show exactly THREE exposed male pins at slots 1,2,4; slot 3 is EMPTY. Remove third pin, not fourth. Keep the enlarged numbered front face with slot 3 blank. Ensure each nearby fan female plug has four contact openings and distinguish it from the male splitter branch. CPU_FAN and AIO_PUMP female mainboard plugs each have four inline socket openings, keyed guide ribs and pin-1 mark.
6. Add small callout "6 Wiring" at the connector panel.
Do not change other geometry, text, subject counts or image style.
```

</details>
