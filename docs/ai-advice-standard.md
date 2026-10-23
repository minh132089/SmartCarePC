# Chuẩn tư vấn và kiểm chứng giá của PC SmartCare

Quy tắc này áp dụng cho `ChatService`, `ProductSearch`, `PriceEngine` và bộ đo ở `tools/evaluate_advice.py`. AI đề xuất; code kiểm tra và quyết định phương án nào được hiển thị.

## 1. Nhu cầu và phương án

1. Đọc yêu cầu mới nhất của người dùng trước; yêu cầu mới về hãng, kiểu vỏ hoặc ngân sách ghi đè yêu cầu cũ. Không suy hãng GPU từ hãng CPU. Lượt mua máy đầu tiên thiếu ngân sách hoặc mục đích thì chỉ hỏi thông tin còn thiếu, không tự gán nhu cầu để dựng máy.
2. Giữ yêu cầu nói rõ của người dùng. Game thông thường ưu tiên VRAM từ 8GB; game nhẹ 6GB; học AI/deep learning từ 12GB và NVIDIA. Với game chốt NVIDIA, không ghi rõ VRAM, chỉ cân nhắc 6GB khi mọi card 8GB cùng hãng đã tìm được vẫn làm phương án vượt trần. Nêu giới hạn 6GB trên thẻ.
3. Trần giá: Rẻ tối đa 90% ngân sách, Phù hợp tối đa ngân sách, Chất lượng tối đa ngân sách + 3 triệu đồng. Tính theo giá mua của từng sản phẩm nhân số bộ phải mua, không nhân đôi giá của kit RAM. Quạt bán theo bộ cũng tính số bộ mua bằng cách làm tròn lên số quạt sử dụng / số quạt mỗi bộ; ghi rõ quạt dư, không nhân giá bộ ba quạt thành ba bộ khi chỉ dùng ba quạt. Dòng so sánh phải ghi số bộ mua và số thanh khi sản phẩm là kit; giao diện ghi đơn vị thanh, không thêm “×2” sau tên kit 32GB làm người đọc hiểu nhầm thành 64GB.
4. Mỗi phương án phải có linh kiện khởi động được, đúng loại và biến thể, lắp chung được và khớp mẫu hướng dẫn 3D. [SPARKLE B580 TITAN OC SB580T-12GOC](https://www.sparkle.com.tw/files/20241203213858182.pdf) có ba quạt, dài 315mm; không được gán mẫu GPU 240mm/hai quạt dùng cho vỏ nhỏ chỉ vì tên trang bán không ghi số quạt. Khi chưa có kích thước card/khoảng trống đúng mã vỏ, phải hiện điều kiện GPU_CLEARANCE; số quạt chưa đủ để kết luận vừa vỏ. Quạt lắp thêm cũng cần đếm vị trí còn trống sau quạt có sẵn của vỏ (FAN_MOUNTS). Phải có ít nhất một phương án trong ngân sách gốc mới tính là đã giải quyết yêu cầu mua; chỉ có thẻ Chất lượng vượt ngân sách chưa đủ. Không có phương án đạt thì nêu linh kiện hoặc số tiền còn thiếu.
5. Bước chốt thẻ phải kiểm lại hãng/model GPU, VRAM, tổng RAM và SSD theo tên sản phẩm thật và số lượng. Tự sửa không thành công thì ẩn thẻ, không cho bản thiếu yêu cầu lọt qua vì đã lắp chung được. Yêu cầu mới “không cần card rời” gỡ model và VRAM cũ; không nhận nhầm “không chơi game” thành nhu cầu chơi game.
6. Không ghép Athlon / Celeron / Pentium vào cấu hình game/GPU của bộ tư vấn hiện tại. Khi người dùng chốt card AMD/Intel cho học sâu, app chưa có kiểm chứng framework tương ứng thì giải thích giới hạn đó, không tự đổi sang NVIDIA.
7. Phương án Chất lượng phải có nâng cấp xác định được: CPU/GPU cao hơn theo bảng đang hỗ trợ, RAM/SSD lớn hơn hoặc bảo hành dài hơn đã đọc trên trang của cả hai sản phẩm. Đổi cửa hàng, tên khác cho cùng chip, nguồn nhiều watt hơn hoặc RAM hãng khác cùng dung lượng chưa đủ chứng minh nâng cấp. Card chậm hơn nhưng nhiều VRAM hơn không tự được gắn nhãn nâng cấp hiệu năng. Không gắn nhãn Chất lượng cho phương án hạ GPU theo bảng xếp hạng đã biết, kể cả đổi SSD lớn hơn hoặc bảo hành dài hơn.
8. Ngân sách là mức tối đa. Khi người dùng nói “rẻ nhất”, ưu tiên bản Rẻ đủ nhu cầu, giá đã kiểm trang và còn hàng. Nếu không có yêu cầu đó, bản Rẻ được ưu tiên khi tiết kiệm ít nhất 20% so với Phù hợp, giữ cùng model CPU, chip GPU/VRAM, tổng RAM và không giảm bảo hành đã biết của ba món này. Những linh kiện khác vẫn phải qua kiểm tra yêu cầu. Với tác vụ AI trên GPU và người dùng chưa chốt CPU, cũng ưu tiên bản Rẻ tiết kiệm ít nhất 20% khi cùng chip GPU/VRAM, không giảm tổng RAM/SSD, CPU thuộc nhóm 6 nhân phổ thông đã hỗ trợ và giá/hàng đã kiểm. Giải thích CPU/nềnRAM/bảo hành có thể khác; không tự hứa tốc độ chạy mô hình. UI chọn mặc định và nhãn Gợi ý phải theo thẻ chính backend trả về. Mọi phương án vượt ngân sách gốc phải ghi rõ khoản vượt và không gắn nhãn Gợi ý. Số tiền chênh dùng giá thật, không làm tròn tới 100.000đ. Không giữ số tiền tiết kiệm do AI tự nêu cho thay đổi chưa được kiểm tra.
9. Ghi chú phương án bị ẩn do code tạo được ghép riêng vào câu trả lời. Không giữ ghi chú kiểu đó chỉ vì AI viết chữ “app”. Hỏi lại lý do thì lấy số ở ghi chú cũ, không để AI tự ước lượng lại ngân sách.
10. Giữ cả ngân sách dương dưới 3 triệu để kiểm trần; không bỏ qua vì khó dựng máy. Khi chưa dựng được cấu hình hoặc lấy được báo giá, không chuyển giá ước lượng của AI thành giá thị trường hay mức ngân sách thay thế. Nêu rõ chưa xác minh được phương án và giá bán.

### Hồ sơ thiết kế đồ họa và dựng nội dung

Thiết kế đồ họa, Photoshop, Illustrator, Lightroom và dựng nội dung được nhận diện riêng với văn phòng và game. Khi người dùng chưa yêu cầu bỏ card, app chọn card rời từ 6GB VRAM cho hồ sơ 2D; Blender/3D/render, After Effects, DaVinci hoặc video 4K từ 8GB. Đây là sàn chọn cấu hình trong danh mục đang hỗ trợ của app, không phải mức tối thiểu chung của mọi phần mềm. Thiếu ngân sách hoặc hàng thì giải thích và giữ chặn thẻ thiếu GPU, không tự biến máy thiết kế thành máy văn phòng. Game kết hợp làm nội dung phải giữ cả hai nhu cầu; không áp dụng bước hạ8→6GB dành riêng cho game chốt NVIDIA lên hồ sơ sáng tạo 8GB.

Người dùng nói rõ không mua card rời vẫn được chọn CPU có GPU tích hợp; lời tư vấn phải nêu điều kiện kiểm phần mềm, phiên bản, driver và tác vụ, chưa hứa tăng tốc hoặc render dự án thực tế. GPU tích hợp không tự động đồng nghĩa đủ mọi tác vụ đồ họa. Đổi từ văn phòng không card sang thiết kế ở lượt mới phải xét lại GPU; giữ đúng yêu cầu bỏ card nếu nó nằm trong lượt mới nhất.

Đối chiếu nguồn chính thức ngày 24/10/2026: [Photoshop 27.x](https://helpx.adobe.com/photoshop/desktop/get-started/technical-requirements-installation/adobe-photoshop-on-desktop-technical-requirements.html) yêu cầu khả năng DirectX 12/bộ nhớGPU, [Illustrator](https://helpx.adobe.com/illustrator/desktop/get-started/learn-the-basics/technical-requirements.html) nêu điều kiện tăng tốcGPU; không được suy thành mọi máy 2D bắt buộc card rời. [Blender](https://www.blender.org/download/requirements/) khuyến nghị 8GB VRAM; CyclesGPU còn có điều kiện riêng theo backend và GPU.

## 2. Tìm sản phẩm và chứng cứ

Kit RAM không ghi “2×16GB” trong tên vẫn phải đọc theo đúng mã sản phẩm. [G.SKILL xác nhận F5-6000U4040E16GX2-TZ5K / F5-6000U4040E16GA2-TZ5K là bộ 32GB gồm 2 thanh 16GB](https://www.gskill.com/specification/165/371/1636011207/F5-6000U4040E16GX2-TZ5K-EOL-F5-6000U4040E16GA2-TZ5K-EOL-Specification). Mua một bộ phải hiện hai thanh; không nhân đôi giá một kit. [Kingston KF432C16BB1AK2/32 cũng là kit hai thanh 16GB](https://www.kingston.com/dataSheets/KF432C16BB1AK2_32.pdf). Phải giữ mã trên h1 khi metadata SEO lược bớt và cập nhật tên hàng dù giá không đổi.

Số thanh RAM phải không vượt số khe của đúng mã bo. Hiện có dữ kiện hãng cho [MSI PRO H610M-E DDR4: 2 khe](https://www.msi.com/Motherboard/PRO-H610M-E-DDR4/Specification), [MSI PRO B760M-E DDR4: 2 khe](https://www.msi.com/Motherboard/PRO-B760M-E-DDR4/Specification), [GIGABYTE H610M-H V3 DDR4: 2 khe](https://www.gigabyte.com/vn/Motherboard/H610M-H-V3-DDR4-rev-10/sp), [B760M GAMING PLUS WIFI DDR4: 4 khe](https://www.gigabyte.com/Motherboard/B760M-GAMING-PLUS-WIFI-DDR4-rev-10/sp), [B760 GAMING X DDR4: 4 khe](https://www.gigabyte.com/Motherboard/B760-GAMING-X-DDR4-rev-10/sp). Không suy số khe từ chipset; model chưa có dữ kiện vẫn cần xác nhận. Khi đổi bo để đủ khe phải kiểm lại socket, DDR, kích cỡ và giá.

1. `PriceEngine` tìm trên trang cửa hàng và SearXNG; `ProductSearch` bổ sung Google Shopping/Serper khi nguồn chính quá ít. Kết quả đã đọc đúng trang sản phẩm được xếp trước giá ở trang danh sách.
2. Cùng model chưa chắc cùng biến thể: phải kiểm dung lượng, hậu tố Ti/F, revision và kiểu bộ kit. Link phải dẫn tới trang sản phẩm, không dùng trang chủ, trang danh mục hoặc kết quả Google làm link mua.
3. `verified=true` chỉ khi đã đọc trang sản phẩm trong hạn 10 phút, tên trên trang khớp đúng biến thể, giá dương và trang không báo hết hàng. Kết quả tìm kiếm có thể lưu 2 giờ để gợi ý URL, nhưng giá trên trang phải được kiểm lại sau 10 phút. Giá từ trang tìm kiếm, snippet hoặc trang không đọc được là **giá tham khảo**, dù có số tiền và link. Tình trạng hàng không ghi rõ là `null`, không suy thành còn hàng. PreOrder/BackOrder không được coi là có sẵn để mua. Nếu trang ghi riêng giá mua online, dùng giá đó thay metadata cũ hoặc giá combo có điều kiện; nhiều giá online mâu thuẫn thì chưa xác minh.
4. Giá trên thẻ được thay bằng giá đọc từ trang sản phẩm khi chứng cứ trên đạt. Giá tham khảo vẫn có thể hiện với `priceChecked=false` và điều kiện phải xem lại trước khi mua. Không gọi đó là giá bán đã xác nhận.
5. Khi thay sản phẩm, kiểm lại giá, tồn kho, biến thể, ngân sách, tương thích và ghi chú đánh đổi. Thông số hiển thị phải lấy từ sản phẩm thật, số thanh đã tính và bảo hành trang bán; không giữ Gen/công suất/bảo hành do AI tự viết khi chưa có dữ kiện. Không tuyên bố là giá rẻ nhất thị trường nếu chưa có bằng chứng bao phủ thị trường.
6. Khi nguồn tìm kiếm lỗi, dùng URL đã biết làm gợi ý để đọc lại trang bán. Chỉ dùng giá sau khi trang được xác minh còn hạn; không lấy giá cũ hết hạn làm giá hiện tại.

## 3. Đo nghiệm thu

`tools/evaluate_advice.py` nhận bộ nhãn độc lập và phản hồi thật đã lưu. Một ca **đúng quyết định** khi các tier mong đợi có mặt, không vượt trần, card đúng hãng/VRAM được yêu cầu, mọi dòng có link sản phẩm, giá và trạng thái cần xác minh, không có lỗi lắp. Một dòng **đúng giá** khi URL trùng trang đã chụp, app xác nhận đúng biến thể theo `OfferEvidence`, và giá hiển thị bằng giá chụp độc lập trên trang cửa hàng ở cùng thời điểm (sai số 0 đồng). Nhãn giá và lần kiểm trang của app phải cách nhau không quá 10 phút. Tên trang được giữ trong nhãn để người kiểm tra rà lại bằng mắt; không so chuỗi tên tuyệt đối vì cửa hàng có thể viết khác nhau cho cùng model.

Mỗi phản hồi còn cần một người chấm độc lập đánh giá **chất lượng lời tư vấn** theo bốn điểm: tóm tắt đúng nhu cầu và giả định, giải thích lựa chọn quan trọng bằng dữ liệu của thẻ, nêu đánh đổi liên quan, không có câu tự mâu thuẫn hoặc lời dặn chung lặp vô ích. Chỉ ghi `adviceQuality.pass=true` khi cả bốn đạt; lưu lý do trong `notes` nếu không đạt. Bộ chấm không tự đánh giá ngữ nghĩa chỉ bằng từ khoá.

Mốc ban đầu: ít nhất 100 ca đa dạng và 100 lần chụp giá độc lập theo URL/thời điểm, tỷ lệ đúng quyết định ≥98%, lời tư vấn đạt ≥98% và giá đúng ≥98%. Theo yêu cầu tiếp theo của người dùng, nghiệm thu hiện tại dùng `--zero-issues`: **cả ba chỉ tiêu phải đạt 100%, không có lỗi được ghi nhận trên bộ kiểm tra**. Một chỉ tiêu còn trượt một ca cũng không đạt. Giữ nguyên đề bài và nhãn kỳ vọng; không đổi ca thiếu hàng thành ca được phép từ chối để tăng điểm. Báo riêng số ca thiếu nhãn hoặc trang không đọc được; thiếu bằng chứng thì chưa được tuyên bố đạt. Bộ ca phải có game, game nhẹ, AI, văn phòng, vỏ ITX, lỗi chính tả hãng, hàng hết, khác biến thể, thay đổi giá và ngân sách sát sàn. Giữ một tập ca chưa dùng để chỉnh quy tắc để kiểm tra cuối. Đây là kết quả trên bộ kiểm tra, không phải bảo đảm mọi đầu vào và mọi thời điểm đều không có lỗi.

Ví dụ một mục trong file nhãn JSON:

```json
{
  "id": "game-nvidia-20m-01",
  "run": "game-nvidia-20m-01.json",
  "expected": {"tiers": ["balanced", "quality"], "budgetVnd": 20000000, "gpuVendor": "nvidia", "minGpuVramGb": 6},
  "adviceQuality": {"pass": true, "notes": "Nêu đúng ưu tiên NVIDIA, giới hạn 6GB và đánh đổi."},
  "prices": [
    {"url": "https://shop.example/vga-3050", "title": "VGA RTX 3050 6GB", "unitPriceVnd": 5990000,
     "capturedAt": "2026-10-23T10:00:00+07:00"}
  ]
}
```

`run` là phản hồi JSON của app (`result.build` / `result.options`). Nhãn giá phải do người kiểm tra đọc từ trang cửa hàng sau lượt chạy, không sao chép giá do app hoặc AI xuất ra; chỉ đặt `reviewed=true` khi đã đọc lại giá và đúng phiên bản của sản phẩm. Chạy `python3 tools/evaluate_advice.py --gold <nhãn.json> --runs <thư_mục_kết_quả> --zero-issues`; lệnh trả mã lỗi cho đến khi đủ mẫu và đạt cả ba chỉ tiêu. Việc agent tự đối chiếu là một kết quả đánh giá nội bộ, cần nói rõ người chấm và phạm vi, không gọi đó là kiểm định độc lập từ khách hàng.

## 4. Chạy đối chiếu trực tiếp

`python3 -B tools/check_advice_live.py --cases tools/advice_validation_cases.json --output /tmp/pcsmartcare-advice-runs`

Bộ ca có lịch sử hội thoại cố định để kiểm tra đổi hãng, đổi mục đích, bỏ card cũ và giải thích các mức tiền đã nêu ở lượt trước. Giá trong lịch sử là dữ liệu của lượt cũ, không phải báo giá hiện tại. Các ca còn lại lấy cấu hình và giá mới từ backend local. Mỗi job có giới hạn chờ 600 giây ở công cụ kiểm tra; timeout được báo lỗi, không tính là ca đạt.

“PASS ràng buộc” chỉ xác nhận các điều kiện đã mã hóa. Công cụ báo riêng ca chưa tìm được cấu hình, để không biến một loạt câu từ chối an toàn thành tỷ lệ tư vấn thành công. Phần lời tư vấn, giá độc lập và tính đáng tiền vẫn phải được đọc và chấm. Chạy backend bằng một JAR cố định trong vòng đối chiếu để tránh thay lớp đang chạy khi biên dịch test; job nền phải chuyển sang lỗi nếu phát sinh Error, không để giao diện chờ mãi.
