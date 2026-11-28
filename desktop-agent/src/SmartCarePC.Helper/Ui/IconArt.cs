using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using SmartCarePC.Helper.Core.Display;
using SmartCarePC.Helper.Core.Health;

namespace SmartCarePC.Helper.Ui;

/// <summary>
/// Icon tự vẽ bằng code, không dùng logo hãng nào. Mỗi mức một hình riêng để không chỉ dựa vào màu:
/// ổn = tròn xanh dương có dấu tích, cần chú ý = tam giác cam có chấm than, cần xử lý ngay = bát giác đỏ có chấm than,
/// đang kiểm tra = tròn xám có ba chấm. Vẽ thẳng ở cỡ cần dùng (16, 20, 24, 32...) nên không bị mờ do Windows co ảnh,
/// và nét đậm hơn ở cỡ nhỏ. Toạ độ tính trên lưới 16 × 16 rồi phóng theo cỡ thật.
/// </summary>
public static class IconArt
{
    static readonly Color Blue = Color.FromArgb(0, 113, 227);
    static readonly Color Amber = Color.FromArgb(245, 165, 36);
    static readonly Color Red = Color.FromArgb(217, 45, 32);
    static readonly Color Grey = Color.FromArgb(142, 142, 147);
    static readonly Color Ink = Color.FromArgb(29, 29, 31);
    static readonly Color AppTop = Color.FromArgb(48, 146, 255);
    static readonly Color AppBottom = Color.FromArgb(0, 96, 210);

    /// <summary>Màu icon khay. Không đổi theo chế độ sáng / tối của app vì thanh taskbar có màu riêng.</summary>
    public static Color LevelColor(OverallLevel level) => level switch
    {
        OverallLevel.RunningWell => Blue,
        OverallLevel.NeedsAttention => Amber,
        OverallLevel.NeedsHelpNow => Red,
        _ => Grey,
    };

    public static Bitmap Status(OverallLevel level, int size)
    {
        var bmp = new Bitmap(size, size, PixelFormat.Format32bppArgb);
        using var g = Graphics.FromImage(bmp);
        Smooth(g);
        DrawStatus(g, level, new RectangleF(0, 0, size, size));
        return bmp;
    }

    public static void DrawStatus(Graphics g, OverallLevel level, RectangleF r)
    {
        var px = Math.Min(r.Width, r.Height);
        var state = Grid(g, r);
        var w = Stroke(px);
        switch (level)
        {
            case OverallLevel.NeedsAttention: Triangle(g, Amber); Exclaim(g, Ink, w, 5.5f, 9.3f, 12.1f); break;
            case OverallLevel.NeedsHelpNow: Octagon(g, Red); Exclaim(g, Color.White, w, 4f, 8.9f, 11.9f); break;
            case OverallLevel.Checking: Disc(g, Grey); Dots(g, Color.White, px <= 16 ? 1.4f : 1.25f); break;
            default: Disc(g, Blue); Check(g, Color.White, w, new(4.5f, 8.2f), new(7f, 10.7f), new(11.6f, 5.7f)); break;
        }
        g.Restore(state);
    }

    /// <summary>Hình nhỏ trong nhãn trạng thái của mục Checks. <paramref name="knock"/> là màu nét đục lỗ (thường là màu nền nhãn).</summary>
    public static void DrawTone(Graphics g, CheckTone tone, RectangleF r, Color fg, Color knock)
    {
        var state = Grid(g, r);
        switch (tone)
        {
            case CheckTone.Ok: Check(g, fg, 2.4f, new(2.8f, 8.6f), new(6.4f, 12.1f), new(13.2f, 4.6f)); break;
            case CheckTone.Attention: Triangle(g, fg); Exclaim(g, knock, 2f, 5.6f, 9.3f, 12.1f); break;
            case CheckTone.Urgent: Octagon(g, fg); Exclaim(g, knock, 2f, 4f, 8.9f, 11.9f); break;
            case CheckTone.Tip:
                Disc(g, fg);
                using (var dot = new SolidBrush(knock)) g.FillEllipse(dot, 6.75f, 3.3f, 2.5f, 2.5f);
                Line(g, knock, 2.1f, new(8f, 7.4f), new(8f, 12f));
                break;
            default:
                using (var ring = new Pen(fg, 1.6f)) g.DrawEllipse(ring, 1.3f, 1.3f, 13.4f, 13.4f);
                Line(g, fg, 1.9f, new(4.9f, 8f), new(11.1f, 8f));
                break;
        }
        g.Restore(state);
    }

