# 13 / Nóc thoáng rộng, nút nguồn ở giữa

Ảnh: [13_case_top.png](13_case_top.png). Tham chiếu: [ảnh người dùng](ref/case_top_style.png). Giữ màu đen và kính trước của [phần 12](12_case_front.md); lấy từ ảnh mẫu cách dùng khung viền, gân chéo và lọc bụi tháo được. Không logo. Đây là đặc tả để dựng mô hình; chưa thay mã nguồn hoặc linh kiện trong máy.

## Kích thước và hướng nhìn

Đơn vị **mm**, tọa độ toàn máy **(X; Y; Z)**. X từ lưng −225 tới trước +225; Y từ đáy −235 tới nóc +235; Z từ nắp kín −110 tới kính hông +110. Thân vẫn **220 rộng × 470 cao × 450 sâu**, không tính chân và các đầu nút/vít nhô sẵn. Tôn nóc giữ Y = **232…235**, dày 3. Nhìn bằng từ trên: lưng ở trên ảnh, trước ở dưới, −Z bên trái, +Z bên phải.

Thay riêng cửa thoáng, nan cũ và bố trí I/O của nóc. Quy định “không dời I/O” trong phần 12 được thay bằng bảng dưới. Không đổi kính trước, khung hông, chân, bo mạch, két, quạt, ống, grommet hay đường dây chính. Két vẫn X = **−140…137**, Z = **−24…96**, mặt trên Y = **231**; không dịch két về giữa để khớp cửa thoáng mới.

## 01–03 / Cửa thoáng và gân

| Chi tiết | Hình học chính xác | Vật liệu / màu |
|---|---|---|
| 01 / Cửa thoáng | Hình chữ nhật bo **R6**, X = **−175…155**, Z = **−94…94**, kích thước **330 × 188**. Khoét xuyên tôn trừ gân, ray và tai được giữ lại bên dưới | Mép thép `#16181D`, roughness 0,65, vát 0,3 |
| Viền cố định quanh cửa | Vành ngoài X = **−184…164**, Z = **−103…103**, R15; trừ cửa R6 bên trong, rộng danh nghĩa **9**. Liền với tôn nóc | Thép sơn nhám `#16181D` |
| 02 / Gân trang trí chịu đỡ | Gân sống giữa rộng **6**; gân chéo và gân ngang rộng **5**, đều Y = **232…233,4**. Công thức bên dưới | Thép graphite `#2A2E35`, roughness 0,6 |
| 03 / Tấm đục lỗ cố định | Bao X = **−177…157**, Z = **−96…96**, R8; dày **0,6**, Y = **233,4…234**. Mép chồng vào khung 2; phần chồng và vùng trên gân để đặc | Thép đục lỗ `#101216`, roughness 0,7 |

Cửa rộng thêm **60**, từ 128 lên 188, tức **46,875%**; dài thêm 30. Hai mép cửa cách cạnh thân cùng **16**. Hai mép ngoài khung lọc cách cạnh thân cùng **7**. Sau lọc chừa 41 tới lưng; trước lọc chừa 61 tới mặt trước để đặt I/O. Không đục thêm hàng khe dài ngoài khung.

**Dựng gân theo mặt phẳng (X; Z):** đặt `a[i] = -175 + 110*i`, i = 0…3. Với mỗi ô i = 0…2, tâm `C[i] = (a[i]+55; 0)`, tương ứng **(−120;0), (−10;0), (100;0)**. Nối C[i] tới bốn góc **(a[i];±94), (a[i+1];±94)** bằng bốn đoạn rộng 5. Thêm sống giữa từ **(−175;0) tới (155;0)**, rộng 6; thêm hai gân ngang từ **(−65;−94) tới (−65;94)** và **(45;−94) tới (45;94)**, rộng 5. Bề rộng đo vuông góc đoạn, đầu đoạn tròn bán kính nửa bề rộng. Hợp các đoạn thành một khối, cắt theo biên cửa R6, nối liền vào viền. Kết quả là **ba ô X hoàn chỉnh, dài bằng nhau**, đối xứng qua Z = 0. Bo các giao điểm trong R1; không tạo ba quạt mới theo ba ô.

**Lỗ tròn thật Ø1,2**, bước tâm **1,8**, lưới tam giác so le. Tâm lỗ:

