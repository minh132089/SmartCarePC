using System.Globalization;

namespace SmartCarePC.Helper.Ui;

/// <summary>
/// Ô giờ yên lặng ở đáy thẻ Reminders: tiêu đề, một câu giải thích, hai ô chọn giờ và nút Save.
/// Bấm Save thì nút đổi thành "Saved" một lúc để người dùng biết đã lưu.
/// </summary>
public sealed class QuietHoursPanel : Panel
{
    const string Title = "Quiet hours";
    const string Hint = "Reminders wait until these hours are over. Urgent warnings still show.";
    readonly ComboBox from = Hours("Quiet hours start");
    readonly ComboBox to = Hours("Quiet hours end");
    readonly AccentButton save = new() { Text = "Save", AccessibleName = "Save quiet hours" };
    readonly System.Windows.Forms.Timer savedTimer = new() { Interval = 1800 };
    Palette palette = Palette.Light;

    public QuietHoursPanel(int start, int end, Action<int, int> onSave)
    {
        SetStyle(ControlStyles.OptimizedDoubleBuffer | ControlStyles.AllPaintingInWmPaint | ControlStyles.ResizeRedraw, true);
        from.SelectedIndex = Math.Clamp(start, 0, 23);
        to.SelectedIndex = Math.Clamp(end, 0, 23);
        // Nút giữ nguyên bật (tắt nút đang có focus làm mất focus và nhảy trang); trong lúc hiện "Saved" thì bỏ qua cú bấm
        save.Click += (_, _) =>
        {
            if (savedTimer.Enabled) return;
            onSave(from.SelectedIndex, to.SelectedIndex);
            save.Text = "Saved";
            save.AccessibleName = "Quiet hours saved";
            save.AnnounceName();
            savedTimer.Start();
        };
        savedTimer.Tick += (_, _) => { savedTimer.Stop(); save.Text = "Save"; save.AccessibleName = "Save quiet hours"; };
        Controls.AddRange([from, to, save]);
        AccessibleName = Title;
        AccessibleDescription = Hint;
        ApplyFonts();
    }

    void ApplyFonts()
    {
        foreach (var box in new[] { from, to }) box.Font = BodyFont;
    }

    static ComboBox Hours(string name)
    {
        var box = new ComboBox { DropDownStyle = ComboBoxStyle.DropDownList, AccessibleName = name };
        for (var h = 0; h < 24; h++) box.Items.Add(h.ToString("00", CultureInfo.InvariantCulture) + ":00");
        box.HandleCreated += (_, _) => WindowChrome.ComboBox(box, Theme.Current.Dark);
        return box;
    }

    public Palette Palette
    {
        get => palette;
        set
        {
            palette = value;
            BackColor = value.Surface;
            save.Palette = value;
            // Ô chọn giữ kiểu gốc của Windows; chế độ tối dùng giao diện tối có sẵn của Windows cho ô chọn
            foreach (var box in new[] { from, to })
            {
                box.BackColor = value.Dark ? value.Surface : SystemColors.Window;
                box.ForeColor = value.Dark ? value.Ink : SystemColors.WindowText;
                WindowChrome.ComboBox(box, value.Dark);
            }
            Invalidate();
        }
    }

    int Px(int logical) => LogicalToDeviceUnits(logical);
    Font TitleFont => Theme.Font(10f, DeviceDpi, Weight.Semibold);
    Font HintFont => Theme.Font(9f, DeviceDpi);
    Font BodyFont => Theme.Font(10f, DeviceDpi);

    /// <summary>Toạ độ dòng chọn giờ, tính một chỗ để đo và đặt control không lệch nhau</summary>
    (int HintHeight, int RowTop, int RowHeight) Measure(int width)
    {
        var hint = Draw.TextHeight(Hint, HintFont, width);
        var rowTop = TitleFont.Height + Px(2) + hint + Px(10);
        var rowHeight = Math.Max(from.PreferredHeight, save.GetPreferredSize(Size.Empty).Height);
        return (hint, rowTop, rowHeight);
    }

    public override Size GetPreferredSize(Size proposedSize)
    {
        var width = proposedSize.Width > 0 ? proposedSize.Width : Width;
        var (_, top, height) = Measure(width);
        return new Size(width, top + height + Px(2));
    }

    protected override void OnLayout(LayoutEventArgs levent)
    {
        base.OnLayout(levent);
        ApplyFonts();
        var (_, top, height) = Measure(Width);
        var boxWidth = Px(84);
        var toLabel = Draw.TextSize("to", BodyFont).Width;
        from.SetBounds(0, top + (height - from.Height) / 2, boxWidth, from.Height);
        var x = from.Right + Px(8) + toLabel + Px(8);
        to.SetBounds(x, top + (height - to.Height) / 2, boxWidth, to.Height);
        var button = save.GetPreferredSize(Size.Empty);
        save.SetBounds(to.Right + Px(12), top + (height - button.Height) / 2, button.Width, button.Height);
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        base.OnPaint(e);
        var g = e.Graphics;
        var (hint, top, height) = Measure(Width);
        Draw.Text(g, Title, TitleFont, new Rectangle(0, 0, Width, TitleFont.Height), palette.Ink, Draw.Single);
        Draw.Text(g, Hint, HintFont, new Rectangle(0, TitleFont.Height + Px(2), Width, hint), palette.Ink2);
        var toLabel = Draw.TextSize("to", BodyFont);
        Draw.Text(g, "to", BodyFont, new Rectangle(from.Right + Px(8), top + (height - toLabel.Height) / 2, toLabel.Width, toLabel.Height), palette.Ink2, Draw.Single);
    }

    protected override void OnDpiChangedAfterParent(EventArgs e)
    {
        base.OnDpiChangedAfterParent(e);
        PerformLayout();
        Invalidate();
    }

    protected override void Dispose(bool disposing)
    {
        if (disposing) savedTimer.Dispose();
        base.Dispose(disposing);
    }
}
