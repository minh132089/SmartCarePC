# Sửa tư vấn đồ họa thiếu card rời

Ngày 24/10/2026, sau khi người dùng test demo trên Cloudflare.

## Nguyên nhân và sửa

`needs()` có nhận thiết kế nhưng chỉ áp sàn RAM/SSD, để VRAM rỗng. Do đó AI có thể chọn CPU có iGPU và qua kiểm tra lắp ráp dù chưa đáp ứng hồ sơ thiết kế; phần giải thích còn dùng câu của văn phòng. Bộ 120 ca cũ chưa bao phủ riêng đồ họa, nên kết quả đó không chứng minh nhánh này đúng.

- Nhận diện riêng thiết kế, Photoshop, Illustrator, Lightroom, dựng nội dung và 3D/render. Không nhận tên linh kiện “card đồ hoạ” hoặc “game đồ họa cao” thành nghề thiết kế.
- Hồ sơ 2D mặc định chọn GPU rời từ 6GB ; 3D/Blender/render/video 4K từ 8GB. Đây là chính sách chọn cấu hình trong danh mục của app, không phải yêu cầu tối thiểu chung của các hãng phần mềm. Code kiểm lại GPU trước khi hiện thẻ, kể cả CPU đã có iGPU.
- Giữ yêu cầu người dùng nói rõ không mua card rời, nêu điều kiện phần mềm/phiên bản/driver; không tự gọi đó là máy văn phòng. Đổi từ văn phòng không GPU sang thiết kế ở lượt sau phải xét lại card.
- Lời tư vấn đồ họa giải thích tăng tốc theo tác vụ, không dùng lời hứa FPS của game, không cam kết thời gian render.

Nguồn tham chiếu: [Adobe Photoshop 27.x](https://helpx.adobe.com/photoshop/desktop/get-started/technical-requirements-installation/adobe-photoshop-on-desktop-technical-requirements.html), [Illustrator](https://helpx.adobe.com/illustrator/desktop/get-started/learn-the-basics/technical-requirements.html), [Blender](https://www.blender.org/download/requirements/). Các nguồn nêu khả năng GPU/bộ nhớ/driver và yêu cầu theo tính năng; không suy thành mọi máy 2D đều bắt buộc card rời.

## Kiểm thử

**269 test backend đạt, 0 thất bại/lỗi/bỏ qua**, 18 suite hiện có. Test thêm gồm phân loại 2D/3D/game, đổi nhu cầu giữa hội thoại, diễn giải đúng với iGPU và chặn thẻ do AI bỏ card dù CPU có iGPU. Frontend build thành công trong quy trình demo; `git diff --check` sạch.

Chạy **8 phản hồi API mới qua `smartcarepc.pages.dev`**, kiểm đủ 3 phương án, GPU/RAM/SSD, giá trong trần, linh kiện lắp chung và diễn giải. Cả 8 ca đạt ràng buộc; Codex /root đã đọc lời của cả 8. [Số liệu và đầu ra chính](advice-creator-validation-summary.json), [bộ ca](../tools/advice_creator_cases.json).

Vòng đầu cấu hình nhầm `tiers=[balanced]` là tập con bắt buộc, trong khi bộ chấm hiểu là tập chính xác, nên báo sai khi ứng dụng có thêm Rẻ/Chất lượng. Giữ nguyên log và nhãn đầu; vòng cuối yêu cầu đủ cả 3 tier và chạy lại 8 job mới, giữ mọi ràng buộc phần cứng. Không đổi bộ 120 ca gốc hoặc công cụ chấm.

Ví dụ lượt cuối: câu gợi ý Photoshop/Illustrator 20 triệu trả phương án chính RX6600 8GB, RAM 32GB/SSD512GB, tổng 19.324.000đ. Ca Blender có RTX4060 8GB, RAM 32GB/SSD1TB. Ca chủ động không mua card vẫn dùng iGPU và nêu điều kiện xác nhận phần mềm.

Đây là kiểm tra sửa lỗi có phạm vi, chưa đo lại độc lập tỷ lệ giá của bộ 120 ca, không phải chứng nhận mọi phần mềm/driver/dự án hoặc tốc độ render. Các điều kiện lắp đặt và GPU còn thiếu thông số vẫn cần xác nhận.

Chứng cứ đầy đủ được lưu cục bộ tại [creator-fix-20261024](/Users/yuuki2/Documents/PCsmartcare-testloop/creator-fix-20261024), gồm cả hai lượt API, cấu hình nhãn ban đầu, 269 test và hash mã nguồn không đổi trong lượt cuối.
