# 11 — Cổng sau của bo mạch ATX

Ảnh: [11_rear_io.png](11_rear_io.png). Bo chung 244 × 305 mm, không thương hiệu. Cụm mới có **15 chỗ cắm và 2 nút**, thay cho cụm I/O thưa trong tờ 02. Đây là thiết kế để dựng mô hình; chưa thay đổi mã nguồn hoặc mô hình 3D.

## Vị trí và chiều nhìn

Đơn vị: **mm**. Giữ hệ trục của `tools/build_gaming.py` và `src/data/gaming.ts`: gốc tại tâm bo, mặt lưng PCB ở Z = 0, mặt linh kiện ở **Z = 1,6**. Trong bảng, **h là chiều cao tính từ mặt linh kiện**, nên Z = 1,6 + h. Không cộng `IO_DY = 20` lần nữa vào các tọa độ đã ghi.

- Nhìn từ sau máy theo **+X**: +Y hướng lên, +h/+Z sang phải. Các cổng mở về −X; đầu dây đi vào theo +X.
- Cửa I/O ATX danh nghĩa: **158,75 theo Y × 44,45 theo Z**. Lá chắn của mô hình giữ **158 × 36 × 0,8**, tại **X = −136,2…−135,4; Y = −18…140; h = 0…36**. Kích thước 158 không phải làm tròn 158,75: đây là lá chắn nhỏ hơn, giữ đúng giới hạn hiện có.
- Cửa case hiện tại trong `build_gaming.py` là Y bo = −18,5…140,5 và Z bo = 1…39. Lá chắn Z = 1,6…37,6 nằm lọt trong cửa này. Không nới lá chắn lên 44,45 khi chưa sửa cửa case.
- Mép dưới lá chắn cao hơn đỉnh bracket GPU Y = −24 là **6 mm**, và cao hơn tâm PCIe đầu tiên Y = −30 là **12 mm**. Cổng thấp nhất vẫn nằm trong lá chắn.
- Mép PCB ở X = −122; mặt lá chắn nằm ngoài mép PCB 14,2 mm. Giữ mặt miệng các cổng gần X = −136,0, ngay sau lá chắn. Hai đầu anten được nhô ra phía −X thêm 4 mm.
- **Cạnh dài của tất cả USB-A chạy theo Y**, tức dựng đứng khi nhìn sau thùng tower. HDMI, DisplayPort và USB-C cũng đặt cạnh dài theo Y. Không xoay cả cụm 90° để xếp cổng nằm ngang.

## Bảng từng cổng

Số 01–17 trùng số trên ảnh. Mỗi hàng là một cổng hoặc một nút, số lượng **1**. Kích thước miệng chữ nhật ghi theo **Y × h**, không phải chiều ngang × chiều dọc của ảnh. Đây là kích thước chọn cho mô hình; không dùng làm bản gia công đầu nối. Màu chỉ phần nhựa hoặc vòng nhận biết, không phải toàn bộ vỏ kim loại. Số tiếp điểm không tính vỏ chống nhiễu, trừ đầu anten được ghi riêng.

