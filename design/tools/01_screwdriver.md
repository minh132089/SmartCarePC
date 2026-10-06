# 01 — Tua vít Phillips đổi đầu

Ảnh: [01_screwdriver.png](01_screwdriver.png). Đơn vị **mm**. Số dưới đây quyết định hình học mô hình; không đo ngược từ ảnh AI. Đối chiếu `00_assembly_needs.md`, `../families/00_families_spec.md` và `../gaming/00_blueprint.md`.

## Kích thước và tọa độ

Gốc O tại tâm mặt đuôi cán. **+X chạy dọc từ đuôi cán tới mũi vít**, +Y lên trên, +Z hướng ra người xem FRONT. FRONT/BACK nhìn từ ±Z, TOP/BOTTOM từ ±Y; LEFT nhìn từ −X, RIGHT từ +X. Tâm các phần lắp đồng trục có Y = Z = 0.

Bao khi lắp một bit: **200 × 32 × 32** theo X/Y/Z. Có **1 thân tua vít và 2 bit tháo rời**, mặc định gắn PH2, PH1 để riêng. “Đảo đầu” ở đây là mỗi bit có hai mũi cùng cỡ: **PH2/PH2 và PH1/PH1**. Hình chiếu và chi tiết phóng to không làm tăng số lượng.

| Số / phần | Số lượng | Vị trí và hình học |
|---|---:|---|
| 1. Cán hai màu | 1 cụm | X = 0…100; Ø32 tại X = 20…55, thu về Ø18 tại X = 90…100; đuôi kín bo R6, cổ bo R2. Lõi nhựa bọc cao su dày 2. |
| Dải cao su xanh | 4 dải + 1 vòng | Dải X = 15…65, rộng 7 theo chu vi, nằm tại các hướng ±Y và ±Z; bo đầu R3,5. Vòng cổ X = 94…98. Cùng bề mặt ngoài với cao su đen. |
| 2. Trục thép | 1 | X = 90…146, Ø6; phần nhìn thấy X = 100…142 dài 42. Đoạn đầu cắm cố định vào cán; đầu kia gắn vào đáy ổ giữ. |
| 3. Ổ giữ lục giác | 1 | X = 142…177, dài 35, Ø10, mép miệng bo R0,5. Hốc mở về +X tại X = 177, lục giác rộng qua hai mặt đối diện 6,45; sâu 27 tới X = 150. |
| Nam châm trong ổ | 1 | X = 148…150, Ø6, mặt hút hướng +X; nằm sau mũi bit ẩn. Không đặt nam châm xuyên thân bit như hình cắt minh họa. |
| 4. Bit PH2/PH2 | 1 | Dài 50; khi lắp X = 150…200. Thân lục giác X = 158…192, rộng qua hai mặt 6,35; hai đoạn mũi dài 8 tại hai đầu. |
| 5. Bit PH1/PH1 | 1 | Cùng chiều dài và thân lục giác với PH2, hai mũi nhỏ hơn. Khi thay vào ổ có cùng tọa độ X = 150…200. Khi trưng bày rời: tâm (175; 0; 30), trục dài +X. |

Mỗi mũi có bốn rãnh tạo chữ thập, đối xứng quanh X; không dùng mũi dẹt. Để dựng mô hình, chọn bao phần làm việc PH2 Ø4,5, PH1 Ø3, thu côn tới đầu; đây là kích thước tạo hình, không phải dung sai chế tạo chuẩn Phillips. Bit cắm sâu 27, lộ 23: **100 + 42 + 35 + 23 = 200**. Đảo bit bằng cách rút ra, quay 180° quanh Y qua tâm rồi cắm lại. Không có bánh cóc, nắp kho chứa hoặc kẹp áo.

## Vật liệu và màu

- Lõi cán nylon `#30343A`; lớp cao su TPE đen `#202329` và xanh `#5796C5`, nhám, roughness 0,8.
- Trục và ổ giữ thép satin `#AEB4BC`, metallic 1, roughness 0,3.
- Hai bit thép dụng cụ `#858B93`, metallic 1, roughness 0,4; nam châm `#555B62`. Không logo hoặc chữ khắc.

## Người mới cần biết

1. PH2 là đầu chính cho bước bắt bo bằng vít M3 × 5, bắt nguồn/lá PCIe bằng #6-32 × 6, và vít quạt trong bộ mô phỏng. Kiểm tra mũi ăn khít trước khi xoay.
2. Chuyển sang PH1 khi giữ SSD M.2 2280 bằng vít M2 × 2,5. Đỡ SSD trên chân đỡ cao 3, siết vừa giữ, tránh ép cong bo SSD.
3. Giữ trục thẳng theo trục vít, ấn nhẹ rồi xoay bằng tay; dừng khi mũi trượt. Không dùng tua vít làm đòn bẩy trên bo.
4. Nam châm giữ bit và hỗ trợ nhặt vít. Rút điện trước khi thao tác; cán cao su này không phải dụng cụ cách điện để sửa mạch đang có điện.
5. Cọc đỡ bo M3 cao 6,5 dùng đầu tuýp 5 mm ở sheet `04_screw_kit`, không dùng mũi Phillips để vặn mặt lục giác của cọc.

## Đối chiếu ảnh

Ảnh có đủ FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM, một hình 3/4 và ô tay dùng PH1 trên SSD. Đã tạo bằng imagegen tích hợp và sửa một lần theo brief.

Sai lệch còn lại: hình RIGHT phóng quá lớn ổ giữ và chữ thập so với cán; tỷ lệ các hình chiếu chưa tuyệt đối đồng nhất. Hai bit và chi tiết mũi vẫn thể hiện PH1 gần bằng PH2; khi dựng phải làm PH1 nhỏ theo số ở trên. Leader “Magnet” trong hình cắt chạm vùng mũi ẩn, không chỉ đúng đĩa nam châm phía sau. Chỉ dẫn kích thước 6,35 trên hình bit là bề rộng thân lục giác, không phải đường kính mũi. Các chỗ này lấy bảng tọa độ làm chuẩn.

Prompt tóm tắt: “White product reference sheet; six aligned orthographic views, one 3/4 view, hand using PH1 on M.2; 200 mm screwdriver, black/blue rubber grip, magnetic hex holder, two reversible PH2/PH2 and PH1/PH1 bits; thin blue dimensions, numbered English labels, no branding.” Lần sửa yêu cầu bỏ chữ giả trên kim loại, sửa mặt cắt bit hai đầu, thu nhỏ hình chiếu đầu cán, phân biệt PH1/PH2 và thêm chú giải 1–5.
