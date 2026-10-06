# standard_tower — Thiết kế lưng case

Ảnh: [standard_tower.png](standard_tower.png). Đơn vị mm. Đây là thiết kế mô phỏng để dựng 3D; dùng số trong tài liệu, không đo từ pixel.

## Mốc giữ nguyên và thay đổi cần làm

Nguồn: `src/data/families.json`, mục `standard_tower`; `design/families/00_families_spec.md`; `design/families/01_standard_tower/01_case.md`; các hàm motherboard, gpu, psu trong `tools/build_gaming.py`; phần dựng lưng trong `tools/build_families.py`. Đường dẫn tính từ gốc dự án. `boards.json` chỉ có mATX/ITX, không có ATX; I/O ATX lấy từ mesh Gaming và `design/gaming/11_rear_io.md`, không thay bằng I/O mATX.

X từ lưng đến trước, Y từ đáy lên nóc, Z từ hông kín đến kính. Nhìn từ −X vào +X: +Y lên trên, +Z sang phải. Mọi tâm trong bảng là **(Y; Z)**; kích thước mặt lưng là **cao Y × rộng Z**. Độ lùi dương theo +X tính từ X = −215.

| Phần | Mốc nguồn, giữ nguyên |
|---|---|
| Thân | 230 Z × 460 Y × 430 X; X ±215, Y ±230, Z ±115; cao cả chân 478 |
| Bo ATX | B = (−79; 45; −83); PCB bắt đầu X = −201; mặt linh kiện Z = −81,4 |
| Tản tháp | `tower_air_155`; bao X −105…−15, Y 44…164, Z −73,8…81,2 |
| GPU ngang | A = (−71; −9,7; −68,4), R0; thân X −211…69, Y −37,2…17,8, Z −68,4…56,6 |
| Quạt sau | 120 × 120 × 25; tâm (−199,5; 128; 28); mặt gá X = −212; thổi −X |
| PSU ATX | Tâm (−142; −177; −15), R0; 140 X × 86 Y × 150 Z; X −212…−72, Y −220…−134, Z −90…60; hút −Y, xả −X |
| PCIe | 7 hàng ngang; mép trên 21, bước 20,32; Z −70…49 |

**Xung đột cần sửa khi triển khai:** brief yêu cầu GPU 280 có lá ba khe, nhưng `pcie.removed = 2`, tài liệu họ cũ và hàm `gpu()` hiện dùng lá hai khe 40,64 mm. Sheet này chọn **lá ba khe 60,96 mm**, ba vít, bốn nắp còn lại. Cần đổi riêng `standard_tower.pcie.removed` thành 3 và dựng biến thể lá GPU ba khe, kéo mép dưới từ Y = −19,64 xuống −39,96. Giữ nguyên gốc GPU, thân, PCB và bốn cổng; không dịch card. Không tự sửa GPU dùng chung cho các họ khác. Hai tệp bàn giao này chưa thay đổi mã hay dữ liệu.

## Cửa, gờ và tấm lưng

Những chi tiết gấp, hốc và lưới dưới đây là đề xuất mới; không phải số đã tồn tại trong JSON. Tấm lưng cố định dày 1,2, không thêm vít tháo cả tấm.