| Số / nhãn ảnh | Loại | SL | Tâm (Y; h) | Miệng cắm / mặt nút | Màu hex | Tiếp điểm mỗi cổng | Người mới cắm gì / dùng làm gì |
|---|---|---:|---|---|---|---|---|
| 01 BIOS Flashback | Nút nhấn cập nhật BIOS | 1 | (131; 10) | Tròn Ø4,0 | `#101216` | 0 lộ ngoài; công tắc 2 cực | Không cắm dây. Dùng cùng USB ở cổng 05 để cập nhật BIOS theo hướng dẫn của bo |
| 02 Clear CMOS | Nút nhấn chìm | 1 | (131; 25) | Tròn Ø4,0 | `#101216` | 0 lộ ngoài; công tắc 2 cực | Không cắm dây. Đưa cài đặt BIOS về mặc định; thao tác khi máy đã tắt và ngắt nguồn |
| 03 HDMI | HDMI Type A | 1 | (115; 10) | 14,0 × 4,6; hình thang | `#101216` | 19 | Dây HDMI tới màn hình khi CPU có đồ họa tích hợp |
| 04 DisplayPort | DisplayPort cỡ đầy đủ | 1 | (115; 25) | 16,2 × 4,8; vát một góc | `#101216` | 20 | Dây DisplayPort tới màn hình khi CPU có đồ họa tích hợp |
| 05 USB-A 2.0 | USB-A 2.0, cổng BIOS | 1 | (94; 10) | 13,0 × 5,0 | `#101216` đen | 4 | Chuột, bàn phím hoặc USB chứa tệp BIOS khi dùng nút 01 |
| 06 USB-A 2.0 | USB-A 2.0 | 1 | (94; 25) | 13,0 × 5,0 | `#101216` đen | 4 | Chuột, bàn phím, đầu thu không dây |
| 07 USB-A 5 Gbps | USB 3.2 Gen 1 Type A | 1 | (74; 10) | 13,0 × 5,0 | `#1F6FD1` xanh | 9 = 4 + 5 | USB lưu trữ, ổ cứng ngoài, webcam |
| 08 USB-A 5 Gbps | USB 3.2 Gen 1 Type A | 1 | (74; 25) | 13,0 × 5,0 | `#1F6FD1` xanh | 9 = 4 + 5 | USB lưu trữ hoặc thiết bị USB tốc độ cao |
| 09 USB-A 10 Gbps | USB 3.2 Gen 2 Type A | 1 | (54; 10) | 13,0 × 5,0 | `#C83C45` đỏ | 9 = 4 + 5 | SSD ngoài có cáp USB-A phù hợp |
| 10 USB-A 10 Gbps | USB 3.2 Gen 2 Type A | 1 | (54; 25) | 13,0 × 5,0 | `#C83C45` đỏ | 9 = 4 + 5 | SSD ngoài hoặc thiết bị USB 10 Gbps |
| 11 LAN 2.5G | Ethernet 2,5 Gbps, RJ45/8P8C | 1 | (32; 12) | Bao miệng 16,0 × 14,0; có rãnh ngàm | `#101216`; LED xem dưới | 8 | Dây mạng từ router hoặc switch |
| 12 USB-C 10 Gbps | USB 3.2 Gen 2 Type C, dữ liệu | 1 | (32; 29) | 9,0 × 3,5; bo hai đầu | `#101216` | 24 = 12 + 12 | SSD ngoài hoặc cáp dữ liệu điện thoại có đầu USB-C |
| 13 Wi-Fi 1 | RP-SMA jack, họ SMA, 50 Ω | 1 | (12; 10) | Ren ngoài Ø6,35; một chân giữa | `#C6A565` vàng; lõi `#E9E4D2` | 1 chân giữa + 1 vỏ mass | Vặn đầu dây anten Wi-Fi RP-SMA vào ren |
| 14 Wi-Fi 2 | RP-SMA jack, họ SMA, 50 Ω | 1 | (12; 25) | Ren ngoài Ø6,35; một chân giữa | `#C6A565` vàng; lõi `#E9E4D2` | 1 chân giữa + 1 vỏ mass | Vặn đầu dây anten Wi-Fi thứ hai |
| 15 Line-out | Jack 3,5 mm TRS, ngõ ra stereo | 1 | (−6; 7) | Ø3,5; vòng ngoài Ø6,0 | `#7EBE72` xanh lá | 3: tip, ring, sleeve | Loa có nguồn riêng hoặc ngõ vào âm thanh của ampli |
| 16 Mic-in | Jack 3,5 mm TRS, ngõ vào mic | 1 | (−6; 19) | Ø3,5; vòng ngoài Ø6,0 | `#E787A0` hồng | 3: tip, ring, sleeve | Micro có đầu 3,5 mm; headset một đầu TRRS cần dây chia mic/tai nghe phù hợp |
| 17 Optical S/PDIF | Ngõ ra quang TOSLINK | 1 | (−6; 30) | Bao miệng 9,0 × 9,0; có cửa che bụi | `#101216` | 0 tiếp điểm điện ở miệng; 1 đường quang | Dây quang tới DAC, ampli hoặc loa có ngõ vào quang |

Tổng: **6 USB-A + 1 USB-C + 2 cổng hình + 1 mạng + 2 anten + 2 jack âm thanh + 1 quang = 15 chỗ cắm**, thêm 2 nút. USB-C ở đây chỉ được chỉ định truyền dữ liệu; không gắn biểu tượng Thunderbolt, xuất hình hay công suất sạc chưa được thiết kế. Màu USB là quy ước của bo này, không phải quy tắc bắt buộc cho mọi bo.

