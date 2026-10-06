# Compact tower — thiết kế mặt lưng

Đơn vị **mm**, tọa độ toàn máy. Sheet cho cấu hình đã lắp: bo **mATX**, GPU **240 nằm ngang R0**, PSU **ATX R0 ở đáy**. Vỏ có **4 vị trí PCIe**; tháo hai nắp trên để lắp GPU, còn hai nắp dưới.

Nguồn chuẩn: [families.json](../../src/data/families.json), mục `compact_tower`; [boards.json](../../src/data/boards.json), mục `mATX`; [đặc tả họ vỏ](../families/00_families_spec.md); [vỏ compact](../families/02_compact_tower/01_case.md); [GPU 240](../families/parts/gpu_240_dual.md). Hình học cổng lấy từ `board()` và `gpu_240_dual()` trong [build_families.py](../../tools/build_families.py), mặt AC và lỗ vít nguồn lấy từ `psu()` trong [build_gaming.py](../../tools/build_gaming.py).

## Trục và mốc giữ nguyên

Gốc ở tâm thân. X từ lưng (−) đến trước (+), Y từ đáy lên nóc, Z từ hông sau khay (−) đến hông mở lắp bo (+). Nhìn **REAR X−** nghĩa là nhìn về +X: +Y lên, +Z sang phải. Hai hông của họ này đều bằng thép kín.

Thân **210 Z × 380 Y × 370 X**, biên X ±185, Y ±190, Z ±105. Mặt ngoài lưng **X = −185**. Tổng cao đặt bàn **394**, gồm chân cao 14. Bảng dưới dùng kích thước **cao Y × rộng Z**, trừ khi ghi khác.

| Linh kiện | Mốc lắp X; Y; Z | Bao hoặc mặt tiếp giáp sau khi lắp |
|---|---|---|
| Bo mATX | −49; 54; −69 | PCB 244 × 244, dày 1,6 về +Z; mặt linh kiện PCB ở Z −67,4 |
| Lá I/O | −185,2…−184,4 theo X | Y 5…163, Z −67,4…−31,4; không ép lá vào mặt X −182 |
| Quạt sau 120 | −169,5; 105; 30 | X −182…−157, Y 45…165, Z −30…90; thổi −X |
| GPU 240, R0 | −61; −26,7; −54,4 | Thân X −181…59, Y −49,2…−4,2, Z −54,4…65,6 |
| Lá GPU | X −182…−181 | Y −41,64…−1; Z −56,4…63,6 |
| PSU ATX, R0 | −112; −143; −7 | X −182…−42, Y −186…−100, Z −82…68 |
| Shroud nguồn | mặt Y −92 | Cách nóc nguồn 8; không nâng nguồn để khớp cửa |

**Không cần đổi số trong JSON hoặc dịch linh kiện.** Các kích thước gờ, vùng dập, lưới và thanh giữ dưới đây là đề xuất mới cho mặt lưng. Đây là chuẩn dựng mô phỏng, chưa phải bản vẽ gia công.

## Cửa mở, gờ và mặt gá