    /// <summary>
    /// Icon app: ô vuông bo góc xanh dương, màn hình máy tính màu trắng có nhịp tim bên trong ("chăm sóc máy").
    /// fullBleed: tô kín cả ô vuông, dùng cho logo thông báo vì Windows cắt thành hình tròn.
    /// </summary>
    public static Bitmap App(int size, bool fullBleed = false)
    {
        var bmp = new Bitmap(size, size, PixelFormat.Format32bppArgb);
        using var g = Graphics.FromImage(bmp);
        Smooth(g);
        var inset = fullBleed ? 0 : size * (size <= 24 ? 0.03f : 0.045f);
        var box = new RectangleF(inset, inset, size - 2 * inset, size - 2 * inset);
        using (var brush = new LinearGradientBrush(new RectangleF(0, 0, size, size + 1), AppTop, AppBottom, 90f))
        {
            if (fullBleed) g.FillRectangle(brush, 0, 0, size, size);
            else { using var tile = Rounded(box, box.Width * 0.225f); g.FillPath(brush, tile); }
        }
        // Logo tròn: thu nhỏ hình để không bị cắt mất góc
        var glyph = fullBleed ? Inflate(box, -box.Width * 0.12f) : box;
        Monitor(g, glyph, size);
        return bmp;
    }

    /// <summary>Ảnh dọc cho cột trái bộ cài (Inno WizardImageFile). BMP không có nền trong suốt nên tô kín.</summary>
    public static Bitmap WizardTall(int width, int height)
    {
        var bmp = new Bitmap(width, height, PixelFormat.Format24bppRgb);
        using var g = Graphics.FromImage(bmp);
        Smooth(g);
        using (var brush = new LinearGradientBrush(new Rectangle(0, 0, width, height + 1), AppTop, AppBottom, 90f))
            g.FillRectangle(brush, 0, 0, width, height);
        using (var glow = new GraphicsPath())
        {
            glow.AddEllipse(-width * 0.4f, -height * 0.15f, width * 1.8f, height * 0.9f);
            using var pg = new PathGradientBrush(glow) { CenterColor = Color.FromArgb(70, 255, 255, 255), SurroundColors = [Color.FromArgb(0, 255, 255, 255)] };
            g.FillPath(pg, glow);
        }
        var side = width * 0.62f;
        Monitor(g, new RectangleF((width - side) / 2, height * 0.3f - side / 2, side, side), (int)side);
        return bmp;
    }

    /// <summary>Ảnh nhỏ góc phải đầu bộ cài (Inno WizardSmallImageFile), nền trắng như phần đầu trang của bộ cài</summary>
    public static Bitmap WizardSmall(int size)
    {
        var bmp = new Bitmap(size, size, PixelFormat.Format24bppRgb);
        using var g = Graphics.FromImage(bmp);
        g.Clear(Color.White);
        using var icon = App(size);
        g.DrawImage(icon, 0, 0, size, size);
        return bmp;
    }

    /// <summary>Đổi ảnh sang Icon rồi huỷ HICON gốc: Icon.FromHandle không tự giải phóng</summary>
    public static Icon ToIcon(Bitmap bmp)
    {
        var handle = bmp.GetHicon();
        try { using var tmp = Icon.FromHandle(handle); return (Icon)tmp.Clone(); }
        finally { Native.DestroyIcon(handle); }
    }

    public static GraphicsPath Rounded(RectangleF r, float radius)
    {
        var path = new GraphicsPath();
        var d = Math.Min(radius * 2, Math.Min(r.Width, r.Height));
        if (d <= 0.5f) { path.AddRectangle(r); return path; }
        path.AddArc(r.X, r.Y, d, d, 180, 90);
        path.AddArc(r.Right - d, r.Y, d, d, 270, 90);
        path.AddArc(r.Right - d, r.Bottom - d, d, d, 0, 90);
        path.AddArc(r.X, r.Bottom - d, d, d, 90, 90);
        path.CloseFigure();
        return path;
    }

    public static void Smooth(Graphics g)
    {
        g.SmoothingMode = SmoothingMode.AntiAlias;
        g.PixelOffsetMode = PixelOffsetMode.HighQuality;
        g.CompositingQuality = CompositingQuality.HighQuality;
        g.InterpolationMode = InterpolationMode.HighQualityBicubic;
    }

    /// <summary>Nét đậm hơn ở cỡ nhỏ để 16 px vẫn rõ (tính trên lưới 16)</summary>
    static float Stroke(float px) => px <= 16 ? 2.25f : px <= 20 ? 2.1f : px <= 24 ? 2f : px <= 32 ? 1.85f : 1.7f;

    static GraphicsState Grid(Graphics g, RectangleF r)
    {
        var state = g.Save();
        var px = Math.Min(r.Width, r.Height);
        g.TranslateTransform(r.X + (r.Width - px) / 2, r.Y + (r.Height - px) / 2);
        g.ScaleTransform(px / 16f, px / 16f);
        return state;
    }

    static void Disc(Graphics g, Color c)
    {
        using var b = new SolidBrush(c);
        g.FillEllipse(b, 0.5f, 0.5f, 15f, 15f);
    }

