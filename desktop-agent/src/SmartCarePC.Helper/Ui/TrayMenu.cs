using SmartCarePC.Helper.Core.Display;
using SmartCarePC.Helper.Core.Health;

namespace SmartCarePC.Helper.Ui;

/// <summary>Việc menu khay gọi tới. Tách riêng để công cụ chụp ảnh dựng được menu mà không chạy app.</summary>
public sealed record TrayActions(
    Func<StatusSnapshot?> Snapshot,
    Action ShowStatus,
    Action<TimeSpan?> Pause,
    Action OpenSite,
    Func<bool> StartsWithWindows,
    Action<bool> SetStartsWithWindows,
    Action Quit);

/// <summary>
/// Menu khay (spec 5.3): Status, Pause reminders 1 day / 1 week, Open SmartCarePC, Start with Windows, Quit.
/// Dòng Status có icon và kết luận tổng hiện tại ở bên phải, để mở menu là biết máy ra sao.
/// </summary>
public static class TrayMenu
{
    public static ContextMenuStrip Build(TrayActions a)
    {
        var renderer = new MenuRenderer();
        var menu = new ContextMenuStrip { Renderer = renderer, ShowImageMargin = true, DropShadowEnabled = !WindowChrome.IsWindows11 };
        var status = new ToolStripMenuItem("Status", null, (_, _) => a.ShowStatus()) { ImageScaling = ToolStripItemImageScaling.None };
        var pause = new ToolStripMenuItem("Pause reminders");
        var resume = new ToolStripMenuItem("Resume reminders", null, (_, _) => a.Pause(null));
        pause.DropDownItems.AddRange([
            new ToolStripMenuItem("For 1 day", null, (_, _) => a.Pause(TimeSpan.FromDays(1))),
            new ToolStripMenuItem("For 1 week", null, (_, _) => a.Pause(TimeSpan.FromDays(7))),
            resume,
        ]);
        pause.DropDown.Renderer = renderer;
        if (pause.DropDown is ToolStripDropDownMenu sub) { sub.ShowImageMargin = true; sub.DropShadowEnabled = menu.DropShadowEnabled; }
        var open = new ToolStripMenuItem("Open SmartCarePC", null, (_, _) => a.OpenSite());
        var startup = new ToolStripMenuItem("Start with Windows") { CheckOnClick = true };
        startup.Click += (_, _) =>
        {
            a.SetStartsWithWindows(startup.Checked);
            startup.Checked = a.StartsWithWindows();
        };
        var quit = new ToolStripMenuItem("Quit", null, (_, _) => a.Quit());
        menu.Items.AddRange([status, new ToolStripSeparator(), pause, open, startup, new ToolStripSeparator(), quit]);

        menu.HandleCreated += (_, _) => WindowChrome.RoundCorners(menu.Handle);
        pause.DropDown.HandleCreated += (_, _) => WindowChrome.RoundCorners(pause.DropDown.Handle);
        // Menu khay mở cạnh taskbar, nên lấy DPI của taskbar (DeviceDpi của menu có thể là của màn hình khác lúc tạo menu)
        pause.DropDownOpening += (_, _) => Style(pause.DropDown, Native.TaskbarDpi);

        Bitmap? statusImage = null;
        menu.Disposed += (_, _) => statusImage?.Dispose();
        menu.Opening += (_, _) =>
        {
            var dpi = Native.TaskbarDpi;
            Style(menu, dpi);
            startup.Checked = a.StartsWithWindows();
            var s = a.Snapshot();
            status.ShortcutKeyDisplayString = s?.Overall.Headline ?? "Checking your PC";
            var old = statusImage;
            statusImage = IconArt.Status(s?.Overall.Level ?? OverallLevel.Checking, (int)Math.Round(16 * dpi / 96.0));
            status.Image = statusImage;
            old?.Dispose();
            resume.Enabled = s?.PausedUntil is { } until && until > DateTimeOffset.Now;
        };
        return menu;
    }

    /// <summary>Font và khoảng đệm theo DPI. Dòng cao hơn mặc định của WinForms để dễ bấm, giống menu Windows 11.</summary>
    static void Style(ToolStripDropDown menu, int dpi)
    {
        int Px(int v) => (int)Math.Round(v * dpi / 96.0);
        menu.Font = Theme.Font(10f, dpi);
        menu.BackColor = Theme.Current.Surface;
        menu.Padding = new Padding(0, Px(4), 0, Px(4));
        menu.ImageScalingSize = new Size(Px(16), Px(16));
        foreach (ToolStripItem item in menu.Items)
        {
            item.Padding = item is ToolStripSeparator ? Padding.Empty : new Padding(Px(2), Px(5), Px(2), Px(5));
            item.Margin = new Padding(0, item is ToolStripSeparator ? Px(2) : 0, 0, item is ToolStripSeparator ? Px(2) : 0);
        }
    }
}
