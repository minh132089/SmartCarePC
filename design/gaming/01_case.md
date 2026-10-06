# 01 / case — Vỏ máy mid-tower

Tham chiếu: [bản tổng thể](00_blueprint.md), [brief](brief_parts.md), [ảnh vỏ](01_case.png). Chỉ dựng vỏ rỗng cùng các chi tiết gắn trên vỏ; không có bo, nguồn, GPU, quạt hoặc két. Số đo dưới đây là mốc dựng hình, không đo ngược từ ảnh tạo sinh. Đây là thiết kế mô phỏng, không phải bản vẽ gia công.

## Kích thước và vật liệu

- Vỏ: **rộng 220 × cao 470 × sâu 450 mm**; tôn khung dày 1. Quy đổi **100 mm = 1 đơn vị cảnh**.
- Gốc ở tâm vỏ. X từ lưng (−225) tới trước (+225); Y từ đáy (−235) tới nóc (+235); Z từ nắp kín (−110) tới kính (+110). Nhìn từ kính: lưng bên trái, trước bên phải. Nhìn sau khay thì trái/phải đảo lại.
- Khung, khay, nắp kín và nắp che nguồn: thép sơn nhám `#16181D`; mặt lưới trước `#101216`; điểm nhấn `#22D3EE`, không phải đèn RGB.
- Kính: **430 sâu × 445 cao × 4 dày**, trong hơi xanh `#DDECF2`, thấy được khoang máy. Cao su grommet, đệm kính và chân đế: `#202329`. Vít thép `#929AA5`; chân đế màu đồng `#B89451`.
- Nắp kín là tôn đặc, không có lưới. Chọn tấm 430 × 445 × 1 để dựng cùng khung hông; đây là kích thước bổ sung cho mô hình.
- Mặt đỡ khay ở **Z = −102,5**. Chân đế cao **6,5** đưa lưng bo tới Z = −96. Khoảng 14 mm từ vách kín tới lưng bo không phải chiều cao chân đế.
- Cửa bảo trì CPU: **100 × 100**, tâm **X = −69; Y = 121,5** trên khay. Không dùng cửa này để luồn dây.
- Nắp che nguồn cố định: mặt trên **Y = −120**, dừng ở **X = 170** để không chặn quạt trước dưới. Nóc giữ nguyên trên khung; mặt trước dùng ngàm; lọc bụi đáy dùng nam châm.

## G0–G6 và mặt sau khay

Tọa độ theo toàn máy; kích thước là phần mở hữu dụng. Mỗi miệng có viền cao su rộng 3 mm, góc bo tròn. Các lỗ trên khay dùng kích thước X × Y.

| Mã | Tâm / vị trí (mm) | Phần mở | Công dụng |
|---|---|---|---|
| G0 | X −35; Y −170, trong khoang nguồn | 45 × 28 | Đưa dây nguồn ra sau khay |
| G1 | X −148; Y 224, trên mép bo phía sau | 32 × 10 | Dây EPS CPU |
| G2 | X 57; Y 110, trước mép bo | 22 × 55 | ATX 24 và quạt trước trên |
| G3 | X −65; Y 224, trên mép bo | 40 × 10 | Bơm và quạt két |
| G4 | **X 95; Y 10; Z 25**, miệng hướng **−X** | **18 × 24 theo Z × Y** | Đầu ra của ống che dây GPU |
| G5 | X 23; Y −104, dưới góc trước bo | 48 × 16 | USB, nút/đèn và hai quạt trước dưới |
| G6 | X −184; Y −104, dưới góc sau bo | 42 × 16 | HD_AUDIO và quạt sau |

**G4 phải là ống kín gắn liền nắp che nguồn**, tiết diện trong 24 × 28. Ống đi từ khoang nguồn lên miệng Y = 10, chênh cao tâm khoảng 130 so với nắp che. Miệng cách ổ GPU dự kiến khoảng 25 mm. Chừa đường chuyển tiếp kín từ sau khay qua G0 vào ống; không dựng dây trần chạy lên từ nắp che. Ống không chạm vùng quạt trước. G4 không phải một lỗ bổ sung trên mặt phẳng khay.

