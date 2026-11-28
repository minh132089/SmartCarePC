using System.Globalization;
using System.Text;
using SmartCarePC.Helper.Core;
using SmartCarePC.Helper.Core.Health;

namespace SmartCarePC.Helper;

/// <summary>Cửa sổ nhỏ Status / Settings: kết luận tổng ở đầu, số đo gần nhất, cảm biến nào đọc được, lịch, giờ yên lặng.</summary>
public sealed class StatusForm : Form
{
    readonly AgentHost host;
    readonly Label headline = new() { Dock = DockStyle.Top, Height = 40, Font = new Font("Segoe UI", 15f, FontStyle.Bold), TextAlign = ContentAlignment.MiddleLeft };
    readonly TextBox text = new() { Multiline = true, ReadOnly = true, ScrollBars = ScrollBars.Vertical, Dock = DockStyle.Fill, BorderStyle = BorderStyle.None, Font = new Font("Segoe UI", 10f) };
    readonly NumericUpDown quietStart = new() { Minimum = 0, Maximum = 23, Width = 50 };
    readonly NumericUpDown quietEnd = new() { Minimum = 0, Maximum = 23, Width = 50 };
    readonly System.Windows.Forms.Timer timer = new() { Interval = 5000 };

    public StatusForm(AgentHost host)
    {
        this.host = host;
        Text = "SmartCarePC Helper";
        Width = 520; Height = 620;
        StartPosition = FormStartPosition.CenterScreen;
        FormBorderStyle = FormBorderStyle.FixedDialog;
        MaximizeBox = false; MinimizeBox = false;
        BackColor = SystemColors.Window;

        var bottom = new FlowLayoutPanel { Dock = DockStyle.Bottom, Height = 44, Padding = new Padding(8), FlowDirection = FlowDirection.LeftToRight };
        bottom.Controls.Add(new Label { Text = "Quiet hours from", AutoSize = true, Padding = new Padding(0, 6, 0, 0) });
        bottom.Controls.Add(quietStart);
        bottom.Controls.Add(new Label { Text = "to", AutoSize = true, Padding = new Padding(0, 6, 0, 0) });
        bottom.Controls.Add(quietEnd);
        var save = new Button { Text = "Save", AutoSize = true };
        save.Click += (_, _) => { host.SetQuietHours((int)quietStart.Value, (int)quietEnd.Value); };
        bottom.Controls.Add(save);
        var panel = new Panel { Dock = DockStyle.Fill, Padding = new Padding(12) };
        // Thêm ô Fill trước, ô Top sau: WinForms xếp ô thêm sau ra phía sau, nên ô Top được đặt chỗ trước rồi ô Fill lấp phần còn lại
        panel.Controls.Add(text);
        panel.Controls.Add(headline);
        Controls.Add(panel);
        Controls.Add(bottom);

        var s = host.Snapshot;
        quietStart.Value = s?.Settings.QuietStartHour ?? 22;
        quietEnd.Value = s?.Settings.QuietEndHour ?? 8;
        timer.Tick += (_, _) => Render();
        timer.Start();
        Render();
    }