```text
X = -175 + 1.8*i + 0.9*(j mod 2)
Z = -94 + (sqrt(3)/2)*1.8*j
i, j là các số nguyên không âm; chỉ giữ tâm nằm trong miền hợp lệ.
```

Giữ lỗ khi toàn bộ đường tròn cách biên cửa bo R6 ít nhất **1**; cách biên gân/ray đặc ít nhất **0,6**; cách tâm mỗi vít S3 ít nhất **7**. Không đục phần chồng khung. Khoét riêng tám cửa tránh tai Ø12 trong tấm đục lỗ, cùng tâm S3. Hợp tấm với viền và gân tại mặt tiếp xúc, tránh hai mặt đồng phẳng khi dựng. Lỗ là khoảng rỗng, không phải chấm màu. Diện tích mở lý thuyết của lưới lỗ khoảng **40,3%**, chưa trừ gân, viền, vít và lọc; đây không phải số liệu lưu lượng.

## 04–05 / Lọc nam châm và tám điểm gá két

Lọc là một cassette **348 × 206**, X = **−184…164**, Z = **−103…103**, ngoài R15; cửa trong 330 × 188, R6. Viền nylon đen `#202329`, rộng 9, Y = **234,4…236**, dày 1,6. Phay bậc đỡ trên viền thép tới Y = 234,4, biên ngoài bậc nới **0,2 mỗi phía**, R15,2. Bậc không mở rộng cửa thông gió. Giữ tôn dưới bậc dày 2,4; góc mặt ngoài viền lọc bo 0,4.

Lưới lọc PET đen `#202329`, ô vuông mở **0,35**, sợi **0,15**, bước **0,5**. Hai phương sợi song song X/Z; tâm sợi tại X = −175 + 0,5k và Z = −94 + 0,5m. Chiều dày tổng lưới **0,3**, Y = **235,2…235,5**, kẹp mép vào viền. Gân X thuộc nóc cố định, **không đi theo lọc khi nhấc**. Lọc không mang dây hoặc đèn.

Tám nam châm **8 theo X × 5 theo Z × 0,8 theo Y**, tâm X = **−156, −58, 38, 136**, Z = **−98,5 và 98,5**, Y = **234,85**. Tám tổ hợp, nằm hoàn toàn trong viền đặc; đáy nam châm Y = 234,45, chừa lớp phủ/keo 0,05 tới bậc thép. Nam châm xám `#636A73`, ẩn khi lắp. Hai bên đặt đối xứng để lọc không vênh.

Tai kéo PET mỏng **10 theo X × 8 theo Z × 0,25**, X = **154…164**, Z = −4…4, Y = 235,65…235,9, đen cùng viền. Một hõm móng tay ở tôn trước viền: X = **164…170**, Z = **−7…7**, đáy Y = **234,4**, góc R2. Tai nằm trong bao lọc, không thêm tay nắm cao.

**Giữ nguyên cả tám tai và vít S3.** Tâm trục Y của vít là tích Descartes:

| X | Hai giá trị Z |
|---|---|
| −114 | −16,5; 88,5 |
| −9 | −16,5; 88,5 |
| 6 | −16,5; 88,5 |
| 111 | −16,5; 88,5 |

Mỗi tai là vòng thép **Ø10 ngoài / Ø3,4 lỗ**, Y = **232…235**, đúng mã hiện tại. Bổ sung **hai ray rộng 4**, tâm Z = −16,5 và 88,5, X = −175…155, Y = **232…233,4**. Ray nối tai tới hai đầu khung; hợp ray, gân và tai, sau đó khoét lại lỗ Ø3,4 xuyên đúng tâm. Ray lệch tâm theo két, không dời chúng để ép đối xứng. Hàng +Z gần mép được nối thêm trực tiếp vào khung bằng phần giao hình học.

Theo mã két, đệm S3 Ø7 nằm Y = **234…235** và đầu vít Ø5,6 nằm Y = **235…237**. Giữ bộ **8 × M3 × 6 và 8 đệm** hiện hữu; không tăng chiều dài vít hoặc hạ két. Lưới lọc có **tám lỗ thông Ø10**, cùng các tâm trên, viền gia cường PET **Ø12 ngoài / Ø10 trong**, Y = **235,2…235,5**. Hợp vòng gia cường vào sợi và viền lọc khi chạm; không thêm nút bịt. Lỗ lọc cách đệm bán kính 1,5 và đầu vít 2,2 nên lọc nhấc thẳng lên được mà không tháo S3. Hai hàng X = −9 và 6 chỉ cách nhau 15; vẫn còn **3** giữa hai vành Ø12.

