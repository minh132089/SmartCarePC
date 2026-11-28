using SmartCarePC.Helper.Core.Display;

namespace SmartCarePC.Helper.Ui;

/// <summary>
/// Một nhóm trên cửa sổ Status: tiêu đề, rồi thẻ trắng có từng dòng "nhãn bên trái, giá trị bên phải".
/// Mục kiểm tra có nhãn trạng thái gồm hình và chữ. Giá trị không đọc được là dấu gạch xám, giải thích một lần ở chú thích cuối thẻ.
/// Có thể gắn thêm một control ở đáy thẻ (ô giờ yên lặng).
/// </summary>
public sealed class SectionCard : Control
{
    sealed record RowBox(StatusRow Row, Rectangle Label, Rectangle Value, int LineY);
    sealed record NoteBox(string Text, Rectangle Box, bool Body);
    sealed record Arrangement(int Width, Rectangle Title, Rectangle Card, IReadOnlyList<RowBox> Rows, IReadOnlyList<NoteBox> Notes, int NotesLineY, Rectangle Footer, int Height);

    StatusSection? section;
    Palette palette = Palette.Light;
    Control? footer;
    Arrangement? cached;

    public SectionCard()
    {
        Draw.OwnerDrawn(this, SetStyle);
        AccessibleRole = AccessibleRole.Grouping;
    }

    public Palette Palette
    {
        get => palette;
        set
        {
            palette = value;
            if (footer is not null) footer.BackColor = value.Surface;
            Changed();
        }
    }

    public Control? Footer
    {
        get => footer;
        set
        {
            if (footer is not null) Controls.Remove(footer);
            footer = value;
            if (value is not null) { value.BackColor = palette.Surface; Controls.Add(value); }
            Changed();
        }
    }

    public StatusSection? Section
    {
        get => section;
        set
        {
            if (Same(section, value)) return;
            section = value;
            AccessibleName = value?.Title;
            Changed();
        }
    }

    static bool Same(StatusSection? a, StatusSection? b) =>
        a is null || b is null ? ReferenceEquals(a, b)
        : a.Key == b.Key && a.Title == b.Title && a.Rows.SequenceEqual(b.Rows) && a.Notes.SequenceEqual(b.Notes);

    void Changed()
    {
        cached = null;
        Parent?.PerformLayout(this, "Content");
        PerformLayout();
        Invalidate();
    }

    int Px(int logical) => LogicalToDeviceUnits(logical);
    Font TitleFont => Theme.Font(10.5f, DeviceDpi, Weight.Semibold);
    Font BodyFont => Theme.Font(10f, DeviceDpi);
    Font NoteFont => Theme.Font(9f, DeviceDpi);
    Font ChipFont => Theme.Font(9f, DeviceDpi, Weight.Semibold);

    public override Size GetPreferredSize(Size proposedSize)
    {
        var width = proposedSize.Width > 0 ? proposedSize.Width : Width;
        return new Size(width, section is null ? 0 : Arrange(width).Height);
    }

