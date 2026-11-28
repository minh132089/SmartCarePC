# SmartCarePC Helper (app khay cho Windows)

App nhỏ chạy ngầm ở khay hệ thống Windows 10/11 x64. Đặc tả đầy đủ ở `docs/maintenance-agent-spec.md`. App làm 2 việc:

1. **Nhắc bảo dưỡng đúng hạn** bằng thông báo của Windows, theo lịch người dùng đã đặt trên web.
2. **Tự kiểm tra sức khoẻ máy** (nhiệt độ, quạt, ổ cứng, dung lượng, tắt máy đột ngột, RAM). Thấy dấu hiệu xấu thì nhắc và chỉ đúng bài hướng dẫn.

Bấm vào thông báo thì app mở web vào thẳng bài 3D, ví dụ `https://smartcarepc.pages.dev/?baotri=dust`. App không có AI. Cài app là **tuỳ chọn** cho người tham gia nghiên cứu, cách nhắc bằng lịch trên web vẫn giữ nguyên.

## Cấu trúc

| Thư mục | Là gì |
|---|---|
| `src/SmartCarePC.Helper.Core/` | Lõi `net8.0`, không phụ thuộc Windows: đọc link, lịch, luật sức khoẻ, quyết định thông báo, hàng đợi sự kiện. Chạy test được trên Mac. |
| `src/SmartCarePC.Helper/` | App WinForms `net8.0-windows`: icon khay, toast, đọc cảm biến, Registry, nhận link `smartcarepc://` |
| `tests/SmartCarePC.Helper.Tests/` | xUnit, 100 test cho lõi. `fixtures/web-dates.json` là kết quả chạy thật hàm ngày tháng của web. |
| `installer/SmartCarePCHelper.iss` | Bộ cài Inno Setup, cài cho từng người dùng, không cần admin |
| `tools/export-web-dates.mjs` | Chạy `addDays` / `addMonths` của `src/data/maintenanceSchedule.ts` để tạo lại fixture |
| `tools/send-test-plan.ps1` | Gửi lịch thử sang app trên Windows (thay cho nút trên web, nút này chưa làm) |

| Việc | File |
|---|---|
| Đọc và kiểm tra link `smartcarepc://plan?d=...` | `Core/PlanLink.cs` |
| Khi nào hiện thông báo (1 thông báo bảo dưỡng / ngày, giờ yên lặng, hoãn 7 ngày) | `Core/Planner.cs` |
| 7 luật sức khoẻ, mỗi luật là một hàm thuần | `Core/Health/Rules.cs` |
| Ngưỡng và nguồn tham khảo | `Core/Health/Thresholds.cs` |
| Ghi đợt nóng, RAM, quạt từ các lần đo; mốc trước / sau khi vệ sinh | `Core/Health/HealthTracker.cs` |
| Sự kiện nghiên cứu, hàng đợi gửi lại | `Core/StudyEvents.cs` |
| Đọc cảm biến (LibreHardwareMonitorLib, GetSystemTimes, GlobalMemoryStatusEx) | `Helper/Sensors/SensorReader.cs` |
| Ổ cứng (MSFT_PhysicalDisk), ổ đầy, Event Log, toàn màn hình | `Helper/Sensors/SystemChecks.cs` |
| Vòng chạy nền | `Helper/AgentHost.cs` |
| Menu khay, cửa sổ Status | `Helper/TrayApp.cs`, `Helper/StatusForm.cs` |

## Build và test

Cần .NET 8 SDK. Trên Mac của Minh SDK đã cài ở `~/.dotnet` (cài bằng script chính thức `dotnet-install.sh`, không cần sudo).

```bash
export PATH="$HOME/.dotnet:$PATH"
```

```bash
cd desktop-agent && dotnet test
```

```bash
cd desktop-agent && dotnet publish src/SmartCarePC.Helper -c Release -r win-x64 --self-contained -p:PublishSingleFile=true -o publish/win-x64
```

Lệnh publish chạy được cả trên Mac (`EnableWindowsTargeting`). Ra một file `publish/win-x64/SmartCarePCHelper.exe` khoảng 74 MB, không cần cài .NET trên máy người dùng. Mac không chạy được file này.

Tạo lại fixture ngày tháng khi luật trên web đổi (Node 22.6 trở lên):

