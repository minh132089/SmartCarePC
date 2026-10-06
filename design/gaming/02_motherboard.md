# 02 — Bo mạch chủ ATX

Ảnh: [02_motherboard.png](02_motherboard.png). Nguồn kích thước và vị trí: [00_blueprint.md](00_blueprint.md), mục 1–4 và 7. Bo đứng riêng, nhìn thẳng mặt linh kiện, kèm một góc nghiêng để thấy chiều cao và cổng I/O. Chưa lắp CPU, RAM, SSD, GPU hoặc dây.

## Kích thước và hướng dựng

- PCB **244 ngang × 305 cao × 1,6 dày**, đơn vị mm; 100 mm = 1 đơn vị cảnh.
- Gốc ở tâm bo: X sang phải, Y lên trên, Z ra phía người nhìn. Mép X = ±122, Y = ±152,5; mặt lưng Z = 0, mặt PCB Z = 1,6. Mốc đặt linh kiện `surface = 3` theo mã nguồn; đây không phải giới hạn chiều cao đầu cắm.
- Tâm bo trong máy: (−88; 62,5; −96). I/O ở mép trái; EPS ở trên; ATX ở mép phải. Không lật gương khi dựng góc nghiêng.
- Các kích thước phụ ghi “chọn” dưới đây dùng cho mô hình chung. Lưới lỗ là lưới mô phỏng của blueprint, không dùng làm bản khoan bo thật. Ưu tiên số liệu trong tài liệu khi nét ảnh khác vị trí.

## Các khối chính

Kích thước phụ theo X × Y × chiều cao trên mặt PCB, trừ khi ghi khác.

| Cụm | Kích thước / vị trí tâm (X; Y) | Chi tiết cần dựng |
|---|---|---|
| Socket LGA | Bao ngoài 50 × 50 tại **(19; 59)** | Tiếp điểm lõm, khung ép bạc có cửa giữa, bản lề, cần khóa và dấu tam giác chân 1; không đặt CPU trong ảnh |
| DIMM A1, A2, B1, B2 | X = **70; 79; 89; 98**, Y = **62**; chọn mỗi thân 6 × 142 × 12 | Bốn khe DDR4 trống, rãnh khóa lệch tâm và hai chốt mỗi khe; đánh dấu A2/B2 bằng chữ hoặc nét cyan |
| Ba PCIe | Tâm **(−34; −30), (−34; −91), (−34; −132)**; chọn thân 89 × 9 × 11 | Khe trên x16 có vỏ gia cường bạc; hai khe dưới đen, cùng hình dạng dài cho mô hình; cả ba có chốt ở đầu +X |
| M.2 | Miệng **(−5; −62)**; chân đỡ **(75; −62)** | Vùng SSD 80 × 22 chạy ngang, tâm (35; −62); chỉ vẽ nét biên vùng lắp, không dựng thành thanh SSD; chân đỡ cao 3 |
| Tản VRM | Chọn 18 × 68 × 18 tại (−41; 59), và 44 × 12 × 16 tại (10; 110) | Hai khối bạc có rãnh nông; tránh lỗ gá bơm và đầu quạt phía trên |
| Tản chipset | Chọn 38 × 30 × 9 tại (75; −97) | Khối bạc thấp, không che khe M.2 hay đầu cắm dưới bo |
| I/O liền bo | Chọn lá chắn dày 0,8, dài theo Y 158, cao theo Z 36; mép X = −122, Y = −38…120 | Cổng mở ra −X; cụm không tháo trong bài học. Giữ thông thoáng lỗ H1 và H4 ở mép trái |

## Đầu cắm

Tọa độ là tâm đầu cắm, riêng M.2 là miệng khe. Mọi đầu có dấu chân 1, gờ định hướng và ngàm phù hợp. Các đầu dây cắm theo +Z, riêng I/O mở ra −X và M.2 nhận SSD từ phía +X.

