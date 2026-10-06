# 09 — Quạt vỏ (`fans`)

Ảnh: [09_fans.png](09_fans.png). Theo [00_blueprint.md](00_blueprint.md), mục 2–5 và 7. **1 đơn vị cảnh = 100 mm.** Bộ này có **4 quạt vỏ: 3 hút trước, 1 thổi sau**. Hai quạt két thuộc phần 04, không tính ở đây.

## Kích thước và cấu tạo

Mỗi quạt **120 × 120 × 25 mm**, tâm bốn lỗ cách nhau **105 × 105 mm**. Quy ước riêng: gốc giữa mặt hút; `u` ngang, `v` lên trên, `d` từ mặt hút tới mặt thổi. Khung nằm trong u, v = −60…60; d = 0…25. Các số chi tiết bên dưới ngoài kích thước tổng và bước lỗ là lựa chọn dựng hình chung.

| Phần | Kích thước / vị trí chọn, mm |
|---|---|
| Khung nhựa | Bao ngoài 120 × 120 × 25; mép bo 2; miệng tròn Ø114, tâm u = v = 0 |
| Cánh | Đúng 7 cánh cong, phân bố đều; đường kính quét Ø110, cách miệng khung 2 mỗi phía; chiều dày cánh khoảng 1,5; nằm trong d = 3…21 |
| Hub / chụp tâm | Ø40 × sâu 20, đồng tâm rotor; mặt hút trơn, không nhãn |
| Nan giữ động cơ | 4 nan cố định rộng khoảng 5, dày 3, ở mặt thổi d = 22…25; không quay cùng cánh |
| Góc chống rung | 4 miếng cao su dạng ôm góc khoảng 20 × 20, lớp mặt dày 2, nằm trong kích thước tổng; lỗ xuyên trùng lỗ khung |
| Mũi tên đúc nổi | Trên thành ngoài dễ thấy, nổi 0,4, dài khoảng 10; một mũi tên theo +d chỉ gió, một theo tiếp tuyến chỉ chiều quay |
| Dây ra | Từ góc phía khay, gần (u; v) = (−52; 52); có cổ chống gập, dây đi sát khung và tránh vùng cánh |

Khung nhựa nhám `#16181d`; cánh và hub `#252932`; cao su `#101216`; dây và đầu nhựa `#17191c`; vít thép phủ đen `#353a42`; tiếp điểm `#b8bec5`. Nét xanh `#1673df` chỉ là chú thích. Không thêm LED, dây RGB hoặc logo.

Mặt hút là mặt thoáng; mặt thổi có nan giữ động cơ. Chọn rotor quay **theo chiều kim đồng hồ khi nhìn thẳng vào mặt hút**; hướng cong và độ nghiêng cánh phải đẩy gió từ mặt hút sang mặt nan. Hai mũi tên phải là hình nổi cùng màu khung, không chỉ là mũi tên trên ảnh.

## Bốn lỗ và vít S5

Tâm lỗ cục bộ `(u; v)` là **(−52,5; −52,5), (52,5; −52,5), (−52,5; 52,5), (52,5; 52,5)**. Mỗi tâm cách hai mép gần nhất 7,5 mm. Trục lỗ song song d; chọn lỗ mồi Ø4,3 trong tai nhựa, cao su có lỗ hở tương ứng. Đây là bốn vị trí bắt, không tính hai miệng của một lỗ thành hai lỗ.

Mỗi quạt dùng **4 vít tự tạo ren Ø5 × 10**, đầu tròn chữ thập; chiều dài 10 tính từ dưới đầu. Không dùng vít M3 dài của két, không thêm vòng đệm rời. Vít đi từ ngoài vỏ, xuyên tôn 1 mm rồi ăn vào tai quạt gần tôn. Quạt trước bắt từ phía mặt trước vào mặt hút; quạt sau bắt từ ngoài lưng vào mặt thổi. Cả bộ có **16 vít S5**, gồm 12 trước và 4 sau. Ảnh lớn chỉ tách bốn vít của một quạt mẫu; rotor vẫn ráp trong khung.

## Đầu PWM và đường dây

Mỗi quạt có một dây liền gồm **4 sợi**, kết thúc bằng đầu **PWM cái, 1 hàng × 4 hốc**. Tiếp điểm nằm chìm; dựng gờ/rãnh dẫn hướng lệch trên vỏ để khớp gờ của SYS_FAN và thêm tam giác chân 1. Vỏ đầu chọn khoảng 10 × 5 × 12 mm, bước hốc 2,54 mm; đây là kích thước dựng gần đúng. Không thay bằng đầu nguồn ATX hoặc đầu RGB.

Thứ tự chân theo dấu chân 1: **1 GND, 2 +12 V, 3 TACH (báo tốc độ), 4 PWM (điều khiển)**. Khi dựng mặt cắm và mặt dây, giữ cùng số chân, không tự đảo sơ đồ trái/phải giữa hai góc nhìn.

