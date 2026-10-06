# 01 Standard tower — Tổng quan lắp ráp

Ảnh: [00_overview.png](00_overview.png). Nguồn số liệu: [đặc tả họ vỏ](../00_families_spec.md), mục 0, 1 và 7; quy ước linh kiện theo Gaming. Đây là kích thước mô phỏng, không phải bản vẽ gia công. **Số trong tài liệu là chuẩn; không đo kích thước từ ảnh tạo sinh.**

## Kích thước và tọa độ

Đơn vị mm. O ở tâm thân, không tính chân. X từ lưng (−) ra trước (+), Y từ đáy (−) lên nóc (+), Z từ hông kín (−) ra kính (+). Tọa độ dưới đây là vị trí **đã lắp**, không phải vị trí tách lớp. W × H × D theo Z × Y × X. Khi dựng: 100 mm = 1 đơn vị cảnh.

Thân **230 × 460 × 430**, biên X ±215, Y ±230, Z ±115. Tổng cao có chân **478**. Bốn chân 42 X × 18 Y × 28 Z, tâm (±175; −239; ±85).

| Số / cụm | Bao kích thước và mốc lắp |
|---|---|
| 1 Vỏ | Khung 1,2; tấm thép rời 1. Kính X −205…205, Y −218…218, Z 111…115. Nắp kín 410 X × 436 Y × 1 tại Z −115…−114. |
| 2 Bo ATX | PCB 244 X × 305 Y × 1,6 Z; gốc lưng PCB **B = (−79;45;−83)**. Mặt linh kiện +Z, I/O −X. Chín chân đế cao 6,5 trên khay Z −89,5. |
| 3 CPU | 37,5 × 37,5 × 4,5; gốc đáy (−60;104;−78,3), mặt IHS (−60;104;−73,8). Tiếp điểm phẳng, tam giác và hai rãnh định hướng. |
| 4 Tower air | Bao 90 X × 120 Y × 155 Z tính từ IHS: X −105…−15, Y 44…164, Z −73,8…81,2. Đế 40 × 40 × 5; bốn heatpipe Ø6; lá 0,3, bước 1,8. Quạt 120 × 120 × 25 tâm (−37,5;104;16,2), thổi −X. Backplate 90 × 90 sau bo; gá 78 × 78. |
| 5 RAM ×2 | DDR4 133,35 Y × 42 Z × 6 X; gốc đáy A2 (0;107;−77,5), B2 (19;107;−77,5). A1/B1 trống. |
| 6 GPU | Thân **280 X × 55 Y × 125 Z**, gốc A = (−71;−9,7;−68,4), không quay. Bao X −211…69, Y −37,2…17,8, Z −68,4…56,6. Ba rotor hướng xuống −Y, hút +Y; cắm thẳng x16. Chân PCIe nhô 8 về phía bo, lá cổng nhô 1 phía −X. |
| 7 SSD | Một M.2 2280, 80 X × 22 Y × tối đa 3; tâm chiếu (−44;−17), miệng M-key (−84;−17), chân đỡ cao 3 tại (−4;−17), trên mặt bo. |
| 8 PSU | ATX 140 X × 86 Y × 150 Z, tâm **(−142;−177;−15)**; quạt hút −Y, xả −X. C14/công tắc ở lưng, dây liền ra +X. |
| 9 Quạt case | **F1 140** tâm (187,5;−72;10); **F2 140** tâm (187,5;74;10); **F3 120** tâm (−199,5;128;28). Cả ba dày 25, thổi −X. F1/F2 hút trước, F3 xả sau. |
| 10 Dây | Đi sau khay trong Z −114…−90,5, sâu 23,5. Hai rãnh rộng 26 tại X −185 và 83, sáu đai; dây dư dưới shroud. R uốn nguồn ≥20, cách cánh quạt ≥10. |

Shroud: mặt trên Y −129, X −211…145, Z −89,5…109; vách bên Z 108…109; đầu trước hở. Khay trên X −211…145, Y −129…227; phần dưới chỉ X −69…145 để không xuyên PSU.

## Panel, cửa thoáng và I/O

- Cassette trước X 201…215, Y ±215, Z −109…89. Lưới tại X 211…211,8; lỗ lục giác rộng 3, bước 4. Cửa thông gió Z −84…82, Y ±200; lọc X 208,5. Dải I/O ở phía +Z, ngoài lưới.
- Nóc: cửa lưới X −168…142, Z ±88, lọc nam châm; thoát thụ động, **không có quạt/két**. Đáy: cửa hút PSU 124 × 124 tâm X −142, Z −15; lọc kéo −X.
- Lưng: một quạt 120, I/O bo, PSU và **7 PCIe ngang** bước 20,32. GPU dùng hai lá, còn năm nắp; thân 55 cần khoảng trống ba khe. Mặt AC nguồn X −212, lá I/O bo X −215,2…−214,4; giữ gờ đỡ riêng cho từng mặt.
- I/O trước hướng +X, tất cả tại X 215, Z 101: power Ø14/Y178; reset Ø4/Y147; headphone TRRS Ø3,5/Y124; mic TRS Ø3,5/Y103; hai USB-A miệng 13 Y × 6 Z tại Y69 và Y36. Không USB-C trước. PLED nằm trong vành power.
- I/O sau bo dùng mesh hiện tại trong `tools/build_gaming.py`: BIOS/CMOS, HDMI, DP, sáu USB-A, RJ45, USB-C, hai đầu anten, hai jack âm thanh, optical. Không dùng cụm I/O giản lược của blueprint cũ. GPU có ba DP và một HDMI.

