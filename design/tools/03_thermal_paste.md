# 03 — Ống keo tản nhiệt và thẻ gạt

Đơn vị **mm**. Kích thước chọn cho mô hình PC SmartCare, không phải số đo sản phẩm thương mại. **Số trong tài liệu này là nguồn chuẩn khi dựng**, ảnh dùng tham khảo hình dáng.

## Kích thước và tọa độ

Một ống chứa khoảng **2 g**, dài **70** khi đậy nắp và pít-tông ở trạng thái minh họa; thân Ø10, bao toàn ống **70 × 18 × 13** theo X/Y/Z. Một thẻ gạt **40 × 25 × 1**.

Gốc O tại tâm mặt ngoài đầu nắp kín. **+X dọc ống, từ nắp về đĩa ấn; +Y hướng lên trong FRONT; +Z hướng ra người nhìn FRONT.** Các chi tiết ống đồng trục Y=Z=0. FRONT/BACK nhìn từ ±Z, LEFT/RIGHT từ −X/+X, TOP/BOTTOM từ ±Y.

| Số trên ảnh / chi tiết | SL | Vị trí và hình học | Vật liệu, màu |
|---|---:|---|---|
| 1 — Thân chứa | 1 | X=14…49; Ø ngoài 10, Ø trong 8; vai côn X=10…14 nối Ø3 với Ø10 | PP mờ xuyên sáng `#D6DADD` |
| Tai tì ngón, liền thân | 1 | X=47…49; bao Y=−9…9, Z=−1,5…1,5; bo đầu R1,5; hợp khối với thân tròn | PP `#D6DADD` |
| 2 — Nắp chụp | 1 | X=0…11; Ø ngoài 6; đáy kín dày 1; lòng Ø3,2 sâu 10 mở về +X; 12 gân dọc nổi 0,25 nằm trong bao Ø6 | PP nhám `#34383D` |
| 3 — Cụm pít-tông | 1 | Cần Ø3,5 tại X=44…67; đĩa ấn Ø13 tại X=67…70; đầu ép Ø7,9 tại X=42…44 | PP `#D9DCDE`; đầu ép đàn hồi `#8A8E92` |
| 4 — Thẻ gạt | 1 | Tâm đặt cạnh ống (35;−27;0); X=15…55, Y=−39,5…−14,5, Z=±0,5; bốn góc R2; cạnh ngắn X=15 vát trong đoạn X=15…17 xuống dày 0,3 | PP xanh nhạt `#91C5E5` |
| 5 — Vòi, liền thân | 1 | X=5…10; Ø ngoài 3; lỗ thông Ø1,5, miệng hướng −X; đầu tù bo R0,3; nắp chụp ma sát, không ren, không kim | PP `#D6DADD` |
| Keo trong ống | 1 lượng nạp | Trong lòng Ø8, khoảng X=14…42; thể tích minh họa khoảng 1,4 ml, khối lượng danh nghĩa ≈2 g tùy công thức | Keo tản nhiệt xám không dẫn điện, `#909398` |

Pít-tông dịch về −X khi bóp; hành trình mô phỏng tối đa 28. Nắp tháo theo −X. Không đếm vòi và tai tì thành món rời. Bộ dụng cụ gồm **1 ống hoàn chỉnh có nắp + 1 thẻ**, không kèm CPU.

## Chi tiết đang sử dụng

CPU dùng kích thước chung **37,5 × 37,5 × 4,5**, đã khóa trong socket. Dùng lại model CPU của dự án, không dựng CPU mới theo ảnh. Gốc C tại tâm mặt kim loại IHS; trục cục bộ u/v trên mặt, w hướng ra ngoài. Chấm keo ở (0;0;0), dạng vòm đáy **Ø4**, cao **2**, đỉnh w=2. Đây là kích thước minh họa “hạt đậu”, không phải liều dùng chung cho mọi CPU. Tay giữ tai tì và ấn pít-tông, miệng vòi vừa nhấc khỏi chấm keo.

## Người mới cần biết

- Keo lấp khe nhỏ giữa nắp CPU và đế tản để truyền nhiệt; không phải keo dán cố định.
- Dùng ở bước 4 trong danh sách đồ nghề, ngay trước đặt tản; tương ứng bước 15–16 trong blueprint Gaming. Máy chưa cắm điện; bóc màng bảo vệ đế tản trước khi lắp.
- Nếu đế tản đã có keo sẵn còn sạch, không bôi chồng thêm. Với đế chưa có keo, làm theo hướng dẫn của tản và loại keo đang dùng.
- Chấm ở giữa mặt kim loại; tránh dây keo vào socket, tiếp điểm hoặc linh kiện quanh CPU. Không đổ cả ống 2 g cho một lần lắp.
- Thẻ gạt chỉ dùng nếu cách bôi được hướng dẫn yêu cầu dàn mỏng. Không cần vừa chấm vừa gạt; đậy lại nắp sau khi dùng.

## Sai khác của ảnh

Ảnh có đủ sáu hướng, một góc 3/4 và ô thao tác; đã sửa một lần bằng imagegen tích hợp. Tỉ lệ giữa các hình chiếu vẫn chưa đồng nhất: thẻ FRONT/BACK nhỏ hơn tỉ lệ thân ống, cạnh thẻ LEFT/RIGHT quá dài và thiếu số 25. Tai tì trong TOP/BOTTOM vẫn nhô quá cao; số 3 ở đây đặt theo chiều X là sai, **độ dày X đúng là 2, bề rộng Z là 3**. Hướng BOTTOM và một số mép còn mang tính minh họa. Gân nắp, vát thẻ và chấm keo không dùng để đếm hay đo theo pixel. Kích thước CPU 37,5 chỉ toàn bộ package, không riêng nắp kim loại.
