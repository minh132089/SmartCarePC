namespace SmartCarePC.Helper.Ui;

/// <summary>Xếp các thẻ từ trên xuống, rộng bằng cửa sổ, cuộn được khi dài hơn cửa sổ</summary>
public sealed class ScrollStack : Panel
{
    bool arranging, dark;

    public ScrollStack()
    {
        AutoScroll = true;
        DoubleBuffered = true;
    }

    /// <summary>Thanh cuộn tối khi app ở chế độ tối</summary>
    public bool Dark
    {
        get => dark;
        set { dark = value; WindowChrome.Scrollbars(this, value); }
    }

    protected override void OnHandleCreated(EventArgs e)
    {
        base.OnHandleCreated(e);
        WindowChrome.Scrollbars(this, dark);
    }

    protected override void OnLayout(LayoutEventArgs e)
    {
        if (arranging) return;
        arranging = true;
        try
        {
            base.OnLayout(e);
            // Thanh cuộn vừa hiện hay ẩn làm đổi bề rộng: xếp lại thêm một lần với bề rộng mới
            for (var pass = 0; pass < 2; pass++)
            {
                var width = ClientSize.Width;
                Arrange();
                AdjustFormScrollbars(true);
                if (ClientSize.Width == width) break;
            }
        }
        finally { arranging = false; }
    }

    void Arrange()
    {
        int pad = LogicalToDeviceUnits(16), gap = LogicalToDeviceUnits(14);
        var width = ClientSize.Width - 2 * pad;
        if (width <= 0) return;
        var y = pad;
        var scroll = AutoScrollPosition.Y;
        foreach (Control c in Controls)
        {
            if (!c.Visible) continue;
            var h = c.GetPreferredSize(new Size(width, 0)).Height;
            c.SetBounds(pad, y + scroll, width, h);
            y += h + gap;
        }
        var total = y - gap + pad;
        if (AutoScrollMinSize.Height != total) AutoScrollMinSize = new Size(0, total);
    }
}