    static void Triangle(Graphics g, Color c) =>
        Polygon(g, c, 1.4f, new(8f, 1.7f), new(15.1f, 14.1f), new(0.9f, 14.1f));

    static void Octagon(Graphics g, Color c)
    {
        const float lo = 0.7f, hi = 15.3f, cut = (hi - lo) / (2f + 1.41421356f);
        Polygon(g, c, 0.9f, new(lo + cut, lo), new(hi - cut, lo), new(hi, lo + cut), new(hi, hi - cut),
            new(hi - cut, hi), new(lo + cut, hi), new(lo, hi - cut), new(lo, lo + cut));
    }

    /// <summary>Tô hình rồi viền cùng màu với góc tròn, để các góc nhọn mềm đi</summary>
    static void Polygon(Graphics g, Color c, float round, params PointF[] points)
    {
        using var b = new SolidBrush(c);
        using var p = new Pen(c, round) { LineJoin = LineJoin.Round };
        g.FillPolygon(b, points);
        g.DrawPolygon(p, points);
    }

    static void Exclaim(Graphics g, Color c, float w, float top, float bottom, float dotY)
    {
        Line(g, c, w, new(8f, top), new(8f, bottom));
        var r = w * 0.62f;
        using var b = new SolidBrush(c);
        g.FillEllipse(b, 8f - r, dotY - r, 2 * r, 2 * r);
    }

    static void Check(Graphics g, Color c, float w, PointF a, PointF b, PointF d)
    {
        using var p = new Pen(c, w) { StartCap = LineCap.Round, EndCap = LineCap.Round, LineJoin = LineJoin.Round };
        g.DrawLines(p, new[] { a, b, d });
    }

    static void Dots(Graphics g, Color c, float r)
    {
        using var b = new SolidBrush(c);
        foreach (var x in new[] { 4.4f, 8f, 11.6f }) g.FillEllipse(b, x - r, 8f - r, 2 * r, 2 * r);
    }

    static void Line(Graphics g, Color c, float w, PointF a, PointF b)
    {
        using var p = new Pen(c, w) { StartCap = LineCap.Round, EndCap = LineCap.Round };
        g.DrawLine(p, a, b);
    }

    /// <summary>Màn hình máy tính có nhịp tim. Cỡ ≤ 20 px: màn hình tô đặc cho rõ, nhịp tim khoét bằng màu nền.</summary>
    static void Monitor(Graphics g, RectangleF box, int px)
    {
        float X(float f) => box.X + box.Width * f;
        float Y(float f) => box.Y + box.Height * f;
        var u = box.Width;
        var pulse = new[] { new PointF(X(.27f), Y(.445f)), new(X(.40f), Y(.445f)), new(X(.455f), Y(.33f)), new(X(.53f), Y(.56f)), new(X(.585f), Y(.41f)), new(X(.625f), Y(.445f)), new(X(.73f), Y(.445f)) };
        using var white = new SolidBrush(Color.White);
        if (px <= 24)
        {
            using (var screen = Rounded(new RectangleF(X(.15f), Y(.18f), u * .70f, u * .48f), u * .08f)) g.FillPath(white, screen);
            g.FillRectangle(white, X(.43f), Y(.65f), u * .14f, u * .11f);
            using (var foot = Rounded(new RectangleF(X(.29f), Y(.74f), u * .42f, u * .09f), u * .045f)) g.FillPath(white, foot);
            var small = new[] { new PointF(X(.25f), Y(.44f)), new(X(.41f), Y(.44f)), new(X(.50f), Y(.29f)), new(X(.59f), Y(.55f)), new(X(.75f), Y(.55f)) };
            using var cut = new Pen(AppBottom, Math.Max(1.3f, u * .095f)) { StartCap = LineCap.Round, EndCap = LineCap.Round, LineJoin = LineJoin.Round };
            g.DrawLines(cut, small);
            return;
        }
        var sw = Math.Max(1.5f, u * .065f);
        using var pen = new Pen(Color.White, sw) { StartCap = LineCap.Round, EndCap = LineCap.Round, LineJoin = LineJoin.Round };
        using (var screen = Rounded(new RectangleF(X(.17f), Y(.22f), u * .66f, u * .41f), u * .07f)) g.DrawPath(pen, screen);
        g.DrawLine(pen, X(.5f), Y(.63f) + sw / 2, X(.5f), Y(.735f));
        g.DrawLine(pen, X(.36f), Y(.765f), X(.64f), Y(.765f));
        using var beat = new Pen(Color.White, Math.Max(1.2f, u * .055f)) { StartCap = LineCap.Round, EndCap = LineCap.Round, LineJoin = LineJoin.Round };
        g.DrawLines(beat, pulse);
    }

    static RectangleF Inflate(RectangleF r, float d) => new(r.X - d, r.Y - d, r.Width + 2 * d, r.Height + 2 * d);
}