## Dây và đầu cắm

Các G nằm trên khay Z −89,5; kích thước là lòng mở X × Y, thêm viền EPDM 3. Cửa bảo trì CPU 100 × 100 tâm (−60;104;−89,5), không luồn dây qua đó.

| G | Tâm X;Y | Lòng mở | Tuyến |
|---|---|---|---|
| G0 | −32;−174 | 60 × 28 | PSU vào khoang dây; luồn đầu ATX theo cạnh 60 |
| G1 | −139;212 | 34 × 12 | EPS qua mép trên bo |
| G2 | 65;102 | 24 × 64 | ATX24, quạt trước |
| G3 | −40;212 | 42 × 12 | CPU_FAN |
| G4 | 89;−9 | 28 × 30 | Nguồn GPU vòng ngoài đầu +X; kẹp tại (92;−25;55) |
| G5 | 23;−120 | 46 × 12 | USB19, F_PANEL |
| G6 | −176;−120 | 40 × 12 | HD_AUDIO, quạt sau |

Tọa độ header dưới đây là **cục bộ theo B**, cộng B để ra toàn máy. Nguồn GPU là ngoại lệ, ghi toàn máy.

| Đầu | Số chân / khóa | Mốc hoặc kết nối |
|---|---|---|
| ATX | 24, 2 × 12, ngàm; dây 20+4 | (117;40;14,6), PSU → G0 → G2 |
| EPS | 8, 4+4, khóa CPU | (−60;146;14,6), qua G1 |
| GPU power | 8, 6+2, khóa khác EPS | Toàn máy **(54;8,85;56,6)**, cửa +Z, qua G4 |
| CPU_FAN | 4 PWM, gờ dẫn hướng | (35;126;9,6), quạt tản qua G3 |
| SYS1 / SYS2 / SYS4 | Mỗi đầu 4 PWM | F1 → (106;85;9,6); F2 → (82;−128;9,6); F3 → (−96;−135;9,6) |
| USB19 | Khung 20, khuyết chân 20 | (113;−127;10,6); hai USB-A trước, mỗi cổng 9 tiếp điểm |
| HD_AUDIO | Khung 10, khuyết chân 8 | (−97;−143;9,1); headphone 4 và mic 3 tiếp điểm |
| F_PANEL | Khung 10, khuyết chân 10 | (95;−143;9,1); PLED 2+/4−, power 6/8, reset 5/7; chân 1/3/9 không dùng ở vỏ này |
| Khe linh kiện | DDR4 288, rãnh lệch; x16 164, có chốt; M-key 67/75 vị trí | A2/B2, khe PCIe chính B+(−34;−30), một M.2 |
| Điện ngoài | C13 ghép C14, 3 tiếp điểm có đất | Cắm ở lưng PSU; không qua G0–G6 |

AIO_PUMP, SYS3 và ARGB trên bo để trống. Không có Type-E trên bo ATX, hub, riser hoặc cáp SATA. Dây ATX/GPU dài 600, EPS 650, I/O và quạt case tối đa 650. Đầu DP có 20 tiếp điểm, HDMI 19, RJ45 8, USB-C 24; USB-A sau có hai cổng 4 và bốn cổng 9 tiếp điểm. Màu đường nét đứt chỉ dùng chú giải, dây thật màu đen.

## Vít, vật liệu và hình chiếu

| Cụm | Vít / vị trí |
|---|---|
| Bo | 9 M3 × 5, 9 chân đế M3 cao 6,5; lỗ Ø3,4 tại tổ hợp X −191/−79/33, Y −97,5/45/187,5 |
| Tản | 4 M3 × 12 lò xo tại tâm CPU X/Y ±39; quạt giữ bằng **hai kẹp Ø1,2**, không vít xuyên lá |
| Quạt case | 12 vít Ø5 × 10; mỗi quạt bốn lỗ trên mặt YZ, lệch tâm ±62,25 cho 140 và ±52,5 cho 120 |
| GPU / nắp PCIe | 2 + 5 vít #6-32 × 6; tái dùng hai vít nắp cho GPU |
| PSU / SSD | PSU 4 #6-32 × 6 theo mesh; SSD 1 M2 × 2,5 vào chân đỡ cao 3 |
| Kính / hông thép | Kính 4 vít tay có đệm tại X ±190, Y ±203; hông thép 2 vít tay tại X −199, Y ±198 |
| Cassette / shroud / chân | Trước 2 M3 × 6 tại (213;−207;−98/78), thêm sáu ngàm; shroud 2 M3 × 6 tại X −170/115, Y −129, Z98; chân 4 M4 × 8 chìm |