| Gọi số | Chi tiết | Tâm (Y; Z) | Kích thước Y × Z | Vị trí X / độ lùi và liên kết |
|---|---|---|---|---|
| 01 | Cửa I/O | 84; −49,4 | **159 × 37** | X −185 xuyên tấm; biên Y 4,5…163,5, Z −67,9…−30,9. Khe hở 0,5 mỗi cạnh quanh lá bo |
| 01 | Lá I/O của bo | 84; −49,4 | 158 × 36 | X −185,2…−184,4, dày 0,8. Lá nhô 0,2 so với mặt ngoài; rãnh kê riêng, ngàm đàn hồi, không thêm vít giả |
| 02 | Vùng quạt | 105; 30 | Bao quạt **120 × 120** | Mặt tiếp xúc quạt X −182, lùi **3**. Lưới ở phía ngoài quạt, không xuyên vào cánh |
| 02 | Trường lưới quạt | 105; 30 | **112 × 112**, góc R8 | Biên Y 49…161, Z −26…86. Cổ cửa từ X −185 đến −182; lưới X −183,2…−182,4. Giữ bốn đảo thép R6 quanh vít, khoét lỗ Ø4,5 xuyên đảo |
| 03 | Dãy PCIe, mốc JSON | −41,64; 3,5 | **81,28 × 119** | Y −82,28…−1, Z −56…63. Đây là mốc bố trí, không phải kích thước lá GPU |
| 03 | Cửa hữu dụng cho dãy | −41,64; −0,45 | **81,28 × 111,9** | Y −82,28…−1, Z −55,9…55. Mặt gá trong X −182; cửa tới thanh giữ phía +Z. Hai nắp còn lại và lá GPU che kín theo từng vị trí |
| 03 | Lá GPU hai khe | −21,32; 3,6 | **40,64 × 120** | X −182…−181, mặt ngoài lá lùi 3. Không đặt thanh ngang case qua khoảng giữa hai khe GPU |
| 03 | Nắp khe 3 | −51,05; 3,5 | **18,82 × 119** | Y −60,46…−41,64; Z −56…63. Thép 1, mặt ngoài X −182, tai vít theo bảng bên dưới |
| 03 | Nắp khe 4 | −71,37; 3,5 | **18,82 × 119** | Y −80,78…−61,96; Z −56…63. Khe hở 1,5 giữa các nắp; phần hở được gờ sau che để không nhìn xuyên vào mép tôn |
| 03 | Lưới trên hai nắp | −51,05/−71,37; −1,5 | Mỗi vùng **10,82 × 93** | Biên Z −48…45; lỗ xuyên, chừa gờ đặc. Không biến mỗi hàng lỗ thành một nắp PCIe riêng |
| 04 | Thanh giữ PCIe | −41,64; 61 | **89,28 × 13** | Y −86,28…3, Z 54,5…67,5. Thanh gấp 1,2; mặt trước X −184,4, tai ép về mặt lá X −182; bốn vít dùng chung với nắp/GPU |
| 04 | Cửa thoát cạnh PCIe | −41,64; 83 | **76 × 18**, R2 | Y −79,64…−3,64, Z 74…92. Lưới X −183,2…−182,4; không xuyên khoang nguồn. Cách thanh giữ 6,5 |
| 05 | Bao mặt nguồn / cửa danh nghĩa | −143; −7 | **86 × 150** | X −182; Y −186…−100, Z −82…68. Cửa thực trừ bốn tai giữ quanh lỗ vít, không cắt mất tai |
| 05 | Khung gá nguồn | −142; −7 | **92 × 158** | Y −188…−96, Z −86…72. Gấp từ X −185 tới mặt áp nguồn −182; dày 1,2. Bốn tai thép nối liền viền, bán kính vùng đặc 4 quanh lỗ |
| 05 | Lưới xả có sẵn trên PSU | −143; −24 | **62 × 92** | X −182…−181; Y −174…−112, Z −70…22. Thuộc mesh PSU, không thêm lưới case phủ đè |
| 06 | Mép gấp ngoài lưng | 0; 0 | Bao **380 × 210**, viền danh nghĩa 8 | Thép 1,2, cạnh R1. Gấp vào +X tới −177 ở cạnh hông/nóc; viền đáy thu hẹp cục bộ để giữ khung PSU tới Y −188 |
| 06 | Vùng dập phía trên | 89; 12 | **176 × 174**, R3 | Biên Y 1…177, Z −75…99. Bề mặt trường lùi 1,2 tại X −183,8; lá I/O giữ rãnh riêng ở mặt ngoài, quạt có cổ xuống −182 |
| 07 | Hai chân sau | −197; ±78 | Ø26 × cao 14 | Tâm X −150; chân trước ở X +150. Chân sau và trước dùng cùng thông số, không gắn chân vào tấm lưng |

Lá GPU rộng hơn dải `pcie.z` danh nghĩa: lá thật Z **−56,4…63,6**, trong khi JSON ghi **−56…63**. Giữ cả hai mốc, làm gờ đỡ theo lá thật; không thu nhỏ GPU về 119. Cửa hữu dụng và thanh giữ ở bảng là hình học mới, thay cửa cũ Z −54…53 vốn che mép lá và một phần vùng lưới.

Cửa I/O kết thúc ở Z −30,9; trường lưới quạt bắt đầu ở Z −26, nên dải thép còn **4,9**. Dùng chung vùng dập, không thêm hai khung trang trí dày chồng lên nhau tại đây. Thân quạt bắt đầu ở Z −30, cách lá I/O 1,4 và không cần dịch tâm quạt.

## Từng miệng cổng nhìn từ sau

Các cửa nhỏ dưới đây nằm trên **lá bo, lá GPU hoặc vỏ PSU**, không khoét lặp lại trên tấm case. Không thêm cổng của bo ATX trong ảnh tham chiếu vào bo mATX này.