Gân/ray thấp nhất Y = 232, cách mặt két 1; tấm lỗ bắt đầu Y = 233,4. Nắp nạp của két tại khoảng **(127,5;232;76)** có đỉnh Y = **233**, còn 0,4 tới tấm lỗ và nằm ngoài gân/ray. Mép trên quạt sau khoảng Y = 205, thấp hơn gân 27. Mép khung kính ở Z = 106…110 không bị cửa hoặc bậc lọc cắt vào. Đỉnh lọc Y = 236 thấp hơn đỉnh vít hiện hữu 237; thân không tăng kích thước. Tám cửa nhỏ quanh vít có thể cho bụi lọt, nên không gọi đây là lọc kín tuyệt đối.

## 06–10 / I/O cân giữa

Vùng I/O trên mặt nóc: **X = 174…210, Z = −88…88**, góc R6, màu nền `#16181D`. Đây là vùng bố trí với đường khắc viền rộng 0,3 sâu 0,15, không phải khối nổi. Cách khung lọc 10 theo X. Các mặt cổng hướng **+Y**. Nút nguồn nằm đúng tâm vùng ở **X190, Z0**.

| Số / chi tiết | Tâm mặt ngoài (X;Y;Z) | Kích thước và màu | Đầu nối trong máy |
|---|---|---|---|
| 08 / Reset | **(190;234,5;−68)** | Nút Ø5, lõm 0,5 dưới nóc; vành ngoài Ø7, lỗ tôn Ø5,4. Nút `#2A2E35`, vành `#929AA5`, không LED | Nút nhấn thường hở; hai dây vào **RESET SW**, đầu 1×2, F_PANEL chân **5–7**, không phân cực |
| 07 / USB-A bên −Z | **(190;235;−32)** | Miệng tôn **6 theo X × 13 theo Z**; vỏ 7×14, Y = 227…235,2; lưỡi xanh `#1F6FD1`, kim loại `#C9CED4`, tiếp điểm `#C6A565` | Nhánh thứ nhất của dây đôi **USB 3.0 19-pin** |
| 06 / Power | **(190;236,6;0)** | Nút Ø14, thân nút Y = 235…236,6; lỗ tôn Ø14,4; mặt `#2A2E35`. Vành khuếch tán **Ø16,2 ngoài / Ø14,4 trong**, Y = 235…236,2 | Nút thường hở, hai dây **PWR SW**, đầu 1×2, F_PANEL **6–8**, không phân cực |
| 06 / Vòng PLED quanh Power | **(190;236,2;0)** | Cyan **`#22D3EE`**, sáng dịu; dùng chính vành nguồn, không thêm chấm LED nguồn | Hai dây **PLED**, đầu 1×2: chân **2 (+), 4 (−)**. Không nối vào ARGB |
| 07 / USB-A bên +Z | **(190;235;32)** | Cùng hình học, vật liệu và 9 tiếp điểm như cổng −Z | Nhánh thứ hai của **cùng một** đầu USB 3.0 19-pin; không dùng thêm header |
| 09 / Headset combo | **(190;235;68)** | **Lỗ âm thanh tròn Ø3,5**, vành ngoài Ø7 / trong Ø3,5, Y = 234,5…235; lỗ lắp thân Ø7,2. Vành `#929AA5`, lòng `#050506`; thân chọn Ø8 tối đa, Y = 221…234,5 | Một jack **TRRS CTIA** cho tai nghe + mic, cáp có shield tới **HD_AUDIO 9-pin**; mạch I/O nối L/R/GND và mic, có jack detection |
| 10 / HDD LED | **(204;235;0)** | Mặt lens Ø1,5, lỗ tôn Ø1,6; amber **`#F59E0B`**, nháy khi có hoạt động; ở phía trước nút nguồn, không chen vào hàng cổng | Hai dây **HDD LED**, đầu 1×2: F_PANEL **1 (+), 3 (−)** |

