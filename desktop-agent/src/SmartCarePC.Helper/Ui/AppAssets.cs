namespace SmartCarePC.Helper.Ui;

/// <summary>
/// Icon app và logo thông báo, đi kèm trong file exe (Assets/, vẽ bằng tools/HelperUi). Không tải gì từ mạng.
/// </summary>
public static class AppAssets
{
    const string IconResource = "SmartCarePC.Helper.app.ico";
    const string LogoResource = "SmartCarePC.Helper.toast-logo.png";
    static readonly object gate = new();
    static Uri? logo;
    static bool missing;

    /// <summary>File logo thông báo. WindowsIntegration.Uninstall xoá file này.</summary>
    public static string ToastLogoPath => Path.Combine(WindowsIntegration.DataDir, "toast-logo.png");

    /// <summary>Icon nhiều cỡ cho cửa sổ: Windows tự chọn cỡ hợp cho thanh tiêu đề, taskbar, Alt+Tab</summary>
    public static Icon WindowIcon()
    {
        using var stream = typeof(AppAssets).Assembly.GetManifestResourceStream(IconResource);
        if (stream is not null) return new Icon(stream);
        using var bmp = IconArt.App(32);
        return IconArt.ToIcon(bmp);
    }

    /// <summary>
    /// Logo hiện ở thông báo. Windows chỉ đọc logo từ file trên ổ đĩa, nên chép một lần vào %LOCALAPPDATA%\SmartCarePC.
    /// Lỗi thì trả null: thông báo vẫn hiện, chỉ không có logo.
    /// </summary>
    public static Uri? ToastLogo()
    {
        lock (gate)
        {
            if (logo is not null || missing) return logo;
            try
            {
                using var stream = typeof(AppAssets).Assembly.GetManifestResourceStream(LogoResource);
                if (stream is null) { missing = true; return null; }
                using var bytes = new MemoryStream();
                stream.CopyTo(bytes);
                var data = bytes.ToArray();
                var path = ToastLogoPath;
                // So cả nội dung: bản app mới có logo khác cùng cỡ file vẫn được chép lại
                if (!File.Exists(path) || !File.ReadAllBytes(path).AsSpan().SequenceEqual(data))
                {
                    Directory.CreateDirectory(WindowsIntegration.DataDir);
                    var temp = path + ".tmp";
                    File.WriteAllBytes(temp, data);
                    File.Move(temp, path, overwrite: true);
                }
                logo = new Uri(path);
            }
            // Lỗi ghi file thì lần thông báo sau thử lại
            catch (Exception e) { Log.Write("toast logo: " + e.GetType().Name); }
            return logo;
        }
    }
}
