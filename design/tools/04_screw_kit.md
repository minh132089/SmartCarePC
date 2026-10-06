# 04 — Khay vít và đầu tuýp cọc đỡ

Ảnh: `04_screw_kit.png`. Đơn vị **mm**; số trong tài liệu này là nguồn chuẩn khi dựng. Đối chiếu `00_assembly_needs.md`, `../families/00_families_spec.md`, `../gaming/00_blueprint.md` và `../gaming/07_ssd.md`. Kích thước đầu vít, khay và tuýp dưới đây là lựa chọn cho mô hình, không phải bản vẽ gia công.

## Khay và hệ tọa độ

Bao đóng nắp **150 × 100 × 20**, theo X × Z × Y. Gốc O ở tâm mặt đáy ngoài; **+X dọc cạnh dài sang phải, +Y lên trên, +Z về phía chốt trước**. FRONT/BACK nhìn từ +Z/−Z; LEFT/RIGHT từ −X/+X; TOP/BOTTOM từ +Y/−Y.

| Bộ phận | Số lượng | Vị trí và kích thước |
|---|---:|---|
| Thân khay | 1 | X=−75…75, Z=−50…50, Y=0…18,5; đáy và thành dày 1,5; góc ngoài R3 |
| Vách ngăn dọc | 2 | Tâm X=±24,75; dày 1,5; Z=−48,5…48,5; Y=1,5…18,5 |
| Vách ngăn ngang | 2 | Tâm Z=±16,25; dày 1,5; X=−73,5…73,5; Y=1,5…18,5 |
| Nắp trong | 1 | X=±75, Z=±50, Y=18,5…20; dày 1,5; đóng sát đỉnh vách để giữ vít M2 |
| Bản lề | 2 | Trục song song X; tâm (−48;18,5;−48,5) và (48;18,5;−48,5); mỗi cụm dài 12, Ø3; mở nắp ra sau 105° |
| Chốt trước | 1 | Tâm (0;14,5;49,25); bao 16 × 8 × 1,5 theo X/Y/Z; nằm trong bao khay |

Chín ô có lòng **48 × 31,5**, sâu 17. Tâm ô theo X/Z: hàng sau **1=(−49;−32,75), 2=(0;−32,75), 3=(49;−32,75)**; hàng giữa **4=(−49;0), 5=(0;0), 6=(49;0)**; hàng trước **7=(−49;32,75), 8=(0;32,75), 9=(49;32,75)**. Đáy lòng Y=1,5. Số ô đặt ở góc trái phía sau từng ô, không che phần cứng.

## Đồ trong từng ô

Chiều dài vít tính **dưới đầu**. Kích thước ren theo yêu cầu dự án; các đường kính đầu và chiều dài chân ren chưa có trong nguồn được chọn rõ dưới đây. Tâm nhóm nằm tại X/Z của ô tương ứng; không để chi tiết xuyên vách.

