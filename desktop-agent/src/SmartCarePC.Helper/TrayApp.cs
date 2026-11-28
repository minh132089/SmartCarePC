using System.Drawing;
using System.Drawing.Drawing2D;
using System.Runtime.InteropServices;
using Microsoft.Toolkit.Uwp.Notifications;
using SmartCarePC.Helper.Core.Health;

namespace SmartCarePC.Helper;

/// <summary>
/// App chỉ có icon ở khay. Menu (spec 5.3): Status, Pause reminders 1 day / 1 week, Open SmartCarePC, Start with Windows, Quit.
/// Icon và chữ khi rê chuột đổi theo kết luận tổng: xanh dương có dấu tích (ổn), cam có dấu chấm than (cần chú ý), đỏ (cần xử lý ngay).
/// </summary>
public sealed class TrayApp : ApplicationContext
{
    readonly NotifyIcon icon;
    readonly AgentHost host = new();
    readonly CancellationTokenSource stop = new();
    readonly ToolStripMenuItem startup;
    readonly Dictionary<OverallLevel, Icon> icons = new();
    readonly System.Windows.Forms.Timer refresh = new() { Interval = 5000 };
    OverallLevel? shownLevel;
    StatusForm? status;

    public TrayApp(string? link)
    {
        var menu = new ContextMenuStrip();
        menu.Items.Add("Status", null, (_, _) => ShowStatus());
        var pause = new ToolStripMenuItem("Pause reminders");
        pause.DropDownItems.Add("For 1 day", null, (_, _) => host.Pause(TimeSpan.FromDays(1)));
        pause.DropDownItems.Add("For 1 week", null, (_, _) => host.Pause(TimeSpan.FromDays(7)));
        pause.DropDownItems.Add("Resume reminders", null, (_, _) => host.Pause(null));
        menu.Items.Add(pause);
        menu.Items.Add("Open SmartCarePC", null, (_, _) => WindowsIntegration.OpenUrl(host.Origin + "/maintain"));
        startup = new ToolStripMenuItem("Start with Windows") { CheckOnClick = true };
        startup.Click += (_, _) =>
        {
            try { WindowsIntegration.StartsWithWindows = startup.Checked; }
            catch (Exception e) { Log.Write("startup toggle: " + e.GetType().Name); }
            startup.Checked = SafeStartsWithWindows();
        };
        menu.Items.Add(startup);
        menu.Items.Add(new ToolStripSeparator());
        menu.Items.Add("Quit", null, (_, _) => ExitThread());
        menu.Opening += (_, _) => startup.Checked = SafeStartsWithWindows();

        foreach (var level in Enum.GetValues<OverallLevel>()) icons[level] = MakeIcon(level);
        icon = new NotifyIcon { Icon = icons[OverallLevel.Checking], Text = WindowsIntegration.AppName, ContextMenuStrip = menu, Visible = true };
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
        status = new StatusForm(host);
        status.Show();
    }

    /// <summary>Đổi icon và chữ khi rê chuột theo kết luận tổng mới nhất. Chỉ đổi khi có thay đổi.</summary>
    void UpdateIcon()
    {
        if (host.Snapshot?.Overall is not { } o) return;
        if (icon.Text != o.TrayText) icon.Text = o.TrayText;
        if (shownLevel == o.Level) return;
        icon.Icon = icons[o.Level];
        shownLevel = o.Level;
    }

    /// <summary>
    /// Icon vẽ bằng code, không dùng logo hãng nào. Ổn: vòng xanh dương với dấu tích. Cần chú ý: cam với dấu chấm than.
    /// Cần xử lý ngay: đỏ với dấu chấm than. Đang kiểm tra: xám với dấu tích. Hình khác nhau để không chỉ dựa vào màu.
    /// </summary>
    static Icon MakeIcon(OverallLevel level)
    {
        var color = level switch
        {
            OverallLevel.NeedsAttention => Color.FromArgb(230, 140, 0),
            OverallLevel.NeedsHelpNow => Color.FromArgb(210, 35, 35),
            OverallLevel.Checking => Color.FromArgb(120, 120, 120),
            _ => Color.FromArgb(0, 113, 227),
        };
        using var bmp = new Bitmap(32, 32);
        using (var g = Graphics.FromImage(bmp))
        {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.Clear(Color.Transparent);
            using var fill = new SolidBrush(color);
            g.FillEllipse(fill, 1, 1, 30, 30);
            using var pen = new Pen(Color.White, 3.5f) { StartCap = LineCap.Round, EndCap = LineCap.Round, LineJoin = LineJoin.Round };
            if (level is OverallLevel.NeedsAttention or OverallLevel.NeedsHelpNow)
            {
                g.DrawLine(pen, 16, 8, 16, 18);
                using var dot = new SolidBrush(Color.White);
                g.FillEllipse(dot, 14, 21.5f, 4, 4);
            }
            else g.DrawLines(pen, new[] { new PointF(9, 16.5f), new PointF(14, 21.5f), new PointF(23, 11) });
        }
        // Icon.FromHandle không tự giải phóng HICON: sao chép rồi huỷ handle gốc
        var handle = bmp.GetHicon();
        try { using var tmp = Icon.FromHandle(handle); return (Icon)tmp.Clone(); }
        finally { DestroyIcon(handle); }
    }

    [DllImport("user32.dll")]
    static extern bool DestroyIcon(IntPtr handle);

    protected override void ExitThreadCore()
    {
        Log.Write("quit");
        stop.Cancel();
        refresh.Dispose();
        icon.Visible = false;
        icon.Dispose();
        foreach (var i in icons.Values) i.Dispose();
        status?.Close();
        host.Dispose();
        base.ExitThreadCore();
    }
}
