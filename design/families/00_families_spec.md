# Đặc tả tổng thể sáu họ vỏ PC SmartCare

Ngày chốt: 03/10/2026. Đơn vị: **mm**. Đây là thiết kế kích thước cho mô hình mô phỏng, không phải số đo suy ra từ ảnh AI hoặc bản vẽ gia công sản phẩm thương mại. Sáu ảnh `design/case-families/01…06` quyết định dáng, vật liệu và bố cục nhìn thấy; các số dưới đây quyết định hình học khi dựng.

Tạo **sáu bộ vỏ mới**, kể cả `standard_tower`. Bộ Gaming hiện có tiếp tục là một bộ độc lập. Không sửa, thu phóng hoặc đổi tên vỏ Gaming để thay cho bất kỳ họ nào trong tài liệu này. Phân nhóm ngoại hình độc lập với nhu cầu Văn phòng/Học tập/Gaming/Đồ họa, theo `design/case-families/PHAN_CHIA_MODEL.md`.

## 0. Quy ước chung và linh kiện giữ lại

### 0.1. Trục, kích thước và mốc lắp

- Gốc **O = (0; 0; 0)** ở tâm bao thân vỏ, không tính chân/đế. **X: lưng (−) → trước (+); Y: đáy (−) → nóc (+); Z: hông kín (−) → hông mở/kính (+)**. Với vỏ kín cả hai hông, +Z vẫn là phía mở để lắp bo. Với SFF sandwich, GPU có thể nằm phía −Z nhưng không đổi tên trục.
- `W × H × D` luôn là **rộng theo Z × cao theo Y × sâu theo X**. Thân có biên X = ±D/2, Y = ±H/2, Z = ±W/2. Kích thước bộ phận trong bảng sẽ ghi rõ trục nếu không dùng thứ tự này.
- Tọa độ `(X; Y; Z)` là tọa độ toàn máy ở trạng thái **đã lắp**. Vị trí tách lớp trên bản vẽ không phải tọa độ lắp. Khoảng ghi `a…b` bao gồm hai biên. Dùng dấu phẩy cho phần thập phân.
- Giữ quy đổi Gaming: **100 mm = 1 đơn vị cảnh**. Dựng bằng mm rồi dùng đúng phép đổi trục/đơn vị của `tools/glib.py`; không coi một đơn vị app là một mét. Giữ các ID `case`, `motherboard`, `cpu`, `cooler`, `ram`, `gpu`, `ssd`, `psu`, `fans`, `cables`; mốc lắp tên `mount_<id>`, thêm hậu tố cho từng quạt/RAM. Mỗi họ có namespace riêng.
- Tâm bo là tâm hình chữ nhật PCB **trên mặt lưng PCB**, không phải tâm khối có đầu cắm. PCB dày 1,6 về +Z cục bộ. Tất cả bo trong sáu cấu hình dùng cùng hướng: cạnh I/O về −X, cạnh EPS về +Y, mặt linh kiện về +Z. Không lật gương PCB.
- `B` chỉ mốc bo. CPU dùng `B + (uCPU; vCPU; 4,7)` làm gốc đáy CPU; mặt IHS ở Z cục bộ **9,2**. Gốc tản khí/bơm nằm trên mặt IHS. RAM dùng gốc đáy thanh ở Z cục bộ 5,5; SSD giữ cách lắp và chân đỡ của Gaming.
- Các hộp bao GPU dưới đây là **thân tản/backplate**, chưa gồm chân PCIe nhô 8, lá cổng nhô 1 và đầu vít nhô tối đa 3. Bản vẽ phải thể hiện cả phần nhô. Tâm GPU theo mã Gaming nằm giữa chiều dài/chiều dày, nhưng Z cục bộ = 0 ở cạnh PCB gần bo; thân chạy Z cục bộ 0…125. Không đặt gốc nhầm vào tâm hộp 280 × 55 × 125.
- `R0(x;y;z)=(x;y;z)`, `R+(x;y;z)=(x;−z;y)` là quay +90° quanh X, `R−(x;y;z)=(x;z;−y)` là quay −90° quanh X. Điểm lắp sau quay: `P = A + R(p)`. Dùng đúng công thức cho GPU và PSU; không đổi thứ tự Euler tùy phần mềm.
- Mọi kích thước clearance là **giới hạn chọn cho cấu hình mô phỏng**, có khoảng thao tác theo bảng, chưa phải kết quả thử nhiệt hoặc xác nhận tương thích mọi sản phẩm ngoài thị trường.

### 0.2. Đối chiếu Gaming và tái sử dụng

Đã đối chiếu `design/gaming/00_blueprint.md`, `02_motherboard.md`, `12_case_front.md`, `13_case_top.md`, `src/data/gaming.ts` và `tools/build_gaming.py`. Các bản Gaming ghi những giai đoạn khác nhau: blueprint ban đầu có quạt trước 120, mã hiện tại dùng 140; bo hiện tại đã có header ARGB và cụm I/O sau mới. **Tái sử dụng hình học linh kiện hiện tại**, không dựng lại bo theo hình minh họa cũ có ít cổng hơn. Tọa độ đặt vỏ mới lấy ở tài liệu này; không sao chép vị trí `BOARD` của Gaming.

| Linh kiện giữ nguyên | Bao kích thước / mốc quan trọng | Dùng trong họ |
|---|---|---|
| CPU LGA chung | 37,5 × 37,5 × 4,5; mặt dưới phẳng, tam giác/rãnh khóa; cùng socket mô phỏng và lưới gá tản 78 × 78 | 01–06, mỗi bộ 1 |
| DDR4 | 133,35 dài theo Y × 42 cao theo Z × 6 dày theo X; không đổi rãnh khóa DDR4 | 01–06, mỗi bộ 2 |
| SSD M.2 2280 | 80 theo X × 22 theo Y × tối đa 3; M-key, một vít M2 × 2,5 | 01–06, mỗi bộ 1 |
| Bo ATX hiện tại | PCB 244 × 305 × 1,6; chín lỗ theo lưới Gaming; CPU (19;59), RAM A2 (79;62), B2 (98;62), PCIe chính (−34;−30), SSD (35;−62) | 01, 03, 06 |
| PSU ATX | 140 theo X × 86 theo Y × 150 theo Z; gốc ở tâm; hút −Y, AC/C14 phía −X, dây liền ra +X | 01, 02, 03, 06 |
| GPU ba quạt | 280 theo X × 55 theo Y × 125 theo Z; mặt quạt −Y; gốc/đầu nguồn giữ theo mã | 01, 03, 06 |
| AIO 240 | Bơm Ø54 × 48; két 277 × 120 × 27, hai quạt 120 × 120 × 25; gốc két giữa **mặt trên**, treo xuống −Y; hai ống Ø11 dài 350 | 03 mặc định; 01 chỉ là khả năng thay tản |
| Quạt case | 120 hoặc 140, dày 25; bước lỗ 105 hoặc 124,5; bản đen hoặc vòng cyan | Theo bảng từng họ |

Bo ATX giữ nguyên đầu cắm hiện tại: ATX24 (117;40;14,6), EPS (−60;146;14,6), CPU_FAN (35;126;9,6), AIO_PUMP (10;126;9,6), SYS1 (106;85;9,6), SYS2 (82;−128;9,6), SYS3 (95;−134;9,6), SYS4 (−96;−135;9,6), F_PANEL (95;−143;9,1), USB19 (113;−127;10,6), AUDIO (−97;−143;9,1), ARGB (55;−143;9,1). Không thêm Type-E lên bo ATX. Lá chắn I/O hiện tại có X cục bộ −136,2…−135,4, Y −18…140, Z 1,6…37,6; khung vỏ phải khoét theo lá này, không theo vị trí −122 của tài liệu cũ.

### 0.3. Vật liệu, lỗ và vít chung

| Mã | Vật liệu / màu | Cách dựng |
|---|---|---|
| M1 | Thép sơn nhám `#24262A` | Vỏ 01/02/03/06; roughness 0,65, metallic 0,25; khung gấp dày 1,2, tấm rời dày 1,0; cạnh ngoài R1 trừ nơi ghi khác |
| M2 | Thép lưới `#101216` | Dày 0,8; lỗ tròn Ø2,4 bước 3,6 so le, chừa mép đặc 6 và vùng vít bán kính 5. Mặt trước 01/06 dùng lỗ lục giác rộng qua hai cạnh 3, bước 4 để gần concept |
| M3 | Kính khói `#B8C4CA` | Dày 4, IOR 1,5, roughness 0,08; truyền sáng mục tiêu 75%; men đen `#101216` rộng 10 ở viền; gioăng EPDM 1 |
| M4 | Nhôm xước graphite `#45474A` | 04: dày 2, metallic 0,8, roughness 0,38; vân dọc Y ở mặt trước, dọc X trên hông |
| M5 | Thép sơn graphite `#343639` | 05: tấm 1,0, khung 1,2; roughness 0,55; mặt trước bo R8, thân ngoài R6 |
| M6 | Nhựa/cao su đen `#202329` | Grommet, lọc, chân; cao su roughness 0,85; không dùng làm tấm chịu lực |
| M7 | Kim loại sáng `#AEB4BC`, PCB `#16181D`, nhựa cổng `#101216` | Linh kiện mới theo Gaming; tiếp điểm `#C6A565`; USB-A lưỡi xanh `#1F6FD1` |
| M8 | Cyan `#22D3EE` | Chỉ đèn đã ghi; vòng cố định, không quay cùng cánh. Không thêm logo hay ánh sáng cầu vồng |

Lọc PET dày 0,5, mắt mở 0,35, sợi 0,15; cassette viền nylon dày 1,5. Lọc luôn nằm trong hốc của bao thân trừ chân/đế, không cộng ngầm vào kích thước. Mỗi cửa thoáng có viền/gân đỡ; lỗ là khoảng rỗng thật hoặc alpha có độ dày viền phù hợp ở LOD xa, không phải chấm sơn trên tấm kín.

Vít khay/bo M3 × 5, chân đế M3 cao **6,5**; lỗ bo Ø3,4. Mỗi quạt case có 4 vít tự tạo ren Ø5 × 10; mỗi lá PCIe một vít #6-32 × 6, tái dùng vít nắp cho lá GPU. PSU ATX giữ bốn lỗ/vít theo mesh; PSU SFX có bốn lỗ riêng tại mục 7. Két dùng 4 vít ngắn M3 × 6 và 4 vít dài M3 × 30 cho mỗi quạt; ray nóc tại vít có bậc dày hiệu dụng 3 để giữ độ ăn ren giống Gaming. Không để vít dài chọc vào ống nước.