## Chi tiết để dựng

Các lỗ trên lá chắn đồng tâm với bảng. Chọn lỗ nút Ø4,6; HDMI 15,0 × 5,6; DP 17,2 × 5,8; mỗi USB-A 14,6 × 6,8; RJ45 17,0 × 15,0; USB-C 10,0 × 4,5; anten Ø6,5; mỗi jack âm thanh Ø6,2; quang 10,0 × 10,0. Giữ biên vát của HDMI/DP và rãnh ngàm RJ45. Bề ngang cuối cùng của lỗ quang kết thúc tại h = 35, còn 1 mm tới mép lá chắn.

- Lá chắn thép bạc `#BCC2C8`, metallic 0,8, roughness 0,35; viền cổng `#C9CED4`, metallic 0,9, roughness 0,3. Lòng cổng tối `#080A0D`, lưỡi nhựa roughness 0,55; tiếp điểm `#C6A565`. Chữ trên bạc dùng `#101216`. Không thêm logo hoặc chữ trang trí.
- USB-A có hốc sâu ít nhất 4 mm và lưỡi nhựa lệch về phía +h. USB 2.0 có 4 tiếp điểm; loại 5/10 Gbps thêm hàng 5 tiếp điểm nằm sâu hơn. Không vẽ 9 vạch thành một hàng. USB-C có lưỡi giữa với 12 tiếp điểm mỗi mặt.
- HDMI có 19 tiếp điểm chia 10/9; DP có 20 chia 10/10. Dựng hình miệng khác nhau để người mới không nhầm hai loại.
- RJ45 có 8 lá tiếp xúc và rãnh ngàm ở phía +Y. Hai ô LED mỗi ô 1,8 × 1,8: **amber `#F2B544` tại (39; 7)**, **xanh lá `#63C64D` tại (39; 17)**. Chọn amber làm báo tốc độ, xanh báo kết nối/nhấp nháy khi truyền dữ liệu trong mô phỏng. LED là chi tiết của cổng 11, không thêm số cổng. Vỏ bao tối đa 19 × 18 quanh tâm (32; 12).
- Hai anten dùng **RP-SMA**, không trộn với SMA thường: ren ngoài, chân giữa nhô, vòng cách điện, đai ốc lục giác rộng 8 mm giữa hai mặt đối diện. Không lắp anten trong ảnh để người mới thấy rõ ren.
- Nút 02 nằm sâu 0,8 mm so với viền chống bấm nhầm. Số chân hàn của công tắc phụ thuộc linh kiện, không phải tiếp điểm để người dùng cắm dây. Cổng 05 in thêm `BIOS`; các chữ khác chỉ dùng `CLR CMOS`, `HDMI`, `DP`, `USB 2.0`, `5G`, `10G`, `2.5G`, `WIFI`, `OUT`, `MIC`, `OPTICAL` khi còn chỗ.
- Phần thân phía sau phải chừa vùng trống bán kính 4,5 mm quanh H1 (−112; 142,5) và H4 (−112; 0). Ở đoạn Y = −18…18, giữ thân cổng phía trong không vượt X = −118; dây anten có thể đi bằng cáp nhỏ. Không lấp H4 bằng một khối nắp kín.

## Nguồn và cách đọc ảnh

Tọa độ lấy theo khối `I/O sau`, `IO_DY` và `io_cut` trong [build_gaming.py](../../tools/build_gaming.py), cùng mốc PCB và tâm bo trong [gaming.ts](../../src/data/gaming.ts). Bố trí mới là lựa chọn thiết kế cho bo chung, không sao chép bo có thương hiệu trong ảnh người dùng.

