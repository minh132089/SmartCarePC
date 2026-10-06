# 03 / CPU — bộ xử lý LGA chung

Ảnh: [03_cpu.png](03_cpu.png). Theo [00_blueprint.md](00_blueprint.md), mục 1–4 và dòng `cpu` ở mục 7. Dùng số đo dưới đây để dựng; không đo theo ảnh tạo sinh. **1 đơn vị cảnh = 100 mm.**

## Kích thước và vị trí

- CPU: **37,5 × 37,5 × 4,5 mm**. Vùng bao socket khi đóng: **50 × 50 mm**.
- Tâm CPU trùng tâm socket: **(19; 59)** theo X/Y cục bộ bo; tương ứng X/Y toàn máy **(−69; 121,5)**. Mặt linh kiện của bo ở Z toàn máy −93.
- Trục chi tiết song song trục bo: X/Y nằm trên mặt CPU; +Z hướng từ socket ra mặt tản nhiệt. Trong hình, CPU nhấc khoảng 8 mm theo +Z để thấy chỗ lắp; đây chỉ là khoảng tách minh họa.
- Các kích thước nhỏ dưới đây là lựa chọn dựng hình cho CPU chung, không phải thông số của một socket thương mại.

## Các phần cần dựng

Lấy tâm CPU làm gốc X/Y, mặt dưới CPU làm Z = 0.

| Phần | Kích thước và chỗ đặt | Vật liệu / màu |
|---|---|---|
| Đế CPU | 37,5 × 37,5 × 1,2; Z = 0…1,2 | Nền mạch xanh đậm `#21483D`, hơi nhám |
| Nắp tản nhiệt | 32 × 32 × 3,3; ở giữa, Z = 1,2…4,5; bo cạnh khoảng 0,5 | Kim loại bạc `#C8CDD2`, xước mịn; mặt trên trơn |
| Dấu tam giác | Tam giác cạnh khoảng 1,5 tại góc X−/Y− của viền đế, trên mặt trên; dấu socket ở góc tương ứng | Vàng `#D6AC50` |
| Hai rãnh định hướng | Đúng 2 rãnh chữ U: một ở cạnh X = −18,75, một ở X = +18,75; cùng tâm Y = +5. Miệng rộng 2 theo Y, sâu 1,2 vào đế; cắt xuyên đế | Cùng vật liệu đế, có hai gờ socket khớp rãnh |
| Tiếp điểm mặt dưới | 1.200 pad vuông cạnh 0,55; bước tâm 0,75. Lưới 40 × 40, bỏ ô vuông 20 × 20 ở giữa: 1.600 − 400 = 1.200. Tâm hàng/cột từ −14,625 đến +14,625 | Vàng `#CBA34D`, phẳng sát mặt Z = 0; không có chân nhô |
| Thân socket tham chiếu | Bao ngoài 50 × 50, cao chọn 3; lòng nhận CPU khoảng 37,7 × 37,7; tâm trùng CPU | Nhựa đen `#20242A`; các lá tiếp xúc nhỏ nằm trong socket |
| Khung ép, bản lề, cần | Khung ép khoảng 44 × 44, tấm dày 0,8, cửa giữa khoảng 30 × 30; bản lề phía Y+. Cần tròn đường kính 1,8, dài hữu dụng khoảng 45 dọc cạnh X+; móc giữ ở đầu Y− | Thép `#9AA2AB`, phản xạ nhẹ |

Lưới pad là quy ước hình học của mô hình, không mô phỏng sơ đồ tín hiệu thật. Có thể dùng các bản sao dùng chung mesh để giữ đúng số lượng. Khi vẽ mặt dưới, lật cả CPU để giữ đúng vị trí rãnh và góc tam giác; không tự đổi góc dấu. Socket, khung ép và cần là phần của bo mạch số 02, chỉ hiện ở đây để hướng dẫn lắp, không tạo thêm socket trong tài sản CPU.

## Đầu nối và vít

- **LGA CPU ↔ socket:** 1.200 tiếp điểm phẳng trên mặt dưới CPU, tiếp xúc với lá đàn hồi trong socket. Hai rãnh và tam giác xác định hướng đặt.
- CPU không có đầu cắm dây, không có cáp nguồn riêng.
- **Lỗ vít trên CPU: 0. Vít lắp CPU: 0.** Khung ép và cần khóa giữ CPU. Bốn vít lò xo của bộ gá bơm thuộc phần 04, không thêm vào phần này.

## Chuyển động và điểm người mới cần thấy

1. **Tam giác:** góc đánh dấu của CPU phải trùng góc đánh dấu trên socket.
2. **Hai rãnh:** cả hai khớp gờ socket trước khi hạ CPU thẳng xuống. Không xoay hoặc trượt CPU trên các lá tiếp xúc.
3. **Mặt dưới phẳng:** pad vàng hướng vào socket; nắp bạc hướng ra phía tản nhiệt. Cầm vào cạnh CPU.
4. **Đóng đúng thứ tự:** hạ khung ép trước; xoay cần xuống sát bo, đưa đầu cần vào dưới móc giữ. Mũi tên cong trong ảnh chỉ hướng đóng.
5. **Không bắt vít CPU:** CPU nằm đúng chỗ bằng dấu và rãnh; khung ép cùng cần mới giữ nó lại.

