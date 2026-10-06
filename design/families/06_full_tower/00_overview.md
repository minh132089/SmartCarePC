# 06 Full tower — tổng thể

Ảnh: [00_overview.png](00_overview.png). Nguồn kích thước: [đặc tả họ vỏ, mục 0, 6 và 7.3](../00_families_spec.md); dáng theo [concept](../../case-families/06_full_tower.png). Giữ linh kiện ATX hiện tại và quy ước [Gaming](../../gaming/00_blueprint.md), nhưng dùng tọa độ họ 06 dưới đây. Đây là thiết kế mô phỏng; số trong đặc tả quyết định hình học, không đo ngược từ ảnh.

## Kích thước và tọa độ

Đơn vị mm. O ở tâm thân, không tính chân. +X ra trước, +Y lên nóc, +Z ra kính. W × H × D = Z × Y × X. Tọa độ là trạng thái đã lắp; khoảng tách lớp chỉ minh họa thao tác. 100 mm = 1 đơn vị cảnh.

Thân **250 × 620 × 550**, biên X ±275, Y ±310, Z ±125. Bốn chân 58 X × 22 Y × 38 Z, tâm `(±226; −321; ±91)`; cao cả chân **642**. Vát hai cạnh trước 8 × 8.

| Số | Cụm / kích thước | Mốc lắp (X; Y; Z), hướng |
|---|---|---|
| 1 | Khung; kính 528 X × 588 Y × 4 Z | Kính Z 121…125; hông kín Z −125…−124; khay trước Z −94,5, dày 1,2 |
| 2 | ATX 244 X × 305 Y × 1,6 Z | B = `(−139;80;−88)`, mặt linh kiện +Z; 9 chân đế cao 6,5 |
| 3 | CPU 37,5 × 37,5 × 4,5 | Đáy `(−120;139;−83,3)`, IHS `(−120;139;−78,8)`; LGA phẳng, có tam giác/rãnh khóa |
| 4 | Bơm Ø54 × 48; két 397 X × 27 Y × 120 Z | Đế bơm trên IHS, nhô +Z; gốc giữa mặt trên két `(−35;304;40)`; két Y 277…304, quạt Y 252…277 |
| 5 | 2 DDR4: 6 X × 133,35 Y × 42 Z | Gốc đáy A2 `(−60;142;−82,5)`, B2 `(−41;142;−82,5)`; A1/B1 trống |
| 6 | GPU 280 X × 55 Y × 125 Z | A = `(−131;25,3;−73,4)`, R0; thân X −271…9, Y −2,2…52,8, Z −73,4…51,6; quạt hướng −Y |
| 7 | M.2 2280, 80 X × 22 Y × ≤3 | Tâm chiếu `(−104;18)` trên bo; miệng X −144, vít X −64; chân đỡ cao 3 theo cụm Gaming |
| 8 | PSU ATX 140 X × 86 Y × 150 Z | Tâm `(−202;−251;−18)`; hút −Y, AC phía −X, dây liền ra +X |
| 9 | 5 quạt vỏ 140 × 140 × 25 | F1–F4 `(247,5;−219/−73/73/219;0)`, thổi −X; F5 `(−259,5;149;40)`, xả −X |
| 4 / R1–R3 | 3 quạt két 120 × 120 × 25 | `(−155/−35/85;264,5;40)`, thổi +Y; thuộc AIO, không cộng lại vào số 9 |
| 10 | Dây, hub và adapter ATX24 | Hub tâm `(90;70;−110)`, hộp 80 X × 50 Y × 18 Z; tai tổng 92, lỗ X 47/133, Y 70 |

Hai ống AIO Ø11, dài tâm 450 mỗi ống, R uốn ≥30; fitting két `(154;264;25/55)`, nối thật tới bơm và vòng trước RAM. Nắp che PSU tại Y −190, X −271…193, Z −94,5…119. Đệm chống võng GPU tại `(−5;−4,2;26)`. GPU có chân PCIe nhô 8, lá cổng nhô 1, đầu vít ≤3 ngoài bao thân card; giữ cả ba phần này.