| Tên | Số chân / tiếp điểm | Tâm (X; Y) và nhận biết |
|---|---|---|
| Socket LGA | 1.200 tiếp điểm quy ước | (19; 59); dùng họa tiết tiếp điểm nhỏ, không dựng chân trên CPU |
| DIMM A1/A2/B1/B2 | 288 mỗi khe DDR4 | Các tâm ở bảng trên; giữ đúng thứ tự trái sang phải |
| PCIe đầu tiên | x16, 164 tiếp điểm | (−34; −30); vỏ bạc và chốt phải. Hai khe dưới là vị trí dự phòng, blueprint không quy định số lane điện |
| M.2 M-key | Khung 75 vị trí, 67 tiếp điểm | (−5; −62); thể hiện phần khóa M, vít giữ ở (75; −62) |
| ATX | 24 = 2 × 12 | **(117; 40)**; thân chọn 10 × 52 × 13, trục dài theo Y; ngàm hướng ra mép phải |
| EPS CPU | 8 = 2 × 4 | **(−60; 146)**; thân chọn 18 × 9 × 13; ghi `CPU`, lỗ vuông/vát có khóa |
| AIO_PUMP | 4: GND, 12 V, tốc độ, PWM | **(10; 126)**; hàng bốn chân có gờ, cấp điện trực tiếp cho bơm |
| CPU_FAN | 4 | **(35; 126)**; nhận dây chia hai quạt két, chỉ một nhánh trả tốc độ |
| SYS_FAN1 | 4 | **(106; 85)**; quạt trước trên, phía trên ATX |
| SYS_FAN2 | 4 | **(82; −128)**; quạt trước giữa |
| SYS_FAN3 | 4 | **(95; −134)**; quạt trước dưới |
| SYS_FAN4 | 4 | **(−96; −135)**; quạt sau |
| F_PANEL | 9 trên khung 2 × 5; thiếu chân 10 | **(95; −143)**; dùng tám chân, chân 9 để trống |
| USB 3.0 | 19 trên khung 2 × 10 | **(113; −127)**; thân chọn 8 × 20 × 9, trục dài Y; một vị trí khóa, không lấn vùng trống quanh H9 |
| HD_AUDIO | 9 trên khung 2 × 5; thiếu chân 8 | **(−97; −143)**; gờ khóa khớp lỗ bịt trên đầu dây |
| I/O: USB-A ×2 | USB 2.0, 4 mỗi cổng | Cụm mép trái; chọn tâm Y = 96 và 78; cửa chữ nhật có lưỡi nhựa |
| I/O: RJ45 ×1 | 8 | Cùng lá chắn, chọn tâm Y = 51; thấy hốc cắm và rãnh ngàm |
| I/O: jack 3,5 mm ×3 | TRS, 3 mỗi cổng | Cùng lá chắn, chọn tâm Y = 22; 8; −6; ba hốc tròn riêng |

Các tâm cổng I/O trên là lựa chọn bố trí, cùng cao Z = 15. Chọn thân đầu quạt khoảng 10 × 5 × 8; F_PANEL và HD_AUDIO khoảng 13 × 5 × 7. Không thêm SATA, RGB/ARGB, đầu nguồn GPU hay ổ modular lên bo.

Sơ đồ F_PANEL nhìn thẳng mặt bo, dấu chân 1 ở góc dưới trái. Đây là sơ đồ riêng của mô phỏng:

| | Cột 1 | Cột 2 | Cột 3 | Cột 4 | Cột 5 |
|---|---|---|---|---|---|
| Hàng trên | 2 PLED + | 4 PLED − | 6 PWR SW | 8 PWR SW | 10 khuyết |
| Hàng dưới | 1 HDD LED + | 3 HDD LED − | 5 RESET SW | 7 RESET SW | 9 không dùng |

Hai nút không phân cực; hai đèn có cực +/−. USB và âm thanh phải thể hiện đúng vị trí thiếu chân, không dùng khối kín thay cho hàng chân.

## Lỗ và vít

