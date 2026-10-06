# 05 Slim console — Tổng quan lắp ráp

Ảnh: [00_overview.png](00_overview.png). Nguồn: [đặc tả họ vỏ](../00_families_spec.md), mục 0, 5, 7, 8; đầu nối theo [Gaming](../../gaming/00_blueprint.md). **Số dưới đây là chuẩn cho mô hình mô phỏng; không đo kích thước hoặc đếm linh kiện từ ảnh tạo sinh.**

## Trục và kích thước

Đơn vị mm; O ở tâm thân, không tính đế. +X ra trước, +Y lên nóc, +Z sang hông lắp linh kiện. W × H × D = Z × Y × X. Thân **100 × 360 × 330**, biên X ±165, Y ±180, Z ±50; 100 mm = 1 đơn vị cảnh. Tọa độ là trạng thái đã lắp, không phải vị trí tách lớp.

Đế đứng bao 280 X × 18 Y × 140 Z, tâm **(0;−189;0)**; tổng đứng **140 × 378 × 330**. Hai ray vát, hai cầu tại X ±85, lòng đỡ thân 100 có đệm 1 mỗi bên; không bịt cửa đáy. Khi nằm ngang, tháo đế, gắn bốn chân keo **12 × 12 × 8** tại **(±137;±148;−54)**; mặt bàn Z −58, hông +Z hướng lên. Bao theo trục thân là 330 X × 360 Y × 108 Z; cao khỏi bàn 108, khe dưới hông −Z là 8.

## Cụm và mốc lắp

| Số / cụm | Bao và vị trí đã lắp |
|---|---|
| 1 — Case | Khung thép 1,2, tấm hông 1; thân R6. Khay X −158…43, Y −174…38, mặt trước Z −36,5, mặt sau −37,7; không kéo khay kín qua tầng GPU. |
| 2 — ITX | PCB 170 X × 170 Y × 1,6 Z; **B = (−66;−65;−30)**. Bao X −151…19, Y −150…20, linh kiện hướng +Z, I/O hướng −X. Bốn chân đế cao 6,5; đưa bo từ +Z về khay. |
| 3 — CPU | 37,5 × 37,5 × 4,5; đáy (−86;−53;−25,3), IHS **(−86;−53;−20,8)**. Mặt dưới LGA phẳng, tam giác/rãnh khóa; hạ theo −Z vào socket, không có vít CPU riêng. |
| 4 — Tản thấp | **92 X × 92 Y × 47 Z từ IHS**; X −132…−40, Y −99…−7, Z −20,8…26,2. Quạt 92 × 92 × 14, tâm (−86;−53;19,2), hút +Z và thổi −Z. Backplate 90 × 90 sau bo, đệm cách điện 0,8. |
| 5 — RAM ×2 | DDR4 133,35 Y × 42 Z × 6 X; gốc đáy (−11;−59;−24,5) và (−1;−59;−24,5); đỉnh Z 17,5. Bo có đúng hai khe và lắp cả hai; ấn −Z, giữ nguyên rãnh DDR4. |
| 6 — GPU | `gpu_240_dual`, **A = (−41;40;8)**, quay **R−(x;y;z) = (x;z;−y)**. Thân X −161…79, Y 40…160, Z −14,5…30,5; hai rotor Ø90 hướng +Z, tâm (−101;100;20) và (19;100;20). Cắm theo −Y vào adapter riser; card nằm trên bo, không phủ CPU. |
| 7 — SSD | Một M.2 2280, 80 X × 22 Y × tối đa 3, trên mặt linh kiện bo. Tâm XY (−78;−113), miệng (−118;−113), vít (−38;−113), chân đỡ cao 3. Cắm nghiêng M-key rồi hạ đầu vít. |
| 8 — SFX | Cục bộ 100 X × 63,5 Y × 125 Z, quay R−, tâm **(97;−90;0)**. Bao X 47…147, Y −152,5…−27,5, Z ±31,75. Quạt 92 tâm (97;−90;24), hút +Z; AC/xả ở −X, dây liền ra +X. Giá L dày 1,2 tại X47, khoét thoáng xả và C14. |
| 9 — Fans | **0 quạt case, 0 radiator**. Chỉ có một quạt CPU, hai rotor GPU và một quạt tích hợp PSU. Không có gá quạt case. |
| 10 — Cables | Dây mềm đen: ATX 250, EPS 350, GPU 350, R uốn ≥12; dây đã buộc cách cánh ≥10. Dây I/O theo nguồn chung tối đa 650, cất dư ở hành lang, không ép sau socket. |

