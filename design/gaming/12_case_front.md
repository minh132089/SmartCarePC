# 12 / Mặt trước kính, ba vòng ARGB cyan

Ảnh: [12_case_front.png](12_case_front.png). Tham chiếu kiểu dáng: [ảnh người dùng](ref/case_front_style.png). Thiết kế giữ dáng tháp đen phẳng, kính trước và kính hông, ba vòng sáng dịu; không dùng logo trên quạt. Đây là đặc tả mô hình, chưa phải bản vẽ chế tạo hoặc kết quả kiểm thử nhiệt.

## Phạm vi và hệ tọa độ

Tất cả kích thước cơ khí, tọa độ, chiều dài dây dưới đây tính bằng **mm**. Gốc ở tâm thân máy; **X từ lưng ra trước**, **Y từ đáy lên nóc**, **Z từ nắp kín sang kính hông**. Thân giữ **220 rộng × 470 cao × 450 sâu**: X = −225…225, Y = −235…235, Z = −110…110. Quy đổi 100 mm = 1 đơn vị cảnh.

Giữ nguyên bo, CPU, AIO, RAM, GPU, SSD, nguồn, quạt sau, kính hông, nắp che nguồn, ống G4, lỗ G0–G6 và đường dây đang có. Ba quạt trước vẫn 120 × 120 × 25, chiếm X = 197…222, Z = −60…60; tâm Y lần lượt **−85, 40, 165**. Giá quạt vẫn X = **221…222**, Y = −150…230, Z = −64…64; không dời giá hoặc tâm lỗ. Chỉ bổ sung vòng LED vào mặt hút của ba quạt, dây ARGB và một header theo yêu cầu brief.

Tôn khung dùng **3** theo hàm `case()` trong `tools/build_gaming.py`, thay vì số 1 của tài liệu cũ. Giá quạt riêng vẫn dày 1. Các yêu cầu “không RGB/ARGB” trong phần 02, 09, 10 được thay thế **riêng cho ba quạt trước và header mới** bằng đặc tả này; không sửa các tệp cũ.

**Giới hạn đóng gói:** từ mặt giá quạt X = 222 đến biên vỏ X = 225 chỉ có 3. Không thể đặt kính 4 cùng một khoang hút sâu trong khoảng này. Phương án giữ nguyên tọa độ dùng kính danh nghĩa 2 và khe 0,9; kính mỏng cần nhà cung cấp xác nhận khả năng cường lực, độ bền và dung sai. Đây là phương án hình học để dựng tham chiếu, chưa đủ cơ sở duyệt chế tạo. Không tự tăng chiều sâu vỏ hoặc đẩy quạt vào trong để che mâu thuẫn.

## Cấu tạo mặt trước

Các khoảng tọa độ dưới đây là trạng thái đã lắp, không phải khoảng tách lớp trong ảnh.

