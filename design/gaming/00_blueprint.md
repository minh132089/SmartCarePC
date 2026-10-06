# Bản vẽ tổng thể bộ máy Gaming

Đây là bước 0: chốt cách bố trí, chỗ cắm dây và chỗ bắt vít để dựng mô hình. Chưa vẽ riêng từng linh kiện, chưa sửa mã nguồn và chưa dựng Blender. Hình đi kèm: [00_blueprint.png](00_blueprint.png). Số 1–10 trong hình dùng chung với bảng dưới đây.

Máy không có logo hay tên sản phẩm thật. Thân màu `#16181d`, mặt trước `#101216`, điểm nhấn xanh `#22d3ee`. Chữ trong hình dùng tiếng Anh ngắn; phần hướng dẫn người học dùng tiếng Việt.

## 1. Dữ liệu gốc và quy ước dựng hình

- Đọc `src/data/builds.ts`, `src/data/layout.ts`, `src/data/parts.ts`; không sửa các tệp này.
- **1 đơn vị cảnh = 100 mm.** Kích thước trong tài liệu đều là mm.
- Các số lấy từ mã nguồn: vỏ 220 × 470 × 450; bo ATX 244 ngang × 305 cao; GPU 280 × 125 × 55, ba quạt; hai thanh RAM; ba quạt trước 120 và một quạt sau 120; khoảng từ vách phải đến lưng bo 14; các vị trí CPU, RAM, PCIe, M.2 và hai đầu nguồn trên bo.
- Các số còn lại là **lựa chọn thiết kế cho mô hình chung**, không phải số đo của sản phẩm có thật. Bao gồm kích thước CPU, loại RAM, bộ gá AIO, chiều dài dây, chân cắm phụ và vị trí lỗ vít. Đây là bản hướng dẫn mô phỏng, không phải bản vẽ để khoan một bo mạch thật.
- Gốc tọa độ toàn máy ở tâm vỏ: **X** từ lưng máy (−) ra mặt trước (+), **Y** từ đáy (−) lên nóc (+), **Z** từ vách kín (−) ra kính (+). Nhìn từ kính: lưng máy bên trái, mặt trước bên phải. Hình phía sau khay là mặt nhìn đối diện, nên trái/phải đảo lại; mã G vẫn giữ nguyên.
- Vỏ nằm trong X = −225…225, Y = −235…235, Z = −110…110. Tâm bo theo `boardOrigin(gaming)` là **(−88; 62,5; −96)**. Tọa độ cục bộ của bo lấy tâm bo làm gốc; mặt linh kiện ở Z cục bộ = 3, tức Z toàn máy = −93.
- Mép bo: X toàn máy −210…34; Y −90…215. `BOARD_STANDOFF = 14` là tổng khoảng từ vách phải tới lưng bo, **không phải chiều dài riêng của chân đế**. Chọn khay có mặt đỡ ở Z = −102,5 và chân đế cao 6,5 để lưng bo ở Z = −96. Dành phần sau khay cho dây dẹt, tránh chồng bó dây.

### Các điểm đặt linh kiện phải giữ theo mã nguồn

| Điểm | X, Y cục bộ trên bo | Cách dùng |
|---|---|---|
| Tâm CPU | 19; 59 | Tâm socket và tâm bơm AIO trùng nhau |
| Bốn khe RAM A1, A2, B1, B2 | X = 70; 79; 89; 98. Y = 62 | Hai thanh lắp **A2 và B2**, tức chỉ số 1 và 3 |
| Khe PCIe chính | −34; −30 | GPU lắp khe đầu tiên |
| Hai khe PCIe còn lại | −34; −91 và −34; −132 | Để trống, vẫn thể hiện khe và chốt |
| Tâm hình SSD M.2 | 35; −62 | Thanh dài theo X; đầu cắm ở X = −5, đầu vít X = 75 |
| ATX 24 chân | 117; 40 | Sát mép trước của bo |
| EPS CPU 8 chân | −60; 146 | Sát mép trên của bo |

## 2. Danh sách linh kiện

Thứ tự kích thước được ghi riêng trong mỗi ô để tránh nhầm chiều. Phần vít, lỗ luồn dây và nắp che thuộc linh kiện chứa chúng, không tạo thêm mã phần trong ứng dụng.

