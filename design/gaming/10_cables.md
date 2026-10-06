# 10 / cables — Bộ dây trong máy

Ảnh: [10_cables.png](10_cables.png). Theo [00_blueprint.md](00_blueprint.md), mục 1–5 và 7. Đơn vị mm; 1 đơn vị cảnh = 100 mm. Dây và đầu cắm đều màu đen; màu đường nét đứt chỉ dùng trong bản vẽ.

## Kích thước và các cụm

Bộ dây mềm không có kích thước bao cố định. Chiều dài dưới đây đo dọc dây, không gồm thân đầu cắm. Trên ảnh chỉ trải các đầu dây với đuôi 20–40 mm; khi dựng phải có đủ đường dây kín phía sau khay. Ba dây nguồn gắn liền PSU, không có đầu modular ở phía nguồn.

Kích thước thân đầu dưới đây là **lựa chọn dựng hình**, chưa được blueprint quy định và không phải bản vẽ sản xuất. Quy ước: ngang mặt cắm × cao mặt cắm × sâu theo hướng cắm, chưa tính ngàm. Làm rãnh khóa và đầu đối ứng khớp nhau; bo cạnh khoảng 0,3–0,5.

| Cụm / số lượng | Dài dây | Thân đầu cắm chọn | Mặt cắm và chi tiết phải dựng |
|---|---:|---|---|
| ATX / 1 | 600 | 51 × 11 × 20 | 24 hốc cái, 2 × 12; tách 20+4 thành 2 × 10 và 2 × 2. Bước tâm chọn 4,2; ngàm ngoài, hốc vuông/vát góc |
| EPS CPU / 1 | 650 | 18 × 11 × 20 | 8 hốc cái, 2 × 4; tách 4+4, mỗi nửa 2 × 2; dấu CPU và ngàm |
| PCIe GPU / 1 | 600 | 18 × 11 × 20 | 8 hốc cái, 2 × 4; tách 6+2 thành 2 × 3 và 2 × 1; dấu GPU, ngàm và khóa khác EPS |
| Bơm AIO / 1 | 350 | 11 × 4 × 14 | 4 hốc cái trên 1 hàng; gờ dẫn hướng; đầu còn lại gắn liền bơm |
| Chia Y quạt két / 1 | Đuôi 300; mỗi nhánh 150 | Đầu cái 11 × 4 × 14; mỗi vỏ đực 11 × 5 × 15 | Đầu vào 4 hốc; hai nhánh đều có vỏ 4 vị trí. Nhánh A đủ 4 tiếp điểm; B chỉ có 1, 2, 4, bỏ tiếp điểm 3 |
| Quạt vỏ / 4 | Chọn 650 mỗi dây, không vượt 650 | Mỗi đầu 11 × 4 × 14 | Mỗi dây 4 hốc cái trên 1 hàng; đầu kia gắn liền góc khung quạt; SYS_FAN1–4 |
| Nút/đèn trước / 1 bó, 4 đầu | 650 | Mỗi đầu 5,1 × 2,6 × 14 | Bốn vỏ 1 × 2: PWR SW, RESET SW, PLED +/−, HDD LED +/−; mỗi vỏ 2 hốc |
| USB 3.0 trước / 1 | 650 | 26 × 12 × 18 | Khung 2 × 10, 19 tiếp điểm; vị trí góc 20 bịt kín, có gờ định hướng và chống gãy đuôi |
| HD_AUDIO / 1 | 650 | 13 × 6 × 14 | Khung 2 × 5, 9 tiếp điểm; bịt vị trí 8, dấu chân 1; giữ bằng ma sát |
| Dây điện ngoài / 1, tham chiếu riêng | 1500 | C13 chọn 26 × 19 × 35 | 3 tiếp điểm cái ghép C14 của PSU; không đi vào G0–G6. Đầu ổ điện tường chưa chốt trong blueprint |