| Nhóm | Số lượng / loại | Vị trí |
|---|---|---|
| H1–H3, hàng trên | 3 lỗ xuyên Ø3,4 | (−112; 142,5), (0; 142,5), (112; 142,5) |
| H4–H6, hàng giữa | 3 lỗ xuyên Ø3,4 | (−112; 0), (0; 0), (112; 0) |
| H7–H9, hàng dưới | 3 lỗ xuyên Ø3,4 | (−112; −142,5), (0; −142,5), (112; −142,5) |
| S1b / S1a | **9 vít M3 × 5**, đầu tròn chữ thập; khớp **9 chân đế M3 cao 6,5** trên khay | Một bộ tại mỗi H1–H9; chừa vùng không linh kiện bán kính 4 quanh tâm. Chân đế thuộc vỏ, chỉ minh họa một bộ với nhãn ×9 |
| Lỗ gá bơm S2 | **4 lỗ riêng**, chọn Ø3,5 thông trụ gá; khớp bốn vít lò xo M3 × 12 của AIO | (−20; 20), (58; 20), (−20; 98), (58; 98): hình vuông 78 × 78. Không cộng vào chín lỗ giữ bo; bộ gá/tấm đỡ 90 × 90 thuộc AIO |
| S7a / S7b | **1 chân đỡ M2 cao 3**, chọn thân Ø4; **1 vít M2 × 2,5** đầu thấp | (75; −62); chân đỡ lắp sẵn trên bo, vít giữ SSD khi lắp |

Khung ép CPU, chốt DIMM và PCIe không có vít tháo rời trong bài học. Lỗ gá bơm không phải lỗ bắt vít CPU. Không tạo điểm tương tác trên vít cố định socket hay tản chipset.

## Vật liệu

PCB mờ `#16181D`, roughness khoảng 0,8; nhựa đầu cắm `#101216`, roughness 0,55. Tản nhiệt và khung socket nhôm/thép bạc `#AEB4BC`, metallic 0,85, roughness 0,4; lá chắn I/O `#BCC2C8`. Tiếp điểm vàng nhạt `#C6A565`, chữ in bo `#D2D7DD`. Nét chú thích xanh `#2164AC`; dấu A2/B2 cyan `#22D3EE` là chỉ dẫn, không phát sáng. Không logo, tem giả hoặc chữ trang trí.

## Năm điểm người mới cần thấy

1. CPU đặt theo tam giác và rãnh khóa, rồi hạ khung ép và cần gạt; không dùng vít để lắp CPU.
2. Hai thanh RAM đi vào **A2 và B2**. Khớp rãnh lệch tâm rồi ấn thẳng cho chốt giữ đóng.
3. GPU dùng khe x16 bạc đầu tiên; nhả chốt trước khi tháo. SSD cắm nghiêng vào M.2, hạ xuống chân đỡ rồi bắt một vít, nên lắp trước GPU.
4. ATX 24 và EPS CPU 8 là hai đầu nguồn khác nhau. Bơm vào AIO_PUMP, hai quạt két qua dây chia vào CPU_FAN, bốn quạt vỏ vào SYS_FAN1–4.
5. Chín lỗ bo phải khớp chín chân đế. Đầu USB/âm thanh có khóa; F_PANEL phải đúng cặp chân và cực đèn.

## Tạo ảnh

Dùng công cụ imagegen tích hợp, một lượt tạo và một lượt sửa theo brief. Lệnh thực tế được lưu bên dưới để tái tạo; số đo và sơ đồ chân phía trên là dữ liệu để dựng.

Kiểm tra ảnh cuối: mặt chính có bốn DIMM, ba PCIe và đủ H1–H9; có góc nghiêng, sơ đồ F_PANEL và chi tiết vít. Ảnh vẫn giản lược sai một số hàng chân nhỏ, vị trí nhóm đầu cắm mép dưới và hướng mặt I/O trong hình chính; góc nghiêng thể hiện dư cửa USB. Khi dựng, dùng bảng trên: đầu quạt **1 × 4**, ATX **2 × 12**, F_PANEL **9 chân** tại (95; −143), SYS_FAN4 ở trên HD_AUDIO và I/O chỉ **hai USB-A**. Không sao chép số chân hoặc vị trí từ các chi tiết ảnh này.

