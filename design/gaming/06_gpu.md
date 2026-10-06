# 06 — Card đồ họa (GPU)

Ảnh: [06_gpu.png](06_gpu.png). Dựa trên [bản tổng thể](00_blueprint.md), mục 1–5 và 7; kích thước thân khớp `src/data/builds.ts`. Các kích thước chi tiết bên dưới là lựa chọn để dựng mô hình chung, không phải bản vẽ chế tạo.

## Kích thước và hướng lắp

- Thân dài **280 × nhô khỏi bo 125 × dày 55 mm**; trong cảnh là **2,80 × 1,25 × 0,55**.
- GPU nằm ngang; ba quạt hướng xuống đáy máy, tấm lưng hướng lên nóc. Lá cổng ở phía sau máy; đầu còn lại hướng ra mặt trước.
- Lá bắt chiếm **hai cửa PCIe**, bước cửa **20,32 mm**. Thân 55 mm lấn gần ba khoảng khe, phải để trống vùng ngay dưới.
- Quy ước đo chi tiết: **u** từ lá cổng tới đầu trước (0–280), **v** từ mép bo tới mép ngoài hướng kính (0–125), **w** từ lưng xuống quạt (0–55). Khi lắp, u cùng chiều +X, v cùng chiều +Z, w cùng chiều −Y của bản tổng thể.

## Các khối chính

| Khối | Kích thước và vị trí chọn, mm | Vật liệu, màu |
|---|---|---|
| Vỏ quạt | Bao thân 280 × 125; thành khoảng 2; mặt quạt tại w = 55. Góc vát, các dải chéo hẹp quanh lỗ quạt | Nhựa đen nhám `#16181D`; điểm nhấn kim loại xám `#4B5058` và bạc tối `#8D949C` |
| Ba quạt hướng trục | Mỗi quạt Ø80 × dày khoảng 12; tâm u = 50, 140, 230; v = 62,5. Cánh nằm chìm trong vỏ | Nhựa `#101216`, tâm trơn, không logo |
| Khối tản nhiệt | Khoảng 270 × 110 × 34, giữa PCB và cụm quạt; lá mỏng chạy đều, nhìn thấy ở cạnh và cửa thông lưng | Nhôm `#A3A9B0`, nhám nhẹ |
| PCB | Bao khoảng 278 × 121 × 1,6, sát mặt lưng; khoét thông dưới cửa tấm lưng, giữ dải mép ngoài đỡ ổ nguồn | `#202327`; chân tiếp xúc vàng `#CBA34D` |
| Tấm lưng | Khoảng 278 × 120 × 2 ở w = 0–2; cửa cắt vát khoảng 48 × 78 gần đầu trước, u = 224–272 | Kim loại đen `#25282D`; cửa cắt xuyên thật, thấy lá tản bên dưới |
| Lá cổng sau | Tôn dày 1; mặt hữu dụng khoảng 120 × 40,64; có tai gập và lưới thoát khí | Thép bạc `#B9BEC5` |
| Ổ nguồn | Vỏ khoảng 18 × 10 × 12, gần đầu trước trên mép ngoài; tâm u khoảng 260–270, v khoảng 119 | Nhựa đen `#111317`; có gờ giữ ngàm |

Không có chữ, nhãn dán, logo hoặc tên sản phẩm trên card. Không thêm đèn hay ổ nguồn phụ.

## Đầu cắm