Mặt lưng không phải một tấm phẳng bị ép trùng mọi lá cổng: làm cổ/gờ gấp từ vỏ tới đúng mặt lá GPU và PSU theo tọa độ đã lắp, chừa cửa cho đầu vít. PSU ATX có mặt AC lùi 3 so với biên sau thân; lá GPU lùi 3–4; lá I/O ATX nhô 0,2 ngoài biên sau, được kê trong rãnh riêng. Giữ các mốc này khi dựng khung, không dịch bo/GPU để ép tất cả về cùng X. Nguồn SFX trước máy dùng giá chữ L dày 1,2, mặt đỡ áp mặt AC X=50 ở họ 04 hoặc X=47 ở họ 05; khoét thoáng mặt xả và cửa C14, bốn vít khớp nguồn.

Với panel chữ nhật có bốn vít, mặc định tâm vít cách hai mép tương ứng **15**; với hai vít giữ nắp trượt, tâm ở mép sau, cách đầu trên/dưới **20**. Đầu vít nổi tối đa 2 ngoài bao thân, riêng vít tay kính tối đa 3. Bảng kích thước tổng chân/đế không cộng đầu vít; khi cần bao vận chuyển, cộng phần nhô ở đúng mặt. Khung hàn/tán cố định và cửa lọc nam châm ghi **0 vít tháo**. Chân: một vít M4 × 8 mỗi chân, nằm chìm.

### 0.4. Dây và cửa PCIe

G0: nguồn vào vùng dây; G1: EPS; G2: ATX24; G3: quạt/tản; G4: GPU; G5: USB/F_PANEL; G6: audio. Mỗi mã có tiền tố họ, ví dụ `03-G2`; không ngụ ý các họ có cùng tọa độ. Kích thước lỗ dưới đây là **lòng mở**, thêm viền EPDM rộng 3. Cửa CPU riêng, không đi dây qua cửa này. Dây nguồn đen `#15171B`, uốn R≥20; trong 04/05 dùng dây mềm ngắn R≥12. Riser R≥15, ống AIO R≥30; cách cánh quạt ≥10 sau khi buộc.

Luồn đầu ATX24 qua cạnh 60 của G0 tower hoặc cạnh 58 của G0 SFF/slim; không cố nhét đầu 52 mm qua bề rộng ngắn. Lỗ riser 96 cho đầu đực 89 đi qua trước, đầu adapter cái 110 giữ nguyên ở khoang GPU rồi mới bắt vào giá. Viền cao su tháo lắp được, dây không bị kẹp giữa panel và khung. Các chiều dài dây không ghi riêng dùng ATX 600, EPS 650, GPU 600, I/O 650, quạt case 650 tối đa; bó phần dư ở khoang đã chỉ, không rút thẳng qua vùng quạt.

Vỏ có đường ray PCIe theo bước **20,32**. Với GPU ba quạt R0, mép trên dãy nắp = `B.y − 24`; dãy chạy xuống −Y. Với GPU hai quạt R0, mép trên = `B.y + vPCIe + 6`. Mỗi nắp chừa khe 1,5 với nắp kế. Hộp GPU dày 55 hoặc 45 vẫn cần **ba khoảng khe trống bên trong**, dù lá cổng chỉ bắt hai nắp; không đặt card khác ngay dưới. Các cấu hình riser dùng **cassette ba khe dọc cố định**, không vẽ thêm bảy khe ngang xuyên qua cùng vùng.

## 1. 01 — `standard_tower` / Tower tiêu chuẩn

### 1.1. Vỏ và các mặt

**Thân 230 × 460 × 430**: X ±215, Y ±230, Z ±115. Bốn chân 42 theo X × 28 theo Z × 18 cao, tâm `(±175; −239; ±85)`; tổng đặt bàn **230 × 478 × 430**. Đây là tower mới mặt lưới, hai quạt trước lớn, không dùng mặt kính trước của Gaming.

| Mặt / panel | Bao, kiểu dáng và giữ panel |
|---|---|
| Trước +X | Cassette 198 rộng theo Z × 430 cao, X 201…215, Y −215…215, Z −109…89; lưới M2 ở X 211…211,8, khung M1. Cửa hữu dụng Z −84…82, Y −200…200. Lọc sau lưới tại X 208,5. Sáu ngàm + **2 M3 × 6** dưới mép; tâm vít `(213;−207;−98)` và `(213;−207;78)`, trục X. Dải I/O nằm ngoài lưới về +Z |
| Hông +Z | Kính M3: X −205…205, Y −218…218, Z 111…115; **4 vít tay có đệm**, tâm X ±190, Y ±203; tháo theo +Z |
| Hông −Z | Thép kín 410 × 436 × 1 tại Z −115…−114; gập gờ vào trong; **2 vít tay** ở X −199, Y ±198, đầu hướng −Z; trượt −X rồi nhấc |
| Nóc | M1; cửa lưới X −168…142, Z −88…88, lọc nam châm phẳng; không gân X trang trí của Gaming. Panel nóc cố định **0 vít tháo**, lọc **0** |
| Đáy | M1 cố định **0 vít tháo**; hút PSU 124 × 124 tâm X −142, Z −15; lọc kéo ra −X. Bốn chân giữ bằng **4 M4** |
| Lưng | Cửa I/O theo B, quạt sau, **7 PCIe ngang**, cửa PSU. Tấm lưng cố định **0 vít tháo**; bảy nắp/bảy vít khi case trống, còn năm nắp khi lắp GPU |

### 1.2. Linh kiện và thông gió

| Cụm | Vị trí / hướng / bao đã lắp |
|---|---|
| Bo | ATX, **B = (−79;45;−83)**; PCB X −201…43, Y −107,5…197,5; mặt linh kiện +Z; khay mặt trước Z −89,5; 9 chân đế |
| CPU, RAM, SSD | Một CPU và SSD, hai RAM A2/B2 theo tọa độ bo giữ lại. IHS CPU tại **(−60;104;−73,8)** |
| Tản CPU | `tower_air_155`, đế tại IHS; bao X −105…−15, Y 44…164, Z −73,8…81,2; quạt CPU 120 tâm **(−37,5;104;16,2)**, hút từ +X, thổi **−X** qua lá tản |
| GPU | GPU 280, R0, gốc **A = (−71;−9,7;−68,4)**; thân X −211…69, Y −37,2…17,8, Z −68,4…56,6. Quạt quay xuống −Y, cắm trực tiếp x16; hai vít lá sau |
| PSU | ATX, R0, tâm **(−142;−177;−15)**; thân X −212…−72, Y −220…−134, Z −90…60; hút đáy −Y, thổi ra −X. Quạt nguồn 120 tâm `(−142;−208;−15)`, nằm trong nguồn |
| Nắp che PSU | Mặt trên Y −129, X −211…145, Z −89,5…109, dày 1; vách +Z tại 108…109. Hai vít M3 × 6, tâm X −170/115, Y −129, Z 98; đầu trước hở lối vào, không che hai quạt trước |

| Quạt rời / két | Kích thước | Tâm | Luồng / dây |
|---|---|---|---|
| F1 trước dưới | 140 × 140 × 25 | **(187,5;−72;10)** | Hút qua lưới, thổi −X; SYS1 |
| F2 trước trên | 140 × 140 × 25 | **(187,5;74;10)** | Hút −X; SYS2 |
| F3 sau | 120 × 120 × 25 | **(−199,5;128;28)** | Xả −X; SYS4 |
| Két mặc định | Không | — | Nóc thoáng thụ động, không vẽ quạt/két ẩn |

Ba quạt case và quạt tản CPU đen, không ARGB. GPU giữ ba rotor có tâm theo mesh: A + (−90/0/90; −19,5; 62,5), hút theo +Y vào card; PSU có quạt tích hợp riêng. Không tính rotor GPU/PSU vào số quạt case.

### 1.3. I/O, dây và khoảng hở

I/O trên dải trước tại X = **215**, cửa hướng +X: Power Ø14 tâm **(215;178;101)**; Reset Ø4 `(215;147;101)`; headphone TRRS Ø3,5 `(215;124;101)`; mic TRS Ø3,5 `(215;103;101)`; USB-A ×2 tâm `(215;69;101)` và `(215;36;101)`, miệng 13 theo Y × 6 theo Z. Không USB-C. Hai jack dùng một module âm thanh hai ổ vào HD_AUDIO; power/reset vào F_PANEL, PLED tích hợp vành power. Module nằm X 202…213, Z 91…109, không che quạt kết thúc Z 80.

Khay dày 1: phần trên X −211…145, Y −129…227; phần kéo xuống khoang nguồn chỉ ở X −69…145, Y −227…−129. Không dựng tấm khay xuyên qua PSU ở phía sau. Vùng dây sau khay Z −114…−90,5, sâu 23,5; hai rãnh đứng rộng 26 tại X −185 và 83, sáu đai. Grommet trên mặt khay Z = −89,5:

| Mã | Tâm (X;Y;Z) | Lòng mở X × Y | Đường đi |
|---|---|---|---|
| 01-G0 | (−32;−174;−89,5) | 60 × 28 | PSU → sau khay |
| 01-G1 | (−139;212;−89,5) | 34 × 12 | EPS qua mép trên bo |
| 01-G2 | (65;102;−89,5) | 24 × 64 | ATX24, dây quạt trước |
| 01-G3 | (−40;212;−89,5) | 42 × 12 | CPU_FAN; dùng lại khi thay AIO |
| 01-G4 | (89;−9;−89,5) | 28 × 30 | GPU: chạy từ sau khay ra phía đầu +X của card rồi vòng lên đầu 8 chân, không băng qua mặt quạt |
| 01-G5 | (23;−120;−89,5) | 46 × 12 | USB19, F_PANEL |
| 01-G6 | (−176;−120;−89,5) | 40 × 12 | HD_AUDIO, quạt sau |

Cửa CPU 100 × 100 tâm `(−60;104;−89,5)`. Dây dư ở khoang PSU. G4 dùng kẹp tại `(92;−25;55)` để giữ vòng dây ngoài đầu card; cửa nguồn GPU đúng gốc A + (125;18,55;125), hướng +Z. Không tái dùng ống G4 Gaming vốn đặt theo đầu cắm cũ hướng +X.

