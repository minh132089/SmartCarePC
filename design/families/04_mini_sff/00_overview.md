# 04 Mini SFF — Tổng quan lắp ráp

Ảnh: [00_overview.png](00_overview.png). Nguồn số liệu: [đặc tả họ vỏ](../00_families_spec.md), mục 0, 4, 7 và 8; quy ước đầu nối theo [Gaming](../../gaming/00_blueprint.md). Đây là kích thước mô hình mô phỏng, không phải bản vẽ gia công. **Số trong tài liệu là chuẩn; không đo hình tạo sinh để dựng hình học.**

## Hệ tọa độ và bao vỏ

Đơn vị mm; O ở tâm thân, không tính chân. +X ra trước, +Y lên nóc, +Z sang hông lắp bo. W × H × D là Z × Y × X. Thân **180 × 250 × 350**, biên X ±175, Y ±125, Z ±90; cao cả chân **262**. 100 mm = 1 đơn vị cảnh. Tọa độ dưới đây là trạng thái đã lắp, không phải khoảng tách trên ảnh.

| Số / cụm | Kích thước, vị trí và hướng lắp |
|---|---|
| 1 — Vỏ | Nhôm dày 2; trước X 173…175, Y ±120, Z ±86, góc R8. Hai hông 334 X × 232 Y × 2, mặt ngoài Z ±90; tháo ra ±Z. Nóc tháo +Y. |
| 2 — ITX | PCB 170 X × 170 Y × 1,6 Z; B = (−76;0;0), mặt linh kiện +Z; đưa từ +Z về khay. Khay mặt trước Z −6,5, dày 1,2; bao X −162…43, Y ±113. |
| 3 — CPU | 37,5 × 37,5 × 4,5; đáy (−96;12;4,7), IHS tại (−96;12;9,2). Tiếp điểm phẳng, tam giác và rãnh định hướng; không có vít riêng. |
| 4 — Tản thấp | 92 X × 92 Y × 47 Z từ IHS; X −142…−50, Y −34…58, Z 9,2…56,2. Quạt 92 × 92 × 14, tâm (−96;12;49,2), thổi −Z vào bo. Backplate 90 × 90 sau socket, đệm cách điện 0,8. |
| 5 — RAM ×2 | DDR4 133,35 Y × 42 Z × 6 X; gốc đáy (−21;6;5,5) và (−11;6;5,5), đỉnh Z 47,5. Cả hai khe đều dùng; ấn theo −Z. |
| 6 — GPU | Thân 240 X × 120 Y × 45 Z sau quay; A = (−51;60;−48), R+(x;y;z) = (x;−z;y). Bao X −171…69, Y −60…60, Z −70,5…−25,5. Hai quạt Ø90 hướng −Z, tâm (−111;0;−60) và (9;0;−60). |
| 7 — SSD | Một M.2 2280, 80 X × 22 Y × tối đa 3; tâm XY (−88;−48), miệng (−128;−48), vít (−48;−48), chân đỡ cao 3. Cắm nghiêng rồi hạ đầu vít, trên mặt linh kiện bo. |
| 8 — SFX | Vỏ cục bộ 100 X × 63,5 Y × 125 Z, quay R−(x;y;z) = (x;z;−y), tâm (100;−47,5;43,25). Bao X 50…150, Y −110…15, Z 11,5…75. Quạt 92 tâm (100;−47,5;67,25), hút hông +Z; xả −X. Giá L dày 1,2 tại mặt AC X50, không bịt lưới xả. |
| 9 — Quạt case ×1 | Nóc 120 × 120 × 25, tâm (−65;105,5;12), Y 93…118; xả +Y, nối SYS1. Bước lỗ 105; tâm lỗ X = −117,5/−12,5 và Z = −40,5/64,5. |
| 10 — Dây | DC đen mềm ở Z −24…−8 và hành lang X14…47; ATX 300, EPS 400, GPU 300; R uốn ≥12. Giữ dây đã buộc cách cánh quạt ≥10. |

