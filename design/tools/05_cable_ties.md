# 05 — Dây rút và dây dán gom cáp

Đơn vị **mm**. Các số dưới đây là kích thước chọn cho mô hình PC SmartCare, không phải bản vẽ gia công. **Dựng theo số trong tài liệu, không đo theo ảnh.**

## Số lượng, kích thước và trục

Bộ mẫu gồm **10 dây rút nylon 100 × 2,5** (9 dây thẳng và 1 dây đang buộc) cùng **3 dây dán hai mặt 150 × 12**. Các hình chiếu là hình lặp của cùng vật, không cộng vào số lượng. Ba dây dán là phụ kiện rời; không thay số đai gắn sẵn của từng case.

Mỗi dây có hệ trục riêng: **+X dọc chiều dài, +Y theo chiều dày, +Z theo chiều rộng**. FRONT/BACK nhìn từ ±Z; LEFT/RIGHT từ −X/+X; TOP/BOTTOM từ ±Y. Dây thẳng nằm trong mặt XZ. Gốc dây rút ở giữa cạnh ngoài đầu khóa, Y=0 tại mặt giữa thân dây; gốc dây dán ở giữa cạnh ngắn đầu tiên, tại mặt giữa chiều dày.

| Chi tiết / số gọi trên ảnh | Vị trí và kích thước | Vật liệu, màu |
|---|---|---|
| 1 — Đầu khóa | X=0…5; Y=−0,5…3; Z=±2,25; bao 5 × 3,5 × 4,5 theo X/Y/Z, bo R0,4 | Nylon PA66 đen nhám `#202226` |
| Thân dây rút | X=5…100; Y=±0,5; Z=±1,25; **dài tổng 100 đã gồm đầu khóa** | PA66, cùng đầu khóa |
| Khe luồn qua đầu | Khe xuyên theo Y; X=1…3; Z=±1,4; miệng 2 × 2,8, bo R0,2 | Khoảng rỗng thật |
| 2 — Lẫy đàn hồi | Liền đầu khóa, chân tại (3;2;0), đầu lẫy tại (2,3;0,8;0); rộng Z=1,8, dày 0,35; chừa khoảng võng 0,4 phía sau | PA66 `#202226`, không phải kim loại |
| 3 — Răng một chiều | Mặt +Y, X=8…90; rộng Z=1,8; bước 0,8; rãnh sâu 0,15 nằm trong bao dày 1; mặt −Y trơn | Liền thân, không thêm lớp dày |
| 4 — Đuôi luồn | X=90…100; thu rộng 2,5 xuống 0,8; đầu bo R0,4, dày giảm 1 xuống 0,5 | PA66 |
| 5 — Mặt vòng mềm | Mỗi dây dán X=0…150, Z=±6, Y=0…0,6; góc R2 | Sợi nylon mềm `#292B2E` |
| 6 — Mặt móc | Cùng bao X/Z; Y=−0,6…0; tổng dày cả hai mặt **1,2** | Nylon đen `#17191C` |

Dây dán có bề rộng đều, không lỗ, không khóa nhựa; hai mặt móc/vòng tự bám khi quấn chồng. Dùng normal map cho sợi và răng nhỏ ở xa. Lẫy và khe đầu khóa cần hình học rõ ở chế độ phóng to.

## Tư thế đang buộc

Bó cáp minh họa có bao Ø12, dài 80, trục dọc Z; gốc C ở tâm bó, Z=−40…40. Vỏ cáp đen `#15171B`; đây là ngữ cảnh sử dụng, không thêm dây điện vào bộ dụng cụ. Vòng dây rút nằm trong XY, tâm C, bán kính đường giữa danh nghĩa 6,7; bề rộng theo Z là 2,5. Đặt tâm đầu khóa gần (0;8;0), đuôi đi ra theo tiếp tuyến +X để tay kéo; hiệu chỉnh đoạn chuyển tiếp vào khe, giữ chiều dài đường giữa theo dây gốc. Không dùng hình vòng này để suy ra đường kính bó tối đa.

Thao tác ở phía sau khay bo, bước 7 của danh sách dụng cụ, tương ứng bước 23 trong blueprint Gaming. Tư thế cuối theo khoang dây từng case; dàn dây thành lớp trong case mỏng.