```text
Use case: infographic-diagram.
Create one high resolution landscape product reference sheet for a 3D modeller, titled "02 / MOTHERBOARD", subtitle "Generic ATX - dimensions in mm". White / very light grey background, soft studio light, realistic dark matte PCB #16181d, black sockets #101216, subtle brushed silver VRM and chipset heatsinks, thin blue dimension lines and short numbered English callouts. Brand-free, no logos, product names, watermark or decorative nonsense text.

Composition: dominant large FACE-ON ORTHOGRAPHIC front view of the bare motherboard on left/center, upright portrait board 244 wide x 305 high. A small angled three-quarter view of the SAME board at upper right, from rear I/O side so the actual recessed ports in its integrated silver I/O shield are visible. Lower right compact F_PANEL pin diagram and mounting detail. Motherboard alone: NO installed CPU, RAM, SSD, GPU, cooling unit, case, or cables. Empty slots expose keys and latches. All four DIMM slots visible and labelled A1 A2 B1 B2; A2/B2 have restrained cyan diagram marks, not lights.

Use exact component placements relative to board center (0,0), X right, Y up; board edges X +/-122, Y +/-152.5. Top edge is EPS edge, rear I/O along upper left edge.
CPU socket center (19,59), outer frame 50x50, dense LGA contact texture, silver hinged load plate with open central window, clearly visible locking lever at side; no CPU. Four cooler holes distinct from board mounting holes at (-20,20), (58,20), (-20,98), (58,98), annotate "Cooler 78 x 78".
EXACTLY FOUR long empty vertical DDR4 DIMM slots, centers X=70,79,89,98 all Y=62, length about 142; key and end latches visible.
EXACTLY THREE horizontal long PCIe sockets with retention latch at RIGHT, centers (-34,-30), (-34,-91), (-34,-132); first x16 has silver metal reinforcement, other two black.
EXACTLY ONE empty M.2 M-key connector mouth at (-5,-62) facing right and ONE threaded standoff at (75,-62). The empty SSD footprint is horizontal 80 x 22, mark with thin dashed technical outline and "M.2 2280 / empty". No installed SSD, no M.2 heatsink hiding socket or standoff.
ATX 24-pin at (117,40), narrow tall housing near right edge with exactly 2 columns x12 cavities, latch on outward right edge.
EPS 8-pin at (-60,146), exactly 2x4 cavities along top edge, label "EPS 8 / CPU".
Six individual four-pin headers, each with four gold pins, keyed guide and pin-1 mark: AIO_PUMP (10,126), CPU_FAN (35,126), SYS_FAN1 (106,85), SYS_FAN2 (82,-128), SYS_FAN3 (95,-134), SYS_FAN4 (-96,-135). Ensure all six are drawn and identified separately.
F_PANEL at (95,-143): 2x5 footprint with 9 pins, pin 10 absent.
USB 3.0 at (113,-127): black keyed 2x10 header housing, 19 pins and one absent keyed position, oriented long axis vertically.
HD_AUDIO at (-97,-143): 2x5 footprint, nine pins with pin 8 absent.
Rear I/O fixed to left edge: exactly 2 USB-A rectangular ports, 1 RJ45 port and 3 circular 3.5mm audio ports, clearly visible in small angled view. Ports point outward left from board.
VRM heatsinks simple silver blocks to left and above socket, chipset simple low silver block in lower right near (75,-97), no oversized ornamental covers; preserve connector access.

Mounting: EXACTLY NINE board mounting holes, diameter 3.4, metallic annuli, in 3x3 grid X=-112,0,112 by Y=-142.5,0,142.5. All nine uncovered and visible. Mark them H1-H9 row-major from top left, fine blue circles and leader "9 x M3 x 5". These are a custom simulation grid, not standard production drilling. Four cooler holes are visibly a separate group, not part of nine. Small mounting section shows ONE representative M3x5 round Phillips screw over PCB thickness 1.6 and hex standoff height 6.5, label "x9"; also ONE tiny M2x2.5 screw near M.2 standoff with height 3. No additional loose hardware.

Fine 244 horizontal and 305 vertical dimension arrows around hero board. Short numbered callouts around exterior, avoid crossing lines or obstructing slots: 01 LGA + lever, 02 DDR4 A1 A2 B1 B2, 03 PCIe x16, 04 M.2 M-key, 05 ATX 24, 06 EPS 8 CPU, 07 AIO_PUMP / CPU_FAN, 08 SYS_FAN1-4, 09 USB 3.0 19, 10 F_PANEL 9, 11 HD_AUDIO 9, 12 Rear I/O, 13 Mounts x9. Place exact individual fan header labels beside each actual header. Small coordinate text for socket "(19,59)", PCIe "(-34,-30)", M.2 "(-5,-62) to (75,-62)", A2 "(79,62)", B2 "(98,62)".

F_PANEL diagram at lower right: exactly two rows by five columns viewed face-on; top row "2 PLED+" "4 PLED-" "6 PWR" "8 PWR" "10 absent"; bottom row "1 HDD+" "3 HDD-" "5 RESET" "7 RESET" "9 unused". Pins 6 and 8 form PWR pair; 5 and 7 form RESET pair. Small caption "Simulation pinout". Readable deliberate typography, no long prose. Keep generous whitespace, visually accurate socket cavities and mounting holes, realistic materials, all objects fully inside sheet.
```

