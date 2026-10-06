# PSU SFX 100

Sheet: [psu_sfx_100.png](psu_sfx_100.png). Nguồn số liệu: [đặc tả tổng](../00_families_spec.md), mục 7 và 7.2; quy ước hình chiếu ở mục 8.1. Đây là linh kiện mô phỏng dùng cho họ 04/05. **Số trong tài liệu là chuẩn để dựng; không đo kích thước từ ảnh.**

## Kích thước và trục

Đơn vị mm; gốc O ở tâm thân. X từ mặt AC ra mặt dây, Y từ đáy lên nóc, Z từ hông phải sang hông trái. Bao cứng **125 Z × 63,5 Y × 100 X**, biên X ±50, Y ±31,75, Z ±62,5; không gồm dây mềm và đầu cắm cuối dây. Quy đổi 100 mm = 1 đơn vị cảnh.

## Các chi tiết

Các tâm C14, công tắc, quạt, grommet và lỗ gá giữ đúng đặc tả tổng. Bề dày, cửa cổng, cửa lưới và gá quạt dưới đây là lựa chọn bổ sung cho mô hình này, không phải bản khoan sản phẩm thương mại.

| Số | Chi tiết | Vị trí / hình học cục bộ (X; Y; Z) |
|---|---|---|
| 1 | Vỏ thép kín | Bao như trên; tôn 1, cạnh ngoài R1; TOP và hai hông đặc, không logo |
| 2 | C14, 3 chân đực | Tâm mặt cửa **(−50; −9; 43)**, hướng −X; bao mặt 28 Z × 20 Y, thân sâu 20 về +X, nằm trong vỏ; miệng có góc vát khóa, ba lá tiếp điểm L/N/PE |
| 3 | Công tắc I/O | Tâm **(−50; 17; 43)**; mặt 18 Z × 12 Y, sâu 12 về +X; giữ trong bao cao 63,5 |
| 4 | Quạt 92 | Tâm **(0; −24; 0)**, trục Y; khung 92 X × 14 Y × 92 Z, Y −31…−17; cánh nằm trong khung; cửa hút/lưới tròn Ø92 tâm (0; −31,75; 0), không nhô dưới đáy |
| 5 | Bốn lỗ gá nguồn | Ren M3 trên mặt **X = −50**, bốn tổ hợp **Y = ±25, Z = ±55**; bước tâm 50 Y × 110 Z; trục vít X |
| 6 | Grommet ra dây | Lòng Ø18, tâm **(50; −13; 35)**; viền cao su rộng 2, chìm trong mặt +X; cả ba nhánh cùng đi qua đây |
| — | Lưới xả sau | Mặt X −50; vùng Z −53…22, Y −23…23; tấm 0,8, lỗ tròn Ø2,4 bước 3,6 so le; chừa viền đặc 6 và vùng bán kính 5 quanh lỗ gá |
| — | Gá quạt/lưới đáy | Bốn tâm (±41,25; −31,75; ±41,25), bước 82,5 X/Z; đầu vít chìm, khác nhóm bốn lỗ gá nguồn phía AC |

Hút không khí từ phía **−Y vào +Y**, xả khỏi vỏ theo **−X**. Cánh, khung, lưới và vít quạt đều thuộc nguồn đã lắp kín; không thêm đầu PWM ngoài nguồn.

## Ba nhánh dây liền

Không có ổ modular. Dây đen mềm ra +X, uốn ngay theo case với R tâm ≥12; không thêm đuôi cứng dài 30. Chiều dài đo theo tâm dây từ grommet tới lưng đầu cắm, không gồm thân đầu cắm. Tư thế dây trong ảnh chỉ để nhận diện.

| Số | Đầu dây và khóa | Bao thân đầu để dựng | Họ 04 / họ 05: dài dây |
|---|---|---|---|
| 7 | ATX 24, 2 × 12 ổ tiếp điểm cái; ghép 20+4, ngàm giữ và ô vuông/vát có hướng | 52 rộng × 10 dày × 20 sâu, chưa tính ngàm | 300 / 250 |
| 8 | EPS CPU 8, 2 × 4; tách 4+4 thành hai cụm 2 × 2, gờ khóa riêng | 18 rộng × 9 dày × 20 sâu | 400 / 350 |
| 9 | PCIe GPU 8, 2 × 4; tách 6+2 thành cụm 2 × 3 và 2 × 1; ngàm và khóa khác EPS | 18 rộng × 9 dày × 20 sâu | 300 / 350 |

Bao đầu dây là lựa chọn mô phỏng; ô khóa phải kế thừa hình học đầu tương ứng của Gaming, không lấy hình ô trong ảnh làm sơ đồ đấu điện. C13 góc và dây nối AC 450 thuộc case, không phải nhánh DC thứ tư của PSU. Họ 05 dùng đầu GPU góc thấp có bao cao ≤12 theo Y toàn máy, thuộc bộ dây case.