Riser x16 dài phần mềm **400**, rộng 70, dày 1,5, R uốn ≥15; đầu đực 89 × 12 × 1,6, adapter cái 110 × 18 × 14, hai tai gá cách 100. Đầu bo (−73;−72;10,4); đầu nhận GPU (−73;64,2;−28,3). Cáp xuống G4 rồi lên trong vùng X −125…−20, Z −22…−9; phần chùng gấp U theo Y. Luồn đầu đực qua cửa trước; đầu cái giữ phía GPU. Không đi qua cửa bảo trì CPU.

GPU nằm sau lưng bo. Cassette có **3 khe dọc**, Y −58…62, Z −83,26…−22,3, bước 20,32; lá GPU dùng hai khe, khe thứ ba giữ trống. Giá dưới X −171…84, Y −66…−64, Z −61…−43. Bao thân GPU chưa tính chân PCIe nhô 8, lá cổng nhô 1 và đầu vít tối đa 3; quay cả các phần này theo R+.

## Vỏ, cổng và dây AC

- Mỗi hông có **33 khe đứng**, dài 170 Y × rộng 4 X, đầu R2; tâm X = −144 + 9j, j = 0…32, Y0. Lọc nằm phía trong, không cắt vùng vít.
- Lỗ nóc trong X −148…133, Z ±70; lỗ đáy trong X −142…135, Z ±67. Vùng lưới lỗ Ø2,4, bước 3,6 so le; mép đặc 6, vùng vít bán kính 5. Lọc PET dày 0,5, mắt mở 0,35, sợi 0,15, viền nylon 1,5.
- Bốn chân Ø22 × cao 12, tâm (±142;−131;±64). Đáy chân Y −137. Nóc và hai hông thoáng; mặt trước đặc.
- Power Ø14 ở (175;−62;59), vòng đèn trắng; USB-C 8,4 Z × 2,6 Y tại (175;−88;59). Module X156…173, Y−99…−49, Z49…69. Không có reset, USB-A hoặc audio mặt trước.
- Lá I/O bo ở X −175…−174,2, Y ±79, Z1,6…37,6; cửa nới 0,5 mỗi cạnh Y/Z. Cổng hướng −X, tâm Z16; 2 USB-A tại Y58/39, RJ45 tại Y5, 3 jack TRS tại Y−25/−42/−59. GPU có 3 DP + 1 HDMI.
- Inlet C14 rời ở (−175;−103;38). Dây AC có vỏ kín, dài tâm **450**, kẹp sát đáy Y−116 tới đầu C13 góc 90° trên PSU. C14 PSU sau quay ở (50;−4,5;52,25); đầu C13 chỉ chiếm X25…50. Không vẽ dây điện trần hoặc nối inlet thẳng vào PCB.

| Cửa khay, Z = −6,5 | Tâm X;Y | Lòng mở X × Y | Tuyến |
|---|---|---|---|
| 04-G0 | 30;−62 | 18 × 58 | PSU vào khoang giữa; đầu ATX đi theo cạnh 58 |
| 04-G1 | −131;99 | 28 × 10 | EPS |
| 04-G2 | 30;19 | 18 × 58 | ATX24 |
| 04-G3 | −54;99 | 32 × 10 | CPU_FAN và quạt nóc |
| 04-G4 | −70;−101 | 96 × 12 | Riser |
| 04-G5 | 0;−101 | 28 × 12 | Type-E và F_PANEL |

Viền EPDM rộng 3 quanh lòng mở. Không có G6. Cửa CPU 96 × 96 tâm (−96;12;−6,5), chỉ để tiếp cận gá tản. Nguồn GPU qua G0, vòng dưới khay, đi ở X82…105 rồi tới ổ (54;−60;−35,25), miệng hướng −Y; không chạy ngang mặt hai quạt.

## Đầu nối và khóa

Tâm header bo ghi cục bộ so với B, dạng (u;v); giữ chiều cao và hình đầu cắm theo tài liệu gốc.

