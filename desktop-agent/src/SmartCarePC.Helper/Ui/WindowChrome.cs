namespace SmartCarePC.Helper.Ui;

/// <summary>Thanh tiêu đề và góc bo theo chế độ sáng / tối. Windows 10 không hỗ trợ màu thanh tiêu đề thì giữ mặc định.</summary>
public static class WindowChrome
{
    const int DarkMode = 20, DarkModeBefore20H1 = 19, CornerPreference = 33, CaptionColor = 35, TextColor = 36;
    const int RoundSmall = 3, DefaultColor = unchecked((int)0xFFFFFFFF);

    public static bool IsWindows11 => Environment.OSVersion.Version.Build >= 22000;

    public static void TitleBar(Form form, Palette p)
    {
        if (!form.IsHandleCreated) return;
        var h = form.Handle;
        var dark = p.Dark ? 1 : 0;
        if (Set(h, DarkMode, dark) != 0) Set(h, DarkModeBefore20H1, dark);
        if (!IsWindows11) return;
        // Cùng màu với nền cửa sổ để thanh tiêu đề liền với nội dung. Tương phản cao thì để Windows tự chọn.
        Set(h, CaptionColor, p.HighContrast ? DefaultColor : ColorRef(p.Canvas));
        Set(h, TextColor, p.HighContrast ? DefaultColor : ColorRef(p.Ink));
    }

    /// <summary>Bo góc nhỏ như menu của Windows 11. Windows 10 giữ góc vuông.</summary>
    public static void RoundCorners(IntPtr handle)
    {
        if (IsWindows11 && handle != IntPtr.Zero) Set(handle, CornerPreference, RoundSmall);
    }

    /// <summary>Thanh cuộn tối trong chế độ tối</summary>
    public static void Scrollbars(Control c, bool dark)
    {
        if (!c.IsHandleCreated) return;
        try { Native.SetWindowTheme(c.Handle, dark ? "DarkMode_Explorer" : "Explorer", null); } catch { }
    }

    /// <summary>Ô chọn (ComboBox) tối trong chế độ tối</summary>
    public static void ComboBox(Control c, bool dark)
    {
        if (!c.IsHandleCreated) return;
        try { Native.SetWindowTheme(c.Handle, dark ? "DarkMode_CFD" : null, null); } catch { }
    }

    static int Set(IntPtr h, int attribute, int value)
    {
        try { return Native.DwmSetWindowAttribute(h, attribute, ref value, sizeof(int)); }
        catch (Exception) { return -1; }
    }

    static int ColorRef(Color c) => c.R | (c.G << 8) | (c.B << 16);
}