| Số / ID | Linh kiện và số lượng | Kích thước thực chọn cho mô hình | Chi tiết phải thấy |
|---|---|---|---|
| 1 / `case` | 1 vỏ mid-tower, 1 kính, 1 nắp kín, 1 nắp che nguồn | Vỏ rộng 220 × cao 470 × sâu 450; kính sâu 430 × cao 445 × dày 4; tôn khung dày 1; nắp che nguồn ở Y = −120 | Kính trong; khay bo; bảy cửa PCIe phía sau; lưới trước; lưới nóc; lọc bụi đáy; G0–G6; chân đế và tất cả lỗ bắt vít |
| 2 / `motherboard` | 1 bo ATX | Ngang 244 × cao 305; PCB dày 1,6, chi tiết bề mặt nằm trong mức `surface = 3` | Socket CPU có nắp và cần gạt; bốn khe RAM; ba khe PCIe; một khe M.2; các đầu cắm ở mục 3; tấm I/O gắn sẵn |
| 3 / `cpu` | 1 CPU kiểu LGA chung | 37,5 × 37,5 × 4,5; vùng socket bao ngoài 50 × 50 | Mặt tản nhiệt trơn; dấu tam giác; hai rãnh định hướng; tiếp điểm phẳng bên dưới. CPU không có chân nhô ra |
| 4 / `cooler` | 1 AIO 240 gồm bơm, két, hai quạt, hai ống, bộ gá | Bơm đường kính 54 × cao 48; két dài 277 × ngang 120 × dày 27; mỗi quạt 120 × 120 × 25; mỗi ống dài khoảng 350, đường kính ngoài 11 | Bơm trên CPU; két dưới nóc; hai quạt dưới két thổi lên; hai đầu ống tại bơm và hai đầu tại két; bộ gá bốn góc |
| 5 / `ram` | 2 thanh DDR4 có tản nhiệt | Mỗi thanh dài 133,35 × cao 42 × dày 6 | Một rãnh khóa lệch tâm, hai chốt khe; không dùng hình rãnh của DDR5 |
| 6 / `gpu` | 1 GPU, ba quạt | Dài 280 × nhô khỏi bo 125 × dày 55; mỗi quạt đường kính khoảng 80; lá bắt phía sau chiếm hai cửa PCIe | Ba quạt quay xuống đáy; chân PCIe; chốt khe; một ổ nguồn 8 chân; hai vít giữ lá bắt |
| 7 / `ssd` | 1 SSD M.2 NVMe 2280 | Ngang 22 × dài 80 × dày tối đa 3; chân đỡ cao 3 | Đầu M-key; rãnh bán nguyệt ở đầu giữ vít; không có dây SATA |
| 8 / `psu` | 1 nguồn ATX có dây liền | Rộng 150 × cao 86 × sâu 140 | Quạt hút xuống đáy; công tắc; ổ điện IEC C14 ở lưng; dây ra ở đầu phía trước; không có ổ modular để người học cắm nhầm |
| 9 / `fans` | 4 quạt vỏ: 3 trước, 1 sau | Mỗi quạt 120 × 120 × 25; khoảng tâm lỗ 105 × 105 | Bốn lỗ mỗi quạt; dây 4 chân; mũi tên chiều gió và chiều quay. Hai quạt trên két thuộc số 4, không tính lại ở đây |
| 10 / `cables` | 3 dây nguồn trong máy; dây bơm; dây chia hai quạt két; 4 dây quạt vỏ; dây USB, âm thanh, nút/đèn; 1 dây điện ngoài | Dây ATX 600; EPS 650; GPU 600; bơm 350; nhánh chia quạt két 150 mỗi nhánh và đuôi 300; dây quạt vỏ tới 650; dây mặt trước 650; điện ngoài 1500 | Vỏ dây đen, đầu cắm có khóa; đường màu chỉ là ký hiệu trong bản vẽ. Gập dây có bán kính, không gập vuông góc |

**Khoảng hở cần giữ:** thân GPU dày 55 mm nên chiếm gần ba khoảng khe, dù lá bắt chỉ hai khe. Để trống cả vùng ngay dưới hai khe đó; không thu nhỏ thân GPU thành 40 mm. SSD nằm sau vùng GPU nên lắp SSD trước GPU.

Két và quạt dày tổng cộng 52 mm, trong khi mép bo chỉ cách nóc 20 mm. Đặt cụm két ở X = −140…137, Y = 179…231, Z = −24…96: két ở trên, quạt ở dưới. Cụm này lệch về phía kính, không nằm đè trên mép bo hoặc RAM. Chừa 4 mm tới nóc và ít nhất 10 mm tới kính sau khi tính mép khung. Vị trí này là lựa chọn của bản vẽ; bước 0 chưa thay cách đặt linh kiện trong mã nguồn.

Quạt sau lấy Z tâm = **14 mm** theo `rearFanZ(gaming)`; chọn Y tâm = 145 và bề dày tại X = −224…−199. Ba quạt trước có Y tâm = −85, 40, 165; Z tâm = 0; bề dày X = 197…222. Quạt trên cùng không chạm két vì két kết thúc tại X = 137. Nắp che nguồn dừng ở X = 170 để phần dưới quạt trước vẫn hút thoáng. Nguồn nằm sát đáy, phía sau, bên dưới nắp che.

## 3. Đầu cắm, số chân và chỗ cắm

Vị trí mới trong bảng là tâm đầu cắm, đơn vị mm, theo **X; Y cục bộ trên bo**. Khi dựng, luôn thêm gờ khóa và dấu chân 1. Tọa độ đầu nguồn ATX/EPS phải giữ theo mã; các đầu phụ dưới đây là thiết kế của bo chung.

