# console_stand — Đế đứng và bộ chân đặt ngang

Nguồn: [đặc tả họ vỏ, mục 5.1 và 7](../00_families_spec.md), [concept slim console](../../case-families/05_slim_console.png). Đơn vị **mm**. Số trong tài liệu là chuẩn dựng mô hình; không đo ngược từ ảnh tạo sinh. Đây là thiết kế mô phỏng, chưa phải bản vẽ gia công.

## Kích thước và tọa độ

Gốc cục bộ **O** ở tâm bao đế. **X** từ lưng ra trước, **Y** từ đáy lên nóc, **Z** từ hông kín sang hông mở. Bao **280 X × 18 Y × 140 Z**: X ±140, Y ±9, Z ±70. Theo thứ tự W × H × D của dự án, đế là **140 × 18 × 280**. Quy đổi 100 mm = 1 đơn vị cảnh.

Khi lắp đứng: `P_toàn_máy = (0;−189;0) + P_cục_bộ`, không quay trục. Đế nằm Y toàn máy −198…−180; thân máy bắt đầu Y −180. Bao cả máy đứng **140 Z × 378 Y × 330 X**.

| Số gọi | Chi tiết | Vị trí / kích thước |
|---|---|---|
| 1 | Hai ray vát | Dọc X −140…140, đối xứng hai phía Z; bao ngoài Z ±70, cao tổng không vượt Y ±9. Má đỡ nhận thân rộng 100 |
| 2 | Hai cầu nối | Tâm X **−85/+85**, chạy theo Z giữa hai ray; hạ thấp khỏi mặt kê để gió đi ngang dưới thân |
| 3 | Hai điểm gá | Trục vít song song Y, tâm tại **(−85;9;0)** và **(85;9;0)** trên mặt tiếp xúc; bước **170**. Toàn máy: **(±85;−180;0)** |
| 4 | Đệm lòng đế | Cao su dày **1 mỗi bên**, dọc má đỡ; nằm trong bao cao 18, không cộng thêm vào chiều cao |
| 5 | Khoảng thoáng | Để rỗng giữa ray/cầu; không thêm tấm đáy kín, lưới giả hoặc quạt |
| 6 | Bốn chân ngang rời | Mỗi chân **12 X × 12 Y × 8 Z**. Tâm toàn máy **(−137;−148;−54)**, **(−137;148;−54)**, **(137;−148;−54)**, **(137;148;−54)** |

Để dựng chi tiết chưa được nguồn chốt, chọn bổ sung cho sheet này: khe kim loại rộng **102**, đệm hai bên đưa lòng hữu dụng về **100**; mặt trong kim loại Z ±51, mặt cao su Z ±50. Ray có vai kê vào phía trong tới |Z| = 40, mặt kê có đệm kết thúc tại Y = 9. Cầu rộng **12 theo X**, phần ngang thấp tại Y −6…−4, tôn dày **2**; giữa cầu có tai nâng uốn lên mặt Y = 9, vùng gá **16 X × 16 Z**, dày 2. Lỗ thông **Ø4,5**, một lỗ mỗi tai; hai vít đưa từ dưới theo +Y vào gá ren của vỏ. Những số bổ sung này là lựa chọn mô hình, không phải số đo từ concept.

Hai cửa đáy vỏ nằm X −138…−20 và 20…138, Z −30…30. Tâm gá X ±85/Z0 nằm trong hình chiếu hai cửa theo nguồn: cần chừa đảo gá nhỏ và hạ cầu bên dưới, giữ đường gió quanh tai; không hiểu “không che cửa đáy” là không có bất kỳ giao nhau nào trong hình chiếu. Không biến cả cầu thành dải đặc áp sát đáy vỏ.

## Vật liệu, liên kết và vít

- Ray/cầu/tai chịu lực: thép sơn graphite **#343639**, roughness 0,55; chọn tôn gấp 2 mm cho mô hình đế. Các mối nối cố định, **0 vít tháo nội bộ**.
- Đệm và bốn chân: cao su đen **#202329**, roughness 0,85. Vít kim loại **#AEB4BC**. Không logo hoặc đèn.
- Gá đế vào vỏ: đúng **2 M4 × 10**, chiều dài tính dưới đầu. Chừa đường đưa tua vít từ mặt dưới; đầu vít không vượt mặt đặt bàn Y = −9 của đế.
- Chân ngang dùng keo định vị, **0 vít**; không áp dụng quy ước M4 × 8 của chân case thông thường.
- **Không có đầu cắm điện, chân tín hiệu, cổng, nút, dây hoặc grommet** trên cụm này. Hai lỗ M4 là liên kết cơ khí.

