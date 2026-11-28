using System.Drawing.Drawing2D;

namespace SmartCarePC.Helper.Ui;

/// <summary>
/// Vẽ menu khay theo kiểu Windows 11: nền trơn, ô được chọn bo góc tô màu nhấn nhạt, dấu tích tự vẽ, theo sáng / tối.
/// Góc bo của cả menu do Windows 11 vẽ (WindowChrome.RoundCorners). Windows 10 giữ góc vuông có viền mảnh.
/// </summary>
public sealed class MenuRenderer : ToolStripRenderer
{
    static Palette P => Theme.Current;

    protected override void OnRenderToolStripBackground(ToolStripRenderEventArgs e) =>
        e.Graphics.Clear(P.Surface);

    protected override void OnRenderToolStripBorder(ToolStripRenderEventArgs e)
    {
        if (WindowChrome.IsWindows11 && !P.HighContrast) return;
        using var pen = new Pen(P.HighContrast ? P.Ink : P.Line);
        var r = e.AffectedBounds;
        e.Graphics.DrawRectangle(pen, r.X, r.Y, r.Width - 1, r.Height - 1);
    }

    protected override void OnRenderImageMargin(ToolStripRenderEventArgs e) { }

    protected override void OnRenderMenuItemBackground(ToolStripItemRenderEventArgs e)
    {
        if (!e.Item.Selected || !e.Item.Enabled) return;
        var dx = e.Item.Owner?.LogicalToDeviceUnits(4) ?? 4;
        var r = new RectangleF(dx, 1, e.Item.Width - 2 * dx, e.Item.Height - 2);
        Draw.Fill(e.Graphics, r, dx, P.HighContrast ? P.Accent : P.AccentSoft);
    }

    protected override void OnRenderItemText(ToolStripItemTextRenderEventArgs e)
    {
        var item = e.Item as ToolStripMenuItem;
        // ShortcutKeyDisplayString dùng để hiện kết luận tổng bên phải dòng Status, chữ xám
        var secondary = item is not null && e.Text == item.ShortcutKeyDisplayString && e.Text != item.Text;
        var color = !e.Item.Enabled ? P.Ink3
            : P.HighContrast && e.Item.Selected ? P.OnAccent
            : secondary ? P.Ink2 : P.Ink;
        TextRenderer.DrawText(e.Graphics, e.Text, e.TextFont, e.TextRectangle, color, e.TextFormat | TextFormatFlags.NoPrefix);
    }

    protected override void OnRenderItemCheck(ToolStripItemImageRenderEventArgs e)
    {
        var r = e.ImageRectangle;
        var side = Math.Min(r.Width, r.Height) * 0.8f;
        IconArt.DrawTone(e.Graphics, Core.Display.CheckTone.Ok, new RectangleF(r.X + (r.Width - side) / 2, r.Y + (r.Height - side) / 2, side, side),
            P.HighContrast && e.Item.Selected ? P.OnAccent : P.Accent, P.Surface);
    }

    protected override void OnRenderSeparator(ToolStripSeparatorRenderEventArgs e)
    {
        var dx = e.Item.Owner?.LogicalToDeviceUnits(10) ?? 10;
        Draw.HLine(e.Graphics, P.HighContrast ? P.Ink : P.Line, dx, e.Item.Width - dx, e.Item.Height / 2);
    }

    protected override void OnRenderArrow(ToolStripArrowRenderEventArgs e)
    {
        var r = e.ArrowRectangle;
        var s = Math.Min(r.Width, r.Height) / 4f;
        float cx = r.X + r.Width / 2f, cy = r.Y + r.Height / 2f;
        IconArt.Smooth(e.Graphics);
        using var pen = new Pen(e.Item?.Enabled == false ? P.Ink3 : P.Ink2, Math.Max(1.2f, s / 2.2f)) { StartCap = LineCap.Round, EndCap = LineCap.Round, LineJoin = LineJoin.Round };
        e.Graphics.DrawLines(pen, new[] { new PointF(cx - s / 2, cy - s), new PointF(cx + s / 2, cy), new PointF(cx - s / 2, cy + s) });
    }
}