| Số trên ảnh / chi tiết | Kích thước và vị trí | Vật liệu, bề mặt |
|---|---|---|
| 01 / Kính trước | **158 theo Z × 456 theo Y × 2 theo X**; X = **222,9…224,9**, Y = −228…228, Z = −79…79. Mặt ngoài lùi **0,1** so với mép khung X = 225. Vát cạnh **0,2 × 45°**, góc bo R2; không khoan xuyên kính | Kính cường lực theo yêu cầu thiết kế, trong hơi khói `#DDECF2`; mục tiêu truyền sáng khoảng 80%, roughness dựng 0,08, IOR 1,5. Viền men đen `#101216` rộng 5 che ngàm và đệm; không dán logo |
| 02 / Khung cố định | Hai trụ ngoài X = 222…225, Z = −110…−101 và 101…110, Y = −232…232. Hai thanh ngang X = 222…225, Y = −235…−228 và 228…235, Z = −110…110 | Thép sơn nhám `#16181D`, roughness 0,65; cạnh ngoài bo 0,5, mặt phẳng không gân trang trí |
| 02 / Giá mang kính tháo được | Hai thanh đỡ mép tại Z = −81…−77 và 77…81, X = 219,9…222,9, Y = −228…228. Thanh ngang ở Y = −228…−225 và 225…228, chỉ nối ngoài vùng quạt; rãnh mép sâu 2, rộng 2,4 | Nhôm sơn cùng màu khung; gioăng EPDM `#202329`, nén còn 0,4 tại bề mặt kẹp. Rãnh giữ kính theo mép, không cho kim loại tì trực tiếp vào kính |
| 03 / Hai dải hút đứng | Mỗi dải **19 theo Z × 432 theo Y × 0,7 theo X**; X = 223,5…224,2, Y = −216…216; Z = −101…−82 và 82…101. Mặt lỗ hướng +X, chạy sát hai cạnh trước | Thép đục lỗ `#101216`; mép cuốn an toàn nằm về phía sau. Đây là hai dải trên mặt trước, không khoét nắp hông |
| 04 / Hai lọc bụi | Mỗi cụm **19 × 432 × 0,8**, X = 221…221,8; cùng vùng Y/Z với dải hút. Lưới hữu dụng 15 × 424, khung viền rộng 2 ở cạnh dài, 4 ở đầu | PET đen `#202329`, ô mở vuông 0,35, sợi 0,15; khung nylon. Lọc nằm sau dải đục lỗ, không trải ngang sau kính |
| 05 / Ba vòng khuếch tán | Mỗi vòng **Ø114 ngoài, Ø110 trong**, X = 220,5…222; tâm Y = −85/40/165, Z = 0. Vòng tích hợp vào miệng quạt, mặt sáng cao nhất X = 222 | PC trắng sữa `#E8F4F4`, LED cyan **`#22D3EE`**. Không tăng bề dày quạt; mặt cánh gần nhất giữ X ≤ 219, chừa tối thiểu 1,5 theo X tới vòng |
| Giá quạt hiện hữu | X = 221…222; ba cửa Ø114 và 12 lỗ vít giữ nguyên | Thép đen; vòng sáng nằm trong cửa Ø114, không nằm sau phần tôn che kín |

Khung kính không chạm quạt: thanh đứng gần tâm nhất ở |Z| = 77, còn giá quạt dừng ở |Z| = 64. Riêng thanh ngang trên tại Y = 225…228 phải gập thành môi mỏng **X = 222,1…222,9**, Z = −77…77, để đi phía trước phần tôn đỉnh giá quạt X = 221…222; không nối bằng thanh dày xuyên qua giá. Không kéo thanh xuống che vòng sáng. Kính hông **430 × 445 × 4** và bộ bốn vít/đệm hiện hữu không đổi.

Họa tiết mỗi dải hút: **4 cột × 107 hàng lỗ tròn Ø2,6, bước 4**. Với dải bên +Z, tâm cột Z = 85,5; 89,5; 93,5; 97,5; dải bên −Z lấy dấu âm tương ứng. Tâm hàng Y = −212 + 4j, j = 0…106. Tổng **428 lỗ mỗi dải**, diện tích mở hình học khoảng **2.272 mm²/dải**, chưa trừ cản của lọc. Không thay các lỗ bằng chấm sơn.

Không khí vào qua hai dải cạnh → lưới lọc → vùng bên ngoài giá quạt, |Z| > 64 → rẽ vào khe giữa mặt hút X = 222 và kính X = 222,9 → ba quạt đẩy theo **−X** vào máy. Gioăng liên tục ở cạnh trên/dưới ngăn bụi đi tắt quanh lọc. Không khí không xuyên qua kính. **Khe 0,9 là nút thắt rõ rệt**; diện tích đục lỗ lớn không khắc phục được khe này. Chưa có số liệu lưu lượng, áp suất hoặc nhiệt độ để khẳng định khả năng làm mát khi tải gaming. Nếu chuyển sang sản phẩm thật, phải thử nhiệt và xác nhận vật liệu; nếu không đạt, cần một brief mới cho phép đổi chiều sâu mặt trước hoặc vị trí quạt.

