using SmartCarePC.Helper.Core.Display;
using SmartCarePC.Helper.Ui;

namespace SmartCarePC.Helper;

/// <summary>
/// Cửa sổ Status / Settings: thẻ kết luận tổng ở đầu, rồi các thẻ Checks, Latest readings, Reminders (có ô giờ yên lặng), Privacy.
/// Nội dung lấy từ StatusPage (lõi, có test). Cập nhật mỗi 5 giây tại chỗ, không dựng lại, nên không nháy và không mất vị trí cuộn.
/// Theo chế độ sáng / tối và tương phản cao của Windows. Esc để đóng.
/// </summary>
public sealed class StatusForm : Form
{
    static readonly string[] SectionKeys = ["checks", "readings", "reminders", "privacy"];
    readonly Func<StatusSnapshot?> snapshot;
    readonly ScrollStack stack = new() { Dock = DockStyle.Fill };
    readonly HeroCard hero = new();
    readonly Dictionary<string, SectionCard> cards = new();
    readonly QuietHoursPanel quiet;
    readonly System.Windows.Forms.Timer timer = new() { Interval = 5000 };

    public StatusForm(Func<StatusSnapshot?> snapshot, Action<int, int> saveQuietHours)
    {
        this.snapshot = snapshot;
        Text = WindowsIntegration.AppName;
        AutoScaleMode = AutoScaleMode.None;
        StartPosition = FormStartPosition.CenterScreen;
        FormBorderStyle = FormBorderStyle.Sizable;
        MaximizeBox = false;
        KeyPreview = true;
        Icon = AppAssets.WindowIcon();
        ClientSize = new Size(LogicalToDeviceUnits(480), LogicalToDeviceUnits(660));
        MinimumSize = new Size(LogicalToDeviceUnits(400), LogicalToDeviceUnits(420));

        var settings = snapshot()?.Settings;
        quiet = new QuietHoursPanel(settings?.QuietStartHour ?? 22, settings?.QuietEndHour ?? 8, saveQuietHours);
        stack.Controls.Add(hero);
        foreach (var key in SectionKeys)
        {
            var card = new SectionCard { Visible = false };
            cards[key] = card;
            stack.Controls.Add(card);
        }
        cards["reminders"].Footer = quiet;
        Controls.Add(stack);

        Theme.Changed += OnThemeChanged;
        timer.Tick += (_, _) => Render();
        timer.Start();
        ApplyTheme();
        Render();
    }

    void Render()
    {
        var page = StatusPage.Build(snapshot(), DateTimeOffset.Now);
        hero.Hero = page.Hero;
        foreach (var key in SectionKeys)
        {
            var section = page.Sections.FirstOrDefault(s => s.Key == key);
            cards[key].Section = section;
            cards[key].Visible = section is not null;
        }
    }

    void ApplyTheme()
    {
        var p = Theme.Current;
        BackColor = p.Canvas;
        stack.BackColor = p.Canvas;
        hero.Palette = p;
        foreach (var card in cards.Values) card.Palette = p;
        quiet.Palette = p;
        WindowChrome.TitleBar(this, p);
        stack.Dark = p.Dark;
        Invalidate(true);
    }

    /// <summary>Theme.Changed có thể đến từ luồng khác: chuyển về luồng giao diện</summary>
    void OnThemeChanged()
    {
        if (IsDisposed || !IsHandleCreated) return;
        try { BeginInvoke(ApplyTheme); } catch (InvalidOperationException) { }
    }

    protected override void OnHandleCreated(EventArgs e)
    {
        base.OnHandleCreated(e);
        ApplyTheme();
    }

    protected override void OnDpiChanged(DpiChangedEventArgs e)
    {
        base.OnDpiChanged(e);
        MinimumSize = new Size(LogicalToDeviceUnits(400), LogicalToDeviceUnits(420));
        stack.PerformLayout();
        Invalidate(true);
    }

    /// <summary>Esc đóng cửa sổ. Page Up / Page Down / Home / End / mũi tên cuộn trang, trừ khi đang ở ô chọn giờ.</summary>
    protected override bool ProcessCmdKey(ref Message msg, Keys keyData)
    {
        // Esc khi ô chọn giờ đang mở thì chỉ đóng ô chọn, không đóng cửa sổ
        if (keyData == Keys.Escape && ActiveControl is not ComboBox { DroppedDown: true }) { Close(); return true; }
        if (ActiveControl is not ComboBox)
        {
            var page = stack.ClientSize.Height * 9 / 10;
            var step = LogicalToDeviceUnits(48);
            int? delta = keyData switch
            {
                Keys.PageDown => page,
                Keys.PageUp => -page,
                Keys.Down => step,
                Keys.Up => -step,
                Keys.End => int.MaxValue / 2,
                Keys.Home => int.MinValue / 2,
                _ => null,
            };
            if (delta is { } d)
            {
                var y = Math.Clamp(-stack.AutoScrollPosition.Y + d, 0, Math.Max(0, stack.AutoScrollMinSize.Height - stack.ClientSize.Height));
                stack.AutoScrollPosition = new Point(0, y);
                return true;
            }
        }
        return base.ProcessCmdKey(ref msg, keyData);
    }

    protected override void OnFormClosed(FormClosedEventArgs e)
    {
        Theme.Changed -= OnThemeChanged;
        timer.Dispose();
        Icon?.Dispose();
        base.OnFormClosed(e);
    }
}
