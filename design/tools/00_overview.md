# 00 — Thảm và bộ đồ lắp máy

Ảnh: [00_overview.png](00_overview.png). Thảm **600 × 400 × 3 mm**, góc R8; tám nhóm đồ, số 1–8 trùng mã sheet. Đây là kích thước thiết kế cho mô phỏng, không phải số đo sản phẩm thương mại. Ren và chỗ ghép theo `00_assembly_needs.md`, `../families/00_families_spec.md` và `../gaming/00_blueprint.md`. **Số trong tài liệu này là nguồn chuẩn khi dựng.**

## Tọa độ và bố trí

Gốc O ở tâm mặt trên thảm; +X sang phải, +Z xuống dưới hình TOP, +Y nhô khỏi thảm. Thảm chiếm X = −300…300, Z = −200…200, Y = −3…0. Gốc mỗi nhóm ở tâm vùng bố trí trên mặt thảm; nâng từng vật theo chiều dày để không xuyên mặt thảm.

Trục dài của vật ở tư thế gốc chạy theo +X. Góc θ trong bảng quay trên mặt X/Z, chiều dương từ +X về +Z (thuận chiều kim đồng hồ trên TOP). Điểm cục bộ `(u,v)` đổi thành `X = X₀ + u cosθ − v sinθ`, `Z = Z₀ + u sinθ + v cosθ`; tương đương quay −θ quanh +Y. Mọi tọa độ và kích thước dưới đây dùng mm.

| Số / nhóm | X₀ | Z₀ | θ | Kích thước và phần con, tọa độ cục bộ (u;v) |
|---|---:|---:|---:|---|
| 1 / Screwdriver | −160 | −140 | 0° | Một tua vít dài 220, cán dài 90, Ø32 tại (−65;0); trục thép Ø6 và đầu PH2 hướng +X. Một đầu PH1 dài 25, chuôi lục giác 6,35 tại (35;27). |
| 2 / Wrist strap | 130 | −135 | 0° | Một bộ, bao bày 180 × 80. Vòng vải rộng 20 tại (−25;−10), nút bấm Ø10 tại (−55;−10); dây xoắn Ø10 nằm dọc v = 25; kẹp cá sấu dài 40 tại (65;20). Dây duỗi danh nghĩa 1800, có điện trở hạn dòng 1 MΩ trong cụm dây. |
| 3 / Thermal paste | −190 | −35 | 0° | Một ống keo dài 110, thân Ø15, tai piston rộng 25, tâm (0;−10); piston phía −X, vòi có nắp phía +X. Một que gạt 80 × 12 × 1,5 tại (0;20). |
| 4 / Screw kit | 30 | −20 | 0° | Một khay mở 180 × 110 × 18, thành/vách dày 2, tám ngăn theo bảng dưới. |
| 5 / Cable ties | −185 | 80 | 0° | Sáu dây rút nylon 100 × 2,5 × 1; đầu khóa phía −X, tâm u = −25, v = −22; −17; −12; −7; −2; 3. Hai dây gai 150 × 12 × 2 tại (0;15) và (0;31). Bao nhóm 150 × 60. |
| 6 / Power cord | 25 | 135 | 0° | Một dây dài 1800, vỏ Ø6,5; cuộn elip bao 150 × 80 tại (0;10), tổng bao bày 180 × 105. Đầu C13 bao 38 × 27 × 20 tại (−55;−35); phích hai chân tròn có tiếp địa bên tại (45;−35), thân bao 45 × 36 × 36, hai chân Ø4,8 dài 19, cách tâm 19. |
| 7 / Wi-Fi antennas | 225 | −20 | 90° | Hai ăng-ten dài 150, thân Ø10, tâm cục bộ (0;−12) và (0;12). Đế ren, khớp gập ở u = −75…−50; đầu tròn tại u = 75. Ren ngoài thấy trên ảnh chỉ là gợi hình; dựng đai ốc đầu nối tương thích cổng Wi-Fi bo Gaming, không suy chuẩn ren từ ảnh. |
| 8 / Flush cutter | 205 | 130 | 0° | Một kìm bao 130 × 55 × 15; hai cán bọc cao su về −X, chốt xoay tại (35;0), mỏ cắt tới u = 65. Mặt lưỡi cắt sát hướng xuống thảm, hai mỏ mở nhẹ. |

Đầu C13 phải có **ba ổ tiếp xúc**, ghép đầu C14 trên nguồn. Phích tường có hai chân tròn và tiếp điểm đất riêng; không dựng dây chỉ có hai lõi. Cáp và dây xoắn được uốn trong vùng bày, không kéo thẳng theo chiều dài danh nghĩa.

## Khay vít: ngăn và số lượng

Tâm ngăn theo hệ cục bộ khay. Chiều dài vít tính từ dưới đầu; số lượng là lượng chuẩn bị cho bộ mô phỏng, không phải toàn bộ đều lắp vào một máy.