Khi đặt ngang, tháo đế và hai vít trước; dán bốn chân lên hông **−Z**, +Z hướng lên. Chân chiếm Z −58…−50, mặt bàn Z = −58, nâng hông 8. Bao theo trục thân là **330 X × 360 Y × 108 Z**. Bộ chân rời không cộng vào bao đế đứng.

## Nội dung các view

| View | Hướng nhìn và nội dung |
|---|---|
| FRONT +X | Nhìn −X, +Y lên, +Z trái; tiết diện hai ray, lòng đệm 100, bao 140 × 18 |
| BACK −X | Nhìn +X, +Y lên, +Z phải; hai đầu ray phía lưng |
| LEFT +Z | Nhìn −Z, +X phải; chiều dài 280, chiều cao 18, cầu khuất dùng nét đứt |
| RIGHT −Z | Nhìn +Z, +X trái; ray đối diện, cùng bao và mức cao |
| TOP +Y | Nhìn −Y, +X xuống, +Z phải; hai ray, hai cầu, đúng hai lỗ gá và khoảng rỗng |
| BOTTOM −Y | Nhìn +Y, +X lên, +Z phải; mặt dưới, đường tiếp cận hai vít |
| 3/4 | Từ +X/+Y/+Z; dáng ray vát, cầu thấp, đệm và tai gá. Khu phụ thể hiện bốn chân rời và hai vít |

## Sai lệch ảnh sau một lần sửa

Ảnh đủ sáu nhãn mặt và một phối cảnh chính, nhưng **chưa đồng tỷ lệ** dù chú thích cuối ảnh ghi vậy. FRONT/BACK còn thấy mặt trên; mũi tên +Z của FRONT chỉ sang phải, phải đổi sang trái khi dựng. Đường dóng 140 ở FRONT/BACK chưa bám đúng hai mép ngoài. Một số tai gá nhô khỏi đường kích thước 18; bao chuẩn vẫn Y ±9, gồm đệm và tai. Hai đoạn ghi 85 ở TOP/BOTTOM chưa đúng tỷ lệ với chiều dài 280; tâm lỗ phải là X ±85. Mặt dưới/tai nâng và bề dày tôn chưa thể hiện đầy đủ.

Khu chân có **bốn chân trong nhóm 2 × 2 và một hình chi tiết phóng to bên phải**; hình phóng to là bản lặp của một chân để ghi 12 × 12 × 8, **không phải chân thứ năm**. Ảnh cuối thiếu kích thước lòng 100/102 và đệm 1; dùng số trong tài liệu này.

## Người mới cần chú ý

1. Đế chống đổ dùng khi máy đứng; tháo đế trước khi đặt ngang.
2. Hai điểm gá cách nhau 170; chỉ có hai vít M4 × 10 cho đế.
3. Khoảng giữa hai ray là đường thoáng thật; không dựng thành tấm kín.
4. Bộ ngang có đúng bốn chân dán, nâng hông −Z lên 8; để mặt hút +Z hướng lên.
5. Cao 18 đã gồm đệm; tọa độ lắp không lấy từ vị trí tách chi tiết trong ảnh.

Ảnh tạo bằng **imagegen tích hợp**, sửa một lần. Prompt chính: “CONSOLE STAND; white reference sheet; six orthographic views plus one 3/4; graphite open cradle 280 X × 18 Y × 140 Z; two chamfered rails, two recessed bridges at X ±85, two M4 mounts, 1 mm rubber lining, open airflow; four loose 12 × 12 × 8 adhesive feet; two M4 × 10 screws; blue dimensions, numbered short English labels, no branding.” Prompt sửa: “TOP: +X down/+Z right; BOTTOM: +X up/+Z right; all six views same scale; FRONT/BACK flat orthographic; all parts within 18 mm; exactly four feet, dimension one of them; retain two rails, two bridges, two holes and two screws.” Các sai lệch còn lại đã ghi phía trên.
