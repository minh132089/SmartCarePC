# Bộ quy luật kiểm tra tương thích linh kiện PC

> Phiên bản: 1.0 — 20/10/2026  
> Mục đích: Làm tài liệu quy tắc cho PC Builder / Compatibility Engine.  
> **Nguồn xác nhận cuối cùng:** trang thông số và danh sách CPU/RAM hỗ trợ của nhà sản xuất theo đúng mã sản phẩm, phiên bản bo mạch và BIOS.

## 1. Phân loại kết quả

- **ERROR (Hard constraint):** Không tương thích hoặc không thể lắp/vận hành theo cấu hình dự kiến.
- **WARNING (Soft constraint):** Có thể dùng nhưng cần cập nhật, kiểm tra hoặc chấp nhận giới hạn.
- **RECOMMENDATION:** Lời khuyên tối ưu, không bắt buộc.
- **UNKNOWN:** Chưa đủ dữ liệu để kết luận; không tự động đánh dấu tương thích.

## 2. CPU và mainboard

- **ERROR:** Socket CPU phải tương thích với socket bo mạch; cùng tên socket vẫn chưa đủ để khẳng định hỗ trợ.
- **ERROR/UNKNOWN:** Model CPU phải nằm trong danh sách CPU Support List của đúng model/revision mainboard; kiểm tra chipset và phiên bản BIOS tối thiểu.
- **WARNING:** Nếu BIOS hiện tại cũ hơn mức yêu cầu, cần cập nhật bằng CPU được hỗ trợ hoặc tính năng BIOS Flashback nếu bo có.
- **WARNING:** VRM và giới hạn công suất bo mạch phải đủ để CPU chạy ổn định ở mức tải dự kiến.
- **WARNING:** Khả năng ép xung phụ thuộc đồng thời CPU, chipset, BIOS và giới hạn của nhà sản xuất; CPU Intel dòng K/KF thường cần bo Z để ép xung hệ số nhân theo cách chính thức.

### Ví dụ nền tảng

| Socket | Chipset ví dụ | Dòng CPU điển hình | Lưu ý |
|---|---|---|---|
| Intel LGA1700 | H610, B760, Z790 | Core thế hệ 12/13/14 | Tùy BIOS và CPU Support List |
| Intel LGA1851 | B860, Z890 | Core Ultra desktop Series 2 | Không lắp CPU LGA1700 |
| AMD AM4 | B450, B550 | Nhiều mẫu Ryzen 3000/5000 | Không phải mọi CPU AM4 đều được hỗ trợ; B450 với Ryzen 5000 có thể cần BIOS mới |
| AMD AM5 | B650, X870 | Ryzen 7000/8000G/9000 | Tùy BIOS và danh sách hỗ trợ |

> Đây là ví dụ, không phải bảng tương thích đầy đủ. Không suy ra khả năng hỗ trợ chỉ từ chipset.

## 3. RAM

- **ERROR:** Loại DDR (DDR4/DDR5...), dạng thanh (desktop UDIMM hay laptop SO-DIMM), chuẩn ECC/registered nếu có, phải phù hợp với mainboard và CPU.
- **ERROR:** Số thanh và tổng dung lượng không vượt giới hạn khe cắm/dung lượng được hỗ trợ.
- **WARNING:** Tốc độ và cấu hình XMP/EXPO phụ thuộc CPU, mainboard, BIOS và số thanh; ép xung RAM không được đảm bảo.
- **WARNING:** Trộn các kit RAM khác nhau có thể gây lỗi hoặc phải hạ xung.
- **RECOMMENDATION:** Ưu tiên kit 2 thanh tương thích để chạy dual-channel trên nền tảng phổ thông.
- **Đặc biệt:** Mainboard LGA1700 có bản DDR4 hoặc DDR5; phải xem đúng mã bo, không suy ra từ socket.

## 4. GPU và xuất hình

