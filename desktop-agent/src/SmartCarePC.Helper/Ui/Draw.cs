using System.Drawing.Drawing2D;

namespace SmartCarePC.Helper.Ui;

/// <summary>Hàm vẽ dùng chung cho các thẻ: chữ bằng GDI (TextRenderer) cho nét chữ giống Windows, hình bo góc bằng GDI+.</summary>
static class Draw
{
    public const TextFormatFlags Wrap = TextFormatFlags.WordBreak | TextFormatFlags.TextBoxControl | TextFormatFlags.NoPadding | TextFormatFlags.NoPrefix;
    public const TextFormatFlags Single = TextFormatFlags.SingleLine | TextFormatFlags.NoPadding | TextFormatFlags.NoPrefix | TextFormatFlags.EndEllipsis;

    /// <summary>Chiều cao đoạn chữ khi xuống dòng trong bề rộng cho trước</summary>
    public static int TextHeight(string text, Font font, int width) =>
        width <= 0 ? 0 : TextRenderer.MeasureText(text, font, new Size(width, int.MaxValue), Wrap).Height;

    public static Size TextSize(string text, Font font) => TextRenderer.MeasureText(text, font, new Size(int.MaxValue, int.MaxValue), Single);

    public static void Text(Graphics g, string text, Font font, Rectangle r, Color color, TextFormatFlags flags = Wrap) =>
        TextRenderer.DrawText(g, text, font, r, color, flags);

    /// <summary>Thẻ bo góc: nền, viền 1 điểm ảnh, bóng rất nhẹ ở chế độ sáng</summary>
    public static void Card(Graphics g, Rectangle r, int radius, Color fill, Color border, bool shadow)
    {
        IconArt.Smooth(g);
        if (shadow)
        {
            using var s = IconArt.Rounded(new RectangleF(r.X, r.Y + 1.5f, r.Width, r.Height), radius);
            using var sb = new SolidBrush(Color.FromArgb(14, 0, 0, 0));
            g.FillPath(sb, s);
        }
        using var path = IconArt.Rounded(new RectangleF(r.X + 0.5f, r.Y + 0.5f, r.Width - 1, r.Height - 1), radius);
        using var b = new SolidBrush(fill);
        using var p = new Pen(border, 1f);
        g.FillPath(b, path);
        g.DrawPath(p, path);
    }

    public static void Fill(Graphics g, RectangleF r, float radius, Color color)
    {
        IconArt.Smooth(g);
        using var path = IconArt.Rounded(r, radius);
        using var b = new SolidBrush(color);
        g.FillPath(b, path);
    }

    public static void HLine(Graphics g, Color c, int x1, int x2, int y)
    {
        var mode = g.SmoothingMode;
        g.SmoothingMode = SmoothingMode.None;
        using var p = new Pen(c, 1f);
        g.DrawLine(p, x1, y, x2, y);
        g.SmoothingMode = mode;
    }

    /// <summary>Bật vẽ đệm đôi cho control tự vẽ để không nháy khi số đo cập nhật mỗi 5 giây</summary>
    public static void OwnerDrawn(Control c, Action<ControlStyles, bool> setStyle)
    {
        setStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint | ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw, true);
        setStyle(ControlStyles.Selectable, false);
        c.TabStop = false;
    }
}
