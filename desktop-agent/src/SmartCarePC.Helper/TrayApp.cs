using Microsoft.Toolkit.Uwp.Notifications;
using SmartCarePC.Helper.Core.Health;
using SmartCarePC.Helper.Ui;

namespace SmartCarePC.Helper;

/// <summary>
/// App chỉ có icon ở khay. Menu (spec 5.3) dựng ở Ui/TrayMenu.cs. Icon và chữ khi rê chuột đổi theo kết luận tổng:
/// tròn xanh dương có dấu tích (ổn), tam giác cam có chấm than (cần chú ý), bát giác đỏ có chấm than (cần xử lý ngay),
/// tròn xám có ba chấm (đang kiểm tra). Icon vẽ đúng cỡ theo DPI của taskbar (16, 20, 24, 32...) nên không bị mờ.
/// </summary>
public sealed class TrayApp : ApplicationContext
{
    readonly NotifyIcon icon;
    readonly AgentHost host = new();
    readonly CancellationTokenSource stop = new();
    readonly Dictionary<OverallLevel, Icon> icons = new();
    readonly System.Windows.Forms.Timer refresh = new() { Interval = 5000 };
    int iconSize;
    OverallLevel shownLevel = OverallLevel.Checking;
    StatusForm? status;

    public TrayApp(string? link)
    {
        Theme.Watch();
        var menu = TrayMenu.Build(new TrayActions(
            Snapshot: () => host.Snapshot,
            ShowStatus: ShowStatus,
            Pause: host.Pause,
            OpenSite: () => WindowsIntegration.OpenUrl(host.Origin + "/maintain"),
            StartsWithWindows: SafeStartsWithWindows,
            SetStartsWithWindows: on =>
            {
                try { WindowsIntegration.StartsWithWindows = on; }
                catch (Exception e) { Log.Write("startup toggle: " + e.GetType().Name); }
            },
            Quit: ExitThread));

        BuildIcons(TrayIconSize());
        icon = new NotifyIcon { Icon = icons[shownLevel], Text = WindowsIntegration.AppName, ContextMenuStrip = menu, Visible = true };
        icon.DoubleClick += (_, _) => ShowStatus();
        refresh.Tick += (_, _) => UpdateIcon();
        refresh.Start();

        // Bấm vào thông báo. Hàm này chạy trên luồng khác; AgentHost tự xếp hàng việc.
        ToastNotificationManagerCompat.OnActivated += e =>
        {
            if (Toasts.Parse(e.Argument) is { } click) host.HandleClick(click);
        };

        SingleInstance.Listen(host.HandleLink, stop.Token);
        host.Start();
        if (link is not null) host.HandleLink(link);
        Log.Write("started");
    }

    static bool SafeStartsWithWindows()
    {
        try { return WindowsIntegration.StartsWithWindows; } catch { return false; }
    }

    void ShowStatus()
    {
        if (status is { IsDisposed: false }) { status.Activate(); return; }
        status = new StatusForm(() => host.Snapshot, host.SetQuietHours);
        status.Show();
    }

    /// <summary>Cỡ icon khay theo DPI của taskbar: 16 ở 100%, 20 ở 125%, 24 ở 150%, 32 ở 200%</summary>
    static int TrayIconSize() => Math.Clamp((int)Math.Round(16 * Native.TaskbarDpi / 96.0), 16, 64);

    /// <summary>Vẽ lại 4 icon ở cỡ mới. Icon cũ chỉ huỷ sau khi khay đã nhận icon mới.</summary>
    void BuildIcons(int size)
    {
        var old = icons.Values.ToList();
        foreach (var level in Enum.GetValues<OverallLevel>())
        {
            using var bmp = IconArt.Status(level, size);
            icons[level] = IconArt.ToIcon(bmp);
        }
        iconSize = size;
        if (icon is not null) icon.Icon = icons[shownLevel];
        foreach (var i in old) i.Dispose();
    }

    /// <summary>Đổi icon và chữ khi rê chuột theo kết luận tổng mới nhất. Chỉ đổi khi có thay đổi.</summary>
    void UpdateIcon()
    {
        var size = TrayIconSize();
        if (size != iconSize) BuildIcons(size);
        if (host.Snapshot?.Overall is not { } o) return;
        if (icon.Text != o.TrayText) icon.Text = o.TrayText;
        if (shownLevel == o.Level) return;
        icon.Icon = icons[o.Level];
        shownLevel = o.Level;
    }

    protected override void ExitThreadCore()
    {
        Log.Write("quit");
        stop.Cancel();
        refresh.Dispose();
        icon.Visible = false;
        icon.ContextMenuStrip?.Dispose();
        icon.Dispose();
        foreach (var i in icons.Values) i.Dispose();
        status?.Close();
        host.Dispose();
        base.ExitThreadCore();
    }
}