- **ERROR:** Mainboard phải có khe mở rộng tương thích; case phải đủ chiều dài, chiều cao, độ dày và số slot cho GPU.
- **ERROR:** PSU phải có công suất và đầu nối cấp điện đúng chuẩn cho GPU.
- **WARNING:** PCIe có tương thích ngược giữa nhiều thế hệ nhưng có thể giới hạn băng thông; kiểm tra số lane thực tế (x16/x8/x4).
- **WARNING:** Nếu CPU không có iGPU và hệ thống không có GPU rời hay phương án hiển thị khác, sẽ không có đầu ra hình ảnh thông thường.
- **Không dùng quy tắc `AMD không có G = không có iGPU`:** Ryzen 7000/9000 desktop thông thường có đồ họa tích hợp cơ bản, nhưng có ngoại lệ như 7500F. Intel hậu tố F thường không có iGPU. Phải tra thông số đúng SKU.
- **WARNING:** Kiểm tra cổng hình ảnh của GPU/bo mạch và màn hình; cổng hình ảnh trên mainboard thường chỉ hoạt động khi CPU hỗ trợ đồ họa tích hợp.

## 5. Tản nhiệt CPU và AIO

- **ERROR:** Tản nhiệt phải có bộ gá tương thích socket và được nhà sản xuất xác nhận hỗ trợ.
- **WARNING:** Bộ gá LGA1700 thường dùng được cho LGA1851, nhưng vẫn cần xác nhận với từng tản; AM4 thường dùng được cho AM5 khi cơ chế gá phù hợp, không phải mọi bộ gá đều dùng chung.
- **ERROR:** Tản khí phải vừa chiều cao case; radiator AIO phải vừa vị trí lắp và không va chạm RAM, VRM, GPU hay panel.
- **WARNING:** Khả năng giải nhiệt phải phù hợp mức tiêu thụ điện thực tế của CPU khi tải nặng.
- **WARNING:** Kiểm tra chính xác hộp CPU có kèm tản không theo SKU/phiên bản boxed/tray; không suy ra tuyệt đối từ hậu tố K/KF/X.
- **WARNING:** Kiểm tra đầu nối bơm/quạt và khả năng điều khiển trên mainboard.

## 6. Nguồn (PSU)

- **WARNING:** Ước tính công suất dựa trên công suất CPU/GPU khi tải cao, cộng RAM, bo mạch, ổ đĩa, quạt, bơm và thiết bị khác.
- **RECOMMENDATION:** Có thể ước lượng sơ bộ `PSU >= (CPU + GPU + thành phần khác) × 1,2–1,3`, nhưng phải đối chiếu công suất PSU khuyến nghị của nhà sản xuất GPU, tải đỉnh và chất lượng nguồn. **Không dùng công thức này làm điều kiện tương thích tuyệt đối.**
- **ERROR:** PSU phải có đủ đầu EPS CPU, ATX mainboard và đầu GPU đúng loại/số lượng (PCIe 6+2, 12VHPWR hoặc 12V-2x6 theo yêu cầu).
- **WARNING:** Ưu tiên PSU phù hợp ATX 3.0/3.1 cho hệ thống có GPU công suất cao khi cần.
- **WARNING:** 80 PLUS Bronze/Gold chỉ là chứng nhận hiệu suất, không bảo đảm chất lượng; xem thêm chất lượng điện, độ tin cậy và bảo vệ OCP/OVP/OPP/SCP/OTP.
- **ERROR:** Kích thước PSU và khoảng trống dây phải phù hợp case.
- **WARNING:** Không hoán đổi dây modular giữa các PSU trừ khi nhà sản xuất xác nhận pinout tương thích.

## 7. Case, kích thước và kết nối

- **ERROR:** Case phải hỗ trợ form factor mainboard (ATX, Micro-ATX, Mini-ITX...).
- **ERROR:** Kiểm tra clearance GPU, tản khí, radiator + quạt, PSU và ổ lưu trữ; lưu ý linh kiện có thể tranh chấp cùng không gian.
- **WARNING:** Kiểm tra cổng USB-A/USB-C/audio mặt trước có header tương ứng trên mainboard; thiếu header có thể khiến một số cổng không dùng được.
- **WARNING:** Kiểm tra số fan header, hub, đầu ARGB 5V 3-pin và RGB 12V 4-pin; **không cắm nhầm 5V/12V**.
- **RECOMMENDATION:** Đảm bảo luồng khí, số quạt và không gian đi dây phù hợp mức tỏa nhiệt.

## 8. SSD và HDD

