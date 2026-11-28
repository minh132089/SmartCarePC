using System.Drawing;
using System.Drawing.Drawing2D;
using Microsoft.Toolkit.Uwp.Notifications;

namespace SmartCarePC.Helper;

/// <summary>
/// App chỉ có icon ở khay. Menu (spec 5.3): Status, Pause reminders 1 day / 1 week, Open SmartCarePC, Start with Windows, Quit.
/// </summary>
public sealed class TrayApp : ApplicationContext
{
    readonly NotifyIcon icon;
    readonly AgentHost host = new();
    readonly CancellationTokenSource stop = new();
    readonly ToolStripMenuItem startup;
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

        icon = new NotifyIcon { Icon = MakeIcon(), Text = WindowsIntegration.AppName, ContextMenuStrip = menu, Visible = true };
        icon.DoubleClick += (_, _) => ShowStatus();

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

    /// <summary>Icon vẽ bằng code: vòng tròn với dấu tích. Không dùng logo hãng nào.</summary>
    static Icon MakeIcon()
    {
        using var bmp = new Bitmap(32, 32);
        using (var g = Graphics.FromImage(bmp))
        {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.Clear(Color.Transparent);
            using var fill = new SolidBrush(Color.FromArgb(0, 113, 227));
            g.FillEllipse(fill, 1, 1, 30, 30);
            using var pen = new Pen(Color.White, 3.5f) { StartCap = LineCap.Round, EndCap = LineCap.Round, LineJoin = LineJoin.Round };
            g.DrawLines(pen, new[] { new PointF(9, 16.5f), new PointF(14, 21.5f), new PointF(23, 11) });
        }
        return Icon.FromHandle(bmp.GetHicon());
    }

    protected override void ExitThreadCore()
    {
        Log.Write("quit");
        stop.Cancel();
        icon.Visible = false;
        icon.Dispose();
        status?.Close();
        host.Dispose();
        base.ExitThreadCore();
    }
}
