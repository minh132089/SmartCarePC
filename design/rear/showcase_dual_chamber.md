# Showcase hai khoang: mặt lưng đã lắp linh kiện

Ảnh: [showcase_dual_chamber.png](showcase_dual_chamber.png). Đơn vị mm. Đây là đặc tả dựng mô hình, không phải bản vẽ gia công. Tọa độ trong bảng quyết định hình học; không đo từ pixel ảnh.

## Mốc giữ nguyên và thay đổi phải làm rõ

Nguồn chính: [families.json](../../src/data/families.json), họ `showcase_dual_chamber`; [đặc tả họ 03](../families/03_showcase_dual_chamber/01_case.md); [đặc tả chung](../families/00_families_spec.md). `boards.json` chỉ có mATX/ITX, nên I/O ATX lấy từ `motherboard()` trong [build_gaming.py](../../tools/build_gaming.py) và [tờ I/O ATX](../gaming/11_rear_io.md). Cổng PSU/GPU cũng lấy từ mã dựng linh kiện đó. Không sao chép tọa độ đặt linh kiện của bộ Gaming.

Thân rộng Z 300 × cao Y 450 × sâu X 450; gốc ở tâm thân. Mặt lưng X = −225; Y ±225; Z ±150. Nhìn từ X− vào +X: Y+ lên trên, Z+ sang phải. Khoang PSU ở trái, khoang kính ở phải. Tổng cao có chân là 470.

| Linh kiện | Mốc giữ nguyên và bao sau quay |
|---|---|
| Bo ATX | B = (−89; 25; −30). Khay mặt trước Z −36,5; mặt sau −37,7. PCB X −211…33, Y −127,5…177,5. |
| GPU 280 | A = (−81; −111; 86), R−(x;y;z) = (x;z;−y). Thân X −221…59, Y −111…14, Z 58,5…113,5. Quạt card hướng +Z. |
| Riser | Đầu bo (−123; −5; −19,6); đầu card (−123; −115,2; 61,3). Cáp dài tâm 400, bán kính uốn ≥15; không dịch hai đầu để khớp ảnh. |
| PSU ATX | Tâm (−152; −95; −94), R+(x;y;z) = (x;−z;y). Thân X −222…−82, Y −170…−20, Z −137…−51. Hút từ hông −Z, xả −X. |
| Quạt sau F6 | 120 × 120 × 25; tâm (−209,5; 97; 77), bao X −222…−197. `blow: -x`, `flip: true`. |

**Mâu thuẫn cần sửa khi triển khai:** brief yêu cầu GPU 280 có bracket ba khe. Mã hiện tại tạo lá hai khe rộng 40,64, hai tai vít; `pcie.removed` đang là 2, và tờ case cũ giữ một nắp. Sheet này chọn bracket **ba khe rộng 60,96**, chiếm cả ba vị trí, đúng yêu cầu mới. Cần đổi `removed: 2 → 3`, mở rộng riêng lá/tai GPU và làm lại cassette vỏ. Không đổi `count: 3`, vị trí GPU, thân tản, cổng hoặc đầu riser. Chưa áp dụng những thay đổi đó vào JSON, Python hoặc GLB trong tác vụ này.

`flip` là quay 180° quanh trục X để đổi góc ra dây, theo `FamilyRig.tsx`; không đảo chiều hút/xả. Ảnh phải giữ gió −X. Bỏ chữ/logo trên cả lá I/O và rocker trong sheet; hình dạng cổng, công tắc vẫn giữ nguyên.

## Cửa và chi tiết trên mặt lưng

Kích thước dưới đây luôn ghi **cao Y × rộng Z**, trừ khi chỉ rõ khác. Độ sâu dương đi vào máy theo +X, tính từ mặt lưng −225. Những gờ, lưới phụ, thanh giữ và hốc mới là đề xuất thiết kế; các mốc lắp ở bảng trên không thay đổi.

