# Bàn giao: làm đẹp giao diện SmartCarePC Helper (28/11/2026)

File này dành cho Claude ở một máy khác, tốt nhất là máy Windows, vì ở đó chạy được app để nhìn giao diện thật. Việc cần làm: làm cho app khay **SmartCarePC Helper** trông đẹp hơn. Logic giữ nguyên.

Đọc hết file này trước khi sửa. Trả lời Minh bằng tiếng Việt, câu ngắn.

## 1. Lấy code

- Repo: `github.com/minh132089/SmartCarePC`
- Nhánh: `claude/sharp-dhawan-68ab27`. Commit mới nhất là `6ff7a2a` "Helper: overall health verdict and five more health rules". File này cũng nằm trên nhánh đó.
- Hai nhánh khác **không dùng được** cho việc này:
  - `feature/maintenance-agent` cũ hơn, thiếu kết luận tổng và 5 luật mới
  - `release-v2` có phần web và backend nối với app, nhưng chưa có thư mục `desktop-agent/`

```bash
git clone git@github.com:minh132089/SmartCarePC.git
cd SmartCarePC
git switch claude/sharp-dhawan-68ab27
git switch -c ui/helper-polish
```

Làm trên nhánh mới `ui/helper-polish`.

## 2. Luật bắt buộc