| Cổng / lỗ | Tâm (Y; Z) | Kích thước Y × Z | Mặt ngoài, chiều sâu |
|---|---|---|---|
| USB-A trên | 142; −51,4 | Cửa lá 14,6 × 6,8; lòng 13 × 5 | Lá X −185,2; lòng cổng tới X −176,2 |
| USB-A dưới | 123; −51,4 | Như trên | Cùng cấu trúc; đúng hai cổng |
| RJ45 | 89; −51,4 | Cửa lá 17 × 15; lòng 16 × 14 | X −185,2; lòng tới −173,2; vỏ ổ 19 × 18 phía trong |
| Ba audio | 59 / 42 / 25; −51,4 | Lỗ lá Ø6,2; vành Ø6; lòng Ø3,5 | Trục X, vòng màu xanh lá / hồng / xanh lam; vòng dày 0,8 |
| DP 1 / 2 / 3 | −14; −34,4 / −10,4 / 13,6 | **5 × 16**, một góc vát | Mặt lá X −182; vỏ ổ từ −181 tới −175 |
| HDMI | −14; 37,6 | **5 × 14**, hai góc tạo hình thang | Mặt lá X −182; không thay bằng DP thứ tư |
| Thông gió lá GPU | Xem công thức dưới | Lỗ lục giác Ø2,6 qua đỉnh | X −182 xuyên lá dày 1; nằm dưới hàng cổng |
| IEC C14 PSU | −159; 42 | **18 × 23**, hai góc trên vát 3 | Mặt X −182, lòng tới −170. Ba lá tiếp điểm nằm sâu X −180…−172 |
| Công tắc PSU | −129; 42 | Vỏ **27 × 19**, rocker 22 × 14 | Viền X −182,8…−181,5; rocker nhô khoảng tới −185,1 theo model, có góc nghiêng 8°. Không phủ gờ case qua mặt nhấn |

Lưới GPU theo model: với `j = 0…16`, `i = 0…3`, tâm toàn máy `Z = −50,4 + 6,6j`, `Y = −37,64 + 3,6i + 1,8(j mod 2)`. Chỉ giữ tâm có `Y ≤ −25`. Điều kiện này theo mã dựng hiện tại, tránh thêm hàng lỗ sát cổng từ minh họa cũ.

Lưới PSU giữ nguyên model: lục giác qua đỉnh 4, bước ngang Z 5, bước Y 4,35, so le 2,5. Tâm hàng đầu Y −172; tâm cột đầu Z −68, hàng lẻ cộng 2,5. Chỉ lấy tâm Y < −114 và Z < 20. C14 nằm dưới công tắc, cả hai ở bên phải lưới. Quạt hút nguồn ở **đáy −Y**, không có quạt tròn lớn trên mặt sau.

Vùng lưới quạt và vent cạnh PCIe dùng lục giác qua hai cạnh 3, bước 4, tôn 0,8; cắt theo bao đã ghi, chừa các đảo vít. Vùng nắp PCIe dùng cùng mắt lưới. Lỗ phải nhìn xuyên, có mép tôn; không dùng chấm đen trên một mặt kín.

## Vít và cách giữ

| Nhóm | Tâm (Y; Z), trừ khi ghi đủ X;Y;Z | Loại / cách lắp |
|---|---|---|
| Quạt sau, 4 vít | (52,5; −22,5), (52,5; 82,5), (157,5; −22,5), (157,5; 82,5) | Bước **105 × 105**; lỗ thông Ø4,5; vít quạt tự tạo ren Ø5 × 10, trục +X, bắt vào khung quạt ở X −182 |
| GPU, 2 vít | (−11,16; 58,6), (−31,48; 58,6) | **#6-32 × 6**, dùng lại hai vít nắp; rãnh tai GPU rộng 3,8, không thêm bộ vít thứ hai trên thanh giữ |
| Nắp PCIe 3 và 4 | (−51,80; 58,6), (−72,12; 58,6) | **#6-32 × 6**, tai lệch 0,75 xuống so với tâm phần nắp phẳng để cùng bước với GPU |
| PSU, đúng 4 vít | (−180; −76), (−180; 62), (−106; −76), (−116; 56) | **#6-32 × 6**, lỗ thông gá Ø4; mặt áp X −182, vít hướng +X. Vít trên phải thấp hơn vít trên trái 10 và vào trong 6; không dùng hình chữ nhật đối xứng |
| Hai nắp hông, 4 vít tay | (−169; 158; ±105), (−169; −158; ±105) | **#6-32 × 6**, trục Z vào trong. Giữ tọa độ nguồn; chỉ thấy đầu/biên ở góc 3/4, không chuyển chúng sang mặt X −185 |
| Bốn chân | (±150; −197; ±78) | **M4 × 8** chìm, trục Y, một vít mỗi chân; vị trí ghi là tâm chân, không phải đầu vít trên sàn |
| Lá I/O, lưới và tấm lưng | Không có vít thao tác | I/O bằng ngàm; lưới hàn điểm vào gờ; khung gấp hàn/tán cố định. Không tạo vít trang trí giả |