Mặt sau khay có rãnh mép rộng 25, sâu hữu dụng 10 ở vùng khung/lõm, không tăng toàn bộ khoảng sau khay. Có **6 đai dán**: 2 phía sau, 2 phía trước, 2 mép dưới; có túi lõm đặt đầu chia quạt. Khi có dây ở bước sau, xếp một lớp, bán kính uốn ít nhất 20. Ảnh phần vỏ để các đường này trống. Hình nhỏ sau khay phải giữ đúng mã G dù hướng nhìn đảo lại.

## Vị trí gá và vít

| Chi tiết | Số lượng / loại | Vị trí và cách dựng |
|---|---|---|
| Chân đế bo | **9**, lục giác cao 6,5; ren ngoài và trong M3 | Tất cả tổ hợp **X = −200; −88; 24** và **Y = −80; 62,5; 205**, trên mặt khay Z = −102,5 |
| Vít bo | **9 × M3 × 5** | Mỗi chân đế nhận một vít từ phía kính; khi vỏ rỗng, vít chưa siết vào bo |
| Quạt trước | **12 lỗ**, dự kiến 12 vít quạt **5 × 10** | Ba vùng 120 × 120, tâm Y = −85; 40; 165, Z = 0. Mỗi vùng 4 lỗ lệch ±52,5 theo Y và Z; quạt sau này chiếm X = 197…222 |
| Quạt sau | **4 lỗ**, dự kiến 4 vít quạt **5 × 10** | Tâm Y = 145; Z = 14, mặt lưng X ≈ −225. Bốn lỗ lệch ±52,5 theo Y và Z |
| Giá két nóc | **8 lỗ**, dự kiến 8 vít **M3 × 6** + 8 đệm dày 1 | Hai tâm X = −61,5 và 58,5; Z = 36. Mỗi tâm có 4 lỗ lệch ±52,5 theo X và Z; vít từ nóc xuống |
| Nắp PCIe | **7 nắp + 7 vít #6-32 UNC × 6** | Xếp ở lưng, bước cao **20,32**. Mỗi nắp một tai và một vít; khi lắp GPU, tháo 2 nắp và dùng lại 2 vít, không cộng thêm vít GPU |
| Gá nguồn | **4 lỗ**, dự kiến 4 vít **#6-32 UNC × 6** | Theo mặt sau nguồn 150 ngang × 86 cao. Nhìn từ ngoài lưng, gốc ở góc dưới trái mặt nguồn: **(6;6), (144;6), (6;80), (138;70)**. Giữ tai tôn quanh từng lỗ; không đặt lỗ giữa khoảng mở |
| Kính | **4 vít tay #6-32 UNC × 8 + 4 đệm cao su** | Bốn góc, tâm lỗ cách mỗi mép 15; đầu kim loại không tì trực tiếp vào kính |
| Nắp kín | **2 vít tay #6-32 UNC × 6 giữ liền nắp** | Mép sau, cách mép trên và dưới 20; nắp trượt vào rãnh trước khi siết |

Lưới 9 chân đế là mẫu mô phỏng đã chọn, không phải khẳng định mẫu khoan ATX chuẩn. Lỗ tương ứng trên bo ở phần 02 có đường kính 3,4 và vùng trống bán kính 4. Không khoan lỗ 3,4 xuyên thân chân đế M3.

Chừa cửa I/O bo ở lưng, vùng PCIe bên dưới và cửa nguồn sát đáy dưới nắp che. Cửa nguồn để trống; ổ C14 và công tắc thuộc phần nguồn, không thuộc vỏ. Vùng gá nguồn khớp nguồn 150 × 86 × 140. Két sau này nằm **dưới nóc**, trong X = −140…137, Y = 179…231, Z = −24…96; giá nóc lệch về kính. Chỉ thể hiện giá, không lắp két hoặc quạt trong ảnh này.

## Cụm I/O trên mép trước nóc