Thứ tự từ −Z sang +Z: **Reset / USB-A / Power / USB-A / Headset**. Tâm cặp USB đối xứng ±32; cặp nút nhỏ và jack đối xứng ±68, hai vành ngoài cùng Ø7. Chữ/biểu tượng khắc `#AEB4BC`, cao 2, ở X = 202 tại Z = −68, −32, 32, 68; biểu tượng nguồn nhỏ trên nút. Không in chữ hãng. Chữ HDD cao 1,5 tại (207;235;0), không thêm biểu tượng mic thành một lỗ thứ hai.

Chọn **combo TRRS**, không jack mic rời. Headset hai phích cần dây chia phù hợp; không làm thêm cổng giả chỉ để cân hình. **Không thêm USB-C**: phần 02 không có header Type-E key A, thêm nó sẽ đổi bo và dây trong máy. Hai USB-A hiện có dùng đủ hai kênh của đầu 19-pin.

Thân công tắc và mạch I/O được giới hạn **X = 182…196**, Y = **219…232**, Z = **−76…76**; riêng nhánh LED tới X = 206 chỉ nằm **Y = 229…232**. Nút có cổ nhỏ qua lỗ tôn, thân công tắc nguồn tối đa 12 theo X, không kéo bề rộng Ø16,2 của vành xuống dưới. Như vậy phần thân thấp tránh quạt trước bắt đầu X = 197; nhánh LED ở trên mép quạt 225. Mạch gắn vào trụ trên mặt dưới nóc, không vít nổi mới. Đây là bao thiết kế để chọn linh kiện, không phải kích thước xác nhận của một cụm I/O bán sẵn.

## Đầu dây và lộ trình giữ nguyên

Các tọa độ header cục bộ dưới đây lấy từ [phần 02](02_motherboard.md), gốc bo toàn máy (−88;62,5;−96). Các thân và đầu dây màu `#202329`, vỏ cáp `#15171B`; dấu + ở nhánh LED màu `#D2D7DD`.

| Bó dây | Header cục bộ trên bo (X;Y) | Qua grommet | Yêu cầu |
|---|---|---|---|
| Hai USB-A | USB 3.0 **(113;−127)**, toàn máy **(25;−64,5)** | **G2 → sau khay → G5** | Một đầu cái khung 2×10, 19 tiếp điểm, vị trí 20 bịt khóa; hai nhánh USB có shield, không ghép thành dây USB 2.0 |
| Headset | HD_AUDIO **(−97;−143)**, toàn máy **(−185;−80,5)** | **G2 → sau khay → G6** | Khung 2×5, 9 tiếp điểm, khóa ở vị trí 8; dùng cáp HD_AUDIO, không AC'97 |
| Power, Reset, PLED, HDD | F_PANEL **(95;−143)**, toàn máy **(7;−80,5)** | **G2 → sau khay → G5** | Bốn đầu cái riêng 1×2 và tám dây; chân 9 bỏ trống, chân 10 khuyết; cặp chân theo bảng I/O |

Chỉ các nhánh ngắn ngay dưới mặt nóc đổi vị trí theo tâm mới. Cho nhánh từ từng cổng/nút đi trong vùng I/O về điểm gom hiện có **(190;222;−30)**; có đoạn chùng để nhấn nút và thao tác cổng, không chạy dây dưới lưới thoáng. Tiếp tục đúng tuyến `GamingRig.tsx`: **(186;206;−94) → (150;112;−101) → (70;104;−101) → (63;104;−101)** rồi qua G2 về **(58;96;−106,2) → (40;−40;−106,2)**. G2 tâm (57;110), G5 (23;−104), G6 (−184;−104). USB/F_PANEL ra G5; audio đi tiếp phía sau qua (−60;−128) rồi ra G6. Đoạn từ lỗ ra header và các đầu cắm giữ như mã hiện tại, không đổi chân bo.

Giữ bó sau khay trong hành lang có sẵn, tâm dây Z = **−107,8…−104,7**. Không thêm bó Type-E, không dùng G4 của GPU, không chăng dây qua quạt hoặc mặt bo. Chiều dài nhánh mới phải đo theo đường tâm uốn sau khi dựng; brief này không cắt ngắn dây chính hoặc tự đặt một chiều dài dây thẳng thay cho tuyến thực.

## Người mới cần nhận ra