| Đầu | Số tiếp điểm / khóa | Tâm hoặc nơi dùng |
|---|---|---|
| ATX | 24, 2 × 12; dây 20+4, ngàm khóa | (80;5) |
| EPS | 8, 2 × 4; dây 4+4, khóa CPU | (−55;78) |
| GPU nguồn | 8, 2 × 4; dây 6+2, khóa PCIe | Ổ GPU hướng −Y; không đổi với EPS |
| CPU_FAN / SYS1 | PWM 4, gờ dẫn hướng; GND/12V/tach/PWM | (17;73) / (−48;−53) |
| Type-E | 20 tiếp điểm, khóa định hướng | (80;58), nối USB-C 24 tiếp điểm ngoài vỏ |
| F_PANEL | 9 chân trong 2 × 5, khuyết 10 | (60;−80); PWR SW chân 6/8, PLED +2/−4; các nhánh khác bỏ trống |
| PCIe x16 | 164 tiếp điểm, rãnh và chốt khóa | (3;−72), nối riser |
| DDR4 | 288 tiếp điểm mỗi thanh, rãnh lệch tâm | Hai khe A/B tại (55;6), (65;6) |
| M.2 | 75 vị trí, 67 tiếp điểm theo quy ước Gaming; M-key | SSD không dùng dây SATA |
| AC | C14/C13, 3 tiếp điểm, vỏ định hướng | Inlet sau → C13 góc → PSU |
| Cổng ngoài | DP20 ×3; HDMI19 ×1; USB-A4 ×2; RJ45 8; TRS3 ×3 | DP khóa góc, HDMI hình thang; USB có lưỡi, RJ45 có lẫy |

USB19 (80;−46), HD_AUDIO (−62;−80) và SYS2 (−25;−53) vẫn có trên bo nhưng không nối dây trong cấu hình này. CPU dùng bề mặt LGA 1.200 tiếp điểm quy ước mô phỏng, không đại diện socket thương mại.

## Vít và vật liệu

| Cụm | Số / loại | Mốc |
|---|---|---|
| Bo | 4 M3 × 5 + 4 chân đế cao 6,5 | X −151/−1, Y ±75; lỗ Ø3,4 |
| Tản | 4 M3 × 12 lò xo | X −135/−57, Y −27/51, bước 78 |
| Quạt CPU tích hợp | 4 M3 × 16 | Tâm tản ±41,25 theo X/Y |
| SSD | 1 M2 × 2,5 | XY (−48;−48), chân đỡ cao 3 |
| PSU | 4 M3 × 6 | X50; Y −102,5/7,5, Z18,25/68,25 |
| Lá GPU / nắp còn lại | 2 + 1 vít #6-32 × 6 | Tái dùng hai vít nắp cho GPU |
| Giá GPU | 2 M3 | Theo giá khung; nguồn chưa chốt chiều dài |
| Quạt nóc | 4 vít tự tạo ren Ø5 × 10 | Bước 105 × 105 |
| Hai hông | 8 M3 × 6 | Mỗi hông X ±152, Y ±101 |
| Nóc | 4 M3 × 6 chìm | X ±157, Z ±72 |
| Trước | 4 M3 × 6, từ trong | Y ±105, Z ±71 |
| Chân | 4 M4 × 8 chìm | Tâm từng chân |

Tai adapter riser phải có lỗ và chốt giữ; chiều dài vít gá riser và vị trí bắt giá L vào khung chưa được tài liệu gốc chốt, không suy ra từ ảnh. Đáy/lưng cố định và lọc nam châm: 0 vít tháo. Không tính vít trang trí trên vỏ GPU/PSU thành thao tác tháo cụm kín.

Nhôm xước graphite **#45474A**, metallic 0,8, roughness 0,38; vân mặt trước dọc Y, hông dọc X. Lưới **#101216**; kim loại sáng **#AEB4BC**; PCB **#16181D**; tiếp điểm **#C6A565**; cao su/nhựa **#202329**; dây **#15171B**. Power trắng **#D2D7DD**, đường kích thước xanh **#2164AC**. Không kính, logo hoặc ARGB.

## Các hình và điểm cần chú ý