## Vật liệu và vít

Thân thép sơn nhám `#24262A` (roughness 0,65, metallic 0,25); lưới `#101216`; nhựa đầu cắm/quạt `#101216`; cao su `#202329`; dây `#15171B`; kim loại vít/chân C14 `#AEB4BC`, tiếp điểm DC `#C6A565`. Không đèn, không tem công suất.

| Nhóm | Số lượng / loại | Cách lắp |
|---|---|---|
| Gá nguồn vào giá case | **4 M3 × 6** | Từ −X vào +X qua giá, theo bốn tâm ở số 5; không dùng mẫu lỗ PSU ATX |
| Gá quạt/lưới, cụm kín | 4 M3 × 16, lựa chọn mô hình | Từ −Y vào +Y, đầu chìm; không tạo thao tác tháo cho người học |

## Nội dung bảy hình

| Hình | Hướng nhìn / chi tiết |
|---|---|
| FRONT +X | Nhìn −X, +Z bên trái; grommet dưới trái và ba nhánh dây |
| BACK −X / AC FACE | Nhìn +X, +Z bên phải; C14 dưới phải, switch trên phải, lưới xả và bốn lỗ M3 |
| LEFT +Z | Nhìn −Z, +X bên phải; hông đặc, mép ra dây bên phải |
| RIGHT −Z | Nhìn +Z, +X bên trái; hông đặc, mép ra dây bên trái |
| TOP +Y | Nhìn −Y, +X xuống dưới, +Z sang phải; tấm nóc đặc 125 × 100 |
| BOTTOM −Y | Nhìn +Y, +X lên trên, +Z sang phải; quạt/lưới Ø92 và bốn vít gá quạt |
| 3/4 | Từ −X/−Y/+Z; thấy mặt AC, đáy quạt và hông; ba đầu dây có hình phóng to để đọc khóa |

Sáu hình chiếu dùng chung tỷ lệ thân; đuôi dây rút ngắn bằng ký hiệu ngắt. Hình phóng đầu cắm không cùng tỷ lệ. Khi lắp họ 04/05 dùng R−(x;y;z) = (x;z;−y): mặt hút chuyển sang +Z toàn máy.

## Người mới cần chú ý

1. 125 là chiều rộng, 100 là chiều sâu; không đổi sang SFX-L hoặc thu nhỏ nguồn ATX.
2. Quạt nằm ở đáy cục bộ −Y; mặt AC là lưới xả, không có quạt thứ hai.
3. Bốn lỗ gá case nằm ở mặt AC; bốn vít dưới đáy chỉ giữ cụm quạt.
4. EPS 4+4 và GPU 6+2 đều có tám tiếp điểm nhưng khác khóa, không thay thế nhau.
5. Ba nhánh dây gắn liền, chung một grommet; giữ dây cách cánh quạt ít nhất 10 sau khi buộc.

## Đối chiếu ảnh

Ảnh tạo bằng imagegen tích hợp, đã sửa một lần theo brief; có đủ sáu hình chiếu và một phối cảnh. Những sai lệch còn lại cần bỏ qua khi dựng:

- BOTTOM ghi nhầm chiều ngang **192**; đúng là **125**. Ø92 chỉ cửa quạt, 82,5 là bước tâm vít quạt.
- Inset EPS vẽ 6+6 ô; đúng phải **4+4**. Inset GPU vẽ 8+2 ô; đúng phải **6+2**. Số 18 là bề rộng **cả đầu 8 chân đã ghép**, không phải từng nửa như các mũi tên trong ảnh. ATX ở phối cảnh bị thu nhỏ và không thể hiện đủ 24 ô; dùng cấu trúc 2 × 12 ở bảng dây.
- Tỷ lệ thân giữa sáu hình chưa đồng nhất hoàn toàn; tâm O trong thân bị mất, một số ký hiệu trục TOP/BOTTOM chưa đúng. Dùng bảng hướng chiếu và gốc tâm ở trên.
- Phối cảnh còn vài vít vỏ trang trí không thuộc BOM. Chỉ dựng bốn vít gá case và bốn vít giữ quạt đã chốt; lưới, ngàm và hình ô khóa trong ảnh chỉ mang tính nhận diện.

Yêu cầu tạo ảnh: sheet nền trắng, thép graphite, nét kích thước xanh; sáu mặt đúng trục và một phối cảnh từ −X/−Y/+Z; C14, switch, quạt 92, bốn lỗ M3, grommet 18 và ba nhánh ATX/EPS/PCIe. Ảnh hỗ trợ nhận diện; bảng số liệu là hướng dẫn hình học chính xác.