| Quạt | Đường dây theo blueprint | Đầu trên bo, tọa độ cục bộ X; Y | Đoạn cuối lộ |
|---|---|---|---|
| Trước trên | Rãnh mặt trước → G2 → sau khay → G2 | SYS_FAN1, (106; 85) | Khoảng 25 |
| Trước giữa | Rãnh mặt trước → G2 → xuống sau khay → G5 | SYS_FAN2, (82; −128) | 20–35 |
| Trước dưới | Rãnh mặt trước → G5 → ghim sau khay → G5 | SYS_FAN3, (95; −134) | 20–30 |
| Sau | Góc giáp khay → rãnh nóc → G1 → rãnh sau → G6 | SYS_FAN4, (−96; −135) | 25–35 |

Cho phép dây dài tới 650 mm để đủ đường đi; chốt chiều dài theo tuyến dựng thực tế. Ảnh chỉ hiện đoạn ngắn khoảng 30–40 mm và dấu ngắt dây, không phải chiều dài toàn bộ. Giấu dây thừa sau khay hoặc trong khoang nguồn, không để chạm cánh. Dây thuộc quạt; phần 10 mô tả tuyến đi, không tạo dây trùng.

## Bốn vị trí trong vỏ

Tọa độ **toàn máy** dùng đúng blueprint: +X hướng mặt trước, +Y lên nóc, +Z ra kính. Nhìn từ kính, lưng ở trái và mặt trước ở phải.

| Vị trí | Tâm (X; Y; Z), mm | Miền bề dày X | Hướng gió |
|---|---|---|---|
| Trước trên | (209,5; 165; 0) | 197…222 | Từ ngoài trước vào máy, −X |
| Trước giữa | (209,5; 40; 0) | 197…222 | Từ ngoài trước vào máy, −X |
| Trước dưới | (209,5; −85; 0) | 197…222 | Từ ngoài trước vào máy, −X |
| Sau | (−211,5; 145; 14) | −224…−199 | Từ trong máy ra lưng, −X |

Cả bốn quạt có mặt hút hướng +X, mặt nan hướng −X. Tâm lỗ trong mặt YZ lệch ±52,5 theo Y và Z so với tâm từng quạt. Lấy `u` theo +Z, `v` theo +Y, `d` theo −X khi đặt cụm vào máy. Nắp che nguồn kết thúc ở X = 170 để không che quạt trước dưới. Hình vỏ nhỏ chỉ chỉ ra bốn quạt vỏ; không vẽ lại hai quạt két.

## Năm điểm người mới cần thấy

1. Ba quạt trước hút vào, một quạt sau thổi ra; nhìn mũi tên trên khung trước khi lắp.
2. Mặt có nan giữ động cơ là mặt gió đi ra trong mẫu quạt này.
3. Mỗi quạt cần đủ bốn vít S5 và bốn góc cao su; bắt vừa chặt, lần lượt theo đường chéo.
4. Đầu PWM có bốn hốc và gờ định hướng; cắm đúng SYS_FAN1–4, không ép đầu khi lệch gờ.
5. Ghim dây sát khung rồi đưa sau khay; kiểm tra không có dây nằm trong vùng cánh quay.

## Tạo ảnh

Công cụ: imagegen tích hợp, không dùng CLI. Kích thước và tọa độ trong tài liệu là mốc dựng; không đo tỉ lệ từ ảnh tạo sinh.

Đã kiểm tra sau một lượt sửa: đủ 7 cánh, 4 góc cao su, 4 vít rời, đầu PWM 4 hốc, hai mũi tên nổi và sơ đồ 3 hút trước + 1 thổi sau. **Các vít rời trong ảnh vẫn tỏa theo bố cục minh họa, chưa song song đúng trục lắp**, nhất là vít trên phải. Khi dựng chuyển động, cho cả bốn vít đi song song trục quạt và dùng hướng bắt trước/sau đã ghi ở mục S5. Mũi tên gió đi ngang bề dày khung; mũi tên quay ở cạnh phải hướng xuống.

<details>
<summary>Lệnh tạo và sửa ảnh (tiếng Anh)</summary>

### Lệnh tạo