| Số trên ảnh / phần | Tâm (Y; Z) | Kích thước và giới hạn | Mặt theo X / thuộc linh kiện |
|---|---|---|---|
| 01 Khung lưng | (0; 0) | 450 × 300; tôn 1,2; gờ ngoài rộng 8; góc ngoài R2; mép gấp sâu 12 | Tôn X −225…−223,8; mép gấp tới −213. Khung cố định, không thêm vít tháo giả. |
| 02 Cửa I/O | (86; −10,4) | **159 × 37**; Y 6,5…165,5; Z −28,9…8,1 | Cửa xuyên khung; lá chắn thực ở X −225,2…−224,4. Thuộc bo ATX. |
| Lá I/O | (86; −10,4) | **158 × 36**, dày 0,8; Y 7…165; Z −28,4…7,6 | Mặt ngoài −225,2 nhô 0,2 so với khung; không lùi cả bo theo hốc trang trí. Lá có ngàm lò xo, không thêm vít xuyên bo. |
| Hốc chung I/O + quạt | (86; 54,05) | Bao hợp nhất 171 × 177,9; Y 0,5…171,5, Z −34,9…143; góc R3 | Nền hốc sâu 2: mặt ngoài −223, mặt trong −221,8. Dừng nền tại các cửa. Gờ/ngàm bậc riêng đưa lá I/O về đúng −225,2. |
| 03 Cửa quạt | (97; 77) | **Ø116**, bao Y 39…155, Z 19…135; khung quạt 120 × 120 | Mặt gá tiếp xúc −222, sâu 3. Lưới dày 0,8 tại X −223,2…−222,4; cách mặt quạt 0,4. |
| Trường gá quạt | (97; 77) | 126 × 132; Y 34…160, Z 11…143; góc R3 | Hợp vào hốc chung, không chồng hai lớp tôn ở chỗ giao. Bốn đệm cục bộ đưa mặt gá trong tới −222. |
| 04 Cassette GPU: bao ba khe | (−53; 85,78) | **120 × 60,96**; Y −113…7, Z 55,3…116,26; bước 20,32 theo Z | Lá GPU X −222…−221; gờ kê tiếp xúc X −221. Mặt lá lùi 3. Ba vị trí cùng được lá GPU che kín. |
| Cửa thông cassette | (−56; 85,78) | **110 × 56,96**; Y −111…−1, Z 57,3…114,26 | Xuyên hốc đến mặt kê −221. Không đặt gân cố định ngang qua cổng. Gờ dưới và tai trên giữ lá. |
| 09 Thanh giữ GPU | (13; 85,78) | 8 × 73; Y 9…17, Z 49,28…122,28; dày 1,2 | Mặt ngoài −223; môi gấp vươn tới −220,8 và đè tai lá. Chừa ba đầu vít lá tại Y 2. |
| 05 Cửa PSU đứng | (−95; −94) | **150 × 86**; Y −170…−20, Z −137…−51; giữ lại bốn tai tròn R5 quanh vít | Cửa cho mặt nguồn ở −222. Không dùng cửa cũ 130 × 66 che mép nguồn/công tắc. |
| Gá PSU | (−95; −94) | Bao ngoài **162 × 98**; Y −176…−14, Z −143…−45; góc R2 | Bậc gá sâu 3, mặt tiếp xúc nguồn −222. Bốn tai liên tục với gờ. Cửa 150 × 86 là bao, trừ bốn tai, không phải lỗ chữ nhật cắt rời tai. |
| 06 Lưới khoang phụ | (100; −94) | **130 × 60**; Y 35…165, Z −124…−64; góc R3 | Cửa xuyên; lưới X −223,2…−222,4, viền hàn rộng 5 ở mặt trong. Thoát khí khoang dây phía sau khay. |
| 06 Lưới cạnh GPU | (−60; 15) | **100 × 26**; Y −110…−10, Z 2…28; góc R3 | Cùng mặt lưới trên. Thông khoang chính, cách bracket 27,3 theo Z. |
| 07 Hai vít tay hông thép | (190; −152), (−190; −152) | Đầu Ø9 × 4, trục Z, tâm X −209 | Vít #6-32 × 6 giữ nắp −Z. Nằm trên mép hông, không giả thành vít xuyên mặt lưng. |
| 08 Chân sau | (−235; −112), (−235; 112) | 20 Y × 36 Z × 50 X; góc vát 3 | Tâm X −180. Hai chân trước ở X 180, cùng Y/Z. Đáy chân Y −245. |