| Đầu cắm | Từ → đến | Số chân / tiếp điểm | Vị trí và cách nhận biết |
|---|---|---|---|
| ATX | Nguồn → bo | 24, bố trí 2 × 12; đầu dây 20+4 ghép kín | (117; 40), sát mép trước bo; ngàm khóa hướng ra mép bo |
| EPS CPU | Nguồn → bo, cấp điện CPU | 8, bố trí 2 × 4; đầu dây 4+4 | (−60; 146), mép trên. Ghi `CPU`; không dùng đầu GPU 6+2 tại đây |
| PCIe power | Nguồn → GPU | 8, bố trí 2 × 4; đầu dây 6+2 | Ổ trên đầu phía trước của GPU, gần mép hướng kính; chọn tâm toàn máy khoảng (70; 10; 25), cửa cắm hướng +X |
| AIO_PUMP | Bơm → bo | 4: GND, 12 V, báo tốc độ, PWM | (10; 126). Bơm dùng nguồn trực tiếp từ đầu này; thiết kế tải bơm dưới 1 A |
| CPU_FAN | Hai quạt két → dây chia Y → bo | 4 trên đầu vào; hai nhánh có vỏ 4 vị trí | (35; 126). Chỉ một nhánh trả tốc độ, nhánh kia bỏ tiếp điểm tốc độ để tránh hai tín hiệu chồng nhau; tổng tải hai quạt chọn dưới 1 A |
| SYS_FAN1 | Quạt trước trên → bo | 4 | (106; 85), dùng lối G2; nằm trên đầu ATX để hai đầu không chồng nhau |
| SYS_FAN2 | Quạt trước giữa → bo | 4 | (82; −128), dùng lối G5 |
| SYS_FAN3 | Quạt trước dưới → bo | 4 | (95; −134), dùng lối G5 |
| SYS_FAN4 | Quạt sau → bo | 4 | (−96; −135), dùng lối G6; dây đi xuống sau khay |
| F_PANEL | Nút nguồn, nút khởi động lại, hai đèn → bo | 9 chân trên khung 2 × 5, thiếu chân 10; sử dụng 8 chân | (95; −143), mép dưới phía trước; sơ đồ riêng bên dưới |
| USB 3.0 mặt trước | Hai cổng USB-A trên vỏ → bo | 19 chân trên khung 20 vị trí, một vị trí khóa | (113; −127), mép trước phía dưới; một đầu cắm lớn, không xoay hoặc ép lệch |
| HD_AUDIO | Hai ổ âm thanh 3,5 mm trước vỏ → bo | 9 chân trên khung 10 vị trí, thiếu chân 8 | (−97; −143), mép dưới phía sau; đầu cáp có một lỗ bịt tương ứng |
| SATA | Không dùng | Không có cáp SATA trong bộ máy này | SSD dùng M.2; bơm dùng AIO_PUMP; không thêm bộ điều khiển cần nguồn SATA |
| RGB / ARGB | Không dùng | Không có | Điểm nhấn cyan là vật liệu của mô hình, không giả làm hệ đèn cần dây |
| Socket CPU | CPU → socket LGA | Quy ước mô hình: 1.200 tiếp điểm dưới CPU, không phải đầu dây | (19; 59); có khung ép, bản lề và cần khóa. Chỉ mô tả bề mặt tiếp điểm ở bản vẽ tổng thể |
| DIMM A2/B2 | Hai thanh RAM → bo | 288 tiếp điểm mỗi thanh DDR4 | X = 79 và 98, Y = 62; khớp rãnh rồi ấn thẳng; các khe A1/B1 vẫn trống |
| PCIe x16 | GPU → bo | 164 tiếp điểm trên đầu nối x16 | (−34; −30); chốt ở đầu phía trước; không nhầm với ổ nguồn GPU 8 chân |
| M.2 M-key | SSD → bo | Đầu nối theo khung 75 vị trí, 67 tiếp điểm sau phần khóa M | Miệng khe tại (−5; −62), vít tại (75; −62); trượt SSD vào khe trước khi hạ đầu giữ vít |
| Điện ngoài | Dây điện → nguồn | IEC C13 trên dây ghép C14 trên nguồn, 3 tiếp điểm | Lưng nguồn; chỉ nối ở bước cuối |

Các đầu xuất hình trên lá GPU chọn ba DisplayPort và một HDMI; cụm I/O bo chọn hai USB-A, một RJ45 và ba jack 3,5 mm. Đây là cổng ngoài, không có dây nội bộ để người học tháo. Chúng vẫn phải được vẽ thành lỗ cắm, không thay bằng mặt phẳng. Số tiếp điểm tương ứng: DisplayPort 20; HDMI 19; USB-A ở đây chọn loại USB 2.0 có 4; RJ45 có 8; jack TRS có 3. USB-A trước vỏ là USB 3.0, mỗi cổng có 9 tiếp điểm.

### Sơ đồ F_PANEL của bo mô phỏng

Nhìn vuông góc mặt linh kiện, theo dấu chân 1 trên bo; **không coi đây là sơ đồ chung cho mọi bo thật**:

```text
Hàng trên:   2 LED nguồn + | 4 LED nguồn − | 6 Nút nguồn | 8 Nút nguồn | 10 khuyết
Hàng dưới:   1 LED ổ đĩa + | 3 LED ổ đĩa − | 5 Reset    | 7 Reset    |  9 không dùng
```

Hai nút không phân cực. Hai đèn phải đúng +/−. Bốn đầu dây đều là vỏ hai vị trí, đánh dấu `PWR SW`, `RESET SW`, `PLED`, `HDD LED`. Trong hình tổng thể chỉ ghi `F_PANEL 9`; sơ đồ chân đầy đủ nằm ở đây để thợ dựng không tự đoán.

## 4. Vít và chỗ bắt