```text
Use case: infographic-diagram.
Create ONE high-resolution landscape product reference sheet for a 3D modeller, title "09 / CASE FANS", subtitle "120 mm / PWM / Dimensions in mm". White to very light grey background, soft studio lighting, realistic matte charcoal plastic #16181d, dark blades #252932, black rubber #101216, thin blue dimension lines, numbered callouts with short readable English labels. Brand-free, no logos, no stickers, no watermark.

Layout: one large three-quarter isometric fan hero on LEFT, two smaller panels stacked on RIGHT. Top-right is "CASE / GLASS SIDE" placement inset. Bottom-right is "4-PIN PWM / DETAIL" connector enlargement. Generous whitespace, no crowded labels.

HERO: exactly ONE assembled square case fan, 120 x 120 x 25 mm, seen from its open intake face, seven clearly distinct curved blades around a plain circular hub, hub diameter about 40 mm, rotor diameter about 110 mm. Four stationary motor-support struts are on the opposite exhaust face, behind blades, subtly visible. Square frame with a round opening, rounded corners. Exactly FOUR black rubber anti-vibration corner pads, each with ONE empty mounting hole, one at each corner. Center spacing of the four holes is 105 x 105 mm; each hole center is 7.5 mm from adjacent frame edges. Show dimension lines for 120 width, 120 height and 25 thickness, plus a restrained 105 hole-center dimension.
EXPLODED MOUNTING: exactly FOUR loose short coarse-thread Phillips pan-head fan screws, one aligned with each corner mounting hole, floated out from the intake face along the same axial direction. Four blue dashed screw-to-hole alignment lines. Screws nominal diameter 5 mm, under-head length 10 mm. All four screws and all four holes must be visible and countable without overlaps. Label "S5 / 4 x 5 x 10". Do not dismantle the rotor from frame and do not add other screws or washers.
Two physically MOULDED raised arrows in the same charcoal plastic appear on the visible outside frame sidewall: one points axially from intake face to exhaust/strut face for airflow, the other tangentially for rotation. These are real relief geometry, not just floating blue arrows. A clear leader "5 Moulded arrows" identifies them; small blue explanatory labels "AIRFLOW" and "ROTATION" nearby. Choose clockwise rotation viewed from intake; blade curvature and the moulded rotation mark agree.
Callouts: "1 Frame", "2 7 blades", "3 Hub", "4 Rubber corners", "5 Moulded arrows", "6 PWM cable". A tidy four-conductor black cable exits a frame corner through a strain relief, follows the outer rim without entering blade sweep, and ends in a female PWM plug; show only a short 30-40 mm illustrative tail with a cable break mark for omitted length. No RGB cable.

PWM DETAIL: enlarge the SAME cable connector, not a second physical cable. Black female housing, exactly FOUR square socket cavities in ONE row, all four visibly countable, recessed metal contacts. Four parallel black wires at rear. Show the offset side guide/key rails that mate with a motherboard fan header guide, not a big ATX latch. Label "4 sockets / 1 x 4" and "Key". Mark pin 1 with small triangle. Small legend "1 GND   2 12V   3 TACH   4 PWM", ordered to correspond to the shown cavity numbering. No exposed male pins.

CASE INSET: generic 220 wide x 470 high x 450 deep mid-tower shown from glass side in slight three-quarter cutaway so front and rear fan faces are recognizable. REAR on LEFT, FRONT on RIGHT. Only four case fan assemblies inside a pale ghosted chassis: EXACTLY THREE 120 mm fans vertically stacked at FRONT RIGHT, and EXACTLY ONE 120 mm fan at upper REAR LEFT. No top fans, no GPU fans, no unnecessary internal components. Three blue intake arrows point from OUTSIDE RIGHT through each front fan toward LEFT into case. One rear exhaust arrow points from inside through rear fan to OUTSIDE LEFT. Labels "FRONT / 3 INTAKE" and "REAR / 1 EXHAUST". Both intake and exhaust air travel right-to-left in this view. Small caption "4 fans / 16 screws total". Front fan vertical centers bottom-to-top at -85, 40, 165 mm relative case center; rear center Y=145 mm. Lower-front fan remains unobstructed, PSU shroud stops before the front fan column.
No other views or extra fan samples. Keep English text crisp, short and mechanically precise. This is a modelling reference, not advertising.
```

### Lệnh sửa

```text
Edit this fan reference sheet with these precise mechanical corrections, preserving the layout, all seven blades, four rubber corners, exactly four loose screws, four empty holes, four female PWM cavities, labels, and correct case inset with three front intake plus one rear exhaust.
1. The moulded AIRFLOW arrow on the visible right frame SIDEWALL incorrectly points vertically upward. Replace it with a SHORT STRAIGHT arrow ACROSS the 25 mm depth of the sidewall, from the front/intake edge toward the back/exhaust edge. In this perspective this is approximately LEFT-TO-RIGHT, slightly upward along the receding thickness edge. It must not point vertically along the 120 mm frame height. Label AIRFLOW with leader to this short axial raised arrow.
2. The moulded ROTATION arrow beneath it must indicate CLOCKWISE as viewed from intake: on the RIGHT edge it therefore points DOWNWARD, tangentially toward bottom of fan. Make this a separate vertical down-pointing arrow raised in charcoal plastic. Remove the extra small central arrow so there are exactly TWO moulded arrows.
3. Improve screw explosion: all four screws point from intake side toward their respective holes along parallel fan-axis lines. Heads nearer viewer, tips toward holes; translate the screws outward along ONE consistent direction in perspective, toward lower-left from each hole. Do not put the upper-right screw on the back/exhaust side. Keep exactly four screws total with one per mounting hole, no screw inside rotor area. Move nearby labels as needed.
4. The 25 mm dimension must span FRONT TO BACK outer faces of the frame on a visible thickness edge, preferably at right lower side. Remove the misleading existing 25 dimension at bottom left. Preserve 120 width, 120 height and 105 hole spacing.
Do not change the case airflow arrows: all correctly point LEFT. Keep the four fan positions and the keyed PWM detail unchanged. No logos or extra parts.
```

</details>