Hình lớn tách lớp 3/4 từ +X/+Y/+Z, theo hướng lắp. FRONT +X nhìn −X thể hiện mặt trước, +Z ở trái ảnh. LEFT/OPEN +Z nhìn −Z bỏ nắp để thấy bo, phía trước ở phải. TOP +Y nhìn −Y, +X xuống dưới và +Z sang phải. RIGHT/GPU −Z nhìn +Z bỏ nắp để thấy card, phía trước ở trái. Các inset dây là sơ đồ tuyến, không phải hình chiếu gia công. Với sheet tổng quan, các hình này thay cho bộ sáu mặt của sheet vỏ riêng theo brief.

1. GPU ở khoang đối diện bo và cần riser; không cắm xuyên khay. Hai mặt quạt hút từ hai hông kim loại thông gió.
2. Chỉ **một quạt case 120 ở nóc**; quạt CPU 92, PSU 92 và hai rotor GPU thuộc các linh kiện riêng.
3. Tản mẫu cao 47 từ IHS, giới hạn 55; giữ RAM DDR4 nguyên cao 42. Mẫu tản cách hông trong 31,8.
4. PSU chỉ SFX sâu 100; đầu AC góc nằm trong hành lang 41 giữa bo và nguồn. Dây AC chạy riêng sát đáy.
5. GPU mẫu 240 × 120 × 45; giới hạn 260 × 120 × 50. Mẫu cách hông hút 17,5, quạt nóc cách mép bo 8. Không lắp radiator trong cấu hình này.

## Ghi chú ảnh tạo sinh

Ảnh dùng công cụ imagegen tích hợp, một lượt tạo và một lượt sửa. Sai lệch còn lại sau kiểm tra:

- LEFT/OPEN bỏ sót tản CPU khi lắp hoàn chỉnh; phải thêm cụm số 4 theo bao ở bảng. Hình lớn còn thể hiện RAM trên bo cùng hai thanh tách rời: đó là lặp hình, BOM chỉ có **hai thanh**, không phải bốn.
- Nhãn SSD trong ảnh ghi nhầm “M.2 220”; đúng là **M.2 2280, 80 × 22**. Mặt trên SSD minh họa không xác định cách đặt chân đỡ.
- TOP đã xoay trục dài xuống dưới nhưng tỷ lệ khác các inset còn lại. Mũi tên +Y nằm trên mặt giấy của TOP là sai: +Y phải vuông góc mặt giấy, hướng về người xem. Không dùng tỷ lệ ảnh để đo.
- GPU đã tách về phía xa nhưng mặt quạt/lá cổng trong phối cảnh còn xoay để dễ nhìn; hướng lắp chuẩn là R+ và mặt quạt −Z. Đường riser phóng đại ở mép khung, không thể hiện chính xác đường cong trong khoang giữa.
- Hình gá tản nổi phía trước chỉ mang tính giải thích; backplate phải nằm **sau bo**, không kẹp giữa quạt và CPU. CPU chưa được tách đủ để thấy mặt dưới.
- Inset AC còn đoạn màu vàng gần chân đầu cắm; đó không phải chỉ dẫn dây trần. Dựng toàn bộ dây với vỏ đen kín. Hình đầu nối, vị trí G, lưới, số khe và vít nhỏ được giản lược; số chân, số lỗ, tọa độ và lịch vít lấy ở bảng, không đếm nét ảnh.

Ảnh phù hợp tham chiếu bố cục và vật liệu, chưa đạt độ chính xác hình học để dùng như bản lắp độc lập.

Lệnh vẽ tóm tắt: “MINI SFF / 00 OVERVIEW; white product reference sheet, graphite aluminium, blue dimensions; exploded sandwich ITX assembly, low-profile47, DDR4 x2, M.2 x1, GPU240 dual-fan with riser400, SFX100, exactly one top120 fan, insulated AC450; FRONT/LEFT/TOP/RIGHT GPU orthographics, keyed connectors and screw schedule; no logos.” Lượt sửa tập trung vào hai nắp hông, hướng sandwich, trục lắp CPU/tản, nhãn 5/10, hướng TOP và vỏ đầu AC.