Chọn thống nhất ren M3 cho bộ gá AIO và két của mô hình này; két ngoài đời có thể dùng ren khác. Chiều dài vít tính dưới đầu vít. Không dùng vít dài bắt két trực tiếp vì có thể chạm đường nước. Đầu vít chữ thập, trừ vít tay của hai nắp hông.

| Mã | Phần cần giữ | Số lượng | Loại chọn | Vị trí / cách dựng |
|---|---|---:|---|---|
| S1a | Chân đế bo → khay | 9 | Chân đế lục giác cao 6,5; ren ngoài M3, lỗ trong M3 | Chín vị trí khớp chín lỗ trên bo; không đặt chân đế dưới vùng không có lỗ |
| S1b | Bo → chân đế | 9 | M3 × 5, đầu tròn | Một vít cho mỗi chân đế; đưa từ phía kính vào khay |
| S2 | Bộ gá bơm → tấm đỡ sau CPU | 4 | Vít M3 × 12 có lò xo, lỗ ren trong trụ gá | Bốn góc hình vuông 78 × 78, tâm trùng CPU; tấm đỡ có bốn trụ liền, không phát sinh bốn vít phụ |
| S3 | Két → giá nóc | 8 | M3 × 6 với 8 vòng đệm dày 1 | Bốn điểm trên mỗi vùng quạt 120; vít từ nóc xuống két; phần ren ăn vào két khoảng 4 |
| S4 | Hai quạt → két | 8 | M3 × 30 với 8 vòng đệm dày 1 | Từ mặt dưới quạt xuyên khung 25 vào két khoảng 4; bốn vít mỗi quạt |
| S5 | Bốn quạt vỏ → vỏ | 16 | Vít quạt tự tạo ren đường kính danh nghĩa 5 × dài 10 | 12 ở mặt trước, 4 ở lưng; bốn góc từng quạt, bước lỗ 105 × 105 |
| S6 | Lá giữ GPU → vỏ | 2 | #6-32 UNC × 6 | Hai tai trên hai cửa PCIe mà GPU chiếm; lắp từ ngoài lưng vỏ |
| S7a | Chân đỡ SSD → bo | 1 | Chân đỡ cao 3, ren mô hình M2 | Tại (75; −62), lắp sẵn trên bo |
| S7b | SSD → chân đỡ | 1 | M2 × 2,5, đầu thấp | Vít giữ rãnh bán nguyệt cuối SSD; không ép cong SSD |
| S8 | Nguồn → lưng vỏ | 4 | #6-32 UNC × 6 | Theo bốn lỗ trên mặt sau nguồn; giữ nguồn từ phía sau |
| S9 | Kính → vỏ | 4 | Vít tay #6-32 UNC × 8, có 4 đệm cao su | Bốn góc kính; kính nằm trên đệm, không chạm trực tiếp đầu kim loại |
| S10 | Nắp kín → vỏ | 2 | Vít tay #6-32 UNC × 6 giữ liền nắp | Mép sau, trên và dưới; nắp trượt vào rãnh rồi khóa vít |
| S11 | Các nắp cửa PCIe còn lại | 5 | #6-32 UNC × 6 | Một vít mỗi nắp chưa tháo; giữ sẵn trên vỏ. Khi chưa có GPU có 7 nắp/7 vít; tháo 2 nắp và dùng lại 2 vít làm S6 |

**Tổng ở trạng thái đã lắp:** 63 vít giữ, 10 chân đế/đỡ có ren (9 của bo và 1 của SSD), 16 vòng đệm két và 4 đệm kính. Không cộng lại hai vít nắp PCIe đã dùng cho GPU. Bốn vít S2 là vít giữ bơm, không phải vít lắp CPU.

Chốt RAM, cần socket CPU, chốt PCIe không dùng vít tháo rời. Mặt trước dùng ngàm; lọc bụi dùng nam châm; nắp che nguồn và khung dùng mối ghép cố định. Tấm I/O và vỏ trong của PSU/GPU/AIO là cụm kín đã ráp sẵn, không có thao tác mở trong bài học. Nếu vẽ vít trang trí trên các cụm kín, dùng màu mờ và không tạo điểm tương tác.

### Mốc lỗ cho người dựng mô hình

- Bo chung dùng lưới chín lỗ tại các tổ hợp X = −112, 0, 112 và Y = −142,5, 0, 142,5, theo tâm bo. Đây là **lưới mô phỏng đã chọn**, không khẳng định là mẫu khoan ATX tiêu chuẩn hay lỗ của model hiện có. Đường kính lỗ 3,4; vùng trống quanh tâm bán kính 4. Nếu tái sử dụng mesh bo hiện có, đo lại lỗ và cập nhật đồng thời khay cùng bảng này trước khi dựng bước tiếp theo.
- Bốn lỗ gá bơm có tâm cục bộ (−20; 20), (58; 20), (−20; 98), (58; 98). Tấm đỡ sau bo 90 × 90, có đệm cách điện; chừa cửa khay 100 × 100 phía sau CPU để tiếp cận. Cửa bảo trì CPU không dùng làm lỗ luồn dây.
- Hai cụm lỗ két có tâm X toàn máy −61,5 và 58,5; Z tâm = 36. Mỗi cụm có bốn lỗ lệch ±52,5 theo X và Z. Cùng vị trí lỗ trên và dưới két, nhưng S3 và S4 đi vào hai mặt khác nhau.
- Bốn lỗ nguồn chọn tọa độ theo góc dưới trái mặt sau nguồn, nhìn từ ngoài lưng máy: (6; 6), (144; 6), (6; 80), (138; 70), đơn vị ngang × cao. Mẫu bất đối xứng này là lựa chọn cho cụm nguồn chung; vỏ phải khoan khớp cụm nguồn này.
- Bốn lỗ kính cách mỗi mép 15. Hai vít nắp kín cách mép trên/dưới 20. Lỗ tai lá PCIe và lỗ vỏ phải trùng nhau; dùng bước cao cửa PCIe 20,32, chừa khoảng thân GPU 55 như đã nêu.