| Đầu cắm | Số chân / hình dạng | Vị trí và cách dựng |
|---|---|---|
| PCIe x16 | **164 tiếp điểm**, 82 mỗi mặt; đoạn ngắn và dài ngăn bởi rãnh khóa | Mép hướng bo, gần lá cổng. Dải tiếp xúc dài khoảng 89, cao lộ khoảng 8; không kéo dọc cả thân. Tách rãnh khóa giữa các tiếp điểm với móc/rãnh giữ ở cuối PCB. Căn tâm theo khe đầu tiên: X/Y cục bộ bo = −34/−30 |
| Nguồn PCIe | **8 vị trí, 2 × 4**; nhận đầu dây **6+2** | Một ổ trên mép ngoài thường gọi là “top edge”, gần đầu trước. Dựng lỗ vuông/lỗ vát và gờ giữ ngàm; không dùng hình khóa của EPS 4+4 |
| DisplayPort ×3 | **20 tiếp điểm mỗi cổng**; miệng có một góc vát | Trên lá cổng sau; ba lỗ riêng, lòng tối và vỏ kim loại. Miệng mô phỏng khoảng 16 × 5 |
| HDMI ×1 | **19 tiếp điểm**; miệng hình thang | Cùng hàng với ba cổng trên; miệng khoảng 14 × 5 |

Xếp bốn cổng dọc chiều 120 mm của lá cổng, theo thứ tự DP–DP–DP–HDMI; chọn tâm cách nhau khoảng 24 mm, phần còn lại làm lưới thoát khí. Tiếp điểm rất nhỏ có thể dùng texture, nhưng miệng cổng phải có chiều sâu.

**Điều chỉnh theo yêu cầu lần này:** ổ 8 chân nằm trên mép trên ngoài, miệng hướng kính (+Z), thay cho miệng hướng +X ở đầu card trong bản tổng thể. Khi dựng dây số 10, cần uốn đoạn cuối từ G4 để đi thẳng vào ổ, giữ bán kính uốn tối thiểu 20 mm và tránh quạt; không dùng nguyên đoạn thẳng 25 mm cũ. Ảnh này để ổ trống để nhìn rõ tám vị trí.

## Lỗ và vít giữ

- **S6: đúng 2 vít #6-32 UNC × 6 mm**, đầu chữ thập, màu bạc; chiều dài tính dưới đầu vít.
- Mỗi tai của lá bắt có một lỗ/rãnh hở rộng khoảng 3,8 mm. Hai tâm cách nhau **20,32 mm** theo chiều chồng khe, phải trùng lỗ vỏ.
- Trong hình chính, đặt mỗi vít hơi tách khỏi tai tương ứng; hình phụ chỉ lặp lại lỗ, không thêm vít rời.
- Vít giữ từ ngoài lưng máy vào vỏ. Vỏ quạt và tấm lưng là cụm đã ráp sẵn; vít nội bộ nếu có chỉ là chi tiết mờ, không tính vào S6 hay tạo thao tác tháo.

## Năm điểm người mới cần thấy

1. Ba quạt quay mặt xuống đáy; tấm kim loại trơn ở phía trên.
2. Chân vàng vào khe x16 đầu tiên, khớp rãnh rồi ấn thẳng. Chốt giữ thuộc bo mạch chủ; nhả chốt trước khi rút card.
3. Nguồn GPU dùng đầu 6+2 ghép kín thành tám chân, không lấy đầu CPU 4+4.
4. Cần đủ hai vít ở lá sau; lá hai khe vẫn đi cùng thân dày 55 mm.
5. Dây màn hình cắm vào một trong bốn cổng của GPU. Lắp SSD phía sau vùng GPU trước khi lắp card.

## Lệnh tạo ảnh

Dùng công cụ imagegen tích hợp. Ảnh diễn tả hình dáng; lấy số đo và số lượng trong tài liệu này để dựng.

Đã kiểm tra sau một lần sửa: ba quạt, hai vít S6, ba cổng DP và một HDMI, ổ nguồn tám vị trí, tấm lưng có cửa thoát gió, không có thương hiệu. Đường dóng “55 BODY” trong ảnh vẫn chưa bám đúng hai mặt đo; khi dựng, lấy 55 theo trục w từ lưng đến mặt quạt, không đo theo mũi tên hoặc tỷ lệ ảnh.