Lệnh sửa:

```text
Edit this motherboard reference sheet, preserving the white background, blue technical annotations, realistic matte black/silver materials, dominant face-on hero, small angled view, dimensions, correct F_PANEL inset and mounting details. Correct the following visible errors, and apply the same geometry corrections to both board views:
1. There must be exactly THREE PCIe slots total: the silver full-length top socket and two black full-length lower sockets. DELETE the extra short black PCIe slot located just left of M.2 between the top silver and middle black slots. Remove the incorrect extra PCIe x1/x4 and PCIe x1 printed callouts; simply label lower sockets "PCIe 2" and "PCIe 3". Keep their centers at (-34,-91) and (-34,-132).
2. There must be exactly NINE visible circular motherboard mounting holes in the specified 3 by 3 grid. Restore the missing center H5 hole at the exact PCB center (0,0): horizontally halfway between H4 and H6 and vertically halfway between H2 and H8, immediately above the silver PCIe slot. Clear small decorative components from radius 4 mm around it, add metallic annulus and blue circle label "H5 (0,0)". Keep the other eight holes in place and the four separate cooler holes.
3. Every fan/pump header must be ONE ROW OF FOUR PINS, not two rows or 6/8 pins. Correct AIO_PUMP and CPU_FAN near the top, SYS_FAN1 at upper right, SYS_FAN2 and SYS_FAN3 lower right, SYS_FAN4 lower left. Each must have exactly four gold pins and a keyed plastic guide. Move SYS_FAN4 to immediately ABOVE HD_AUDIO, not beside it: center (-96,-135); HD_AUDIO stays (-97,-143). Ensure the same in angled view. Repair actual printed label "AIO_PUMP" and "SYS_FAN2", keep labels legible and no fake words.
4. Make front-view ATX 24 housing show exactly 2 columns by 12 cavities, not a shorter 2x10 array. Keep center (117,40), latch on right.
5. Rear I/O exactly TWO USB-A openings total, ONE RJ45 opening and THREE audio jack holes in both views. The angled view currently shows four USB-A holes: remove two, leaving exactly two. I/O ports physically face out from the LEFT EDGE; in orthographic face-on hero show tops of metal port blocks and a narrow edge of shield, not a flat frontal elevation of all port openings on top of PCB. The small angled view should clearly reveal actual port openings facing left.
6. Remove any stamped text/logo from socket load plate. Preserve the lever and empty LGA socket, four empty labelled DIMM slots A1 A2 B1 B2, single empty M.2 connector and standoff. Keep all main blueprint component placements.
Keep the correct F_PANEL 2x5 diagram with pin10 absent, dimensions 244 x305, screws M3x5 x9 and M2x2.5, no brand logos. This is the final corrected sheet.
```
