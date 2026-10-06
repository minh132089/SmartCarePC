# 07 — SSD M.2 NVMe 2280

Ảnh: [07_ssd.png](07_ssd.png). Dựng theo [00_blueprint.md](00_blueprint.md), mục 1–4 và 7. Có **1 SSD, 1 chân đỡ, 1 vít giữ**; các hình nhỏ là góc nhìn khác của cùng bộ này. Khe và chân đỡ thuộc bo mạch chủ, không tạo thêm bản sao khi ghép cảnh.

## Kích thước và vị trí

- SSD dài **80**, rộng **22**, dày cả linh kiện **không quá 3 mm**. PCB chọn dày **0,8 mm**, chỉ gắn chip ở mặt trên.
- **1 đơn vị cảnh = 100 mm**. Trục dài theo X của bo; tâm SSD tại **(35; −62)**, đầu cắm tại **(−5; −62)**, tâm vít tại **(75; −62)**.
- Tọa độ riêng: **u = 0…80** từ đầu cắm đến đầu vít; **v = −11…11** theo bề ngang; **w = 0** ở mặt dưới PCB. Khi lắp phẳng: X = u − 5, Y = v − 62.
- Mặt đỡ bo dùng Z cục bộ = 3 theo blueprint. Chân đỡ cao **3 mm** nên mặt dưới SSD ở Z = 6; toàn bộ SSD ở Z = 6…9. Không nhầm chân đỡ SSD với chân đế bo cao 6,5 mm.
- Các kích thước nhỏ bên dưới là lựa chọn dựng mô hình chung; dùng số trong tài liệu, không đo theo nét ảnh tạo sinh.

| Bộ phận | Số lượng, kích thước và vị trí chọn (mm) |
|---|---|
| PCB | 1 tấm 80 × 22 × 0,8; mặt dưới không có chip |
| Controller | 1 khối 10 × 10 × 1,2; tâm (u; v) = (14; 0), đáy w = 0,8 |
| Flash NAND | 2 khối, mỗi khối 16 × 18 × 1,2; tâm (33; 0) và (54; 0), đáy w = 0,8 |
| Linh kiện nhỏ | Vài tụ/điện trở cao tối đa 0,8 trên PCB; tránh vùng tiếp điểm và vùng vít; không thêm chip lớn |
| Rãnh giữ vít | 1 rãnh bán nguyệt bán kính 1,2, tâm (80; 0), mở ra mép u = 80; vành kim loại bán kính ngoài 2,4 |
| Khe trên bo | Vỏ nhựa khoảng 24 ngang × 6 sâu × 4 cao; miệng tại (−5; −62), mở về +X; căn rãnh nhận PCB với cao độ mặt dưới SSD = 6 |
| Chân đỡ S7a | 1 trụ kim loại cao 3, đường kính ngoài chọn 4, lỗ ren trong M2; tâm (75; −62), có sẵn trên bo |
| Vít giữ S7b | **1 vít M2 × 2,5**, chiều dài tính dưới đầu; đầu thấp chữ thập, đường kính đầu chọn 3,5, cao 0,7 |

## Vật liệu và đầu nối

PCB nhám màu **#20252A**; ba chip đen **#111418**; khe cắm nhựa **#181B20**; tiếp điểm mạ vàng **#C8A65C**; vít và chân đỡ kim loại **#A8ADB4**. Đường mạch chìm nhẹ, không dán nhãn, không logo hoặc chữ trên chip. Xanh **#2463B5** chỉ dùng cho đường ghi chú trong ảnh.

Có **1 đầu M.2 M-key**, ở mép ngắn u = 0. Theo blueprint: khung **75 vị trí, 67 tiếp điểm** chia trên hai mặt PCB. Dựng các ngón tiếp xúc phẳng riêng nhau; không dựng chân nhọn. Chỉ có **một rãnh khóa lệch tâm**, gần phía v âm, tạo một dải tiếp điểm dài và một dải ngắn. Gờ trong khe phải khớp rãnh này. Không thêm rãnh B-key thứ hai. Đây là mô hình hình học đầu nối, không phải sơ đồ điện từng chân.

Phần giữ SSD có **1 rãnh bán nguyệt mở**, không phải lỗ tròn kín. Chân đỡ có **1 lỗ ren M2** nhận vít; ren ngoài ở đáy chân đỡ gắn sẵn vào bo theo S7a. Không thêm vít rời để bắt chân đỡ. Không có cáp SATA, cáp nguồn hoặc dây tín hiệu riêng.

## Chuyển động lắp và điểm người mới cần thấy

1. Lắp SSD trước GPU vì GPU che vùng này. Nhìn đúng rãnh M-key trước khi đưa vào khe.
2. Nâng đầu giữ vít khoảng **25°** so với mặt bo, trượt đầu tiếp điểm vào khe dọc theo thanh. Góc 25° là lựa chọn cho hoạt cảnh.
3. Khi đầu cắm đã vào, xoay thanh quanh đầu cắm để hạ đầu còn lại. Đầu vít đang nghiêng nằm gần khe hơn vị trí vít cuối; không kéo giãn thanh để ép hai điểm trùng nhau.
4. Hạ rãnh bán nguyệt lên chân đỡ **3 mm**. Khi nằm phẳng, PCB phải được đỡ và không cong.
5. Giữ bằng đúng **1 vít M2 × 2,5**. Đầu vít đè lên vành quanh rãnh; siết vừa đủ. SSD nhận cả nguồn và dữ liệu qua khe M.2.

