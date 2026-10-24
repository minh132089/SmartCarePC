# Hỏi thêm nhu cầu trước khi tư vấn PC

**Thu hồi kết luận nghiệm thu sau khi khách kiểm tra:** phát hiện follow-up RTX bị đổi sang AMD và RAM Lexar Phúc Anh đã ngừng kinh doanh dù metadata còn giá/tồn kho. Các tỷ lệ bên dưới là lịch sử của bộ kiểm cũ; chưa đủ để xác nhận mua được. Bộ chấm đã bổ sung yêu cầu kiểm tồn kho độc lập. Xem [bản sửa lỗi khách phát hiện](advice-user-followup-fix.md).

## Luồng mới

- “Thiết kế đồ họa, 20 triệu” → hỏi chủ yếu 2D hay 3D/render, phần mềm nếu biết.
- “Chơi game, 25 triệu” → hỏi nhẹ/esports hay nặng/AAA, hoặc tên game.
- Có cả hai nhu cầu chung thì gộp một lượt hỏi. Đã nêu Photoshop/Illustrator, game cụ thể hoặc mức nặng/nhẹ thì dùng dữ liệu đó.
- Khi hỏi thêm, chưa tạo thẻ hay tự ước lượng giá. Ngân sách nhắc lại trong câu hỏi không bị coi là báo giá.
- Khách trả “3D”, “chỉ 2D” hoặc “nhẹ” thì gắn với câu hỏi trước; giữ ngân sách, RAM/SSD và hãng đã nói. Phủ định 3D không làm tăng sàn VRAM; mức game mới ghi đè mức cũ.
- Chặn hỏi lặp theo nhóm nhu cầu. Khách muốn tư vấn luôn hoặc chưa biết thì dùng giả định được nêu rõ. Đổi từ game sang thiết kế vẫn có thể hỏi về nhu cầu mới.

Đã bỏ quy tắc cũ ép dựng máy ngay khi có ngân sách. Model tạo câu hỏi theo hội thoại; backend xử lý câu trả lời nối tiếp và giữ các bước kiểm linh kiện/giá khi dựng thẻ.

## Kết quả ngày 24/10/2026

**274 test backend đạt, 0 thất bại/lỗi/bỏ qua**, 18 suite hiện có. Hash hai file nguồn và test trùng với bản đã chạy Maven thành công trước khi tạm dừng; không sửa mã Java trong lượt API này. Có test câu hỏi dù đã có ngân sách, trả lời ngắn, phủ định 3D, ghi đè mức game, chặn hỏi lặp và đổi nhóm nhu cầu. Build frontend trong lần bật demo thành công.

Sau khi provider hoạt động lại, chạy **9 job mới qua Cloudflare trên cùng một bản nguồn**:

| Nhóm | Kết quả |
|---|---|
| Đồ họa chung, game chung, cả hai | 3/3 hỏi đúng thông tin còn thiếu, chưa tạo thẻ |
| Photoshop/Illustrator, Valorant, yêu cầu tư vấn luôn | 3/3 tạo cấu hình, không hỏi thừa |
| Trả lời “3D”, “Không làm3D, chỉ2D”, “Nhẹ thôi” | 3/3 dùng nguyên câu hỏi model vừa trả, giữ ngân sách/RAM/SSD, không hỏi vòng lặp |

**9/9 đạt ràng buộc và quyết định; 9/9 lời tư vấn được Codex đọc lại và đạt tiêu chí nội bộ.** Ví dụ phương án chính: 3D dùng RTX 3060 12GB, RAM 32GB/SSD 1TB, 27.746.000đ; 2D dùng RTX 3050 6GB, RAM 32GB/SSD 1TB, 22.975.000đ. Đây là kết quả quan sát, không phải cấu hình cố định cho mọi khách.

**48/48 quan sát giá, từ 48 URL, khớp với trang bán; sai số 0 đồng.** Chụp trang độc lập qua Jina/HTML trực tiếp; khoảng cách lớn nhất với lúc app kiểm giá là 321,21 giây, dưới hạn 600 giây. Tính RAM theo bộ kit và quạt theo số bộ mua. Đã đọc tên hàng và vùng giá; sửa riêng nhãn đề xuất của công cụ chụp cho hai trang Phúc Anh vì nó bắt giá sản phẩm liên quan. Giá đúng của MIK G3 GLASS M là 599.000đ và RX 6600 V3 là 5.490.000đ; giữ nguyên index và trang gốc để kiểm lại. Không lấy giá app làm nhãn chuẩn.

Một lần GET đọc trạng thái Valorant gặp lỗi TLS; đã lấy lại kết quả từ **đúng job ID ban đầu**, không gửi POST thay thế. Log runner gốc giữ lỗi; báo cáo cuối dùng đủ chín kết quả từ chín ID đó.

## Chứng cứ và giới hạn

- [Số liệu](advice-context-validation-summary.json), [9 ca hồi quy tĩnh](../tools/advice_context_cases.json).
- [Lượt API thật và chứng cứ giá](/Users/yuuki2/Documents/PCsmartcare-testloop/context-fix-20261024-round2): phản hồi, đề bài, nhãn đọc lại, bản băm, công cụ đối chiếu và báo cáo cuối. Các lượt nối tiếp dùng câu hỏi thật; câu hỏi trong fixture tĩnh chỉ dùng để tái lập test.
- [Lượt bị chặn trước đó](/Users/yuuki2/Documents/PCsmartcare-testloop/context-fix-20261024): giữ sáu lỗi provider 401 INVALID_API_KEY, không tính chúng là ca đạt. Chứng cứ 274 test XML/log ở cùng thư mục.

Đây là hồi quy nội bộ cho tính năng hỏi thêm, chưa có người chấm độc lập và không chứng minh mọi đầu vào đạt 98% hay 100%. Không ghép số liệu với 120 ca hoặc tám ca đồ họa của bản trước. Giá đúng tại thời điểm chụp; các thẻ vẫn có điều kiện cần cửa hàng xác nhận về BIOS, gá tản và khoảng trống GPU. Chưa đo hiệu năng hay lắp thử phần cứng.