- Chỉ commit khi Minh bảo. Không push, không deploy, trừ khi Minh nói rõ.
- Cuối commit message ghi `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Không bao giờ commit `deploy/`, `design/photos/`, `docs/feedback-ui-update.md`, `tools/__pycache__/*.pyc`, `assets-src/`, `backend/.env`. Nếu máy có những thứ đó thì cứ để yên.
- Không in hay chép API key hay demo key nào ra chat, file hay git.
- Luật cứng của app. Làm đẹp không được phá luật nào sau đây:
  - Không cần quyền admin, `app.manifest` để `asInvoker`. Không cài driver, không tự cập nhật ngầm.
  - Số đo chỉ nằm trên máy (`%LOCALAPPDATA%\SmartCarePC\`). Không gửi tên máy, tên người dùng, số serial, địa chỉ IP.
  - Không thử nghiệm thì không gọi mạng, trừ lúc mở trình duyệt. Vì vậy **không tải font, ảnh hay icon từ mạng**. Mọi thứ đi kèm trong app.
  - Gỡ được sạch. File mới nào app ghi ra máy thì phần gỡ (`WindowsIntegration.Uninstall`, bộ cài Inno) cũng phải xoá được.
  - Không có chữ hay logo của hãng thật như Windows, Intel, NVIDIA, AMD. Icon tự vẽ.
- Trạng thái không được chỉ dựa vào màu. Luôn kèm hình (dấu tích hoặc chấm than) và chữ.
- Chữ trên giao diện giữ tiếng Anh như hiện tại.
  - Viết cho người không rành kỹ thuật: mỗi câu một ý, nói vì sao, không doạ, không bắt làm ngay.
  - Không dùng từ như "thermal throttling", "SMART", "Kernel-Power".
  - Không bao giờ bảo người dùng mở bộ nguồn.
- App phải nhẹ. Ở yên trong WinForms trên .NET 8. Không thêm WebView2, WPF hay WinUI, không thêm thư viện giao diện nặng.

## 3. Những chỗ có giao diện

Toàn bộ nằm ở `desktop-agent/src/SmartCarePC.Helper/`.

| File | Hiện giờ | Chỗ còn xấu |
|---|---|---|
| `TrayApp.cs` | Icon khay vẽ bằng GDI+ 32×32, có 4 mức trong `MakeIcon`. Menu là `ContextMenuStrip` gồm Status, Pause reminders, Open SmartCarePC, Start with Windows, Quit. | Icon chỉ vẽ một cỡ 32 rồi để Windows co lại, nên ở 16 px bị mờ. Menu dùng renderer mặc định của WinForms, trông như Windows 7. |
| `StatusForm.cs` | Cửa sổ 520×620 kiểu `FixedDialog`. Tiêu đề in đậm, đổi màu theo mức. Toàn bộ nội dung dồn vào **một TextBox nhiều dòng**: LATEST READINGS, CHECKS, REMINDERS, PRIVACY. Ô giờ yên lặng nằm ở đáy. | Đây là chỗ xấu nhất. Không có thẻ, không có khoảng cách, chữ "not available on this PC" lặp đi lặp lại. Không theo chế độ tối. Chưa có icon cửa sổ. |
| `Toasts.cs` | Dựng bằng `ToastContentBuilder`, gồm tiêu đề, thân và nút. | Chưa có logo app (`AddAppLogoOverride`), cũng chưa có dòng attribution. |
| `installer/SmartCarePCHelper.iss` | Inno Setup, `WizardStyle=modern`. | Chưa có `SetupIconFile`, chưa có ảnh wizard. |
| `SmartCarePC.Helper.csproj` | | Chưa có `<ApplicationIcon>`. Cả thư mục chưa có file `.ico` hay `.png` nào. |

Dữ liệu để hiện lấy từ `host.Snapshot` (`AgentHost.cs`) và `OverallStatus` (`SmartCarePC.Helper.Core/Health/Overall.cs`):
- `OverallStatus` có `Level`, `Headline`, `Lines`, `Problems`, `Tips`, `NotChecked`, `TrayText`
- `TrayText` không quá 63 ký tự vì Windows cắt chữ dài

Nếu cần dữ liệu ở dạng khác, thêm thuộc tính mới. Đừng đổi luật, ngưỡng hay lịch trong `SmartCarePC.Helper.Core`.

## 4. Hướng thiết kế nên theo

Bám theo web (`src/index.css` ở gốc repo) để app và web trông cùng một nhà:
- Màu nhấn `#0071e3`, chữ `#1d1d1f`, chữ phụ `#6e6e73`, nền `#f5f5f7`, mặt thẻ `#ffffff`, viền `rgb(0 0 0 / 0.06)`
- Ổn: `#1d7a3a` trên nền `#eaf7ee`
- Cần chú ý: `#7a4b00` trên nền `#fff4e5`
- Cần xử lý ngay: `#b42318` trên nền `#fdecec`
- Font: web dùng SF Pro. Trên Windows dùng `Segoe UI Variable`, máy nào không có thì dùng `Segoe UI`. Cả hai đều có sẵn trong Windows, không phải tải.

Gợi ý cụ thể, tự chọn làm cái nào:
1. **StatusForm**
   - Bỏ cái TextBox. Đầu cửa sổ là một thẻ trạng thái, gồm icon lớn, Headline và các dòng giải thích.
   - Bên dưới là từng nhóm thẻ: số đo, kiểm tra, lịch nhắc, quyền riêng tư. Mỗi dòng có nhãn bên trái, giá trị bên phải.
   - Mục nào không đọc được thì hiện chữ xám kèm một câu giải thích chung. Đừng lặp lại chữ đó ở từng dòng.
   - Có thể cuộn.
2. **Chế độ sáng / tối**
   - Đọc `HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize\AppsUseLightTheme`. Chỉ đọc khoá HKCU, không cần admin.
   - Thanh tiêu đề tối: dùng `DwmSetWindowAttribute(DWMWA_USE_IMMERSIVE_DARK_MODE = 20)`.
   - Nếu muốn có nền Mica trên Windows 11, dùng `DWMWA_SYSTEMBACKDROP_TYPE = 38`. Trên Windows 10 phải tự lùi về nền thường.
3. **Icon khay**
   - Vẽ riêng ở các cỡ 16, 20, 24, 32 theo DPI (`SystemInformation.SmallIconSize`, hoặc `LoadIconMetric`).
   - Nét đậm hơn khi vẽ cỡ 16.
   - Nhớ giải phóng `HICON` như `MakeIcon` đang làm.
4. **Menu khay**
   - Viết `ToolStripRenderer` riêng: bo góc, khoảng đệm rộng, highlight màu nhấn nhạt, theo sáng/tối.
5. **Icon app**
   - Làm một file `.ico` nhiều cỡ (16–256) và gắn vào `ApplicationIcon`, `StatusForm.Icon`, `SetupIconFile` của Inno.
   - Toast dùng logo PNG qua `AddAppLogoOverride(new Uri(<đường dẫn file>), ToastGenericAppLogoCrop.Circle)`. File phải nằm trên ổ đĩa, ví dụ chép ra cạnh exe hoặc vào `%LOCALAPPDATA%\SmartCarePC`. Nếu ghi vào `%LOCALAPPDATA%` thì phần gỡ phải xoá được.
6. **Bộ cài**
   - Ảnh `WizardImageFile` / `WizardSmallImageFile` cùng tông màu.

## 5. Build, chạy, test

Cần .NET 8 SDK. Cài cho riêng người dùng, không cần admin:

```powershell
powershell -ExecutionPolicy Bypass -c "& ([scriptblock]::Create((iwr https://dot.net/v1/dotnet-install.ps1))) -Channel 8.0"
$env:PATH = "$env:LOCALAPPDATA\Microsoft\dotnet;$env:PATH"
```

Trên Mac hoặc Linux thì dùng `dotnet-install.sh -c 8.0`. Ở đó chỉ build và test được, không chạy được app.

```bash
cd desktop-agent
dotnet build src/SmartCarePC.Helper -c Release   # phải ra 0 cảnh báo
dotnet test                                       # hiện có 125 test, phải qua hết
dotnet run --project src/SmartCarePC.Helper       # chỉ trên Windows: icon hiện ở khay
dotnet publish src/SmartCarePC.Helper -c Release -r win-x64 --self-contained -p:PublishSingleFile=true -o publish/win-x64
```

- Muốn thử thông báo và link từ web: chạy `SmartCarePCHelper.exe --register` một lần, rồi chạy `tools/send-test-plan.ps1`. Danh sách đủ 15 bước thử nằm trong `desktop-agent/README.md`.
- Trên Windows: chụp ảnh cửa sổ Status, menu và icon khay ở 100% và 150% DPI, cả sáng lẫn tối, để Minh xem trước và sau.
- Không chạy app bằng quyền admin để thử. App phải chạy được với tài khoản thường. Với tài khoản thường, nhiệt CPU và quạt thường hiện "not available", đó là bình thường.
- Gỡ xong phải chạy `SmartCarePCHelper.exe --uninstall` để xoá khoá Registry và đăng ký thông báo đã ghi lúc thử.

## 6. Xong khi nào

- `StatusForm` không còn là một TextBox. Đọc dễ ở 100% và 150% DPI, theo được sáng/tối.
- Icon khay rõ ở 16 px, 4 mức phân biệt được bằng hình chứ không chỉ bằng màu.
- App có icon riêng (exe, cửa sổ, toast, bộ cài).
- Build ra 0 cảnh báo, test qua hết, không đổi logic trong `SmartCarePC.Helper.Core`.
- README được cập nhật nếu thêm file ảnh hoặc bước build mới.
- Báo cáo ngắn bằng tiếng Việt cho Minh, có ảnh chụp. Ghi rõ cái nào đã thử trên Windows thật, cái nào chưa.