**Giới hạn:** GPU dài tối đa **350**, dày 60, nhô từ cạnh gốc Z tối đa 130; tản khí cao tối đa **175 từ IHS**. GPU mẫu kết thúc X 69, quạt trước bắt đầu X 175, hở 106; ở giới hạn 350 còn 36. Tản mẫu tới Z 81,2, cách kính trong Z 111 là 29,8. Hai quạt trước cách lưới tối thiểu 11. Tùy chọn thay tản: **AIO 240 nóc duy nhất**, gốc mặt trên `(−11,5;224;35)`, bao két/quạt X −150…127, Y 172…224, Z −25…95; không lắp đồng thời tản tower. Không hỗ trợ két trước/280/360 trong phiên bản này.

**Người mới cần thấy:** nguồn dưới nắp che hút riêng qua đáy; quạt trước đưa gió vào, quạt sau đưa ra; GPU nằm ngang và cắm thẳng vào bo; tháo kính có bốn vít và phải đỡ kính. Lưới trước của họ 01 không phải kính như Gaming.

## 2. 02 — `compact_tower` / Tower nhỏ gọn

### 2.1. Vỏ và các mặt

**Thân 210 × 380 × 370**: X ±185, Y ±190, Z ±105. Bốn chân tròn Ø26 cao 14, tâm `(±150;−197;±78)`; tổng **210 × 394 × 370**. Hai hông kín, mặt trước phẳng bo cạnh R8; giữ dáng thấp của concept, không thu nhỏ bo ATX.

| Mặt / panel | Kiểu và vít |
|---|---|
| Trước | M1; cassette X 172…185, Y −181…181, Z −99…99; tấm đặc chính dày 1. Bốn cột khe ngang: tâm Z = −60/−20/20/60, mỗi khe dài 30 theo Z, cao 5, đầu R2,5; bảy hàng Y = −154 + 14j, j=0…6. Hai khe hút cạnh rộng 6 tại Z ±94, Y −155…145, thông vào plenum trước X 171…181. Lọc phủ cả khe thấp và khe cạnh. Sáu ngàm, **2 M3 × 6** ở `(182;−170;±84)` |
| Hông ±Z | M1 kín, 350 theo X × 356 theo Y × 1; mỗi nắp **2 vít tay**, ở X −169, Y ±158; rãnh trượt, không cửa sổ/kính |
| Nóc | M1, đục lỗ X −145…125, Z −82…82, lọc nam châm; cố định **0 vít tháo**. I/O phía trước vùng lỗ |
| Đáy | M1 cố định, **0 vít tháo**; lọc PSU 124 × 124 tại X −112, Z −7; **4 M4** giữ chân |
| Lưng | Cố định, **0 vít tháo**; mATX I/O, một quạt 120, **4 PCIe ngang**, PSU ATX. Bốn nắp/bốn vít khi trống, còn hai nắp khi có GPU |

### 2.2. Lắp ráp và quạt

| Cụm | Đặc tả |
|---|---|
| Bo | `motherboard_matx`, B = **(−49;54;−69)**; PCB X −171…73, Y −68…176. Khay Z −75,5, chín chân đế. CPU cục bộ (0;50), PCIe chính (−34;−61) |
| CPU/RAM/SSD | IHS **(−49;104;−59,8)**; hai DDR4 A2/B2 và M.2 theo bảng bo mới |
| Tản | `tower_air_155`, bao X −94…−4, Y 44…164, Z −59,8…95,2. Quạt CPU 120 tâm **(−26,5;104;30,2)**, thổi −X |
| GPU | `gpu_240_dual`, R0, A = **(−61;−26,7;−54,4)**; thân X −181…59, Y −49,2…−4,2, Z −54,4…65,6; hai quạt quay xuống −Y; cắm trực tiếp x16 |
| PSU | ATX R0 tâm **(−112;−143;−7)**; thân X −182…−42, Y −186…−100, Z −82…68. Hút đáy; quạt nguồn tâm `(−112;−174;−7)` |
| Che PSU | Mặt trên Y −92, X −181…118, Z −75,5…100; **2 M3 × 6** tại X −145/90, Z 89. Bo dừng ở Y −68, không xuyên nắp che |
| F1/F2 trước | Hai quạt **120 × 120 × 25**, tâm **(157,5;−82;12)** và **(157,5;43;12)**, hút/thổi −X; SYS1/SYS2. Quạt trên lấy gió từ hai khe cạnh và khoang mặt trước, không xuyên qua phần tôn đặc |
| F3 sau | **120 × 120 × 25**, tâm **(−169,5;105;30)**, xả −X; SYS3 |
| Nóc / két | Không quạt nóc, không két; nóc thoát thụ động |

Tất cả quạt đen. Hai rotor GPU có tâm A + (−60/60;−12;60), hút +Y. Tản CPU vào CPU_FAN; không hub trong họ 02.

### 2.3. I/O, dây và khoảng hở

I/O hướng +Y trên nóc, X = **157**, Y = **190**: power Ø14 ở Z −58; USB-A ở Z −20 và 17 (miệng 6 theo X × 13 theo Z); headphone combo Ø3,5 ở Z 52. Không reset, không USB-C, không jack mic rời. Mặt nút bằng nóc, module sâu tối đa 14, X 149…165 nên không chạm quạt trước có đỉnh Y 103. PLED là vòng trắng dịu `#D2D7DD`.

Khay dày 1: phần trên X −181…125, Y −92…188; phần dưới chỉ X −39…125, Y −187…−92, chừa trọn bao PSU phía sau. Sau khay Z −104…−76,5, sâu 27,5; hai rãnh 24 tại X −152 và 102, sáu đai. Cửa CPU 100 × 100 tâm `(−49;104;−75,5)`. G1/G3 là rãnh chữ U mở lên mép khay Y 188, viền cao su cạnh dày 1, không thêm viền bắc ngang cửa; luồn đầu dây qua mép trước khi bắt bo, rồi cắm EPS/quạt.

| Mã | Tâm | Lòng mở X × Y |
|---|---|---|
| 02-G0 | (5;−137;−75,5) | 60 × 28 |
| 02-G1 | (−109;182;−75,5) | 32 × 12, rãnh mở lên mép khay |
| 02-G2 | (96;88;−75,5) | 24 × 60 |
| 02-G3 | (−13;182;−75,5) | 36 × 12, rãnh mở lên mép khay |
| 02-G4 | (104;−28;−75,5) | 28 × 28 |
| 02-G5 | (40;−82;−75,5) | 42 × 12 |
| 02-G6 | (−141;−82;−75,5) | 38 × 12 |

Đường đi G0→G1 EPS, G0→G2 ATX, G0→G4 GPU; dây I/O xuống mép trước vào G2 rồi ra G5/G6. F1/F2 qua G2; F3 qua G6; CPU_FAN qua G3. Dây GPU vòng ở phía X > 64 rồi về ổ nguồn trên cạnh +Z, không đặt bó dây giữa đầu quạt tản CPU và nắp hông.

**Giới hạn:** GPU **280 × 120 × 50**, tản cao **160 từ IHS**, không hỗ trợ radiator trong bản này. GPU mẫu kết thúc X 59, mặt quạt trước X 145, hở 86; GPU dài 280 còn 46. Tản mẫu tới Z 95,2, nắp trong Z 104, hở 8,8. Khay còn 14 giữa mép bo dưới và nắp che; dãy bốn khe PCIe dừng Y −82,28, cao hơn nắp che 9,72. Không thêm quạt nóc 120 vào hình: nó sẽ tranh vùng đầu tản tower hiện tại.

**Người mới cần thấy:** hông thép phải mở mới thấy linh kiện; micro-ATX ngắn hơn ATX, số khe sau ít hơn; mặt trước đặc vẫn cần cửa hút cạnh/khe thấp; nguồn ATX không đồng nghĩa bo phải là ATX.

## 3. 03 — `showcase_dual_chamber` / Showcase hai khoang

### 3.1. Vỏ và hai khoang

**Thân 300 × 450 × 450**: X ±225, Y ±225, Z ±150. Bốn chân vát M1 có đệm M6, 50 theo X × 36 theo Z × 20 cao, tâm `(±180;−235;±112)`; tổng **300 × 470 × 450**. Kính trước và hông gặp nhau ở cạnh +X/+Z, không dựng cột đặc dày che góc nhìn như tower thường.

| Mặt / panel | Kiểu và vít |
|---|---|
| Hông +Z | Kính M3 X −215…220, Y −211…211, Z 146…150; dán lên cassette viền M1, hai chốt đáy và **2 M3 × 6** ẩn mép nóc tại `(−185;214;141)` và `(190;214;141)`; không vít xuyên giữa kính |
| Trước +X | Kính M3 X 221…225, Y −211…211, Z −24…145; khoang sau −Z được che bởi dải M1 đặc. Hai chốt đáy + **2 M3 × 6** ẩn phía trên tại `(218;214;−12/132)`; mép nối kính có gioăng 1, không thông gió qua kính |
| Hông −Z | M1 tại Z −150…−149; cửa PSU X −215…−85, Y −170…−20; cửa cấp gió side-bank X 78…207, Y −112…147, đều lưới M2/lọc. Nắp **2 vít tay** tại X −209, Y ±190 |
| Nóc | M1, cửa lưới X −190…180, Z −8…133; cassette nóc **2 M3 × 6** phía sau `(−213;225;−15/132)` + ngàm trước; lọc **0 vít**. Giá két bên trong độc lập, không nhấc két khi tháo lọc |
| Đáy | Lưới/lọc X −197…187, Z −1…129 cho ba quạt; tấm đáy cố định **0 vít tháo**; lọc kéo +X, **4 M4** giữ chân |
| Lưng | I/O ATX, quạt sau, AC PSU phía khoang −Z, **3 PCIe dọc** tại mục 3.2; tấm cố định **0 vít tháo**. Không có thêm dãy 7 khe ngang |

Khay/vách phân khoang mặt trước Z **−36,5**, mặt sau −37,7. Khoang phụ Z −149…−37,7; khoang trưng bày Z −36,5…146. Phần khay mang bo ở X −221…74; phần X 78…207 là ống gió cấp cho hai quạt bên, **không bị tấm khay kín bịt phía sau**. Thành ống gió dày 1 ở hai mép X và hai đầu Y, chạy Z −148…−35; tách khỏi khoang dây/PSU.

### 3.2. Cụm chính và tất cả quạt

