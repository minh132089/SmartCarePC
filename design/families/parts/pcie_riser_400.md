# Riser PCIe x16, dải mềm 400

Nguồn: [đặc tả sáu họ](../00_families_spec.md), mục 0.4, 7 và 8; [Gaming](../../gaming/00_blueprint.md), mục 3; khóa kế thừa `PCIE_KEY=11.65` trong `tools/build_gaming.py`. Dùng cho họ **03, 04, 05**. Đơn vị **mm**, 100 mm = 1 đơn vị cảnh. Số trong tài liệu là chuẩn dựng mô phỏng, không đo ngược từ ảnh và không dùng làm bản vẽ chế tạo. Vị trí chi tiết chưa có trong nguồn được chọn rõ dưới đây.

## Kích thước, trục và tư thế

Sáu hình chiếu dùng cùng tư thế **duỗi thẳng**, không xoắn. Gốc **O=(0;0;0)** ở giữa đường tiếp giáp dải mềm với đầu đực. **X** chạy ngang dải, +X về phía chốt đầu cái; **+Y** dọc dải từ đầu đực đến đầu cái; **+Z** ra mặt có miệng khe cái. Đây là trục riêng của sheet, không phải tọa độ lắp trong case.

Dải mềm **400 dài tâm × 70 rộng × 1,5 dày**. Đầu đực **89 X × 12 Y × 1,6 Z**; cụm đầu cái **110 X × 18 Y × 14 Z**, đã gồm tai và chốt đóng. Bao duỗi thẳng, chưa tính hai vít rời: **110 X × 430 Y × 14 Z**. Số 400 chỉ tính phần mềm, không gồm hai đầu cứng; khi uốn, giữ chiều dài đường tâm 400, không giữ bao 430.

## Chi tiết và vị trí

| Số | Chi tiết | Biên / vị trí cục bộ đã chọn |
|---|---|---|
| 1 | Dải shield đen | X ±35; Y 0…400; Z ±0,75. Vân dọc Y, không biến vân thành các dây rời hoặc số đường điện |
| 2 | Đầu đực x16 | PCB X ±44,5; Y −12…0; Z ±0,8. Cạnh cắm hướng −Y, vàng trên cả hai mặt ±Z; vùng tiếp điểm chọn Y −12…−4. Không tai gá, không lỗ vít |
| 3 | Adapter cái | PCB X ±55; Y 400…418; Z −0,8…0,8. Thân khe X ±44,5; Y 404,5…413,5; Z 0,8…11,8. Miệng hướng +Z, tâm `(0;409;11,8)`; lòng mở rộng 1,6 theo Y, sâu 8 từ miệng, có vách khóa |
| 4 | Chốt nhả | Một chốt ở đầu +X. Bao đóng chọn X 44,5…48,5; Y 402…406; Z 0,8…13,2. Chuyển động mở chỉ minh họa, không suy góc quay từ ảnh |
| 5 | Hai tai gá | Tận dụng hai đầu PCB X −55…−45 và 45…55, Y 400…418. Hai lỗ xuyên Ø3,4, trục Z, tâm `(−50;409;0)` và `(50;409;0)`, cách tâm **100** |
| 6 | Rãnh / vách khóa | Tâm X **−32,85**, cách đầu −X của dải chân 11,65. Rãnh đầu đực rộng 2, sâu 3 từ cạnh Y−12. Vách đầu cái rộng 1,8 theo X, cùng tâm, nằm giữa hai nhóm tiếp điểm |

Hai chuyển tiếp vào dây nằm trong bao đầu cứng, không cộng thêm chiều dài hoặc bề dày. Chi tiết lòng khe, chốt, lỗ và vít là lựa chọn hình học mô phỏng; cần kiểm tra khớp mesh GPU khi dựng.

## Đầu cắm, vật liệu, vít và uốn

