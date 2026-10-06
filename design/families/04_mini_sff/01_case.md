# 04 Mini SFF — 01 Vỏ trống

Nguồn: [đặc tả chung](../00_families_spec.md), mục 0, 4, 7 và 8; [concept](../../case-families/04_mini_sff.png). [Ảnh sheet](01_case.png) dùng để đọc hình dáng; **số trong tài liệu là chuẩn để dựng**, không đo theo pixel. Đây là mô hình mô phỏng, không phải bản vẽ gia công.

## Kích thước, trục và vật liệu

Đơn vị mm. Gốc O ở tâm thân; X từ lưng (−) ra trước (+), Y từ đáy (−) lên nóc (+), Z từ khoang GPU (−) sang khoang bo (+). Tọa độ dưới đây là trạng thái đã lắp. W × H × D = **180 × 250 × 350** theo Z/Y/X; biên X ±175, Y ±125, Z ±90. Bốn chân Ø22 × 12 tại `(±142; −131; ±64)`; cao cả chân **262**. Quy đổi 100 mm = 1 đơn vị cảnh.

Nhôm xước graphite `#45474A`, dày 2, metallic 0,8, roughness 0,38; vân mặt trước theo Y, hông theo X. Khay/giá thép dày 1,2, sơn `#24262A`; giá GPU dày 2. Cao su/nhựa `#202329`, dây `#15171B`, kim loại sáng `#AEB4BC`, tiếp điểm `#C6A565`. Vòng power trắng `#D2D7DD`. Lọc PET dày 0,5, mắt 0,35, sợi 0,15; cassette nylon 1,5 nằm trong bao thân.

## Các phần và vị trí

| Số trên hình | Phần | Hình học và mốc lắp |
|---|---|---|
| 1 | Mặt trước | X 173…175; Y ±120; Z ±86; góc R8. Bốn vít ẩn, tâm Y ±105/Z ±71. |
| 2 | Hai hông | X ±167, Y ±116; Z 88…90 và −90…−88. Mỗi nắp 334 × 232 × 2, bốn vít tại X ±152/Y ±101. Mỗi bên **33 khe**, tâm X = −144 + 9j, j=0…32, Y=0; khe rộng 4 theo X, dài 170 theo Y, R2. Lọc áp mặt trong. |
| 3 | Nóc | Y 123…125; cửa X −148…133, Z ±70 (281 × 140). Bốn vít tại X ±157/Z ±72. Lỗ tròn Ø2,4, bước 3,6 so le; chừa vùng vít và gân. Lọc nam châm. |
| — | Đáy | Y −125…−123; cửa X −142…135, Z ±67 (277 × 134), lỗ tròn như nóc. Bốn lỗ chân theo tâm chân. |
| 4 | Khay giữa | X −162…43, Y ±113, Z −7,7…−6,5. Bốn chân đế M3 cao 6,5 tại X −151/−1, Y ±75, đỉnh Z=0. Cửa CPU 96 × 96 tâm `(−96;12;−6,5)`. Không lắp bo. |
| 5 | Lưng / PCIe | I/O ITX để rỗng: X=−175, Y ±79,5, Z 1,1…38,1 (159 × 37). **Ba nắp PCIe dọc**: Y −58…62, dãy Z −83,26…−22,3, bước 20,32 về −Z; khe giữa nắp 1,5. Lá GPU tương lai ở X −172…−171, dùng gờ lùi từ khung sau, không ép lá về X −175. |
| 6 | AC | Inlet C14 tâm `(−175;−103;38)`, cửa hướng −X. Dây bọc kín dài tâm 450, kẹp sát đáy Y=−116; đầu C13 góc 90° trong X 25…50, đến ổ PSU tương lai tại `(50;−4,5;52,25)`. |
| 7 | Gá SFX | Giá chữ L dày 1,2, mặt đỡ X=50, chừa cửa xả và đầu AC. Bốn lỗ tại mọi tổ hợp Y=−102,5/+7,5, Z=18,25/68,25; bước 110 × 50, trục vít X. Không có PSU trong sheet. |
| 8 | Giá GPU / riser | Giá GPU X −171…84, Y −66…−64, Z −61…−43, hai M3. Giá đầu riser theo tâm khe `(−73;64,2;−28,3)`, hai tai cách 100 theo X. Không lắp riser/card. |
| 9 | Chân | Bốn chân tròn nêu trên, mỗi chân một vít M4 chìm. |
| 10 | I/O trước | Power Ø14 tại `(175;−62;59)`; USB-C 8,4 theo Z × 2,6 theo Y tại `(175;−88;59)`. Module X 156…173, Y −99…−49, Z 49…69. |

Một vị trí gá quạt nóc 120, **không có quạt**: tâm dự kiến `(−65;105,5;12)`, bốn lỗ tại X=−117,5/−12,5 và Z=−40,5/64,5, bước 105 × 105. Giữ mặt gá ở Y=118, chừa khoảng lên nóc. Không giá radiator, quạt sau hoặc cửa PSU ở lưng.

## Lỗ đi dây trên khay

Tất cả ở Z=−6,5; kích thước là lòng mở X × Y, thêm viền EPDM rộng 3 tháo được. Khoang dây Z −24…−8 và hành lang trước X 14…47.

| Mã | Tâm (X;Y) | Lòng mở | Công dụng |
|---|---|---|---|
| 04-G0 | (30;−62) | 18 × 58 | Dây PSU vào khoang giữa; đầu ATX24 đi theo cạnh 58 |
| 04-G1 | (−131;99) | 28 × 10 | EPS |
| 04-G2 | (30;19) | 18 × 58 | ATX24 |
| 04-G3 | (−54;99) | 32 × 10 | CPU_FAN / quạt nóc |
| 04-G4 | (−70;−101) | 96 × 12 | Đầu đực riser đi qua; giữ adapter cái bên GPU |
| 04-G5 | (0;−101) | 28 × 12 | Type-E / F_PANEL |