Vùng đầu vít PCIe chừa Ø7 để thao tác. Thanh giữ được bốn vít PCIe giữ chung; tháo vít rồi nhấc thanh về −X. Đầu vít nguồn/quạt/PCIe nhô ra phía −X của mặt gá, không chôn vào lưới. Các vít trên linh kiện kín có sẵn không được tính thành vít tháo case.

## Mặt cắt và khoảng trống

Mặt cắt phải đọc theo X, ngoài máy ở trái, trong máy ở phải. Dùng ba lát cắt dịch vị trí Y/Z để cùng thấy I/O, GPU và PSU; không hiểu đó là một mặt phẳng duy nhất xuyên cả ba bộ phận.

- I/O: mặt sau PCB ở Z −69, mặt linh kiện ở −67,4. Lá chắn kê X −185,2…−184,4; cụm cổng nối vào bo, không treo trên case. Thân ổ về phía +X tới khoảng −167.
- GPU: mặt lá ngoài −182, mặt trong −181. Thân dài 240 từ −181 đến 59; không vẽ một khoảng hở 3 giữa lá và GPU. **3 mm** là khoảng từ mặt ngoài case −185 tới lá −182. Card dày 45, nên dù lá chiếm hai vị trí, không lắp card phụ ngay ở vị trí thứ ba. Nắp che ngoài của vị trí 3 vẫn giữ.
- PSU: mặt AC −182, thân kết thúc −42, sâu 140. Cổ khung chuyển từ −185 tới −182, tai áp đúng bốn lỗ nguồn. Cửa ngoài mở theo mặt nguồn, không có dây nối AC hay inlet case riêng.
- Quạt: khung ngoài ở −182, sau lưới ít nhất 0,4. Thân quạt dày 25, tới −157. Gờ và vít nằm ngoài quỹ đạo cánh.
- Đáy PSU: cửa hút **124 X × 124 Z**, tâm `(X;Z) = (−112;−7)`, ở sàn Y −190; lọc rút về −X. Đây là cửa đáy, không phải một cửa trên lưng. Giữ lối rút lọc dưới khung PSU, không dùng gờ lưng chặn cassette lọc hiện có.

## Vật liệu

Khung thép 1,2 và nắp rời 1,0 sơn nhám **#24262A**, roughness 0,65, metallic 0,25; cạnh ngoài R1, cổ dập R1,5 nếu không ghi riêng. Lưới **#101216**, dày 0,8. Lá I/O **#BCC2C8**, lá GPU **#B9BEC5**, đầu vít **#A8ADB4**. PSU **#16181D**, nhựa ổ/công tắc **#101216**, tiếp điểm **#B8BEC5**; USB xanh **#1F6FD1**. Chân cao su **#202329**, roughness 0,85. Không logo, RGB, chữ in hoặc biểu tượng trang trí trên panel; chữ trong sheet là chú thích bên ngoài sản phẩm.

## Đã sửa so với bản hiện tại

- Thay cảm giác tấm phẳng bằng mép gấp, vùng dập, cổ cửa và mặt gá có độ lùi xác định.
- Dùng đúng I/O mATX: hai USB-A, một RJ45, ba audio, không sao chép cụm cổng ATX của ảnh tham chiếu.
- Thay vòng chắn quạt lộ thiên bằng trường lưới có đảo vít; giữ tâm quạt và bước lỗ nguồn dữ liệu.
- Chốt đúng bốn vị trí PCIe, GPU che hai vị trí trên, hai nắp dưới; bổ sung thanh giữ và cửa xả bên cạnh.
- Mở khung PSU theo đúng mặt 150 × 86, giữ tai vít bất đối xứng, C14 và công tắc đúng vị trí trên nguồn.
- Giữ vít hông trên hai mặt hông và chân tròn theo họ compact; không kéo dài dáng thành standard tower.

## Ảnh và kiểm tra

Ảnh cạnh tài liệu: `compact_tower.png`. Tạo bằng **imagegen tích hợp**, nền trắng, một góc sau trực giao lớn, một góc 3/4, ba lát cắt theo X và các ô chi tiết. Số trong bảng là chuẩn dựng; ảnh tạo sinh dùng để đọc dáng, độ sâu và vật liệu, không đo kích thước bằng pixel.

Đã đối chiếu số từ JSON và phép cộng tọa độ bo/GPU/PSU, đặc biệt tâm I/O `(84;−49,4)`, cổng bo Z −51,4, hàng cổng GPU Y −14, mặt gá X −182 và bốn lỗ PSU. Chỉ tạo hai file của sheet này; không sửa model, mã dựng hoặc JSON.

