using System.Diagnostics.Eventing.Reader;
using System.Management;
using System.Runtime.InteropServices;

namespace SmartCarePC.Helper.Sensors;

/// <summary>Các kiểm tra không cần cảm biến đặc biệt và không cần admin. Trả null khi không đọc được.</summary>
public static class SystemChecks
{
    /// <summary>
    /// Tình trạng ổ đĩa từ MSFT_PhysicalDisk (cùng nguồn với lệnh Get-PhysicalDisk), đọc được bằng tài khoản thường.
    /// HealthStatus: 0 Healthy, 1 Warning, 2 Unhealthy, 5 Unknown.
    /// </summary>
    public static IReadOnlyList<string>? DiskHealth()
    {
        try
        {
            using var searcher = new ManagementObjectSearcher(@"root\Microsoft\Windows\Storage", "SELECT HealthStatus FROM MSFT_PhysicalDisk");
            var list = new List<string>();
            foreach (var o in searcher.Get())
            {
                using (o)
                {
                    list.Add(Convert.ToInt32(o["HealthStatus"]) switch { 0 => "Healthy", 1 => "Warning", 2 => "Unhealthy", _ => "Unknown" });
                }
            }
            return list;
        }
        catch (Exception) { return null; }
    }

    public static (long? Free, long? Total) SystemDrive()
    {
        try
        {
            var d = new DriveInfo(Path.GetPathRoot(Environment.SystemDirectory)!);
            return (d.TotalFreeSpace, d.TotalSize);
        }
        catch (Exception) { return (null, null); }
    }

    /// <summary>Các lần tắt máy đột ngột trong 8 ngày qua (Event Log System, Microsoft-Windows-Kernel-Power, mã 41)</summary>
    public static IReadOnlyList<DateTimeOffset>? UnexpectedShutdowns() =>
        SystemEvents("Provider[@Name='Microsoft-Windows-Kernel-Power'] and (EventID=41)");

    /// <summary>
    /// Màn hình xanh trong 8 ngày qua: mã 1001, nguồn hiện trong Event Viewer là "BugCheck".
    /// Trên Windows 10/11 tên provider thật là Microsoft-Windows-WER-SystemErrorReporting (EventSourceName = BugCheck).
    /// Lọc theo cả hai tên, cần thử trên máy thật xem tên nào khớp.
    /// </summary>
    public static IReadOnlyList<DateTimeOffset>? BlueScreens() =>
        SystemEvents("Provider[@Name='Microsoft-Windows-WER-SystemErrorReporting' or @Name='BugCheck'] and (EventID=1001)");

    /// <summary>Lỗi đọc / ghi ổ đĩa trong 8 ngày qua: nguồn "disk", mã 7 (khối hỏng), 51 (lỗi khi ghi trang), 153 (phải thử lại)</summary>
    public static IReadOnlyList<DateTimeOffset>? DiskErrors() =>
        SystemEvents("Provider[@Name='disk'] and (EventID=7 or EventID=51 or EventID=153)");

    /// <summary>Lỗi phần cứng trong 8 ngày qua: Microsoft-Windows-WHEA-Logger, mã 17, 18, 19, 47</summary>
    public static IReadOnlyList<DateTimeOffset>? HardwareErrors() =>
        SystemEvents("Provider[@Name='Microsoft-Windows-WHEA-Logger'] and (EventID=17 or EventID=18 or EventID=19 or EventID=47)");

    /// <summary>
    /// Đọc thời điểm các sự kiện trong System log, 8 ngày gần nhất (691200000 ms), tối đa 100 sự kiện.
    /// Tài khoản thường đọc được System log. null = không đọc được.
    /// </summary>
    static IReadOnlyList<DateTimeOffset>? SystemEvents(string filter)
    {
        try
        {
            var xpath = $"*[System[{filter} and TimeCreated[timediff(@SystemTime) <= 691200000]]]";
            using var reader = new EventLogReader(new EventLogQuery("System", PathType.LogName, xpath));
            var list = new List<DateTimeOffset>();
            for (var i = 0; i < 100 && reader.ReadEvent() is { } e; i++)
                using (e) if (e.TimeCreated is { } t) list.Add(new DateTimeOffset(t));
            return list;
        }
        catch (Exception) { return null; }
    }

    /// <summary>
    /// Lần khởi động thật gần nhất (Win32_OperatingSystem.LastBootUpTime, đọc được bằng tài khoản thường).
    /// Shut down kiểu fast startup không đổi giá trị này, chỉ Restart mới đổi. null = không đọc được.
    /// </summary>
    public static DateTimeOffset? LastBoot()
    {
        try
        {
            using var searcher = new ManagementObjectSearcher(@"root\CIMV2", "SELECT LastBootUpTime FROM Win32_OperatingSystem");
            foreach (var o in searcher.Get())
                using (o)
                    if (o["LastBootUpTime"] is string s) return new DateTimeOffset(ManagementDateTimeConverter.ToDateTime(s));
            return null;
        }
        catch (Exception) { return null; }
    }

    /// <summary>
    /// Người dùng đang chơi game toàn màn hình, đang trình chiếu hoặc đang bận (Focus Assist). Khi đó không hiện nhắc bảo dưỡng.
    /// </summary>
    public static bool UserBusy()
    {
        try
        {
            if (SHQueryUserNotificationState(out var s) != 0) return false;
            // 2 QUNS_BUSY, 3 QUNS_RUNNING_D3D_FULL_SCREEN, 4 QUNS_PRESENTATION_MODE, 7 QUNS_APP (app toàn màn hình)
            return s is 2 or 3 or 4 or 7;
        }
        catch (Exception) { return false; }
    }

    [DllImport("shell32.dll")]
    static extern int SHQueryUserNotificationState(out int state);
}