Hai lưới phụ mới dùng tấm 0,8, lỗ lục giác đường tròn ngoại tiếp Ø4, bước Z 5, bước Y 4,35, hàng xen kẽ lệch Z 2,5. Dừng tâm lỗ cách viền ≥3; giữ vành đặc 5. Lưới quạt cùng mẫu, cắt theo Ø116 và giữ vùng đặc R5 quanh lỗ gá. Đây là đường xả, không thêm lớp lọc kín. Không đặt quạt giả sau lưới phụ, cũng không vẽ quạt PSU nhìn thẳng ra lưng.

## Cổng I/O: tọa độ toàn máy

Từ mã bo: Y toàn máy = 25 + Y cục bộ; Z = −30 + 1,6 + h = −28,4 + h. Mọi cổng mở ra −X, mặt miệng sát X −225,2. Bảng ghi **lỗ trên lá / miệng sử dụng**, Y × Z; hốc sâu tính vào +X từ mặt lá. Đây là lỗ của linh kiện I/O, không phải các lỗ khoan riêng trên tôn vỏ.

| Cổng / số lượng | Tâm (Y; Z) | Lỗ lá / miệng | Độ sâu hốc |
|---|---|---|---|
| Hai nút | (156; −18,4), (156; −3,4) | Ø4,6 / Ø4 | Nút thứ hai lùi 0,8 so với nút thứ nhất |
| HDMI | (140; −18,4) | 15 × 5,6 / 14 × 4,6, hình thang | 6 |
| DP | (140; −3,4) | 17,2 × 5,8 / 16,2 × 4,8, vát một góc | 6 |
| USB-A đen ×2 | (119; −18,4), (119; −3,4) | 14,6 × 6,8 / 13 × 5 | 9 |
| USB-A xanh ×2 | (99; −18,4), (99; −3,4) | 14,6 × 6,8 / 13 × 5 | 9 |
| USB-A đỏ ×2 | (79; −18,4), (79; −3,4) | 14,6 × 6,8 / 13 × 5 | 9 |
| RJ45 | (57; −16,4) | 17 × 15 / 16 × 14 | 12; rãnh ngàm phụ 3 × 4 tâm (64,5; −16,4) |
| USB-C | (57; 0,6) | 10 × 4,5 / 9 × 3,5, bo hai đầu | 7 |
| Anten ×2 | (37; −18,4), (37; −3,4) | Ø6,5 / ren Ø6,35, có chân giữa | Nhô ra −X khoảng 4 |
| Audio xanh | (19; −21,4) | Ø6,2 / Ø3,5; vòng ngoài Ø6 | Theo thân jack hiện có |
| Audio hồng | (19; −9,4) | Ø6,2 / Ø3,5; vòng ngoài Ø6 | Theo thân jack hiện có |
| Quang | (19; 1,6) | 10 × 10 / 9 × 9 | 3 |

RJ45 có hai ô LED 1,8 × 1,8 tại (64; −21,4), (64; −11,4); đây là chi tiết của ổ mạng. Các vỏ cổng kéo vào máy tới X −207 theo mã; không đẩy bo ra trước. Tổng 15 chỗ cắm và hai nút. Không thêm I/O vỏ ở lưng: power/reset/USB của case vẫn nằm trên nóc.

## Lá GPU ba khe và PSU sau khi quay

Lá GPU hiện có dùng `GPU_PCB_Y = 24,7`, Y cục bộ trên = 30,7. Mở rộng biên dưới từ −9,94 thành **−30,26** để đạt 60,96. Giữ X cục bộ −141…−140 và Z cục bộ −2…118. Sau R− và cộng A, lá nằm đúng X −222…−221, Y −113…7, Z 55,3…116,26. Khe 1/2/3 có tâm Z **65,46 / 85,78 / 106,10**. Không còn nắp trống khi đã lắp card.

| Cửa trên lá GPU | Tâm (Y; Z) | Bao lỗ Y × Z | Hình và chiều sâu |
|---|---|---|---|
| DP 1 | (−91; 68,3) | 16 × 5 | Một góc vát, lòng 14,4 × 3,75, hốc 5 |
| DP 2 | (−67; 68,3) | 16 × 5 | Như trên |
| DP 3 | (−43; 68,3) | 16 × 5 | Như trên |
| HDMI | (−19; 68,3) | 14 × 5 | Hình thang, lòng 12,6 × 3,75, hốc 5 |

