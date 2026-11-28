# SmartCarePC Helper (app khay cho Windows)

App nhỏ chạy ngầm ở khay hệ thống Windows 10/11 x64. Đặc tả đầy đủ ở `docs/maintenance-agent-spec.md`. App làm 2 việc:

1. **Tự kiểm tra sức khoẻ máy** (nhiệt độ, quạt, ổ cứng, dung lượng, tắt máy đột ngột, RAM, màn hình xanh, lỗi ổ đĩa, lỗi phần cứng, CPU bận khi rảnh, lâu không khởi động lại). Gộp lại thành một kết luận "Running well / Needs attention / Needs help now". Thấy dấu hiệu xấu thì nhắc và chỉ đúng bài hướng dẫn.
2. **Nhắc bảo dưỡng theo lịch** nếu nhận được lịch qua link `smartcarepc://`. Web đã bỏ nút gửi lịch: nhắc theo lịch giờ do email của web lo, nên thường app chỉ làm việc 1.

Người dùng tải app từ GitHub Releases qua hộp Quick health check trên web hoặc email kiểm tra hằng tháng.

Bấm vào thông báo thì app mở web vào thẳng bài 3D, ví dụ `https://smartcarepc.pages.dev/?baotri=dust`. App không có AI. Cài app là **tuỳ chọn** cho người tham gia nghiên cứu, cách nhắc bằng lịch trên web vẫn giữ nguyên.

## Cấu trúc

| Thư mục | Là gì |
|---|---|
| `src/SmartCarePC.Helper.Core/` | Lõi `net8.0`, không phụ thuộc Windows: đọc link, lịch, luật sức khoẻ, quyết định thông báo, hàng đợi sự kiện. Chạy test được trên Mac. |
| `src/SmartCarePC.Helper/` | App WinForms `net8.0-windows`: icon khay, toast, đọc cảm biến, Registry, nhận link `smartcarepc://` |
| `src/SmartCarePC.Helper/Ui/` | Giao diện: bảng màu sáng / tối, icon vẽ bằng code, thẻ của cửa sổ Status, menu khay |
| `src/SmartCarePC.Helper/Assets/` | `app.ico` (16–256 px) và `toast-logo.png`, đi kèm trong exe. Sinh bằng `tools/HelperUi`, không sửa tay |
| `tests/SmartCarePC.Helper.Tests/` | xUnit, 142 test cho lõi. `fixtures/web-dates.json` là kết quả chạy thật hàm ngày tháng của web. |
| `installer/SmartCarePCHelper.iss` | Bộ cài Inno Setup, cài cho từng người dùng, không cần admin. Ảnh bộ cài ở `installer/images/` |
| `tools/export-web-dates.mjs` | Chạy `addDays` / `addMonths` của `src/data/maintenanceSchedule.ts` để tạo lại fixture |
| `tools/send-test-plan.ps1` | Gửi lịch thử sang app trên Windows (web đã bỏ nút gửi lịch, chỉ dùng để thử phần nhắc theo lịch) |
| `tools/HelperUi/` | Chỉ chạy trên Windows, không đi kèm app: vẽ lại icon, chụp ảnh giao diện. Không nằm trong `.sln` |