Riser: dải mềm **400 × 70 × 1,5**, R uốn ≥15; đầu đực 89 × 12 × 1,6, đầu cái 110 × 18 × 14, hai tai cách 100. Đầu bo **(−63;−137;−19,6)** → G4 → sau khay → mép Y26 → đầu nhận GPU **(−63;35,8;−11,7)**. Vòng chùng nằm nửa trên phía −Z. Luồn đầu đực qua lỗ 96 trước, giữ đầu cái bên GPU; không dùng cửa CPU làm lối dây.

Cassette lưng có **3 khe dọc**, Y 38…158, Z −17,7…43,26, bước 20,32. Lá GPU dùng hai khe; khe thứ ba giữ trống. Giá GPU X −161…94, Y 34…36, Z −3…15. Bao GPU chưa gồm chân PCIe nhô 8, lá cổng nhô 1 và đầu vít tối đa 3; quay cả phần nhô bằng R−.

## Panel, lỗ và cổng

| Mặt | Chi tiết |
|---|---|
| Trước +X | Cassette R8, X153…165, Y±174, Z±45; lưới trừ vùng I/O thấp; bốn ngàm và hai vít. |
| Hông ±Z | Tấm 314 X × 340 Y × 1, X±157, Y±170, mặt ngoài Z±50. Cửa lỗ X±142, Y±155, lọc phía trong; tháo vít, trượt −X rồi nhấc ra ±Z. |
| Nóc | Cửa X−140…130, Z±34; cố định, không tháo như một nắp rời. |
| Đáy | Hai cửa X−138…−20 và 20…138, Z±30; giữ vùng giữa cho đế, không bịt hai cửa. |
| Lưng | I/O ITX thấp, ba khe GPU cao, inlet C14 tại (−165;−166;28); không quạt sau. |

Lưới thép dày 0,8; lỗ Ø2,4, bước 3,6 so le, mép đặc 6, vùng vít bán kính 5. Lọc PET dày 0,5, mắt mở 0,35, sợi 0,15; viền nylon 1,5, nằm trong bao vỏ. Lỗ phải xuyên thật hoặc dùng alpha có viền dày phù hợp ở LOD xa.

Mặt trước tại X165, Z0: **Power Ø12 Y−92; USB-A 13 Y × 6 Z tại Y−119; USB-C 8,4 Y × 2,6 Z tại Y−143; combo audio Ø3,5 Y−160**. Không reset. Module X153…164; hành lang nguồn tại Z−40…−33 tránh module. USB-A dùng một nhánh USB19, nhánh còn lại bịt trong module; USB-C dùng Type-E, audio dùng HD_AUDIO.

Lá I/O bo X−165…−164,2, Y−144…14, Z−28,4…7,6; cửa vỏ nới 0,5 mỗi cạnh Y/Z. Cổng hướng −X, tâm Z−14: hai USB-A tại Y−7/−26, RJ45 Y−60, ba TRS Y−90/−107/−124. GPU có ba DisplayPort và một HDMI.

AC có vỏ kín dài tâm **450**, inlet sau → sát đáy Y−171 → C13 góc 90° trên PSU. C14 PSU tâm **(47;−47;9)**, switch (47;−47;−17); bao đầu C13 X22…47. Dây liền DC ra grommet Ø18 tại (147;−55;13), uốn trong hành lang X148…162. Không dựng đầu cứng dài 30 ở đây.

## Lỗ khay và tuyến dây

Các lỗ dưới đây nằm trên Z−36,5, kích thước lòng mở X × Y, viền EPDM rộng 3. Khoang sau khay Z−49…−37,7 chỉ nhận dây dẹt một lớp; bó lớn đi hành lang X25…43. Cửa bảo trì CPU **96 × 96**, tâm (−86;−53;−36,5), không luồn dây.

| Mã | Tâm X;Y | Lòng mở | Tuyến |
|---|---|---|---|
| 05-G0 | 31;−135 | 18 × 56 | PSU vào rãnh cạnh; đầu ATX theo cạnh dài 56 |
| 05-G1 | −121;29 | 28 × 10 | EPS |
| 05-G2 | 31;−62 | 18 × 58 | ATX24 |
| 05-G3 | −38;29 | 32 × 10 | CPU_FAN |
| 05-G4 | −68;−166 | 96 × 10 | Riser xuống sau khay rồi vòng lên giữa hai tầng |
| 05-G5 | 2;−166 | 22 × 10 | USB19, Type-E, F_PANEL |
| 05-G6 | −143;−166 | 22 × 10 | HD_AUDIO |

