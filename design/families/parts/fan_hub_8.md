# fan_hub_8 — bộ chia 8 PWM + 8 ARGB

Ảnh: [fan_hub_8.png](fan_hub_8.png). Theo [đặc tả chung](../00_families_spec.md), mục 7 và 7.3; quy ước hình chiếu ở mục 8.1. **Số trong tài liệu là chuẩn để dựng**, không đo ngược từ ảnh. Các kích thước chi tiết bổ sung dưới đây là lựa chọn cho mô hình mô phỏng, không phải bản thiết kế mạch điện hay kích thước sản phẩm thương mại.

## Kích thước và tọa độ

Đơn vị mm. Gốc O ở tâm hộp; X dọc chiều 80, Y dọc chiều 50, Z dọc chiều dày 18; mặt nắp +Z. FRONT nhìn từ +X, BACK −X, LEFT +Z, RIGHT −Z, TOP +Y, BOTTOM −Y. Quy đổi 100 mm = 1 đơn vị cảnh.

| Chi tiết | Bao / vị trí cục bộ (X; Y; Z) |
|---|---|
| 1. Hộp | 80 × 50 × 18; X ±40, Y ±25, Z ±9; góc R2, thành/nắp dày 1,5; nắp ngàm, không vít tháo |
| 4. Hai tai liền | Mỗi tai 6 × 12 × 2; X −46…−40 và 40…46, Y ±6, Z −9…−7. Bao hộp + tai **92 × 50 × 18** |
| Lỗ / vít gá | Hai lỗ Ø3,4 xuyên Z tại (−43;0) và (43;0), cách tâm 86; **2 M3 × 5**, đưa từ +Z về −Z qua tai vào giá có ren của case |
| 2. PWM 1–8 | Tâm miệng (xᵢ;25;0), hướng +Y; miệng 9 × 6 theo X/Z, hốc sâu 7 về −Y; gờ khóa và chân nằm trong bao hộp |
| 3. ARGB 1–8 | Tâm miệng (xᵢ;−25;0), hướng −Y; miệng 9 × 6 theo X/Z, hốc sâu 7 về +Y; khóa, dấu +5V và chân khuyết |
| Ba cửa dây | Trên X=40, Z=0: nguồn tại Y=−14, Ø6; PWM tại Y=0, Ø4; ARGB tại Y=14, Ø4. Grommet ôm dây, không đi qua lỗ tai |
| PCB kín trong hộp | 76 × 46 × 1,6; tâm (0;0;−4); giữ bằng trụ/ngàm cách điện, không thêm vít thao tác |
| 5. Adapter ATX24 | Bao **25 X × 58 Y × 42 Z**; gốc riêng A ở giữa mặt tiếp xúc phía bo, thân Z riêng 0…42; đầu đực phía bo −Z, đầu cái nhận dây PSU +Z |

Với i=1…8, **xᵢ = −33,25 + 9,5(i−1)**. Cả hai dãy tăng số theo +X; mỗi chân cách nhau 2,54 theo X. Dấu tam giác trắng ở PWM1. Các tai và cổng đều nằm trong bao 92 × 50 × 18; dây, đầu cáp đã cắm và adapter tính riêng.

## Đầu cắm và dây

| Đầu | Tiếp điểm, khóa và cách nối |
|---|---|
| PWM ra, 8 cổng | Mỗi cổng **4 chân**, thứ tự theo +X: GND / 12V / TACH / PWM, gờ dẫn hướng bất đối xứng. Chỉ TACH của **cổng 1** nối về bo; TACH cổng 2–8 để hở điện |
| ARGB ra, 8 cổng | Mỗi cổng **3 chân trên 4 vị trí**: +5V / DATA / khuyết / GND theo +X. Vị trí 3 không có chân; không dùng đầu RGB 12V bốn chân |
| ATX24 nối tiếp | Hai mặt đều 2 × 12 vị trí, bước mô hình 4,2; mặt tiếp xúc trong bao 10 × 52. Có ngàm, hốc vuông/vát định hướng; nối đủ **24 đường một-một**, không chập các chân điều khiển. Bao cao 42 đo từ mặt tiếp xúc phía bo |
| 6. Nhánh nguồn | Adapter → hub, **650 dài theo tâm phần mềm**, không gồm hai thân cứng; mang 12V/5V/GND. Bó bọc Ø5, uốn R≥20. Yêu cầu nhánh ≥3A ở 12V và ≥2,5A ở 5V, bảo vệ quá dòng riêng từng rail |
| 7. Dây PWM điều khiển | Hub → SYS1; vỏ cái 4 vị trí có gờ khóa, **3 tiếp điểm** GND / bỏ trống / TACH / PWM. Vị trí 12V không có tiếp điểm, không cấp nguồn ngược về bo |
| 8. Dây ARGB điều khiển | Hub → ARGB bo; vỏ 4 vị trí, **2 tiếp điểm** bỏ trống / DATA / bịt khóa / GND. Vị trí +5V không có tiếp điểm; vị trí 3 bịt. Không nối +5V bo với rail đèn hub |

Hai dây điều khiển chọn dài tâm **650 mỗi dây**, bó Ø3, đầu cứng 10 × 7 × 12, gờ khóa nằm trong bao này. Đây là kích thước bổ sung của sheet. Tất cả dây thật màu đen; nét màu chỉ là chú thích. Không thêm SATA, nút bấm, LED hoặc quạt lên hộp hub.