Nóc, đáy, lưng cố định và lọc nam châm: 0 vít tháo. Vít panel nhô tối đa 2, kính tối đa 3; kích thước bao thân không cộng đầu vít.

Thép sơn nhám `#24262A` (roughness 0,65; metallic 0,25); lưới 0,8 và men kính `#101216`. Kính khói 4 mm `#B8C4CA`, truyền sáng mục tiêu 75%, IOR 1,5, roughness 0,08; men viền 10 và gioăng 1. Cao su/nhựa `#202329`; kim loại linh kiện `#AEB4BC`, PCB `#16181D`, tiếp điểm `#C6A565`, dây `#15171B`. Không ARGB. Lọc PET 0,5, mắt mở 0,35, sợi 0,15, viền nylon 1,5.

Hình lớn: tách lớp 3/4 từ +X/+Y/+Z, mũi tên lắp, đủ nhóm 1–10. Hình nhỏ: FRONT nhìn −X (I/O bên trái ảnh), BACK nhìn +X, LEFT nhìn −Z (mặt trước bên phải), RIGHT nhìn +Z, TOP nhìn −Y (trước ở dưới), BOTTOM nhìn +Y (trước ở trên). Sáu mặt cùng tỷ lệ; hình sau khay minh họa G0–G6 và tuyến dây.

## Người mới cần chú ý

1. Có **3 quạt case + 1 quạt CPU**; ba rotor GPU và quạt trong PSU tính riêng. Nóc không lắp quạt.
2. GPU nằm ngang, quạt quay xuống, cắm trực tiếp x16. Lắp SSD trước GPU; thân card cần ba khoảng khe dù chỉ tháo hai nắp.
3. Gió trước → tản → sau; PSU hút riêng qua đáy. Không bịt lưới và cửa lọc bằng tấm kín.
4. Đỡ kính khi tháo bốn vít có đệm. EPS 4+4 và GPU 6+2 có khóa khác nhau, không đổi chỗ.
5. Tản cách kính 29,8; GPU cách quạt trước 106. Giới hạn chọn: GPU dài 350, dày 60, nhô Z130; tản cao 175 từ IHS. Chỉ dựng cấu hình tower air của sheet này.

## Kiểm tra ảnh

Ảnh tạo bằng công cụ imagegen tích hợp, dùng concept `design/case-families/01_standard_tower.png` làm tham chiếu dáng. Prompt yêu cầu một sheet tách lớp 3/4, sáu hình chiếu cùng tỷ lệ, sơ đồ dây, nhãn 1–10, kích thước xanh và cấu hình ATX / tower155 / GPU280 ngang / ATX PSU / 2×140 trước + 1×120 sau. Đã sửa ảnh một lượt theo brief: kích thước RIGHT, mặt PSU, socket trống và hướng quạt GPU.

**Sai lệch còn lại trong ảnh, cần sửa khi dựng:**

- Hình tách lớp ghi nhầm **448 (body)**: đúng là **460**. Tổng cao **478** đã gồm chân 18, không cộng thêm 18. Chưa có đủ ba kích thước bao trên riêng hình lớn; lấy W230/H460/D430 từ bảng.
- RIGHT đã ghi đúng 430 nhưng bị vẽ hẹp hơn LEFT; các hình nhỏ chưa hoàn toàn cùng tỷ lệ. TOP bị tô kín, phải có cửa lưới/lọc 310 × 176 theo tọa độ ở trên. BOTTOM giản lược lọc quá lớn; cửa hút PSU đúng là 124 × 124 ở phía sau.
- PSU tách lớp lộ mặt AC về phía trước ảnh: khi lắp phải xoay mặt C14/công tắc về **−X**, dây ra **+X**, quạt xuống **−Y**. Backplate tản đang đặt cạnh CPU để nhìn rõ; vị trí thật ở **sau PCB**. Mũi tên tách lớp chỉ minh họa, không dùng làm đường thao tác chính xác.
- Nắp thép −Z chưa được tách rõ ở hình lớn, chỉ có mặt RIGHT. Một số đầu vít minh họa bị lặp/dư quanh kính và cassette; dùng đúng **4 vít kính**, **2 vít hông thép**, **2 vít đáy cassette** và bảng vít, không đếm nét trong ảnh.
- Hình thu nhỏ không thể hiện đủ chính xác từng chân, lỗ và cụm I/O sau 17 vị trí; lỗ x16/M-key, bảy khe PCIe và G0–G6 phải dựng theo số liệu. Nhãn G3 bị rút ngắn, nghĩa là CPU_FAN. GPU/PSU trong dải BOM là hình nhận diện bộ phận, không phải hướng lắp.