| Số | Phần / tâm (Y; Z) | Kích thước, biên | Mặt X / liên kết phía trong |
|---|---|---|---|
| 01 | Khung ngoài (0; 0) | 460 × 230; viền gấp rộng 8, R ngoài 1, R uốn trong 1,2 | Mặt ngoài −215; gờ quay vào đến −207; ngắt gờ tại cửa và vùng gá |
| 02 | Cửa I/O (106; −63,4) | 159 × 37; Y 26,5…185,5; Z −81,9…−44,9 | Gá đúng lá chắn ATX, không khoét theo mép PCB |
| 02a | Lá chắn I/O (106; −63,4) | 158 × 36; Y 27…185; Z −81,4…−45,4 | X −215,2…−214,4, nhô 0,2; thân cổng vào đến khoảng X −197 |
| 02b | Hốc quanh I/O (106; −63,4) | 172 × 49, R3 | Mặt hốc −214,4, lùi 0,6; rãnh đỡ riêng áp mặt trong lá. Không đẩy lá lùi theo hốc |
| 03 | Cửa thoáng quạt (128; 28) | 116 × 116, R8; Y 70…186; Z −30…86 | Lưới dày 0,8 ở X −214…−213,2; sau đó tới khung quạt −212 |
| 03a | Vùng dập quạt (128; 28) | 128 × 128, R6; Y 64…192; Z −36…92 | Gờ chuyển từ −215 tới mặt gá −212, sâu 3; không lấn vào khung quạt |
| 04 | Bao dãy PCIe (−50,12; −10,5) | 142,24 × 119; Y −121,24…21; Z −70…49 | Đây là bao bố trí, không phải một cửa cắt xuyên thanh giữ |
| 04a | Lá GPU ba khe (−9,48; −10,4) | 60,96 × 120; Y −39,96…21; Z −70,4…49,6 | X −212…−211, lùi 3 ở mặt ngoài; cổ gấp nối về lưng, tiếp giáp card ở −211 |
| 04b | Bốn nắp dưới | Mỗi nắp 18,82 × 119; bảng riêng bên dưới | X −212…−211; mỗi nắp gấp mép, đục lưới thật |
| 04c | Thanh giữ (−50,12; 45) | 150,24 × 8; Y −125,24…25; Z 41…49 | Gờ ren áp X = −211; bảy vị trí vít trục X, đầu hướng −X; để lộ đầu vít |
| 05 | Cửa thoáng cạnh PCIe (−50; 80) | 126 × 24, R3; Y −113…13; Z 68…92 | Lưới X −214…−213,2; phía trong là khoang card, không có ổ cắm hay đường dây xuyên qua |
| 06 | Bao PSU (−177; −15) | 86 × 150 | Mặt PSU X = −212, lùi 3; giữ kích thước và hướng nguồn |
| 06a | Gờ gá PSU (−177; −15) | 98 × 162; Y −226…−128; Z −96…66; R3 | Gấp từ −215 tới −212; có bốn bệ vít theo mesh nguồn, không che shroud Y = −129 |
| 06b | Cửa lộ mặt PSU (−176,5; −16,75) | 67 × 126,5; Y −210…−143; Z −80…46,5 | Giữ cửa hiện tại, bo R1; chừa đảo thép R5 quanh tâm vít (−150; 48). Đây là cửa nhìn/xả khí, không dùng làm cửa luồn cả nguồn |
| 07 | Lưới xả trên chính PSU (−177; −32) | 62 × 92; Y −208…−146; Z −78…14 | Tôn nguồn X −212…−211; không chồng thêm lưới case lên lưới này |
| 08 | Miệng C14 của PSU (−193; 34) | 18 × 23, hai góc trên vát 3 | Miệng X = −212, lòng tới −200; ba lá tiếp xúc nằm sâu, không vẽ ổ AC rời trên case |
| 09 | Khung rocker PSU (−163; 34) | 27 × 19; mặt bập bênh 22 × 14 | Khung X −212,8…−211,5; mặt nhô xấp xỉ −215, có xoay 8° như mesh; ở trên C14 |
| 10 | Hai chân sau (−239; −85), (−239; 85) | 18 Y × 28 Z × 42 X | Tâm X = −175; chân trước X = 175, cùng Y/Z; đáy chân −248 |

Cửa GPU xuyên hữu dụng: Y −39,46…20,5, Z −68…39, tâm (−9,48; −14,5), 59,96 × 107. Lá GPU phủ mép 0,5 theo Y và tối thiểu 2,4 theo Z. Mép Z 39…49 dùng để bắt vít, không cắt mất. Không giữ các gân ngang chia ba hàng phía sau lá GPU.

| Nắp / hàng trong case | Tâm (Y; Z) | Biên Y của nắp | Cửa hữu dụng sau nắp |
|---|---|---|---|
| 4 | (−49,37; −10,5) | −58,78…−39,96 | 17,82 × 107, tâm (−49,37; −14,5) |
| 5 | (−69,69; −10,5) | −79,10…−60,28 | 17,82 × 107, tâm (−69,69; −14,5) |
| 6 | (−90,01; −10,5) | −99,42…−80,60 | 17,82 × 107, tâm (−90,01; −14,5) |
| 7 | (−110,33; −10,5) | −119,74…−100,92 | 17,82 × 107, tâm (−110,33; −14,5) |

Mỗi nắp có trường lưới 10 × 99, cùng tâm nắp. Lỗ lục giác rộng qua hai cạnh 2,4, bước 3,6. Lưới quạt và cửa 05: lỗ lục giác qua hai cạnh 3, bước 4; giữ vùng đặc R5 quanh vít. Lưới PSU giữ mesh hiện có: lục giác bán kính đỉnh 2, bước ngang Z 5, bước Y 4,35, lệch hàng 2,5. Không phủ màu đen lên tấm kín để giả lỗ.

## Các cổng phải nhìn thấy