Tư thế trưng bày: O cố định; gốc adapter A=(115;0;−9), nên tâm bao adapter là (115;0;12). Tâm hai đầu điều khiển lần lượt (65;60;0) và (95;60;0), miệng hướng +Z. Dây nguồn cuộn lỏng phía Y<−50; dây điều khiển cuộn riêng phía Y>50; các đoạn giao nhau lệch Z đủ để không xuyên nhau. Giữ cùng tư thế khi đổi sáu hướng nhìn. Chiều dài 650 là chiều dài theo đường cong, không phải khoảng cách hai đầu hoặc kích thước bao cuộn dây.

## Vật liệu và vị trí sử dụng

- Hộp ABS nhám `#202329`, nhựa đầu nối `#101216`, PCB `#16181D`; tiếp điểm `#C6A565`, vít `#AEB4BC`, dây/grommet `#15171B`. Chữ số trắng xám `#D2D7DD`; đường kích thước xanh `#2164AC`. Không logo, không đèn trang trí.
- Họ 03: tâm hub (85;−148;−85), hai vít M3 trong khoang sau; 6 motor case vào PWM1–6, 8 vòng đèn vào ARGB1–8. Hai motor két vào CPU_FAN qua dây Y, bơm vào AIO_PUMP.
- Họ 06: tâm hub (90;70;−110), bao dày Z=−119…−101; 5 motor case vào PWM1–5, toàn bộ ARGB để trống có nắp. Ba motor két dùng dây chia riêng tới CPU_FAN. Khi lắp sau khay, quay cụm để mặt gá tai áp giá và còn khoảng tháo đầu cắm; các tâm hộp trên giữ nguyên.

## Nội dung từng hình chiếu

| View | Chi tiết cần đọc |
|---|---|
| FRONT +X | Mặt ngắn 50 × 18, ba grommet, cạnh tai; adapter và dây ở cùng tư thế |
| BACK −X | Mặt ngắn kín, đường ráp ngàm; không thêm cổng |
| LEFT +Z | Nắp 80 × 50, bao tai 92, hai lỗ cách 86, thứ tự hai dãy 1–8 và gốc O |
| RIGHT −Z | Mặt đáy kín, hai tai; phía đối diện của adapter ATX24 |
| TOP +Y | Trực diện đủ 8 cổng PWM, mỗi cổng 4 chân và gờ khóa |
| BOTTOM −Y | Trực diện đủ 8 ARGB, mỗi cổng có chân khuyết thứ ba |
| 3/4 | Độ dày hộp, tai gá, adapter nối tiếp, nhánh nguồn và hai dây điều khiển tách biệt |

## Người mới cần chú ý

1. Một quạt có đèn dùng hai đường khác nhau: PWM cho motor, ARGB cho đèn.
2. Hub lấy điện qua adapter ATX24; dây SYS1 và ARGB điều khiển không thay dây nguồn.
3. Chỉ quạt ở PWM1 trả tốc độ. Không gộp tám tín hiệu TACH về bo.
4. ARGB khuyết chân thứ ba; đầu điều khiển còn bỏ tiếp điểm +5V để tách hai nguồn.
5. 92 là chiều dài gồm tai, 80 là thân hộp. Không cộng chiều dài cuộn dây vào kích thước cơ khí này.

## Kiểm tra ảnh

Ảnh 1536 × 1024 tạo bằng **imagegen tích hợp**, đã sửa lại một lần theo brief. Có sáu hình chiếu thân hộp, một phối cảnh bộ dây và chi tiết đầu cắm. Ảnh còn các sai lệch sau; **không sao chép chúng vào mô hình**:

- FRONT ghi nhầm tên ba dây: chuẩn là Y=−14 nguồn, Y=0 PWM, Y=+14 ARGB. Chiều dài tai nhô thêm 6 theo X, không dùng số 9 dưới FRONT làm kích thước tai.
- TOP/BOTTOM đã có tám cổng mỗi dãy nhưng số chân nhỏ bị giản lược: PWM phải có 4, ARGB phải có 3 ở vị trí 1/2/4. Hình đầu ARGB điều khiển còn giống ba tiếp điểm; phải bỏ tiếp điểm vị trí 1 và bịt vị trí 3 theo bảng.
- Thứ tự số ở TOP và mũi tên trục BOTTOM/RIGHT chưa đúng. Luôn đánh số tăng theo +X; TOP có +X xuống, BOTTOM có +X lên; RIGHT có +X sang trái. Chữ phụ “TOP VIEW/BOTTOM VIEW” trong LEFT/RIGHT chỉ mặt nắp/đáy, không đổi tên trục.
- Adapter chưa thể hiện đáng tin cậy hai mặt đối nhau với đủ 2 × 12 vị trí. Dựng theo bao 25 X × 58 Y × 42 Z và sơ đồ nối tiếp 24 đường; không theo hình hốc nhỏ trong ảnh.
- Sáu hình chiếu lược bỏ phần lớn bộ dây/adapter, chưa giữ đủ tư thế của cả cụm mềm; phối cảnh còn chồng nét nhánh nguồn và lặp nhãn 6 lên dây điều khiển. Chỉ dựng một nhánh nguồn adapter → hub và hai dây điều khiển độc lập, đúng tọa độ trưng bày đã ghi. Tỷ lệ giữa các ô chỉ để tham khảo.

Prompt sử dụng, tóm tắt: “Brand-free fan hub 8, white technical reference sheet, six orthographic views and one 3/4 kit view; 92 × 50 × 18 envelope, eight PWM plus eight ARGB edge ports, keyed ATX24 pass-through adapter, 650 mm power lead and separate signal leads; blue dimensions, pin details and two mounting screws.” Lần sửa yêu cầu dựng lại hình chiếu theo đúng trục, chuyển cổng từ nắp ra hai cạnh và phân biệt chân trống trên dây điều khiển.