| Chi tiết | Tiếp điểm / đầu dây | Hình dạng và vị trí |
|---|---|---|
| Nút nguồn | Công tắc 2 tiếp điểm, đầu dây **PWR SW 2 vị trí** | Một nút tròn trên mép trước nóc, dễ ấn |
| USB-A 3.0 | **2 cổng, mỗi cổng 9 tiếp điểm**; chung đầu nội bộ **19 chân / 20 vị trí**, một vị trí khóa | Hai miệng chữ nhật có vỏ kim loại và lưỡi nhựa xanh; không dựng thành hốc trơn |
| Audio 3,5 mm | **1 jack combo TRRS, 4 tiếp điểm**; cáp **HD_AUDIO 9 chân / 10 vị trí**, khóa vị trí 8 | Một miệng tròn cạnh hai cổng USB; có vòng cách điện |

Theo yêu cầu riêng của phần 01, mặt ngoài dùng **một jack audio**, thay cho hai jack trong bản tổng thể. Chọn jack combo cho mô hình này. Cụm được thể hiện gồm nút nguồn, hai USB và một jack; không thêm nút hoặc cổng ngoài phạm vi yêu cầu. Các đầu dây nội bộ được mô tả để nối ở phần 10, không trải dây ra trong ảnh vỏ. USB/PWR SW ra G5, HD_AUDIO ra G6 theo đường sau khay. Không thêm đầu SATA hoặc RGB.

## Người mới cần nhận ra

1. Tháo cả kính và nắp kín để tiếp cận hai mặt khay; giữ đủ 4 vít kính và 2 vít nắp kín.
2. Bo phải nằm trên đúng **9 chân đế**, không chạm trực tiếp khay và không có chân đế thừa dưới PCB.
3. **G1 và G3 nằm trên bo; G4 là ống che dựng lên từ nắp nguồn.** Không kéo dây qua cửa bảo trì CPU.
4. Vỏ rỗng có **7 nắp PCIe**. Khi lắp GPU, chỉ tháo 2 nắp, giữ lại vít và chừa khoảng cho thân GPU dày 55.
5. Phân biệt 3 giá quạt trước, 1 giá quạt sau và giá két dưới nóc; cửa nguồn có 4 lỗ bất đối xứng, không phải lưới quạt.

## Tạo ảnh

Dùng công cụ imagegen tích hợp. Prompt cuối và ghi chú kiểm tra ảnh được lưu ở cuối tệp để có thể tạo lại.

<details>
<summary>Prompt tạo ảnh (tiếng Anh)</summary>