    Arrangement Arrange(int width)
    {
        if (cached is { } c && c.Width == width) return c;
        var s = section!;
        int pad = Px(16), vpad = Px(9), gap = Px(16), inner = width - 2 * pad;
        var title = new Rectangle(Px(4), 0, width - Px(8), TitleFont.Height);
        var cardTop = title.Bottom + Px(8);
        var y = cardTop;

        var rows = new List<RowBox>();
        foreach (var r in s.Rows)
        {
            var value = ValueSize(r, inner);
            var labelWidth = Math.Max(Px(80), inner - value.Width - gap);
            var labelHeight = Draw.TextHeight(r.Label, BodyFont, labelWidth);
            var h = Math.Max(labelHeight, value.Height) + 2 * vpad;
            rows.Add(new RowBox(r,
                new Rectangle(pad, y + (h - labelHeight) / 2, labelWidth, labelHeight),
                new Rectangle(width - pad - value.Width, y + (h - value.Height) / 2, value.Width, value.Height),
                rows.Count == 0 ? -1 : y));
            y += h;
        }

        // Không có dòng nào thì chú thích là nội dung chính: chữ to và đậm màu hơn
        var notes = new List<NoteBox>();
        var body = rows.Count == 0;
        var notesLine = rows.Count > 0 && s.Notes.Count > 0 ? y : -1;
        if (s.Notes.Count > 0)
        {
            y += body ? Px(14) : Px(10);
            foreach (var n in s.Notes)
            {
                var h = Draw.TextHeight(n, body ? BodyFont : NoteFont, inner);
                notes.Add(new NoteBox(n, new Rectangle(pad, y, inner, h), body));
                y += h + Px(6);
            }
            y += Px(body ? 8 : 6);
        }

        // Không dùng footer.Visible: thuộc tính đó trả về false khi thẻ cha còn ẩn, làm thẻ tính thiếu chiều cao
        var footerBox = Rectangle.Empty;
        if (footer is not null)
        {
            var h = footer.GetPreferredSize(new Size(inner, 0)).Height;
            footerBox = new Rectangle(pad, y + Px(14), inner, h);
            y = footerBox.Bottom + Px(14);
        }
        if (rows.Count == 0 && notes.Count == 0 && footer is null) y += Px(16);
        cached = new Arrangement(width, title, new Rectangle(0, cardTop, width, y - cardTop + 1), rows, notes, notesLine, footerBox, y + 3);
        return cached;
    }

    Size ValueSize(StatusRow r, int inner)
    {
        if (r.Tone is not null)
        {
            var chip = Draw.TextSize(r.Value ?? "", ChipFont);
            return new Size(Px(8) + Px(12) + Px(5) + chip.Width + Px(10), ChipFont.Height + Px(6));
        }
        // Giá trị dài (ví dụ nhiều ổ đĩa khác trạng thái) xuống dòng chứ không cắt, để không giấu mất ổ đang hỏng
        var text = r.Value ?? "—";
        var size = Draw.TextSize(text, BodyFont);
        var max = Math.Max(Px(40), inner * 55 / 100);
        return size.Width <= max ? size : new Size(max, Draw.TextHeight(text, BodyFont, max));
    }

    protected override void OnLayout(LayoutEventArgs e)
    {
        base.OnLayout(e);
        if (footer is null || section is null || Width <= 0) return;
        var box = Arrange(Width).Footer;
        if (footer.Bounds != box) footer.Bounds = box;
    }

    protected override void OnResize(EventArgs e)
    {
        cached = null;
        base.OnResize(e);
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        var g = e.Graphics;
        g.Clear(Parent?.BackColor ?? palette.Canvas);
        if (section is null) return;
        var a = Arrange(Width);
        int pad = Px(16);
        Draw.Text(g, section.Title, TitleFont, a.Title, palette.Ink, Draw.Single);
        Draw.Card(g, a.Card, Px(8), palette.Surface, palette.Line, !palette.Dark && !palette.HighContrast);

        foreach (var row in a.Rows)
        {
            if (row.LineY >= 0) Draw.HLine(g, palette.Line, pad, Width - pad, row.LineY);
            Draw.Text(g, row.Row.Label, BodyFont, row.Label, palette.Ink);
            if (row.Row.Tone is { } tone) Chip(g, tone, row.Row.Value ?? "", row.Value);
            else Draw.Text(g, row.Row.Value ?? "—", BodyFont, row.Value, row.Row.IsMissing ? palette.Ink3 : palette.Ink, Draw.Wrap | TextFormatFlags.Right);
        }
        if (a.NotesLineY >= 0) Draw.HLine(g, palette.Line, pad, Width - pad, a.NotesLineY);
        foreach (var n in a.Notes)
            Draw.Text(g, n.Text, n.Body ? BodyFont : NoteFont, n.Box, n.Body ? palette.Ink : palette.Ink2);
        if (!a.Footer.IsEmpty && (a.Rows.Count > 0 || a.Notes.Count > 0))
            Draw.HLine(g, palette.Line, pad, Width - pad, a.Footer.Top - Px(14));
    }

