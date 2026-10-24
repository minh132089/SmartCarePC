# Sửa lỗi follow-up RTX, RAM và trang bán — 24/10/2026

## Lỗi được khách phát hiện

- Câu “mình muốn sử dụng 16gb vẫn dùng RTX cơ” từng trả AMD RX dù khách đã chọn NVIDA/NVIDIA.
- Ảnh RTX 3060 lấy nhầm thumbnail quà tặng từ kết quả tìm kiếm.
- Link Lexar THOR 32GB (2×16GB) tại Phúc Anh đã ngừng kinh doanh nhưng metadata SEO vẫn ghi 1.999.000đ và InStock. Bộ đối chiếu cũ đã nhận sai bằng chứng này.

**Thu hồi kết luận nghiệm thu giá trước đây.** Các con số 48/48 trong báo cáo hỏi thêm context chỉ là kết quả của bộ chấm cũ, không chứng minh sản phẩm còn mua được.

## Thay đổi

1. Nhận RTX/GTX như yêu cầu NVIDIA; bỏ phần phủ định hãng trước khi phân tích câu có cả NVIDA và “chưa ưa chuộng VGA nhà AMD”. Yêu cầu RAM 16GB ở tin mới ghi đè RAM 32GB cũ, không nhầm dung lượng VRAM hoặc SSD.
2. Tên, giá và ảnh ưu tiên trang sản phẩm đã xác minh. Không giữ thumbnail tìm kiếm khi trang có ảnh khác; ảnh lỗi có chỗ giữ ghi “Chưa có ảnh” và thử lại khi URL đổi.
3. Trạng thái ngừng bán hiển thị thắng schema cũ. CTA đăng ký chờ hàng hoặc chỉ liên hệ chưa xác nhận được tồn kho. Giá kèm main/cả bộ chưa kiểm điều kiện không được cộng như giá bán lẻ.
4. Làm mới cache theo phiên bản parser; lần đọc mới thiếu giá/tồn kho phải xoá chứng cứ cũ. Khi engine tìm giá hoạt động, mọi dòng trên thẻ mua hàng phải có giá trang đã xác minh; không dùng giá tìm kiếm thay thế rồi chỉ gắn cảnh báo. Bộ chấm giá cũng yêu cầu nhãn tồn kho độc lập và từ chối giá rỗng.
5. Không lấy giá dung lượng nhỏ cho SSD có nhiều biến thể; yêu cầu M.2/NVMe rõ ràng cho mô hình ổ M.2. Bổ sung nguồn tìm SSD từ hai trang danh mục TPassion được phép đọc, vẫn xác minh từng trang mua.
6. Máy đồ họa vượt ngân sách có card rời có thể chọn bản F/KF cùng dòng Intel, hoặc các i5 LGA1700 12400F/13400F/14400F có ít nhất sáu nhân, khi khách không khoá CPU. So giá trang thực tế; không hạ máy 3D xuống i3. Ghi rõ thay CPU và hiệu năng có thể khác. Không hỏi lại 2D/3D khi khách đã ghi Photoshop/2D hoặc đã trả lời nhu cầu.

## Cách kiểm

- Chạy toàn bộ 290 test backend: 0 failure, 0 error, 0 skipped, 18 suite có file nguồn tương ứng. Lượt cuối dùng một JAR cố định, có SHA-256, tách khỏi thư mục biên dịch.
- Các fixture dương kiểm ngân sách được bổ sung trang giả có giá/tồn kho xác minh, giữ nguyên giá và thông số. Ba ca âm từng cho hiện giá tham khảo (chưa đọc trang, sai biến thể, sai đơn vị) nay phải không hiện thẻ. Ca không hạ GPU để lấy SSD lớn vẫn kiểm không có tier chất lượng sai và giữ RTX 4060; không yêu cầu ẩn cả tier rẻ hợp lệ được tạo thêm.
- 23 test bộ chấm Python; frontend TypeScript/Vite build và oxlint cho Proposal.tsx.
- Ba ca API mới: đổi AMD sang NVIDA; follow-up RAM 16GB + RTX dùng toàn bộ câu trả lời mới của ca đầu; Photoshop/2D + RAM 32GB gồm 2×16GB + SSD 1TB + RTX.
- Ca đầu dùng các câu khách đã nhập, chép từ giao diện. Hai câu trả lời assistant trước đó là đoạn chép tóm tắt; đây không phải bản phát lại đầy đủ payload lịch sử.
- Chụp trang cửa hàng độc lập cùng lượt, kiểm tên/dung lượng, giá bán, điều kiện mua và CTA của sản phẩm chính. Giá RAM tính theo số thanh/kit thực mua; không nhân giá bộ quạt theo số quạt sử dụng. Dung sai giá 0đ, cách lần kiểm giá trong app không quá 600 giây.
- Đọc thủ công lời tư vấn và thẻ; kiểm ảnh thực tế RTX 3060 cùng các test ưu tiên ảnh trang sản phẩm.