| Cụm | Đặc tả |
|---|---|
| Bo | ATX, **B = (−89;25;−30)**; PCB X −211…33, Y −127,5…177,5; 9 chân đế |
| CPU / bơm | IHS **(−70;84;−20,8)**; AIO 240 bơm Ø54 × 48 theo +Z, tới Z 27,2; hai DDR4 A2/B2, một M.2 |
| Két | AIO 240 giữ nguyên, gốc mặt trên **(−15;219;65)**; két X −153,5…123,5, Y 192…219, Z 5…125; hai quạt nằm Y 167…192; đầu ống tại `(114;179;50/80)` theo mốc fitting Gaming |
| GPU | GPU 280, **R−**, A = **(−81;−111;86)**; thân X −221…59, Y −111…14, Z 58,5…113,5. Ba quạt hướng +Z, mặt card trưng bày; `pcie_riser_400` nối bo tới GPU; không cắm card xuyên qua bo |
| Cassette PCIe | Ba dải dọc tại lưng, dài theo Y: −113…7; dãy Z **55,3…116,26**, bước 20,32. Hai nắp đầu được tháo cho lá GPU; khoảng thứ ba giữ thoáng phía trong vì thân dày 55. Giá dưới GPU 300 × 18 × 2 tại Y −117…−115, X −221…79, Z 75…93; hai M3 vào khung, thêm đệm nâng card tại X 45 |
| PSU | ATX **R+**, tâm **(−152;−95;−94)**; bao X −222…−82, Y −170…−20, Z −137…−51. Quạt nguồn hút −Z từ nắp sau, thải −X; tâm rotor `(−152;−95;−125)`. PSU đặt đứng cạnh bo, không dưới sàn trưng bày |
| Bộ chia quạt | `fan_hub_8`, tâm hộp **(85;−148;−85)**; gắn 2 M3 vào khoang sau, không nằm trong ống hút side-bank |

| ID | Cỡ | Tâm | Hướng / đèn |
|---|---|---|---|
| F1, F2, F3 đáy | 120 × 120 × 25 | **(−132;−196,5;63)**, **(−10;−196,5;63)**, **(112;−196,5;63)** | Hút từ đáy, thổi **+Y**, cyan |
| F4, F5 bên | 120 × 120 × 25 | **(142;−45;−22,5)**, **(142;80;−22,5)** | Hút từ hông −Z qua ống gió riêng, thổi **+Z**, cyan; đây là quạt bên cạnh bo, không nằm trên kính trước |
| F6 sau | 120 × 120 × 25 | **(−209,5;97;77)** | Xả **−X**, cyan |
| R1, R2 dưới két | 120 × 120 × 25 | **(−75;179,5;65)**, **(45;179,5;65)** | Xả **+Y**, cyan; thuộc AIO, không cộng vào 6 quạt case |

Tổng **6 quạt case + 2 quạt két**, đúng nhịp 3 dưới/2 bên/1 sau/2 trên của concept. Rotor GPU theo A + R−(−90/0/90;−19,5;62,5), hút −Z từ mặt +Z; không dùng rotor GPU làm quạt case. Vòng cyan trên quạt két dùng biến thể vòng của quạt 120 hiện có, giữ nguyên kích thước, tám lỗ và gốc AIO.

### 3.3. I/O, dây và khoảng hở

I/O trên nóc của khoang kín: X **188**, Y **225**; power Ø14 Z −116; reset Ø4 Z −89; USB-A ×2 Z −65/−42, miệng 6 theo X × 13 theo Z; headphone combo Ø3,5 Z −17. Không USB-C. Module X 180…196, Y 209…223, Z −128…−9; tránh mép quạt nóc. Power dùng vòng cyan PLED riêng.

| Mã | Tâm, trên vách Z −36,5 | Lòng mở X × Y / mục đích |
|---|---|---|
| 03-G0 | (−49;−170;−36,5) | 60 × 28, nguồn vào rãnh |
| 03-G1 | (−149;193;−36,5) | 34 × 14, EPS |
| 03-G2 | (54;79;−36,5) | 26 × 65, ATX24 |
| 03-G3 | (−30;193;−36,5) | 44 × 14, bơm/quạt nóc |
| 03-G4 | (54;−20;−36,5) | 24 × 30, nguồn GPU vòng ở phía +X card |
| 03-G5 | (6;−145;−36,5) | 46 × 14, USB/F_PANEL |
| 03-G6 | (−183;−145;−36,5) | 40 × 14, audio |

Cửa CPU 100 × 100 tâm `(−70;84;−36,5)`; chừa vùng bảo trì phía sau cửa, PSU kết thúc Y −20 nên không chắn cửa bắt đầu Y 34. Rãnh dây rộng 30 tại X 54 (ngoài ống gió), rãnh dưới Y −180, tám đai. Nguồn GPU ra G4, đi ở X 80…105, lên ổ nguồn tại **(44;14;67,45)**, cửa hướng +Y. Không quấn dây lên mặt ba quạt.

Riser: đầu bo ở tâm khe **(−123;−5;−19,6)**; đầu đỡ card đi theo dải tiếp điểm GPU, tâm **(−123;−115,2;61,3)**. Cáp vòng trong vùng X −172…−76, Y −166…−8, Z −15…55, có kẹp trên giá, không đi vào cánh F1. Chiều dài tâm 400 gồm vòng chùng R≥15, không kéo căng theo đường thẳng giữa hai đầu.

PWM F1–F6 vào hub, hub lấy điều khiển SYS1; hai quạt két qua dây Y vào CPU_FAN, bơm AIO_PUMP. Tám nhánh ARGB vào hub, header ARGB của bo chỉ đưa tín hiệu/đất theo mục 7. Không mắc tám vòng trực tiếp vào nguồn 5 V giới hạn 1 A của bo.

**Giới hạn:** GPU dài **330**, cao 130, dày **60** khi dựng dọc; khoảng kính hút gió mặc định **32,5** (146−113,5), ở dày 60 vẫn 30. Nóc chỉ hỗ trợ **AIO 240**, tổng két/quạt 52, không 280/360. Không hỗ trợ radiator bên/đáy trong cấu hình có GPU dọc. Két kết thúc X 123,5, cách kính trước 97,5; đáy quạt két Y 167 cao hơn đỉnh quạt bên Y 140 là 27. Đỉnh quạt đáy −184 cách đáy GPU −111 là 73. Có thể thay bằng tản khí cao tối đa **150 từ IHS** sau khi bỏ AIO; không phải cấu hình mặc định.

**Người mới cần thấy:** kính trước không hút gió; quạt bên nhận gió qua hông đối diện; nguồn và dây ở khoang sau; card dựng mặt quạt ra kính cần riser và giá đỡ. Hai ống nước phải nối thật giữa bơm và két, không kết thúc trên kính hoặc khung.

## 4. 04 — `mini_sff` / Mini dạng hộp

### 4.1. Vỏ và panel

**Thân 180 × 250 × 350**: X ±175, Y ±125, Z ±90. Bốn chân Ø22 cao 12 tại `(±142;−131;±64)`; tổng **180 × 262 × 350**. Nhôm xước M4, mặt trước đặc, bốn vít hông dễ nhận biết. Bố trí **sandwich**: bo hướng +Z, GPU hướng −Z; cả hai hông đều tháo được.

| Mặt / panel | Hình học / vít |
|---|---|
| Trước | Nhôm 2, X 173…175, Y −120…120, Z −86…86; bo góc R8. **4 M3 × 6** ẩn từ phía trong, tâm Y ±105, Z ±71; I/O thấp bên +Z |
| Hông ±Z | Nhôm 2, X −167…167, Y −116…116, Z 88…90 hoặc −90…−88. **4 M3 × 6 mỗi nắp**, tâm X ±152, Y ±101. Khe đứng dài 170 theo Y, rộng 4 theo X, đầu R2, tâm X −144 + 9j, j=0…32, Y=0; không cắt vùng vít. Lọc mỏng bên trong, cassette ở cùng mép nắp |
| Nóc | Nhôm 2, cửa lỗ X −148…133, Z −70…70; **4 M3 × 6** chìm tại X ±157, Z ±72; lọc nam châm 0 vít |
| Đáy | Nhôm 2, cửa thoáng X −142…135, Z −67…67; cố định **0 vít tháo**; **4 M4** giữ chân |
| Lưng | Khung nhôm/thép, cố định **0 vít tháo**; I/O ITX ở phía +Z, **3 PCIe dọc** phía −Z; ổ C14 đầu vào rời ở thấp phía +Z |

### 4.2. Linh kiện và quạt

| Cụm | Đặc tả |
|---|---|
| Bo | `motherboard_itx`, **B = (−76;0;0)**; PCB X −161…9, Y −85…85; 4 chân đế, khay trước Z −6,5 (tấm dày 1,2), normal +Z |
| CPU / tản | IHS **(−96;12;9,2)**; `low_profile_47`, bao X −142…−50, Y −34…58, Z 9,2…56,2. Quạt 92 tích hợp tâm **(−96;12;49,2)**, hút từ +Z và thổi **−Z** vào lá tản |
| RAM / SSD | Hai DDR4 nguyên cỡ tại bo mới; SSD M.2 nằm mặt linh kiện, không giấu vào mặt sau để tránh riser |
| GPU | `gpu_240_dual`, **R+**, A = **(−51;60;−48)**; bao X −171…69, Y −60…60, Z −70,5…−25,5; hai quạt hướng −Z. Riser nối từ khe ITX sang khoang GPU |
| Ba PCIe | Dọc theo Y **−58…62**, dãy Z **−83,26…−22,3**, bước 20,32 từ −22,3 về −Z; hai nắp tháo cho GPU, nắp thứ ba không có card phụ. Giá dưới tại Y −66…−64, X −171…84, Z −61…−43; 2 M3 |
| PSU | `psu_sfx_100`, **R−**, tâm **(100;−47,5;43,25)**; bao X 50…150, Y −110…15, Z 11,5…75. Hút từ hông +Z, xả qua mặt −X vào hành lang trước bo rồi thoát cửa nóc; không bịt mặt xả bằng vách đặc. Quạt PSU 92 tâm `(100;−47,5;67,25)` |
| F1 nóc | Quạt **120 × 120 × 25** đen, tâm **(−65;105,5;12)**; bao Y 93…118; xả **+Y**, SYS1. Không quạt case khác, không radiator |

GPU rotor tại A + R+(−60/60;−12;60), hút +Z từ hông −Z. Nguồn SFX dùng **dây nối AC có vỏ C13 góc 90°**, dài tâm 450, đi từ inlet C14 tâm `(−175;−103;38)` tới ổ PSU tại X 50. Đầu C13 giới hạn X 25…50, tránh mép bo X 9. Dây AC bọc kín, kẹp vào đáy tại Y −116, tách khỏi chân PCB; không mở PSU hoặc vẽ dây AC trần. Đây là dây của case, không sửa ổ C14 trên PSU.