Dải nhấn cyan hiện có giữ bề rộng 3, Y = −195…195, Z = 103…106; đặt bề mặt trong X = 224,8…225 để không vượt biên thân. Không tạo thêm dải ARGB cần dây. Giữ nguyên vật liệu nhấn hiện có, giảm cảm giác chói trong ảnh bằng ánh sáng studio.

## Ngàm, vít và thao tác tháo

| Chi tiết giữ | Số lượng / loại | Tâm và cách lắp |
|---|---|---|
| Kẹp mép kính vào giá mang | **4 kẹp chữ U có đệm EPDM**, mỗi kẹp dài 12 theo Y, ôm 3 theo Z | Tâm Y = −210 và 210, Z = −78 và 78, mặt ép tại X = 222,9/224,9. Bốn tổ hợp; kẹp ẩn sau viền men, không xuyên kính |
| Vít khóa kẹp | **4 × M2 × 4**, vít thép đen `#353A42` | Trên tai phía sau giá mang, tâm X = 221, Y = ±210, Z = ±80; trục Z hướng vào mép. Đầu vít nằm trong |Z| ≤ 81; không lấn dải hút bắt đầu ở |Z| = 82 |
| Chốt định vị cassette kính | **2 chốt Ø2 × 3** và hai ổ nhận có cao su | Tâm (X;Y;Z) = (221;−228;−77) và (221;−228;77); trục Y, đầu chốt hướng −Y. Chốt định vị và đỡ trọng lượng, không thay lẫy khóa |
| Lẫy khóa cassette | **2 lẫy lò xo inox**, mỗi lẫy 8 × 5 × 0,4 | Vùng quanh (221;220;−79) và (221;220;79); nhả theo Z từ khe dịch vụ bên khung. Tách khỏi các kẹp Y = 210; không đặt vít tay nổi trước kính |
| Dải hút | **4 vít M2 × 4**, mỗi dải hai vít | Tâm X mặt đầu = 224,5; Y = ±222; Z = ±91,5. Vít trên tai kéo dài ngoài vùng đục lỗ, đầu chìm không vượt X = 225 |
| Lọc bụi | **8 nam châm 4 × 3 × 0,8**, bốn mỗi lọc; không có vít | Tâm X = 221,4, Y = ±214, Z = 84/99 và −84/−99; nằm trong viền đặc của lọc. Hai tai kéo vải, tâm Y = −212, Z = ±91,5 |
| Quạt trước | **12 vít tự tạo ren Ø5 × 10**, giữ đúng vị trí và hướng −X | Với mỗi Yc = −85/40/165, tâm lỗ Y = Yc ±52,5, Z = ±52,5. Giữ lỗ và chiều ăn ren; riêng đầu vít mặt trước chọn đầu thấp và bậc tựa tại tai giá sao cho điểm cao nhất **X ≤ 222,5**, còn ít nhất 0,4 tới kính |

Đầu vít quạt hiện tại dựng bằng `phillips()` có thể vượt vùng kính mới. Đây là **thay đổi bắt buộc của chi tiết giữ mặt trước**, không được để xuyên kính hoặc lùi cả quạt. Bậc tựa/đầu thấp cần xác nhận độ bền khi chế tạo; không khoét rộng lỗ tâm hoặc khoét vào cánh. Tổng mới: 4 vít khóa kẹp + 4 vít dải hút; 12 vít quạt là bộ thay hình dạng đầu, không cộng thêm 12 vít. Không thay vít kính hông.

Tháo kính trước: tắt máy, rút điện; đỡ cassette bằng hai tay, nhả hai lẫy, nghiêng mép trên ra +X rồi nâng nhẹ theo +Y để nhấc hai chốt đáy. Lọc kéo ra +X sau khi tiếp cận mép trong; có thể tháo dải hút bằng hai vít nếu cần. Kính không mang dây điện, nên không phải giật đầu ARGB khi tháo kính. Không bẻ kẹp trực tiếp vào cạnh kính.

