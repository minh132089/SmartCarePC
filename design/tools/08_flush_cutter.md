# 08 — Kìm cắt sát dây rút

Ảnh: [08_flush_cutter.png](08_flush_cutter.png). Một kìm nhỏ dùng ở bước 7 của danh sách dụng cụ, tương ứng bước 23 trong blueprint Gaming: gom dây sau khay bo rồi cắt đuôi dây rút. Các số dưới đây là kích thước thiết kế cho mô hình, **là nguồn chuẩn khi dựng**, không phải số đo sản phẩm thương mại.

## Kích thước và hệ trục

Bao khi bóp khép lưỡi: **125 × 15 × 46 mm theo X/Y/Z**. Gốc O ở giữa hai đầu cán, trên mặt phẳng Y=0; +X từ đuôi cán tới mũi kìm, +Y theo chiều dày về mặt có vát lưỡi, +Z ngang về một bên cán. Biên X=0…125, Y=±7,5, Z=±23. Mặt cắt sát hướng −Y.

TOP/BOTTOM nhìn từ +Y/−Y; FRONT/BACK từ +Z/−Z; LEFT/RIGHT từ −X/+X. Sáu hình chiếu cùng tư thế khép. Góc 3/4 và ô thao tác chỉ minh họa. Bao 130 × 55 × 15 của sheet tổng quan là kích thước bày sơ bộ; dùng **125 × 46 × 15** của sheet này cho kìm.

## Bộ phận

Tọa độ ghi theo (X;Y;Z), đơn vị mm. Có **1 kìm, gồm 6 chi tiết**: 2 nửa thép liền cán–mỏ, 2 vỏ cao su, 1 chốt tán và 1 lò xo lá. Mỏ và lõi cán cùng một nửa thép, không cộng thành chi tiết rời.

| Số trên ảnh / bộ phận | Số lượng | Vị trí và hình học | Vật liệu, màu |
|---|---:|---|---|
| 1 — Hai nửa mỏ và lõi cán | 2 | Lõi cán X=4…88, tiết diện khoảng 6 × 3 theo Z/Y; nối qua khớp tại X=97 tới mỏ X=103…125. Bao hai mỏ khép rộng Z=18 ở gốc, thu về mũi bo R0,3; dày Y=5. Hai nửa bắt chéo ở khớp, mỗi nửa dày 3 tại vùng chồng | Thép tối màu `#363A3D`, metallic 0,85, roughness 0,4 |
| 1 — Mép cắt, liền hai mỏ | 2 mép | X=113…125, dài 12; khép tại Z=0. Cả hai mặt dưới đồng phẳng Y=−2,5. Chỉ vát từ mặt +Y về mép cắt; không răng, không mặt kẹp phẳng như kìm mũi nhọn | Thép mài `#BEC4C8`, roughness 0,25 |
| 2 — Chốt tán | 1 | Tâm (97;0;0), trục Y; thân Ø4 × 6, hai đầu Ø9, tổng dài theo Y=8. Hai đầu nhìn thấy thuộc cùng một chốt, không phải hai vít | Thép `#ABB2B8`, metallic 0,9 |
| 3 — Lò xo hồi | 1 | Một lá uốn chữ U trong khe cán, bao X=66…87, Y=±1,5, Z=±7. Hai đầu tựa tại (84;0;±7), đáy U gần (66;0;0); lá rộng theo Y=3, dày 0,35, bán kính uốn tối thiểu 1,5. Hốc giữ liền lõi cán | Thép lò xo không gỉ `#AEB7BD`, metallic 0,9 |
| 4 — Vỏ bọc cán | 2 | X=0…78. Đường tâm mỗi cán đi qua (7;0;±16), (40;0;±15), (71;0;±8). Tiết diện bọc tối đa 14 theo Z × 15 theo Y; thành quanh lõi ≥2, đầu kín bo R5. Giữ toàn bộ trong bao Z=±23 | Cao su TPE xanh nhạt `#659CCB`, metallic 0, roughness 0,8 |

Khi diễn hoạt, quay hai nửa quanh chốt Y theo hai chiều đối nhau để mở lưỡi. Mở khoảng 6° mỗi nửa tạo khe mũi gần 6; cao su đi cùng lõi, lò xo biến dạng theo cán. Các số bao phía trên chỉ áp dụng khi khép, không áp cho tư thế thả tay.

## Người mới cần biết

