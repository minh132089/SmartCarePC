using System.Runtime.InteropServices;

namespace SmartCarePC.Helper.Ui;

/// <summary>Các hàm Windows dùng cho giao diện. Không hàm nào cần quyền admin.</summary>
static class Native
{
    [DllImport("dwmapi.dll")]
    internal static extern int DwmSetWindowAttribute(IntPtr hwnd, int attribute, ref int value, int size);

    [DllImport("uxtheme.dll", CharSet = CharSet.Unicode)]
    internal static extern int SetWindowTheme(IntPtr hwnd, string? appName, string? idList);

    [DllImport("user32.dll")]
    internal static extern uint GetDpiForSystem();

    [DllImport("user32.dll")]
    internal static extern uint GetDpiForWindow(IntPtr hwnd);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    internal static extern IntPtr FindWindow(string className, string? windowName);

    [DllImport("user32.dll")]
    internal static extern bool DestroyIcon(IntPtr handle);

    /// <summary>DPI hệ thống, tức DPI mà Windows dùng khi đổi cỡ chữ tính bằng point ra điểm ảnh</summary>
    internal static int SystemDpi
    {
        get
        {
            try { return (int)Math.Max(96, GetDpiForSystem()); } catch { return 96; }
        }
    }

    /// <summary>DPI của thanh taskbar chính, nơi có icon khay. Không tìm được thì dùng DPI hệ thống.</summary>
    internal static int TaskbarDpi
    {
        get
        {
            try
            {
                var bar = FindWindow("Shell_TrayWnd", null);
                if (bar != IntPtr.Zero && GetDpiForWindow(bar) is var dpi and > 0) return (int)dpi;
            }
            catch { }
            return SystemDpi;
        }
    }
}