## 5. Lỗ luồn dây và đường đi kín

Mỗi lỗ có viền cao su đen, mép bo tròn và dấu G. Kích thước là phần mở hữu dụng; viền cao su rộng 3. Với lỗ trên khay, kích thước theo X × Y. Tọa độ dưới đây theo **toàn máy**, không theo tâm bo.

| Mã | Tâm / vị trí | Kích thước mở | Chức năng |
|---|---|---|---|
| G0 | Khay phía khoang nguồn, X = −35; Y = −170 | 45 × 28 | Dây nguồn vào đường sau khay ngay khi ra khỏi PSU |
| G1 | Khay trên bo, X = −148; Y = 224 | 32 × 10 | EPS xuất hiện ngay trên đầu CPU 8 chân |
| G2 | Khay phía trước bo, X = 57; Y = 110 | 22 × 55 | ATX 24 chân và quạt trước trên; hai dây nằm cạnh nhau |
| G3 | Khay phía trên bo, X = −65; Y = 224 | 40 × 10 | Bơm và hai quạt két; dây vòng qua mép trên bo tới đầu cắm, không xuyên PCB hoặc cửa bảo trì CPU |
| G4 | Miệng ống che nối từ nắp che nguồn tới đầu GPU, tâm (95; 10; 25), mặt lỗ hướng −X | 18 × 24 theo Z × Y | Đầu nguồn GPU đi ra ngay cạnh ổ cắm, cách khoảng 25 |
| G5 | Khay ngay dưới góc trước bo, X = 23; Y = −104 | 48 × 16 | USB trước, F_PANEL, quạt trước giữa và dưới |
| G6 | Khay ngay dưới góc sau bo, X = −184; Y = −104 | 42 × 16 | HD_AUDIO và quạt sau |

**Chi tiết bắt buộc cho G4:** dây không được chạy lộ từ nắp che nguồn lên GPU. Sau khi đi sau khay, dây quay vào khoang nguồn qua G0, đi trong ống che gắn liền nắp che nguồn rồi ra G4. Ống che có tiết diện trong 24 × 28, đi sát phía trước đầu GPU và được vỏ trước che khi nhìn từ kính; miệng ống cách ổ nguồn GPU khoảng 25. Đây là một phần của vỏ số 1. Không để ống hoặc dây cắt ngang cánh quạt trước. Hình tổng thể ký hiệu G4 ở nhánh đi lên từ nắp che nguồn; tọa độ trên là điểm ra cuối cùng.

| Dây | Đường đi sau khay | Lỗ ra / đoạn nhìn thấy cuối dây | Màu nét đứt trong hình |
|---|---|---|---|
| ATX 24 chân | PSU → G0 → rãnh dọc phía trước khay | G2 → ATX, khoảng 25–35 | Cyan |
| EPS 8 chân | PSU → G0 → rãnh dọc phía sau → mép nóc | G1 → EPS, khoảng 20 | Vàng cam |
| GPU 8 chân | PSU → G0 → sau khay → trở lại khoang kín G0 → ống che trong vỏ | G4 → GPU, khoảng 25 | Tím |
| Bơm AIO | Đoạn từ bơm nằm trong vỏ bọc dọc ống nước tới mép két, vào đường sau khay ở vùng G3 | G3 → AIO_PUMP, khoảng 20–30; phần rời bơm chỉ 20 | Xanh lá |
| Hai quạt két | Dây men theo rãnh khung két → G3 → đầu chia Y cố định sau khay | G3 → CPU_FAN, khoảng 20–35 | Xanh lá |
| Quạt trước trên | Dây rời góc quạt về rãnh kín dọc mặt trước rồi vào sau khay qua G2 | G2 → SYS_FAN1, khoảng 25 | Xanh lá |
| Quạt trước giữa | Dây trong rãnh mặt trước → G2 → đi xuống sau khay | G5 → SYS_FAN2, khoảng 20–35 | Xanh lá |
| Quạt trước dưới | Dây trong rãnh mặt trước → G5 → ghim sau khay | G5 → SYS_FAN3, khoảng 20–30 | Xanh lá |
| Quạt sau | Dây từ góc giáp khay → rãnh mép nóc → G1 → rãnh dọc phía sau | G6 → SYS_FAN4, khoảng 25–35 | Xanh lá |
| USB 3.0 trước | Cụm cổng trên vỏ → rãnh kín mặt trước → G2 → xuống sau khay | G5 → USB 19 chân, khoảng 35–40 | Xanh dương |
| Nút và đèn trước | Cụm nút trên vỏ → rãnh kín mặt trước → G2 → xuống sau khay; chỉ tách bốn đầu nhỏ sát bo | G5 → F_PANEL, khoảng 20–30 | Đỏ san hô |
| HD_AUDIO | Cụm cổng trước → rãnh kín mặt trước → G2 → mép dưới sau khay; chạy riêng khỏi bó nguồn | G6 → HD_AUDIO, khoảng 20–30 | Xám |
| Điện ngoài | Ngoài lưng máy, nối vào PSU | Không qua G0–G6; không xuất hiện trong khoang kính | Không vẽ đường bên trong |