- **ERROR:** SSD M.2 phải khớp giao thức mà khe hỗ trợ: NVMe/PCIe hoặc M.2 SATA; cùng hình dạng M.2 không đồng nghĩa cùng giao thức.
- **ERROR:** Khe M.2 phải hỗ trợ kích thước ổ (2230/2242/2260/2280/22110...) và cách cố định.
- **WARNING:** PCIe 3.0/4.0/5.0 thường tương thích ngược nhưng tốc độ có thể bị giới hạn.
- **WARNING:** Một số khe M.2 dùng chung lane với SATA hoặc PCIe, có thể vô hiệu hóa cổng/khe khác hoặc giảm băng thông; tra sơ đồ chia lane của mainboard.
- **ERROR:** Ổ SATA phải có cổng dữ liệu, nguồn SATA và vị trí lắp thích hợp (2.5/3.5 inch).
- **WARNING:** Kiểm tra hỗ trợ boot, RAID và giới hạn chipset/BIOS khi cần.

## 9. Kiểm tra nâng cao

| Hạng mục | Điều cần xác nhận | Mức gợi ý |
|---|---|---|
| BIOS Flashback | Có thể nâng BIOS khi chưa có CPU tương thích? | WARNING |
| PCIe lane sharing | M.2 hoặc khe mở rộng có vô hiệu hóa/giảm lane của khe khác? | WARNING |
| USB headers | Cổng mặt trước có header tương ứng? | WARNING |
| Fan/RGB | Đúng số lượng, điện áp và loại đầu cắm? | WARNING/ERROR |
| Wi-Fi/Bluetooth | Có sẵn hay cần card/USB bổ sung? | RECOMMENDATION |
| Display | Có GPU/iGPU hoạt động và cổng phù hợp? | ERROR nếu cần xuất hình |
| Hệ điều hành | CPU, TPM, Secure Boot và driver đáp ứng yêu cầu? | WARNING/ERROR theo mục đích |
| VRM | Khả năng cấp điện cho tải CPU dài hạn? | WARNING |
| BIOS features | XMP, EXPO, Resizable BAR, RAID... có hỗ trợ? | WARNING |
| Physical clearance | Các linh kiện có va chạm nhau không? | ERROR |

## 10. Quy trình kiểm tra cho Compatibility Engine

1. Chuẩn hóa **mã SKU** và revision của từng linh kiện, không chỉ tên dòng sản phẩm.
2. Nạp thông số từ nguồn chính thức: socket, CPU support/BIOS, DDR, kích thước, giao thức, cổng nguồn, công suất và khe cắm.
3. Kiểm tra **ERROR**: socket, CPU support, RAM, khe/cổng, nguồn, kích thước, tản và giao thức lưu trữ.
4. Kiểm tra **WARNING**: BIOS, VRM, chia lane, XMP/EXPO, PCIe bandwidth, headroom PSU, làm mát.
5. Đưa ra **RECOMMENDATION**: dual-channel, luồng khí, chất lượng PSU, khả năng nâng cấp.
6. Nếu thiếu dữ liệu quan trọng, trả **UNKNOWN** kèm thông tin cần xác minh; không kết luận tương thích.
7. Với mỗi kết quả, ghi `rule_id`, `severity`, `affected_parts`, `reason`, `required_action`, `source_url` và `checked_at`.

### Ví dụ cấu trúc kết quả

```json
{
  "rule_id": "CPU_BIOS_SUPPORT",
  "severity": "UNKNOWN",
  "affected_parts": ["cpu", "motherboard"],
  "reason": "Chưa biết phiên bản BIOS đang cài trên mainboard",
  "required_action": "Xác nhận BIOS hiện tại và BIOS tối thiểu cho CPU",
  "source_url": null,
  "checked_at": null
}
```

## 11. Nguyên tắc dữ liệu

- Không kết luận tương thích chỉ bằng hậu tố như `F`, `G`, `X`, `K`, tên chipset hoặc tên series.
- Không suy đoán từ socket rằng mọi CPU đều được hỗ trợ.
- Lưu cả nguồn, thời điểm kiểm tra, revision mainboard và BIOS tối thiểu.
- Khi nhà sản xuất cập nhật CPU Support List hoặc BIOS, cần làm mới dữ liệu.
- Các thông số chưa xác minh phải được đánh dấu `UNKNOWN`.
