# Đặc tả: Hướng dẫn bảo trì + Nhắc bảo trì

Viết 2026-10-29 để giao cho agent khác làm. Đọc `HANDOFF.md` trước, rồi file này.

## Vì sao cần

Proposal (BTEC Unit 16) có câu hỏi **Q3**: "Nhắc bảo trì đến đúng lúc có làm người chưa từng vệ sinh máy bắt tay làm không?".
Section 2 của proposal nói rõ vấn đề: keo tản nhiệt khô và bụi chặn gió theo thời gian (Due and Robinson, 2013). Lỗi chỉ lộ
khi máy chạy nặng, lúc rảnh máy vẫn trông bình thường nên chủ máy tưởng máy "già". Hiện app **chưa có gì** cho phần này:
"thermal paste" chỉ có ở bước lắp máy mới (`src/data/guide.ts`, `coolerStep`), không có bài vệ sinh, không có nhắc.

### Bám proposal đã duyệt (bản có chữ ký thầy: `~/Downloads/Reasearch Proposal form _ CHU HUY MINH (2).pdf`)

Không đổi logic proposal. Những điểm phần này phải phục vụ:
- **Q3** đúng nguyên văn: "Does a maintenance reminder arriving at the right moment get owners to act, when those owners have never cleaned or serviced anything before?"
  → phải biết ai **chưa từng vệ sinh / bảo dưỡng** (nhóm chính của Q3), có được nhắc không, nhắc xong **có làm không**.
- **Objective / gap effect**: đo việc người đó **tự làm được một mình** trước và sau hướng dẫn. Responsibilities ghi thử nghiệm 4 tuần đo
  "how much of each task is finished alone" và "whether the machine is maintained, both before the guidance and after it"
  → ghi **tình trạng bảo trì lúc đầu** (B1 ở lần đầu = mốc trước) và **tự làm hay nhờ người khác** khi làm xong (xem B3).
- **Section 2**: hướng dẫn "shaped around one person", đi từng bước "at the moment they are standing there doing it", mục tiêu là việc vốn nhờ người khác thì chủ máy tự làm xong
  → bài bảo trì phải theo **đúng máy của người đó** (xem A3), từng bước một lúc đang làm.
- **Q4**: so người không rành và người đã rành kỹ thuật → mọi sự kiện phải gắn được với nhóm nền tảng của người tham gia (câu này thuộc form v2, sự kiện chỉ cần mang mã người tham gia + phase để ghép sau).
- Đề tài lấy con người làm trung tâm, không phải đề tài AI: không giới thiệu phần bảo trì như tính năng AI, khung chat chỉ là phụ trợ.
- "Report what came out of every group": không bịa hạn để lấy dữ liệu, không lọc bỏ người không làm (xem B4).

Cần làm 2 phần, làm phần A trước vì phần B dẫn người dùng vào phần A:
- **A. Hướng dẫn bảo trì từng bước** (có 3D), dùng lại khung hướng dẫn lắp máy đang có.
- **B. Nhắc bảo trì**: tính lúc nào cần làm, nhắc người dùng kể cả khi họ không mở app, và ghi dữ liệu cho Q3.

## Quy ước phải giữ

- Chữ trên giao diện bằng **tiếng Anh** (app đã chuyển sang tiếng Anh ở commit edbbf03). Chú thích trong code giữ tiếng Việt như code xung quanh.
- Viết cho người **không rành kỹ thuật**: mỗi bước một việc, nói "để làm gì", dấu hiệu làm đúng, cảnh báo an toàn. Không dùng tên hãng thật.
- Dùng lại cấu trúc có sẵn, không viết khung mới: `GuideStep` (`src/data/guide.ts`), `machineOf` (tản khí / thấp / nước, nguồn ATX / SFX, bo, riser),
  `GuidePanel.tsx`, `loadDone` / `saveDone`, store `useViewer` (`guide`, `guidePart`, `setGuideIndex`), thứ tự tháo trong `src/data/assembly.ts` (`remove`).
- localStorage luôn bọc try/catch như `src/ui/study/study.ts` (ẩn danh hoặc chặn cookie thì chỉ sống trong phiên).
- **Không thu email / số điện thoại** ở bản này: khảo sát đã hứa ẩn danh (lỗi đã ghi trong ASM1: câu F3 lại thu liên hệ). Nhắc ngoài app làm bằng lịch (xem B2).
- Kiểm tra trước khi commit: `npx tsc -b && npx oxlint src && npm run build`, backend `cd backend && sh mvnw test` (mvnw không có quyền chạy, phải gọi qua `sh`).
- Nhánh hiện tại `feature/login-feedback-update`, có sẵn thay đổi chưa commit ở `docs/feedback-ui-update.md` và thư mục `deploy/`: đừng đụng / đừng stage chúng.
  Không commit `assets-src/`, `design/photos/`, `tools/__pycache__/`. Khoá chỉ nằm trong `backend/.env` (gitignore).