### 4.3. I/O, dây và khoảng hở

Mặt trước +X: power Ø14 **(175;−62;59)**; USB-C miệng 8,4 theo Z × 2,6 theo Y tâm **(175;−88;59)**. Không USB-A, reset hay jack âm thanh. USB-C nối **Type-E của bo ITX mới**; không giả nối vào USB19. PLED vòng trắng quanh power. Module X 156…173, Y −99…−49, Z 49…69; cách thân PSU phía +X ít nhất 6.

Khoang dây trung tâm nằm Z −24…−8, còn vùng mép trước X 14…47. Không nhét bó dây vào khe hút GPU. Khay giới hạn X −162…43, Y −113…113; cửa CPU 96 × 96 tâm `(−96;12;−6,5)`.

| Mã | Tâm | Lòng mở / mặt |
|---|---|---|
| 04-G0 | (30;−62;−6,5) | 18 × 58 theo X/Y, cho đầu ATX24 đi qua từ PSU vào khoang giữa |
| 04-G1 | (−131;99;−6,5) | 28 × 10 X/Y, EPS |
| 04-G2 | (30;19;−6,5) | 18 × 58 X/Y, ATX24 |
| 04-G3 | (−54;99;−6,5) | 32 × 10 X/Y, CPU_FAN/quạt nóc |
| 04-G4 | (−70;−101;−6,5) | 96 × 12 X/Y, riser; nguồn GPU đi cạnh qua G0 rồi vòng dưới mép khay |
| 04-G5 | (0;−101;−6,5) | 28 × 12 X/Y, Type-E/F_PANEL |
| 04-G6 | Không có | Không jack audio; không khoét lỗ thừa |

Riser đầu bo `(−73;−72;10,4)`; đầu khe GPU theo dải tiếp điểm có tâm `(−73;64,2;−28,3)`. Đường cáp vòng mép dưới bo qua G4, men vùng X −125…−20, Z −22…−9, đi lên phía lưng card; phần chùng gấp U dọc Y, R≥15. Không luồn xuyên cửa CPU hoặc tì vào mặt backplate GPU. Nguồn GPU ra G0, vòng dưới khay, đi ở X 82…105 rồi vòng về ổ tại **(54;−60;−35,25)**, hướng −Y. Dây nguồn ngắn: ATX 300, EPS 400, GPU 300; đo theo tâm đường đã uốn.

**Giới hạn:** GPU **260 dài × 120 cao × 50 dày**, tản CPU tối đa **55 từ IHS**, PSU chỉ **SFX sâu 100**, không SFX-L/ATX; không radiator. Mẫu GPU cách hông hút trong −88 là 17,5; ở dày 50 còn 15. Mẫu tản cách hông trong +88 là 31,8; giới hạn 55 vẫn còn 23,8. Quạt nóc đáy Y 93 cách mép PCB trên 85 là 8. PSU bắt đầu X 50 cách mép bo X 9 là 41, đủ đầu AC góc đã định. Phần GPU dài 260 kết thúc X 89, không chiếm khoang nguồn vì khác Z.

**Người mới cần thấy:** hai nắp đều thông gió; GPU nằm sau lưng bo, cần riser; SFX nhỏ hơn ATX; CPU dùng tản thấp và vẫn giữ RAM DDR4 nguyên chiều cao. Mở nắp −Z để tiếp cận GPU, +Z để tiếp cận bo.

## 5. 05 — `slim_console` / Slim kiểu console

### 5.1. Vỏ và tư thế

**Thân 100 × 360 × 330**: X ±165, Y ±180, Z ±50. M5 graphite, mặt trước lưới hẹp, hông đục lỗ lớn, mép ngoài R6. Tư thế mặc định đứng trên `console_stand`: bao đế 140 theo Z × 18 cao × 280 theo X, tâm **(0;−189;0)**. Tổng đứng **140 × 378 × 330**. Trục vẫn theo thân, không theo chân đế.

| Mặt / panel | Hình học / vít |
|---|---|
| Trước | Cassette bo R8, X 153…165, Y −174…174, Z −45…45; lưới M2 trừ vùng I/O thấp. Bốn ngàm và **2 M3 × 6** ở `(162;±159;0)`; lỗ vít chìm dưới mép cassette |
| Hông ±Z | Tôn M5 dày 1, X −157…157, Y −170…170; cửa lỗ X −142…142, Y −155…155, lọc phía trong. Mỗi nắp **2 vít tay** mép sau tại X −151, Y ±150; trượt −X rồi nhấc. +Z là mặt hút chính |
| Nóc | Lưới X −140…130, Z −34…34; panel cố định **0 vít tháo** |
| Đáy | Hai cửa lưới X −138…−20 và 20…138, Z −30…30; phần giữa giữ gá đế. Cố định **0 vít tháo** |
| Lưng | I/O ITX thấp, **3 PCIe dọc ở nửa trên**, inlet AC rời ở thấp phía trước cụm bo; tấm cố định **0 vít tháo** |
| Đế | Hai ray đỡ dọc X sát hai mép Z của thân, có đệm cao su; **2 M4 × 10** trục Y tại `(−85;−180;0)` và `(85;−180;0)` đi qua tai gá, không che hai cửa đáy |

Hỗ trợ đặt ngang **sau khi tháo đế**: gắn bốn chân cao su 12 × 12 × 8 (keo định vị, 0 vít) tại `(±137;±148;−54)` lên hông −Z. Tổng bao theo trục thân khi nằm ngang: **108 theo Z × 360 theo Y × 330 theo X**; mặt bàn ở Z −58, +Z quay lên. Nâng hông lưới thấp khỏi bàn 8; không dùng ảnh dựng nằm đè trực tiếp lên lưới. Sheet vẫn giữ trục gốc đứng, thêm hình tư thế ngang nhỏ trong overview.

### 5.2. Linh kiện: cùng mặt nhưng hai tầng

| Cụm | Đặc tả |
|---|---|
| Bo | `motherboard_itx`, **B = (−66;−65;−30)**; PCB X −151…19, Y −150…20; normal +Z; khay mặt trước Z −36,5, 4 chân đế |
| CPU / tản | IHS **(−86;−53;−20,8)**; `low_profile_47`, bao X −132…−40, Y −99…−7, Z −20,8…26,2. Quạt 92 tâm **(−86;−53;19,2)**, hút từ +Z, thổi −Z |
| RAM / SSD | Hai DDR4 nguyên cỡ, một M.2 mặt trước bo; đỉnh RAM Z 17,5, không thay bằng RAM thấp giả để vừa vỏ |
| GPU | `gpu_240_dual`, **R−**, A = **(−41;40;8)**; bao X −161…79, Y 40…160, Z −14,5…30,5. Hai quạt hướng +Z; nằm **trên bo**, không chồng hình chữ nhật GPU lên CPU |
| Ba PCIe | Dọc Y **38…158**, Z **−17,7…43,26**, bước 20,32 từ −17,7 về +Z; hai nắp tháo, khoảng thứ ba không thêm card. Giá GPU tại Y 34…36, X −161…94, Z −3…15; hai M3 |
| PSU | SFX, **R−**, tâm **(97;−90;0)**; bao X 47…147, Y −152,5…−27,5, Z −31,75…31,75. Hút hông +Z, xả −X qua khe giữa PSU và bo, thoát lưới trước/nóc. Quạt PSU 92 tâm `(97;−90;24)` |
| Quạt case / két | **0 quạt case, 0 radiator**. Làm rõ cửa thoát thụ động quanh khoang; không vẽ thêm quạt 120 trong thân rộng 100 |

Hai rotor GPU ở A + R−(−60/60;−12;60), hút −Z. Inlet C14 tại **(−165;−166;28)**, đầu C13 góc tại mặt PSU X 47, bao X 22…47; dây AC 450 đi sát đáy Y −171, kẹp riêng, không xuyên PCB. Phía ra dây PSU +X có hành lang X 148…162 và một khe chuyển bó lên vùng trước bo; không để đầu nối thẳng cần 30 mm chạm panel. Dùng dây liền mềm theo SFX mới.

### 5.3. I/O, dây và khoảng hở

Mặt trước +X, Z=**0**: power Ø12 tâm **(165;−92;0)**; USB-A `(165;−119;0)`, miệng 13 theo Y × 6 theo Z; USB-C `(165;−143;0)`, miệng 8,4 theo Y × 2,6 theo Z; headphone combo Ø3,5 `(165;−160;0)`. Không reset. USB-A vào một nhánh USB19, nhánh không dùng bịt kín trong module; USB-C vào Type-E; headphone vào HD_AUDIO. Module X 153…164; chừa hành lang dây nguồn ở Z −40…−33 để không đi qua module.

Khay bo chỉ ở vùng X −158…43, Y −174…38, không kéo vách kín qua mặt hút của card trên. Khoang dây phía −Z nằm −49…−37,7; xếp dây dẹt một lớp, không chồng đầu ATX dày vào đó. Cửa CPU 96 × 96 tâm `(−86;−53;−36,5)`. Bó lớn chạy dọc hành lang X 25…43 cạnh bo.

| Mã | Tâm | Lòng mở / mặt |
|---|---|---|
| 05-G0 | (31;−135;−36,5) | 18 × 56 X/Y; bó PSU vào rãnh cạnh |
| 05-G1 | (−121;29;−36,5) | 28 × 10 X/Y; EPS |
| 05-G2 | (31;−62;−36,5) | 18 × 58 X/Y; ATX24 |
| 05-G3 | (−38;29;−36,5) | 32 × 10 X/Y; CPU_FAN |
| 05-G4 | (−68;−166;−36,5) | 96 × 10 X/Y; riser xuống sau khay rồi vòng lên khe giữa hai tầng |
| 05-G5 | (2;−166;−36,5) | 22 × 10 X/Y; USB/Type-E/F_PANEL |
| 05-G6 | (−143;−166;−36,5) | 22 × 10 X/Y; audio |

