# low_profile_47 — Tản CPU thấp

Nguồn: [đặc tả họ vỏ](../00_families_spec.md), mục 0, 4, 5, 7.2 và 8; quy ước gá theo [Gaming](../../gaming/00_blueprint.md). Dùng cho `mini_sff` và `slim_console`. **Số trong tài liệu là chuẩn dựng; không đo theo pixel của [ảnh](low_profile_47.png).** Đây là linh kiện mô phỏng.

## Kích thước và tọa độ

Đơn vị mm; thứ tự dưới đây là **X × Y × Z**. Gốc O ở tâm mặt tiếp xúc IHS. X hướng ra trước máy, Y hướng lên nóc, Z hướng từ CPU ra quạt. Không đổi trục để gọi mặt quạt là TOP. Quy đổi 100 mm = 1 đơn vị cảnh.

| Chi tiết / số gọi | Bao và vị trí cục bộ |
|---|---|
| Cụm tản | **92 × 92 × 47**; X/Y = −46…46, Z = 0…47, gồm tai gá và đầu vít; không gồm dây mềm hoặc backplate |
| 1 — Quạt | **92 × 92 × 14**; X/Y ±46, Z = 33…47; tâm **(0;0;40)**. Hút từ +Z, thổi **−Z** qua lá tản |
| 2 — Lá / heatpipe | Trong X/Y ±46, Z = **5…33**; chừa vùng gá và đường dây. Heatpipe uốn thấp, không xuyên đế tiếp xúc hoặc cánh quạt |
| 3 — Đế | **40 × 40 × 5**; X/Y ±20, Z = 0…5; mặt tiếp xúc phẳng ở Z = 0 |
| 4 — Gá CPU | Bốn trục lỗ **(−39;−39), (−39;39), (39;−39), (39;39)** theo X/Y, song song Z; bước **78 × 78**, lỗ thông Ø3,5 |
| 5 — Gá quạt | Bốn trục tại **(±41,25;±41,25)**; bước **82,5 × 82,5**. Tai ren ở dưới mặt Z = 33; đầu vít chìm trong khung, không vượt Z = 47 |
| 6 — Dây PWM | Ra cạnh khung, đi ngoài vòng cánh. Chọn điểm thoát **(46;36;40)** cho mô hình; dây mềm đen, không tính vào bao cứng |
| 7 — Backplate | **90 × 90** theo X/Y, tâm trùng CPU; bốn trụ ren cùng lưới ±39. Đệm cách điện **0,8** giữa tấm và lưng PCB; không đặt sát đế tản |

Lưng PCB nằm tại Z = −9,2 theo gốc tản; đệm nằm Z = −10…−9,2. Bề dày tấm kim loại, hình tai, số/đường kính heatpipe và bước lá chưa được nguồn chốt; ảnh chỉ gợi dáng, không đặt thêm số giả. Tai gá phải nằm trong bao và chừa đường đưa vít; có thể tháo quạt trước khi siết gá CPU.

Lắp họ 04: O = **(−96;12;9,2)**, tâm quạt **(−96;12;49,2)**. Lắp họ 05: O = **(−86;−53;−20,8)**, tâm quạt **(−86;−53;19,2)**. Giữ nguyên hướng trục, không quay tản. Đáy lá cách đỉnh VRM ITX **2,6** theo bố trí gốc.

## Vật liệu, đầu cắm và vít

- Lá nhôm, đế/heatpipe kim loại sáng phủ nickel: **#AEB4BC**; khung/cánh nhựa đen **#101216**, đệm **#202329**, dây **#15171B**. Tiếp điểm **#C6A565**; không logo hoặc ARGB.
- Một đầu cái PWM **1 × 4**, đủ bốn lỗ tiếp điểm và gờ dẫn hướng khớp header **CPU_FAN**. Chân 1 GND, 2 +12 V, 3 tach, 4 PWM; nhận chân 1 theo khóa, không suy ra thứ tự trái/phải từ ảnh nghiêng. Không có đầu ARGB riêng.
- Gá CPU: **4 vít lò xo M3 × 12**, vào trụ ren bộ gá/backplate; không cộng bốn vít giữ CPU. Lỗ bo trùng lưới 78 × 78.
- Gá quạt: **4 M3 × 16**, xuyên khung dày 14 vào tai ren; đầu chìm. Tổng **8 vít giữ** cho hai nhóm, không dùng bước lỗ quạt case 105.

## Các hình chiếu

| View | Hướng nhìn / nội dung |
|---|---|
| FRONT +X | Nhìn −X; +Y lên, +Z trái. Cạnh quạt, lá, đế; chuỗi Z 14 + 28 + 5 = 47 |
| BACK −X | Nhìn +X; +Y lên, +Z phải. Cạnh đối diện và tai gá |
| LEFT +Z | Nhìn −Z; +Y lên, +X phải. **COMPONENT FACE**, mặt hút quạt, bốn vít quạt; gá CPU bị che |
| RIGHT −Z | Nhìn +Z; +Y lên, +X trái. Đế 40 × 40 và bốn điểm gá CPU |
| TOP +Y | Nhìn −Y; +X xuống, +Z phải. Cạnh cụm tản, quạt bên phải |
| BOTTOM −Y | Nhìn +Y; +X lên, +Z phải. Cạnh đối diện, quạt bên phải |
| 3/4 | Từ +X/+Y/+Z; thấy quạt, lá, gá và mũi tên gió −Z. Inset riêng cho đầu PWM và backplate |

## Sai lệch ảnh sau một lần sửa

Ảnh có đủ sáu view và một phối cảnh nhưng **chưa đạt đồng tỷ lệ**: TOP/BOTTOM nhỏ hơn FRONT/BACK. Glyph RIGHT vẫn chỉ X sang phải; glyph TOP/BOTTOM thiếu hoặc sai chiều X. Dùng bảng hướng nhìn ở trên. Lưới nét đứt 78 × 78 trên LEFT bị vẽ quá nhỏ và không khớp lỗ ở RIGHT. Chuỗi 5/28/14 và bề dày đế không khớp tỷ lệ hình học; không lấy đường dóng làm mặt phân lớp. Nhãn đế 40 ở RIGHT bị lặp theo chiều đứng, chiều ngang vẫn phải là 40. Đầu PWM nghiêng lớn và một số đầu dây nhỏ chỉ rõ ba khoang; **inset nhìn thẳng có bốn khoang mới là mẫu đếm đúng**. Vít trên backplate chỉ minh họa đầu trụ, không tăng số vít. Dây không xuất hiện nhất quán ở các view.

## Người mới cần chú ý

1. Chiều cao **47** đo từ IHS, đã gồm quạt **14**; không cộng thêm 14.
2. Gió đi **−Z vào lá tản**; mặt quạt là LEFT/+Z theo hệ trục này.
3. Phân biệt gá CPU **78** với gá quạt **82,5**; siết bốn vít CPU luân phiên đường chéo.
4. Backplate ở sau bo, có đệm cách điện; cắm quạt vào CPU_FAN và giữ dây ngoài cánh.

Ảnh tạo bằng công cụ imagegen tích hợp, đã sửa một lần. Prompt chính: sheet nền trắng, sáu hình chiếu đúng trục và một 3/4; tản 92 × 92 × 47, quạt 92 × 14 thổi −Z, hai lưới gá 78/82,5, PWM bốn chân, backplate 90 và đệm 0,8; nhãn tiếng Anh ngắn, kích thước xanh, không thương hiệu.