Cạnh dài cổng chạy theo Y. Vỏ ổ đi từ X −221 tới −215, không chỉ là hình đen dán trên lá. Giữ thứ tự cổng từ dưới lên **DP / DP / DP / HDMI**. Đề xuất vùng lỗ thoát trên hai dải còn lại: tâm (−53; 96,5), bao 104 Y × 31 Z, Y −105…−1, Z 81…112; lỗ hex Ø2,6, bước Y 6,6 và Z 3,6, hàng xen lệch Z 1,8. Dừng lỗ trước gờ/tai 3 mm.

| Chi tiết của PSU | Tâm (Y; Z) sau R+ | Kích thước Y × Z / vị trí |
|---|---|---|
| Vùng lưới xả | (−78; −94) | Bao 92 × 62, Y −124…−32, Z −125…−63; lỗ hex Ø4 theo mã nguồn, không đổi sang lưới quạt tròn |
| C14 | **(−144; −110)** | Miệng **23 × 18**, góc vát quay cùng nguồn; X −222, hốc đến −210 |
| Công tắc | **(−144; −80)** | Vỏ ngoài **19 × 27**; mặt rocker 14 × 22; mặt ngoài khoảng X −225, có góc nghiêng theo mesh |
| Ba chân C14 | (−144; −105), (−137; −114), (−151; −114) | Lá kim loại 0,8 Y × 4 Z; X −220…−212 |

C14 và công tắc cùng hàng Y −144 ở đáy mặt nguồn; C14 bên trái, công tắc bên phải. Không thêm ổ AC rời hoặc cáp nối C13 trong case này. Quạt hút của PSU có tâm (−152; −95; −125), trục Z; hông thép có cửa hút 130 X × 150 Y tại X −215…−85, Y −170…−20, Z −150. Cửa hông được thấy trong mặt cắt, không chuyển lên tấm lưng.

## Vít, tai và mặt cắt lắp

| Cụm | Tâm lỗ toàn máy | Loại / hướng |
|---|---|---|
| Quạt F6, 4 lỗ | X −222; (Y;Z) = (44,5;24,5), (44,5;129,5), (149,5;24,5), (149,5;129,5) | Lỗ Ø4,5; vít quạt Ø5 × 10, trục X; bước 105 × 105 |
| Lá GPU, 3 tai | X mặt lá −222; (Y;Z) = (2;65,46), (2;85,78), (2;106,10) | #6-32 × 6, đầu Ø5,6; trục X; rãnh hở rộng 3,8 theo Z, Y 0…7 |
| Thanh giữ GPU | X mặt −223; (Y;Z) = (13;52,28), (13;119,28) | Hai M3 × 6; lỗ Ø3,4, trục X, đầu thấp Ø5,5 |
| PSU, 4 tai | Mặt tiếp xúc X −222; (Y;Z) = (−26;−131), (−164;−131), (−26;−57), (−158;−67) | #6-32 × 6, lỗ vỏ Ø3,6; trục X; đầu vít hiện có tâm X −226, đầu Ø5,6 |
| Hông thép | (−209;190;−152), (−209;−190;−152) | Hai vít tay #6-32 × 6, trục Z; giữ mốc có sẵn |
| Cassette kính hông | (−185;214;141), (190;214;141) | Hai M3 × 6 ẩn, trục Y; giữ hai chốt dưới. Không thêm vít xuyên kính |
| Bốn chân | (±180;−225;±112) tại mặt đáy | Một M4 × 8 chìm mỗi chân, trục Y |

Bốn lỗ PSU là mẫu **bất đối xứng** đã quay từ mesh, không thay bằng bốn góc cách đều. Đầu vít nằm phía ngoài −X. Vùng gờ PSU vẫn giữ tai R5 quanh tâm lỗ; không khoét mất tai khi mở cửa toàn mặt.