1. **Nút lớn có vòng cyan ở giữa là nguồn.** Nút nhỏ lõm ở đầu trái là Reset, không phải nút đổi đèn. Vòng cyan báo nguồn, chấm amber riêng báo hoạt động ổ.
2. **Jack phải có lỗ tròn Ø3,5 nhìn thấy rõ.** Một phích headset TRRS mang cả tai nghe và mic; hai ô chữ nhật xanh là USB-A.
3. **Nhấc lọc, không tháo vít két để vệ sinh.** Tám lỗ của lọc đi qua đầu vít; gân và két vẫn ở nguyên trên máy. Tắt máy, nhấc từ tai kéo, làm sạch rồi để khô trước khi lắp lại.
4. Ba ô X chỉ là gân đỡ. Két vẫn là AIO 240 và lệch về phía kính theo bố trí cũ; các lỗ vít không đối xứng với cửa thoáng. Không dời két để làm hình nhìn cân hơn.
5. Cửa rộng hơn không tự chứng minh máy mát hơn. Lọc và tấm đục lỗ vẫn cản khí thoát lên trên; chưa có thử nghiệm nhiệt hoặc kiểm tra dung sai chế tạo.

## Tạo ảnh và kiểm tra

Dùng **imagegen tích hợp**, ảnh người dùng làm tham chiếu gân/khung/lọc; ảnh phần 12 làm tham chiếu màu và vỏ kính. Ảnh diễn tả vật liệu và thao tác; khi dựng, dùng các tọa độ và công thức trong tài liệu, không đo từ pixel. Chỉ bàn giao một ảnh và một đặc tả, không sửa mô hình hiện tại.


Ảnh cuối có đủ ba góc nhìn, callout 01–10, hai USB-A, nguồn ở giữa, Reset và jack tròn. **Giới hạn ảnh sinh:** chiều dài các ô gân chưa bằng nhau, ô cuối trong hình bằng còn đọc như chữ V; khoảng cách hàng vít và hình gân trong phối cảnh chỉ gần đúng. Không sao chép các sai lệch này vào mô hình: dựng ba ô X dài 110 và tám tâm S3 đúng bảng. Lỗ đục/lưới cũng được phóng lớn để nhìn rõ. Bộ lọc nhấc lên trong ảnh chỉ minh họa thao tác, không phải vị trí lắp Y thực.

<details>
<summary>Prompt tạo ảnh và các lượt sửa dùng cho ảnh bàn giao</summary>

### Tạo ảnh