Dùng sáu đai dán sau khay: hai ở rãnh sau, hai ở rãnh trước, hai ở mép dưới. Phần dây thừa gập vòng trong khoang nguồn, không nhét sau socket CPU. Bó ATX/EPS/GPU dàn thành một lớp, không chồng lên nhau trong khoảng sau khay hẹp. Các rãnh mép khung rộng 25, sâu hữu dụng 10; bán kính uốn chọn ít nhất 20 cho dây nguồn và USB. Đầu chia quạt nằm ở túi lõm trên khay, không bị nắp kín ép vào bo.

Ống nước AIO vẫn thấy rõ vì người học cần biết bơm nối với két. Dây điện bơm phải nằm trong vỏ bọc sát ống và chỉ đi ra ở hai đầu; không vẽ một dây điện chéo qua mặt bo. Không có dây nào chạm cánh quạt, nằm giữa kính và GPU hoặc đi qua mặt RAM.

## 6. Thứ tự lắp trong trình mô phỏng

1. Để công tắc nguồn ở vị trí tắt, chưa cắm điện ngoài.
2. Tháo kính và nắp kín; mở lối sau khay.
3. Kiểm tra chín chân đế bo và các viền cao su G0–G6.
4. Đặt CPU theo dấu tam giác, hạ khung ép và khóa cần.
5. Lắp hai thanh RAM vào A2 và B2 đến khi chốt giữ đóng.
6. Cắm nghiêng SSD vào M.2, hạ xuống chân đỡ rồi bắt một vít M2.
7. Gắn tấm đỡ AIO sau bo, giữ bốn trụ gá đúng vị trí.
8. Đặt bo lên chín chân đế, bắt chín vít M3 vừa chặt.
9. Lắp ba quạt trước hút vào và một quạt sau thổi ra.
10. Lắp nguồn dưới nắp che, quạt hướng xuống; bắt bốn vít sau.
11. Luồn dây ATX, EPS và GPU vào đường sau khay qua G0.
12. Cắm EPS qua G1 và ATX qua G2 khi nóc còn dễ tiếp cận.
13. Bắt hai quạt vào két, kiểm tra chiều gió hướng lên nóc.
14. Đặt két dưới nóc, lệch về kính; bắt tám vít ngắn từ trên xuống.
15. Bóc màng bảo vệ đế bơm và đặt lớp keo tản nhiệt mỏng lên CPU.
16. Đặt bơm lên CPU, siết bốn vít lò xo luân phiên theo đường chéo.
17. Cắm bơm vào AIO_PUMP, hai quạt két qua dây Y vào CPU_FAN.
18. Cắm bốn quạt vỏ vào SYS_FAN1–4 theo các lối đã ghi.
19. Cắm USB, HD_AUDIO và bốn đầu nút/đèn; kiểm tra dấu +/− của đèn.
20. Tháo hai nắp PCIe tương ứng, giữ lại hai vít để bắt GPU.
21. Cắm GPU vào khe x16 đầu tiên, đóng chốt rồi bắt hai vít phía sau.
22. Cắm đầu PCIe 6+2 vào GPU qua G4; kiểm tra ngàm đã giữ.
23. Buộc dây sau khay, cất dây thừa dưới nắp che và kiểm tra quạt quay tự do.
24. Đóng nắp kín và kính, giữ đủ sáu vít tay của hai nắp.
25. Cắm điện ngoài, bật công tắc nguồn và nhấn nút nguồn trên vỏ.

Khi tháo để luyện tập, ngắt điện trước và làm ngược thứ tự; nhả chốt PCIe trước khi kéo GPU. Phần mô phỏng chỉ cho hoàn tất thao tác khi đúng đầu cắm, đúng hướng và đủ vít của bước đó.

## 7. Kế hoạch bản vẽ riêng, chưa thực hiện ở bước 0

Mỗi dòng là **một ảnh** sẽ vẽ ở bước sau, dùng lại số và ký hiệu của bản tổng thể.