Đầu riser trên bo `(−63;−137;−19,6)`; đầu khe GPU `(−63;35,8;−11,7)`. Tuyến cáp xuống G4, đi dọc mặt sau khay tới mép Y 26, vòng ra +Z dưới GPU; vòng chùng đặt ở nửa trên phía −Z, không che lưới hút +Z. Riser dài 400, R≥15. Nguồn GPU đi hành lang X 88…119 tới ổ **(64;160;−4,75)**, cửa hướng +Y; dùng đầu 6+2 góc thấp có bao cao ≤12 theo Y, sau đó dây quay về +X, tránh vượt nóc trong Y 179. Dây nguồn ATX 250, EPS 350, GPU 350, loại mềm; đầu góc thuộc bộ dây case, không thay mesh ổ cắm GPU.

**Giới hạn:** GPU **250 dài × 120 cao × 45 dày**, tản CPU **50 từ IHS**, PSU SFX **100 sâu**, không radiator. Khoảng CPU mẫu tới nắp trong Z 49 là 22,8; GPU tới +49 là 18,5; PSU tới +49 là 17,25. Giữa bo trên Y 20 và GPU dưới Y 40 có 20, dùng cho giá/riser, không đặt thêm quạt. GPU kết thúc Y 160, chừa 19 tới nóc trong cho đầu nguồn góc; không đổi sang đầu cắm thẳng cao hơn. Đây là cấu hình thông gió phụ thuộc quạt từng linh kiện, không khẳng định hiệu suất nhiệt bằng tower có quạt case.

**Người mới cần thấy:** thân mỏng cần card qua riser, bố trí hai tầng; đế chống đổ không phải chi tiết trang trí; muốn đặt ngang phải tháo đế và dùng bốn chân, để mặt hút +Z hướng lên.

## 6. 06 — `full_tower` / Tower lớn

### 6.1. Vỏ và các mặt

**Thân 250 × 620 × 550**: X ±275, Y ±310, Z ±125. Bốn chân vát 58 theo X × 38 theo Z × 22 cao, tâm `(±226;−321;±91)`; tổng **250 × 642 × 550**. Mặt trước cao với **bốn** quạt 140, nóc có AIO 360, hông kính, các cạnh trước có vát đứng 8 × 8 như concept.

| Mặt / panel | Kiểu / vít |
|---|---|
| Trước | Cassette M1 X 262…275, Y −299…299, Z −115…115; lưới M2 tại X 271…271,8, cửa Z −92…92, Y −294…294, lọc X 268,5. Sáu ngàm, **2 M3 × 6** dưới tại `(273;−302;±98)` |
| Hông +Z | Kính M3 X −264…264, Y −294…294, Z 121…125; **4 vít tay có đệm** tại X ±249, Y ±279 |
| Hông −Z | M1 kín, 528 × 588 × 1; **2 vít tay** ở X −258, Y ±274 |
| Nóc | M1, lưới X −245…215, Z −90…104; ray két lệch +Z. Cassette nóc **2 M3 × 6** phía sau tại `(−261;310;±103)` + ngàm trước; lọc 0 vít |
| Đáy | Cửa PSU 124 × 124 tại X −202, Z −18, lọc kéo −X; thêm lưới thụ động X −95…178, Z −75…90. Cố định **0 vít tháo**, chân **4 M4** |
| Lưng | I/O ATX, một quạt 140, **8 PCIe ngang**, PSU ATX; tấm cố định **0 vít tháo**. Tám nắp/vít khi trống; còn sáu nắp khi lắp GPU |

### 6.2. Linh kiện và quạt

| Cụm | Đặc tả |
|---|---|
| Bo | ATX, **B = (−139;80;−88)**, không đổi sang E-ATX; PCB X −261…−17, Y −72,5…232,5. Khay trước Z −94,5, 9 chân đế |
| CPU / bơm | IHS **(−120;139;−78,8)**; giữ bơm Ø54 × 48 và gá Gaming; hai DDR4 A2/B2, một M.2 |
| Két | `aio_360`, gốc mặt trên **(−35;304;40)**; két X −233,5…163,5, Y 277…304, Z −20…100; ba quạt Y 252…277; hai ống Ø11 dài 450 |
| GPU | GPU 280 R0, **A = (−131;25,3;−73,4)**; bao X −271…9, Y −2,2…52,8, Z −73,4…51,6; cắm thẳng x16. Giá chống võng nhỏ thuộc case, đệm tại `(−5;−4,2;26)` |
| PSU | ATX R0 tâm **(−202;−251;−18)**; bao X −272…−132, Y −294…−208, Z −93…57; hút đáy/xả −X. Rotor PSU tại `(−202;−282;−18)` |
| Che PSU | Mặt trên Y −190, X −271…193, Z −94,5…119; đầu trước hở vùng quạt. **2 M3 × 6** tại X −225/158, Z 108; không thêm ổ cứng chưa có trong BOM |
| Hub | `fan_hub_8` tâm **(90;70;−110)**, gắn 2 M3 sau khay; hộp dày 18 nằm Z −119…−101, không ép vào nắp −124 |

| ID | Cỡ | Tâm | Hướng |
|---|---|---|---|
| F1…F4 trước | 140 × 140 × 25 | **(247,5;−219;0)**, **(247,5;−73;0)**, **(247,5;73;0)**, **(247,5;219;0)** | Hút qua lưới, thổi −X |
| F5 sau | 140 × 140 × 25 | **(−259,5;149;40)** | Xả −X |
| R1…R3 dưới két | 120 × 120 × 25 | **(−155;264,5;40)**, **(−35;264,5;40)**, **(85;264,5;40)** | Xả +Y |

Tất cả quạt đen, không ARGB mặc định. Tổng **5 quạt case + 3 quạt két**. PWM case vào hub, điều khiển SYS1; ba quạt két qua dây chia ba nhánh vào CPU_FAN, mỗi motor yêu cầu ≤0,2 A, tổng ≤0,6 A; chỉ một nhánh trả tach. Bơm vào AIO_PUMP. Rotor GPU theo A + (−90/0/90;−19,5;62,5), hút +Y.

### 6.3. I/O, dây và khoảng hở

I/O nóc ở X **246**, Y **310**, cửa hướng +Y: power Ø14 Z=0; reset Ø4 Z=−84; USB-A ×2 Z=−42/+42; headphone combo Ø3,5 Z=84. Không USB-C. Miệng USB 6 theo X × 13 theo Z; module X 238…254, Y 296…308, Z −95…95, nằm trên đỉnh quạt trước Y 289 với khoảng 7. HDD LED Ø1,5 tại `(261;310;0)`, PLED vòng trắng. Không thêm cổng vào kính.

Khay dày 1,2: phần trên X −271…205, Y −190…307; phần dưới chỉ X −129…205, Y −307…−190 để tránh PSU. Khoang sau khay Z −124…−95,7, sâu 28,3; rãnh nguồn 30 tại X 25, rãnh EPS 26 tại X −245, tám đai. Cửa CPU 100 × 100 tâm `(−120;139;−94,5)`.

| Mã | Tâm | Lòng mở X × Y |
|---|---|---|
| 06-G0 | (−91;−248;−94,5) | 60 × 28 |
| 06-G1 | (−199;252;−94,5) | 34 × 14 |
| 06-G2 | (10;133;−94,5) | 28 × 68 |
| 06-G3 | (−94;252;−94,5) | 48 × 14 |
| 06-G4 | (52;31;−94,5) | 30 × 34 |
| 06-G5 | (−42;−91;−94,5) | 46 × 14 |
| 06-G6 | (−235;−91;−94,5) | 42 × 14 |

G0→G1 EPS, G0→G2 ATX; G4 ra nguồn GPU, kẹp giữ tại `(40;45;65)`. Đầu nguồn GPU **(−6;43,85;51,6)** hướng +Z; chừa ít nhất 25 cho thân đầu cắm và vòng dây. Bơm/quạt két qua G3; I/O qua rãnh trước, G5/G6. Dây nguồn ATX/EPS/GPU dùng dây hiện có với đoạn nối tháo được 200 nếu tuyến vượt chiều dài, không kéo PSU về phía trước. Không sửa vỏ PSU để có thêm đầu modular.

**Giới hạn:** GPU dài **450**, nhô từ cạnh gốc Z tối đa 135, dày 65; tản khí thay AIO cao tối đa **180 từ IHS**. GPU mẫu cách quạt trước 226 theo X; GPU dài 450 kết thúc X 179, còn 56 tới quạt ở X 235. Nóc hỗ trợ **360 hoặc 240**, bề rộng 120, tổng két/quạt **52**; không hỗ trợ 420/280. Ba quạt két thấp nhất Y 252, cách mép bo trên 232,5 là 19,5; cụm két lệch +Z tránh đầu EPS. Két kết thúc X 163,5, cách quạt trước X 235 là 71,5. Không gắn radiator trước trong phiên bản có bốn quạt 140 này; không hứa khả năng E-ATX chỉ vì vỏ lớn.

**Người mới cần thấy:** không gian dư không tự làm máy mạnh hơn; bo vẫn ATX như họ 01/03; bốn quạt trước và ba quạt két là hai nhóm khác nhau; PSU dưới shroud, đường EPS dài đi sau khay.

## 7. Các linh kiện mới dùng chung

Chốt **10 loại** dưới đây. Chân thường, bracket, khay, shroud, module I/O, dây nối AC, đầu nguồn góc và các đoạn nối thụ động là chi tiết của case/cables, không tạo model linh kiện độc lập. Không vẽ lại CPU, DDR4, SSD, bo ATX, PSU ATX, GPU 280 hoặc bơm AIO hiện có.

