# Đồ nghề và phụ kiện còn thiếu khi tự lắp máy

Rà theo thứ tự lắp thật trong cả 7 máy (6 họ case và máy Gaming). Đã có model: vỏ case, cọc đỡ bo trên khay, bo, CPU, tản (kèm vít lò xo, tấm backplate), RAM, SSD, card, nguồn, quạt, hub, riser, đế đứng, dây bên trong máy.
Còn thiếu là những thứ **người lắp phải cầm trên tay** hoặc lấy từ hộp phụ kiện:

| Bước lắp | Cần cầm | Mã sheet |
|---|---|---|
| 0. Trước khi chạm linh kiện | Dây đeo cổ tay chống tĩnh điện, kẹp cá sấu vào khung case | `02_wrist_strap` |
| 1. Kiểm tra / thêm cọc đỡ bo | Đầu tuýp lục giác 5 mm vặn cọc M3 cao 6,5 | trong `04_screw_kit` |
| 2. Bắt bo vào khay | Tua vít PH2, vít M3 × 5 | `01_screwdriver`, `04_screw_kit` |
| 3. Lắp SSD M.2 | Tua vít nhỏ PH1 (đầu đổi được), vít M2 × 2,5 | `01_screwdriver`, `04_screw_kit` |
| 4. Bôi keo, lắp tản | Ống keo tản nhiệt + que gạt | `03_thermal_paste` |
| 5. Bắt nguồn, nắp khe PCIe, card | Vít #6-32 × 6 | `04_screw_kit` |
| 6. Bắt quạt / két nước | Vít quạt tự ren 5 × 10, vít M3 × 6 và M3 × 30 của két | `04_screw_kit` |
| 7. Gom dây sau khay | Dây rút nhựa 100 mm, dây dán gai, kìm cắt sát | `05_cable_ties`, `08_flush_cutter` |
| 8. Máy Gaming có Wi-Fi | 2 ăng-ten vặn vào cổng sau | `07_wifi_antenna` |
| 9. Cắm điện khi đã lắp xong | Dây nguồn AC (đầu C13 → phích tường 2 chân tròn) | `06_power_cord` |

`00_overview`: tất cả đồ trên một tấm thảm lắp ráp, đánh số theo bảng trên.

Quy ước chung: đơn vị mm, không logo / chữ hãng, màu trung tính (đen nhám, thép, cam/xanh dương nhạt cho tay cầm). Mỗi món có gốc toạ độ ghi rõ trong file `.md`.