| Tâm (u;v) | Đồ trong ngăn | Số lượng |
|---|---|---:|
| (−66;−26) | Vít bo M3 × 5, đầu chữ thập | 9 |
| (−22;−26) | Cọc lục giác 5 qua hai mặt, thân cao 6,5; ren trong/ngoài M3, chân ren dài 4 | 9 |
| (22;−26) | Vít M.2 M2 × 2,5, đầu thấp | 2 |
| (66;−26) | Vít nguồn/PCIe #6-32 × 6 | 8 |
| (−66;26) | Vít quạt tự ren Ø5 × 10 | 24 |
| (−22;26) | Vít két M3 × 6 và vòng đệm Ø7/Ø3,2 × 1 | 8 + 8 |
| (22;26) | Vít quạt két M3 × 30 và vòng đệm Ø7/Ø3,2 × 1 | 8 + 8 |
| (66;26) | Đầu tuýp 5 mm, dài 25, chuôi lục giác 6,35 | 1 |

Tổng: 59 vít, 9 cọc, 16 vòng đệm và 1 đầu tuýp. Không thêm vít giữ CPU, vít lò xo tản hoặc vít nắp đã nằm sẵn trên linh kiện.

## Vật liệu và màu

Thảm cao su nhám `#D6D8DA`, không mặc định coi là thảm ESD đã chứng nhận. Khay ABS `#30343A`; vỏ dây, nylon, thân ăng-ten và nhựa đen `#202329`; vải dây gai `#41454A`. Cán tua vít/kìm và vòng cổ tay xanh `#4C9BD6`; thép `#AEB4BC`, thép phủ đen `#25282D`, đồng thau `#C6A565`. Ống và que gạt PP `#E6E8EA`, keo xám `#9CA0A5`. Không logo; nhãn và đường kích thước xanh `#1673C9` chỉ thuộc bản vẽ.

## Người mới cần biết

1. Trước khi chạm linh kiện, tắt nguồn và rút điện ngoài; đeo vòng chống tĩnh điện, kẹp vào kim loại trần của khung theo hướng dẫn bộ dây. Vòng tay không thay thế việc ngắt điện.
2. Chọn đúng vít: M2 cho SSD, M3 cho bo, #6-32 cho nguồn/PCIe. Chỉ đặt cọc dưới lỗ bắt bo; siết vừa tay, không cố ép ren khác loại.
3. Keo dùng ở bước gắn tản CPU; kiểm tra lớp keo có sẵn và bóc màng bảo vệ đế tản trước khi lắp.
4. Gom dây sau khay rồi cắt phần dây rút thừa. Giữ mỏ kìm xa dây điện, không siết bó dây tới mức bẹp vỏ; chừa đường quay của quạt.
5. Vặn hai ăng-ten vào cổng Wi-Fi trước khi dùng mạng. Chỉ cắm dây nguồn ngoài sau khi kiểm tra đầu nối, vít rơi và đóng máy.

## Đối chiếu ảnh

Ảnh có đủ tám nhóm, tám ngăn, sáu dây rút, hai dây gai và hai ăng-ten; góc 3/4 lặp lại cùng bộ, không cộng số lượng. Không có PC. Đã tạo bằng công cụ imagegen tích hợp và chỉnh một lần theo brief.

Sai khác còn lại: đầu C13 trong ảnh vẫn có dấu lõm giống **lỗ thứ tư** ở giữa phía dưới dù đã yêu cầu sửa; khi dựng phải bỏ lỗ này, chỉ giữ ba ổ tiếp xúc. Tua vít, ống keo và dây rút bị phóng lớn so với thảm; tâm đồ chỉ gần đúng bảng. Vít/cọc trong ngăn là minh họa đại diện, không đủ số lượng bảng; vòng đệm và điện trở trong dây không thể đọc từ ảnh. Màu xanh trong ảnh đậm hơn mã màu. Không đo kích thước, bước ren hoặc số chi tiết nhỏ từ pixel.

Lệnh chỉnh ảnh cuối đã dùng (tiếng Anh cho công cụ):

```text
Correct this assembly-kit reference sheet, preserving the layout, all eight numbered labels, eight tray compartments, six nylon ties and two fabric straps, exactly two antennas, dimensions, colors and typography. Change the C13 cable connector in BOTH top and small perspective views: the front mating face must have EXACTLY THREE recessed rectangular contact sockets total, arranged one centered above and two below left/right. REMOVE the extra fourth lower-center opening currently visible. The molded black face has solid uninterrupted plastic below the three sockets. No fourth hole or blue marker that resembles one. Put the C13 blue leader on the OUTER EDGE of the connector housing. Keep the two-round-pin wall plug and its earth contact as shown. Keep all other objects and labels unchanged. This is one final white-background product reference sheet, no logos.
```