## Người mới cần biết

- Gom dây sau khi cắm đủ đầu nối, trước khi đóng nắp. Máy chưa cắm điện ngoài.
- Luồn đuôi vào đúng chiều lẫy; chỉ kéo vừa giữ bó, không làm lõm vỏ cáp hoặc kéo căng đầu cắm.
- Dây rút nylon dùng một lần. Khi cắt đuôi bằng kìm cắt sát, tránh phạm vào cáp; không để đầu nhọn cọ dây.
- Dây dán tháo bằng cách bóc hai mặt, có thể dùng lại khi chỉnh đường dây.
- Giữ dây cách cánh quạt ít nhất 10; không kẹp giữa nắp và khung. Dây nguồn uốn R≥20, hoặc R≥12 với dây mềm của họ 04/05 theo đặc tả case.

## Sai khác của ảnh

Ảnh đã sửa một lần bằng **imagegen tích hợp**, có sáu nhãn hình chiếu, một góc 3/4, chi tiết lẫy và ô thao tác. Còn các sai khác cần bỏ qua khi dựng:

- Góc 3/4 chỉ thấy **7 đầu dây rút rời**, dù nhãn ghi 9; bộ đúng là **9 rời + 1 đang buộc**, cùng đúng 3 dây dán.
- Mũi tên 100 bắt đầu sau đầu khóa làm hình trông như dài 105; **100 là tổng chiều dài**, thân từ X=5 đến 100 dài 95.
- Tỉ lệ giữa các hình chiếu chưa đồng nhất; LEFT/RIGHT bị phóng to. RIGHT tách đuôi khỏi đầu khóa thay vì chiếu chồng đúng trục.
- Ô đầu khóa nhỏ đặt cả 5 và 4,5 trên cùng phương là sai: **5 dọc X, 4,5 dọc Z, 3,5 dọc Y**.
- Mũi tên luồn vẫn hai đầu; cơ cấu thực chỉ siết một chiều. Răng, lẫy và sợi vải phóng đại để dễ thấy, không dùng để đếm theo ảnh.

<details>
<summary>Lệnh sửa cuối gửi imagegen (tiếng Anh)</summary>

```text
Correct this cable-management reference sheet once, keeping its title, white background, blue dimensions, six-panel layout, ratchet cutaway, three fabric straps, and hand tightening one closed tie around cables.

Repair orthographic geometry and scale. In ALL SIX panels, use the SAME physical scale, 2.5 image pixels per mm at this 1536px canvas size: 100mm tie =250px long, 150mm strap=375px long; widths and thicknesses to same scale. These are separate projections of straight flat parts with length X, thickness Y, width Z:
FRONT (+Z) and BACK (-Z): show ONLY thin edge profiles. Zip band 100 long x 1 thick, raised head 5 long x 3.5 high. Fabric is 150 long x 1.2 thick; no broad fabric face. Remove the 2.5 and 12 width dimensions from these two edge views.
TOP (+Y) and BOTTOM (-Y): broad faces, tie 100 long and 2.5 wide with head 5 long x 4.5 wide; strap 150 long x 12 wide. Teeth only on TOP tie. Smooth BOTTOM tie.
LEFT (-X) and RIGHT (+X): TRUE END VIEWS with no long silhouettes! Each shows a tiny head silhouette 4.5 wide x 3.5 high, and separate fabric cross-section 12 wide x 1.2 high. RIGHT has small tip centered in front of distant head; no side-view strip. Do not enlarge these end views. Remove wrong 5 width label on LEFT, replace with 4.5. Give head dimensions 5 x 4.5 x 3.5 beside enlarged ratchet inset.
Maintain generous whitespace even when end views are very small.

In 3/4 KIT VIEW show EXACTLY NINE separate loose nylon ties, count NINE clearly visible ratchet heads in a staggered row, plus exactly THREE fabric straps. Keep '9 loose + 1 in use'. The hand inset uses the tenth tie. Preserve readable numbered callouts 1 Head, 2 Pawl, 3 Teeth, 4 Tapered tip, 5 Loop face, 6 Hook face. Pawl must be black nylon continuous with the head, not metal. Feed direction must be a SINGLE one-way arrow, not a double-headed arrow. No branding.
```

</details>