| ID | Một ảnh dự kiến |
|---|---|
| `case` | Hình isometric tách hai nắp, kèm hai mặt khay nhỏ; chỉ rõ kích thước vỏ, G0–G6, ống che G4, chín chân đế và mọi điểm giữ quạt/nguồn/GPU/kính. |
| `motherboard` | Mặt bo nhìn thẳng, thêm góc nhìn nghiêng nhỏ; ghi tọa độ socket, A2/B2, PCIe, M.2, các đầu dây, chín lỗ vít và sơ đồ F_PANEL. |
| `cpu` | CPU nổi ngay trên socket đang mở, kèm mặt dưới nhỏ; chỉ tam giác, rãnh, tiếp điểm và hướng đóng cần, không thêm vít CPU. |
| `cooler` | Một cụm bơm–hai ống–két tách khỏi hai quạt và bộ gá; chỉ 4 vít bơm, 8 vít quạt dài, 8 vít nóc ngắn, chiều gió và hai đầu AIO_PUMP/CPU_FAN. |
| `ram` | Hai thanh RAM ngay trên A2/B2, một mặt cắt nhỏ để thấy rãnh khóa và chốt; ghi 133,35 × 42 × 6. |
| `gpu` | GPU ba quạt nhìn từ dưới, hơi nghiêng để thấy chân PCIe, đầu nguồn 8 chân, bốn cổng hình ảnh và hai vít lá bắt; ghi rõ thân 55 mm, lá bắt hai khe. |
| `ssd` | SSD 2280 đang cắm nghiêng vào khe M-key, kèm mũi tên hạ đầu giữ xuống chân đỡ và một vít M2. |
| `psu` | Nguồn nhìn từ sau và đầu ra dây trong cùng ảnh; chỉ bốn vít, quạt hút đáy, C14, công tắc và ba dây liền đi vào G0. |
| `fans` | Một quạt 120 mẫu tách bốn vít, cạnh đó sơ đồ bốn vị trí trên vỏ; phân biệt hút trước/thổi sau và đầu 4 chân. |
| `cables` | Mặt sau khay với các đường nét đứt có màu và nhãn; viền quanh là hình hai đầu từng dây, số chân, mã G và các đoạn cuối 20–40 mm. |

## 8. Quy cách tờ tổng thể và kiểm tra bàn giao

Tờ PNG nền trắng, đường mảnh xanh đậm, chữ ngắn. Giữa tờ là cả bộ máy tách nhẹ theo hướng lắp; bên phải là mặt kính khi đã lắp và mặt sau khay có đường dây nét đứt. Dùng số 1–10 cho linh kiện; G0–G6 cho lối dây; biểu tượng vít cạnh chỗ bắt. Chú giải phải phân biệt dây nguồn, dây bơm/quạt và dây mặt trước bằng cả màu lẫn chữ.

Trước khi dựng Blender, kiểm tra đủ: 2 RAM, 1 SSD, 1 GPU ba quạt, 2 quạt két, 3 quạt trước, 1 quạt sau, 1 PSU dưới nắp che; két ở **dưới** nóc; dây chỉ lộ sát đầu cắm. Số đo, tọa độ và số vít trong tài liệu này là mốc định lượng; ảnh tạo sinh trình bày bố cục, không dùng đo tỉ lệ hoặc suy ra số ren từ nét vẽ.

Ảnh dùng công cụ imagegen tích hợp. Lệnh tạo ảnh và lệnh sửa hướng linh kiện được lưu bên dưới để có thể tạo lại đúng bố cục. Đây là phần kỹ thuật cho công cụ, giữ tiếng Anh theo lệnh đã gửi.