Mặt cắt trên ảnh là **mặt cắt lệch qua nhiều Z**, không phải một lát cắt chứa cả ba linh kiện. Dùng ba mốc riêng: I/O Z −10,4, lá GPU Z 85,78, PSU Z −94. Thể hiện vách khay Z −36,5 ngăn nguồn và GPU; thân GPU dài 280 đi về +X; nguồn sâu 140 đi về +X trong khoang phụ. Quạt sau dày 25 kết thúc X −197. Khoảng cách từ đỉnh quạt Y 157 tới đáy quạt két Y 167 là 10, giữ nguyên.

Giá GPU bên trong giữ X −221…79, Y −117…−115, Z 75…93. Cáp riser đi từ đầu bo tới đầu card theo đường vòng đã định, không xuyên PSU, tôn lưng hoặc thân quạt. Chừa lối đưa đầu màn hình vào từ −X trước khi siết thanh giữ; không che mặt cổng bằng môi gấp.

## Vật liệu và màu

| Phần | Vật liệu / màu hex |
|---|---|
| Khung, tấm lưng, gờ và thanh giữ | Thép sơn nhám `#24262A`; khung/tôn lưng 1,2, hông 1; metallic 0,25, roughness 0,65; cạnh tôn R1 |
| Lưới case | Thép 0,8, `#101216`; thấy lỗ xuyên và bóng linh kiện bên trong |
| Lá I/O / viền cổng | `#BCC2C8` / `#C9CED4`; metallic 0,8–0,9, roughness 0,3–0,35 |
| Lá GPU | Thép bạc `#B9BEC5`, dày 1; metallic 0,9, roughness 0,3 |
| PSU / lưới nguồn | `#16181D` / `#252932`; giữ lưới thật của nguồn |
| Vít | Thép `#A8ADB4`; vít tay đen `#202329` |
| Nhựa, chân cao su, dây | `#202329`, lòng cổng `#080A0D`, dây `#15171B` |
| Điểm màu linh kiện | Cyan quạt `#22D3EE`; USB xanh `#1F6FD1`, đỏ `#C83C45`; anten `#C6A565`; audio `#7EBE72` / `#E787A0` |
| Kính | Kính khói 4, `#B8C4CA`, IOR 1,5; men viền `#101216` |

## Đã sửa so với bản hiện tại

- Chia rõ khoang nguồn bên trái và khoang kính bên phải; PSU đứng 150 cao × 86 rộng, giữ C14/công tắc sau phép quay R+.
- Thay cửa PSU bị thu nhỏ bằng cửa đủ bao mặt và bốn tai đúng mẫu vít.
- GPU có ba khe dọc và một lá ba khe có cổng thật; bỏ nắp thứ ba theo thay đổi được nêu rõ, không thêm dãy khe ngang.
- Đặt I/O đúng lá ATX hiện có, giữ 15 cổng/hai nút; hốc trang trí không làm lệch bo.
- Quạt sau đúng tâm và bước vít, thêm lưới đục xuyên, giữ chiều xả khi lật 180° quanh X.
- Thêm mép gấp, hốc có bậc, thanh giữ, lưới phụ và đầu vít có chỗ bám. Chân và vít hông giữ nguyên mốc.

## Kiểm tra ảnh cuối

Ảnh đã có ba hình nhìn, ba vị trí PCIe dọc, PSU đứng ở khoang phụ, chân vuông và mặt cắt tách theo Z. Tuy vậy, ảnh imagegen vẫn là minh họa bố cục, chưa phải hình chiếu CAD đúng tỷ lệ. Các sai khác cần biết khi dựng:

- Trong hình chính, I/O hơi cao và cụm GPU kéo xuống quá thấp; lấy tâm và biên trong bảng, không lấy vị trí từ ảnh. Đường đo tổng cao/thân cũng chưa bám đúng mọi mép.
- Hình chính còn đơn giản hóa một số cổng I/O, thiếu rõ USB-C, RJ45 và jack xanh. Ô I/O dưới cùng quay ngang chỉ để nhận dạng. Dựng đủ 15 chỗ cắm và hai nút theo bảng, cạnh dài USB chạy theo Y.
- Chú thích X ở mặt cắt GPU bị lặp `−221…−221`; **thân đúng là −221…59**, lá là −222…−221. Các hình cắt là lát lệch, vị trí xếp trên giấy không phải vị trí trong máy.
- Bốn vít PSU trong ảnh còn gần đối xứng; vít dưới bên phải phải ở (Y;Z) = (−158;−67). Không dựng theo bốn góc của ảnh. Một số nét vít ngoài gờ chỉ minh họa, số lượng và chức năng lấy từ bảng vít.
- Ảnh ghi công tắc 14 × 22 là **mặt rocker**, vỏ ngoài phải 19 × 27. Hai ký hiệu nhỏ còn trên rocker cần bỏ khi dựng phiên bản không chữ. Hình lá C14/mesh nhỏ không đủ để đếm chính xác.