    void Render()
    {
        var s = host.Snapshot;
        if (s is null) { headline.Text = "Checking your PC"; text.Text = "Starting…"; return; }
        var b = new StringBuilder();
        const string na = "not available on this PC";
        string Temp(double? v) => v is { } x ? $"{Math.Round(x)} °C" : na;
        string Count(int? n) => n is { } x ? x.ToString(CultureInfo.InvariantCulture) : na;

        // Kết luận tổng: màu chữ theo mức, kèm chữ để không phụ thuộc vào màu
        headline.Text = s.Overall.Headline;
        headline.ForeColor = s.Overall.Level switch
        {
            OverallLevel.RunningWell => Color.FromArgb(0, 128, 60),
            OverallLevel.NeedsAttention => Color.FromArgb(176, 96, 0),
            OverallLevel.NeedsHelpNow => Color.FromArgb(200, 30, 30),
            _ => SystemColors.ControlText,
        };
        foreach (var line in s.Overall.Lines) b.AppendLine(line);
        b.AppendLine();

        b.AppendLine("LATEST READINGS");
        if (s.Last is { } l)
        {
            b.AppendLine($"Processor use: {Math.Round(l.CpuLoad * 100)}%");
            b.AppendLine($"Processor temperature: {Temp(l.CpuTempC)}");
            b.AppendLine($"Processor fan: {(l.CpuFanRpm is { } f ? $"{Math.Round(f)} RPM" : na)}");
            b.AppendLine($"Graphics card temperature: {Temp(l.GpuTempC)}");
            b.AppendLine($"Memory in use: {Math.Round(l.RamUsed * 100)}%");
        }
        b.AppendLine($"Main drive free space: {(s.Drive is { Free: { } fr, Total: { } t } && t > 0 ? $"{Math.Round(100.0 * fr / t)}%" : na)}");
        b.AppendLine($"Drive health: {(s.Disks is { Count: > 0 } d ? string.Join(", ", d) : na)}");
        b.AppendLine($"Unexpected shutdowns this week: {Count(s.ShutdownsThisWeek)}");
        b.AppendLine($"Blue error screens this week: {Count(s.BlueScreensThisWeek)}");
        b.AppendLine($"Drive read and write errors this week: {Count(s.DiskErrorsThisWeek)}");
        b.AppendLine($"Hardware errors this week: {Count(s.HardwareErrorsThisWeek)}");
        b.AppendLine($"Last restart: {(s.LastBoot is { } boot ? LastRestart(boot) : na)}");
        if (s.BaselineSentence is { } bl) { b.AppendLine(); b.AppendLine(bl); }

        b.AppendLine();
        b.AppendLine("CHECKS");
        foreach (var (rule, result) in s.Rules)
            b.AppendLine($"{Rules.DisplayName(rule)}: {(result.Status switch { RuleStatus.Ok => "OK", RuleStatus.Alert => result.Finding?.Urgency == Urgency.Light ? "tip" : "needs attention", _ => "not checked (" + na + ")" })}");

        b.AppendLine();
        b.AppendLine("REMINDERS");
        if (s.Plan is null)
            b.AppendLine("This app only checks your PC's health. To get cleaning reminders by email, set them up on the SmartCarePC website (Open SmartCarePC in the tray menu).");
        else
            foreach (var task in Tasks.All)
                if (s.Plan.Dates.TryGetValue(task, out var date))
                    b.AppendLine($"{TaskName(task)}: {LocalDay.Parse(date).ToString("d MMM yyyy", CultureInfo.InvariantCulture)}");
        if (s.PausedUntil is { } p && p > DateTimeOffset.Now) b.AppendLine($"Reminders paused until {p:d MMM HH:mm}.");

        b.AppendLine();
        b.AppendLine("PRIVACY");
        b.AppendLine(s.Plan?.Study is null
            ? "Your readings stay on this PC. This app sends nothing over the internet."
            : $"You are taking part in the study. Only reminder and click events are sent, with your anonymous code. Waiting to send: {s.PendingEvents}.");

        if (text.Text != b.ToString()) text.Text = b.ToString();
    }

    static string TaskName(MaintenanceTask t) => t switch { MaintenanceTask.Dust => "Clean out dust", MaintenanceTask.Paste => "Replace thermal paste", _ => "Quick health check" };

    static string LastRestart(DateTimeOffset boot)
    {
        var days = (int)Math.Floor((DateTimeOffset.Now - boot).TotalDays);
        return days switch { <= 0 => "today", 1 => "1 day ago", _ => $"{days} days ago" };
    }

    protected override void OnFormClosed(FormClosedEventArgs e)
    {
        timer.Dispose();
        base.OnFormClosed(e);
    }
}