## A. Hướng dẫn bảo trì

### A1. Ba việc bảo trì

| id | Tên (UI) | Bao lâu một lần | Áp dụng |
|---|---|---|---|
| `dust` | Clean out dust | 6 tháng (3 tháng nếu môi trường bụi, xem B1) | mọi máy |
| `paste` | Replace thermal paste | 2–3 năm, hoặc ngay khi có triệu chứng nóng | tản khí và tản thấp; tản nước AIO: chỉ thay keo khi tháo đầu bơm, KHÔNG mở bơm / ống |
| `check` | Quick health check | mỗi tháng, 5 phút, không mở máy | mọi máy |

### A2. Nội dung từng bước (nháp, agent viết lại cho gọn và đúng giọng các bước lắp)

**dust** (mở máy, tháo ít nhất):
1. Prepare: tắt máy hẳn (Shut down, không Sleep), rút dây nguồn, gạt công tắc nguồn sau máy sang 0, bấm nút nguồn 5 giây để xả điện. Đồ nghề: tua vít Phillips số 2, bình khí nén hoặc máy thổi mini, cọ mềm khô, khăn giấy. Đặt máy chỗ thoáng, ngoài phòng ngủ.
2. Open the side panel (dùng đúng tấm theo vỏ, máy Mini / riser thì có tấm sau). Chạm vào khung kim loại của vỏ trước khi chạm linh kiện.
3. Remove dust filters (trước, nóc, đáy nếu có), rửa nước được nhưng phải khô hẳn mới lắp lại.
4. Fans: **giữ cánh quạt bằng ngón tay** khi thổi (quạt quay tự do có thể sinh điện ngược và hỏng ổ bi), thổi từ trong ra ngoài vỏ.
5. CPU cooler fins / radiator: thổi xuyên lá tản theo chiều gió, cọ nhẹ bụi bám mặt quạt.
6. Graphics card: giữ cánh quạt card, thổi qua khe tản. Không tháo vỏ card.
7. Power supply: chỉ thổi qua lưới quạt nguồn từ bên ngoài. **Không bao giờ mở nguồn** (tụ còn điện).
8. Close up and test: lắp lọc bụi, đóng tấm hông, cắm lại, bật máy, nghe quạt quay êm.
Cảnh báo chung: không dùng máy hút bụi thường hút bên trong (tĩnh điện), không dùng nước / khăn ướt bên trong, bình khí nén giữ thẳng đứng (dốc ngược phun chất lỏng lạnh).

**paste** (sau `dust`, tháo tản CPU):
1. Prepare như `dust` + đồ: cồn isopropyl ≥ 90%, khăn giấy không xơ / bông, keo tản nhiệt mới, găng hoặc tay sạch.
2. Chạy máy 5 phút trước khi tắt để keo cũ mềm (tháo dễ, đỡ kéo CPU dính theo, nhất là socket AM4).
3. Rút dây quạt tản khỏi chân CPU_FAN, nới vít tản theo hình chữ X, mỗi vít vài vòng. Xoay nhẹ tản qua lại rồi mới nhấc, **không giật thẳng lên**.
4. Lau keo cũ trên CPU và đáy tản bằng cồn tới khi sạch bóng. Không để cồn chảy vào socket. CPU còn nằm trong socket, không cần tháo ra.
5. Bôi keo mới: dùng lại đúng câu trong `coolerStep` (một giọt cỡ hạt đậu ở giữa, không trét) cho thống nhất với bài lắp.
6. Lắp lại tản, siết vít chữ X, cắm lại CPU_FAN.
7. Check: bật máy, mở phần mềm xem nhiệt độ, chạy nặng 10 phút. So với trước khi thay (nếu người dùng có ghi).
AIO: chỉ tháo đầu bơm khỏi CPU, không tháo radiator, không mở ống. Tản thấp (máy Mini): cẩn thận card riser khi tháo tấm hông.

**check** (không mở máy): quạt có kêu to bất thường không, máy có tự tắt / giật khi chơi game không, lưới hút gió có bám bụi nhìn thấy được không, dây có lỏng không.
Mục này là bộ câu hỏi triệu chứng của B1, kết quả xấu thì gợi ý `dust` hoặc `paste`.

### A3. Gắn với 3D

- Thêm chế độ hướng dẫn thứ hai bên cạnh hướng dẫn lắp, ví dụ store thêm `guideKind: 'assembly' | 'maintenance'` và `maintenanceTask: 'dust' | 'paste' | null`.
  Gợi ý đặt dữ liệu ở file mới `src/data/maintenance.ts`, hàm `maintenanceSteps(task, familyId, buildId, hasGpu)` trả `GuideStep[]`, gọi `machineOf` để đổi nội dung theo máy.