Nguồn GPU đi ở X88…119 tới ổ **(64;160;−4,75)** hướng +Y; dùng đầu **6+2 góc thấp, cao ≤12 theo Y**, dây quay về +X. Không phủ mặt hai quạt. G0 của riêng họ 05 là 56 theo bảng mục 5, khác mô tả chung 58 ở mục 0.

## Đầu nối và khóa

Tâm header bo ghi cục bộ (u;v) so với B; cao độ và chi tiết ổ lấy theo mô hình nguồn.

| Đầu | Số tiếp điểm / khóa | Mốc bo hoặc chức năng |
|---|---|---|
| ATX / EPS | 24 (2×12), ngàm, dây20+4 / 8 (2×4), dây4+4, khóa CPU | (80;5) / (−55;78) |
| GPU power | 8, 6+2, khóa PCIe; khác khóa EPS | Ổ hướng +Y như trên |
| CPU_FAN | PWM4, gờ dẫn hướng; GND/12V/tach/PWM | (17;73), quạt tản thấp |
| USB19 / Type-E | 19 trên20 vị trí, một khóa / 20, khóa định hướng | (80;−46) / (80;58) |
| F_PANEL | 9 trên2×5, khuyết10; PWR6/8, PLED+2/−4 | (60;−80); reset/HDD không dùng |
| HD_AUDIO | 9 trên10 vị trí, khuyết8 | (−62;−80), combo TRRS4 ngoài vỏ |
| PCIe x16 | 164, rãnh khóa và chốt | (3;−72), nối riser |
| DDR4 / M.2 | 288/thanh, rãnh lệch / 75 vị trí, 67 tiếp điểm quy ước, M-key | Hai DIMM (55;6)/(65;6); M.2 không dây SATA |
| Cổng ngoài | USB-A trước9/sau4, USB-C24; DP20×3, HDMI19; RJ45 8, TRS3×3; IEC C14/C13 3 | Lưỡi USB, khóa góc DP, miệng HDMI hình thang, lẫy RJ45, vỏ IEC định hướng |

SYS1 (−48;−53), SYS2 (−25;−53) có header PWM4 nhưng bỏ trống; không AIO_PUMP/ARGB/hub. Socket LGA dùng quy ước mô phỏng 1.200 tiếp điểm, không xác định socket thương mại.

## Vít và vật liệu

| Cụm | Số / loại | Mốc |
|---|---|---|
| Bo | 4 M3×5 + 4 chân đế M3 cao6,5 | X−141/9, Y−140/10; lỗ Ø3,4 |
| Tản | 4 M3×12 lò xo | X−125/−47, Y−92/−14; lưới78×78, lỗ Ø3,5 |
| Quạt CPU | 4 M3×16 | Tâm tản ±41,25 theo X/Y |
| SSD | 1 M2×2,5 | XY(−38;−113), chân đỡ3 |
| SFX | 4 M3×6 | X47, Y−145/−35, Z±25 |
| Lá GPU / nắp thứ ba | 2 + 1 vít #6-32×6 | Hai vít GPU tái dùng từ nắp tháo; tai lá tại X−162, Y153, Z−7,54/12,78 |
| Giá GPU | 2 M3 | Chiều dài và tâm lỗ gá khung chưa chốt trong nguồn |
| Hai hông | 2 vít tay mỗi hông | X−151, Y±150, Z±50; nguồn chưa chốt ren/chiều dài |
| Trước | 2 M3×6 | (162;±159;0), chìm dưới mép cassette |
| Đế đứng | 2 M4×10, trục Y | (±85;−180;0) |
| Chân ngang | Keo, 0 vít | Bốn tâm đã ghi ở phần kích thước |

Đầu vít panel nhô tối đa 2 ngoài bao. Nóc/đáy/lưng cố định và lọc nam châm: 0 vít tháo. Tai riser và gá L phải có lỗ giữ; nguồn chưa chốt ren, chiều dài vít và tâm bắt gá L vào khung, không suy ra từ ảnh. Vít trang trí trên GPU/PSU không thuộc thao tác mở cụm kín.

Thép graphite **#343639**, roughness0,55; lưới/nhựa cổng **#101216**; PCB **#16181D**; kim loại **#AEB4BC**; tiếp điểm **#C6A565**; USB-A xanh **#1F6FD1**; cao su **#202329**; dây **#15171B**. Đường kích thước **#2164AC**; không kính, logo hay ARGB.

## Các hình và điều người mới cần chú ý