## Cửa, panel và vít

| Vị trí | Hình học / cách giữ |
|---|---|
| Trước | Cassette X 262…275, Y ±299, Z ±115; cửa lưới 184 Z × 588 Y, tâm Y/Z = 0; lưới X 271…271,8, lọc X 268,5. Sáu ngàm + 2 M3 × 6 tại `(273;−302;±98)` |
| Kính | X ±264, Y ±294; 4 vít tay có đệm tại X ±249, Y ±279, trục Z; tháo +Z |
| Hông kín | 528 × 588 × 1; 2 vít tay X −258, Y ±274; trượt −X rồi nhấc −Z |
| Nóc | Cửa X −245…215, Z −90…104 (460 × 194); 2 M3 × 6 tại `(−261;310;±103)` + ngàm trước; lọc nam châm 0 vít |
| Đáy | Hút PSU 124 × 124 tại X −202, Z −18; lọc kéo −X. Lưới phụ X −95…178, Z −75…90; 4 chân dùng 4 M4 × 8 |
| Lưng | 8 khe PCIe ngang, bước 20,32, mép trên Y 56; GPU thay 2 nắp, còn 6. I/O theo lá X −275,2…−274,4, Y 62…220, Z −86,4…−50,4; mở rộng cửa 0,5 mỗi cạnh Y/Z. Mặt AC PSU X −272 |

Vít linh kiện: bo **9 M3 × 5** trên 9 chân đế; bơm **4 M3 × 12** lò xo, lưới 78 × 78 quanh CPU, backplate 90 × 90; két **12 M3 × 6**; quạt két **12 M3 × 30**, tổng 24 đệm. Mỗi tâm quạt két có 4 lỗ lệch ±52,5 theo X/Z. Năm quạt vỏ dùng **20 vít Ø5 × 10**, bước lỗ 124,5 theo mặt quạt. GPU 2 và PSU 4 vít **#6-32 × 6**; SSD 1 **M2 × 2,5**; hub 2 **M3**, chiều dài khớp tai gá; shroud 2 **M3 × 6** tại X −225/158, Y −190, Z 108. Sáu nắp PCIe còn lại dùng 6 vít #6-32 × 6; không đếm lại 2 vít chuyển sang GPU. Lỗ bo Ø3,4 theo mesh hiện tại; khung cố định 0 vít tháo.

I/O nóc hướng +Y tại X 246, Y 310: reset Ø4 Z −84; USB-A Z −42/+42 (miệng 6 X × 13 Z); power Ø14 Z 0; jack combo Ø3,5 Z 84. HDD LED Ø1,5 tại `(261;310;0)`, PLED vòng trắng `#D2D7DD`. Không USB-C trên vỏ. Cụm module X 238…254, Y 296…308, Z −95…95.

I/O bo giữ bản hiện tại trong `tools/build_gaming.py`: 2 nút BIOS/CMOS, HDMI, DP, 6 USB-A, RJ45, USB-C, 2 anten RP-SMA, 2 jack âm thanh và cổng quang. Cổng USB-C sau bo không phải cổng trước vỏ. Lá GPU có 3 DP + 1 HDMI; nguồn có C14 và công tắc.

## Đầu cắm và đường dây