| Việc | File |
|---|---|
| Đọc và kiểm tra link `smartcarepc://plan?d=...` | `Core/PlanLink.cs` |
| Khi nào hiện thông báo (1 thông báo bảo dưỡng / ngày, giờ yên lặng, hoãn 7 ngày) | `Core/Planner.cs` |
| 12 luật sức khoẻ, mỗi luật là một hàm thuần | `Core/Health/Rules.cs` |
| Kết luận tổng 3 mức | `Core/Health/Overall.cs` |
| Ngưỡng và nguồn tham khảo | `Core/Health/Thresholds.cs` |
| Ghi đợt nóng, RAM, quạt, CPU bận khi rảnh từ các lần đo; mốc trước / sau khi vệ sinh | `Core/Health/HealthTracker.cs` |
| Sự kiện nghiên cứu, hàng đợi gửi lại | `Core/StudyEvents.cs` |
| Đọc cảm biến (LibreHardwareMonitorLib, GetSystemTimes, GlobalMemoryStatusEx, GetLastInputInfo) | `Helper/Sensors/SensorReader.cs` |
| Ổ cứng (MSFT_PhysicalDisk), ổ đầy, Event Log, lần khởi động (Win32_OperatingSystem), toàn màn hình | `Helper/Sensors/SystemChecks.cs` |
| Vòng chạy nền | `Helper/AgentHost.cs` |
| Nội dung cửa sổ Status (chữ, thứ tự, dấu gạch cho số không đọc được), test được trên Mac | `Core/Display/StatusPage.cs` |
| Icon khay theo kết luận tổng, cửa sổ Status | `Helper/TrayApp.cs`, `Helper/StatusForm.cs` |
| Menu khay, cách vẽ menu | `Helper/Ui/TrayMenu.cs`, `Helper/Ui/MenuRenderer.cs` |
| Màu, font, chế độ sáng / tối, thanh tiêu đề tối | `Helper/Ui/Palette.cs`, `Helper/Ui/Theme.cs`, `Helper/Ui/WindowChrome.cs` |
| Icon khay, icon app, ảnh bộ cài (vẽ bằng code) | `Helper/Ui/IconArt.cs` |

## Build và test

Cần .NET 8 SDK. Trên Mac của Minh SDK đã cài ở `~/.dotnet` (cài bằng script chính thức `dotnet-install.sh`, không cần sudo).

```bash
export PATH="$HOME/.dotnet:$PATH"
```

Trên Windows cài cho riêng người dùng, không cần admin, bằng script chính thức `dotnet-install.ps1` (`-Channel 8.0`), rồi thêm thư mục cài vào `PATH`. Nếu máy có sẵn `dotnet` bản x86 trong `C:\Program Files (x86)\dotnet` mà báo lỗi `hostpolicy.dll` thì đó là bản 32 bit hỏng, dùng bản vừa cài.

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

### Icon và ảnh chụp giao diện (trên Windows)

Icon app, logo thông báo và ảnh bộ cài đều vẽ bằng code trong `Ui/IconArt.cs`, không tải gì từ mạng, không có logo hãng nào. Sửa hình thì sửa code rồi vẽ lại:

```powershell
cd desktop-agent
dotnet run --project tools/HelperUi -- assets
```

Lệnh ghi đè `src/SmartCarePC.Helper/Assets/app.ico`, `toast-logo.png` và `installer/images/*.bmp`. Commit cả các file này.

Chụp ảnh giao diện để so trước / sau:

```powershell
dotnet run --project tools/HelperUi -- shots
```

Ra thư mục `artifacts/ui-shots/` (không commit): cửa sổ Status ở 4 mức (đang kiểm tra, ổn, cần chú ý, cần xử lý ngay) với dữ liệu giả, cả sáng lẫn tối (thêm một ảnh màu hệ thống), mỗi mức một ảnh đầu trang, một ảnh cuối trang, một ảnh cả trang; menu khay; bảng icon khay ở 16 / 20 / 24 / 32 px trên taskbar sáng và tối; icon app. Thêm `--live` để chụp thêm cửa sổ với số đo thật của máy, `--toast` để hiện một thông báo mẫu (chỉ chụp được khi Windows hiện banner thông báo, tức là không bật Do not disturb).

Ép chế độ màu khi thử app: đặt biến môi trường `SMARTCAREPC_THEME=dark`, `light` hoặc `contrast` (màu hệ thống, gần giống tương phản cao) trước khi chạy. Không đặt thì app theo Windows (Settings → Personalization → Colors).

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

Chỉ chạy khi đã nhận lịch qua link. Tới ngày hẹn (hoặc lần bật máy đầu tiên sau ngày hẹn): toast "Time to clean your PC", câu lý do kiểu "It has been 6 months since the last clean.", 2 nút **Start guide** và **Remind me in 1 week**.

Không có lịch là bình thường. Lần chạy đầu app hiện một toast chào duy nhất "SmartCarePC Helper is checking your PC" (không có nút, không mở web), sau đó không nhắc gì về lịch nữa. Status ghi "This app only checks your PC's health. To get cleaning reminders by email, set them up on the SmartCarePC website".

### Kết luận tổng