Hình lớn tách lớp từ +X/+Y/+Z. FRONT +X nhìn −X thấy I/O trước; LEFT/OPEN +Z nhìn −Z bỏ nắp, phía trước ở phải; TOP +Y nhìn −Y, phía trước xuống dưới và +Z sang phải. BACK −X nhìn +X thấy cổng sau. BEHIND TRAY nhìn từ −Z, phía trước bên trái. HORIZONTAL thể hiện tư thế sau khi tháo đế. Đây là sheet overview với hình lắp và inset theo mục 8.1, không phải sheet sáu mặt của case trống.

1. Bo ITX dưới, GPU trên cùng hướng +Z; cần riser, không cắm card trực tiếp hoặc đặt card phủ CPU. Khoảng giữa mép bo Y20 và GPU Y40 là20.
2. **Không quạt case**. Bốn rotor thuộc CPU/GPU/PSU; khí thoát thụ động qua cửa lưới. Không suy ra hiệu suất nhiệt từ ảnh.
3. Tản mẫu47, giới hạn50 từ IHS; GPU mẫu240×120×45, giới hạn250×120×45; nguồn chỉ SFX sâu100. Không radiator hoặc SFX-L/ATX.
4. Khoảng tới hông trong Z49: tản22,8, GPU18,5, PSU17,25. GPU còn19 tới nóc trong, phải dùng đầu nguồn góc cao≤12.
5. Đứng phải dùng đế chống đổ; nằm ngang phải tháo đế, gắn đủ bốn chân8 và quay mặt hút +Z lên trên.

## Kiểm tra ảnh tạo sinh

Dùng **imagegen tích hợp**, một lượt tạo và một lượt sửa theo brief. Ảnh phục vụ tham chiếu vật liệu và bố trí; còn các sai lệch sau:

- **Ảnh đảo số 6/8:** đúng là 6 GPU và 8 SFX. Ô BOM SFX ghi nhầm240×120×63,5; đúng bao sau quay là **100 X ×125 Y ×63,5 Z**. Nhãn5 ở dây AC phải thuộc10; số7/10 chưa được gắn đủ trên hình lớn.
- Hình lớn vẫn có chi tiết RAM trên bo bên cạnh hai thanh tách rời; BOM chỉ **hai thanh/hai khe**. CPU/tản chưa đồng trục hoàn toàn, các mũi tên RAM chưa chỉ đúng −Z. Dùng tọa độ lắp và hướng ở bảng, không lấy vị trí tách làm mốc.
- Nóc vẫn được vẽ nhấc rời dù thiết kế là cố định. Đường riser ở hình lớn chưa nối rõ cả hai đầu; adapter phải ở dưới GPU, không ở mép trên bo. Sơ đồ tuyến phía sau chỉ minh họa, chưa mô tả đúng toàn bộ đường cong400.
- BEHIND TRAY ghi G6 ở cả hai đầu đáy: phía trái/front phải là **G5**, phía phải/rear mới là **G6**. Nhãn G3 cạnh phải không đúng vị trí; lấy bảng tọa độ G0–G6. Các đường màu không phải màu vỏ dây thực.
- Tiêu đề HORIZONTAL “−Z SIDE PRIMARY” gây nhầm: **−Z ở dưới, +Z hướng lên**. Hình nằm ngang chưa đúng tỷ lệ chiều360/330; các orthographic ghi cùng tỷ lệ nhưng chưa thật sự đồng tỷ lệ. Bốn chân không hiện đủ do khuất.
- Nhãn CPU_FAN ở hàng đầu nối minh họa chưa bảo đảm đúng4 chân; dùng số chân/khóa trong bảng. Lỗ/vít, bộ gá, backplate, cổng và nét lưới nhỏ bị giản lược; không đếm từ ảnh. Không dùng ảnh làm bản lắp độc lập.

Prompt chốt, tóm tắt: “05 SLIM CONSOLE / 00 OVERVIEW; white realistic graphite steel reference sheet, blue dimensions; body100×360×330; exploded assembly viewed +X/+Y/+Z; ITX below, dual-fanGPU above, SFX beside ITX, low-profile47, DDR4×2, M.2×1, riser400 behind tray, insulated AC450, zero case fans; stand and four horizontal feet; FRONT/LEFT OPEN/TOP/BACK, cable routing, keyed connectors and screw schedule; no logos.” Lượt sửa yêu cầu chỉnh RAM, hướng CPU/tản/GPU, nóc cố định, tuyến G/riser/AC, tư thế ngang, số gọi tên và lịch vít; các lỗi còn lại được liệt kê ở trên.
