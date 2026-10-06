# 08 — Nguồn ATX dây liền (`psu`)

Ảnh: [08_psu.png](08_psu.png). Mốc chính: [00_blueprint.md](00_blueprint.md), mục 2–5 và 7. **1 đơn vị cảnh = 100 mm.** Các kích thước chi tiết bổ sung dưới đây là lựa chọn cho mô hình chung.

## Kích thước và hướng đặt

Thân **rộng 150 × cao 86 × sâu 140 mm**, chưa tính dây. Mặt sau và mặt trước đều 150 × 86; đáy 150 × 140. Đặt dưới nắp che nguồn, sát lưng và đáy vỏ. Mặt sau có C14 hướng −X; mặt ra dây hướng +X; quạt hút ở đáy hướng −Y. Không dựng quạt trên nóc, mặt sau hay mặt ra dây.

Quy ước riêng để dựng: mặt sau nhìn từ ngoài có gốc dưới trái, `u` sang phải, `v` lên trên; `d` đi từ mặt sau vào thân, 0…140. Mặt trước dùng gốc dưới trái khi nhìn trực tiếp vào mặt trước, trái/phải đảo so với mặt sau.

## Các phần chính

| Phần | Kích thước và vị trí chọn, mm |
|---|---|
| Vỏ kín | Tôn dày 1; cạnh bo khoảng 1; không mở vỏ trong bài học |
| Lưới tổ ong | Trên mặt sau, vùng u = 12…104, v = 12…74; lỗ lục giác rộng khoảng 4, gân 1 |
| IEC C14 | Viền ngoài 30 ngang × 25 cao, tâm (124; 27); lòng sâu 12; ba lá kim loại gồm một trên giữa, hai dưới |
| Công tắc bập bênh | Viền 19 ngang × 27 cao, tâm (124; 57); ký hiệu I/O; khởi đầu ở O |
| Quạt đáy | Một cụm 120 × 120 × 25, tâm đáy tại u = 75, d = 70; bảy cánh, trục theo chiều cao |
| Lồng bảo vệ quạt | Vòng đồng tâm đường kính ngoài khoảng 120, dây thép đường kính 1,5; bốn nan; gắn cố định |
| Lối ra dây | Một vòng cao su đường kính ngoài 24, lỗ hữu dụng 18; tâm cách mép trái mặt trước 25 và mép đáy 30 |
| Bó dây | Ba nhánh đen ra chung một lỗ; không có ổ modular; uốn bán kính ít nhất 20 |

Vỏ thép sơn nhám `#16181d`; lồng quạt `#252932`; cánh, đầu cắm và công tắc nhựa `#101216`; cao su và dây `#17191c`; tiếp điểm kim loại `#b8bec5`. Chữ I/O trắng `#f2f4f7`. Xanh `#1673df` chỉ dành cho nét chú thích; không thêm đèn hoặc logo.

## Đầu cắm và dây

Kích thước đầu nhựa là kích thước mô phỏng gần đúng, chưa tính ngàm. Mặt cắm có hốc và tiếp điểm cái nằm chìm; chỉ C14 có ba lá đực nhô trong lòng ổ. Dựng ngàm khóa, rãnh ghép, hốc vuông/vát và dấu chân 1. Không dùng hình này làm sơ đồ đấu điện.

| Đầu | Bố trí bắt buộc | Vỏ đầu và dây | Điểm đến |
|---|---|---|---|
| ATX 24 | 2 hàng × 12; ghép 20+4, đường ghép sau cột 10; ngàm trên cạnh dài | Khoảng 52 × 11 × 20; dây 600 | Bo: (117; 40), qua G0 → G2; đoạn cuối lộ 25–35 |
| CPU EPS 8 | 2 × 4; ghép 4+4, đường ghép giữa cột 2 và 3; có ngàm, nhãn CPU | Khoảng 19 × 11 × 20; dây 650 | Bo: (−60; 146), qua G0 → G1; đoạn cuối lộ khoảng 20 |
| PCIe 8 | 2 × 4; ghép 6+2, đường ghép giữa cột 3 và 4; ngàm trên phần 6 chân | Khoảng 19 × 11 × 20; dây 600 | GPU: khoảng (70; 10; 25) toàn máy; qua G0 → sau khay → G0 → ống che → G4; đoạn cuối khoảng 25 |
| IEC C14 | 3 tiếp điểm, nhận đầu C13 của dây điện ngoài | Trên mặt sau; không nối điện ngoài trong ảnh | Chỉ nối sau khi lắp xong |