- Mỗi bước có `part` để camera bay tới và tô sáng đúng linh kiện (dùng `guidePart` + `guideViews.ts` sẵn có). Máy vẫn **lắp nguyên**, không explode cả máy:
  với `paste` chỉ đưa tản CPU ra (giống trạng thái `isOut` của bước cooler), các bước khác chỉ tô sáng.
- Nút vào: trong thanh bên (chỗ đang có nút bắt đầu hướng dẫn lắp, `Sidebar.tsx` khoảng dòng 261–286) thêm "Maintenance guide", chọn việc `dust` / `paste` / `check`.
  Ở trang tư vấn có thể thêm một dòng giới thiệu nhỏ, không bắt buộc.
- **Máy của chính người dùng** (proposal: hướng dẫn theo từng người): người tham gia phần lớn đã có máy sẵn, không lắp từ app. Trước khi vào bài,
  hỏi "Which one looks most like your PC?" bằng ảnh 6 họ case + Gaming, rồi "What cooler does it have?" (tháp quạt / tản thấp / tản nước, có ảnh,
  thêm "I don't know" → mặc định tản tháp và nhắc mở hông xem). Lưu lựa chọn ở `pcsc-maintenance-v1`. Người vừa lắp từ trang tư vấn thì dùng luôn máy đó.
  Máy laptop: ghi rõ "This guide is for desktop PCs" và không làm tiếp (proposal nói về máy tự lắp / nâng cấp).
- Bước đã làm lưu riêng theo việc và máy (`loadDone('maint-dust-<máy>')`), không trộn với tiến độ lắp.
- Khung chat hỏi khi lắp (`guideChatContext`) cũng phải nhận ngữ cảnh bước bảo trì, để AI trả lời theo đúng nội dung chuẩn của bước.
- Model 3D: nếu thiếu đồ nghề (bình khí nén, cọ, chai cồn) thì dùng icon / ảnh 2D trong panel, KHÔNG cần dựng model mới ở bản này.

## B. Nhắc bảo trì

### B1. Tính "đúng lúc"

Khi người dùng bật nhắc, hỏi 4 câu ngắn (UI tiếng Anh):
1. Lần cuối máy được vệ sinh: chưa bao giờ / không nhớ / < 6 tháng / 6–12 tháng / > 1 năm.
2. Máy mua / lắp bao lâu rồi: < 1 năm / 1–2 / 2–3 / > 3 năm.
3. Môi trường (chọn nhiều): có thú cưng, thảm, hút thuốc, gần đường / nhiều bụi, máy chạy cả ngày.
4. Triệu chứng hiện tại (chọn nhiều): quạt to hơn trước, máy nóng / giật khi chạy nặng, tự tắt / khởi động lại, không có.

Luật (để trong một hàm thuần, có test):
- Chu kỳ `dust` = 6 tháng, có bất kỳ mục nào ở câu 3 thì 3 tháng.
- `dust` đến hạn **ngay** nếu: câu 1 là chưa bao giờ / không nhớ / > chu kỳ, hoặc có triệu chứng ở câu 4.
- `paste` đến hạn ngay nếu: máy > 2 năm và chưa từng thay keo, hoặc có triệu chứng nóng / tự tắt **sau khi** đã vệ sinh bụi mà vẫn còn.
- Không đến hạn: hẹn ngày = lần vệ sinh cuối + chu kỳ.
- Ước lượng "lần cuối" từ đáp án khoảng thì lấy mốc xấu nhất (vd 6–12 tháng → 12 tháng trước).
App nói lý do bằng câu đời thường, vd "Your PC has never been cleaned and you have a pet, so we suggest cleaning it now and every 3 months."

### B2. Cách nhắc

Phải nhắc được cả khi người dùng **không mở app** (đó mới là "đúng lúc"). Không có email / push ở bản này, nên:
1. **Thêm vào lịch**: nút "Add to Google Calendar" (link `https://calendar.google.com/calendar/render?action=TEMPLATE&text=...&dates=...&details=...`) và nút tải file `.ics`
   (tạo bằng code trong trình duyệt, có `RRULE` theo chu kỳ, `VALARM` nhắc trước 1 ngày). Phần mô tả sự kiện có link quay lại app mở thẳng bài hướng dẫn:
   `https://smartcarepc.pages.dev/?baotri=dust` (và mã thử nghiệm nếu có, xem B3). Link không chứa dữ liệu cá nhân nào.
2. **Trong app**: lưu lịch hẹn ở localStorage (`pcsc-maintenance-v1`: đáp án, ngày hẹn từng việc, lần làm xong). Mở app khi đã tới hạn thì hiện thẻ nhắc
   "Time to clean your PC" với 2 nút: Start guide / Remind me in 1 week. Đã làm xong thì cập nhật ngày hẹn tiếp theo.