```bash
node desktop-agent/tools/export-web-dates.mjs
```

### Tạo bộ cài (trên Windows)

1. Cài [Inno Setup 6](https://jrsoftware.org/isinfo.php).
2. Publish như trên (trên Windows dùng cùng lệnh).
3. Chạy `"C:\Program Files (x86)\Inno Setup 6\ISCC.exe" installer\SmartCarePCHelper.iss`. Ra file `installer\Output\SmartCarePCHelper-Setup-1.0.0.exe`.

Bộ cài:
- đặt app vào `%LOCALAPPDATA%\Programs\SmartCarePC Helper`
- đăng ký `smartcarepc://` ở `HKCU\Software\Classes\smartcarepc`
- thêm mục khởi động ở `HKCU\Software\Microsoft\Windows\CurrentVersion\Run` (có ô bỏ chọn)
- có trong Apps & features. Gỡ thì xoá khoá Registry, mục khởi động, đăng ký thông báo, và hỏi có xoá dữ liệu không.

**File chưa ký số**, nên Windows SmartScreen sẽ hiện "Windows protected your PC". Người dùng bấm **More info** rồi **Run anyway**. Hướng dẫn có ảnh cho người không rành chưa làm (cần chụp trên máy Windows thật).

Thử nhanh mà không cài: chạy `SmartCarePCHelper.exe --register` một lần để đăng ký `smartcarepc://`, rồi chạy `SmartCarePCHelper.exe`.

## Nối với web: định dạng link

Web mở `smartcarepc://plan?d=<base64url của JSON>`. JSON:

```json
{
  "v": 1,
  "pc": { "machine": "standard_tower", "cooler": "air", "hasGpu": true },
  "dates": { "dust": "2026-12-05", "paste": "2029-05-28", "check": "2026-12-28" },
  "completed": { "dust": "2026-06-05" },
  "study": { "code": "P07", "phase": "before" },
  "origin": "https://smartcarepc.pages.dev"
}
```

- `pc` lấy từ `MaintenancePC`, `dates` / `completed` lấy thẳng từ `pcsc-maintenance-v1`. App **chỉ đọc** ngày hẹn, không tự tính lại luật của web.
- `study` chỉ có khi đang ở chế độ thử nghiệm (`useStudy().code`). Không có thì app không gọi mạng.
- `origin` chỉ nhận `https://smartcarepc.pages.dev` hoặc `http://localhost:5173`.
- App bỏ cả link nếu sai bất kỳ chỗ nào: link dài hơn 4096 ký tự, JSON lớn hơn 2048 byte, sai kiểu, `machine` / `cooler` lạ, ngày không có thật, mã thử nghiệm sai `CODE_PATTERN`, origin lạ. Không có tên, email hay thông tin cá nhân nào.

Hoãn 1 tuần trên app làm giống web: ngày hẹn thành hôm nay + 7 (`addDays(day(), 7)`). Làm xong một việc trên web thì web cần gửi lại link để app biết ngày hẹn mới. Chưa gửi lại thì app im 7 ngày sau khi bấm Start guide rồi mới nhắc lại.

## App làm gì

### Nhắc theo lịch

Tới ngày hẹn (hoặc lần bật máy đầu tiên sau ngày hẹn): toast "Time to clean your PC", câu lý do kiểu "It has been 6 months since the last clean.", 2 nút **Start guide** và **Remind me in 1 week**. Chưa có lịch: toast "Open SmartCarePC to set up your reminders", bấm vào mở `<origin>/maintain`.

### Luật sức khoẻ

| Luật | Đọc từ | Khi nào báo | Dẫn tới |
|---|---|---|---|
| `cpu_hot` | LibreHardwareMonitorLib | Tải CPU > 80% và ≥ 90 °C liên tục 2 phút, ở ≥ 2 ngày trong 7 ngày | `dust`, hoặc `paste` nếu đã vệ sinh trong 30 ngày |
| `gpu_hot` | LibreHardwareMonitorLib (NVIDIA / AMD) | Tải GPU ≥ 70% và ≥ 85 °C liên tục 2 phút, ở ≥ 2 ngày trong 7 ngày | `dust` |
| `fan_stopped` | LibreHardwareMonitorLib | Quạt CPU (đã từng quay) báo 0 RPM trong khi CPU > 70 °C, liên tục 30 giây | `check`, báo ngay |
| `disk_failing` | `MSFT_PhysicalDisk.HealthStatus` (nguồn của `Get-PhysicalDisk`) | Warning hoặc Unhealthy | "Back up your files now", báo ngay |
| `drive_low` | `DriveInfo` | Ổ hệ thống còn < 10% | Gợi ý nhẹ + nút mở Storage Sense |
| `unexpected_shutdown` | Event Log System, Kernel-Power 41 | ≥ 2 lần trong 7 ngày, có lần mới từ lần báo trước | `dust` |
| `ram_full` | `GlobalMemoryStatusEx` | > 90% RAM trong > 50% thời gian máy chạy, 7 ngày, cần ≥ 10 giờ số đo | Gợi ý `<origin>/upgrade` |

Đo mỗi 5 giây khi máy chạy nặng (CPU ≥ 50% hoặc GPU ≥ 40%), mỗi 60 giây khi rảnh. Kiểm tra ổ cứng, dung lượng và Event Log mỗi 30 phút.

### Không làm phiền

- Tối đa 1 thông báo bảo dưỡng mỗi ngày. Gợi ý nhẹ (ổ đầy, RAM) tối đa 1 lần / ngày và mỗi luật 1 lần / 7 ngày.
- Ổ cứng sắp hỏng và quạt đứng báo ngay, kể cả trong giờ yên lặng hay lúc tạm dừng. Mỗi luật tối đa 1 lần / 24 giờ.
- Giờ yên lặng mặc định 22:00–08:00, chỉnh trong cửa sổ Status. Không báo khi đang chơi game toàn màn hình, trình chiếu, hoặc Focus Assist đang bận (`SHQueryUserNotificationState`).
- Hiện rồi mà người dùng không bấm: 3 ngày sau mới nhắc lại. Remind me in 1 week: im đúng 7 ngày.
- Menu khay: Status, Pause reminders (1 day / 1 week / Resume), Open SmartCarePC, Start with Windows, Quit.

### Mốc trước / sau khi vệ sinh

App giữ nhiệt độ trung bình lúc chạy nặng theo từng ngày. Web gửi lịch có `completed.dust` mới thì app lấy trung bình 14 ngày trước làm mốc. Sau đó Status hiện câu kiểu "After cleaning, your CPU runs 12 °C cooler under load". Đây là số đo của một máy, không được gọi là kết quả nghiên cứu.

## Cảm biến và driver

Đã kiểm tra trước khi viết code:

- `LibreHardwareMonitorLib` 0.9.6 (bản ổn định mới nhất, 14/2/2026, MPL-2.0) đã bỏ WinRing0 (Defender gắn cờ `VulnerableDriver:WinNT/Winring0`) và chuyển sang driver **PawnIO**.
- Đã xem file DLL win-x64 net8.0: không có WinRing0, không có file `.sys`, không có bộ cài driver, không có địa chỉ mạng. Thư viện chỉ đọc khoá `SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\PawnIO` để biết PawnIO đã cài chưa, rồi mở `\\?\GLOBALROOT\Device\PawnIO`.
- PawnIO phải cài riêng, lúc cài cần admin. Theo các dự án dùng lại thư viện, kể cả đã cài PawnIO thì driver chỉ mở cho SYSTEM và Administrators.

Vì app không cài driver và không xin admin:

| Số đo | Tài khoản thường | Ghi chú |
|---|---|---|
| Tải CPU, RAM | đọc được | API Windows thường |
| Nhiệt CPU | **thường không đọc được** | Cần PawnIO + admin. App ghi "not available on this PC", bỏ luật `cpu_hot` |
| Quạt CPU | **thường không đọc được** | Chip Super I/O, cũng qua PawnIO. Bỏ luật `fan_stopped` |
| Nhiệt / tải GPU NVIDIA, AMD | thường đọc được | Qua thư viện của hãng card |
| Tình trạng ổ cứng | đọc được | `MSFT_PhysicalDisk`. SMART qua WMI `root\wmi` cần admin nên không dùng |
| Event Log System | đọc được | |

Nếu máy đã có PawnIO (ví dụ cài cùng một app quạt khác) và người dùng tự chạy app bằng quyền admin thì app đọc được nhiệt CPU. App không bao giờ tự làm việc này. **Tất cả các dòng trên chưa thử trên máy Windows thật.**

## Quyền riêng tư

- Số đo, lịch, hàng đợi sự kiện, nhật ký chỉ nằm ở `%LOCALAPPDATA%\SmartCarePC\` (`state.json`, `events.json`, `log.txt`).
- Không có tên máy, tên người dùng Windows, số serial, địa chỉ IP trong dữ liệu nào.
- Không ở chế độ thử nghiệm thì app **không tạo HttpClient**, chỉ mở trình duyệt khi người dùng bấm.
- Ở chế độ thử nghiệm: gửi `POST <origin>/api/maintenance/events`, User-Agent `SmartCarePCHelper/1.0`, xếp hàng trên đĩa, gửi lại giữ nguyên `eventId`. Máy chủ trả 4xx thì chuyển sự kiện sang danh sách `rejected` để không chặn cả hàng.
- Thông báo chỉ mở địa chỉ của web SmartCarePC (origin trong danh sách) hoặc `ms-settings:storagesense`. Link `smartcarepc://` đi từ bản thứ hai sang bản đang chạy qua named pipe chỉ cho chính người dùng đó, không mở cổng mạng.

## Việc Minh cần tự thử trên Windows 10/11 x64 thật

Chưa thử được gì trên Windows. Danh sách thử:

1. **Cài**: chạy bộ cài bằng tài khoản thường. Không có hộp hỏi admin (UAC). SmartScreen hiện cảnh báo: More info → Run anyway, chụp ảnh lại cho hướng dẫn.
2. **Khay**: icon hiện ở khay. Chuột phải có đủ menu. Bật / tắt Start with Windows, khởi động lại máy xem app có tự chạy không.
3. **Chưa có lịch**: lần chạy đầu hiện "Open SmartCarePC to set up your reminders", bấm vào mở `/maintain`.
4. **Nhận lịch**: `powershell -ExecutionPolicy Bypass -File tools\send-test-plan.ps1`. Trình duyệt / Windows hỏi mở app thì đồng ý. Hiện "Your reminders are on". Status thấy đúng ngày.
5. **Link hỏng**: chạy với `-Bad`. App không crash, `log.txt` có dòng `ignored link`.
6. **Toast theo lịch**: gửi lịch dust hôm nay (ngoài giờ yên lặng). Trong 1 phút hiện "Time to clean your PC". Bấm Start guide mở đúng bài 3D. Đổi ngày máy để thử ngày khác.
7. **Hoãn**: bấm Remind me in 1 week. Đổi ngày máy +6 ngày: chưa báo. +7 ngày: báo lại.
8. **Giờ yên lặng / toàn màn hình**: đặt giờ yên lặng bao giờ hiện tại, hoặc mở game toàn màn hình: không có toast bảo dưỡng.
9. **Cảm biến**: mở Status, ghi lại số nào đọc được, số nào "not available on this PC". Chạy nặng CPU vài phút (ví dụ Cinebench), xem tải CPU và nhiệt có ghi được không. Thử thêm một lần chạy app bằng quyền admin trên máy đã có PawnIO để so.
10. **GPU**: chơi game hoặc chạy benchmark GPU, xem nhiệt GPU trong Status.
11. **Ổ đĩa, Event Log**: Status có Drive health và số lần tắt máy đột ngột (không phải "not available").
12. **Hiệu năng**: Task Manager, để app chạy ít nhất 30 phút. Mục tiêu dưới 1% CPU và dưới 80 MB RAM. Ghi số vào báo cáo.
13. **Mạng**: không ở chế độ thử nghiệm, mở Resource Monitor (tab Network) hoặc Fiddler: `SmartCarePCHelper.exe` không có kết nối nào.
14. **Thử nghiệm**: gửi lịch với `-Study P07` và `-Origin http://localhost:5173` khi chạy web và backend local. Sự kiện vào bảng `maintenance_event`. **Cần sửa backend trước**: regex `eventType` chưa có 5 loại mới, máy chủ sẽ trả 400 và app chuyển sự kiện sang `rejected`.
15. **Gỡ**: gỡ trong Apps & features. Kiểm tra không còn `HKCU\Software\Classes\smartcarepc`, không còn mục Run, thư mục dữ liệu xoá hay giữ theo câu trả lời.