| Đầu | Chân, khóa và tuyến |
|---|---|
| ATX24 | 2 × 12, ngàm; tọa độ B + `(117;40;14,6)`. Adapter male/female nối đủ 24 đường đặt ngay header, bao 25 X × 58 Y × 42 Z; dây nhánh 650 cấp 12 V/5 V/GND cho hub |
| EPS8 / GPU8 | EPS 4+4 tại B + `(−60;146;14,6)`; GPU 6+2 tại `(−6;43,85;51,6)`, cửa +Z. Khác khóa; không đổi hai đầu cho nhau |
| Bơm / quạt két | AIO_PUMP 4 chân tại B + `(10;126;9,6)`; CPU_FAN 4 chân tại B + `(35;126;9,6)`. Chia ba quạt két ≤0,2 A/quạt, tổng ≤0,6 A, chỉ một tach |
| Hub PWM | 5 quạt case vào port 1–5; 6–8 trống. Điều khiển SYS1 tại B + `(106;85;9,6)` dùng GND/tach/PWM, không nối ngược 12 V; chỉ port 1 trả tach |
| USB / audio / nút | USB19 tại B + `(113;−127;10,6)`, một vị trí khóa; AUDIO9 tại B + `(−97;−143;9,1)`, khuyết chân 8; F_PANEL9 tại B + `(95;−143;9,1)`, khuyết chân 10, đúng cực LED |
| Khe linh kiện | DDR4 288 tiếp điểm/rãnh lệch; PCIe x16 164 tiếp điểm/chốt cuối; M.2 khung 75 vị trí, 67 tiếp điểm/M-key; CPU dùng tiếp điểm LGA phẳng |
| ARGB / AC | 8 cổng hub +5V/D/khuyết/GND có nắp, không nối đèn trong cấu hình này. C13 ghép C14 có 3 tiếp điểm |

Hub lấy nguồn qua adapter ATX24, không dùng SATA hoặc sửa nguồn thành modular. Nhánh thiết kế ≥3 A ở 12 V và ≥2,5 A ở 5 V, bảo vệ quá dòng từng rail. Dây nguồn đen, R uốn ≥20; bó dây cách cánh ≥10. ATX 600, EPS 650, GPU 600; thêm đoạn nối tháo được 200 khi cần. Chừa ≥25 phía +Z ổ nguồn GPU cho đầu và vòng dây, kẹp tại `(40;45;65)`.

Tất cả G nằm trên khay Z −94,5; số đo là lòng mở X × Y, thêm viền EPDM rộng 3:

| Mã | Tâm X; Y | Lòng mở | Tuyến |
|---|---|---|---|
| 06-G0 | −91; −248 | 60 × 28 | PSU vào khoang dây |
| 06-G1 | −199; 252 | 34 × 14 | EPS |
| 06-G2 | 10; 133 | 28 × 68 | ATX24, quạt trước |
| 06-G3 | −94; 252 | 48 × 14 | Bơm/quạt két |
| 06-G4 | 52; 31 | 30 × 34 | GPU |
| 06-G5 | −42; −91 | 46 × 14 | USB/F_PANEL |
| 06-G6 | −235; −91 | 42 × 14 | HD_AUDIO |

Khoang dây Z −124…−95,7 sâu 28,3; rãnh nguồn rộng 30 tại X 25, rãnh EPS rộng 26 tại X −245, 8 đai. Cửa bảo trì CPU 100 × 100 tâm `(−120;139;−94,5)` không dùng luồn dây. Dây dư nằm dưới shroud.

## Vật liệu và các hình nhìn

Khung thép nhám `#24262A`, dày 1,2; tấm rời 1,0, roughness 0,65, metallic 0,25. Lưới `#101216` dày 0,8; mặt trước lỗ lục giác rộng 3, bước 4; các lưới khác lỗ Ø2,4 bước 3,6. Kính khói `#B8C4CA` dày 4, truyền sáng mục tiêu 75%, IOR 1,5, viền men đen 10; gioăng 1. Cao su/nhựa `#202329`, dây `#15171B`, PCB `#16181D`, kim loại `#AEB4BC`, tiếp điểm `#C6A565`. Lọc PET 0,5 nằm trong bao thân. Toàn bộ quạt đen, không ARGB.