## Lệnh tạo ảnh

Công cụ: imagegen tích hợp, tạo mới rồi sửa một lần. Ảnh dùng nhãn tiếng Anh ngắn; tài liệu này là mốc định lượng cho người dựng.

```text
Use case: scientific-educational.
Create ONE landscape high-resolution product reference sheet for a 3D modeller, generic LGA CPU for a beginner PC assembly simulator. White / very light grey background, realistic brushed metal and dark green substrate, soft studio lighting, crisp thin blue dimension lines, navy short English labels, numbered callouts and generous whitespace. Brand-free, no logos, no product names, no watermark, no fake etched writing.
Title at upper left: "03 / CPU". Subtitle: "Generic LGA • Dimensions in mm".
COMPOSITION: one large 3/4 isometric hero taking left two thirds; one small clearly framed underside view at upper right; one small side profile below the underside. Bottom margin short assembly instruction strip.
HERO: a single square CPU hovering only about 8 mm directly above and aligned with the empty open LGA socket on a minimal cropped dark motherboard patch. CPU package 37.5 x 37.5 mm, total thickness 4.5 mm. Plain smooth silver heat spreader centered on a thin dark green substrate, thin substrate rim visible. One small gold orientation triangle on the substrate corner nearest the front-left; corresponding gold triangle directly below on same corner of socket. Exactly TWO small U-shaped orientation notches cut into opposite substrate side edges, both offset toward the same end, matched by two socket keys. Make both notches legible, use underside view for full clarity. No corner cuts pretending to be orientation notches. CPU has no projecting pins.
Socket outer footprint 50 x 50 mm with dark polymer housing, fine gold spring contacts INSIDE socket only, hinged silver load plate raised behind CPU so it does not overlap the floating CPU. A steel retention lever along the right side is raised open. A clear blue curved arrow follows the actual lever pivot DOWN toward its retaining hook; label "5 Close lever". Ghosted closed lever position beside right socket edge, label "Down + under hook". Load plate closure is first; label "4 Lower plate" with separate small curved arrow toward socket. Subtle dashed vertical alignment guides and a downward arrow "Lower gently" show CPU placement, no sideways slide or forced push. CPU itself absolutely no screws and no screw holes. No loose screws and no cooler mounting kit.
Callouts with precise leaders: "1 Triangle" pointing to actual gold corner triangle; "2 Key notches ×2" branching to the two notches (use underside if clearer); "3 Flat gold pads" pointing to underside contacts; "4 Lower plate"; "5 Close lever". No duplicated numbered labels.
DIMENSIONS: hero shows two perpendicular CPU footprint dimensions each "37.5", and socket footprint marked "50 × 50". Side profile shows a complete CPU package with vertical dimension "4.5", substrate below silver lid, no bottom spikes.
UNDERSIDE inset: the SAME CPU viewed straight-on from below, clearly titled "UNDERSIDE". Dark green substrate, exactly two opposing side U-notches visibly offset from the midline toward the same end, mirrored appropriately relative to top view. Dense orderly flat square gold contact pads arranged as a square ring around a central bare green region. For modelling convention use 40 x 40 grid with central 20 x 20 omitted = 1200 contacts; do not draw pin needles, pins, balls, legs, or protrusions. Thin flat pads sit flush on surface. A small triangle locates the corresponding physical package corner. Label below "1,200 flat contacts" and smaller "Model convention". The inset represents a second view, not a second physical CPU to install.
BOTTOM STRIP exact short text: "Align triangle + notches → Lower CPU → Lower plate → Lock lever". Add "CPU screws: 0" at bottom right. Keep all English labels readable and brief, no decorative circuitry, no unrelated RAM, GPU, cooling parts, cables or tools. The final image must be a polished realistic modelling reference, not a cartoon or wireframe blueprint.
```


Lệnh sửa lần cuối:

```text
Edit this CPU reference sheet, keeping the layout, realistic materials, white background, typography, dimensions, underside inset and assembly instruction strip.
Make these focused mechanical corrections:
1. The raised silver socket load plate behind the CPU must be an OPEN-CENTER retention frame with a large square hole through its middle, not a solid lid. White background must be visible through the hole. Retain its perimeter metal, hinge and "4 Lower plate" arrow.
2. The hero "1 Triangle" leader currently points to a side notch. Move this leader to the GOLD TRIANGLE at the frontmost CPU substrate corner. It must unambiguously end on the triangle. Keep the socket triangle directly below at the matching corner. Two opposite side notches remain correctly labelled "2 Key notches ×2".
3. CPU SIDE PROFILE must have a FLAT flush underside under the thin green substrate, no broad protruding silver block underneath. Overall height remains 4.5 from bottom of substrate to top of silver lid.
4. Reduce CPU hovering gap slightly so it reads as just above its socket, keeping socket contact area visible and the two corresponding front triangle corners vertically aligned.
5. Keep the socket lever pivot at the back end of the right edge, with its long arm able to lie down along the socket right side to a front retaining hook. Curved closure arrow shows lowering to the socket edge. No screws on CPU, no CPU screw holes, no new objects.
Preserve all other sheet content. Flat gold CPU pads only, no protruding pins.
```