| Ô | Chi tiết, số lượng | Hình học dựng |
|---|---|---|
| 1 | **9 cọc bo M3** | Đồng thau; thân lục giác cao **6,5**, ngang hai mặt đối diện **5**; ren ngoài M3 dài 3,5 ở đáy, lỗ ren M3 đồng trục sâu 5 ở đỉnh; tổng dài 10. Không có lỗ bên hông |
| 2 | **9 vít bo M3 × 5** | Đầu tròn chữ thập Ø5,5 cao 2; thân ren Ø3 dài 5 |
| 3 | **11 vít #6-32 UNC × 6** | Ren Ø3,505; đầu có vành Ø7 cao 2,5, chữ thập; 4 vít nguồn + 7 vít PCIe. Hai vít nắp khe được dùng lại cho GPU, không cộng thêm |
| 4 | **16 vít quạt tự tạo ren 5 × 10** | Đầu chữ thập Ø8 cao 2,5; ren thưa Ø5 dài 10, đầu thuôn; dành cho lỗ nhựa khung quạt |
| 5 | **8 vít két M3 × 6** | Đầu chữ thập Ø5,5 cao 2; thân ren Ø3 dài 6 |
| 6 | **8 vít két M3 × 30** | Cùng đầu với ô 5; thân ren Ø3 dài 30; dựng dài khác biệt rõ với vít ngắn |
| 7 | **1 vít M2 × 2,5 + 1 cọc M.2** | Vít đầu thấp Ø3,5 cao 0,7. Cọc tròn Ø4, thân cao **3**, lỗ M2 ở đỉnh; chọn chân ren M2 dài 2 ở đáy, tổng dài 5 |
| 8 | **6 vít tay #6-32** | 4 vít dài 8 cho kính, 2 vít dài 6 cho nắp kín; đầu khía Ø9 cao 5, rãnh chữ thập; ren Ø3,505 |
| 9 | **1 đầu tuýp HEX 5** | Tổng dài 25; đầu trụ Ø8 dài 13, cổ chuyển dài 2, chuôi lục giác 6,35 dài 10. Miệng âm lục giác ngang hai mặt **5**, sâu 7, mở ở đầu −X |

Cách xếp tham chiếu: tất cả chi tiết nằm ngang, trục dài +X; tâm chiều dài tại vị trí lưới sau, tính tương đối với tâm ô. Ô 1 và 2: 3 × 3, bước X/Z=13/9; ô 3: lưới 4 × 3 bước 11/9, bỏ góc cuối; ô 4: 4 × 4 bước 11,5/7,5, hai hàng xen kẽ cao thêm 4 để tránh chồng đầu Ø8. Ô 5 và 6: hai lớp, mỗi lớp bốn vít; X=0, Z=−10,5/−3,5/3,5/10,5, trục ở Y=4,25 và 9,75. Ô 7: tâm X=−8 và +8; ô 8: 3 × 2 bước 15/13; ô 9: tâm X/Z của ô, trục Y=5,5. Với các ô còn lại, trục Y=1,5 + nửa đường kính bao lớn nhất; riêng cọc lục giác dùng nửa đường kính qua đỉnh 2,887. Những hình chiếu lặp lại cùng bộ đồ, không cộng số lượng.

Tổng: **59 vít + 10 cọc + 1 tuýp = 70 món**, trong một khay có nắp. Đây là bộ minh họa theo Gaming/AIO 240; AIO 360 cần 12 vít mỗi loại két. Đệm két 16 chiếc và đệm kính 4 chiếc thuộc cụm PC hiện có, không thêm vào khay của yêu cầu này. Khi lắp cảnh, chuyển món từ khay tới mốc lắp hoặc dùng nó làm bản thay thế; không nhân đôi cọc/vít có sẵn trên bo và case. Hai vít nắp kín được giữ liền nắp khi đã lắp.

## Vật liệu và màu

Khay, vách, nắp, bản lề và chốt bằng nhựa trong `#E5EDF0`, transmission 0,92, roughness 0,16, IOR 1,49. Cọc bo đồng thau `#C6A35C`; vít bo, vít nguồn, cọc/vít M.2 và tuýp thép mạ `#AEB4BC`, metallic 1, roughness 0,28. Vít quạt, vít két và vít tay đen `#25272A`, roughness 0,45. Số ô xanh đậm `#24496B`; đường kích thước xanh `#2588D9` chỉ thuộc tờ tham chiếu.

## Người mới cần biết

- Kiểm tra cọc bo ở bước 1, trước khi đặt bo. Chỉ đặt cọc dưới đúng lỗ bắt vít; tuýp 5 mm phải ôm phần lục giác, không tì vào ren.
- Bước 2 dùng M3 × 5 cho bo; bước 3 dùng vít M2 × 2,5 giữ SSD. Cọc SSD cao 3, không thay bằng cọc bo cao 6,5.
- Bước 5 dùng #6-32 cho nguồn và PCIe. Nếu vít không vào nhẹ bằng tay, kiểm tra lại loại ren; không ép vít M3 vào lỗ #6-32.
- Bước 6 phân biệt vít quạt tự ren với vít két M3. Vít dài 30 đi qua khung quạt; vít ngắn 6 giữ két lên giá. Dùng đúng đệm và giá của mô hình, tránh đầu vít chạm đường nước.
- Lắp khi máy chưa cắm điện; nhặt hết vít rơi trong case trước khi bật nguồn. Đỡ kính khi tháo vít tay và giữ đệm cao su giữa kính với kim loại.

