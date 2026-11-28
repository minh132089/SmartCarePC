namespace SmartCarePC.Helper.Ui;

/// <summary>Nút chính màu nhấn, bo góc, có trạng thái rê chuột, nhấn, tắt và viền khi được chọn bằng bàn phím</summary>
public sealed class AccentButton : Button
{
    Palette palette = Palette.Light;
    bool hover, pressed;

    public AccentButton()
    {
        SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint | ControlStyles.OptimizedDoubleBuffer, true);
        FlatStyle = FlatStyle.Flat;
        FlatAppearance.BorderSize = 0;
        Cursor = Cursors.Hand;
    }

    public Palette Palette
    {
        get => palette;
        set { palette = value; Invalidate(); }
    }

    Font Label => Theme.Font(10f, DeviceDpi, Weight.Semibold);

    /// <summary>Báo trình đọc màn hình khi tên nút đổi (ví dụ "Saved")</summary>
    public void AnnounceName() => AccessibilityNotifyClients(AccessibleEvents.NameChange, -1);

    public override Size GetPreferredSize(Size proposedSize)
    {
        var text = Draw.TextSize(Text, Label);
        return new Size(text.Width + LogicalToDeviceUnits(36), text.Height + LogicalToDeviceUnits(14));
    }

    protected override void OnMouseEnter(EventArgs e) { hover = true; Invalidate(); base.OnMouseEnter(e); }
    protected override void OnMouseLeave(EventArgs e) { hover = pressed = false; Invalidate(); base.OnMouseLeave(e); }
    protected override void OnMouseDown(MouseEventArgs e) { pressed = e.Button == MouseButtons.Left; Invalidate(); base.OnMouseDown(e); }
    protected override void OnMouseUp(MouseEventArgs e) { pressed = false; Invalidate(); base.OnMouseUp(e); }
    protected override void OnEnabledChanged(EventArgs e) { Invalidate(); base.OnEnabledChanged(e); }
    protected override void OnTextChanged(EventArgs e) { Invalidate(); base.OnTextChanged(e); }

    protected override void OnPaint(PaintEventArgs e)
    {
        var g = e.Graphics;
        g.Clear(Parent?.BackColor ?? palette.Surface);
        var fill = !Enabled ? palette.Fill : pressed ? palette.AccentPressed : hover ? palette.AccentHover : palette.Accent;
        var text = !Enabled ? palette.Ink3 : palette.OnAccent;
        var r = new RectangleF(1, 1, Width - 2, Height - 2);
        var radius = LogicalToDeviceUnits(7);
        Draw.Fill(g, r, radius, fill);
        if (Focused && ShowFocusCues)
        {
            // Viền trong màu chữ của nút: thấy rõ trên nền màu nhấn ở cả chế độ sáng lẫn tối
            var inset = LogicalToDeviceUnits(3);
            using var path = IconArt.Rounded(new RectangleF(inset, inset, Width - 2 * inset, Height - 2 * inset), Math.Max(1, radius - inset));
            using var pen = new Pen(text, 1.5f);
            g.DrawPath(pen, path);
        }
        Draw.Text(g, Text, Label, ClientRectangle, text, Draw.Single | TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
    }
}