Đối chiếu số tiếp điểm với tài liệu linh kiện: [USB-A 9 tiếp điểm](https://www.molex.com/en-us/products/series-chart/48404), [USB-C 24 tiếp điểm](https://www.molex.com/en-us/products/part-detail/2216100004), [HDMI Type A 19 tiếp điểm](https://www.te.com/en/product-1759548-1.html), [DisplayPort 20 tiếp điểm](https://www.te.com/en/product-2498798-1.html). Đầu anten chọn kiểu [RP-SMA bulkhead jack](https://www.amphenolrf.com/en-us/part/901-10803/9725/). Kích thước miệng và lỗ khoét trong bảng là số đo thiết kế mô phỏng, không được trích từ bản vẽ sản xuất của các linh kiện trên.

Ảnh là tờ tham chiếu hình dáng và cách nhận biết. Dùng bảng tọa độ, kích thước và số tiếp điểm làm dữ liệu khi dựng; không đo kích thước từ pixel hoặc suy ra pinout từ nét kim loại nhỏ trong ảnh.


<details>
<summary>Lệnh tạo ảnh để tái sử dụng (tiếng Anh)</summary>

Dùng công cụ imagegen tích hợp: một lượt tạo, hai lượt sửa chú thích. Hai ảnh tham chiếu là `02_motherboard.png` (phong cách) và `ref/buildcores_rear_io.png` (mức độ đầy đủ của cổng). Bảng tiếng Việt phía trên là dữ liệu dựng chính.

```text
Use case: infographic-diagram. Create ONE polished landscape product reference sheet, matching the visual style of the supplied 02 motherboard sheet: white background, dark navy title, realistic brushed silver metal and dark plastic, crisp thin blue technical leaders and numbered blue boxes, generous whitespace. The other supplied image is the user's realism reference only. Do not reproduce its website or brands.

Title "11 / REAR I/O". Subtitle "Generic ATX • Tower orientation • mm". Main view is a LARGE TRUE STRAIGHT-ON rear elevation of a tall narrow motherboard I/O shield, seen from behind a tower, occupying middle-left. Small matching THREE-QUARTER view upper-right, showing shallow connector housings behind the shield and a tiny black PCB edge, same ports, same order, NO full motherboard and no case. Below right put a concise technical inset with text specified below. No logos, brand/product names, watermark or decorative pseudo text.

GEOMETRY: Actual shield is 158 mm VERTICAL by 36 mm HORIZONTAL, silver #BCC2C8, 0.8 mm thick. It spans board-local Y=-18 bottom to Y=140 top. Height above PCB h runs horizontally LEFT TO RIGHT, from h=0 to h=36. View from -X toward +X, +Y up, +Z (h) right. Every USB-A port is a TALL rectangle: its LONG side follows vertical Y; NEVER landscape USB-A. Likewise HDMI, DisplayPort and USB-C long axes vertical. Panel is slim with a roughly 4.39:1 aspect ratio. Keep dimensions physically proportional. Top/bottom margins and row spacings must follow the coordinate list. Labels/callouts OUTSIDE the metal. Front face only tiny actual silk legends BIOS, CLR CMOS, HDMI, DP, USB 2.0, 5G, 10G, 2.5G, WIFI, OUT, MIC, OPTICAL where room allows. Numbers are annotation boxes OUTSIDE shield, not printed on it.

EXACT PORT LIST, numbered 01-17. Coordinates are (Y, h), and openings use vertical Y size × horizontal h size:
Top row Y131: 01 BIOS Flashback pushbutton at (131,10), dark round face Ø4.0. 02 Clear CMOS recessed pushbutton at (131,25), dark round face Ø4.0.
Y115: 03 HDMI at (115,10), opening 14.0×4.6, upright HDMI trapezoid, dark tongue. 04 DisplayPort at (115,25), opening 16.2×4.8, upright rectangular mouth with one clipped corner, dark tongue.
Y94: 05 USB-A 2.0 at (94,10) and 06 USB-A 2.0 at (94,25), each opening 13×5, BLACK tongues #101216, metallic rims. Port 05 has tiny BIOS marker identifying flashback USB.
Y74: 07 USB-A 5G at (74,10), 08 USB-A 5G at (74,25), each opening13×5, BLUE tongues #1F6FD1.
Y54: 09 USB-A 10G at (54,10), 10 USB-A 10G at (54,25), each opening13×5, RED tongues #C83C45.
Y32: 11 LAN 2.5G at (32,12), RJ45 opening16×14, dark deep keyed cavity with latch notch and eight gold contact strips; one green LED and one amber LED at its two upper corners, metal rim. 12 USB-C 10G at (32,29), opening9×3.5 upright oval capsule, centered thin dark tongue and fine contacts.
Y12: 13 Wi-Fi 1 at (12,10), 14 Wi-Fi 2 at (12,25): two gold threaded RP-SMA bulkhead jacks, metal outer diameter6.35, hex nuts across flats8, pale dielectric and ONE center pin in each. No antennas attached.
Bottom row Y=-6: 15 Line-out at (-6,7), circular Ø3.5 opening, GREEN #7EBE72 outer ring diameter6.0. 16 Mic-in at (-6,19), circular Ø3.5 opening, PINK #E787A0 outer ring diameter6.0. 17 Optical S/PDIF at (-6,30), 9×9 square black optical port with hinged dust shutter and keyed edges; no electrical gold contacts.

TOTAL exactly six USB-A (two black, two blue, two red), one USB-C, one HDMI, one DP, one RJ45, two antenna jacks, two audio jacks, one optical, two buttons. Do not add sockets in either view.

ANNOTATIONS: Every individual item needs its unique number and short English label, one leader terminating at that exact port. Arrange left-column callouts 01,03,05,07,09,11,13,15; right-column callouts 02,04,06,08,10,12,14,16; label17 below/right with a leader reaching the optical port. Stagger 15/16/17 labels enough for clarity. Keep ALL leaders and labels legible, with no crossings over ports. Dimension left side 158; bottom 36. Annotate top Y=140, bottom Y=-18, +Y upward and +h right. Tiny caption "Rear elevation • looking along +X".

Technical inset below the 3/4: "FIT & ORIENTATION" / "ATX aperture: 158.75 × 44.45" / "Model shield: 158 × 36" / "Y: -18 … 140" / "PCB top: Z = 1.6" / "Port Z = 1.6 + h" / "USB-A long axis: vertical". A restrained blue/black/red swatch row "USB 2.0 / 5 Gbps / 10 Gbps", colors correctly black / blue / red. Small final text "15 connections + 2 buttons". Main elevation much bigger than secondary view. Render convincing cavities, thin metal connector rims and actual plastic tongues; no flat black rectangles pretending to be ports. Correct exact row positions and proportions take precedence over ornamental detail.
```

Lượt sửa thứ nhất:

```text
Edit this sheet with only these precise corrections; preserve all layout, geometry, colors, typography, numbering, dimension text and all other labels. Bottom of main elevation: label 16 Mic-in (-6, 19) currently incorrectly points to the black optical port. Reroute its leader with a clean elbow through whitespace just ABOVE the bottom socket row, ending with a blue small hollow endpoint on the PINK circular mic jack (the middle circular jack). Give label 17 Optical S/PDIF (-6, 30) its own clearly visible separate blue leader to the RIGHT EDGE of the BLACK SQUARE optical port. Keep label 15 pointing to the GREEN circular jack. No crossed leaders, do not obscure ports. In BOTH views ensure RJ45 shows exactly EIGHT gold spring contact strips inside (not seven), and the LED on the viewer-left is AMBER #F2B544, viewer-right GREEN #63C64D. Both gold Wi-Fi RP-SMA jacks must have tiny protruding center PINS, not center holes, in both views. No other changes, no extra ports.
```

Lượt sửa cuối:

```text
Precise annotation repair on this 1536×1024 image. Change ONLY blue leader lines near labels 16 and 17 at bottom of the large panel; all ports, other text, metal, dimensions, colors and views must remain identical. Label 16 currently STILL connects diagonally down-left to the optical port at pixel(630,876), while a disconnected line reaches the pink microphone at (537,865). Remove the entire incorrect line segment from label16 (724,848) to the optical endpoint(630,876). Remove the disconnected mic elbow segment as well. Replace them with ONE continuous blue polyline for label16, following these exact image pixels: START (724,848), then (675,848), then (675,823), then (537,823), END (537,865). Put one blue/white hollow endpoint circle at (537,865), on the upper pink mic ring. This single unbroken line goes above the optical socket and reaches only the pink jack. Label17 retains its existing separate leader from (724,911) via (690,911) to (630,876), with one endpoint on the black optical socket's right edge. No shared endpoint between 16 and17. Do not add any text. Keep entire image framing unchanged.
```

</details>