**Không có G6.** Nguồn GPU đi qua G0 rồi vòng dưới khay. Không dùng cửa CPU làm đường dây. Dây mềm uốn R≥12, riser R≥15; AC luôn có vỏ cách điện và kẹp riêng.

## Đầu cắm và vít

- C14: ba chân đực L/N/PE trong vỏ khóa hình thang; C13 góc có ba tiếp điểm cái tương ứng. Giữ đầu dây kín, không vẽ AC trần. Hình dáng vỏ/cửa bắt inlet chưa có kích thước chi tiết trong nguồn, cần khớp mesh inlet khi dựng.
- USB-C ngoài: 24 tiếp điểm, cắm đảo chiều; đầu trong **Type-E 20 tiếp điểm**, vỏ có khóa định hướng, đi G5. Không dùng USB19.
- PWR SW: hai tiếp điểm, không phân cực, vào F_PANEL chân 6/8. PLED: hai tiếp điểm có cực, vào chân 2(+)/4(−). Không reset, audio, USB-A hoặc HDD LED.

| Nhóm | Vít / điểm gá |
|---|---|
| Panel | 8 hông + 4 trước + 4 nóc = **16 M3 × 6** |
| PCIe trống | **3 #6-32 × 6**, mỗi nắp một vít; không thêm vít dưới nắp |
| Chân | **4 M4 × 8**, chìm |
| Giá GPU | **2 M3**, chiều dài và tâm bắt khung chưa được nguồn chốt |
| Bo tương lai | 4 chân đế M3 cao 6,5; 4 M3 × 5 khi lắp bo; lỗ bo Ø3,4 |
| SFX tương lai | 4 M3 × 6 theo lưới ở bảng trên |
| Quạt tương lai | 4 vít Ø5 × 10 theo bước 105 |
| Khung/lọc | Lưng, đáy, khay cố định và lọc nam châm: 0 vít tháo |

Tâm vít nắp PCIe đầu tiên suy ra từ lá GPU đã quay: Y=−53, Z=−32,46, rồi giảm Z từng 20,32. Tọa độ tai neo khung của gá SFX/riser và kiểu giữ module I/O chưa được nguồn quy định; không suy ra từ vít trang trí trong ảnh. Vít nổi tối đa 2 ngoài thân, không cộng vào bao chân ở trên.

## Cách đọc các view và sai lệch ảnh

FRONT +X nhìn −X: mặt đặc, power/USB-C ở trái ảnh. BACK −X nhìn +X: PCIe bên trái, I/O và C14 bên phải. LEFT +Z nhìn −Z: hông bo, trước ở phải; RIGHT −Z nhìn +Z: hông GPU, trước ở trái. TOP +Y nhìn −Y: trước ở dưới; BOTTOM −Y nhìn +Y: trước ở trên; cả hai có +Z bên phải. Sáu hình chiếu yêu cầu cùng tỷ lệ. Hình 3/4 mở hai hông và nâng nóc để thấy khay, giá và dây AC; khoảng tách không phải tọa độ lắp.

Ảnh tạo bằng **imagegen tích hợp**, đã sửa một lượt theo brief. Các sai lệch còn lại phải bỏ khi dựng:

- BACK vẫn vẽ **bốn** nắp dù nhãn ghi ba; chuẩn là **ba nắp/ba vít**, không bốn. Cụm phối cảnh cũng không dùng để đếm vít/nắp.
- Số khe hông và tỷ lệ giữa các hình chiếu chưa khớp hoàn toàn; dựng đúng công thức 33 khe, không đếm hoặc đo pixel.
- TOP/BOTTOM bị vẽ thành khe dài; chuẩn là lỗ tròn so le. Bốn lỗ gá quạt trong TOP chỉ đúng ý đồ, cần đặt lại theo tọa độ ở trên.
- Khay trong ảnh có thêm khe đứng không nhãn ở giữa; bỏ khe thừa, chỉ giữ cửa CPU và G0–G5. Hình 3/4 vẽ gá SFX giống hộp; thay bằng giá L mở đúng bảng.
- Hình chi tiết đầu cáp vẫn giống USB-C hơn Type-E; không dùng hình đó làm mesh Type-E. Dấu chân 1/khóa và các tiếp điểm nhỏ phải dựng theo đặc tả. Một số chân/callout bị che hoặc trỏ lệch.

Prompt tóm tắt: “Empty graphite Mini SFF 180 × 250 × 350; six aligned orthographic views plus open 3/4; 33 vertical slots per side, center tray G0–G5, three vertical PCIe covers, C14/C13 AC extension, keyed front USB-C/Type-E; white background, thin blue dimensions, no installed PC components.” Lượt sửa tập trung vào số PCIe, phía inlet, lỗ khay, gá quạt và đầu cáp; những lỗi chưa sửa hết đã ghi ngay trên.

## Người mới cần thấy

1. Hai hông đều là nhôm thông gió và đều tháo được; +Z để lắp bo, −Z để lắp GPU.
2. Khay giữa tạo bố trí sandwich. GPU cần riser, không cắm xuyên khay; ba khoảng PCIe vẫn phải để trống phía trong.
3. Inlet AC ở lưng nối tới PSU SFX phía trước qua dây bọc kín, không phải cửa lắp PSU sau.
4. Vỏ trống vẫn có giá, chân đế, lọc và I/O; sheet này không có quạt hoặc linh kiện PC.
5. Giới hạn mô phỏng: GPU 260 × 120 × 50, tản cao 55 từ IHS, SFX sâu 100; không SFX-L/ATX và không radiator.