    /// <summary>Nhãn trạng thái: nền nhạt bo tròn, hình nhỏ, chữ. Tương phản cao: chỉ viền, không tô.</summary>
    void Chip(Graphics g, CheckTone tone, string text, Rectangle r)
    {
        var (fg, bg) = palette.Tone(tone);
        if (palette.HighContrast)
        {
            using var path = IconArt.Rounded(new RectangleF(r.X + .5f, r.Y + .5f, r.Width - 1, r.Height - 1), r.Height / 2f);
            using var pen = new Pen(palette.Ink);
            IconArt.Smooth(g);
            g.DrawPath(pen, path);
        }
        else Draw.Fill(g, r, r.Height / 2f, bg);
        var glyph = Px(12);
        IconArt.DrawTone(g, tone, new RectangleF(r.X + Px(8), r.Y + (r.Height - glyph) / 2f, glyph, glyph), fg, palette.HighContrast ? palette.Surface : bg);
        var left = r.X + Px(8) + glyph + Px(5);
        Draw.Text(g, text, ChipFont, new Rectangle(left, r.Y, r.Right - left, r.Height), fg, Draw.Single | TextFormatFlags.VerticalCenter);
    }

    protected override void OnDpiChangedAfterParent(EventArgs e)
    {
        base.OnDpiChangedAfterParent(e);
        Changed();
    }

    /// <summary>Trình đọc màn hình đọc từng dòng "nhãn: giá trị" và từng chú thích</summary>
    protected override AccessibleObject CreateAccessibilityInstance() => new CardAccessible(this);

    Arrangement? accessSource;
    IReadOnlyList<Item> accessItems = [];

    /// <summary>Danh sách dòng cho trình đọc màn hình. Giữ nguyên đối tượng tới khi nội dung đổi, để trình đọc không đọc lặp hay bỏ sót.</summary>
    IReadOnlyList<Item> Items()
    {
        if (section is null || Width <= 0) return [];
        var a = Arrange(Width);
        if (ReferenceEquals(a, accessSource)) return accessItems;
        var parent = AccessibilityObject;
        var named = a.Rows.Select(r => ($"{r.Row.Label}: {r.Row.Value ?? "not available"}", Rectangle.Union(r.Label, r.Value)))
            .Concat(a.Notes.Select(n => (n.Text, n.Box)));
        accessItems = named.Select((x, i) => new Item(parent, this, i, x.Item1, x.Item2)).ToList();
        accessSource = a;
        return accessItems;
    }

    sealed class CardAccessible(SectionCard card) : ControlAccessibleObject(card)
    {
        public override int GetChildCount() => card.Items().Count + (card.footer is null ? 0 : 1);

        public override AccessibleObject? GetChild(int index)
        {
            var items = card.Items();
            if (index >= 0 && index < items.Count) return items[index];
            return index == items.Count ? card.footer?.AccessibilityObject : null;
        }
    }

    sealed class Item(AccessibleObject parent, Control owner, int index, string name, Rectangle box) : AccessibleObject
    {
        public override string? Name => name;
        public override AccessibleRole Role => AccessibleRole.StaticText;
        public override AccessibleStates State => AccessibleStates.ReadOnly;
        public override AccessibleObject Parent => parent;
        public override Rectangle Bounds => owner.RectangleToScreen(box);

        public override AccessibleObject? Navigate(AccessibleNavigation direction) => direction switch
        {
            AccessibleNavigation.Next or AccessibleNavigation.Down => parent.GetChild(index + 1),
            AccessibleNavigation.Previous or AccessibleNavigation.Up => index > 0 ? parent.GetChild(index - 1) : null,
            _ => null,
        };
    }
}