## Lệnh tạo ảnh

Dùng công cụ **imagegen tích hợp**. Có một lượt sửa rãnh khóa ở hình nhìn từ trên.

<details>
<summary>Prompt gốc và prompt sửa (tiếng Anh)</summary>

```text
Use case: scientific-educational.
Create ONE landscape product reference sheet for a 3D modeller, for part 07 of a generic gaming PC. Title "07 / M.2 NVMe SSD", subtitle "2280 • Dimensions in mm". White / very light grey background, soft studio lighting, realistic physical materials, crisp thin blue dimension lines, navy typography, generous whitespace. No branding, no stickers, no writing printed on components. Image layout: ONE large three-quarter isometric hero across upper two thirds; TWO small secondary views across bottom, top view on left and installation side profile on right.

Subject: ONE single-sided M.2 2280 NVMe SSD, a slender 80 mm long by 22 mm wide plain dark charcoal PCB (#20252a), 0.8 mm PCB thickness, total populated thickness at most 3 mm. Exactly TWO identical large rectangular matte black NAND flash chips, and ONE smaller square matte black controller near connector. Position left to right: edge fingers, controller, flash chip, flash chip, empty end area and half-moon screw notch. No heatsink. Restrained tiny passive components and subtle traces, no additional large chips. Gold plated fingers on both faces at LEFT short end. Exactly ONE off-center M-key rectangular cutout in this contact edge, dividing a long bank of fingers from a short bank; never two cutouts, never a centered split. RIGHT short end has exactly ONE semicircular open notch centered across width with a metallic annular land, open to the end, not a fully enclosed hole.

HERO: on a small quiet cropped patch of dark motherboard, show black horizontal M.2 socket at LEFT. Socket opening faces RIGHT toward the SSD; its internal key matches the card notch. SSD's gold edge is partially inserted into this mouth, with right screw end raised at about 25 degrees above motherboard. Expose enough fingers to see the M-key. The two flash chips and controller face viewer/upwards. A single short blue arrow parallel to the inclined card points LEFT INTO socket, label "1 Insert at 25°". A blue curved downward arrow at the raised right tip shows rotation down about the seated connector, label "2 Lower end". Below the raised tip, at the position reached after lowering, show ONE cylindrical metallic standoff, height exactly 3 mm measured from motherboard top to SSD underside. Above this standoff show ONE loose low-profile Phillips screw, thread M2 and under-head length 2.5 mm, vertical axis matching standoff. Label "3 Secure • 1 × M2 × 2.5". Show dashed alignment axis, do not make SSD horizontal projection falsely line up before rotation. Clear numbered callouts with short labels: "4 M-key", "5 Controller", "6 Flash ×2", "7 Half-moon notch", "8 Standoff • 3 mm". Fine dimension lines for 80 length and 22 width.

BOTTOM LEFT: title "TOP / CONTACT DETAIL". Orthographic top view of the same bare SSD, LEFT gold connector entirely visible and RIGHT open half-moon notch. Maintain exactly 2 flash chips + 1 smaller controller and same orientation. Draw individual gold edge contacts divided by one M-key gap near one side. Label "M-key • 67 contacts / 75 positions". Do not put text on PCB. Dimensions "80" along long edge, "22" across short edge.

BOTTOM RIGHT: title "SIDE / LOWER TO SEAT". Technical side elevation of the same assembly in solid installed horizontal position and thin dashed outline at 25 degrees, pivot at LEFT socket. Curved blue arrow from raised dashed position to flat solid position. Show socket at left, SSD PCB resting level on ONE 3 mm standoff at right. The SAME single retaining screw is installed at right (this is another view of same assembly, not an extra fastener). Dimension the gap "3" vertically between motherboard top and SSD underside. Label "3 max" across full populated SSD thickness. Leave PCB visibly straight, not bowed. Small footer "1 SSD • 1 standoff • 1 screw" and "No cables".
Constraints: physically plausible thin elongated card, realistic material rendering, short legible English labels, all view geometry consistent. No cables, no SATA connectors, no brand logo, no stickers, no fake serial numbers, no extra screws, no extra flash chips, no extra M-key notches, no motherboard clutter, no hands. Blue graphics are annotations only.
```

```text
Edit this product reference sheet. Correct ONLY the connector key geometry and make its orientation consistent between views. The bottom-left TOP / CONTACT DETAIL currently wrongly has TWO rectangular cutouts in its gold contact edge, a B+M pattern. Remove the UPPER cutout completely: fill that cutout with continuous PCB and evenly spaced gold fingers so the entire upper and middle bank is continuous. Retain ONLY the LOWER off-center M-key cutout, aligned to the lower single notch already shown on the hero card. The final top view must have exactly ONE rectangular key cutout near its lower edge, creating one very long gold contact bank above and one very short bank below. The rounded mounting half-moon at the opposite right end remains unchanged. The top-view controller is left of the two NAND chips. Keep all other content, labels, dimensions, layout, materials, 2 flash + 1 controller, hero angled insertion and screw alignment, and side profile unchanged. No B-key notch, no second connector cutout. The gold finger edge in all views is single M-key.
```

</details>

