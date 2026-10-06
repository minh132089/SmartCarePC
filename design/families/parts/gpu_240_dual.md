# GPU 240 hai quạt

Nguồn: [đặc tả sáu họ](../00_families_spec.md), mục 0, 7.2 và 8; quy ước đầu cắm của [Gaming](../../gaming/00_blueprint.md). Dùng cho họ 02, 04, 05. Đơn vị **mm**. Số dưới đây là chuẩn dựng mô phỏng; không đo ngược từ ảnh và không dùng làm bản vẽ gia công.

## Kích thước và trục

Gốc **O=(0;0;0)** ở giữa chiều dài/chiều dày, trên cạnh PCB gần bo. **+X** về đầu card, **−X** về lá cổng; **+Y** về backplate, **−Y** về mặt quạt; **+Z** từ cạnh chân PCIe ra cạnh nguồn. Gốc không nằm giữa chiều cao 120.

Thân **240 X × 45 Y × 120 Z**: X ±120, Y ±22,5, Z 0…120. Cách gọi **240 × 120 × 45** là dài × cao × dày. Chân PCIe nhô tới Z −8; lá cổng X −121…−120 và Y tới 25,7. Bao gồm lá và chân, chưa gồm vít/đầu dây: **241 X × 48,2 Y × 128 Z**. Đầu vít nhô tối đa 3 ngoài mặt gá.

## Chi tiết và vị trí

| Số | Chi tiết | Tọa độ / hình học |
|---|---|---|
| 1 | Hai rotor Ø90 | Tâm `(−60;−12;60)` và `(60;−12;60)`, trục Y, cách tâm 120. Mặt hút −Y, gió đi +Y; hai cửa tròn riêng trên shroud |
| 2 | Backplate và PCB | Backplate ở phía +Y, không vượt Y22,5. PCB tâm Y19,7, dày 1,6 nên Y18,9…20,5; backplate chọn dày 2, Y20,5…22,5 |
| — | Shroud và tản | Vỏ bao X±120/Z0…120, mặt quạt Y−22,5; khe cạnh phải thông vào lá nhôm. Lá/ống nhiệt nằm trong bao, tránh rotor, chân PCIe và hốc nguồn; nguồn chưa chốt lưới lá, số ống và gá motor |
| 3 | Chân PCIe x16 | X−66,5…22,5, Z−8…0, cùng mặt PCB; dài 89, tâm X−22. Giữ khoảng 53,5…142,5 từ mặt thân phía lá cổng |
| 4 | Ổ nguồn 8 chân | Tâm miệng `(105;12,75;120)`, hướng +Z. Hốc chọn theo vỏ đầu Gaming: 18 X × 10,7 Y × 12 Z, X96…114/Y7,4…18,1/Z108…120; không cộng vào bao thân |
| 5 | Lá hai khe | X−121…−120, Y−14,94…25,7, Z−2…118; dày 1, rộng 40,64. Hai điểm giữ tại Y15,54/−4,78, Z113; bước 20,32 |
| 6 | Ba DisplayPort | Cửa hướng −X; tâm `(−121;12,7;20/44/68)`. Miệng mô phỏng 16 theo Z × 5 theo Y, một góc vát; mỗi cổng 20 tiếp điểm |
| 7 | Một HDMI | Cửa hướng −X; tâm `(−121;12,7;92)`. Miệng mô phỏng 14 Z × 5 Y, hai góc tạo hình thang; 19 tiếp điểm |

Vị trí và hình miệng cổng kế thừa bố trí GPU Gaming, dịch theo lá mới; đây là chi tiết mô phỏng. Vùng thoát khí trên lá nằm phía −Y, tách khỏi bốn ổ hình. Có thể kế thừa lưới lục giác Gaming: rộng qua đỉnh 2,6, bốn hàng theo Y, 17 vị trí theo Z, tâm `Z=4+6,6j`, `Y=−10,94+3,6i+1,8(j mod 2)`, i=0…3, j=0…16. Không bịt các cửa bằng mặt đen đặc.

## Khóa, vật liệu và vít

- **x16:** 164 tiếp điểm, 82 mỗi mặt (11 ngắn + 71 dài). Theo khóa Gaming, tâm rãnh X−54,85, rộng 2; móc giữ ở đầu +X của dải, X24,5…31,5, nhô tối đa 6 xuống −Z. Chốt nhả nằm trên khe bo/riser, không gắn thêm cần chốt lên GPU.
- **Nguồn:** một ổ 2 × 4, lỗ vuông/vát góc và gờ ngàm phía +Y; dùng đầu PCIe **6+2**, không dùng EPS **4+4**. Hình khóa và tiếp điểm lấy từ đầu GPU Gaming; không suy ra sơ đồ điện từ ảnh.
- **Màu:** shroud nhựa nhám `#16181D`, cánh `#101216`, backplate kim loại `#25282D`, lá tản nhôm `#AEB4BC`, PCB `#16181D`, nhựa cổng `#101216`, tiếp điểm vàng `#C6A565`. Không logo, không RGB.
- **Vít thao tác:** đúng **2 #6-32 × 6**, tái dùng từ nắp PCIe của case. Hai tai có rãnh mở rộng 3,8 ở các mốc Y/Z trên. Hướng tai và trục vít phải khớp gá case tương ứng; không lấy nét phối cảnh làm chuẩn khoan. Các vít nhỏ trên backplate/shroud là vít cụm kín, không tạo thao tác tháo trong bài học; nguồn chưa chốt số và tâm lỗ của chúng.