Các lỗ ở bảng này thuộc lá chắn hoặc linh kiện, không khoét thêm lần nữa trên tấm lưng case. I/O ATX dùng phép đổi **Y = 45 + y cục bộ; Z = −81,4 + h**. Mặt lá X = −215,2; giữ lòng ổ và lưỡi tiếp xúc của model.

| Cổng I/O | Tâm (Y; Z) | Miệng / lỗ trên lá Y × Z |
|---|---|---|
| Hai nút BIOS/CMOS | (176; −71,4), (176; −56,4) | Nút Ø4, lỗ Ø4,6; CMOS lùi thêm 0,8 |
| HDMI | (160; −71,4) | Miệng 14 × 4,6; lỗ 15 × 5,6, hình thang |
| DP | (160; −56,4) | Miệng 16,2 × 4,8; lỗ 17,2 × 5,8, vát một góc |
| USB-A đen, 2 ổ | (139; −71,4), (139; −56,4) | Miệng 13 × 5; lỗ 14,6 × 6,8 |
| USB-A xanh, 2 ổ | (119; −71,4), (119; −56,4) | Như hàng trên |
| USB-A đỏ, 2 ổ | (99; −71,4), (99; −56,4) | Như hàng trên |
| RJ45 | (77; −69,4) | Miệng 16 × 14; lỗ 17 × 15; rãnh ngàm 3 × 4 tâm (84,5; −69,4) |
| USB-C | (77; −52,4) | Miệng 9 × 3,5; lỗ 10 × 4,5, R2,2 |
| Hai anten RP-SMA | (57; −71,4), (57; −56,4) | Lỗ Ø6,5; ren Ø6,35; đai ốc rộng khoảng 8; giữ chân giữa |
| Audio xanh / hồng | (39; −74,4), (39; −62,4) | Miệng Ø3,5, vành Ø6, lỗ Ø6,2 |
| Optical | (39; −51,4) | Miệng 9 × 9; lỗ 10 × 10 |

RJ45 có tám tiếp điểm; LED tâm (84; −74,4) và (84; −64,4), mỗi ô 1,8 × 1,8. Cổng nhỏ và thân ổ giữ nguyên theo model, không thêm tên hoặc logo lên lá chắn.

GPU có 3 DP tại **(8; −48,4), (8; −24,4), (8; −0,4)**, mỗi lỗ bao 5 Y × 16 Z, vát một góc. HDMI tại **(8; 23,6)**, lỗ bao 5 × 14, hình thang. Đây là kích thước biên cắt từ hàm `gpu()`; miệng trong nhỏ hơn theo hệ số Y 0,75, Z 0,9. Mặt lá X −212, ổ kéo vào tới −205. Vùng thông gió mới của lá ba khe: tâm (−19; −14,5), 30 × 99, lỗ lục giác như nắp PCIe; giữ thép quanh bốn cổng và ba tai vít.

## Vít và cách lắp

| Cụm | Tâm và mặt gá | Loại / số |
|---|---|---|
| Quạt sau | (75,5; −24,5), (75,5; 80,5), (180,5; −24,5), (180,5; 80,5); mặt gá X −212 | 4 vít tự tạo ren Ø5 × 10; lỗ case Ø5,2, bước 105; đầu hướng −X |
| GPU ba khe | Z = 44,6; Y = 10,84 / −9,48 / −29,80; lá X −212…−211 | 3 vít #6-32 × 6; rãnh tai hở rộng 3,8 theo Y, Z 42,6…50,6; vít thứ ba là bổ sung theo brief |
| Bốn nắp | Z = 44,6; Y = −50,12 / −70,44 / −90,76 / −111,08 | 4 vít #6-32 × 6; lỗ thoát Ø3,8 trên tai; bệ ren ở thanh giữ |
| PSU | (−214; −84), (−214; 54), (−140; −84), (−150; 48) | 4 vít #6-32 × 6; lỗ gờ Ø3,8; ống bậc hiệu dụng 3 từ −215 tới −212 để khớp đầu vít mesh |
| Hông kín | XYZ = (−199; ±198; −115) | 2 vít tay #6-32 × 6, trục Z, đầu −Z; chỉ thấy nghiêng từ sau |
| Kính | XYZ = (±190; ±203; 115) | 4 vít tay #6-32 × 8 có đệm, trục Z; chỉ cặp X −190 gần lưng |
| Chân | XYZ = (±175; −239; ±85), tâm hình học; vít đi từ dưới theo +Y, đầu chìm trong chân | 4 M4 × 8, một vít/chân |
| I/O, lưới, khung | Lá I/O dùng ngàm đàn hồi; lưới hàn điểm; khung hàn/tán cố định | Không thêm vít tháo lên vùng I/O hoặc viền lưng |