Dây nguồn chọn đường kính ngoài từng sợi 2,0; dây tín hiệu/quạt 1,2–1,4; bó USB đường kính khoảng 7; bó âm thanh khoảng 4. Đây là kích thước hình học mô phỏng, không phải chỉ định tiết diện điện. Bó nguồn dàn phẳng một lớp. Uốn dây nguồn và USB với bán kính ít nhất 20; không gập góc vuông.

Đầu ATX/EPS/PCIe có ngàm khóa rõ. Đầu quạt có gờ dẫn hướng và phần giữ tương ứng đầu trên bo. Đầu nút/đèn và HD_AUDIO giữ bằng ma sát, không thêm ngàm lớn. Các hốc cái có tiếp điểm kim loại **nằm sâu bên trong**; chỉ hai nhánh đầu ra Y có chân đực nhô ra. Dấu chân 1 là tam giác nhỏ trên vỏ. Không suy ra sơ đồ cấp điện từ màu dây.

## Chỗ cắm và đường đi

Tọa độ bo ở cột cuối là X; Y **cục bộ**, cộng tâm bo (−88; 62,5; −96) để ra tọa độ toàn máy; mặt linh kiện ở Z cục bộ 3. Vị trí tâm là mốc lắp ráp; chiều xoay đầu phải khớp ngàm trên ổ nhận.

| Dây | Nguồn → đường kín → lỗ ra | Đích trên bo / đoạn cuối lộ |
|---|---|---|
| ATX | PSU → G0 → rãnh trước sau khay → G2 | (117; 40), ngàm hướng ra mép bo; 25–35 |
| EPS | PSU → G0 → rãnh sau → mép nóc → G1 | (−60; 146); 20 |
| GPU | PSU → G0 → sau khay → quay lại G0 → khoang nguồn → ống che → G4 | Tâm GPU **toàn máy** (70; 10; 25), miệng nhận hướng +X; 25 |
| Bơm | Bơm → bọc sát ống nước → mép két → vùng G3 → sau khay → G3 | AIO_PUMP (10; 126); 20–30. Chỉ lộ 20 tại bơm |
| Y quạt két | Hai quạt → rãnh khung két → G3 → Y trong túi lõm sau khay → G3 | CPU_FAN (35; 126); 20–35 |
| Quạt trước trên | Góc quạt → rãnh mặt trước → G2 | SYS_FAN1 (106; 85); 25 |
| Quạt trước giữa | Rãnh mặt trước → G2 → xuống sau khay → G5 | SYS_FAN2 (82; −128); 20–35 |
| Quạt trước dưới | Rãnh mặt trước → G5 → ghim sau khay → G5 | SYS_FAN3 (95; −134); 20–30 |
| Quạt sau | Góc quạt → mép nóc → G1 → rãnh sau → G6 | SYS_FAN4 (−96; −135); 25–35 |
| USB trước | Cụm cổng vỏ → rãnh trước → G2 → xuống sau khay → G5 | USB 19 (113; −127); 35–40 |
| Nút/đèn | Cụm nút vỏ → rãnh trước → G2 → xuống sau khay → G5 | F_PANEL (95; −143); 20–30, chỉ tách bốn đầu sát bo |
| Âm thanh | Cụm jack trước → rãnh trước → G2 → mép dưới sau khay → G6 | HD_AUDIO (−97; −143); 20–30, chạy riêng khỏi bó nguồn |

Bơm dùng AIO_PUMP trực tiếp, tải thiết kế dưới 1 A. Hai quạt két dùng chung CPU_FAN, tổng tải dưới 1 A. Đầu quạt theo thứ tự chức năng 1 GND, 2 +12 V, 3 báo tốc độ, 4 PWM; nhánh Y thứ hai bỏ số 3 để chỉ một quạt trả tốc độ.

### Mốc G0–G6 cho hình sau khay