Hình lớn 3/4 từ +X/+Y/+Z chỉ hướng lắp. FRONT +X nhìn −X thấy 4 quạt; BACK −X nhìn +X thấy I/O, quạt sau, PCIe và AC; LEFT +Z nhìn −Z thấy bo, cạnh GPU, AIO và shroud; RIGHT −Z nhìn +Z thấy nắp kín; TOP +Y nhìn −Y thấy lưới/két và I/O; BOTTOM −Y nhìn +Y thấy lọc, lưới và chân. Inset BEHIND TRAY mở nắp kín để chỉ đường dây. Sáu hình chiếu dùng cùng tỷ lệ mục tiêu, không lấy tỷ lệ pixel làm chuẩn. FRONT: +Z trái; BACK: +Z phải; LEFT: +X phải; RIGHT: +X trái; TOP: +X xuống, +Z phải; BOTTOM: +X lên, +Z phải.

## Người mới cần chú ý

1. **4 trước 140 + 1 sau 140 + 3 két 120 = 8 quạt rời**. Ba rotor GPU và rotor nguồn tính riêng.
2. GPU nằm ngang, quạt quay xuống, cắm thẳng x16; lá giữ hai khe nhưng thân 55 cần khoảng trống ba khe.
3. Két nằm dưới nóc, quạt dưới két thổi lên. Không dùng vít quạt dài thay vít nóc ngắn.
4. Hub sau khay cần nhánh nguồn ATX24; dây PWM điều khiển không tự cấp điện cho năm quạt.
5. Bo vẫn ATX. Két/quạt cách mép bo 19,5; GPU mẫu cách quạt trước 226. Không tự thêm E-ATX, két trước, 280/420 hoặc ổ cứng vào cấu hình.

## Kiểm tra ảnh

Tạo bằng công cụ imagegen tích hợp, một lượt tạo và một lượt sửa theo brief. Prompt: bản tham chiếu full tower tách lớp 3/4, sáu hình chiếu, bảng vít/khóa đầu cắm và dây sau khay; sửa số quạt FRONT, CPU/bơm và hướng inset.

PNG cuối đủ sáu hướng nhìn và hình tách lớp; hình lớn có đúng 4 quạt trước + 1 sau + 3 két. Ảnh còn các sai lệch sau, không dùng chúng làm chuẩn dựng:

- FRONT và hình case nhỏ vẫn chỉ hiện 3 rotor rõ; phải dựng **4 quạt 140** theo bảng. Tỷ lệ giữa các inset chưa đồng nhất tuyệt đối.
- BEHIND TRAY thiếu nhãn G0 và hub vẫn nằm về bên phải; đúng ra hub ở phía trước/bên trái của hình nhìn −Z. Dựng tất cả G và hub theo tọa độ, không chép đường nét trong inset. Adapter ATX24 chưa được thể hiện rõ đúng vị trí trên header.
- Nhãn reset ở dải TOP I/O ghi nhầm Ø14; đúng **Ø4**. Nhãn AUDIO9 trong dải đầu cắm bị lỗi chữ; dùng tên và khóa chân trong bảng. Cụm I/O minh họa có chữ “Audio 7.1”, không phải thông số được chốt; số cổng lấy theo mã nguồn đã dẫn.
- Hình lớn tách CPU theo mũi tên dọc trang, vị trí khe/socket và cụm I/O còn giản lược; CPU, RAM và GPU thực tế cắm theo hướng về mặt bo **−Z**. Không tạo thêm socket hoặc CPU từ hai trạng thái minh họa. Hình GPU ba rotor trong chú giải là mặt dưới, không phải hướng GPU khi lắp.
- Két tách cao hơn vị trí lắp; khi dựng phải hạ mặt trên xuống Y 304, đặt dưới nóc và giữ tổng dày 52. Các vít rời chỉ là ký hiệu, chưa thể hiện đủ từng lỗ; số lượng và tọa độ trong bảng mới là chuẩn. Ô quạt số 9 bị lặp trong chú giải, không tăng BOM.
