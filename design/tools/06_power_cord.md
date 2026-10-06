# 06 — Dây nguồn AC 1,5 m

Đơn vị **mm**. Một dây đen, đầu cái **IEC C13 có 3 lỗ** → phích **CEE 7/7 có 2 chân tròn Ø4,8**, kèm tiếp điểm tiếp đất. Kích thước vỏ và dung sai dưới đây chọn cho mô hình PC SmartCare, không phải bản vẽ chế tạo. **Số trong tài liệu là nguồn chuẩn; không đo theo pixel của ảnh.**

## Kích thước, trục và chi tiết

Đoạn cáp mềm dài **1500 theo đường tâm**, không cộng hai đầu đúc và cổ chống gập; Ø ngoài **6,8**. Khi duỗi theo quy ước dưới đây, bao toàn cụm **1649 × 38 × 38** theo X/Y/Z. C13 gồm thân 40 × 21 × 28 và cổ dài 25; phích tường gồm thân dài 40, Ø38, cổ dài 25 và chân nhô 19.

Gốc **O=(0;0;0)** tại tâm mặt cắm C13. **+X dọc dây duỗi từ C13 tới phích tường; +Y lên phía lỗ tiếp đất của C13; +Z hướng người nhìn FRONT.** FRONT/BACK nhìn từ ±Z; LEFT/RIGHT từ −X/+X; TOP/BOTTOM từ ±Y. Các tọa độ sau là trạng thái duỗi, không phải tọa độ lấy từ cuộn dây trong ảnh.

| Số / chi tiết | SL | Vị trí và hình học | Vật liệu, màu |
|---|---:|---|---|
| 1 — Đầu C13 | 1 | Mặt cắm X=0, hướng −X; thân X=0…40. Vai cầm X=10…40, bao Y=±10,5; Z=±14, bo R2. Mũi cắm X=0…10, rộng Z=22,6; cao Y=17,6; hai góc trên vát 2,8 | PVC đúc đen nhám `#17191C` |
| Ba lỗ C13 | 3 | Tâm tại (0;5;0), (0;−4;−7), (0;−4;7). Mỗi lỗ cao Y=4,6 × rộng Z=1,4, sâu 12 về +X; tiếp điểm cái nằm chìm, không có chân nhô | Hốc `#050506`; kim loại chìm `#B8BEC5` |
| 2 — Cáp mềm | 1 | Đường tâm thẳng từ (65;0;0) tới (1565;0;0), dài 1500; tiết diện tròn Ø6,8. Khi cuộn, giữ nguyên chiều dài đường tâm | Vỏ PVC mềm `#15171B`; 3 lõi đồng bên trong, không phơi lõi |
| 3 — Cổ chống gập | 2 | X=40…65 và 1565…1590; dài 25 mỗi cổ, Ø12 phía thân thu về Ø6,8 phía dây. Mỗi cổ 5 rãnh vòng rộng 1, sâu 0,6, tâm cách vai thân 4/8/12/16/20 | PVC mềm `#202329` |
| 4 — Thân phích tường | 1 | X=1590…1630, trục X; bao Ø38, cạnh R2; mặt chân tại X=1630 | PVC đúc `#17191C` |
| Hai chân tròn | 2 | Trục +X, X=1630…1649; tâm Y=0, Z=±9,5; Ø4,8, dài nhô 19, bước tâm 19; đầu bo R0,8 | Đồng thau mạ nickel `#B8BEC5` |
| 5 — Tiếp đất phích | 1 cụm | Hai dải tiếp xúc bên thân tại Y=±18,5, Z=0, X=1608…1628; mỗi dải dài X=20 × rộng Z=8 × dày Y=0,5. Lỗ tiếp đất trên mặt chân: tâm (1630;10;0), Ø5,5, sâu 15 về −X. Đây là lỗ chìm, không phải chân thứ ba | Kim loại `#B8BEC5`, hốc `#050506` |
| 6 — C14 trên PSU | 1 có sẵn | Chỉ hiện trong ô thao tác, không thuộc bộ dây; 3 lá đực trong hốc có khóa | Nhựa `#101216`, lá `#B8BEC5` |

Tổng bộ dụng cụ: **1 dây hoàn chỉnh**, gồm 2 đầu đúc, 2 cổ, 2 chân tròn và 3 ổ tiếp xúc C13; không có vít tháo. Đường dẫn tiếp đất nối với tiếp điểm đất C13, không bỏ trống. Ảnh không phải sơ đồ đấu điện.

