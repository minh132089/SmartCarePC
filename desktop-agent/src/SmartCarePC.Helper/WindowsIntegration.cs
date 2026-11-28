using System.Diagnostics;
using System.IO.Pipes;
using System.Text;
using Microsoft.Win32;
using SmartCarePC.Helper.Core;

namespace SmartCarePC.Helper;

/// <summary>Đường dẫn, Registry (chỉ HKCU, không cần admin), mở trình duyệt, nhật ký.</summary>
public static class WindowsIntegration
{
    public const string AppName = "SmartCarePC Helper";
    public const string MutexName = "SmartCarePCHelper.Running"; // bộ cài Inno Setup dùng cùng tên (AppMutex)
    const string RunKey = @"Software\Microsoft\Windows\CurrentVersion\Run";
    const string ProtocolKey = @"Software\Classes\" + PlanLink.Scheme;

    /// <summary>%LOCALAPPDATA%\SmartCarePC\: số đo, lịch, hàng đợi sự kiện. Không rời khỏi máy.</summary>
    public static string DataDir => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "SmartCarePC");
    public static string ExePath => Environment.ProcessPath ?? Application.ExecutablePath;

    public static bool StartsWithWindows
    {
        get
        {
            using var k = Registry.CurrentUser.OpenSubKey(RunKey);
            return k?.GetValue(AppName) is string s && s.Contains(Path.GetFileName(ExePath), StringComparison.OrdinalIgnoreCase);
        }
        set
        {
            using var k = Registry.CurrentUser.CreateSubKey(RunKey);
            if (value) k.SetValue(AppName, $"\"{ExePath}\" --background");
            else k.DeleteValue(AppName, throwOnMissingValue: false);
        }
    }

    /// <summary>Đăng ký smartcarepc:// trong HKCU\Software\Classes. Bộ cài cũng làm việc này; lệnh --register dùng khi thử bản không cài.</summary>
    public static void RegisterProtocol()
    {
        using var k = Registry.CurrentUser.CreateSubKey(ProtocolKey);
        k.SetValue("", "URL:" + AppName);
        k.SetValue("URL Protocol", "");
        using (var icon = k.CreateSubKey("DefaultIcon")) icon.SetValue("", $"\"{ExePath}\",0");
        using var cmd = k.CreateSubKey(@"shell\open\command");
        cmd.SetValue("", $"\"{ExePath}\" \"%1\"");
    }

    /// <summary>Gỡ sạch những gì app tự ghi: khoá URL scheme, mục khởi động, đăng ký thông báo.</summary>
    public static void Uninstall()
    {
        try { Registry.CurrentUser.DeleteSubKeyTree(ProtocolKey, throwOnMissingSubKey: false); } catch { }
        try { StartsWithWindows = false; } catch { }
        try { Microsoft.Toolkit.Uwp.Notifications.ToastNotificationManagerCompat.Uninstall(); } catch { }
    }

    /// <summary>
    /// Chỉ mở địa chỉ của web SmartCarePC (origin trong danh sách) hoặc trang dọn ổ đĩa của Windows.
    /// Tham số thông báo có thể bị app khác giả, nên kiểm tra lại ở đây.
    /// </summary>
    public static bool IsSafeUrl(string? url) =>
        url is not null && url.Length < 512 &&
        (url == "ms-settings:storagesense" || PlanLink.AllowedOrigins.Any(o => url.StartsWith(o + "/", StringComparison.Ordinal)));

    public static void OpenUrl(string? url)
    {
        if (url is null) return;
        if (!IsSafeUrl(url)) { Log.Write("refused to open url"); return; }
        try { Process.Start(new ProcessStartInfo(url!) { UseShellExecute = true }); }
        catch (Exception e) { Log.Write("open url failed: " + e.GetType().Name); }
    }
}

/// <summary>Nhật ký ngắn trong thư mục dữ liệu, không ghi tên máy, tên người dùng hay đường dẫn cá nhân. Giữ dưới 256 KB.</summary>
public static class Log
{
    static readonly object gate = new();
    public static void Write(string line)
    {
        try
        {
            lock (gate)
            {
                var path = Path.Combine(WindowsIntegration.DataDir, "log.txt");
                Directory.CreateDirectory(WindowsIntegration.DataDir);
                if (File.Exists(path) && new FileInfo(path).Length > 256 * 1024) File.Move(path, path + ".old", overwrite: true);
                File.AppendAllText(path, $"{DateTimeOffset.Now:yyyy-MM-dd HH:mm:ss} {line}{Environment.NewLine}");
            }
        }
        catch { }
    }
}

/// <summary>
/// Chỉ chạy một bản. Bản thứ hai (do trình duyệt mở link smartcarepc://) chuyển link cho bản đang chạy qua named pipe
/// chỉ cho chính người dùng này (PipeOptions.CurrentUserOnly), rồi thoát. Không mở cổng mạng nào.
/// </summary>
public static class SingleInstance
{
    static string PipeName => $"SmartCarePCHelper.{Process.GetCurrentProcess().SessionId}";

    public static void Send(string link)
    {
        try
        {
            using var c = new NamedPipeClientStream(".", PipeName, PipeDirection.Out, PipeOptions.CurrentUserOnly);
            c.Connect(3000);
            var bytes = Encoding.UTF8.GetBytes(link.Length > PlanLink.MaxLinkLength ? link[..(PlanLink.MaxLinkLength + 1)] : link);
            c.Write(bytes, 0, bytes.Length);
        }
        catch (Exception e) { Log.Write("could not pass link to running app: " + e.GetType().Name); }
    }

    public static void Listen(Action<string> onLink, CancellationToken stop)
    {
        var t = new Thread(() =>
        {
            while (!stop.IsCancellationRequested)
            {
                try
                {
                    using var s = new NamedPipeServerStream(PipeName, PipeDirection.In, 1, PipeTransmissionMode.Byte, PipeOptions.Asynchronous | PipeOptions.CurrentUserOnly);
                    s.WaitForConnectionAsync(stop).GetAwaiter().GetResult();
                    var buf = new byte[PlanLink.MaxLinkLength + 2];
                    int total = 0, n;
                    while (total < buf.Length && (n = s.Read(buf, total, buf.Length - total)) > 0) total += n;
                    onLink(Encoding.UTF8.GetString(buf, 0, total));
                }
                catch (OperationCanceledException) { return; }
                catch (Exception e) { Log.Write("pipe: " + e.GetType().Name); Thread.Sleep(1000); }
            }
        }) { IsBackground = true, Name = "link-listener" };
        t.Start();
    }
}
