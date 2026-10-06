# 05 / RAM — hai thanh DDR4

Ảnh: [05_ram.png](05_ram.png). Nguồn kích thước và vị trí: [00_blueprint.md](00_blueprint.md), mục 1–4 và 7; đã đối chiếu ba tệp dữ liệu trong `src/data/`. **1 đơn vị cảnh = 100 mm.**

**Lưu ý bàn giao:** ảnh đã sửa một lần nhưng chưa đạt phần bố trí: nhãn khe bị lặp A2, thiếu A1/B2; hai thanh vẫn lệch dọc, chưa nằm đúng trên hai khe xen kẽ. Dùng ảnh để tham khảo vật liệu, tiếp điểm và mặt cắt; dựng vị trí theo bảng dưới, không sao chép bố trí sai trong ảnh. Không suy số chân từ số vạch vàng vẽ được.

## Kích thước và các phần chính

Có đúng **2 thanh**, mỗi thanh **133,35 dài × 42 cao × 6 dày mm**, tính cả chân tiếp xúc và viền trên. Hai thanh giống nhau. Các số chi tiết bên dưới là lựa chọn dựng mô hình, không phải bản vẽ chế tạo DDR4.

Quy ước riêng của thanh: `u` dọc chiều dài, gốc ở giữa; `h` từ mép chân vàng lên trên; `t` theo bề dày, gốc ở giữa PCB.

| Phần | Kích thước / vị trí, mm | Vật liệu và màu |
|---|---|---|
| PCB | 133,35 × 31,25 × 1,2; h = 0…31,25; t = ±0,6 | Mạch nhám gần đen `#18201E` |
| Hai má tản nhiệt | Bao ngoài dài 133,35, h = 6…42; tổng bề dày cả cụm 6, t = ±3; cạnh vát nhỏ 0,4 | Nhôm anod đen `#16181D`, nhám vừa |
| Viền trên | Dải dài khoảng 130, cao 1; nằm sát h = 41…42, không vượt bao ngoài | Cyan `#22D3EE`; vật liệu màu, không cần đèn hay dây |
| Chân tiếp xúc | Hai mặt PCB, vùng h = 0…5; chừa khoảng rãnh khóa | Kim loại vàng `#C9A34A`, có khe tối giữa từng tiếp điểm |
| Rãnh khóa | Một rãnh mở từ mép dưới, tâm u = +4,25; rộng 2, sâu 3; bo góc 0,3 | Khoét xuyên PCB, không phủ vàng qua khoảng rỗng |
| Rãnh giữ hai đầu | Tại u = ±66,675, vùng h = 7…10; khoét vào mỗi đầu khoảng 1,5 | Chừa lộ PCB để móc chốt vào |
| Thân khe, thuộc bo | Mỗi khe bao ngoài khoảng 142 dài × 7,5 rộng × 7 cao; bốn khe song song | Nhựa đen `#101216` |
| Chốt hai đầu, thuộc bo | 2 chốt/khe, mỗi chốt khoảng 5 × 7 × 9; bản lề ở chân chốt | Nhựa xám `#AEB4BA`, không phải vít |
| Gờ khóa trong khe | Tâm dọc khớp u = +4,25; rộng 1,6, cao 2,5 tính từ đáy nhận chân | Nhựa cách điện cùng màu khe |

Rãnh lệch nhẹ khỏi giữa, không đặt ở sát đầu thanh. Dùng cùng một tham số vị trí cho rãnh RAM và gờ khe để chúng khớp nhau. Các kích thước rãnh trên là quy ước hình học cho mô phỏng, không dùng làm khuôn sản xuất.

## Đầu nối và vị trí lắp

**Mỗi thanh có một đầu nối cạnh DDR4: 288 tiếp điểm, 144 trên mỗi mặt.** Hai mặt dùng chung một rãnh khóa. Không có dây nguồn, dây RGB hay đầu cắm phụ.

Tọa độ dưới đây theo tâm bo, đơn vị mm. Chiều dài thanh chạy theo Y, bề dày theo X, chiều cao nhô theo +Z; lúc lắp ấn theo −Z.

| Khe | Tâm X; Y trên bo | Trạng thái |
|---|---|---|
| A1 | 70; 62 | Để trống |
| A2 | 79; 62 | Thanh thứ nhất; chỉ số 1 trong mã |
| B1 | 89; 62 | Để trống |
| B2 | 98; 62 | Thanh thứ hai; chỉ số 3 trong mã |

Cả bốn khe có hai đầu thẳng hàng. Không chia thành hai cụm nối đuôi nhau. Khi dựng thanh: `Y = 62 + u`, `X = X_khe + t`. Mặt bo ở Z = 3; chọn thân khe nằm Z = 3…10, mép dưới thanh đã cắm ở Z = 5,5, đỉnh thanh ở Z = 47,5. Đây là cao độ dựng bổ sung; không đổi X/Y gốc.

Tâm bo toàn máy là (−88; 62,5; −96), nên A2/B2 có X toàn máy lần lượt −9 và 10, Y tâm cùng 124,5. Hình tách lắp chỉ dịch thanh theo +Z; không dịch dọc Y hoặc sang khe khác.

Chốt mở quay ra ngoài khoảng 25–30° quanh bản lề ở chân. Khi thanh xuống hết, hai chốt đứng lên và móc vào rãnh hai đầu; chân vàng nằm trong khe. Dùng mặt cắt để thấy gờ nhựa đi vào rãnh, không xuyên PCB. Cụm khe và chốt thuộc phần motherboard, không nhân đôi khi ghép RAM vào cảnh.

## Vít và lỗ vít