## ARGB và vị trí header mới

Chọn **cyan đứng yên `#22D3EE`**, độ sáng mặc định khoảng 30%; không chạy cầu vồng hoặc hiệu ứng nhấp nháy. Mỗi quạt có 12 LED địa chỉ hóa sau vòng PC, chụp tâm và cánh giữ đen `#252932`, không logo. Vòng không quay cùng rotor. Quạt sau và hai quạt két không được bổ sung ARGB.

Mỗi quạt vẫn có dây PWM 4 chân riêng. Bổ sung một nhánh ARGB 3 dây gồm +5 V, dữ liệu và GND, vỏ đen `#15171B`, đầu nhựa `#202329`, tiếp điểm `#BFC4CA`. Đầu thuộc dạng **3 tiếp điểm trên 4 vị trí, khuyết vị trí 3**, bước 2,54. Nhìn theo thứ tự chân được đánh dấu: **1 = +5 V; 2 = D; 3 = khóa/không chân; 4 = GND**. Đầu cái có vị trí 3 bịt kín; không dựng bốn hốc có kim loại rồi chỉ ghi nhãn “3-pin”.

Bổ sung header mô phỏng **`JRAINBOW1`** vào bo, không gắn logo hãng và không coi đây là model bo thương mại:

- Tâm cục bộ bo **(55; −143; 3)**, tương ứng toàn máy **(−33; −80,5; −93)**. Đây là mốc chân đế nhựa; chân cắm nhô về +Z, thao tác cắm từ phía kính về −Z.
- Thân chọn **11,5 theo X × 5 theo Y × 7 theo Z**; phạm vi toàn máy X = −38,75…−27,25, Y = −83…−78, Z = −93…−86. Bốn vị trí dọc X có tâm **−36,81; −34,27; −31,73; −29,19**, cùng Y = −80,5; vị trí X = −31,73 không có chân. Nhãn và tam giác chân 1 nằm cạnh thân, không dùng vít giữ.
- Vùng dành cho đầu cắm và thao tác, chiếu lên bo: cục bộ X = 47…63, Y = −149…−137; toàn máy X = −41…−25, Y = −86,5…−74,5. Khoảng Z cần trống tới −73 khi cắm.
- Đối chiếu phần 02: F_PANEL tại (95;−143), thân bắt đầu X cục bộ 88,5, cách thân mới 27,75; SYS_FAN2 tại (82;−128), SYS_FAN3 tại (95;−134), USB tại (113;−127) đều ngoài vùng này. PCIe dưới và chốt kết thúc gần X cục bộ 18,5; H8 tại (0;−142,5), H9 tại (112;−142,5) cũng ngoài vùng giữ trống. Tản chipset kết thúc ở Y cục bộ −112, cách mép trên thân mới 28,5. Không dời header, khe, lỗ gá hoặc chipset nào.

Thiết kế điện chọn **5 V, ngân sách header 1 A**, yêu cầu mỗi cụm vòng LED có giới hạn dòng tối đa **0,25 A kể cả khi bật trắng**; tổng ba vòng ≤0,75 A, còn 0,25 A dự phòng. Đây là yêu cầu chọn linh kiện/thiết kế bo mới, không phải thông số đã được xác nhận trên bo hiện có. Không suy dòng tối đa chỉ từ độ sáng cyan mặc định.

Chọn **daisy-chain vật lý**, không hub và không nguồn SATA: header → ARGB quạt trên → quạt giữa → quạt dưới. Đường +5 V/D/GND nối chuyền song song, cả ba vòng nhận cùng 12 địa chỉ và sáng đồng bộ; không mô tả thành 36 LED nối dữ liệu nối tiếp. Mỗi quạt có một đầu IN cái và một OUT đực; OUT cuối được bịt bằng nắp cách điện. Ba dây PWM không ghép vào chuỗi đèn.