Đã kiểm tra số học các tâm GPU, C14, công tắc và bốn vít PSU bằng đúng R−/R+ từ dữ liệu nguồn. Chỉ tạo ảnh và đặc tả này; chưa sửa mô hình đang chạy.

Ảnh tạo bằng công cụ imagegen tích hợp. Lệnh tạo ảnh được lưu bên dưới để dựng lại; bảng tọa độ tiếng Việt là dữ liệu chuẩn.

<details>
<summary>Lệnh imagegen: bản đầu và lượt sửa hình học</summary>

### Lệnh tạo

```text
Use case: infographic-diagram. Create exactly ONE high-resolution landscape engineering/product reference sheet, white background, title "SHOWCASE DUAL CHAMBER — REAR", subtitle "ATX / 280 mm vertical GPU / upright ATX PSU / mm". Beautiful realistic matte charcoal folded steel, brand-free, slim radiused edges, physically credible cavities and screw heads. Blue technical dimension lines and numbered callouts OUTSIDE the panel; NO lettering or logos printed on the case or components. This is a PC case REAR redesign, not an ordinary narrow tower. Prioritize geometric accuracy.

LAYOUT: left 55% dominated by a LARGE TRUE ORTHOGRAPHIC REAR elevation viewed from X-negative toward +X, +Y up, +Z right. Right top a smaller realistic rear three-quarter view showing glass-side chamber and deep steel secondary chamber. Right bottom an engineering CUTAWAY / OFFSET SECTION illustrating the actual seating of I/O, vertical GPU bracket/riser and upright PSU at rear wall with X depth dimensions. Tiny bottom strip has coordinate legend and port details. Keep generous white space, no overlapping dimensions.

REAR body is 300 wide Z by 450 tall Y, rear plane X=-225, bounds Y=-225..225 and Z=-150..150; four 20mm feet make height470. Rear is notably wider than a tower, width:height 2:3. Closed cable/PSU chamber is the LEFT ~113mm wide region, separated by tray at Z=-36.5. Main glass chamber is on RIGHT. Draw thin dashed tray line. Body is #24262A steel 1.2mm, border folded inward12 with 8mm wide visible rail and R2 rounded outside corners. No giant flat featureless plate. Components and openings MUST share these same positions in all views.

NUMBERED DETAILS and dimensions on main rear:
01 "Folded frame", leader to rim, note "1.2 steel / 12 return".
02 "ATX I/O", slim SILVER vertical 158 high x36 wide shield centered Y86 Z-10.4. CASE aperture159 high x37 wide, edges Y6.5..165.5 Z-28.9..8.1. At 300mm panel width this shield is 12% width and slightly LEFT of center. Its front X=-225.2; cannot recess entire board. Form recessed surround2mm then stepped clips bring shield back to its fixed datum. Dimension159 vertical and37 horizontal. Show accurately condensed full ATX I/O: 2 top buttons; HDMI+DP row; 3 rows of paired tall USB-A (black, blue, red); LAN+USB-C; 2 gold antenna jacks; green audio,pink audio,optical. No text on shield.
03 "120 rear exhaust", upper RIGHT. Center Y97 Z77, square120x120 frame Y37..157 Z17..137. Circular aperture diameter116 with fine stamped hex mesh, not exposed concentric wire rings. Visible rotor/cyan ring behind mesh, subtle cyan only. Four fan screws on105x105 at Y44.5/149.5 and Z24.5/129.5. Recess3mm; fan rear surfaceX=-222, centerX=-209.5. Note "105 pitch / flip180° / exhaust -X". Fan field rounded126x132 centeredY97 Z77. Top of fan is almost same height as top of I/O, NOT centered vertically on entire panel.
04 "3 vertical GPU slots", lower RIGHT but around middle-low of panel, NOT bottom. Three vertical module bands collectively 60.96 wide x120 high, Y=-113..7 Z55.3..116.26. These are VERTICAL long slots side-by-side with pitch20.32 across Z, absolutely NO horizontal expansion slot row. One SILVER unified three-slot GPU bracket covers all three positions; first narrow band holds exactly four ports in vertical stack: lowest DP Y-91, next DP-67, next DP-43, highest HDMI-19, all Z68.3. Other two bands have genuine hex vent holes. No remaining blank cover. Bracket flange atX=-222..-221. Three visible retention screws along its TOP, atY2 Z65.46/85.78/106.10. Dark retention bar directly above, 8high x73wide, centeredY13 Z85.78, two end screws. Dimension full120 height,60.96 width,20.32 pitch; leader also labels underlying "Clear110 x56.96". Slot section must show GPU280mm long extending forward fromX=-221 to59, GPU tall125, thickness55, fans face+Z toward glass. Riser connects GPU to ATX board, no direct card/board intersection.
05 "Upright ATX PSU", lower LEFT behind tray, exact face150 HIGH x86 WIDE centeredY-95 Z-94, boundsY-170..-20 Z-137..-51. It is upright, not landscape, not a horizontal bottom-mounted PSU. Body depth140 extends X-222..-82. Frame outer162high x98wide; opening150x86 with four little integral screw tabs. Four mounting screws at(Y,Z)=(-26,-131),(-164,-131),(-26,-57),(-158,-67), asymmetrical bottom-right. Main PSU face has honeycomb exhaust in upper area Y-124..-32 Z-125..-63. C14 socket and rocker sit side by side near BOTTOM atY-144: C14 on LEFT Z-110, mouth23high x18wide; rocker on RIGHT Z-80, outer19high x27wide. These fittings are rotated90° with whole PSU. Render three actual C14 pins. Labels outside "C14 23 x18", "Switch19 x27", "PSU150 x86". No extra PSU fan circle on rear: intake fan faces closed side-Z. Four screws, not symmetric generic substitute.
06 "Open mesh vents", two secondary open hex mesh regions: rear chamber upper LEFT centerY100 Z-94,130high x60wide; main chamber beside GPU at centerY-60 Z15,100high x26wide. Dimension each130x60 and100x26 with leaders. Not dust filters. Keep second vent separate from GPU bracket. Hex pattern about4mm cells. No other arbitrary vent cutouts.
07 "Panel fasteners", rear-edge visible thumbscrews for closed steel side only: X-209 Y+/-190 Z=-152, axesZ, seen at left edge in rear and 3/4. Glass is mounted with concealed top screws, no screws through glass.
08 "Feet", two visible rear feet each36wide x20high, centersY-235 Z+/-112, depth50, X=-180. Dimension20 feet, body450, total470, width300. Rounded black feet.
09 "Retention bar", leader above the 3 GPU columns; screws and bent lip distinct.

SECTION details: white technical cutaway with rear wall vertical atX-225, interior+X pointing right. Use offset sections to show THREE separate aligned junctions without implying same Z: I/O atZ-10.4 faceX-225.2 to-224.4; GPU atZ85.78 bracketX-222..-221,280mm bodyX-221..59 and riser cable with smooth bendR15; PSU atZ-94 with rearX-222 and140mm depth to-82, rotated upright150mm high and86mm acrossZ. Show trayZ=-36.5 between PSU and GPU in a small plan cutaway or 3/4 section, label "PSU behind tray". Show fanX-222..-197 and airflow-X. Dimension recess3 to PSU/fan/GPU seats; I/O separate fixed datum. Do not show a PSU underneath GPU.
Tiny explanatory note "3-slot bracket revision; existing GPU body and port centers retained". Footer "+X front / +Y up / +Z glass side. Dimensions govern; not pixel scale."
Rendering: precise realistic metal construction with folded borders, recessed fields, small chamfers, clean sparse shadows, polished professional white-background reference sheet, English labels legible, no watermark.
```

### Lệnh sửa

```text
undefined
```

</details>