Các số trong bảng sau là tọa độ **toàn máy**, giữ nguyên khi đổi góc nhìn. Nhìn từ vách kín: mặt trước ở trái, lưng máy ở phải. G4 là miệng ống che, không phải lỗ xuyên khay.

| Lối | Tâm | Phần mở hữu dụng |
|---|---|---|
| G0 | X −35; Y −170 | 45 × 28 |
| G1 | X −148; Y 224 | 32 × 10 |
| G2 | X 57; Y 110 | 22 × 55 |
| G3 | X −65; Y 224 | 40 × 10 |
| G4 | X 95; Y 10; Z 25; cửa hướng −X | 18 × 24 theo Z × Y |
| G5 | X 23; Y −104 | 48 × 16 |
| G6 | X −184; Y −104 | 42 × 16 |

G1/G3 ở trên mép bo Y = 215; dây vòng qua mép, không xuyên PCB. Viền cao su rộng 3 thuộc vỏ. Ống G4 có lòng 24 × 28, đi kín sát phía trước GPU, tránh cánh quạt; chỉ đoạn cuối 25 lộ ra. Rãnh khung rộng 25, sâu hữu dụng 10. Sáu đai dán: hai rãnh trước, hai rãnh sau, hai mép dưới. Dây thừa cuộn trong khoang nguồn, không chồng sau CPU. Khoảng 14 từ vách tới lưng bo gồm cả khay và chân đế, không phải 14 trống để nhét dây.

### Khóa chân và cực đèn

F_PANEL nhìn từ mặt linh kiện, theo dấu chân 1; đây là sơ đồ của **bo mô phỏng**:

| Hàng | Cột 1 | Cột 2 | Cột 3 | Cột 4 | Cột 5 |
|---|---|---|---|---|---|
| Trên | 2 PLED + | 4 PLED − | 6 PWR SW | 8 PWR SW | 10 khuyết |
| Dưới | 1 HDD LED + | 3 HDD LED − | 5 RESET SW | 7 RESET SW | 9 không dùng |

Hai nút không phân cực. Mỗi đèn dùng một vỏ hai vị trí với dấu +/−. HD_AUDIO dùng khung tương tự 2 × 5 nhưng **khuyết chân 8**, không khuyết 10. Sơ đồ nhìn mặt bo và mặt cắm của đầu dây là hai mặt đối nhau; đảo tương ứng khi dựng, không chép nguyên ảnh thành mặt đối ứng. USB có 20 vị trí nhưng chỉ 19 tiếp điểm. Các đầu nguồn phải thể hiện hốc vuông/vát góc và seam 20+4, 4+4, 6+2; không dùng chung một mesh EPS/GPU chỉ đổi chữ.

## Vật liệu, chỗ giữ và điểm người mới cần thấy

- Vỏ dây PVC/TPE đen `#15171B`, hơi mềm, độ nhám chọn 0,6; vỏ đầu nhựa đen `#202329`, nhám 0,45; tiếp điểm kim loại `#BFC4CA`; chữ `#E5E7EB`; đai vải `#25282E`.
- Đường nét đứt: ATX `#22D3EE`, EPS `#F59E0B`, GPU `#A855F7`, bơm/quạt `#65A30D`, USB `#2563EB`, nút/đèn `#F97360`, âm thanh `#6B7280`. Không tô màu này lên dây thực.
- **Lỗ vít: 0; vít tháo rời: 0.** Giữ bằng ngàm, ma sát và sáu đai dán của vỏ; không thêm vít vào đầu cắm. Đai, lỗ G và ống che thuộc phần 01, không đếm thành phần cứng mới.

Năm điểm trình mô phỏng cần làm nổi bật:

1. CPU dùng 4+4, GPU dùng 6+2; cùng tám chân nhưng khác khóa.
2. ATX ghép đủ 20+4; ấn đúng chiều đến khi ngàm giữ, không ép đầu lệch.
3. Nhánh Y thứ hai thiếu tiếp điểm báo tốc độ là có chủ ý; bốn quạt vỏ có bốn dây riêng.
4. PLED/HDD LED phải đúng +/−; đối chiếu lỗ bịt của USB và HD_AUDIO trước khi cắm.
5. Dây đi sau khay, chỉ lộ 20–40 sát đầu; không chạm quạt hay chắn RAM. Cắm điện ngoài cuối cùng. Bộ này không có dây SATA hoặc RGB/ARGB.

Ảnh tạo sinh dùng để xem bố cục và vật liệu. Số lượng hốc, khóa, tọa độ và kích thước khi dựng phải theo bảng trên; không đo trực tiếp từ phối cảnh.

**Kiểm tra ảnh sau một lần sửa:** ảnh còn sai số hốc ở một số đầu ATX/EPS, bơm và quạt; một số hình ghi 4 nhưng chỉ vẽ 3. Chi tiết HD_AUDIO phóng lớn còn thừa cột, và hình F_PANEL chưa thể hiện đủ chín chân. Không sao chép những mặt cắm này thành mesh. Dựng lại mặt cắm theo bảng và sơ đồ chân trong tài liệu; đường GPU phải quay lại G0 trước khi lên ống G4, dù nét đứt trong ảnh chưa mô tả rõ đoạn quay lại. Đã giữ giới hạn một lần sửa ảnh theo brief.

<details>
<summary>Lệnh tạo ảnh và sửa ảnh — công cụ imagegen tích hợp</summary>

Lệnh tạo ảnh:

```text
Use case: scientific-educational.
Create ONE high-resolution landscape product reference sheet for a 3D modeller, title "10 / CABLE KIT", subtitle "Dimensions in mm". Light grey-white background, realistic black plastic connectors and black insulated wires, soft studio lighting, precise blue dimension lines and short readable English labels. Brand-free. Large neatly arranged cable kit in three-quarter isometric views fills left 76%; two smaller supporting panels at right: behind-tray route inset above, F_PANEL pin diagram below. Use generous space, crisp countable connector cavities, restrained navy typography. No marketing. The illustration is a connector modelling reference: use only short 20–40 mm wire tails with break marks; full cable lengths are labels, not long tangled coils. Every kit item has a close, readable mating face AND visible side showing latch or guide/key; there is no need to invent latches on plain friction plugs.

Arrange numbered groups in orderly rows:
01 ATX 24 / 20+4 / 600 mm / G0 > G2. Exactly 24 female cavities in 2 rows of 12. Main 20-position body is 2x10, detachable 4 is 2x2, seam after column 10; show halves slightly separated but aligned. Square and chamfered key cavities, external retaining latch.
02 CPU EPS 8 / 4+4 / 650 mm / G0 > G1. Exactly 2 rows of 4 cavities, split into two 2x2 halves. Molded CPU label, latch, square/chamfered keys.
03 GPU PCIe 8 / 6+2 / 600 mm / G0 > G4. Exactly 2 rows of 4 cavities, split into 2x3 plus 2x1. Molded GPU label. Show latch and keyed cavities. Clear visual difference from CPU EPS.
04 AIO PUMP / 4-pin / 350 mm / G3. Exactly one female 1x4 socket, visible guide rails and retention feature, four black wires. Source end is fixed to pump, depict a small break mark labelled "Fixed to pump", not a modular PSU connector.
05 FAN Y / CPU_FAN / G3. One female 1x4 input on 300 mm tail branching into TWO male outputs each on 150 mm branch. Both output housings have FOUR positions: output A has 4 metallic pins, output B has ONLY 3 metallic pins at positions 1,2,4; position 3 is visibly empty. Labels "4 contacts", "3 contacts", "No tach at 3". Guide ribs and retaining tabs.
06 CASE FANS x4 / 4-pin each / Up to 650 mm. Exactly FOUR individual female 1x4 fan cable connectors, side by side, numbered SYS_FAN1 / G2, SYS_FAN2 / G5, SYS_FAN3 / G5, SYS_FAN4 / G6. Not three, not five. Four countable cavities each, guide rails.
07 FRONT PANEL / 650 mm / G5. Exactly FOUR small black 2-position female housings: "PWR SW", "RESET SW", "PLED + -", "HDD LED + -". Each has 2 countable cavities. Label "Friction fit"; do not add giant latches. LED polarity readable.
08 USB 3.0 / 19 contacts / 650 mm / G5. Large black 2x10 female connector with EXACTLY 20 positions, 19 open and one blocked at corner (position 20); central polarizing guide, strain relief and thick black cable. Label "19 + blocked key".
09 HD_AUDIO / 9 contacts / 650 mm / G6. Small 2x5 female connector with 10 positions, nine open and one blocked at position 8 (fourth column on even-numbered row). Label "Key at 8". Show pin-1 mark and friction fit, not a fake latch.

Three PSU cable source ends are permanently fixed, represented by short cut tails labelled "Fixed PSU leads", NEVER modular plugs. Fan cables fixed to fans and front I/O cables fixed to front panel, no invented second detachable ends. Connector enlargement shows physical cavity/key detail. Use thin blue leaders to label "Latch", "Key", and "Pin 1" where appropriate. Add dimension arrows "20–40" along one tail and "R >= 20" beside curved power/USB wire.

RIGHT TOP supporting inset "BEHIND TRAY / FRONT < | > REAR", small realistic case back-tray elevation, FRONT IS LEFT and REAR IS RIGHT because seen opposite glass side. All seven G0–G6 labels visible exactly once. Overall enclosure 450 horizontal x 470 vertical. Project world locations onto rear view: G1 at upper right (X=-148,Y=224), G3 top center (X=-65,Y=224), G2 left-upper (X=57,Y=110), G5 left-lower (X=23,Y=-104), G6 right-lower (X=-184,Y=-104), G0 near bottom center-right (X=-35,Y=-170), PSU bottom right. G4 is NOT a tray hole: show a ghosted covered riser on left from PSU shroud up to GPU socket, final outlet (X=95,Y=10,Z=25) with 25 mm tail, label "G4 / Covered riser". Cable routes dashed overlays only: cyan ATX from PSU through G0 then left perimeter to G2; amber EPS through G0 then right perimeter to G1; purple GPU through G0 behind tray then back through G0 into PSU chamber then hidden shroud riser to G4. Green pump and radiator fan at G3, green case fans via G2/G5/G6, blue USB and coral front-panel via G5, grey audio separately at G6. Black physical cables under colored dashed schematic lines. Six hook-and-loop straps: 2 left, 2 right, 2 bottom. CPU access cutout empty. Neat rounded routes, no diagonal crossing of PCB, no wires through CPU cutout, no wires in fans. Small legend words Power / Fan-pump / Front I/O, paired with connector labels.

RIGHT BOTTOM small exact diagram "F_PANEL / BOARD VIEW":
Top row five positions labelled "2 +" "4 -" "6" "8" "10 KEY"
Bottom row five positions labelled "1 +" "3 -" "5" "7" "9 NC"
Top pair 2,4 bracket labelled PLED; pair 6,8 bracket PWR SW.
Bottom pair 1,3 bracket HDD LED; pair 5,7 bracket RESET SW.
Position 10 absent, position 9 unused.
Footer small "No cable screws. No SATA / RGB." Include a tiny separate marginal reference "EXTERNAL ONLY / C13 / 3 contacts / 1500 mm" with an IEC C13 end only; this is outside internal kit, no internal route. No wall plug specification needed.

Prioritize accurate counts and readable compact labels over decorative detail. Do not draw complete PC components in the kit, only connectors and short wires. No logos or fake product stickers.
```