## Bộ dây thêm và đường qua grommet

Dây ARGB dùng ba sợi 24 AWG, bó dẹt dày mục tiêu ≤2. Mỗi quạt có đuôi IN **450**, nhánh OUT **80**; thêm **hai đoạn nối dài 200** giữa các cụm và **một đoạn nối 650** từ header tới IN của quạt trên. Đo chiều dài theo đường tâm dây, không theo đường thẳng giữa hai điểm. Đầu nối sau khay chọn thân dày theo Z ≤5, đặt trong khoảng Z = −109,5…−104,5; không chồng lên nhau.

| Nhánh | Lộ trình trong tọa độ máy / grommet | Điểm nối |
|---|---|---|
| ARGB quạt trên | Ra góc phía khay của khung, đi dọc mép trước ở X khoảng 190…196, Z khoảng −85…−75; tới **G2 (57;110)** rồi vào sau khay | Cụm nối trên gần (150;150;−107), dùng đai trước hiện có |
| ARGB quạt giữa | Cùng rãnh mép, qua **G2**, hạ sau khay; không căng dây băng qua mặt quạt | Cụm nối giữa gần (150;0;−107), OUT trên nối IN giữa bằng đoạn 200 |
| ARGB quạt dưới | Sát mép dưới/quanh phía khay, qua **G5 (23;−104)** | Cụm nối dưới gần (60;−110;−107), OUT giữa nối IN dưới bằng đoạn 200; bịt OUT dưới |
| Dây tới bo | Từ cụm nối trên → rãnh trước sau khay → mép dưới → ra **G5** → ôm mép dưới bo tới **JRAINBOW1 (−33;−80,5;−93)** | Đoạn sau G5 dự trù **75–90** để có độ chùng và uốn, không kéo đường chéo qua GPU |

Đầu dây quay theo rãnh, bán kính uốn **≥20**; giữ cách vùng cánh quạt **≥10** sau khi buộc. Rãnh hiện có rộng 25, sâu hữu dụng 10; dây thêm xếp một lớp cạnh bó PWM, dùng sáu đai hiện có. Phần thừa gấp vòng lớn ở khoang nguồn qua **G0 (−35;−170)** nếu cần, không nhét sau cửa CPU. Không đi ARGB qua G1, G3, G4 hoặc G6; G4 vẫn dành cho nguồn GPU. Nếu bó thực tế không vừa, phải chọn cáp/đầu nhỏ hơn trong ngân sách này, không khoan thêm grommet.

PWM giữ nguyên: quạt trên → G2 → SYS_FAN1; giữa → G2 → sau khay → G5 → SYS_FAN2; dưới → G5 → SYS_FAN3. Header mới chỉ cấp đèn 5 V; các SYS_FAN vẫn cấp động cơ 12 V và tín hiệu PWM. Không cắm đầu ARGB vào đầu RGB 4 chân 12 V.

## I/O, chân đế và vùng nguồn

**Không dời I/O trên nóc.** Theo `case()`, cả cụm ở X = 190: nút nguồn tâm Z = −60, Ø14; hai USB-A tâm Z = −30 và −12, mỗi miệng khoảng 6 × 13 theo X × Z; jack combo tâm Z = 6, lỗ Ø3,5. Mặt nóc Y = 235, đầu nút cao nhất Y = 236,6 như hiện có. Không thêm nút RGB. USB/nút vẫn qua G2 rồi G5; HD_AUDIO qua G2 rồi G6 theo phần 10. Cụm cách mặt kính trước theo X hơn 25 nên không vướng cassette.