Đầu cửa sổ Status có một thẻ kết luận (icon lớn giống icon khay, chữ to, các dòng giải thích), icon khay và chữ khi rê chuột cũng đổi theo. Mỗi mức một hình riêng, không chỉ khác màu:

| Mức | Khi nào | Icon khay |
|---|---|---|
| **Running well** | Không luật nào đang báo. Gợi ý nhẹ (ổ đầy, RAM, CPU bận khi rảnh, lâu không khởi động lại) chỉ hiện thành dòng "Tip:", không đổi mức | tròn xanh dương, dấu tích |
| **Needs attention** | Có luật bảo dưỡng hoặc Event Log đang báo: `cpu_hot`, `gpu_hot`, `unexpected_shutdown`, `blue_screen`, `disk_errors`, `hardware_errors` | tam giác cam, chấm than đen |
| **Needs help now** | Có luật khẩn: `disk_failing`, `fan_stopped` | bát giác đỏ, chấm than trắng |

- Luật không đọc được (ví dụ nhiệt CPU, quạt khi không có quyền admin) **không tính là ổn**. Thẻ kết luận ghi "Not checked on this PC: Processor heat, Processor fan. This app cannot read these here, so they are not counted as OK." Tooltip ghi kiểu "SmartCarePC Helper: Running well, 2 not checked".
- Chưa luật nào chạy xong (vừa mở app): "Checking your PC", icon tròn xám có ba chấm.
- Dưới thẻ kết luận là các thẻ Checks (mỗi luật một nhãn có hình và chữ: OK, Tip, Needs attention, Needs help now, Not checked; mục cần để ý xếp lên đầu), Latest readings (số nào không đọc được thì hiện dấu gạch xám, giải thích một lần ở cuối thẻ), Reminders (có ô giờ yên lặng), Privacy. Cửa sổ cuộn được, theo chế độ sáng / tối và tương phản cao của Windows, Esc để đóng.
- Icon khay vẽ đúng cỡ theo DPI của taskbar (16 px ở 100%, 24 px ở 150%...), không để Windows co ảnh 32 px nên không mờ.
- Luật Event Log đã báo rồi vẫn giữ mức Needs attention trên Status suốt 7 ngày, chỉ không hiện thông báo lại. Quạt đứng giữ Needs help now thêm 1 giờ sau lần báo.

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
| `blue_screen` | Event Log System, BugCheck 1001 | ≥ 1 lần trong 7 ngày, có lần mới từ lần báo trước | Câu khuyên lưu bài, nhờ người rành xem. Không mở bài bảo dưỡng |
| `disk_errors` | Event Log System, nguồn `disk`, mã 7 / 51 / 153 | ≥ 3 lần trong 7 ngày, có lần mới. **CHƯA CHỐT** | Nhắc sao lưu file |
| `hardware_errors` | Event Log System, WHEA-Logger 17 / 18 / 19 / 47 | ≥ 1 lần trong 7 ngày, có lần mới. **CHƯA CHỐT** (mã 17 có thể lặp liên tục trên vài bo mạch) | Câu khuyên nhờ người rành xem nếu máy treo / tự khởi động lại |
| `cpu_busy_idle` | `GetSystemTimes` + `GetLastInputInfo` | Tải CPU trung bình > 60% suốt 30 phút không ai chạm chuột / phím, ở ≥ 2 ngày trong 7 ngày. **CHƯA CHỐT** | Gợi ý nhẹ: mở Task Manager (Ctrl + Shift + Esc) |
| `no_restart` | `Win32_OperatingSystem.LastBootUpTime` | ≥ 14 ngày chưa khởi động lại thật (Shut down kiểu fast startup không tính). **CHƯA CHỐT** | Gợi ý nhẹ: chọn Restart |

Cả 5 luật mới chỉ dùng nguồn đọc được bằng tài khoản thường, không cài driver, không xin admin. Nguồn tham khảo và lý do từng ngưỡng ghi trong `Thresholds.cs`. `cpu_busy_idle` cần 2 ngày vì Windows tự chạy bảo trì (quét virus, cập nhật) đúng lúc máy rảnh.