Sơ đồ đếm hốc, nhìn vào đầu dây; dấu `|` chỉ đường ghép:

```text
ATX 20+4                 EPS 4+4      PCIe 6+2
oooooooooo|oo            oo|oo        ooo|o
oooooooooo|oo            oo|oo        ooo|o
```

Dây liền thuộc cụm PSU; phần 10 hướng dẫn đường đi, không tạo thêm một bộ dây trùng. G0 thuộc khay vỏ, tâm toàn máy X = −35, Y = −170, lỗ 45 × 28. Trong ảnh, dấu gãy dây bỏ qua phần dài; không dựng dây thật chỉ dài bằng đoạn minh họa. Không thêm SATA.

## Bốn lỗ và vít S8

Đúng **4 lỗ ren #6-32 UNC**, trục vuông góc mặt sau. Tọa độ tâm `(u; v)`, mm: **(6; 6), (144; 6), (6; 80), (138; 70)**. Lỗ trên phải lệch vào trong và thấp hơn lỗ trên trái; giữ mẫu bất đối xứng này và làm lỗ vỏ khớp theo.

Dùng **4 vít #6-32 UNC × 6**, đầu tròn chữ thập; chiều dài 6 tính dưới đầu vít. Vít đi từ ngoài lưng vỏ vào nguồn. Các vít nhỏ trên vỏ kín/lồng quạt chỉ là chi tiết đã ráp sẵn, màu mờ, không tạo điểm tháo lắp và không cộng vào S8.

## Năm điểm người mới cần thấy

1. Quạt hướng xuống lọc bụi đáy; gió đi vào nguồn qua lồng quạt.
2. Bốn lỗ sau phải khớp vỏ trước khi bắt đủ bốn vít S8.
3. Ba dây gắn liền với nguồn, luồn qua G0 rồi mới đi tới linh kiện.
4. EPS 4+4 dành cho CPU; PCIe 6+2 dành cho GPU. Ghép đúng phần và ấn đến khi ngàm giữ.
5. Để công tắc O trong lúc lắp; dây C13 ngoài chỉ cắm vào C14 ở bước cuối.

## Kiểm tra ảnh

Ảnh dùng imagegen tích hợp, đã sửa một lượt. Đủ ba góc nhìn, một quạt đáy, C14 ba lá, công tắc, lưới tổ ong, bốn lỗ sau và bốn vít rời. **Ảnh vẫn đặt đường kích thước 150/140 sai cạnh và đầu ATX chỉ thể hiện khoảng 10 cột hốc thay vì 12.** Mốc 25 ở đầu ra dây cũng chưa chỉ đúng tâm vòng cao su. Khi dựng, dùng số và sơ đồ trong tài liệu này; không đo hoặc đếm các chi tiết đó từ ảnh.

<details>
<summary>Lệnh tạo ảnh và sửa ảnh (tiếng Anh)</summary>

Công cụ: built-in `image_gen`; không dùng CLI.

### Lệnh tạo

