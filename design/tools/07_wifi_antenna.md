# 07 — Cặp ăng-ten Wi-Fi

Ảnh: [07_wifi_antenna.png](07_wifi_antenna.png). Bộ có **2 ăng-ten giống nhau**, mỗi chiếc dài **110 mm khi duỗi thẳng**, dùng ở bước 8 của bộ dụng cụ. Số dưới đây là dữ liệu dựng mô hình, không phải bản vẽ chế tạo hay thiết kế điện cao tần.

## Kích thước và tọa độ

Đơn vị mm. Mỗi chiếc có gốc O tại tâm miệng đai ốc; **+X từ đầu nối tới chóp**, +Y là hướng bẻ lên, +Z theo trục chốt bản lề. Khi thẳng, bao X = 0…110, Y/Z tối đa ±6. Hai chiếc dùng cùng một mesh, không lật gương.

| Chi tiết | SL mỗi chiếc / cả bộ | Vị trí và kích thước khi thẳng |
|---|---:|---|
| 01 Vỏ que dipole | 1 / 2 | X = 26…110, dài 84; tròn Ø9 ở gốc, thu dần tới Ø7 gần đầu; chóp kín bo tròn nằm trong giới hạn X = 110 |
| 02 Cổ xoay | 1 / 2 | X = 8…14, Ø8, tâm (11; 0; 0); xoay định hướng quanh X |
| 02 Bản lề | 1 / 2 | X = 14…26, bao 12 × 12 × 12; tâm quay (20; 0; 0), trục Z; chốt Ø3 dài 10, hai nắp Ø5 dày 0,5 tại Z = ±5,25 |
| 03 Đai ốc giữ liền RP-SMA | 1 / 2 | X = 0…8, dài 8, lục giác rộng 8 giữa hai mặt đối diện, đường kính qua đỉnh khoảng 9,24; xoay độc lập quanh X |
| 04 Lõi cách điện | 1 / 2 | Tâm (3; 0; 0), Ø4,4, dài 2 theo X; nằm trong đai ốc |
| 04 Tiếp điểm giữa dạng lỗ | 1 / 2 | Đồng tâm X; miệng tại (2; 0; 0), lỗ mô phỏng Ø1, sâu 3; không có chân kim nhô ra |

Đầu ăng-ten có **ren trong**, ghép ren ngoài Ø6,35 của cổng bo. Dùng quy ước SMA 1/4-36 UNS (bước xấp xỉ 0,706) cho hình học ren; đoạn ăn ren mô phỏng X = 0…4. Mỗi đầu có một tiếp điểm giữa và vỏ mass. Không thay bằng SMA có chân giữa ở đầu ăng-ten.

Bản lề bẻ **0…90° quanh +Z** tại (20; 0; 0), các tư thế minh họa 0°, 45°, 90°. Chỉ phần chuyển động và que phía sau bản lề quay; đai ốc và cổ giữ nguyên. Khi bẻ 90°, chóp ở (20; 90; 0). **Mỗi chiếc chỉ có một bản lề**; không dựng thêm đoạn que ngang hoặc khuỷu thứ hai. Cổ cho phép chọn phương bẻ quanh X trong mô phỏng, không diễn giải thành khả năng xoay liên tục vô hạn của dây bên trong.

## Khớp với bo Gaming

Theo [11_rear_io.md](../gaming/11_rear_io.md) và cụm RP-SMA hiện tại trong [build_gaming.py](../../tools/build_gaming.py): cổng 13/14 có ren ngoài, **chân giữa nhô**, cùng Y bo = 12, Z bo = 11,6 và 26,6. Khoảng tâm **15**; mặt lá chắn X bo = −136,2, đầu cổng nhô ra tới khoảng −140,2. Bản I/O này cập nhật cụm cổng thưa trong blueprint cũ.

Mốc đặt miệng ăng-ten khi đã vặn, chọn cho mô hình: A1 = (−137,4; 12; 11,6), A2 = (−137,4; 12; 26,6), tọa độ cục bộ bo. Ánh xạ từ trục ăng-ten sang trục bo: **Pbo = Ai + (−x; y; −z)**. Sau khi bẻ 90° tại bản lề, hai que hướng lên +Y bo, tâm cách nhau 15; vỏ que Ø9 còn khe 6. Tâm bản lề ở X bo = −157,4, đủ lùi ra ngoài để dựng que lên. Mốc đặt là lựa chọn lắp mô phỏng, không đo từ ảnh.

## Vật liệu

- Vỏ que: nhựa ABS bề mặt nhám `#202329`, roughness 0,7; cổ và bản lề nylon đen `#101216`, roughness 0,6.
- Đai ốc và tiếp điểm: đồng thau màu `#C6A565`, metallic 1, roughness 0,3. Chốt thép `#AEB4BC`; lõi cách điện `#E9E4D2`.
- Nét kích thước `#2164AC`, nền `#FAFBFC`. Không logo. Phần dẫn sóng nằm kín trong vỏ; không suy ra chiều dài điện hoặc độ lợi từ chiều dài vỏ 110.

## Người mới cần biết

1. Lắp cả hai ăng-ten vào hai cổng Wi-Fi sau bo ở bước 8, sau khi hoàn tất phần trong máy và trước bước nối điện.
2. Đưa đầu nối thẳng trục. Cầm đai ốc, vặn nhẹ bằng ngón tay tới khi vừa chặt; nếu ren kẹt ngay, tháo ra và căn lại.
3. Không dùng kìm siết, không dùng que dài làm tay đòn để vặn. Không chạm hoặc bẻ chân giữa trên cổng bo.
4. Sau khi siết, chỉnh tại khớp để hai que hướng lên; không bẻ cong vỏ que. Khi tháo, nới đai ốc trước.
5. Đúng cỡ ren chưa đủ: đầu ăng-ten của bộ này phải có **lỗ giữa**, khớp **chân giữa** trên bo.

## Kiểm tra ảnh và sai khác

Đã tạo bằng imagegen tích hợp và sửa một lần theo brief. Ảnh có đủ FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM, góc 3/4 của cặp và ô bàn tay lắp trên I/O. Nhãn chi tiết 01–04 đọc được.

**Không dùng ảnh để đo:** các hình chiếu chưa cùng tỷ lệ; BACK chưa đảo chiều chiếu đúng; LEFT và chi tiết phóng lớn vẽ lỗ giữa quá lớn, thiếu vòng cách điện rõ ràng. Que bẻ ở ô 3/4 vẫn có đoạn ngang và khớp thừa. Đúng là một khớp tại X = 20, rồi một que liền tới chóp theo bảng. Bàn tay và mũi tên Finger-tight còn chạm thân đen thay vì thể hiện rõ thao tác trên đai ốc vàng. Kích thước 15 áp dụng giữa tâm hai cổng, không lấy theo vị trí chân đường dóng trong ảnh. Dựng theo số liệu và hướng dẫn ở trên.