Tư thế trưng bày: cuộn khoảng **2,5 vòng**, bán kính đường tâm khoảng **75**, nằm trong mặt XY; các vòng lệch nhẹ theo Z để không xuyên nhau. Hai đuôi vươn về ±X. Bao bố cục mục tiêu **340 × 180 × 40**; chỉnh đuôi và khoảng cách vòng khi dựng spline để đường tâm vẫn dài 1500. Đây là bao tư thế mềm, không phải kích thước bắt buộc của sản phẩm.

## Ghép với nguồn trong dự án

Dùng PSU ATX sẵn có **140 X × 86 Y × 150 Z**. Theo `tools/build_gaming.py`, tâm miệng C14 trong hệ PSU là **(−70;−16;49)**; lòng rộng Z=23, cao Y=18, sâu X=12; ba lá có tâm lệch (Y;Z)=(5;0), (−4;−7), (−4;7). Mũi C13 22,6 × 17,6 chừa 0,2 mỗi phía; lỗ C13 khớp lá mô hình 4 × 0,8.

Khi lắp, đổi trục dây bằng **R(x;y;z)=(−x;y;−z)**. Đầu C13 tiến theo **+X của PSU**. Vị trí đã cắm: **P=(−60;−16;49)+R(p)**, mũi sâu 10, vai tại mặt X=−70. Ô thao tác cho tay cầm thân nhựa, mũi còn cách miệng ổ 8, công tắc ở O, phích tường chưa cắm. Với họ 04/05, dây ngoài cắm vào C14 trên vỏ; dây AC 450 bên trong case đã có sẵn, không thuộc sheet này.

## Người mới cần biết

- Dây đưa điện từ ổ tường vào PSU, không cắm vào bo mạch hoặc ổ modular bên trong máy.
- Dùng ở **bước 9 của danh sách đồ nghề**, tương ứng bước 25 trong blueprint Gaming: lắp xong, kiểm tra dây/quạt và đóng nắp rồi mới nối điện ngoài.
- Để công tắc PSU ở **O**, giữ thân nhựa, căn hai góc vát C13 với C14 và đẩy thẳng; không kéo dây để rút, không ép đầu sai chiều.
- Phích này có hai chân cấp điện và tiếp điểm đất riêng. Dùng ổ có tiếp đất tương thích; ổ hai lỗ nhận được chân không đồng nghĩa đã có tiếp đất. Không bẻ chân hoặc bỏ tiếp điểm đất.
- Ngắt điện ngoài trước khi mở máy; không dùng dây nứt vỏ hoặc đầu cắm lỏng, không mở vỏ PSU.

Đối chiếu kiểu đầu: [Interpower: CEE 7/7, hai chân Ø4,8 cách tâm 19 và tiếp đất](https://www.interpower.com/ic/designers/designing-for-export/national-power-mains/continental-europe.html); [SCHURTER: C14 và đầu C13 tương ứng](https://catalogue.schurter.com/en/datasheet/6100-3). Các số vỏ đúc và tọa độ dựng ở trên là lựa chọn mô phỏng của sheet.

## Kiểm tra ảnh

Ảnh tạo bằng **imagegen tích hợp**, đã sửa một lần theo brief. Có đủ FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM, góc 3/4, chi tiết hai đầu và tay đưa C13 vào C14; ba lỗ C13 và hai chân tròn thể hiện rõ.

Sai khác còn lại: kích thước **40 của C13 đang đặt sau đoạn mũi 10**, khiến ảnh trông như thân dài 50; số đúng là **40 gồm cả mũi 10**. Số **28** bị đặt cạnh cổ chống gập; đúng ra đó là **bề rộng Z của thân C13**, cổ chỉ Ø12 thu về Ø6,8. LEFT/RIGHT còn phóng to đầu cắm so với cuộn và đặt đầu hơi thấp; các hình chưa đồng nhất hoàn toàn về tỉ lệ và tư thế. Số vòng ảnh nhiều hơn mục tiêu 2,5; không dùng để suy ra chiều dài. Số 1–5 chỉ có trong bảng chú giải, số 6 có đường dẫn tới C14. Vết dập mờ trên phích chỉ là lỗi tạo sinh, không dựng lại thành chữ, logo hay dấu chứng nhận.

Prompt dùng cho ảnh, tóm tắt: “Black 1500 mm coiled mains cord, three-hole keyed C13 female to grounded EU wall plug with two Ø4.8 mm round pins; six labelled orthographic views, one 3/4 view, blue dimensions, connector details, hand mating C13 to PSU C14 with switch O, white background, no branding.” Lượt sửa yêu cầu đúng hình chiếu LEFT/RIGHT, bỏ đường kích thước 1500 qua cuộn, sửa kích thước thân C13 và xóa dấu dập.