- Dùng sau khi cắm đủ đầu nối, trước khi đóng nắp case; máy chưa cắm điện ngoài.
- Kìm này cắt phần thừa của dây rút nylon 100 × 2,5 × 1 trong sheet 05. Chỉ siết dây rút đủ giữ bó, không làm lõm vỏ cáp.
- Áp mặt phẳng −Y của lưỡi vào mặt thoát đuôi của đầu khóa; chỉ đưa đuôi nhựa giữa hai mép cắt, tránh cả vòng đang ôm bó và dây điện.
- Giữ đoạn đuôi sắp rơi bằng tay còn lại, cách xa lưỡi; thu mẩu cắt khỏi case và kiểm tra không còn đầu nhọn.
- Không dùng kìm này cắt vít, dây thép hay cáp đang có điện. Cao su tay cầm không phải chứng nhận cách điện.

## Đối chiếu ảnh

Ảnh tạo bằng **imagegen tích hợp**, đã sửa một lần theo brief; đủ sáu nhãn hình chiếu, một góc 3/4 và ô thao tác. Các sai khác còn lại cần bỏ qua khi dựng:

- LEFT/RIGHT vẫn lớn hơn tỉ lệ các hình chiếu dài; còn hiệu ứng chiều sâu. Không lấy pixel ảnh để đo. FRONT/BACK và hai mặt rộng chưa thể hiện đầy đủ phép đảo hướng của các hình chiếu đối nhau.
- BOTTOM đã phẳng hơn nhưng chưa mô tả chắc chắn mặt cắt sát; dựng hai mặt dưới đồng phẳng Y=−2,5 theo bảng, không sao chép các vệt sáng thành mặt vát.
- Mũi tên 15 ở FRONT còn đặt gần phần đầu thép; **15 là chiều dày lớn nhất tại cao su**, đầu thép chỉ dày 5.
- Ô IN USE chưa cho thấy rõ đuôi nằm giữa hai lưỡi, mỏ còn trông như tì cạnh đầu khóa. Khi dựng thao tác, đặt đường cắt ngay cửa ra đuôi, không cắt đầu khóa, vòng dây rút hoặc bó cáp.

<details>
<summary>Lệnh sửa cuối đã gửi cho imagegen (tiếng Anh)</summary>

```text
Correct this flush-cutter reference sheet once. Preserve title, white/blue technical style, six-panel positions and labels TOP BOTTOM FRONT BACK LEFT RIGHT, blue rubber grips, one steel pivot, one return spring, ONE 3/4 VIEW, ONE IN USE inset, overall 125 x 46 x 15 mm. Keep every label crisp and brand-free.

Fix geometry: this is a FLUSH SIDE CUTTER for plastic tie tails, with two short sharp side-cutting edges that meet along the center seam. Not gripping pliers. BOTTOM must show a single coplanar FLAT ground face across both jaw halves, separated only by the fine cutting seam; remove bevel facets from this BOTTOM face. TOP may have bevel facets. The steel cutting head is only 5 mm thick; side profile has flat underside and bevel on upper side. The 15 mm overall thickness dimension on FRONT must span the thickest rubber grip, not the thinner metal head. Add small "Ø9" at pivot.

Fix scale: all SIX orthographic views must use identical scale. Use precisely 3.3 image pixels per millimetre at this 1536-wide sheet: 125 mm is 412.5 px; 46 mm is 151.8 px; 15 mm is 49.5 px. Broad TOP/BOTTOM total bounding size about 413 x 152 px. FRONT/BACK total size 413 x 50 px. LEFT/RIGHT total size ONLY 152 x 50 px, centered within their panels, with intentional surrounding whitespace. Those two end views currently are too large, shrink them. True orthographic end views have no perspective convergence or depth blur. BOTTOM is viewed from the opposite side; preserve the actual flat jaw face. Keep long side profiles thin.

Add a compact readable legend beneath the 3/4 object in its panel, exactly:
"1 Flush jaws   2 Pivot"
"3 Return spring   4 Rubber grips x2"
Leaders must target correct physical parts.

Fix IN USE contact: the current blades touch the side of the locking head rather than straddling the tail. Show a close-up with visible narrow plastic tail protruding UP from the outer exit face of the tie locking head. Put the TWO cutting edges on opposite sides of this protruding narrow tail, at the exit, with the flat flush face lying against that exit surface. Jaw tips contact the tail just above the locking head, never the tie loop wrapped around cables and never the electrical cables. One hand holds blue grips, the other holds the free tail well above the cutting point. Mark the tail cut point "Trim tail flush". Keep PC rear motherboard tray and intact black insulated cable bundle recognizable. No additional panels.
```

</details>