Bốn chân cao **12**, kích thước 50 theo X × 30 theo Z, tâm (X;Y;Z) = (±190;−241;±85), giữ nguyên. Vì chân hiện có kéo xuống Y = −247, số **470** là chiều cao thân, không phải tổng từ bàn tới nút nguồn; chiều cao toàn bộ hiện hữu tới nút là 483,6. Không thu nhỏ hoặc cộng thêm chân để đạt số 470 trên ảnh.

Nắp che nguồn giữ mặt trên **Y = −120**, đầu trước **X = 170**. Vách phía kính Z = 104…106 giữ **8 khe đứng**, mỗi khe 6 theo X × 60 theo Y, X = 60+12k…66+12k, k = 0…7; Y = −200…−140. Không biến phần này thành quạt thứ tư và không kéo nắp che ra X = 222 che quạt dưới.

## Người mới cần nhận ra

1. Kính trước nhìn xuyên được nhưng không hút gió; hai dải đục lỗ ở cạnh mới là cửa vào, và lọc nằm sau chúng. Khe hút của phương án giữ kích thước này rất hẹp, nên hình đẹp không chứng minh máy mát.
2. Mỗi quạt trước có **hai hệ dây**: PWM 4 chân cho động cơ và ARGB 3 chân 5 V cho đèn. Đèn tắt không có nghĩa quạt ngừng quay.
3. Dấu +5 V phải trùng chân 1; vị trí khuyết ngăn cắm lệch. Tắt nguồn trước khi nối, không ép đầu vào header 12 V.
4. Tháo cassette bằng lẫy và chốt đáy; kính không có vít xuyên mặt và không có dây bám theo. Đỡ kính, không giật mép hoặc tì kim loại lên cạnh.
5. Giữ ba quạt hút trước, quạt sau thổi ra và két nóc như cũ. Làm sạch lọc rồi lắp khô; không để dây ARGB rơi vào cánh hoặc chiếm ống G4 của GPU.

## Tạo ảnh và kiểm tra

Dùng **imagegen tích hợp**, ảnh người dùng làm tham chiếu kiểu dáng và `review/assembly_front.png` làm tham chiếu bố trí nội thất. Ảnh minh họa vật liệu, ánh sáng và lớp lắp ráp; tọa độ trong tài liệu quyết định hình học. Khe 0,9 trong hình cắt được phóng đại để đọc được, không đo ngược từ ảnh.


Đã kiểm tra ảnh cuối: có phối cảnh toàn máy, ba vòng cyan, kính trước/hông, hai dải hút, hai lọc, giá quạt tách lớp, callout 01–08 và đầu ARGB có vị trí khóa. Không còn logo trên hub hoặc nắp bơm. Các khe nhỏ, số lỗ đục và ngàm trong ảnh được giản lược; dựng theo bảng, không theo số điểm ảnh. Phối cảnh nội thất giữ bố cục tham chiếu nhưng không thay thế hình học của mô hình hiện có. Chưa kiểm chứng dung sai lắp, gia công kính mỏng hay hiệu năng nhiệt.

<details>
<summary>Prompt tạo ảnh và các lượt sửa thực tế, tiếng Anh</summary>

### Tạo ảnh

