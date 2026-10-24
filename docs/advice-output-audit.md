# Kết quả đối chiếu đầu ra AI tư vấn PC

**Luồng hỏi thêm nhu cầu, 24/10/2026:** [báo cáo mới](advice-context-followup.md): 274 test backend đạt; 9/9 job API thật đạt kiểm tra hội thoại, cấu hình và lời tư vấn; 48/48 quan sát giá khớp trang bán trong lượt này. Đây là bộ hồi quy riêng cho tính năng, không phải tỷ lệ chính xác cho mọi đầu vào.

**Cập nhật 24/10/2026:** số liệu 120 ca dưới đây thuộc bản nghiệm thu trước khi phát hiện nhánh thiết kế đồ họa thiếu ràng buộc card rời. Đã bổ sung sửa và kiểm thử riêng: [báo cáo đồ họa](advice-creator-fix.md). Không lấy tỷ lệ 100% cũ làm bảo đảm cho mọi nhu cầu hoặc cho bản sửa mới.

**Đạt 100% trên bộ nghiệm thu nội bộ 120 ca; không còn lỗi được ghi nhận trong bộ này.** Hoàn tất 23/10/2026, 23:48 (UTC+7), nhánh `guide-reveal`. Lượt `eval100-acceptance15` chạy 120 phản hồi API mới trên cùng một JAR cố định sau khi provider hoạt động lại. Không ghép phản hồi từ các lượt trước.

## Kết quả cuối

| Chỉ tiêu | Bộ chính | Bộ bổ sung | Tổng |
|---|---:|---:|---:|
| Đúng ràng buộc cấu hình / quyết định | 100/100 | 20/20 | **120/120 — 100%** |
| Lời tư vấn đạt tiêu chí đã đặt | 100/100 | 20/20 | **120/120 — 100%** |
| Giá khớp trang bán | — | — | **281/281 — 100%** |

281 quan sát giá theo URL/thời điểm, từ **165 URL sản phẩm**. Sai số cho phép **0 đồng**, tính theo số bộ thực tế phải mua. Khoảng cách lớn nhất giữa lúc app kiểm trang và chứng cứ riêng là **438,85 giây**, dưới hạn 600 giây. Một quan sát chỉ tính một lần nhưng kiểm tất cả thẻ/câu trả lời dùng nó; đây không phải 281 mẫu phần cứng độc lập về thống kê.

Bộ chấm `--zero-issues` yêu cầu cả ba tỷ lệ bằng 100%, ít nhất 100 ca và 100 quan sát giá. Kết quả: **ĐẠT, mã thoát 0**. Sau khi chuyển chứng cứ sang thư mục lưu bền vững, chạy lại bộ chấm vẫn đạt. Đề bài và nhãn kỳ vọng của 120 ca giữ nguyên theo ID; mọi job có ID riêng. Mã nguồn, công cụ chấm và JAR không đổi trong lượt chạy.

## Các lỗi đã tìm và sửa