```text
Use case: product-mockup / infographic-diagram.
Create ONE polished landscape product reference sheet titled "13 / CASE TOP", subtitle "220 W x 470 H x 450 D mm". White studio background, charcoal black powder-coated steel #16181D, dark graphite mesh #101216, restrained cyan #22D3EE annotations and power ring. Short crisp English labels, realistic material rendering, generous whitespace. No brands, logos, hub markings or watermark.

INPUTS: first image is STYLE REFERENCE ONLY: copy its wide inset mesh field, structural diagonal ribs, perimeter frame and removable magnetic filter feel. Do not copy grey color, orange/yellow illumination or its proportions. Second image shows existing black glass-front/glass-side case family. Preserve that case style, only redesign roof. Existing front fans cyan, top radiator and top fans black with NO lighting.

COMPOSITION: left 42% large true ORTHOGRAPHIC TOP PLAN of entire roof, portrait rectangle 450 deep by 220 wide; REAR at top and FRONT at bottom. Z negative left, positive right. Right upper area a realistic close THREE-QUARTER view of the same case TOP, showing roof and upper front/side only; filter lifted slightly above vent to reveal structural ribs, filter mesh translucent, with an arrow "Lift to clean". Right lower area large straight-on TOP I/O DETAIL spanning full strip width. Exactly these three primary views. Show dimension arrows "450", "220", "330 x 188". Footer "Concept reference / dimensions in specification".

GEOMETRY: roof plane X -225 rear to +225 front, Z -110 to +110. Vent opening X -175 to +155 (330 length), Z -94 to +94 (188 width), corners radius 6. The vent is WIDE and horizontally centered with narrow even side margins. Outer magnetic filter frame X -184 to +164, Z +/-103: 348 x 206, radius15, 9mm rim. Roof remains solid behind it and in front under I/O. In plan front edge of vent is near bottom but leaves generous I/O strip area.

RIB PATTERN: three equal 110mm-long cells along depth, boundaries X=-175,-65,45,155. Cell centers X=-120,-10,100. A single 6mm spine runs on Z=0 from rear to front. Each cell is an X truss: four diagonal 5mm ribs join its four corners at Z=+/-94 to its center on Z=0. Two 5mm transverse ribs at X=-65 and45. This creates THREE regular X motifs along the roof, symmetric left/right, not random zigzags, not old straight slats. Fine ROUND true perforations diameter1.2mm, staggered1.8mm pitch, densely fill triangular bays. Magnetic filter on top has fine PET mesh, dark rim; ribs remain visible through it. Eight radiator screw positions at X=-114,-9,6,111, each at Z=-16.5,+88.5. Keep this asymmetric hardware layout, don't change to symmetric four corners. Eight small round reinforced 10mm clearance windows through the removable mesh expose screw heads without undoing screws. In lifted filter show eight matching mesh windows, no loose screws. Fixed mount rails remain subtle under mesh. Existing radiator below stays offset to positive Z, NOT centered to match the vent. No extra radiator or fans.

TOP I/O: one understated strip X174..210, Z-88..88, centered on roof width. All five main controls at X190. EXACT left-to-right order in front-facing top detail (Z negative to positive): small round RESET at Z-68 with outer bezel diameter7 and recessed button diameter5; blue USB-A 3.0 at Z-32 (opening6 along X x13 along Z); CENTRAL POWER at Z0, button diameter14 with cyan ring outer diameter16.2; second blue USB-A 3.0 at Z+32 same dimensions; round HEADSET COMBO at Z+68, outer metal ring diameter7, true black circular bore3.5mm. Power is exactly at geometric center of I/O and roof width. One tiny amber HDD LED diameter1.5 at X204,Z0, directly toward front from power. Power cyan ring is PLED, no separate extra power dot. No USB-C, no separate MIC jack. Exactly TWO USB rectangles, ONE large round power, ONE tiny round reset, ONE round headset jack, ONE small amber dot. Keep same arrangement in all three views. Labels below detail "RESET", "USB-A 3.0", "POWER / PLED", "USB-A 3.0", "3.5 mm TRRS"; small "HDD" beside amber dot. Under row include subtle spacing note "Z: -68 / -32 / 0 / +32 / +68 mm".

NUMBERED CALLOUTS, each badge once, clean short leaders ending on the named feature:
01 "Wide vent / 330 x 188"
02 "Three-cell rib structure"
03 "Fine perforation / 1.2 mm"
04 "Magnetic filter / 348 x 206"
05 "8 screw clearance windows"
06 "Centered power / PLED"
07 "2 x USB-A 3.0"
08 "Recessed reset"
09 "Round headset / TRRS"
10 "HDD LED"
Distribute 01-05 around plan and angled top; 06-10 around I/O close-up. Keep labels outside parts and avoid overlapping. Main image must feel balanced, crafted, mechanically plausible and brand-free. No extra ports, rainbow lighting, logos, decorative text, UI screenshots or full internal rebuild.
```

### Sửa 1

```text
Edit this CASE TOP reference sheet. Keep the composition, title, materials, all ten callout numbers and three views. Correct these mechanical errors consistently across all views.

1. I/O CLOSE-UP currently has THREE USB-A ports (two left of power and one right). DELETE the extra left USB. Rebuild the row with exactly FIVE main objects equally balanced about power: RESET at Z=-68, one USB-A at -32, POWER at 0, one USB-A at +32, round HEADSET at +68. Main row all same X=190. Power remains precisely centered. Callout07 leader can branch to the TWO remaining USB ports, one each side. Move amber HDD dot OFF the horizontal row: place it DIRECTLY BELOW the central power (toward case front), at X204,Z0. It is a tiny 1.5mm amber point, not a sixth full-size control. Leader10 points to this tiny dot below power. Do the same in plan and three-quarter. No small white dot above power.
2. PLAN screws: replace symmetric left/right screw columns with the actual offset mount pattern. Roof left-right Z -110 to110. All EIGHT screw windows form two columns at Z=-16.5 and Z=+88.5. Thus first column is just LEFT OF CENTRAL SPINE, second is NEAR RIGHT EDGE. FOUR rows in each column at X=-114,-9,+6,+111 (rear=-225 top, front=+225 bottom). Middle two rows are a CLOSE PAIR separated by15mm. No screws at left edge, no screws at roof corners. These are 10mm mesh clearance windows around unchanged small screw heads. Render exactly eight, also in lifted filter.
3. RIBS: use three full equal-depth X motifs stacked on the roof, each spans full mesh width. Boundaries X=-175,-65,+45,+155; centers X=-120,-10,+100. Four diagonal ribs from each cell's four corners to its center. Single thin center spine, two thin crossbars separating the cells. Thus THREE complete X patterns, not one X plus diamonds and an incomplete end. They are FIXED to roof.
4. LIFTED FILTER must be ONLY fine translucent mesh plus slender perimeter magnetic frame and eight reinforced screw clearance windows. DELETE the thick diagonal and central ribs from the lifted filter, those ribs remain fixed on the roof below. Make filter transparent enough to read underlying roof. No second thick rib lattice on filter.
5. Keep main roof vent visibly wide, outer348x206 relative whole roof450x220, fine1.2mm perforations (smaller and denser than current image), understated inset frame, no extra slots outside the specified vent. Labels and dimensions unchanged. Short labels only, no logos. Output one complete corrected reference sheet.
```