```text
Use case: product-mockup / scientific-educational.
Create ONE high-resolution landscape product reference sheet for a 3D modeller, title "01 / CASE", subtitle "Dimensions in mm". Very light grey-white background, soft studio lighting, realistic charcoal powder-coated steel #16181d, front mesh #101216, restrained cyan #22d3ee, transparent pale glass. Thin blue dimension lines, numbered callouts with SHORT legible English labels. Brand-free: absolutely no logos, brands, trademarks, stickers, fake text or watermark. Crisp detailed hardware, generous whitespace.

SUBJECT: ONLY ONE empty generic mid-tower case, 220 W x 470 H x 450 D. No motherboard, CPU, RAM, SSD, GPU, PSU, fans, radiator, cooler or cable kit installed. The fan and radiator MOUNTS are empty. Permanent frame, motherboard tray, PSU shroud and G4 covered riser remain installed. Roof attached.

COMPOSITION: large exploded 3/4 isometric hero occupies left 70%, seen through open glass side, REAR at LEFT and FRONT at RIGHT. Show front mesh, top mesh/mounts and inside tray clearly. Detached tempered glass panel 430 D x 445 H x 4 thick floats outward towards viewer, placed low-left without obscuring chassis. Exactly four glass corner holes, four rubber washers and four thumb screws, one at each corner. Solid opaque steel cable-side panel floats behind/above tray, shows smooth solid exterior, no perforations, with TWO captive thumb screws on rear edge. Front mesh unclipped slightly forward to reveal three vertically stacked empty 120 mm fan mounting bays: no blades or hubs. Two small framed secondary views on RIGHT: "CABLE SIDE" reversed view of bare tray, and "REAR" straight rear elevation. Rear detail shows every one of seven slot covers, rear fan mount, empty motherboard I/O cutout and empty PSU opening. Bottom narrow fastener strip shows representative screw types with quantity labels instead of piles.

GEOMETRY / COORDINATES to keep visually consistent: global X rear -225 to front +225, Y bottom -235 to roof +235, Z solid side -110 to glass +110. Tray support plane Z=-102.5, nine hex standoffs 6.5 tall reaching Z=-96. EXACTLY NINE brass standoffs in a 3 by 3 grid on the tray, X=-200,-88,24 and Y=-80,62.5,205. All nine clearly visible in hero, small leaders "9 x 6.5 standoffs" and "M3". Empty square CPU service cutout 100x100 centered X=-69,Y=121.5; does not remove or overlap any standoff. PSU shroud top Y=-120, terminates X=170 before front intake. Bottom magnetic dust filter. Raised feet.
G4 is a narrow permanent enclosed rectangular riser rising from PSU shroud near front end of eventual GPU: outlet center X=95,Y=10,Z=25, faces rear (-X), open mouth 18x24. Internal riser passage 24x28, roughly 130 tall from shroud to outlet, charcoal steel. Must look like a closed cable duct with a rubber mouth, not a loose wire or an open bracket. Label "G4 / covered riser". Keep clear of front fan bays.

GROMMETS, black rubber lips, label exactly G0 through G6 with blue leaders to actual openings, never random floating labels:
G0 45x28 at X=-35,Y=-170, hidden in PSU chamber on tray; visible in cable-side inset.
G1 32x10 at X=-148,Y=224, top rear above eventual motherboard.
G2 22x55 at X=57,Y=110, vertical opening in front of board region.
G3 40x10 at X=-65,Y=224, separate top opening above board.
G4 at raised covered shroud riser outlet, NOT a hole on flat tray.
G5 48x16 at X=23,Y=-104, below front bottom corner of board.
G6 42x16 at X=-184,Y=-104, below rear bottom corner of board.
All edges rubber lined, no duplicate phantom grommets. Cable-side inset shows SIX tray grommets G0,G1,G2,G3,G5,G6, and a dashed outline leader locating G4 on opposite face. Include CPU service cutout, six empty hook-and-loop straps: two each front channel, rear channel, lower channel. Channels 25 wide. No routed electrical cables or components in this empty case sheet.

MOUNTS:
Front: three empty 120x120 mounting bays at Y=-85,40,165; Z=0. Exactly four hole locations per bay, square pitch 105x105, twelve total. Mesh in front, rails behind. Callout "3 x 120 / 12 holes".
Rear: one EMPTY 120 fan grille with 4 corner screw holes, 105x105 pitch, center Y=145,Z=14. No fan blades. Callout "Rear 120 / 4 holes".
Top: attached vented roof with TWO 120 radiator hole patterns, total EIGHT short-screw holes in rails, for a 240 radiator mounted BELOW roof, offset toward glass. Pattern centers X=-61.5,58.5,Z=36, each +/-52.5 in X and Z. Callout "240 mount / 8 holes". Do not depict the radiator or any fans.
Rear expansion area: EXACTLY SEVEN individual horizontal perforated PCIe slot covers stacked vertically, each with ONE retention screw at same end. All seven are installed. Pitch 20.32. Callout "7 covers / 7 screws".
PSU: empty rear PSU mounting aperture at bottom with EXACTLY FOUR screw holes around flange. Callout "PSU / 4 holes". For a 150x86 PSU, asymmetrical mounting hole centers from lower-left rear face are (6,6),(144,6),(6,80),(138,70). Hole centers on flange steel, not floating inside open aperture. No PSU, no socket or switch in empty opening.
Glass: four thumb screws #6-32 x8 plus four rubber pads.
Solid panel: two captive thumbs #6-32 x6.
Board: nine standoffs M3 male/female 6.5 high, nine M3x5 board screws represented in fastener strip, board absent.
Front I/O cluster built into TOP FRONT edge and clearly visible: ONE round power button, TWO rectangular USB-A 3.0 ports with blue tongues (9 contacts each), ONE round 3.5 mm audio jack. Short label "Power / 2 x USB 3.0 / Audio". Do not add logos or extra audio jack.

BOTTOM fastener strip with realistic representative hardware and short labels:
"Board: 9 x M3x5"; "Roof: 8 x M3x6 + washers"; "Fans: 16 x 5x10"; "PCIe: 7 x 6-32x6"; "PSU: 4 x 6-32x6"; "Glass: 4 x thumb + pads"; "Panel: 2 x captive thumb".
Dimension arrows 470 height, 450 depth, 220 width around main chassis, not exploded panels. Primary priority: accurate empty case, exactly 9 standoffs, exactly 7 slot covers, all G0-G6, covered raised G4 duct, two detached side panels, clearly distinguish openings from installed hardware. No extra components. Whole sheet uncropped.
```