- Giữ hãng/model/VRAM được chốt, hiểu lỗi chính tả NVIDIA, áp dụng nhu cầu ở lượt chat cuối. Gỡ card và yêu cầu VRAM cũ khi đổi sang văn phòng không GPU rời; không nhận “không chơi game” thành chơi game.
- Tính RAM theo đúng mã kit, số thanh và số khe của đúng bo. G.SKILL Trident Z5 GX2/GA2 và Kingston Fury KF432C16BB1AK2/32 là kit hai thanh, không được tính thành hai kit khi chỉ cần 32GB. Giữ mã hàng trên h1 dù metadata lược bớt, cập nhật tên ngay cả khi giá không đổi. UI ghi số thanh và số bộ mua.
- Đồng bộ CPU/bo/RAM khi đổi socket hoặc DDR, giữ kiểu vỏ ITX; kiểm lại cấu hình sau mọi bước thay hàng/hạ giá. Không dùng nguồn Jetek B550/B650 làm bo mạch chủ, không đọc MSI A620AM thành nền AM4.
- Tính quạt bán theo bộ bằng số bộ phải mua làm tròn lên; ghi số quạt sử dụng và quạt dư. UI dùng đơn vị quạt. Chặn SPARKLE B580 TITAN OC SB580T-12GOC dài315mm/ba quạt khỏi mẫu GPU240mm/vỏ SG13. Dữ kiện hãng: [SPARKLE](https://www.sparkle.com.tw/files/20241203213858182.pdf), [hướng dẫn SilverStone SG13](https://resources.mini-box.com/online/Enc-Silverstone-SG13/Enc-Silverstone-SG13-manual.pdf).
- Giá phải lấy từ đúng trang sản phẩm, đúng biến thể và thời hạn thật. Giá mua online được ưu tiên thay metadata cũ/giá combo; không tự làm mới timestamp khi đọc trang thất bại. PreOrder/BackOrder không tính là có sẵn. Nguồn tìm kiếm lỗi thì đọc lại URL đã biết, không biến giá AI ước lượng thành giá bán.
- Từng phương án phải giữ yêu cầu RAM/SSD/GPU và trần tiền; ít nhất một thẻ trong ngân sách gốc mới tính là giải quyết nhu cầu mua. Phương án vượt tiền nêu rõ khoản vượt. Thẻ Chất lượng cần nâng cấp CPU/GPU, dung lượng hoặc thời hạn bảo hành có chứng cứ; không hạ GPU để đổi SSD/bảo hành, không coi đổi hãng cùng chip là tăng hiệu năng.
- Gợi ý chính ưu tiên tiết kiệm theo tiêu chí đã công bố, đồng bộ với thẻ mặc định UI. Với AI, không khẳng định bản đắt hơn chạy mô hình nhanh hơn khi chưa đo; không hứa FPS.
- Job gặp lỗi hoặc phản hồi rỗng/bị cắt phải kết thúc với trạng thái lỗi, không treo giao diện hoặc tính lời xin lỗi là tư vấn thành công.

## Một số đầu ra thực tế của lượt cuối

| Ca | Kết quả đã lưu |
|---|---|
| `o08`: bố mẹ, 11 triệu, rẻ nhất | Gợi ý Rẻ **7.146.000đ**, i3-12100 có iGPU, RAM8GB/SSD256GB; nêu giới hạn đa nhiệm. |
| `e08`: đúng RTX3060 12GB, RAM64GB, 36 triệu | Phù hợp **27.798.000đ**, RAM Lexar32GB/kit: **mua2 bộ, dùng4 thanh16GB**, tiền RAM **3.998.000đ**; bo Gigabyte B760 GAMING X DDR4 có4 khe. Cả ba thẻ giữ64GB. |
| `e05`: đúng RTX4070 12GB | Có thẻ Rẻ **31.306.000đ**, Phù hợp **35.607.000đ** đúng model; khắc phục ca chưa chốt được ở lượt98% trước. |
| `e07`: đúng RX6600 8GB | Phù hợp **21.297.000đ** đúng model; không đổi thành RX7600 để đạt điểm. |
| `k10`: Stable Diffusion, VRAM16GB | Gợi ý Rẻ **22.575.000đ**, RTX4060Ti16GB, RAM32GB, SSD1TB; giữ yêu cầu và tiết kiệm **8.432.000đ** so với Phù hợp. |
| `e04`: đúng RTX4060Ti16GB | Gợi ý Rẻ **22.833.000đ**, cùng13400F/GPU/RAM với Phù hợp; tiết kiệm **12.643.000đ**. |
| `a03`: bộ quạt | Thẻ Chất lượng dùng3 quạt Cooler Master SICKLEFLOW, **mua1 bộ ba quạt =690.000đ**, không tính thành3 bộ. |
| `m03`: giữ kiểu mini | Phù hợp **27.105.000đ**, Chất lượng **28.644.000đ**, giữ ITX; không còn B580TITAN315mm, vẫn nêu điều kiện kích thước GPU cần cửa hàng xác nhận. |

## Cách chấm và phạm vi

Người chấm lời và đọc lại giá: **Codex /root, đối chiếu nội bộ**. Từng lời được đọc theo tiêu chí: đúng nhu cầu, giải thích phù hợp dữ liệu thẻ, nêu đánh đổi và không tự mâu thuẫn. Chỉ tiêu này là đánh giá của agent, chưa có người chấm độc lập bên ngoài.

Chứng cứ trang giá được thu riêng qua Jina/HTML trực tiếp, rồi kiểm tên/mã, biến thể và vùng giá sản phẩm chính. Đề xuất parser sai được giữ trong index; nhãn sửa ghi riêng sau khi đọc trang. Ví dụ i5-12400 An Phát có giá chung5.699.000đ và deal3.799.000đ chỉ tại showroom Lý Thường Kiệt: nhãn giữ giá chung, app cũng dùng5.699.000đ. Không lấy số app làm nhãn chuẩn. Thuật toán chọn chứng cứ theo thời gian và khả năng đọc trang, không chọn theo việc giá có trùng app hay không.

Bộ ca đã dùng nhiều vòng để tìm lỗi, gồm cả một số ca từng gọi là holdout. Vì vậy đây là **nghiệm thu hồi quy nội bộ, không phải thử nghiệm mù**. Kết quả100% giới hạn ở120 câu và các URL/thời điểm đã lưu, không chứng minh mọi đầu vào đều đúng hoặc giá sau này không đổi.

Thẻ `CONDITIONAL` vẫn cần xác nhận kích thước GPU, khoảng trống dây/cáp, vị trí quạt và các thông số còn thiếu. Bộ này chưa đo FPS/tốc độ AI hay thử lắp phần cứng thực tế. Chuẩn áp dụng: [ai-advice-standard.md](ai-advice-standard.md).

## Kiểm thử và chứng cứ

- Backend: **265 test, 0 thất bại, 0 lỗi, 0 bỏ qua**, 18 bộ XML; package thành công. `LinkageError` chủ động tạo trong test job không phải lỗi chạy thực tế.
- Bộ chấm Python: **21 test đạt**, gồm kit RAM, phân biệt Ti/SUPER, số bộ quạt và GPU315mm/vỏ SG13. Frontend build và lint phần UI sửa: đạt. `git diff --check`: sạch.
- [Kết quả chấm](/Users/yuuki2/Documents/PCsmartcare-testloop/eval100-acceptance15-evaluation.txt), [tổng hợp số liệu](/Users/yuuki2/Documents/PCsmartcare-testloop/eval100-acceptance15-evidence/summary.json), [120 phản hồi API](/Users/yuuki2/Documents/PCsmartcare-testloop/eval100-acceptance15-evidence/api), [nhãn cuối](/Users/yuuki2/Documents/PCsmartcare-testloop/eval100-acceptance15-evidence/gold.json).
- [Thư mục chứng cứ](/Users/yuuki2/Documents/PCsmartcare-testloop/eval100-acceptance15-evidence): **360 file trang gốc được dùng**, JAR đã chạy, nguồn hãng, snapshot mã nguồn, test XML/log, nhãn, bản băm và nguồn gốc từng file.
- JAR SHA256: `3ced72bbf4c987bfd32ab07832f9352853cc28022315166592be3387bb07d316`.

## Lịch sử không được tính là kết quả cuối

Mốc98% cũ ở `eval98-acceptance12` đạt118/120 quyết định,119/120 lời và235/235 giá. Những lỗi cũ đó đã được chạy lại trong lượt15.

Các lượt mang tiền tố `eval100` trước được giữ nguyên chứng cứ: lượt10 từng báo đủ điểm nhưng **bị loại sau hậu kiểm** vì sai số khe RAM/bảo hành; lượt12 còn lỗi nhận kit Kingston; lượt13 bị provider502 nên không có kết quả nghiệm thu hợp lệ; lượt14 bị loại do tính sai bộ quạt và B580TITAN quá dài. Sau sửa, chạy lại toàn bộ120 ca ở lượt15; không chỉ chạy lại những ca trượt rồi ghép điểm. Lịch sử nằm trong thư mục chứng cứ và [nhật ký trước](/Users/yuuki2/Documents/PCsmartcare-testloop/advice-output-audit-history.md).

Chạy lại phép chấm từ thư mục dự án, không cần gọi provider:

```sh
python3 -B tools/evaluate_advice.py \
  --gold /Users/yuuki2/Documents/PCsmartcare-testloop/eval100-acceptance15-evidence/gold.json \
  --runs /Users/yuuki2/Documents/PCsmartcare-testloop/eval100-acceptance15-evidence/api \
  --zero-issues
```

Bản sửa được đóng gói trên nhánh `fix`. Kết quả trên được đo trước khi tạo commit; các file chứng cứ ngoài repository được lưu cục bộ trên máy đã chạy kiểm thử. [Số liệu nghiệm thu](advice-validation-summary.json).