```text
Use case: product-mockup / infographic-diagram.
Create ONE polished landscape product reference sheet, high-resolution 1536x1024 or larger, titled "12 / GLASS FRONT", subtitle "220 W x 470 H x 450 D mm". White-to-pale-grey studio background, realistic crisp charcoal powder-coated steel, restrained cyan annotation lines, generous whitespace, short English labels. Brand-free throughout: NO logos, fan hub marks, trademarks, fake labels or watermark.

INPUTS: The first reference image is a STYLE REFERENCE only: clean glass-front tower, glass side, slim black frame, three visible illuminated front fans, small feet. Do NOT copy its logos or rainbow lighting. The second reference is our EXISTING INTERIOR geometry guide only: ignore its website UI and text, preserve the top-mounted AIO radiator with two fans, circular CPU pump, two RAM modules, horizontal GPU with underside fans, covered GPU cable riser and perforated PSU shroud. Never reproduce UI.

COMPOSITION: A large assembled three-quarter hero of the whole tower fills the LEFT 62%, rear at left, FRONT at right, viewing through installed transparent glass side and front. Front must be broad enough to see all three complete cyan rings. Entire tower and small existing feet in frame. On RIGHT upper third put a small, clear exploded front assembly titled "FRONT LAYERS", with separated tempered glass pane, open perimeter frame, two slender perforated vertical intake strips, two matching fine-mesh filter strips, and original three-aperture fan bracket. Space parts along depth and label each with short leaders; these strips are LATERAL parts flanking the glass, not full-width sheets blocking the glass. Include three fan silhouettes behind the bracket. Lower right has a tiny section titled "PACKAGING LIMIT" and concise text "2 mm glass / 0.9 mm inlet gap" and "Airflow validation required". Do not invent a deep plenum. Under that put a small keyed ARGB connector with the four positions "5V  D  -  GND", third position blank, caption "3-pin 5V / G5".

HERO GEOMETRY: black mid-tower body 220 wide, 470 high, 450 deep; installed glass side, small existing black feet. Narrow matte charcoal flat frame, full clean front transparent lightly smoked tempered-glass pane, no central mesh overlay. Front pane nominal 158 wide x 456 high x 2 thick, inset only 0.1 mm behind the front rim. Glass has subtly polished 0.2 mm bevels and hidden edge clips, no big screws through its face. Two vertical front-facing perforated intake strips each 19 wide x 432 high run symmetrically along outer front edges, with fine dark dust mesh behind them, and thin black rails around. Both strips may be partly hidden by perspective; show the pair clearly in exploded view. Palette frame #16181D, vents #101216, rubber #202329, glass faint #DDECF2, all three rings constant soft cyan #22D3EE. Rings glow gently with visible dark blades and plain unmarked hubs. NO rainbow, magenta, exaggerated bloom, LED blade fill, or glowing rear fan. Exactly three 120mm front fans, equally spaced 125mm centre to centre, positioned toward upper portion leaving larger lower fascia: global Y=-85,40,165 with body Y=-235..235; Z=0. The upper ring stops 10mm below roof. Fans remain X=197..222, original bracket X=221..222, glass X=222.9..224.9. All interior parts stay as second reference, don't reposition or swap them; side glass can have understated reflections, but interior topology should remain visible. Shroud top at Y=-120, stops before front fans at X=170; retain eight narrow vertical perforations near its forward side, and covered GPU cable duct. No extra front fan or side fans. Keep slim existing cyan accent on glass-side front frame as a quiet line.

TOP I/O stays on top near front: ONE round power button, EXACTLY TWO rectangular USB-A ports and ONE small audio jack. No buttons on front glass. Four existing short feet, two/three visible in perspective. Dimension arrows on hero show 470 body height excluding feet, 450 depth, 220 width.

NUMBERED CALLOUTS, each used once around hero with clean leaders:
"01  Tempered glass"
"02  Flat black frame"
"03  Twin edge intakes"
"04  Removable dust filters"
"05  Three cyan ARGB rings"
"06  Hidden clips"
"07  Top I/O unchanged"
"08  Perforated PSU shroud"
Callout 04 may point into exploded view to the pair of filters; 06 to frame edge. Do not cross leader lines over fan centres. Tiny footer "Concept reference / dimensions governed by specification". This is one complete reference sheet, not multiple separate images. Prioritize clean reference-like case styling, three full front rings, accurate existing interior, useful exploded layers, and legibility.
```

### Sửa chi tiết cơ khí và điện