3. Đọc `?baotri=dust|paste|check` khi mở app: vào thẳng trang 3D, chế độ bảo trì, đúng việc. Đọc xong xoá khỏi thanh địa chỉ (giống `?thunghiem` trong `study.ts`).

### B3. Ghi dữ liệu cho Q3 (chỉ khi đang ở chế độ thử nghiệm `?thunghiem=`)

Q3 cần biết: được nhắc → có bắt tay làm không → làm xong không. Ghi các sự kiện:
`reminder_set`, `calendar_added` (google / ics), `reminder_shown` (thẻ trong app), `reminder_snoozed`, `opened_from_calendar` (vào qua `?baotri`),
`maintenance_started`, `step_done`, `maintenance_completed`, kèm `task`, `participantCode`, `phase`, máy, thời điểm, và đáp án B1 khi `reminder_set`.

Thêm cho gap effect và Q3:
- `reminder_set` có cờ `never_serviced` (câu B1.1 là "chưa bao giờ" hoặc "không nhớ"): đây là nhóm chính của Q3. Đáp án B1 ở lần đầu (`phase=before`) là mốc
  "máy có được bảo trì không" trước hướng dẫn.
- Mỗi bước có 2 nút thay cho một nút tick: "Done it myself" / "I needed help" → `step_done` có `alone: true|false`. Từ đó tính được phần việc tự làm một mình.
- Khi xong (`maintenance_completed`) hỏi một câu: "Who did the work?" (me alone / me with someone's help / someone else did it). Thoát giữa chừng thì ghi
  `maintenance_abandoned` kèm bước đang dở, để báo cáo được cả người không làm xong.

Backend: entity mới `MaintenanceEvent` (bảng `maintenance_event`, `ddl-auto=update` tự tạo), `POST /api/maintenance/events`, `GET /api/maintenance/export.csv`
và `GET /api/maintenance/list` dùng **cùng khoá header `X-Export-Key`** như `FeedbackController` (không nhận khoá qua query). Gửi lỗi thì xếp hàng gửi lại giống
`flushQueue` trong `study.ts`, có id sự kiện để không ghi trùng. Không ở chế độ thử nghiệm thì không gửi gì lên máy chủ.
Trang `/feedback` (FeedbackAdmin) thêm một bảng đếm: bao nhiêu người đặt nhắc, bao nhiêu mở từ lịch, bao nhiêu làm xong, theo nhóm (khi form v2 có câu nền tảng kỹ thuật thì tách được theo Q4).

### B4. Khớp với thử nghiệm 4 tuần

Chu kỳ 3–6 tháng dài hơn thời gian thử nghiệm. Vì phần lớn người không rành **chưa từng vệ sinh máy**, luật B1 sẽ cho "đến hạn ngay": khi đó app cho người dùng
**tự chọn ngày làm** trong 1–14 ngày tới (mặc định cuối tuần gần nhất) và đặt nhắc vào ngày đó, nên vẫn đo được trong 4 tuần.
Không được bịa hạn sớm hơn luật để lấy dữ liệu. Lần đo sau 4 tuần (`lan=sau`) nên có thêm câu "Since your first visit, have you cleaned or serviced your PC? (yes, with the app / yes, someone else did it / no)":
câu này thuộc form v2, ghi lại cho người làm form chứ không làm ở đây.

## Tiêu chí xong

- Người có máy sẵn chọn được máy giống máy mình + loại tản trước khi vào bài; người vừa lắp từ trang tư vấn vào thẳng đúng máy.
- Sự kiện thử nghiệm có `never_serviced`, `alone` từng bước, câu "Who did the work?" và `maintenance_abandoned`.
- Chọn được `dust` / `paste` / `check` cho cả 6 họ case + Gaming, nội dung đổi đúng theo tản (khí / thấp / AIO) và nguồn SFX. Camera tới đúng linh kiện từng bước.
- Hàm luật B1 có test cho ít nhất: chưa bao giờ vệ sinh, có thú cưng (3 tháng), vệ sinh 2 tháng trước không triệu chứng (hẹn đúng ngày), máy 3 năm có triệu chứng nóng (`paste`).
- File `.ics` mở được trong Apple Calendar và Google Calendar, có lặp lại và nhắc trước 1 ngày. Link Google Calendar đúng ngày.
- Mở `?baotri=dust` vào thẳng bài; mở app khi đã tới hạn thì thấy thẻ nhắc; hoãn 1 tuần chạy đúng.
- Chế độ thử nghiệm: sự kiện vào bảng `maintenance_event`, CSV tải được bằng header khoá, gửi lại không trùng. Ngoài thử nghiệm: không có request nào lên `/api/maintenance`.
- Chạy được ở http://localhost:5173 và qua demo `smartcarepc.pages.dev`. Kiểm tra lệnh ở mục "Quy ước" đều qua.