Đầu vít GPU ở phía −X của lá, không quay ngược vào trong. Lỗ ren trên thanh giữ đồng trục với bảy vít; tâm vít nắp lệch tâm tấm 0,75 theo −Y do quy ước tai giữ. Bố trí PSU bất đối xứng lấy từ mesh, không thay bằng bốn góc đối xứng. Không thêm vít tay hướng −X trên lưng chỉ để giống ảnh tham chiếu; vít hông vẫn ở đúng mặt hông.

## Vật liệu, chiều sâu và luồng khí

Thép khung `#24262A`, dày 1,2; nắp rời 1,0; roughness 0,65, metallic 0,25. Lưới case `#101216`, dày 0,8. Lá I/O `#BCC2C8`, dày 0,8; lá GPU `#B9BEC5`, dày 1; đầu vít `#A8ADB4`. PSU `#16181D`, lòng tối `#0B0C0E`; nhựa cổng `#101216`; tiếp điểm `#C6A565`. Chân/cao su `#202329`; kính `#B8C4CA`, dày 4. Không logo, chữ in, đèn RGB; ký hiệu công tắc cũng bỏ khỏi bản trình bày theo brief.

Mặt cắt phải thể hiện ba bậc độc lập: I/O nhô 0,2; GPU lùi 3 và có lá dày 1; PSU lùi 3. Lưới quạt cách mặt khung quạt 1,2. Không ép mọi cổng về X = −215. Thân ổ I/O quay vào trong tới bo, lá GPU nối vào card ngang dài 280, nguồn nối gờ gá ở X = −212. Tản tháp xả về quạt sau; cửa 05 thông khoang card; PSU hút qua đáy và xả qua lưới riêng. Không đặt rotor lớn nhìn thẳng ra lưng PSU, không thêm lỗ C14 phụ hay quạt trên nóc.

## Đã sửa so với bản hiện tại

- Thêm viền gấp, hốc dập và mặt kê có chiều sâu; giữ ba cao độ X của linh kiện.
- Thay lồng quạt tròn lộ thiên bằng cửa vuông bo góc, lưới xuyên và bốn vít đúng tâm.
- Lá GPU chiếm ba khe theo brief, để lộ 3 DP + 1 HDMI; bốn nắp còn lại và thanh giữ có vít quay ra ngoài.
- Thêm cửa lưới cạnh PCIe, gờ gá nguồn và bốn vít bất đối xứng; C14 ở dưới rocker theo PSU thật.
- Giữ vị trí vít hông, chân, bo, nguồn và quạt; bỏ logo/chữ trang trí khỏi panel.

## Ảnh tham chiếu và prompt

Tạo bằng imagegen tích hợp, một lượt tạo và hai lượt sửa. Ảnh là tham chiếu hình dáng, không phải bản CAD hay bản khoan sản xuất. Các chi tiết cổng nhỏ, vít và đường dóng phải kiểm tra theo bảng trên khi dựng model.

Kiểm tra ảnh cuối: có hình lưng, góc 3/4, mặt cắt, lá GPU và bốn nắp dưới, lưới quạt, nguồn với rocker trên C14. Ảnh vẫn giản lược sai cụm I/O: thiếu hình RJ45 và jack hồng rõ ràng, màu/thứ tự USB chưa khớp bảng; không dùng cụm cổng trong ảnh làm mẫu sao chép. Một số đầu đường dóng PCIe chưa chạm đúng mép cuối, bốn vít PSU và vít viền chưa khớp hoàn toàn tọa độ. Nhãn 06 hiện ở mặt cắt; cửa PSU trên hình lưng thiếu đường dóng cao 67. Những phần này phải dựng theo bảng số, không theo nét ảnh. Bảng kích thước ghi đúng lá GPU 60,96 × 120 và bao PCIe 142,24 × 119.

Prompt thiết kế chính: “One white-background standard tower rear-panel reference sheet, large orthographic rear view plus matching three-quarter rear and stepped section; body 230 × 460 × 430, rear X −215; recessed graphite folded steel frame; I/O 159 × 37 at (106, −63.4), shield X −215.2…−214.4; rear fan 120 at (128, 28), 116-square mesh vent, 105 screw pitch; seven PCIe pitches20.32 startingY21, three-slot GPU plate60.96 ×120 with3DP+1HDMI, four blank covers, retention rail; side vent126 ×24 at(−50,80); bottom ATX PSU at(−177,−15), faceX−212, asymmetric four screws, honeycomb left, rocker above C14 right; exact English dimensions and numbered leaders, realistic holes and folded shoulders, no logos or panel text. Preserve internal coordinates; label proposed GPU bracket revision.”