```text
Use case: infographic-diagram.
Create one high-resolution landscape product reference sheet for a 3D modeller. Title "08 / PSU" and subtitle "ATX / FIXED CABLES / mm". White-light grey background, realistic charcoal powder-coated steel #16181d, black plastic/rubber #101216, soft studio lighting, thin blue dimension lines, concise readable English numbered callouts. Brand-free, no logos, stickers or watermark.

Layout: ONE large rear three-quarter isometric hero occupying left 60%, TWO smaller secondary panels stacked on right: "BOTTOM / INTAKE" and "FRONT / FIXED CABLES". Keep plenty of whitespace, all features fully visible.
PSU body exactly 150 mm wide x 86 mm tall x 140 mm deep. Hero clearly shows REAR 150x86 panel and solid top and side. Rear toward viewer-left, front opposite far end. Show 150 W, 86 H, 140 D dimensions following correct edges. Rear has honeycomb exhaust vent over left two thirds, recessed IEC C14 MALE inlet with THREE visible metal blades on right lower area, rocker switch marked I / O above inlet, O depressed. No fan on rear or side or top. Callouts "1 C14 / 3 blades", "2 I / O", "3 Honeycomb".
Exactly FOUR empty threaded mounting holes on rear panel with thin leaders; coordinates measured from lower left of rear face viewed straight from outside: (6,6), (144,6), (6,80), (138,70) mm. Crucial upper right hole slightly inset and lower than upper left. Callout "4 S8 / 4 holes". Show exactly FOUR separate Phillips pan-head mounting screws nearby in a neat row labelled "4 x #6-32 UNC x 6". No additional loose screws, no extra rear mounting holes.

Upper-right BOTTOM panel: same PSU viewed square-on from below, 150 wide x 140 deep bottom surface with exactly ONE 120 mm intake fan behind black concentric wire grille and four supporting spokes. Fan centered, seven dark blades discernible. Small three-quarter depth if helpful. Caption "5 Fan DOWN / intake", small airflow arrow from outside INTO grille. This is underside, never top. Grille is factory-fixed, no additional user fasteners depicted.

Lower-right FRONT panel: show opposite 150x86 face, solid metal with ONE rubber strain-relief opening toward lower-left (25 mm from left edge, 30 mm above bottom when viewing front). Show THREE permanently attached tidy black harness branches leaving that single opening, short displayed tails with zigzag break marks indicating omitted cable length. Label "6 Fixed exit" and blue schematic arrow "TO G0" toward a ghost outline grommet with caption "CASE". G0 belongs to case, not a PSU port.
At ends show exactly THREE enlarged cable connectors, faces toward camera and latches visible, NO modular sockets on PSU:
"7 ATX 24 / 20+4" has exactly TWO rows of TWELVE female cavities, visible seam after ten columns, locking latch on long side.
"8 CPU EPS 8 / 4+4" exactly TWO rows of FOUR female cavities, middle seam dividing two 2x2 blocks, latching tabs.
"9 PCIe 8 / 6+2" exactly TWO rows of FOUR female cavities, seam after three columns dividing 2x3 and 2x1 blocks, main locking latch on six-pin body.
Individual cavities show square and chamfered keying; recessed metal female contacts, no projecting male pins. Distinguish CPU 4+4 and GPU 6+2. Connector housings realistic, enough enlargement to count cavities. Only last 20-40 mm cable near connectors shown; no long cable loops. Small caption "Cable lengths omitted".
Do not add SATA, RGB, additional fans, extra connectors, a rear fan or modular ports. Product reference illustration, not an electrical wiring diagram.
```

### Lệnh sửa đã dùng cho ảnh cuối

```text
Edit this PSU reference sheet. Preserve layout, materials, readable callouts, four empty rear mounting holes and four loose screws, bottom fan view, and front fixed-wire view. Correct these precise engineering errors:
1. Hero dimensions swapped: rear panel containing honeycomb, IEC inlet and rocker is 150 mm wide. The long top edge above this REAR panel slopes from upper-left at screen x30 y240 toward screen x650 y330: label this edge 150 mm. The receding SIDE depth is 140 mm along edge from screen x650 y330 toward screen x970 y190. Remove both existing top-edge dimension lines and redraw dimensions along these correct edges with their labels. Keep height 86 mm. Rear panel 150 W x 86 H, body depth140.
2. ATX connector at bottom has insufficient columns. Replace ONLY its connector with a wider clear female connector face, EXACTLY 12 columns by 2 rows = 24 individual cavities. Every cavity must be visible and countable. Seam between columns 10 and 11 for 20+4 split. Label unchanged. Show latch on long side.
3. CPU EPS plug EXACTLY 4 columns by 2 rows = 8 cavities, seam BETWEEN columns 2 and3, two joined 2x2 blocks and proper latches. GPU PCIe plug EXACTLY 4 columns by2 rows, seam BETWEEN columns3 and4 for 6+2 split, main latch on 6-pin housing. Keep cables connected to their correct connectors.
4. C14 inlet has EXACTLY THREE metal blades: one top-center protective-earth blade and two lower left/right blades, no fourth blade or metallic ghost. Crisp realistic chamfered inlet cavity.
5. Rocker callout 2 leader must end ONLY on switch, not on upper-right mounting hole. Mounting-hole callout4 leaders identify exactly four holes.
6. Front grommet 25mm horizontal dimension must reach center of grommet, from left front-face edge. Its center is30mm above bottom. Keep lower-right panel and cable breaks.
All else unchanged. No new views or extra parts.
```

</details>