| ID mới / sheet | Kích thước chính xác và mốc | Chi tiết bắt buộc | Họ dùng |
|---|---|---|---|
| `motherboard_matx` | PCB **244 X × 244 Y × 1,6 Z**; gốc lưng PCB; bao cứng gồm I/O X −136,2…122, Y −122…122, Z 0…37,6 | Socket CPU chung, 4 DIMM, 2 PCIe, 1 M.2; ATX24/EPS, USB19, audio, fan headers; không Type-E | 02 |
| `motherboard_itx` | PCB **170 X × 170 Y × 1,6 Z**; bao gồm I/O X −99…85, Y −85…85, Z 0…37,6 | 2 DIMM DDR4 nguyên cỡ, 1 x16 cho riser, 1 M.2, thêm Type-E USB-C; 4 lỗ bo | 04, 05 |
| `psu_sfx_100` | Vỏ **100 X × 63,5 Y × 125 Z**, gốc tâm; C14/switch −X, dây liền +X, hút −Y; không gồm dây mềm | Một quạt 92 trong nguồn; bộ dây ATX24, EPS4+4, GPU6+2; không modular; lưới, C14, công tắc | 04, 05 |
| `gpu_240_dual` | Thân **240 X × 45 Y × 120 Z**; X ±120, Y ±22,5, Z 0…120; chân PCIe xuống −8; lá cổng tới X −121 | Hai rotor Ø90; PCB tâm Y 19,7, x16 có khóa như GPU cũ, lá hai khe; một ổ 8 chân hướng +Z; 3 DP + 1 HDMI, không logo | 02, 04, 05 |
| `tower_air_155` | Từ IHS: bao gồm tai gá **90 X × 120 Y × 155 Z**, X ±45, Y ±60, Z 0…155; thân lá/quạt X −40…35 | Đế 40 × 40 × 5; bốn heatpipe Ø6; lá nhôm dày 0,3 bước 1,8; một quạt 120 × 120 × 25 tại (22,5;0;90), thổi −X; gá 78 × 78 | 01, 02 |
| `low_profile_47` | Bao **92 X × 92 Y × 47 Z**, gốc mặt IHS, X/Y ±46, Z 0…47 | Đế 40 × 40 × 5, lá/heatpipe thấp Z 5…33; quạt tích hợp 92 × 92 × 14 ở Z 33…47, tâm (0;0;40), thổi −Z; gá 78 × 78 | 04, 05 |
| `aio_360` | Két **397 X × 120 Z × 27 Y**; gốc giữa mặt trên; cùng ba quạt dày 25, tổng 52; bơm giữ Ø54 × 48 | Ba quạt 120, lõi 360, hai bình đầu 18,5; tái dùng bơm/backplate; hai ống Ø11 dài **450** | 06 |
| `pcie_riser_400` | Dải mềm **400 dài tâm × 70 rộng × 1,5 dày**; đầu cắm đực 89 × 12 × 1,6, adapter cái 110 × 18 × 14; hai tai gá cách nhau 100 | x16, chốt khóa tại đầu cái, shield đen, R≥15; không khẳng định thế hệ PCIe/tốc độ chưa kiểm thử; chiều dài mềm không gồm hai đầu cứng | 03, 04, 05 |
| `console_stand` | Bao **280 X × 18 Y × 140 Z**, gốc giữa bao; má đỡ thân rộng 100, lòng có đệm 1 mỗi bên | Hai ray vát, hai cầu nối tại X ±85; khe giữa để hở cửa đáy, 2 lỗ M4; kèm 4 chân ngang 12 × 12 × 8 | 05 |
| `fan_hub_8` | Hộp **80 X × 50 Y × 18 Z**; hai tai tổng dài X **92**, tâm lỗ X ±43/Y0. Bộ lấy nguồn ATX24 pass-through **25 X × 58 Y × 42 Z**; dây tới hộp dài 650 | 8 cổng PWM và 8 ARGB, nguồn 12/5 V từ adapter 24-pin; đầu điều khiển riêng; nắp đen không logo, đánh số 1–8 | 03, 06 |

### 7.1. Bo mATX và ITX: tọa độ cục bộ khóa để vẽ

Kế thừa hình socket, tiếp điểm và cơ cấu gạt CPU, rãnh DDR4, khóa M-key, chân cắm và sơ đồ F_PANEL từ Gaming. Các tọa độ mới là bố trí mô phỏng, **không phải lưới khoan chuẩn thương mại**. Không cắt ngắn hoặc scale mesh bo ATX để tạo hai bo này.

| Điểm | mATX: (X;Y), chiều Z lấy theo Gaming | ITX: (X;Y), chiều Z lấy theo Gaming |
|---|---|---|
| CPU | **(0;50)** | **(−20;12)** |
| Lỗ gá tản | CPU + (±39;±39), 4 lỗ Ø3,5 | CPU + (±39;±39), 4 lỗ Ø3,5 |
| DIMM | A1/A2/B1/B2 X=70/79/89/98, Y=40; thân khe 6 × 142 × 12; lắp A2/B2 | A/B X=55/65, Y=6; thân khe 6 × 142 × 12; lắp cả hai |
| PCIe x16 chính | **(−34;−61)**, thân 89 × 9 × 11 | **(3;−72)**, thân 89 × 9 × 11 |
| PCIe thứ hai | (−34;−101), cùng chiều dài; để trống | Không có |
| M.2 tâm / miệng / vít | (35;−83) / (−5;−83) / (75;−83) | (−12;−48) / (−52;−48) / (28;−48) |
| ATX24 | (117;30), 10 × 52 × 13 trên PCB | (80;5), 10 × 52 × 13 trên PCB |
| EPS8 | (−60;115), 18 × 9 × 13 | (−55;78), 18 × 9 × 13 |
| CPU_FAN | (25;115) | (17;73) |
| SYS_FAN | 1=(106;79), 2=(81;−99), 3=(−96;−103) | 1=(−48;−53), 2=(−25;−53) |
| AIO_PUMP / ARGB | Không lắp header, không giả thêm đầu | Không lắp header |
| USB19 | (113;−92), thân 8 × 20 × 9 | (80;−46), thân 8 × 20 × 9 |
| Type-E | Không có | **(80;58)**, thân 8 × 20 × 10; khóa có định hướng, đầu cáp USB-C riêng |
| F_PANEL | (94;−113) | (60;−80) |
| HD_AUDIO | (−97;−113) | (−62;−80) |
| Lỗ bo Ø3,4 | 9 tổ hợp X=−112/0/112, Y=−112/0/112 | 4 tổ hợp X=±75, Y=±75 |
| Lá I/O | X −136,2…−135,4, Y −49…109, Z 1,6…37,6 | X −99…−98,2, Y −79…79, Z 1,6…37,6 |

Hai bo mới dùng cụm I/O tối giản cùng số lượng: 2 USB-A, 1 RJ45, 3 jack TRS. Cổng hướng −X, tâm chiều Z=16; mATX tâm Y=88/69 (USB), 35 (RJ45), 5/−12/−29 (audio); ITX tâm Y=58/39, 5, −25/−42/−59. Không tự thêm HDMI sau bo nếu mô phỏng chưa có iGPU. Lỗ case bao quanh lá I/O nới 0,5 mỗi cạnh Y/Z, khung đỡ riêng đặt mặt lá đúng X đã ghi.

Giữ vùng trống bán kính 4 quanh lỗ bo, dải 3 quanh đầu cắm; các linh kiện SMD nền không lấn vùng lắp tản. Bo mATX có thể trang trí tản VRM 16 × 60 × 16 tại (−53;50), bo ITX dùng 14 × 55 × 10 tại (−65;18); không kéo khối tản qua lỗ gá CPU. Hai bo đều dùng tấm đỡ tản 90 × 90 sau socket, phải giữ cửa khay tương ứng.

### 7.2. GPU, PSU và gá tản mới

GPU 240 giữ khoảng từ lá cổng tới dải tiếp điểm **53,5…142,5 theo X**, giống card 280; theo gốc card mới, dải này là X −66,5…22,5, tâm −22. PCB tâm Y 19,7; chân tiếp điểm Z −8…0. Để cắm trực tiếp bo mATX, gốc card = B + (−12;−80,7;14,6); PCB card khớp Y khe −61. Rotor tâm (−60;−12;60) và (60;−12;60), mặt hút −Y. Ổ nguồn tâm **(105;12,75;120)**, cửa +Z; khoét hốc để thân ổ nằm trong bao card. Lá cổng hai khe có biên Y −14,94…25,7, Z −2…118, X −121…−120; tai vít tại Y 15,54/−4,78, Z113. Khi quay card, quay cả lá, vít, ổ nguồn và đầu riser.

PSU SFX lỗ bốn vít M3 × 6 ở mặt sau X −50, tọa độ cục bộ (Y;Z) = **(±25;±55)**. Quạt 92 tâm **(0;−24;0)**, trục Y, hút −Y; khung/cánh trong vỏ, lưới thấp nhất Y −31,75. C14 tâm `(−50;−9;43)`, công tắc `(−50;17;43)`; thân cổng phải nằm trong cao 63,5. Grommet ra dây Ø18 ở `(50;−13;35)`; đuôi mềm được uốn theo case, không dựng sẵn đoạn cứng 30 như ATX. Không thay thứ tự dây EPS 4+4 và GPU 6+2 cho nhau.

Hai tản khí mới dùng **4 vít lò xo M3 × 12** vào bộ gá 78 × 78, tấm đỡ sau bo 90 × 90 có đệm cách điện 0,8, như phương pháp Gaming. Kích thước bao tản tính từ IHS, đã gồm tai gá; phần backplate ở phía sau bo ghi riêng. Tower: đế Z 0…5, heatpipe đi trong X −40…10, khối lá X −40…10/Y ±60/Z 30…155; fan X 10…35/Y ±60/Z 30…150. Vì lá bắt đầu ở Z 30 trên IHS, không xuyên VRM thấp trên bo. Quạt tower dùng hai kẹp thép Ø1,2 nằm trong bao X ±45, không thêm bốn vít dài xuyên lá nhôm. Quạt thấp gắn bốn M3 × 16 vào tai ở (±41,25;±41,25), không nhầm bước này với fan 120. VRM ITX cao 10 trên PCB, thấp hơn đáy lá tản thấp Z 14,2 tính từ lưng PCB; chừa 2,6. Hai quạt tản dùng PWM 4 chân CPU_FAN, không ARGB.

### 7.3. AIO 360 và hub có nguồn

AIO 360: thân két X ±198,5, Y −27…0, Z ±60; lõi X ±180. Quạt tâm X −120/0/120, Y −39,5, Z0, xả +Y. Mỗi tâm có 4 lỗ tại X/Z ±52,5: **12 vít nóc M3 × 6, 12 vít quạt M3 × 30**, 24 đệm. Fitting hai ống tại cục bộ **(189;−40;−15/+15)**; gốc toàn máy họ 06 cho fitting `(154;264;25/55)`. Giữ bơm Gaming và hai fitting trên bơm; ống đi vòng phía trước RAM, không băng xuyên quạt trước. Chiều dài 450 là chiều dài theo tâm ống, chọn đường cong đủ chiều dài và R≥30. Màu đen như AIO 240, không thêm LCD hoặc bồn nước.

Hub không yêu cầu sửa PSU ATX: adapter **24-pin male/female nối tiếp đầy đủ 24 đường**, nằm ngay đầu ATX trên bo, nhánh riêng lấy 12 V/5 V/GND qua dây dài 650 tới hub. Định mức thiết kế nhánh ≥3 A ở 12 V, ≥2,5 A ở 5 V, bảo vệ quá dòng từng rail; đây là yêu cầu của linh kiện mô phỏng mới. Vẽ rõ adapter và nhánh nguồn, không giả hub tự có điện từ một dây tín hiệu. Họ 03 đủ cho 8 vòng ≤0,25 A/vòng và 6 motor ≤0,2 A; họ 06 chỉ cấp 5 motor, cổng đèn để trống có nắp.

