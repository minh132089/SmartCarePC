# Tản tháp `tower_air_155`

Nguồn: [đặc tả sáu họ](../00_families_spec.md), mục 0, 7.2 và 8; [quy ước Gaming](../../gaming/00_blueprint.md). Dùng cho họ 01 và 02. Đơn vị **mm**; số trong tài liệu là chuẩn dựng, không đo ngược từ ảnh. Chi tiết bổ sung dưới đây là lựa chọn cho mô hình, không phải thông số sản phẩm thương mại.

## Trục và kích thước

**O=(0;0;0)** tại tâm mặt tiếp xúc IHS. +X về trước case, +Y lên nóc, **+Z từ bo ra phía linh kiện**. Chiều cao tản 155 chạy theo Z, không phải Y. Bao tản gồm tai gá: **90 X × 120 Y × 155 Z**, X±45, Y±60, Z0…155. Backplate phía sau bo và dây mềm nằm ngoài bao này.

## Cụm chi tiết

| Số | Chi tiết | Kích thước, vị trí đã lắp |
|---|---|---|
| 1 | Đế | 40 X × 40 Y × 5 Z; X/Y±20, Z0…5; mặt tiếp xúc phẳng Z0 |
| 2 | Heatpipe | **4 ống Ø6** liên tục, uốn chữ U, nằm trong X−40…10; đi từ đế lên lá. Chọn bốn mặt phẳng Y=−15/−5/5/15, hai nhánh đứng mỗi ống ở X−30 và 0; đầu kết thúc không vượt Z155. Tám đầu nhánh không có nghĩa là tám ống |
| 3 | Khối lá | X−40…10, Y±60, Z30…155. Nhôm dày **0,3**, bước **1,8** theo Z; lá nằm trong mặt phẳng XY, khe gió thông theo X. Phần mép/cổ ép nằm trong bao; không dựng thành khối kín |
| 4 | Quạt | **25 X × 120 Y × 120 Z**, tâm **(22,5;0;90)**; X10…35, Y±60, Z30…150. Hút từ +X, thổi **−X** qua lá |
| 5 | Kẹp quạt | **2 kẹp thép Ø1,2**, một bên Y âm, một bên Y dương. Móc vào khung quạt và mép lá, giữ toàn bộ trong X±45/Y±60/Z0…155 |
| 6 | Tai gá và vít | Bốn tâm **(±39;±39)**, bước **78 × 78**, trục vít Z. Chọn tai thép dày 2 tại Z5…7, lỗ Ø3,5, mép ngoài X±45; nối tai về đế và tránh đường heatpipe |
| 7 | Backplate | **90 X × 90 Y**, X/Y±45; chọn thép dày 2 tại Z−12…−10. Đệm cách điện **0,8** ở Z−10…−9,2. Bốn trụ ren liền tại X/Y±39; không thêm bốn vít giữ trụ |
| 8 | Dây PWM | Một dây đen ra góc khung chọn tại (22,5;58;38), men khung ngoài vùng cánh. Một đầu cái 1×4 có khóa vào CPU_FAN; dây mềm không dùng để đo bao tản |

Mốc lắp backplate: lưng PCB ở Z−9,2, mặt linh kiện PCB ở Z−7,6, IHS ở Z0. Bốn cổ xuyên lỗ bo Ø3,5 có cách điện; trụ nâng tới mặt dưới tai Z5, ren trong M3. Chọn đầu dưới mũ vít tại Z12, thân dài 12 xuống Z0, lò xo nằm giữa tai và mũ; phần ren ăn vào trụ khoảng 5. Không đặt tấm thép chạm đường mạch. Các chiều dày/trụ vừa chọn chỉ chốt hình học mô phỏng của sheet.

Quạt có bốn lỗ xuyên khung Ø4,5 (chọn), trục X: Y±52,5 và Z37,5/142,5, bước **105 × 105**. Lỗ hai mặt là hai miệng của cùng bốn lỗ; **không dùng vít dài xuyên lá**.

## Vật liệu, đầu cắm và vít