- Cả hai đầu là **PCIe x16, 164 tiếp điểm**, 82 mỗi mặt/hàng đối diện, chia **11 + 71** bởi khóa lệch tâm. Đầu đực cắm vào bo; đầu cái nhận chân GPU. Giữ thứ tự đường nối tương ứng, không suy pinout điện từ hình. Không khẳng định thế hệ PCIe hoặc tốc độ chưa kiểm thử.
- Dải bọc shield đen `#15171B`, roughness 0,8; PCB `#16181D`; vỏ khe và chốt nhựa `#101216`; tiếp điểm `#C6A565`; vít kim loại `#AEB4BC`. Shield nằm dưới lớp cách điện, không vẽ bề mặt dẫn điện trần. Không logo hay đèn.
- Vít gá chọn **2 M3 × 5**, một vít mỗi tai, đưa từ +Z về −Z vào giá có ren của case. Đầu vít chọn Ø5,5 × cao 2; tại X±50/Y409. Vít rời trong sheet không cộng vào bao cụm. Giá phải đỡ mặt dưới tai Z−0,8 và chừa khoảng dưới PCB; không bắt vít xuyên dây. Hai vít này khác vít giữ giá GPU vào case và vít lá cổng GPU.
- **R tâm ≥15** cho dải mềm. Với bề dày 1,5, tại R tâm 15 có R trong 14,25 và R ngoài 15,75. Uốn theo mặt YZ quanh X; giữ đoạn thẳng tối thiểu 10 sát mỗi đầu cứng như lựa chọn dựng, không bẻ ngay mối nối. Hình chữ U chỉ giải thích bán kính, không đổi tư thế sáu view.
- Theo tuyến của từng case: họ 03 vòng chùng dưới GPU; họ 04 qua 04-G4 96 × 12; họ 05 qua 05-G4 96 × 10. Luồn **đầu đực 89** trước qua cửa 96, giữ adapter cái 110 ở khoang GPU rồi bắt giá. Grommet thuộc case, không thêm vào riser. Cách cánh quạt ≥10 sau khi buộc.

## Bảy góc nhìn

| View | Hướng nhìn và nội dung |
|---|---|
| FRONT +X | Nhìn −X; +Y lên, +Z trái. Cạnh mỏng dải và phía có chốt |
| BACK −X | Nhìn +X; +Y lên, +Z phải. Cạnh đối diện, thấy bậc cao đầu cái |
| LEFT +Z | Nhìn −Z; +Y lên, +X phải. Mặt rộng dải, miệng khe, hai tai; khóa đực ở trái, chốt cái ở phải |
| RIGHT −Z | Nhìn +Z; +Y lên, +X trái. Lưng PCB và mặt kia của chân vàng; không thấy xuyên PCB vào miệng khe |
| TOP +Y | Nhìn −Y; +X xuống, +Z phải. Nhìn từ đầu cái dọc chiều dài; thân 110 theo chiều dọc ảnh, 14 ngang ảnh |
| BOTTOM −Y | Nhìn +Y; +X lên, +Z phải. Nhìn từ cạnh cắm đực; chân 89 dọc ảnh, dày 1,6 ngang ảnh; phần đầu cái phía xa có thể lộ ngoài hình chiếu đầu đực |
| 3/4 | Nhìn từ +X/−Y/+Z; thấy chân vàng, mặt shield và khe cái nâng cao. Các inset đầu cắm/uốn được phóng riêng |

## Người mới cần chú ý

1. Hai đầu khác nhau: chân vàng vào bo, khe có chốt nhận GPU. Riser không phải dây nguồn PCIe 6+2.
2. **400 là chiều dài mềm**, không phải khoảng cách thẳng giữa hai đầu khi đã lắp.
3. Khóa chia 11/71 lệch tâm; căn đúng trước khi cắm, không xoay đầu rồi ép xuống.
4. Bắt đủ hai tai và đỡ GPU bằng giá; không treo trọng lượng card lên dải mềm. Nhả chốt trước khi rút card.
5. Uốn tròn R≥15, không gập nếp hoặc kẹp giữa panel; giữ dây ngoài vùng quạt.

## Kiểm tra ảnh

Ảnh dùng **imagegen tích hợp**, một lượt tạo và một lượt sửa. Prompt tóm tắt: “PCIE RISER 400; realistic black shield; white sheet, blue dimensions; six same-scale orthographic views and one 3/4; flexible400×70×1.5, male89×12×1.6, female110×18×14; x16/164, asymmetric key, one latch, two ears100 apart, twoM3×5, centerline bendR≥15; no logos.” Lượt sửa bỏ tai đầu đực, đổi phía khóa, thêm số tiếp điểm và sửa hình uốn.

**Sai lệch còn lại:** TOP ghi nhầm `+X looking −Y`, đúng phải là **+Y looking −Y**; TOP vẫn lộ miệng khe như nhìn từ +Z, BOTTOM bỏ phần đầu cái phía xa. Hai view này chưa cùng tỷ lệ với bốn hình dài. Đầu đực ở hình dài còn hẹp hơn tỷ lệ 89/70; không dùng pixel để dựng. Gốc O còn nằm lệch trái và cao hơn đường nối, đúng là tâm seam Y0. Cụm chuyển tiếp đầu cái nhìn dài hơn bao 18; phải thu về biên trong bảng. Vị trí khóa và số vạch tiếp điểm chỉ minh họa, inset gán số 6 vào vách đầu cái thay vì rãnh đực. Ảnh không thể hiện chính xác mọi lỗ/lòng khe theo chiều sâu; ưu tiên các số và hướng chiếu trong tài liệu này.