## Bảy hình nhìn

| View | Hướng nhìn và chi tiết phải thấy |
|---|---|
| FRONT +X | Nhìn −X, +Z bên trái: đầu tản 120 ngang × 45 cao, không cổng hình |
| BACK −X | Nhìn +X, +Z bên phải: lá 120 ngang × 40,64 cao, 3 DP + 1 HDMI, lưới và hai điểm giữ |
| LEFT +Z | Nhìn −Z, +X bên phải: cạnh 240 × 45; ổ nguồn gần đầu phải |
| RIGHT −Z | Nhìn +Z, +X bên trái: cạnh 240 × 45; thấy cạnh PCB/chân vàng, không thấy miệng ổ nguồn |
| TOP +Y | Nhìn −Y, +X xuống, +Z sang phải: backplate 120 ngang × 240 cao; lá cổng trên, chân vàng bên trái |
| BOTTOM −Y | Nhìn +Y, +X lên, +Z sang phải: hai quạt; lá cổng dưới, chân vàng bên trái |
| 3/4 | Nhìn từ −X/−Y/+Z: thấy hai rotor, lá cổng, tản và cạnh nguồn |

## Người mới cần chú ý

1. Lá bắt hai khe nhưng thân dày 45: phải để trống **ba khoảng khe** bên trong case.
2. Chân vàng x16 truyền dữ liệu/cấp điện qua bo; ổ 8 chân là đầu nguồn riêng. Cắm đúng rãnh rồi khóa khe.
3. Hai quạt đều hút từ mặt −Y vào tản. Không đặt dây nguồn hoặc dây riser qua mặt hút.
4. Họ 02 cắm trực tiếp R0; họ 04 quay R+ và họ 05 quay R− qua riser. Quay cả lá cổng, vít và ổ nguồn cùng card.
5. Khi tháo, đỡ card, rút nguồn 6+2, tháo hai vít và nhả chốt x16 trước khi kéo.

## Kiểm tra ảnh

Ảnh dùng công cụ imagegen tích hợp, một lượt tạo và một lượt sửa. Lệnh vẽ tóm tắt: “GPU 240 DUAL; white realistic graphite reference sheet; six orthographic views at one scale and one 3/4; body240×120×45, two90mm fans, keyed x16/164, one recessed2×4 power socket, two-slot bracket,3DP+1HDMI, blue dimensions, short numbered English labels, no logos.” Lượt sửa tập trung vào tỷ lệ hai mặt đầu, hướng TOP/BOTTOM, cạnh chân vàng và loại cổng hình.

**Sai lệch còn lại sau lượt sửa:**

- LEFT ghi nhầm **140 mm**; đúng là **240 mm**. Sáu hình chưa đồng nhất tỷ lệ, đặc biệt hai cạnh dài bị thu nhỏ; không đo pixel để dựng.
- BACK vẽ cả bốn miệng gần giống HDMI và đặt hàng cổng phía dưới; đúng là **3 DP một góc vát + 1 HDMI**, tâm Y12,7 ở phía +Y. Hình 3/4 cũng chưa phân biệt đúng các miệng cổng.
- Kích thước 120 giữa quạt đang chỉ vào khoảng giữa hai vòng; đúng phải đo **tâm đến tâm**. Dải chân vàng, rãnh khóa và số tiếp điểm chỉ minh họa, chưa đúng toàn bộ hình học đã ghi.
- Miệng nguồn ở TOP/BOTTOM bị xoay về phía người nhìn; thực tế chỉ mở **+Z**, vì vậy hai view này chỉ thấy cạnh vỏ ổ. Sơ đồ 2×4 bên phải dùng để nhận số lỗ.
- Số gọi tên mới nằm trong chú giải, chưa có đủ đường dẫn tới chi tiết. Hai vít nhỏ trên mặt lá chưa thể hiện rõ hai tai giữ theo bước20,32; vít trang trí backplate không phải lịch vít lắp case.

Ảnh đã có đủ sáu view và một góc3/4, đúng hai rotor; dùng để nhận dáng và vật liệu. Dựng hình theo bảng số và hướng chiếu trong tài liệu này.
