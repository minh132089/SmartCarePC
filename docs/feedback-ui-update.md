# Giao diện phản hồi và hồ sơ người tham gia

- Cloudflare Pages (`*.pages.dev`): mở form tên, tuổi (số nguyên 1–120), nghề nghiệp trước khi sử dụng; nghề nghiệp có gợi ý Học sinh và Sinh viên, vẫn cho nhập tự do. Slide Commit xác nhận sau khi điền đủ. Có mô tả việc gắn hồ sơ vào phản hồi.
- Sau khi kéo xác nhận, thanh hiện viên xanh lá, dấu tích và “Đã xác nhận” trong 1,4 giây rồi mới đóng popup; timer được hủy khi form unmount.
- Localhost, demo LAN: bỏ qua form bắt buộc. `/feedback` dùng khóa xem dữ liệu riêng và không mở form hồ sơ.
- Hồ sơ lưu trong trình duyệt ở `pcsc-participant-v1`. Khi gửi feedback, đính kèm `participant_name`, `participant_age`, `participant_occupation` vào JSON `answers`. CSV hiện có tự xuất ba cột tương ứng; không cần thay bảng database. Hàng đợi giữ hồ sơ tại thời điểm gửi.
- Dashboard `/feedback`: Magic Bento với số phản hồi, điểm sao, số phản hồi có hồ sơ, biểu đồ ngày, phân bố sao, điểm từng câu, nghề nghiệp, nhóm tuổi; danh sách hiện tên/tuổi/nghề nghiệp. Mỗi biểu đồ đếm phản hồi, không suy diễn số người duy nhất. Ngày thống kê theo thời gian do backend ghi, chưa có thông tin múi giờ.
- Loader Lattice giữ thời gian trong lúc cập nhật bước xử lý. Spring Check dùng cho đánh dấu từng bước lắp, tách khỏi nút mở nội dung bước.

Nguồn component ReactBits (bản TypeScript/Tailwind lấy qua registry `https://reactbits.dev/r/{Name}-TS-TW.json`):

- [Lattice Loader](https://reactbits.dev/micro/lattice-loader)
- [Slide Commit](https://reactbits.dev/micro/slide-commit)
- [Spring Check](https://reactbits.dev/micro/spring-check)
- [Magic Bento](https://reactbits.dev/c/components/magic-bento)

Slide Commit được bổ sung Enter/Space, bắt đầu kéo đúng vị trí con trỏ, xác nhận ở 94% cuối hành trình và không xác nhận khi pointer bị hủy. Nhãn thanh kéo lấy màu tương phản từ track. Lattice Loader được chỉnh thông báo tiếng Việt và cho trạng thái dài xuống dòng. Giữ font và token màu của SmartCarePC, tắt tilt, magnetism và hạt sáng trong dashboard để dễ đọc dữ liệu. Các component hỗ trợ reduced motion.

Kiểm tra: `npm run build`, `npx oxlint` trên các file thay đổi, `node tools/check_feedback_ui.mjs`, và `cd backend && sh mvnw -q -Dtest=FeedbackServiceTests,FeedbackControllerTests,FeedbackDedupTests test`.

Kết quả 25/10/2026: build production và build preview đạt; luồng JavaScript đạt; 14 test backend đạt, không lỗi hoặc bỏ qua. Trong môi trường chặn self-attach của Mockito, chạy Maven thêm `-DargLine=-javaagent:/Users/yuuki2/.m2/repository/org/mockito/mockito-core/5.20.0/mockito-core-5.20.0.jar`. Lint không có error; mã animation ReactBits có cảnh báo về refs và async effect hiện có có cảnh báo effect. Tách router sang Root.tsx để entrypoint không khai báo component lazy, tránh Fast Refresh gọi lại createRoot. Kiểm tra trực tiếp: kéo và bàn phím xác nhận form, khung hẹp 360px, loader, checkbox và tiến độ 3D; kiểm tra dashboard với dữ liệu giả lập cách ly. Console bản Cloudflare không có lỗi trong các trang đã xem.

Ngày 26/10/2026, người dùng cho phép mở tunnel công khai để test AI và feedback thật. Bản test https://ui-preview.smartcarepc.pages.dev/ dùng build thường, proxy `/api` qua Cloudflare Tunnel tới Vite preview (4173), rồi backend local (8083). Dùng HTTP/2 cho tunnel sau khi QUIC rớt kết nối. Địa chỉ tạm được ghi trong file bỏ qua Git `functions/_backend.js`; khi đổi tunnel cần deploy lại Pages branch `ui-preview`. Dashboard thật ở `/feedback`, vẫn yêu cầu khóa; `/feedback-preview` và badge dữ liệu minh họa chỉ hoạt động khi build riêng với `VITE_UI_PREVIEW=true`. Production chưa được cập nhật. Máy chạy backend và tunnel phải còn mở trong lúc test.

Loader đang xử lý không còn khung nền/viền; tin nhắn trả lời vẫn dùng bong bóng. Prompt yêu cầu trả lời ngắn và không hỏi thêm ngân sách khi chỉ giải thích kiến thức. Khi mở mô hình từ thẻ tư vấn, viewer giữ cấu hình đã chọn; cấu hình dùng iGPU bỏ GPU, dây PCIe và bước lắp GPU, chỉ cổng màn hình trên bo mạch. AI nhận nội dung chuẩn của bước đang xem, quy tắc khe RAM và các món đã chọn. Ghi chú chênh lệch số lượng với máy mẫu vẫn được hiển thị.

Kiểm tra bổ sung: `node tools/check_guide_context.mjs`, build, lint và 203 test backend thuộc ChatServiceTests, ModelMatchServiceTests, ChatControllerTests cùng ba suite feedback đạt. Cache giá được giữ nguyên và sao lưu tại `/tmp/pcsmartcare-price-cache-before-ui-20261026.json`; backend nạp lại 70 từ khóa và 498 link sau khi khởi động.