Lá nhôm `#AEB4BC`; đế đồng và heatpipe mạ nickel cùng màu bạc `#AEB4BC`; tai, kẹp và vít thép `#969DA5`. Khung/cánh quạt nhựa đen `#202329`, dây `#15171B`, đầu cắm `#101216`, đệm cách điện `#202329`. Không logo, không ARGB.

Đầu PWM có **4 ổ tiếp điểm cái**, một hàng, gờ dẫn hướng bất đối xứng và dấu chân 1. Theo thứ tự đánh số: **1 GND, 2 +12 V, 3 TACH, 4 PWM**; không đảo chân theo chiều nhìn mặt sau. Chọn thân mô phỏng 10 × 5 × 8, bước tiếp điểm 2,54; dây dài tâm 350, bó phần dư sau khay, cách cánh ≥10. Không có cổng, nút hay grommet rời trên tản.

| Giữ cụm | Số lượng | Loại |
|---|---:|---|
| Tản → bộ gá/backplate | **4** | **M3 × 12 có lò xo**, siết từ +Z về −Z |
| Quạt → khối lá | **2** | Kẹp dây thép Ø1,2, không phải vít |
| Backplate | **4** | Trụ ren liền, không cộng vào số vít |

Họ 01 đặt O tại **(−60;104;−73,8)**; họ 02 tại **(−49;104;−59,8)**. Giữ nguyên hướng trục, cộng trực tiếp tọa độ cục bộ. CPU_FAN tương ứng theo tọa độ toàn máy là (−44;171;−73,4) và (−24;169;−59,4).

## Các hình chiếu

Sáu hình vuông góc cùng tỷ lệ dự kiến; hình 3/4 dùng để đọc kết cấu. Hình tách backplate chỉ giải thích hướng lắp, không biểu diễn tọa độ đã lắp.

| View / hướng nhìn | Nội dung và hướng trên giấy |
|---|---|
| FRONT +X → −X | Mặt hút quạt, bốn lỗ khung; +Y lên, +Z sang trái |
| BACK −X → +X | Cửa ra qua lá, heatpipe; +Y lên, +Z sang phải |
| LEFT +Z → −Z | Đầu khối lá, đầu ống; quạt chỉ hiện cạnh bên phải; +Y lên, +X sang phải |
| RIGHT −Z → +Z | Mặt đế, lưới gá, backplate nét ẩn; +Y lên, +X sang trái |
| TOP +Y → −Y | Biên dạng lá/quạt/ống/đế; +X xuống, +Z sang phải |
| BOTTOM −Y → +Y | Biên dạng đối diện, kẹp và dây; +X lên, +Z sang phải |
| 3/4 từ +X/+Y/+Z | Một quạt, bốn heatpipe, đế, tai, bốn vít lò xo; backplate tách theo −Z |

## Người mới cần chú ý

1. Đo 155 từ IHS ra phía +Z; không cộng backplate vào chiều cao tản.
2. Quạt ở +X đẩy gió về −X, cùng hướng từ trước ra sau case.
3. Bốn vít lò xo giữ tản; hai kẹp giữ quạt. Không thay kẹp bằng vít xuyên lá.
4. Backplate ở sau bo, có lớp cách điện; siết bốn vít luân phiên theo đường chéo.
5. Bóc màng bảo vệ đế, dùng keo tản nhiệt và cắm đúng CPU_FAN 4 chân trước khi vận hành mô phỏng.

## Kiểm tra ảnh

Ảnh tạo bằng **imagegen tích hợp**, đã sửa lại một lần theo brief; có đủ sáu view và một hình 3/4, nhãn đọc được, không logo. Ảnh vẫn có các sai lệch sau, phải sửa theo số trong spec khi dựng:

- Mũi tên xanh trên hình 3/4 vẫn hướng ra khỏi mặt hút. **Chiều đúng là từ +X xuyên qua quạt vào lá, về −X**; không làm theo mũi tên ảnh.
- BACK thiếu phần đế/gá bên trái; LEFT chỉ thể hiện sáu đầu ống. Phải có bốn ống chữ U, hai nhánh mỗi ống, tổng tám đầu. Hình 3/4 che khuất một phần vít và ống; không dùng số chi tiết thấy được làm BOM.
- Trụ backplate vẫn giống ren ngoài, dù chú thích ghi female. Dựng trụ có **ren trong M3**; đầu vít hướng +Z. Một số vít trong FRONT/RIGHT và chiều dài ký hiệu 12 chưa đúng cách chiếu/đo dưới mũ.
- Tỷ lệ giữa các view và đường dóng 90/120/155 chưa khớp tuyệt đối; chuỗi 30/120/5 bị đặt sai phía ở FRONT. Kích thước đúng: khoảng từ IHS tới quạt 30, quạt dài 120 theo Z, phần lá nhô thêm 5. Bước lỗ quạt 105 × 105 chưa được ghi trên ảnh. Triad chỉ mang tính chỉ dẫn, không dùng suy ra phép quay hình 3/4.


<details>
<summary>Prompt sửa ảnh cuối — công cụ imagegen tích hợp</summary>

```text
Revise this engineering sheet, preserving title, white/blue style, six-view grid, short labels, right 3/4, bottom details. Correct mechanical projection and airflow, not just labels. Current image incorrectly shows upright cooler geometry where the printed axes demand other orientations.
Use SAME SCALE for six ortho views.
FRONT +X: entire actual cooler must lie SIDEWAYS with contact base at RIGHT (Z=0), distant end cap at LEFT (Z=155), +Y up. Fan is face-on square120x120 at Z30..150, centered Y0; therefore its right edge is30 away from base datum and left edge5 away from far end. The frame is120 wide on page, overall cooler155 wide. Fins behind fan. NO heatpipe tips sticking up over fan: they point LEFT along +Z. Four mounting screws point horizontally, heads toward LEFT.
BACK -X: mirror the geometry placement, NOT the fan: base at LEFT and fin end cap at RIGHT, +Z right, +Y up. Fin channels are VERTICAL lines in this projection because each plate is XY plane and plates stack along horizontal Z. No fan face visible. Dimensions155 horizontal,120 vertical.
LEFT +Z: true end-on view shows only rectangular metal end cap50 X by120 Y with EIGHT small pipe ends (4 U heatpipes), and black fan edge25 along X at RIGHT. No base hanging below the end cap, base hidden behind. Overall mounting ears extend width90 in hidden line. 120 vertical,90 horizontal.
RIGHT -Z: view from contact base side, 40x40 contact base CENTER, four ears/screws on78x78 grid; fin stack footprint behind base is50x120 and fan edge on LEFT (because +X left). Remove fan edge from RIGHT. Backplate drawn dashed transparent outline so base visible. 90 horizontal120 vertical.
TOP +Y: base at LEFT Z0, pipe end cap at RIGHT Z155, +X down: fin profile upper part, fan narrow25 thick strip at BOTTOM side extending Z30..150. Fin layers VERTICAL page lines. No full end cap face here.90 vertical155 horizontal.
BOTTOM -Y: base LEFT, pipe end RIGHT, +X up: fan25 thick strip at TOP, fin profile below.90 vertical155 horizontal.
Perspective may remain conventional cooler standing in illustration for readability but clearly triad there must mark +Z along base-to-fin-top, +X out from fan inlet, +Y across width. Reverse BLUE AIRFLOW arrow to point INTO black fan and through silver fins (-X), NOT toward viewer out of fan. Label 'INTAKE +X / FLOW -X'. Show FOUR U heatpipes only, eight terminal tips. Remove male threaded studs rising from backplate: backplate has FOUR integral FEMALE threaded posts receiving four spring M3x12 screws from cooler side. Keep actual backplate square90x90 with pad0.8, behind ghostPCB1.6; screws4, clips2, fan1. Show fan thickness25 and fan hole pitch105x105 as small dimensions. Keep labels readable and numbered callouts1..8.
All six views must be geometrically different correct projections, not random rotated photographs. No extra perspective view, no extra fan, no branding.
```
</details>
