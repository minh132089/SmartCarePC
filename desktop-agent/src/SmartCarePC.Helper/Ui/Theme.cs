using Microsoft.Win32;

namespace SmartCarePC.Helper.Ui;

public enum Weight { Regular, Semibold }

/// <summary>
/// Chế độ sáng / tối theo Windows (chỉ đọc khoá HKCU, không cần admin) và font có sẵn trong Windows, không tải gì từ mạng.
/// </summary>
public static class Theme
{
    const string PersonalizeKey = @"Software\Microsoft\Windows\CurrentVersion\Themes\Personalize";
    static readonly Dictionary<(string, float, FontStyle), Font> fonts = new();
    static readonly Dictionary<string, bool> installed = new();
    static Palette? current;
    static bool watching;

    /// <summary>Có thì ép "light", "dark" hoặc "contrast" (màu hệ thống), dùng khi chụp ảnh giao diện. Mặc định đọc biến môi trường SMARTCAREPC_THEME.</summary>
    public static string? Override { get; set; } = Environment.GetEnvironmentVariable("SMARTCAREPC_THEME");

    /// <summary>Báo khi người dùng đổi sáng / tối hoặc bật tương phản cao. Có thể chạy trên luồng khác luồng giao diện.</summary>
    public static event Action? Changed;

    public static Palette Current => current ??= Read();

    /// <summary>Đọc lại chế độ màu. Gọi một lần lúc mở app để theo dõi khi người dùng đổi trong Settings.</summary>
    public static void Watch()
    {
        if (watching) return;
        watching = true;
        SystemEvents.UserPreferenceChanged += (_, e) =>
        {
            if (e.Category is UserPreferenceCategory.General or UserPreferenceCategory.Color or UserPreferenceCategory.Accessibility) Refresh();
        };
    }

    public static void Refresh()
    {
        var next = Read();
        if (next == current) return;
        current = next;
        Changed?.Invoke();
    }

    static Palette Read()
    {
        if (SystemInformation.HighContrast) return Palette.FromSystem();
        switch (Override?.Trim().ToLowerInvariant())
        {
            case "dark": return Palette.Night;
            case "light": return Palette.Light;
            case "contrast": return Palette.FromSystem();
        }
        try
        {
            using var key = Registry.CurrentUser.OpenSubKey(PersonalizeKey);
            return key?.GetValue("AppsUseLightTheme") is int light && light == 0 ? Palette.Night : Palette.Light;
        }
        catch (Exception) { return Palette.Light; }
    }

    /// <summary>
    /// Font cho cửa sổ ở DPI <paramref name="dpi"/>. Windows 11 có Segoe UI Variable, Windows 10 dùng Segoe UI. Font được giữ lại dùng chung,
    /// người gọi không Dispose.
    /// </summary>
    public static Font Font(float points, int dpi, Weight weight = Weight.Regular, bool display = false)
    {
        var family = (display, weight) switch
        {
            (true, Weight.Semibold) => Pick("Segoe UI Variable Display Semib", "Segoe UI Semibold"),
            (true, _) => Pick("Segoe UI Variable Display", "Segoe UI"),
            (false, Weight.Semibold) => Pick("Segoe UI Variable Text Semibold", "Segoe UI Semibold"),
            _ => Pick("Segoe UI Variable Text", "Segoe UI"),
        };
        // Windows đổi point ra điểm ảnh theo DPI hệ thống. Cửa sổ nằm trên màn hình có DPI khác thì phải bù.
        var size = (float)Math.Round(points * dpi / Native.SystemDpi, 2);
        // Không có font Semibold riêng thì lấy chữ đậm của font hệ thống
        var style = family is null && weight == Weight.Semibold ? FontStyle.Bold : FontStyle.Regular;
        var key = (family ?? "", size, style);
        lock (fonts)
        {
            if (fonts.TryGetValue(key, out var f)) return f;
            f = family is null
                ? new Font(SystemFonts.MessageBoxFont?.FontFamily ?? FontFamily.GenericSansSerif, size, style)
                : new Font(family, size, style);
            fonts[key] = f;
            return f;
        }
    }

    static string? Pick(params string[] names)
    {
        lock (installed)
        {
            foreach (var name in names)
            {
                if (!installed.TryGetValue(name, out var ok))
                {
                    try { using var family = new FontFamily(name); ok = true; } catch (ArgumentException) { ok = false; }
                    installed[name] = ok;
                }
                if (ok) return name;
            }
            return null;
        }
    }
}