## Sai khác của ảnh và cách tạo

Ảnh tạo bằng **imagegen tích hợp**, đã sửa đúng một lần. Đủ FRONT/BACK/LEFT/RIGHT/TOP/BOTTOM, góc 3/4 và ô tay thao tác. Tuy nhiên, số món nhìn thấy chưa khớp: ô 1 chỉ thấy rõ sáu miệng cọc, không phải chín; ô 3, 5, 6, 8 cũng thiếu hoặc che khuất món. Dùng số lượng và lưới xếp trong bảng, không đếm theo ảnh. LEFT/RIGHT bị thu nhỏ so với FRONT; sáu hình chưa cùng tỷ lệ. BOTTOM bỏ phần cứng để thấy đáy nên không phải phép nhìn xuyên đầy đủ.

Đường đo 6,5 ở chi tiết cọc còn kéo xuống chân ren; đúng phải đo riêng thân lục giác. Chân ren dưới cọc M.2 chưa thể hiện rõ. Ô thao tác vẫn cho cảm giác tuýp tì lên đỉnh cọc; khi dựng phải lồng miệng tuýp xuống ôm các mặt lục giác. Ren, góc mở nắp và tỉ lệ các mẫu phóng to chỉ dùng nhận dạng.

<details>
<summary>Prompt cuối, nguyên văn lần sửa bằng imagegen tích hợp</summary>

```text
Correct this screw-kit technical sheet, preserving its layout, typography, nine labels, all SIX named orthographic views, dimensions, materials, 3/4 open tray and IN USE panel. This is the single final correction pass.
CRITICAL GEOMETRY: Detail 1 brass motherboard standoff must be a HEX BODY, exactly 6.5 high and 5 across flats, with female M3 threaded bore at TOP axial end and male M3 threaded stud at BOTTOM axial end; NO radial hole on any hex flat. Show slight 3/4 angle so the top bore is visible. In compartment 1 show EXACTLY NINE of these in a 3x3 arrangement, female bore up, all nine clearly countable, no extra studs protruding above. Detail 7 M.2 standoff must be a SILVER CYLINDER diameter 4, height3, female M2 TOP bore and tiny male M2 stud underneath, not a tall hex motherboard standoff. Tiny screw remains M2x2.5.
COUNTS: in the clear TOP view use neat arrangements so counts agree: 1 nine brass standoffs (3x3); 2 nine board screws (3x3); 3 eleven PSU screws (4+4+3); 4 sixteen fan screws (4x4); 5 eight M3x6 screws (4x2); 6 eight M3x30 screws in parallel rows with axes horizontal; 7 one M2 screw and one standoff; 8 six thumbscrews (3x2); 9 one socket. Perspective view same counts, objects may overlap slightly. All contents fully inside cells.
ORTHOGRAPHIC SCALE: TOP and BOTTOM width150 and depth100, same pixel scale. FRONT and BACK width150 and height20 at this same scale. LEFT and RIGHT width100 and height20 at this same scale (increase side-view widths to 2/3 of front width). Move drawing grid as needed to avoid overlap. No perspective in these six views.
IN USE: the silver socket mouth must be slid DOWN OVER the brass hex standoff's upper hex body, visibly engaging its flats. Hand holds driver whose axis is collinear with socket and standoff; lower male stud enters bare case tray, no exposed upward stud between socket and brass body. Standoff is being installed before motherboard.
Everything else stays. No logos. Keep short English labels readable.
```

</details>
