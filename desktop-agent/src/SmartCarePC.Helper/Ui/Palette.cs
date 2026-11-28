using SmartCarePC.Helper.Core.Display;
using SmartCarePC.Helper.Core.Health;

namespace SmartCarePC.Helper.Ui;

/// <summary>
/// Bảng màu. Bản sáng lấy đúng màu của web (src/index.css) để app và web trông cùng một nhà.
/// Web chưa có chế độ tối, nên bản tối do app tự đặt theo cùng tinh thần. Chế độ tương phản cao dùng màu hệ thống.
/// </summary>
public sealed record Palette(
    bool Dark, bool HighContrast,
    Color Canvas, Color Surface, Color Fill, Color Line,
    Color Ink, Color Ink2, Color Ink3,
    Color Accent, Color AccentHover, Color AccentPressed, Color OnAccent, Color AccentSoft,
    Color Ok, Color OkBg, Color Info, Color InfoBg, Color Warn, Color WarnBg, Color Bad, Color BadBg)
{
    public static readonly Palette Light = new(false, false,
        Canvas: Hex(0xf5f5f7), Surface: Hex(0xffffff), Fill: Hex(0xe8e8ed), Line: Hex(0xe6e6ea),
        Ink: Hex(0x1d1d1f), Ink2: Hex(0x6e6e73), Ink3: Hex(0x86868b),
        Accent: Hex(0x0071e3), AccentHover: Hex(0x0077ed), AccentPressed: Hex(0x0062c4), OnAccent: Color.White, AccentSoft: Hex(0xe8f1fc),
        Ok: Hex(0x1d7a3a), OkBg: Hex(0xeaf7ee), Info: Hex(0x0b4f9c), InfoBg: Hex(0xeef5fe),
        Warn: Hex(0x7a4b00), WarnBg: Hex(0xfff4e5), Bad: Hex(0xb42318), BadBg: Hex(0xfdecec));

    public static readonly Palette Night = new(true, false,
        Canvas: Hex(0x1c1c1e), Surface: Hex(0x2c2c2e), Fill: Hex(0x3a3a3c), Line: Hex(0x3d3d40),
        Ink: Hex(0xf5f5f7), Ink2: Hex(0xaeaeb2), Ink3: Hex(0x8e8e93),
        Accent: Hex(0x0a84ff), AccentHover: Hex(0x409cff), AccentPressed: Hex(0x0071e3), OnAccent: Color.White, AccentSoft: Hex(0x253f5c),
        Ok: Hex(0x4cd47a), OkBg: Hex(0x1c3325), Info: Hex(0x6cb6ff), InfoBg: Hex(0x1b2d44),
        Warn: Hex(0xffc35c), WarnBg: Hex(0x3d2e14), Bad: Hex(0xff8a80), BadBg: Hex(0x45201d));

    /// <summary>Chế độ tương phản cao của Windows: chỉ dùng màu hệ thống, không tô nền màu</summary>
    public static Palette FromSystem()
    {
        Color w = SystemColors.Window, t = SystemColors.WindowText, h = SystemColors.Highlight, ht = SystemColors.HighlightText;
        return new Palette(false, true, w, w, w, t, t, t, SystemColors.GrayText, h, h, h, ht, w, t, w, t, w, t, w, t, w);
    }

    /// <summary>Màu chữ và nền của thẻ kết luận tổng</summary>
    public (Color Fg, Color Bg) Level(OverallLevel level) => level switch
    {
        OverallLevel.RunningWell => (Info, InfoBg),
        OverallLevel.NeedsAttention => (Warn, WarnBg),
        OverallLevel.NeedsHelpNow => (Bad, BadBg),
        _ => (Ink, Surface),
    };

    /// <summary>Màu chữ và nền của nhãn trạng thái từng mục kiểm tra</summary>
    public (Color Fg, Color Bg) Tone(CheckTone tone) => tone switch
    {
        CheckTone.Ok => (Ok, OkBg),
        CheckTone.Tip => (Info, InfoBg),
        CheckTone.Attention => (Warn, WarnBg),
        CheckTone.Urgent => (Bad, BadBg),
        _ => (Ink2, Fill),
    };

    static Color Hex(int rgb) => Color.FromArgb(255, (rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
}
