using SmartCarePC.Helper.Core.Display;
using SmartCarePC.Helper.Core.Health;

namespace SmartCarePC.Helper.Ui;

/// <summary>
/// Thẻ đầu cửa sổ Status: icon lớn giống icon khay, kết luận tổng, rồi câu mở đầu hoặc danh sách vấn đề, gợi ý,
/// và một câu xám chung cho những mục không kiểm tra được. Nền tô nhạt theo mức, nhưng chữ và hình mới là thứ nói lên mức.
/// </summary>
public sealed class HeroCard : Control
{
    StatusHero? hero;
    Palette palette = Palette.Light;

    public HeroCard()
    {
        Draw.OwnerDrawn(this, SetStyle);
        // Ô đầu tiên nhận focus khi mở cửa sổ: trình đọc màn hình đọc kết luận trước, và cửa sổ không tự cuộn xuống ô giờ yên lặng
        SetStyle(ControlStyles.Selectable, true);
        TabStop = true;
        TabIndex = 0;
        AccessibleRole = AccessibleRole.Grouping;
    }

    protected override void OnGotFocus(EventArgs e) { base.OnGotFocus(e); Invalidate(); }
    protected override void OnLostFocus(EventArgs e) { base.OnLostFocus(e); Invalidate(); }

    public Palette Palette
    {
        get => palette;
        set { palette = value; Invalidate(); }
    }

    public StatusHero? Hero
    {
        get => hero;
        set
        {
            if (Same(hero, value)) return;
            hero = value;
            AccessibleName = value?.Headline;
            AccessibleDescription = value is null ? null : string.Join(" ", Texts(value).Select(t => t.Text));
            Parent?.PerformLayout(this, nameof(Hero));
            Invalidate();
        }
    }

    static bool Same(StatusHero? a, StatusHero? b) =>
        a is null || b is null ? ReferenceEquals(a, b)
        : a.Level == b.Level && a.Headline == b.Headline && a.Summary == b.Summary && a.NotCheckedNote == b.NotCheckedNote
          && a.Problems.SequenceEqual(b.Problems) && a.Tips.SequenceEqual(b.Tips);

    enum Kind { Summary, Problem, Tip, Note }

    static IEnumerable<(Kind Kind, string Text)> Texts(StatusHero h)
    {
        if (h.Summary is { } s) yield return (Kind.Summary, s);
        foreach (var p in h.Problems) yield return (Kind.Problem, p);
        foreach (var t in h.Tips) yield return (Kind.Tip, "Tip: " + t);
        if (h.NotCheckedNote is { } n) yield return (Kind.Note, n);
    }

    int Px(int logical) => LogicalToDeviceUnits(logical);
    Font HeadFont => Theme.Font(17f, DeviceDpi, Weight.Semibold, display: true);
    Font BodyFont => Theme.Font(10.5f, DeviceDpi);
    Font NoteFont => Theme.Font(9.5f, DeviceDpi);
    int IconSize => Px(44);
    int TextLeft => Px(20) + IconSize + Px(16);
    int Bullet => Px(16);

    public override Size GetPreferredSize(Size proposedSize)
    {
        var width = proposedSize.Width > 0 ? proposedSize.Width : Width;
        return new Size(width, Measure(width, null));
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        var g = e.Graphics;
        g.Clear(Parent?.BackColor ?? palette.Canvas);
        if (hero is null) return;
        var (_, bg) = palette.Level(hero.Level);
        Draw.Card(g, new Rectangle(0, 0, Width, Height - 2), Px(10), bg, palette.Line, !palette.Dark && !palette.HighContrast);
        IconArt.DrawStatus(g, hero.Level, new RectangleF(Px(20), Px(20), IconSize, IconSize));
        Measure(Width, g);
        if (Focused && ShowFocusCues)
        {
            // Viền focus chỉ hiện khi dùng bàn phím
            using var path = IconArt.Rounded(new RectangleF(1.5f, 1.5f, Width - 3, Height - 5), Px(9));
            using var pen = new Pen(palette.HighContrast ? palette.Ink : palette.Accent, 2f);
            g.DrawPath(pen, path);
        }
    }

    /// <summary>Tính chiều cao, và vẽ luôn nếu có Graphics: cùng một chỗ tính toạ độ nên đo và vẽ không lệch nhau</summary>
    int Measure(int width, Graphics? g)
    {
        if (hero is null) return 0;
        var (fg, _) = palette.Level(hero.Level);
        int x = TextLeft, w = width - x - Px(20), y = Px(18);
        var head = Draw.TextHeight(hero.Headline, HeadFont, w);
        if (g is not null) Draw.Text(g, hero.Headline, HeadFont, new Rectangle(x, y, w, head), palette.HighContrast ? palette.Ink : fg);
        y += head + Px(4);

        foreach (var (kind, text) in Texts(hero))
        {
            var font = kind == Kind.Note ? NoteFont : BodyFont;
            var indent = kind is Kind.Problem or Kind.Tip ? Bullet : 0;
            if (kind == Kind.Note) y += Px(6);
            var h = Draw.TextHeight(text, font, w - indent);
            if (g is not null)
            {
                if (indent > 0) Marker(g, kind, fg, new Rectangle(x, y + (font.Height - Px(10)) / 2, Px(10), Px(10)));
                Draw.Text(g, text, font, new Rectangle(x + indent, y, w - indent, h), kind == Kind.Note ? palette.Ink2 : palette.Ink);
            }
            y += h + Px(4);
        }
        return Math.Max(y + Px(14), Px(20) + IconSize + Px(20)) + 2;
    }

    /// <summary>Chấm tròn trước vấn đề, chữ i trước gợi ý</summary>
    void Marker(Graphics g, Kind kind, Color levelColor, Rectangle r)
    {
        if (kind == Kind.Tip)
        {
            var (fg, _) = palette.Tone(CheckTone.Tip);
            IconArt.DrawTone(g, CheckTone.Tip, r, palette.HighContrast ? palette.Ink : fg, palette.HighContrast ? palette.Surface : palette.Level(hero!.Level).Bg);
            return;
        }
        var d = Px(6);
        Draw.Fill(g, new RectangleF(r.X + (r.Width - d) / 2f, r.Y + (r.Height - d) / 2f, d, d), d / 2f, palette.HighContrast ? palette.Ink : levelColor);
    }

    protected override void OnDpiChangedAfterParent(EventArgs e)
    {
        base.OnDpiChangedAfterParent(e);
        Parent?.PerformLayout(this, "Dpi");
        Invalidate();
    }
}