```text
Use case: product-mockup / technical reference sheet.
Create ONE high-resolution landscape product reference sheet for a 3D modeller, title "06 / GPU", subtitle "Dimensions in mm". White very light grey background, soft studio light, realistic physically believable materials, thin blue dimension lines and short crisp English numbered callouts. Generous whitespace. Product is ONE generic mainstream gaming graphics card, recognisable familiar triple axial fan add-in board silhouette, matte black shroud with dark gunmetal and silver angular accents. ABSOLUTELY NO logos, brands, product names, NVIDIA, GeForce, RTX, fake stickers or watermark anywhere. No RGB.

LAYOUT: large hero occupies upper 65% of sheet; two small supporting views along bottom. Main view is a low three-quarter view FROM BELOW, clearly displaying all THREE fan circles in a row, actual underside facing camera, rear silver bracket on left, front tip on right. Floating product, no motherboard or case. Long axis 280 mm, depth from motherboard edge to outer top edge 125 mm, body thickness 55 mm. EXACTLY THREE separate black axial fans diameter 80 mm, plain blank hubs, swept blades, left/middle/right centers spaced evenly. Matte black angular molded shroud, thin diagonal dark silver trim around fan openings; tightly spaced aluminum heatsink fins visible through long side. The black metal backplate sits on the opposite broad face from the fans, not over them. Edge of backplate visible in hero.
Gold PCIe x16 contact fingers MUST clearly project from the BOARD-FACING long edge of the thin black PCB, opposite the power-socket edge, near rear bracket, with the small key gap dividing short and long contact sections and the separate shaped retention hook/notch at the end toward the front tip. Do not turn this into a full 280 mm strip. Callout "2 PCIe x16" plus "Key gap + latch notch".
ONE and only ONE black PCIe power receptacle on the OUTER TOP EDGE near the front tip, same location in every view, keyed 2 rows x 4 cavities = EIGHT positions, visible latch catch. No cable attached. User specifically wants this top-edge socket, not an end-facing socket. Callout "3 PCIe power / 8-pin".
Two-slot silver stamped metal I/O bracket at rear left, with vent perforations and FOUR real recessed output ports, THREE DisplayPort and ONE HDMI. Two mounting ears with two mounting openings. Show exactly TWO loose silver Phillips screws next to their mounting ears with dotted alignment lines. No other loose screws. Callout "4 Bracket / 2 slots", "5 S6 / 2 screws", "#6-32 UNC x 6".
Blue dimension arrows for "280", "125", and "55 BODY" correctly aligned to respective axes. Fan callout "1 3 x Ø80 fans". Small note "Fans face case floor".

BOTTOM LEFT supporting view "REAR BRACKET": clear straight-on view of SAME silver two-slot bracket, exactly four ports, in one vertical stack of 3 DisplayPort with one chamfered corner each and 1 HDMI with trapezoidal opening. Labels "DP 20" beside each of three DP ports, "HDMI 19" beside HDMI. Show vent perforations beside stack. TWO mounting ears/slots, pitch dimension "20.32". Do not show additional loose screws in this inset, since only TWO total screw objects should be on sheet.
BOTTOM RIGHT supporting view "BACKPLATE + POWER": small upper three-quarter view of SAME card showing black metal backplate, large angular flow-through vent cut near front tip, heatsink fins visible THROUGH vent, top-edge 8-pin receptacle enlarged sufficiently to count all eight keyed cavities, two rows of four and latch catch. Callouts "6 Backplate vent" and "8-pin / 2 x 4 / latch". Backplate plain, no decoration that resembles a word. Geometry must match main card.

Footer "280 x 125 x 55 mm" and "2-slot bracket / 55 mm body". Keep all labels clean and readable. Correct mechanical arrangement and counts take priority. No exploded cooling parts, no extra fans, no extra sockets, no attached cable.
```

Lệnh sửa một lần để đưa hai lỗ S6 về cùng mép gập, sửa phía ổ nguồn trong hình phụ và vị trí đường đo độ dày:

```text
undefined
```