</details>

<details>
<summary>Prompt sửa một lần (tiếng Anh)</summary>

```text
Edit this case reference sheet once, preserving overall layout, realistic charcoal materials, blue dimension lines, 220 W x 470 H x 450 D, exploded glass and SOLID side panel, two right-hand insets, seven PCIe covers, empty mounts and bottom fastener strip. Case remains entirely empty with no installed PC parts.

Fix these specific visible errors:
1. FRONT I/O on top front rail: exactly ONE round power button total, exactly TWO USB-A 3.0 rectangular ports with blue inner tongues and visible contact structure, and exactly ONE small black hollow 3.5mm audio jack. Current two large round controls are wrong. Replace the extra large round control closest to the viewer with a small, clearly hollow 3.5mm jack; remove the tiny intervening extra control. Keep the rearward round button as sole power button. Existing label remains "Power / 2 x USB 3.0 / Audio".
2. TRAY STANDOFFS: exactly NINE brass hex standoffs, arranged in THREE columns and THREE rows. Nine only. No extra brass dots, no brass service-cutout screws. Perspective grid positions relative to eventual board envelope: left/middle/right columns, top/middle/bottom rows. Top row entirely above the square CPU service opening, middle row below it, bottom row just above G5/G6. Every standoff has a clear small central threaded hole. Remove stray extra post halfway between middle and lower rows on the rear/left side. Label remains "9 x 6.5 standoffs / M3".
3. CABLE SIDE INSET: REMOVE every black electrical cable and connector, this is the EMPTY CASE. Six empty short black hook-and-loop straps are placed TWO in left vertical perimeter channel, TWO in right vertical perimeter channel, TWO horizontally along bottom channel; not all six in one vertical column. Leaders should point correctly, two per channel. Keep G0/G1/G2/G3/G5/G6 positions as previously shown, G4 dashed opposite-face locator, and CPU maintenance cutout. Change heading to "CABLE SIDE" (remove misleading "Inside view"). From this opposite face FRONT is LEFT and REAR is RIGHT: G2 front left, G5 bottom left, G6 bottom right, G3 top left of G1. All open holes have rubber lips.
4. Keep exactly SEVEN PCIe slot covers and seven retention screws in both main and rear views. Keep exactly FOUR clearly visible screw holes on solid tabs around the EMPTY rear PSU aperture, with the upper right slightly lower to show the asymmetric mounting pattern. Do not install PSU.
5. G4 remains enclosed raised duct fixed to PSU shroud, not a cable. All dimensions, detached four-hole glass with four screws and pads, solid panel with two captive screws, three empty front fan bays, top eight radiator mounting points and rear four fan mounting points must remain. Do not add any other hardware, fans or cables.
```

</details>

### Kiểm tra ảnh bàn giao

Đã kiểm tra ảnh đầu và sửa một lần theo brief. Ảnh cuối thể hiện vỏ rỗng, hai nắp tách rời, G0–G6, ống G4, 7 nắp PCIe, các giá quạt/két, cửa nguồn và cụm I/O. Không có logo hoặc linh kiện máy lắp bên trong.

**Điểm thợ dựng phải sửa theo số liệu:** vị trí các chân đế trong ảnh chưa thành lưới 3 × 3 chuẩn như bảng; sáu đai trong hình nhỏ vẫn dồn về một mép thay vì chia 2 trước / 2 sau / 2 dưới. Tai vít trên phải của cửa nguồn và vị trí G0 cũng chỉ mang tính minh họa. Dựng các chi tiết này bằng tọa độ/bảng trong tài liệu, không sao chép vị trí từ ảnh. Không dùng nét lưới hoặc lỗ phụ trên khung để suy ra thêm điểm tương tác.