Đo mỗi 5 giây khi máy chạy nặng (CPU ≥ 50% hoặc GPU ≥ 40%), mỗi 60 giây khi rảnh. Kiểm tra ổ cứng, dung lượng, Event Log và lần khởi động mỗi 30 phút.

### Không làm phiền

- Tối đa 1 thông báo bảo dưỡng mỗi ngày (màn hình xanh, lỗi ổ đĩa, lỗi phần cứng cũng nằm trong giới hạn này, lỗi ổ đĩa được ưu tiên trước). Gợi ý nhẹ (ổ đầy, RAM, CPU bận khi rảnh, lâu không khởi động lại) tối đa 1 lần / ngày và mỗi luật 1 lần / 7 ngày.
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
| Event Log System | đọc được | Tắt máy đột ngột, màn hình xanh, lỗi ổ đĩa, WHEA |
| Lần thao tác cuối (`GetLastInputInfo`) | đọc được | Chỉ biết thời điểm, không ghi phím hay chuột nào |
| Lần khởi động (`Win32_OperatingSystem`) | đọc được | |

Nếu máy đã có PawnIO (ví dụ cài cùng một app quạt khác) và người dùng tự chạy app bằng quyền admin thì app đọc được nhiệt CPU. App không bao giờ tự làm việc này. **Tất cả các dòng trên chưa thử trên máy Windows thật.**

## Quyền riêng tư

- Số đo, lịch, hàng đợi sự kiện, nhật ký chỉ nằm ở `%LOCALAPPDATA%\SmartCarePC\` (`state.json`, `events.json`, `log.txt`). Cùng thư mục có `toast-logo.png`: logo cho thông báo, chép ra từ exe vì Windows chỉ đọc logo từ file. `--uninstall` xoá file này.
- Không có tên máy, tên người dùng Windows, số serial, địa chỉ IP trong dữ liệu nào.
- Không ở chế độ thử nghiệm thì app **không tạo HttpClient**, chỉ mở trình duyệt khi người dùng bấm.
- Ở chế độ thử nghiệm: gửi `POST <origin>/api/maintenance/events`, User-Agent `SmartCarePCHelper/1.0`, xếp hàng trên đĩa, gửi lại giữ nguyên `eventId`. Máy chủ trả 4xx thì chuyển sự kiện sang danh sách `rejected` để không chặn cả hàng.
- Thông báo chỉ mở địa chỉ của web SmartCarePC (origin trong danh sách) hoặc `ms-settings:storagesense`. Link `smartcarepc://` đi từ bản thứ hai sang bản đang chạy qua named pipe chỉ cho chính người dùng đó, không mở cổng mạng.

## Việc Minh cần tự thử trên Windows 10/11 x64 thật

Ngày 28/11/2026, trên một máy Windows 11 (màn hình 100%, tài khoản thường) đã chạy: build, test, cửa sổ Status với số đo thật của máy (nhiệt CPU và quạt không đọc được, nhiệt GPU đọc được, Event Log và Last restart đọc được), cửa sổ Status và menu khay ở cả sáng lẫn tối. Chưa thử: icon trong khay thật, màn hình 150%, banner thông báo, bộ cài. Danh sách thử:

1. **Cài**: chạy bộ cài bằng tài khoản thường. Không có hộp hỏi admin (UAC). SmartScreen hiện cảnh báo: More info → Run anyway, chụp ảnh lại cho hướng dẫn.
2. **Khay**: icon hiện ở khay. Chuột phải có đủ menu. Bật / tắt Start with Windows, khởi động lại máy xem app có tự chạy không.
3. **Chưa có lịch**: lần chạy đầu hiện một toast "SmartCarePC Helper is checking your PC", không có nút. Thoát app, chạy lại, đổi ngày máy +3 ngày: không hiện lại. Status ghi dòng "This app only checks your PC's health..." ở thẻ Reminders.
4. **Nhận lịch**: `powershell -ExecutionPolicy Bypass -File tools\send-test-plan.ps1`. Trình duyệt / Windows hỏi mở app thì đồng ý. Hiện "Your reminders are on". Status thấy đúng ngày.
5. **Link hỏng**: chạy với `-Bad`. App không crash, `log.txt` có dòng `ignored link`.
6. **Toast theo lịch**: gửi lịch dust hôm nay (ngoài giờ yên lặng). Trong 1 phút hiện "Time to clean your PC". Bấm Start guide mở đúng bài 3D. Đổi ngày máy để thử ngày khác.
7. **Hoãn**: bấm Remind me in 1 week. Đổi ngày máy +6 ngày: chưa báo. +7 ngày: báo lại.
8. **Giờ yên lặng / toàn màn hình**: đặt giờ yên lặng bao giờ hiện tại, hoặc mở game toàn màn hình: không có toast bảo dưỡng.
9. **Cảm biến**: mở Status, ghi lại số nào đọc được, số nào hiện dấu gạch (không đọc được). Chạy nặng CPU vài phút (ví dụ Cinebench), xem tải CPU và nhiệt có ghi được không. Thử thêm một lần chạy app bằng quyền admin trên máy đã có PawnIO để so.
10. **GPU**: chơi game hoặc chạy benchmark GPU, xem nhiệt GPU trong Status.
11. **Ổ đĩa, Event Log**: Status có Drive health, số lần tắt máy đột ngột, Blue error screens, Drive read and write errors, Hardware errors và Last restart (không phải dấu gạch). So số đếm với Event Viewer (Windows Logs → System, lọc theo nguồn BugCheck / disk / WHEA-Logger). Nếu máy từng bị màn hình xanh mà số đếm là 0, ghi lại tên nguồn hiện trong Event Viewer: bộ lọc đang dùng tên `Microsoft-Windows-WER-SystemErrorReporting` hoặc `BugCheck`.
12. **Hiệu năng**: Task Manager, để app chạy ít nhất 30 phút. Mục tiêu dưới 1% CPU và dưới 80 MB RAM. Ghi số vào báo cáo.
13. **Mạng**: không ở chế độ thử nghiệm, mở Resource Monitor (tab Network) hoặc Fiddler: `SmartCarePCHelper.exe` không có kết nối nào.
14. **Thử nghiệm**: gửi lịch với `-Study P07` và `-Origin http://localhost:5173` khi chạy web và backend local. Sự kiện vào bảng `maintenance_event`. **Cần sửa backend trước**: regex `eventType` chưa có 5 loại mới, máy chủ sẽ trả 400 và app chuyển sự kiện sang `rejected`.
15. **Gỡ**: gỡ trong Apps & features. Kiểm tra không còn `HKCU\Software\Classes\smartcarepc`, không còn mục Run, thư mục dữ liệu xoá hay giữ theo câu trả lời.
16. **Kết luận tổng**: mở Status, dòng đầu phải là Running well / Needs attention / Needs help now, có dòng "Not checked on this PC" liệt kê nhiệt CPU, quạt nếu không đọc được. Rê chuột lên icon khay xem chữ. Ảnh cả 4 mức (kể cả cam và đỏ, khó tạo bằng tay) có sẵn bằng `tools/HelperUi shots`; trên máy thật chỉ cần xem mức đang có.
17. **CPU bận khi rảnh**: để máy chạy việc nặng (ví dụ nén file lớn, Cinebench vòng lặp) và không chạm chuột / phím 35 phút, làm 2 ngày khác nhau. Ngày thứ hai có gợi ý "Your PC stays busy when you are away" (ngoài giờ yên lặng). Kiểm tra màn hình khoá hoặc tắt màn hình có làm `GetLastInputInfo` sai không.
18. **Lâu không khởi động lại**: Status ghi Last restart. Bấm Shut down rồi bật lại: Last restart không đổi (fast startup). Bấm Restart: thành "today". Muốn thấy gợi ý thì cần máy đã chạy 14 ngày.
19. **Giao diện**: đổi Settings → Personalization → Colors sang Dark rồi Light khi cửa sổ Status đang mở: cửa sổ và menu khay đổi theo, thanh tiêu đề cũng tối. Đổi Display → Scale sang 150%: chữ và icon khay vẫn nét. Bật tương phản cao (Left Alt + Left Shift + Print Screen): cửa sổ dùng màu hệ thống. Bấm Tab: viền focus hiện ở thẻ kết luận, ô chọn giờ, nút Save. Page Up / Page Down cuộn trang.