Lệnh sửa ảnh một lần:

```text
Correct this existing cable-kit sheet. Keep the entire layout, style, kit groups, routes, lengths, labels and light background. This is a PRECISION CONNECTOR COUNT CORRECTION; correct all physical faces, not just their labels. Use simple clearly separated cavities and reduce decorative connector duplicates if necessary. Do not put male pins in cable female sockets: show recessed metal sleeves inside square openings, not projecting pins.

01 ATX hero is WRONG: replace its main body with EXACTLY TEN columns of TWO cavities (20); detached piece EXACTLY TWO columns of TWO cavities (4). The assembled straight-on diagram below must have TWELVE columns of TWO cavities (24) with split seam after column 10. Current detached 4 body has six; remove surplus column.
02 CPU EPS hero both halves are WRONG: each half must have EXACTLY TWO columns and TWO rows (four cavities per half). Joined front diagram below EXACTLY four columns x two rows.
03 GPU hero: main body EXACTLY THREE columns x TWO rows (six), detachable EXACTLY ONE column x TWO rows (two). Current detachable piece has four; remove surplus column. Joined diagram FOUR columns x TWO rows.
04 AIO PUMP hero currently has 3 cavities. Replace with EXACTLY FOUR female cavities in one single horizontal row, 4 wires. Same exact four cavities in any alternative face view.
05 Y splitter input must be EXACTLY ONE ROW OF FOUR FEMALE CAVITIES, not 2 rows. Keep two male branch output details: A has exactly 4 projecting pins at 1,2,3,4; B has only 3 pins at 1,2,4, position 3 empty. Show FOUR housing positions on both outputs. 300 mm input, each branch 150 mm.
06 All FOUR case fan hero connectors currently show 3 cavities. Every one must have EXACTLY FOUR square FEMALE cavities in ONE horizontal row and four wires. There must remain exactly FOUR case fan cable assemblies.
07 FRONT PANEL keep exactly four 2-hole female housings. In tiny lower polarity labels, use + and - for each LED, and 1 2 for switches. Do not write "4 2". Labels PWR SW, RESET SW, PLED + -, HDD LED + -.
08 USB hero and straight-on detail must both be EXACTLY TEN columns by TWO rows (20 positions). Nineteen open female cavities and ONE blocked corner. Pin 1 at upper left; number rows 1..10 above and 11..20 below as pictorial position labels if necessary, with position 20 bottom right blocked.
09 HD_AUDIO hero and straight-on detail currently have 8 positions. Both must have EXACTLY FIVE columns by TWO rows (10 positions), nine open cavities, ONE blocked at pin 8. Define consistent board-view row numbering: TOP ROW 2,4,6,8,10; BOTTOM ROW 1,3,5,7,9. Block fourth column in TOP row; pin 1 mark bottom left. Label "9 contacts / Key at 8". Its cavity diagram is a key-location reference, not a mating-face numbering chart. No extra latch on HD_AUDIO.
C13 external connector has exactly three female rectangular slots and zero male pins. Correct its detail if it looks like male C14.
F_PANEL diagram: depict actual nine projecting HEADER pins on board, five positions per row. Top 2,4,6,8,10 absent; bottom 1,3,5,7,9 present but 9 unused. Preserve pair bracket labels.
Behind tray inset: case FRONT remains left, REAR right. PSU is the rectangle at BOTTOM RIGHT; remove an apparent second PSU at bottom left. Six flat straps (2 left,2 right,2 bottom). G4 remains a covered riser, not a tray hole. Purple GPU route goes PSU > G0 > behind tray > BACK THROUGH G0 > enclosed shroud > G4. Make this return leg clear. Add small note "Fixed PSU leads" below the top three cards. Preserve G0 G1 G2 G3 G4 G5 G6 labels.
Everything else unchanged. Accurate integer cavity counts take highest priority.
```

</details>
