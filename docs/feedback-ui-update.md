# Giao diện phản hồi và hồ sơ người tham gia

- Cloudflare Pages (`*.pages.dev`): mở form tên, tuổi (số nguyên 1–120), nghề nghiệp trước khi sử dụng; nghề nghiệp có gợi ý Học sinh và Sinh viên, vẫn cho nhập tự do. Slide Commit xác nhận sau khi điền đủ. Có mô tả việc gắn hồ sơ vào phản hồi.
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

Preview Cloudflare: https://ui-preview.smartcarepc.pages.dev/ và `/feedback-preview` (dashboard có nhãn dữ liệu minh họa). Bản build có `VITE_UI_PREVIEW=true`; deployment được đóng gói trong `/tmp/pcsmartcare-ui-preview`, API trả JSON 503 cố định, không proxy tới máy local. Không mở tunnel theo lựa chọn người dùng. Trang chính production chưa được cập nhật; bản preview này không lưu feedback lên máy chủ. Hồ sơ trên form chỉ lưu trong trình duyệt; chức năng gắn vào feedback thật đã được kiểm bằng test backend và luồng gửi giả lập.