## Kết quả lượt cuối

Lượt 12: **3/3 ca quyết định và lời tư vấn đạt; 26/26 URL giá khớp, sai số 0đ**. Lần chụp độc lập cách lần kiểm của app tối đa 191,463 giây. Mã nguồn và JAR giữ nguyên trong cả ba job.

| Ca | Phương án chính | Kết quả |
| --- | ---: | --- |
| Đổi từ AMD sang NVIDA |19.918.000đ|NVIDIA RTX 3060, RAM 16GB, SSD 512GB|
| Follow-up 16GB + RTX |19.518.000đ|Giữ RTX 3060 và đúng 16GB RAM|
| Photoshop/2D, 2×16GB, SSD 1TB, RTX |29.628.000đ|Hai thanh 16GB DDR4, RTX 4060, SSD 1TB|

[Ba ca tái lập](../tools/advice_user_followup_cases.json), [tóm tắt](advice-user-followup-validation-summary.json), [mã băm bằng chứng](advice-user-followup-evidence-sha256.json). Trang tản ID-COOLING có giá bán thường 379.000đ và giá Build PC 299.000đ; kiểm tay xác nhận dùng 379.000đ, giữ nguyên bản chụp gốc và ghi lý do trong nhãn.

## Các lượt chưa được nhận

Giữ nguyên bằng chứng từng lượt; không ghép kết quả đạt từ các phiên bản khác nhau:

| Lượt | Lý do chưa được nhận |
| --- | --- |
| 1 | Hai ca 20 triệu không có phương án trong ngân sách. |
| 2 | Giá SSD nhiều dung lượng bị lấy theo bản nhỏ và còn vượt ngân sách. |
| 3 | Cache làm mới timestamp nhưng giữ giá cũ khi lần đọc mới không rõ biến thể. |
| 4 | Kiểm cấu hình tự động đạt, kiểm tay phát hiện hai trang An Phát chỉ đăng ký chờ. |
| 5 | Hỏi lại 2D/3D dù khách đã ghi Photoshop/2D. |
| 6 | Kiểm cấu hình tự động đạt, kiểm tay phát hiện giá CPU kèm B760 và nguồn chỉ liên hệ. |
| 7 | CPU Phong Vũ chỉ bán kèm trọn bộ; ca follow-up không có phương án trong ngân sách. Tunnel 503 gián đoạn lấy kết quả, đã lấy lại đúng job ID qua localhost. |
| 8 | FPT Kingston NV1 đang sắp về; Samsung 980 PRO chỉ chờ tư vấn. Form báo khi có hàng phải thắng metadata InStock. |
| 9 | SSD nhiều dung lượng đã bị parser từ chối nhưng vẫn lên thẻ bằng giá tìm kiếm chưa xác minh. Bổ sung chặn tại điều kiện hiển thị thẻ. |
| 10 | Đã chặn giá chưa xác minh; hai ca 20 triệu vẫn thiếu phương án trong ngân sách vì chọn 14400F. Bước cứu máy đồ họa chưa thử các i5 sáu nhân cùng socket rẻ hơn khi CPU ban đầu đã là bản F. |
| 11 | Đổi CPU đã giảm giá nhưng bước ép giá lại đưa SSD chưa rõ biến thể vào. Sửa cheaperLinked bắt buộc giá trang hợp lệ; phép kiểm đổi linh kiện cũng từ chối trang đã đọc nhưng không đủ chứng cứ. |

## Phạm vi

Kết quả lượt cuối ghi trong [tóm tắt máy đọc được](advice-user-followup-validation-summary.json). Bằng chứng API, bản chụp gốc và mã băm được lưu ngoài repo tại thư mục được ghi trong tóm tắt. Dữ liệu này là hồi quy nội bộ cho lỗi khách báo, không phải phép ước lượng 98% hay 100% cho mọi cuộc hội thoại.

Tồn kho là trạng thái cửa hàng quảng cáo tại thời điểm kiểm, không bảo đảm tồn kho vật lý. Cấu hình vẫn có điều kiện cần cửa hàng xác nhận về BIOS, bộ gá tản và khoảng trống GPU. Chưa lắp máy hoặc benchmark thực tế.