### Sửa 2

```text
Make precise corrections to this reference sheet, preserving all other design, I/O objects and text.
LEFT TOP PLAN: redraw ONLY the vent interior as exactly THREE EQUAL rectangular bays, each containing a COMPLETE X brace. Vent interior screen pixel bounding approximately x240..490,y177..781. Three bay vertical spans 177..378, 378..580, 580..781. Horizontal crossbars at y378 and580. In EACH bay draw diagonals from all four corners to its own center (centers approximately x365,y278 /479 /680), plus one uninterrupted center vertical spine at x365. No two huge Xs, no unbraced bottom strip. Fine perforation fills every triangular space.
There must be EXACTLY EIGHT circular screw windows in this plan: two vertical columns at screen x343 and482; rows y289,481,509,701. These pixel positions are layout guides within this sheet, matching the required case coordinates. Close middle two rows have a small gap. Remove ALL old screw windows before adding these exact eight. Keep existing callout05 but retarget leader to an actual screw window.
Apply three equally long X-braced bays and eight matching screw windows to angled roof; lifted filter itself has only mesh, thin rim and eight holes, no structural X ribs.
LABEL FIXES: in I/O detail, DELETE entire duplicate 07 badge and misspelled 'HSB-A 3.0' above right USB. Retain single07 above left USB but text should read '2 x USB-A 3.0'. There are still two correctly drawn USBs, one each side power. Keep all lower USB labels unchanged. Callout10 HDD label is crowded under power: move10 badge and 'HDD LED' to open white space BELOW and RIGHT of amber dot, with short leader. Keep POWER/PLED lower label legible. Each badge01..10 appears ONCE. Remove tiny white dot just above power in PLAN. Keep amber dot below power, centered between USBs in ALL views. Don't add any ports. Output entire sheet.
```

### Sửa 3

```text
In this image, replace the WHOLE left orthographic vent interior with a fresh mechanically precise drawing. Preserve its outer rectangular filter frame, roof, I/O, all text, and both right-side views. This is a localized redraw, not a restyle.
The new vent has exactly THREE equal-height X-braced bays stacked vertically: TOP X, MIDDLE X, BOTTOM X. Each bay one-third the vent height. Two horizontal separators, at one-third and two-thirds height. Each X consists of two diagonals corner-to-corner. A narrow continuous central vertical spine. The old design has two Xs and an empty bottom region: erase all of it and draw three complete identical Xs.
Behind them is very fine dark round perforated sheet.
Erase every existing screw. Add EXACTLY EIGHT small screw heads/clearance circles. Arrange them as two columns and four rows. LEFT column is close to center spine, slightly LEFT of it, NOT near the left edge. RIGHT column is close to the right inner edge. Four row positions measured from top of vent: 18.5%, 50.3%, 54.8%, 86.7%. Thus the two center rows are close together, with only a small gap between circles. Small 10mm diameter circles relative to188mm vent width. No other circles, no screws on far-left edge, no screws in corners. Retarget leader05 to a right-column screw. Do not change text, right-side images, or I/O. The priority is THREE complete equal Xs and EIGHT precisely arranged small circles in this one left view.
```

</details>
