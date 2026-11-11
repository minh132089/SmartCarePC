# Kiểm tra bổ sung tư vấn nâng cấp — 11/11/2026

Kiểm trên nhánh `feature/ui-motion`, base `6008a5e`. Kết quả dưới đây được chốt trước khi commit/push; lượt kiểm thử không deploy ứng dụng. Báo cáo trước trong `upgrade-test-validation-20261111.md` là kết quả lịch sử, không thay cho lượt kiểm này.

## Lỗi đã tái hiện và sửa

- **Ngân sách follow-up:** biểu mẫu vẫn ghi 12 triệu nhưng người dùng giảm xuống 3 triệu, app vẫn duyệt card 7,99 triệu. Câu ngân sách tổng rõ ràng trong follow-up giờ cập nhật cả giới hạn duyệt, context cho AI và ngân sách trên thẻ. Mức mới được giữ qua các câu tiếp theo; giá linh kiện được nhắc lại, ngân sách riêng cho PSU và câu phủ định không đổi ngân sách tổng trong các ca đã kiểm.
- **Mục tiêu RAM:** yêu cầu lên 32GB vẫn có thể được duyệt kit 16GB. Ngược lại, quy tắc mặc định cho máy chơi game chặn nâng từ 16GB lên 32GB dù người dùng yêu cầu. App giờ đối chiếu mục tiêu RAM ở follow-up với tổng RAM sau khi thêm hoặc thay.
- **Mua dư RAM và mô tả sai:** lượt API đầu mua thêm 2 thanh 16GB vào máy có 16GB, thành 48GB, nhưng reason/summary vẫn nói 32GB. Yêu cầu dung lượng cụ thể giờ kiểm đúng tổng; câu “ít nhất / at least” được phép cao hơn. Reply và reason RAM được tạo từ dung lượng thực tế; summary tự do của AI không dùng cho kế hoạch có RAM. Lượt cuối chỉ thêm 1 thanh 16GB: tổng 32GB, giá 2,99 triệu thay vì 5,98 triệu của phương án mua dư.
- **Provider ngắt khi đang viết:** trước đây mất cả kế hoạch dù đã kiểm xong. Khi đã có kế hoạch hợp lệ, app trả thẻ đã kiểm cùng lời dẫn dự phòng. Nếu provider lỗi trước khi chốt thì vẫn báo lỗi, không tạo thẻ mua hàng.
- **Giá/tồn kho đổi trước khi trả thẻ:** app trước đây dùng bản Offer lúc đề xuất. Giờ đọc lại Offer mới nhất của ứng dụng trước khi trả kết quả, cập nhật giá nếu còn đạt, hoặc bỏ thẻ nếu hết hàng, giá bất hợp lý, vượt ngân sách hay không còn rẻ hơn như yêu cầu. Đây là kiểm lại dữ liệu Offer hiện có, không phải một lần tải mới toàn bộ trang bán cho mỗi response.

## Cách kiểm và kết quả

| Phạm vi | Kết quả cuối |
|---|---:|
| Toàn bộ backend, build sạch `sh mvnw -q clean package` | 372 đạt; 0 fail/error/skip |
| Trong đó test tư vấn nâng cấp | 57 đạt, thêm 12 test hồi quy |
| Test bộ đối chiếu đầu ra Python | 33 đạt |
| Luồng API thật, cùng một JAR | 12/12 bước đạt |
| Đối chiếu trang bán độc lập | 7 quan sát giá trên 6 trang; sai số 0đ |

12 bước gồm phạm vi laptop, nâng GPU RTX, RTX rẻ hơn, giảm ngân sách xuống 3 triệu, tăng lại lên 10 triệu, chuyển sang chỉ nâng RAM 32GB, SSD cho máy văn phòng dùng HDD, DDR5 trên bo hai khe với hai mức ngân sách, RAM chưa rõ loại, CUDA cần NVIDIA 12GB và ngân sách GPU 300 nghìn.

Đã đọc các reply/thẻ của lượt cuối. Trang bán được chụp độc lập với parser Java và xem tên/biến thể, giá chính, dấu hiệu còn hàng hoặc nút mua. Kit DDR5 2×16GB tính tiền một kit, hiển thị hai thanh. Độ lệch thời điểm chụp trang và `checkedAt` lớn nhất là 138,250 giây.

Lượt đầu `upgrade-live-20261111-followup-round1` bị loại sau đối chiếu thủ công: bộ chấm cũ chỉ kiểm RAM tối thiểu nên bỏ lọt 48GB cho yêu cầu đúng 32GB. Đã thêm giới hạn tối đa, test cho bộ chấm và test hồi quy ứng dụng. Không lấy kết quả đạt từ lượt đầu ghép vào lượt cuối.

## Bằng chứng và phạm vi

- Kịch bản chạy lại: `tools/upgrade_followup_cases.json`.
- Kết quả máy đọc: `docs/upgrade-followup-validation-20261111.json`.
- Bằng chứng: `/Users/yuuki2/Documents/PCsmartcare-testloop/upgrade-live-20261111-followup-final`, gồm request/job từng bước, summary, trang bán, giá đã xem, đối chiếu từng dòng và SHA-256.
- JAR và source khớp hash sau lượt chạy; backend kiểm thử cổng 8084 đã tắt. Không khởi động lại backend demo của người dùng.

Gián đoạn provider và thay đổi Offer được kiểm bằng mock để tái hiện ổn định; các bước API sử dụng provider/tìm kiếm cửa hàng thật. Chưa kiểm GUI trong lượt này, chưa lắp máy hay chạy benchmark. Các câu ngôn ngữ tự nhiên khác và giá/tồn kho trong tương lai vẫn cần kiểm riêng; kết quả này xác nhận bộ ca đã nêu, không phải bảo đảm mọi tình huống đều không còn lỗi.