Đầu PWM điều khiển tới SYS1 mang PWM/tach/GND, không trả nguồn 12 V ngược về bo; chỉ port1 báo tach. Đầu ARGB tới bo họ 03 mang DATA/GND; chân +5 V từ bo **không nối vào rail cấp đèn của hub**, tránh nối song song hai nguồn. Các cổng ARGB ra vẫn đúng +5V/D/khuyết/GND, có khóa. Tám vòng có thể dùng cùng dữ liệu để sáng cyan đứng yên. Adapter ATX đặt theo hướng cắm header, cao tổng 42 tính từ mặt tiếp xúc; không đặt hộp hub trên mặt bo hoặc che chốt RAM. Vẽ dây điều khiển PWM/ARGB màu đen, dùng nét chú thích khác màu trong sheet.

## 8. Kế hoạch bản vẽ tiếp theo

**22 ảnh, mỗi dòng đúng một sheet/PNG. Chỉ là danh sách cần vẽ ở bước sau; không tạo ảnh trong bước đặc tả này.** Không thêm sheet cho linh kiện giữ lại. Mỗi ảnh có nền trắng, đường kích thước xanh `#2164AC`, mô hình đen/graphite theo bảng, nhãn ngắn và số thứ tự; phần hướng dẫn bằng tiếng Việt. Số đo bằng mm là chuẩn, không lấy tỷ lệ từ ảnh tạo sinh để sửa số.

### 8.1. Bố cục và hướng chiếu bắt buộc

- `00_overview.png`: một hình lắp ráp tách lớp 3/4 từ +X/+Y/+Z; đủ case/panel, bo, CPU, tản, **2 RAM**, **1 SSD**, **1 GPU**, **1 PSU**, toàn bộ quạt đúng số, dây và bộ gá. Dùng số 1–10 theo ID Gaming; riser/hub/stand là nhãn phụ có tên rõ. Inset nhỏ lắp hoàn chỉnh và đường dây sau khay/khoang phụ; mũi tên chỉ hướng lắp và hướng gió. Chỉ vẽ cấu hình mặc định, không chồng linh kiện tùy chọn vào BOM. Họ 04 thêm inset hông −Z để nhìn card; họ 05 thêm tư thế ngang.
- `01_case.png`: **case trống** trong **6 hình chiếu vuông góc + 1 hình 3/4**. Case trống vẫn có khay, chân đế bo, giá riser, shroud, grommet, giá quạt, lọc, I/O vỏ, nắp PCIe, chân/đế; **không có bo, PSU, GPU, CPU, tản, RAM, SSD, fan hoặc radiator**. Các panel được đặt đúng trạng thái để sáu view đối chiếu được; riêng 3/4 tháo hai hông/cassette theo mũi tên để thấy phần trong. Ghi kích thước cửa, panel, độ dày, tất cả vít panel, tâm gá quạt/két và mã G.
- Sáu view luôn ghi cả tên và trục: **FRONT +X nhìn −X**, **BACK −X nhìn +X**, **LEFT/OPEN +Z nhìn −Z**, **RIGHT/CLOSED −Z nhìn +Z**, **TOP +Y nhìn −Y**, **BOTTOM −Y nhìn +Y**. Front: +Z ở trái ảnh; back: +Z ở phải; left/open: +X ở phải; right/closed: +X ở trái. Top: +X xuống dưới, +Z sang phải; bottom: +X lên trên, +Z sang phải. Không lật gương mặt lưng để giống mặt trước. Sáu view chung tỷ lệ trong mỗi sheet.
- `parts/<part>.png`: **6 orthographic + 1 hình 3/4** của riêng bộ phận/cụm mới; trục cục bộ và gốc phải hiện rõ. Với bo/tản, ghi thêm COMPONENT FACE +Z; với PSU ghi AC FACE −X; GPU ghi FAN FACE −Y. Với cụm mềm riser/AIO/hub, sáu view dùng cùng một tư thế trưng bày đã cố định, ghi chiều dài tâm dây/ống riêng; không lấy hộp bao dây cong làm kích thước cơ khí cứng.
- Mỗi sheet có bảng vít gọn và vài kích thước quan trọng, không chỉ ảnh phối cảnh đẹp. Vít panel và vít linh kiện đánh số khác nhóm. ARGB dùng đúng số vòng, không thêm đèn vào vỏ kín. Giữ chân CPU phẳng, rãnh DDR4, chiều cắm M.2 và chốt PCIe thấy được trong overview.

### 8.2. Danh sách file chính xác

| STT | File cần tạo sau | Nội dung cần nhấn |
|---|---|---|
| 01 | `design/families/01_standard_tower/00_overview.png` | ATX, tower air, GPU 280 ngang, ATX PSU, 2 quạt trước 140 + 1 sau 120 |
| 02 | `design/families/01_standard_tower/01_case.png` | Vỏ trống 230 × 460 × 430, mặt lưới, hông kính, 7 PCIe, G0–G6 |
| 03 | `design/families/02_compact_tower/00_overview.png` | mATX, tower air, GPU 240 ngang, ATX PSU, 2 trước + 1 sau; không quạt nóc |
| 04 | `design/families/02_compact_tower/01_case.png` | Vỏ trống 210 × 380 × 370, hai hông kín, khe thấp/khe cạnh trước, 4 PCIe |
| 05 | `design/families/03_showcase_dual_chamber/00_overview.png` | Hai khoang, AIO 240, GPU 280 dọc + riser, PSU đứng sau bo, hub và 8 vòng cyan |
| 06 | `design/families/03_showcase_dual_chamber/01_case.png` | Vỏ trống 300 × 450 × 450, hai kính, ống hút bên, ba khe dọc, giá GPU |
| 07 | `design/families/04_mini_sff/00_overview.png` | ITX, tản thấp, SFX, GPU 240 sandwich + riser, một quạt nóc 120, dây AC |
| 08 | `design/families/04_mini_sff/01_case.png` | Vỏ trống 180 × 250 × 350, nhôm khe đứng, khay giữa, 3 PCIe dọc, inlet AC |
| 09 | `design/families/05_slim_console/00_overview.png` | ITX dưới/GPU trên, SFX cạnh bo, riser, tản thấp, đế đứng/chân ngang, không fan case |
| 10 | `design/families/05_slim_console/01_case.png` | Vỏ trống 100 × 360 × 330, hông lỗ, mặt lưới hẹp, ba khe dọc, lỗ gá đế |
| 11 | `design/families/06_full_tower/00_overview.png` | ATX, AIO 360, GPU 280 ngang, ATX PSU, hub, 4 trước 140 + 1 sau 140 + 3 két 120 |
| 12 | `design/families/06_full_tower/01_case.png` | Vỏ trống 250 × 620 × 550, 8 PCIe, ray két 360, kính lớn, shroud dài |
| 13 | `design/families/parts/motherboard_matx.png` | 244 × 244, 9 lỗ, bốn DIMM, hai PCIe, tọa độ/header/socket |
| 14 | `design/families/parts/motherboard_itx.png` | 170 × 170, 4 lỗ, hai DIMM, x16/riser, M.2, USB19 và Type-E |
| 15 | `design/families/parts/psu_sfx_100.png` | 125 × 63,5 × 100, C14, quạt 92, bốn lỗ gá, ba nhánh dây liền |
| 16 | `design/families/parts/gpu_240_dual.png` | 240 × 120 × 45, hai quạt, chân x16, lá hai khe, nguồn 8 chân |
| 17 | `design/families/parts/tower_air_155.png` | Bao 90 × 120 × 155, quạt 120 thổi −X, đế/heatpipe/gá/backplate |
| 18 | `design/families/parts/low_profile_47.png` | Bao 92 × 92 × 47, quạt 92 × 14 thổi −Z, bốn lỗ gá |
| 19 | `design/families/parts/aio_360.png` | Két 397 × 120 × 27, ba quạt, bơm giữ lại, ống 450, 12+12 vít |
| 20 | `design/families/parts/pcie_riser_400.png` | Dải 400, hai đầu khác nhau, chốt và tai gá, bán kính uốn |
| 21 | `design/families/parts/console_stand.png` | Đế 280 × 18 × 140, hai điểm gá, khe thoáng, bộ bốn chân nằm ngang |
| 22 | `design/families/parts/fan_hub_8.png` | Hộp/tai 92 × 50 × 18, 8 PWM + 8 ARGB, adapter nguồn ATX24 và dây điều khiển |

## 9. Kiểm tra trước khi giao dựng Blender

1. Đối chiếu mỗi ảnh đúng họ 01…06; cả sáu vỏ là model mới, Gaming giữ nguyên. Hông kín 02/04/05 không biến thành kính để khoe nội thất.
2. Dựng box bao theo tọa độ trước, rồi thêm hình học. Kiểm tra cả đầu cắm, lá PCIe, vít, ống và dây, không chỉ hộp tản. Các khoảng hở đã ghi là kiểm tra bố trí danh nghĩa; vẫn cần collision check trên mesh ở bước dựng.
3. Board/socket/slot giữ hướng; GPU ngang khớp chân x16, GPU dọc/sandwich có riser tới đúng mặt đầu cắm. Hai khe lá sau không đồng nghĩa card chỉ dày hai khe.
4. Đủ fan mặc định: **01=3 case+1 CPU; 02=3 case+1 CPU; 03=6 case+2 két; 04=1 case+1 CPU; 05=0 case+1 CPU; 06=5 case+3 két**. Cộng riêng rotor GPU và PSU, không dựng hai lần quạt két.
5. Mỗi quạt có cửa hút/xả thực và dây đúng đầu. Kính không dẫn gió; side-bank 03 có đường từ hông −Z, 04 có hai khoang, 05 có lưới và khoảng kê khi nằm ngang.
6. Kiểm tra từng G có lòng mở, viền, dây đến header; cửa CPU không dùng làm G. Không gộp PWM/ARGB thành một dây, không cắm USB-C vào header không hỗ trợ.
7. Sáu mặt case khớp vị trí I/O, khe PCIe, lỗ vít và chân. Bản case trống không còn quạt/bo/PSU; overview đủ BOM và đường lắp, một tờ cho mỗi file ở mục 8.