**0 vít, 0 lỗ bắt vít, 0 chân đế cho RAM.** Giữ bằng hai chốt của mỗi khe sử dụng, tổng 4 chốt giữ hai thanh. Cả dải bốn khe có 8 chốt. Không dựng các đốm tròn trên ảnh thành vít hay điểm tương tác.

## Người mới cần nhận ra

1. Bộ này cắm hai thanh vào **A2 và B2**; A1 và B1 để trống.
2. Rãnh lệch tâm phải trùng gờ khe trước khi ấn. Sai hướng thì không ép xuống.
3. Mở cả hai chốt, giữ thanh thẳng và ấn đều hai đầu.
4. Chỉ báo lắp xong khi thanh xuống hết và cả hai chốt giữ đã đóng.
5. Cầm ở mép hoặc tản nhiệt; tránh chạm chân vàng. RAM không cần bắt vít hay nối dây.

## Ghi nhận tạo ảnh

Dùng công cụ **image_gen tích hợp**. Đã kiểm tra ảnh đầu, sửa một lần theo brief và lưu bản sau sửa. Phần bố trí khe còn sai như ghi ở đầu tài liệu; chưa coi ảnh là bản duyệt cuối.

Lệnh tạo:

```text
Use case: scientific-educational.
Create ONE landscape product reference sheet for a 3D modeller, title "05 / RAM", subtitle "DDR4 • 2 DIMMs • Dimensions in mm". White / very light grey background, soft studio lighting, realistic engineered materials, fine blue dimension lines, numbered blue callouts with short English labels, generous whitespace. Brand-free, no logos, fake sticker text or watermark.

MAIN HERO across upper two thirds: 3/4 isometric view of exactly TWO identical desktop DDR4 RAM sticks standing upright, parallel, hovering directly above sockets A2 and B2 of a cropped motherboard strip with exactly FOUR parallel DIMM sockets. Label the four sockets in order "A1", "A2", "B1", "B2" at their near ends; highlight A2 and B2 subtly in cyan, leave A1 and B1 empty. Position camera so all four slots and both RAM broad faces remain distinguishable; do not add extra sticks. Fine dashed insertion guides from each stick down to its corresponding A2 or B2 socket, straight insertion arrows normal to board. Sticks each 133.35 mm long, 42 mm tall, 6 mm thick. Dimension arrows correctly span length, height and thickness on one stick, exact text "133.35", "42", "6". Dark charcoal #16181d anodized metal heatspreaders with subtle angular bevels, thin cyan #22d3ee top-edge accent only, dark PCB. Clearly exposed finely segmented GOLD edge fingers on the bottom edge, with one off-centre DDR4 key notch slightly right of centre, not far near the end, no DDR5 geometry. End retention recesses exposed for latches. Every socket has a plastic latch at EACH end, all main-view latches pivoted outward open. Precisely aligned slot key under each RAM notch. No wires, screws, screw holes, RGB lighting or unrelated PC parts.

BOTTOM LEFT secondary view: framed enlarged cutaway titled "KEY MATCH". Show a partial face-on section of the RAM lower edge with individual gold fingers on both sides of the single notch, hovering just above a longitudinally cut-open socket. The solid insulating slot key protrudes upward directly aligned with the notch and fits inside it when inserted. Dashed vertical alignment centreline through notch and key. Small inset outline of the whole RAM within this same panel locates the notch slightly right of centre. Numbered leaders "1  Gold contacts", "2  Offset notch", "3  Slot key". Small text "288 contacts / DIMM". Gold contacts occur on BOTH PCB faces; 288 total, not 288 per side.

BOTTOM RIGHT secondary view: framed detail titled "LATCH STATES". Two side-by-side cropped views of the SAME socket end, labelled "OPEN" and "CLOSED". OPEN shows the latch pivoted outward and the RAM edge raised above socket. CLOSED shows RAM fully seated and upright latch hooked into the RAM side retention recess. A curved blue arrow shows inward latch movement. Numbered leader "4  Retaining latch". This panel shows only cropped RAM ends, not extra complete sticks. Short footer "A2 + B2" and "No screws". Keep labels crisp, sparse and readable. Mechanically coherent educational assembly reference, not advertising.
```

Lệnh sửa đã dùng:

```text
Correct this reference sheet. Preserve title, white background, realistic charcoal/cyan RAM materials, dimension values 133.35 / 42 / 6, gold fingers, readable labels and the two bottom detail panels.
CRITICAL: completely rebuild the main motherboard strip and RAM arrangement as ONE bank of EXACTLY FOUR DIMM sockets, each the SAME length, all four parallel and SIDE BY SIDE, with their left endpoints aligned on one line and their right endpoints aligned on one line. No split banks, no end-to-end sockets, no staggering. There must be exactly EIGHT end latches total in the main view, two per socket. Show four long sockets in depth, nearest-to-farthest ordered A1, A2, B1, B2. Each label is beside that socket's LEFT endpoint. Both floating RAM modules have exactly the same length and their left ends and right ends align with the slot endpoints below. Module 1 hovers over the SECOND socket A2; module 2 hovers over the FOURTH socket B2. They are parallel, front-to-back separated, NOT next to each other end-to-end. Use greater exploded height for the back B2 module so both full broad faces are visible; preserve insertion alignment in the plane of each slot. A1 and B1 are empty and clearly visible. Add dashed downward guides to the correct A2 and B2 sockets. Make the main view a compact, coherent four-slot bank like real motherboard DIMM sockets.
In bottom latch panel keep OPEN pivoted outward and show CLOSED fully upright, tooth engaged in side recess, fully seated gold fingers concealed inside socket. No metallic screw-like holes on RAM; end recesses are latch features. Keep exactly two full sticks in hero and only cropped details in bottom panels.
```