```text
Edit this ONE reference sheet and preserve its polished white studio style, layout, overall case dimensions, interior component arrangement, labels and cyan palette. Make these precise corrections:
1. BRAND-FREE: remove EVERY X-like symbol and every logo from all three front fan hubs, all exploded fan hubs, CPU pump cap, and any other component. All hubs and pump cap must be entirely plain, unmarked dark circles.
2. FRONT FAN HEIGHTS: Current fans extend too low. Keep three fans 120x120 with 125mm centre pitch. Body height is 470mm, top fan centre is 70mm below roof, middle is195mm below roof, lower is320mm below roof. Therefore top fan square top is10mm below roof, lower fan square bottom is90mm above body bottom. Correct the hero's three fans to these proportions; show the larger blank dark-glass lower fascia beneath them. Do not move the existing PSU shroud or other interior components. Exactly three soft cyan rings; do not enlarge them to fill the full height.
3. CALLOUT 01 "Tempered glass": relocate its leader endpoint to the FRONT glass pane, not side glass. Route line unobtrusively around hero.
4. CALLOUT 06 "Hidden clips": move it into the exploded front inset if needed, with leader to small concealed clip on FRONT frame. Do not point it at a side-panel thumb screw.
5. EXPLODED FRONT LAYERS: show exactly TWO matching narrow PERFORATED INTAKE STRIPS as a paired group labelled "Intake strips x2 / 19 x 432 mm"; and exactly TWO matching fine MESH FILTER STRIPS as a separate paired group labelled "Dust filters x2". Glass, open frame, paired vent strips, paired filters, three-aperture bracket, three fans. Add four tiny hidden edge clips on frame corners, dark, no prominent exposed glass screws.
6. ARGB CONNECTOR: current illustration incorrectly contains four metal contacts. It must have FOUR housing positions but ONLY THREE recessed metal female contacts: left 5V, next D, THIRD position solid BLACK PLASTIC KEY with NO hole and NO metal, last GND. Exactly THREE wires exiting. Keep the labels "5V D - GND" and "3-pin 5V / G5".
7. The small "PACKAGING LIMIT" panel currently has only text. Include a very small schematic SECTION showing parallel glass slab (2mm), gap(0.9mm), fan inlet rim; retain "Airflow validation required". Diagram gap may be exaggerated for legibility, label "Section enlarged".
Retain exactly two USB-A, one power button and one audio jack on TOP. Keep dimensions 220W 470H 450D, installed glass side, existing AIO, GPU, RAM, covered riser, perforated PSU shroud and feet. Keep hero fully assembled, main front pane in place. No logos whatsoever. All eight numbered callouts legible exactly once. Output one complete revised sheet.
```

### Sửa điểm chỉ dẫn

```text
Edit only the annotation graphics on this reference sheet. Preserve every rendered object, material, geometry, fan, connector, section and all other text.
REMOVE the cyan 01 badge, "Tempered glass" text and its entire leader currently on the LEFT of the hero, pointing incorrectly to a motherboard heatsink through side glass.
Instead put the SAME cyan "01" badge immediately beside the label "Tempered glass pane 158 x 456 x 2 mm" above the detached FRONT pane in the FRONT LAYERS inset. That existing short vertical leader to the detached pane correctly identifies the front glass.
RESTORE missing callout 04: add a cyan "04" badge and the short label "Removable dust filters" in the free margin just above the FRONT LAYERS inset or in its lower margin, with a clean thin cyan leader that ends on the pair of fine-mesh dust filters (the two wide grey narrow rectangles between perforated strips and fan bracket). Avoid other labels.
Make sure callout numbers 01,02,03,04,05,06,07,08 are all visible exactly once. Keep other annotations unchanged. No new logos or text.
```

### Sửa số đo trên nhãn

```text
Make a single tiny typography correction to this sheet. Under callout 01 in the FRONT LAYERS inset, the glass size is incorrectly reading "168 x 456 x 2 mm". Replace that complete dimension text with the exact text "158 x 456 x 2 mm" (ONE FIVE EIGHT, not one six eight). Use crisp readable type, slightly larger if necessary within that label area. Change absolutely nothing else anywhere on the sheet. Preserve all eight callouts, renderings, keyed three-contact connector and case.
```

</details>