```text
Use case: infographic-diagram.
Create ONE high-resolution landscape technical blueprint sheet for a generic beginner gaming PC. White background, fine navy and blue linework, restrained cyan accents, generous whitespace, crisp readable short English text only. Absolutely no brand logos or real product names, no watermark. Title "GAMING PC / 00 BLUEPRINT", subtitle "Dimensions in mm". This is an assembly reference, not advertising.

COMPOSITION: a large exploded isometric assembly in the middle occupying 65% of sheet. On the right, two smaller framed orthographic views stacked: "GLASS SIDE" assembled elevation, and "BEHIND TRAY" showing hidden cable routes. Bottom strip: compact part legend and screw schedule. The whole PC must be present, no cropping. Fine straight leaders that do not cross. Numbered part callouts 1 through 10, exactly consistent with legend.

CENTRAL ASSEMBLY: dark charcoal mid-tower frame 220 W x 470 H x 450 D, shown as light technical linework with dark edges and cyan accent; transparent glass panel offset toward viewer, rear metal panel offset away. Rear is left and front is right. Generic ATX motherboard 305 high x 244 wide, vertical against far tray. CPU socket near upper middle; four RAM sockets to its right with exactly TWO heatspreader DIMMs in alternate sockets A2/B2. Small square CPU slightly exploded directly out of socket; round AIO pump directly above CPU, four corner mounting screws and backplate behind board. Radiator 277 x 120 x 27 with exactly TWO 120 mm fans below it, suspended immediately UNDER the case roof, displaced slightly downward along mounting direction, not outside/above the roof; two curved coolant tubes between radiator and pump. Radiator shifted toward glass for board clearance.
One graphics card, 280 long x 125 wide x 55 thick, horizontal, THREE fans on underside, two-slot rear metal bracket with TWO retention screws; slight outward displacement along PCIe insertion direction, clearly aligned to first x16 slot. Small M.2 SSD 22 x 80 on lower motherboard and ONE tiny retaining screw. PSU 150 x 86 x 140 at rear bottom, slightly slid out rearward, FOUR screws at rear, permanent shroud shown ghosted above it. Exactly THREE 120 mm front intake fans in vertical column, each with FOUR corner screws; exactly ONE 120 mm rear exhaust fan with FOUR screws. Front mesh panel slightly displaced forward. Airflow arrows front inward, rear and roof outward. Exactly SIX large case/radiator fans in total, plus the three smaller GPU fans.

FASTENERS: use small screw icons, leaders and short labels to make mounting sites readable: board 9 M3 + 9 standoffs, pump 4 M3, radiator-to-roof 8 M3x6, fans-to-radiator 8 M3x30, front case fans 12 fan screws, rear fan 4 fan screws, GPU bracket 2 UNC, PSU 4 UNC, SSD 1 M2, glass 4 thumb, rear panel 2 thumb. Show nine board mounting points. No loose irrelevant screws. Legend "UNC = 6-32" and "Fan screw = 5 x 10". Draw socket outlines and distinct latches at CPU, RAM, PCIe, M.2. Header labels near their locations: "EPS 8" upper left board, "ATX 24" right edge, "AIO_PUMP" and "CPU_FAN" near CPU top, "SYS_FAN 1-4", "USB 19" right lower edge, "F_PANEL 9" bottom right, "HD_AUDIO 9" bottom left, "GPU 8" GPU outer edge.

CABLE MANAGEMENT INSETS: all electrical cables black in central view and glass-side view, mostly fully hidden behind motherboard tray, only last 20-40 mm visible beside connectors. Coolant tubes are visible mechanical hoses, never electrical cable routes. Show rubber-edged grommet slots labelled G0 through G6. G0 is PSU chamber-to-rear channel opening. G1 top rear near EPS. G2 upper right beside ATX24. G3 small tray slot immediately beside AIO_PUMP/CPU_FAN. G4 slot in PSU shroud directly below GPU power socket. G5 below board front corner for USB and F_PANEL. G6 below board rear corner for HD_AUDIO and rear fan. In BEHIND TRAY inset show all seven holes and colored dashed cable paths from PSU at G0: amber to G1 EPS, cyan to G2 ATX24, purple to G4 GPU. Green dashed fan/pump paths via G3/G2/G5/G6. Blue USB and coral F_PANEL via G5, grey audio via G6. Keep cables confined to rear channel, clipped along tray perimeter. Glass-side inset shows only short connector tails, no diagonal wires across board. Small cable legend includes "Power", "Fan / pump", "Front I/O"; pair colors with connector labels, not color alone.

PART LEGEND exact mapping: "1 Case", "2 Board", "3 CPU", "4 AIO", "5 RAM x2", "6 GPU", "7 M.2 SSD", "8 PSU", "9 Case fans x4", "10 Cables".
Include overall dimension arrows 470 H, 450 D, 220 W. All connectors and fasteners should read as purposeful engineered assembly details. Clean visual hierarchy, no dense prose, no decorative electronic circuitry filling whitespace.
```

Lệnh sửa trên cùng tờ ảnh:

```text
Edit this existing blueprint sheet, preserving the clean white/blue style, landscape layout, part numbering, text legibility, dimensions, legend, screw schedule and all component counts. This remains ONE final blueprint image.

Correct the mechanical assembly orientation, with these critical changes:
1. GPU: Both the central exploded GPU and the assembled GLASS SIDE inset must show a standard HORIZONTAL graphics card installed perpendicular to the vertical motherboard. Its long 280 mm axis runs rear-left to front-right. Its broad face with THREE fans faces DOWN toward the PSU shroud, not upright toward the glass. In the glass-side elevation show the narrow long 55 mm thickness edge with horizontal heatsink fins; do NOT show a row of three fan circles facing the viewer there. In the central isometric, slightly elevate the viewpoint as needed so the GPU top backplate and its narrow edge are visible, with three fans only on underside; a small underside detail within the existing GPU legend tile can show the three circles. Its rear two-slot bracket is vertical at the rear LEFT, TWO screws, and the gold PCIe connector lies along the board-facing edge correctly aligned with the motherboard first x16 socket. Move it only outward normal to the motherboard along its insertion direction. The GPU power socket is on the front end at right, with a short 25 mm cable from the covered shroud riser G4.
2. PSU: In the center view, orient the 150 x 86 x 140 PSU with large intake fan DOWN toward case bottom. Rear end toward left rear of case has IEC inlet and rocker switch plus perforations and four screws. No large circular fan on the rear or cable-side face. In the BEHIND TRAY inset show solid rectangular PSU side with wires leaving its front end, not a large exposed fan.
3. Radiator: Central exploded assembly must keep radiator and two fans immediately BELOW roof in their install position, with only 10 mm illustrative separation. Roof stays attached to case. Remove the huge gap to roof; radiator shifted toward glass. GLASS SIDE inset continues showing radiator and two fans inside beneath roof.
4. In BEHIND TRAY inset label the purple cable's final hidden shroud riser outlet G4 clearly; keep all G0 G1 G2 G3 G4 G5 G6 labels present. G1 and G3 are holes ABOVE the top edge of the motherboard, so cables never pass through solid motherboard PCB. G4 rises from shroud behind GPU front end with only final short tail visible.
5. Clearly show all NINE motherboard screw icons at the nine mounting holes, with a short leader "9 x M3". Keep two RAM sticks only and four DIMM slots. There must be THREE front fans, ONE rear fan, TWO radiator fans, THREE GPU underside fans. Electrical cables stay behind tray; coolant hoses remain visible. Retain callout number 10 on an actual hidden cable route in the BEHIND TRAY view.
Do not add branding or long prose. Do not change the overall visual identity or crop the sheet.
```
