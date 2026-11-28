namespace SmartCarePC.Helper.Core.Health;

/// <summary>
/// Kết luận tổng "máy chạy ổn không". Người dùng thấy 3 mức: Running well, Needs attention, Needs help now.
/// Checking chỉ dùng khi chưa có luật nào kiểm tra được (vừa mở app), để không nói "ổn" khi chưa biết gì.
/// </summary>
public enum OverallLevel { Checking, RunningWell, NeedsAttention, NeedsHelpNow }

/// <param name="Problems">Tiêu đề các luật đang báo (khẩn trước)</param>
/// <param name="Tips">Tiêu đề các gợi ý nhẹ (ổ đầy, RAM, khởi động lại...), không làm đổi mức</param>
/// <param name="NotChecked">Luật không đọc được trên máy này, ví dụ nhiệt CPU khi không có quyền admin. Không tính là ổn.</param>
public sealed record OverallStatus(OverallLevel Level, IReadOnlyList<string> Problems, IReadOnlyList<string> Tips, IReadOnlyList<string> NotChecked)
{
    public string Headline => Level switch
    {
        OverallLevel.RunningWell => "Running well",
        OverallLevel.NeedsAttention => "Needs attention",
        OverallLevel.NeedsHelpNow => "Needs help now",
        _ => "Checking your PC",
    };

    /// <summary>Chữ khi rê chuột lên icon khay. Windows cắt chữ dài, nên giữ dưới 63 ký tự.</summary>
    public string TrayText
    {
        get
        {
            var text = "SmartCarePC Helper: " + Headline;
            if (Level == OverallLevel.RunningWell && NotChecked.Count > 0) text += $", {NotChecked.Count} not checked";
            return text.Length <= 63 ? text : text[..63];
        }
    }

    /// <summary>Câu mở đầu dưới tiêu đề khi chưa thấy vấn đề. null khi đang có vấn đề: khi đó Problems nói thay.</summary>
    public string? Summary => Level switch
    {
        OverallLevel.Checking => NotChecked.Count == 0
            ? "The first checks are still running. This takes a minute or two."
            : "None of the checks could run on this PC yet.",
        OverallLevel.RunningWell => "No problems found in the checks this app could run.",
        _ => null,
    };

    /// <summary>Một câu chung cho mọi luật không đọc được. null khi luật nào cũng chạy được.</summary>
    public string? NotCheckedNote => NotChecked.Count == 0 ? null
        : $"Not checked on this PC: {string.Join(", ", NotChecked.Select(Rules.DisplayName))}. "
            + "This app cannot read these here, so they are not counted as OK.";

    /// <summary>Các dòng giải thích dưới tiêu đề trong cửa sổ Status</summary>
    public IReadOnlyList<string> Lines
    {
        get
        {
            var lines = new List<string>();
            if (Summary is { } summary) lines.Add(summary);
            else lines.AddRange(Problems.Select(p => "• " + p));
            lines.AddRange(Tips.Select(t => "Tip: " + t));
            if (NotCheckedNote is { } note) lines.Add(note);
            return lines;
        }
    }

    /// <summary>
    /// Gộp kết quả các luật. Có luật khẩn (ổ cứng sắp hỏng, quạt đứng) thì Needs help now. Có luật bảo dưỡng / Event Log
    /// thì Needs attention. Gợi ý nhẹ không đổi mức. Luật không đọc được ghi riêng, không tính là ổn.
    /// </summary>
    public static OverallStatus Of(IReadOnlyDictionary<string, RuleResult> results)
    {
        var alerts = results.Values.Where(r => r.Status == RuleStatus.Alert && r.Finding is not null).Select(r => r.Finding!).ToList();
        var notChecked = results.Where(kv => kv.Value.Status == RuleStatus.NotAvailable).Select(kv => kv.Key).ToList();
        var level = alerts.Any(f => f.Urgency == Urgency.Urgent) ? OverallLevel.NeedsHelpNow
            : alerts.Any(f => f.Urgency == Urgency.Maintenance) ? OverallLevel.NeedsAttention
            : results.Values.Any(r => r.Status != RuleStatus.NotAvailable) ? OverallLevel.RunningWell
            : OverallLevel.Checking;
        var problems = alerts.Where(f => f.Urgency != Urgency.Light).OrderBy(f => f.Urgency).Select(f => f.Title).Distinct().ToList();
        var tips = alerts.Where(f => f.Urgency == Urgency.Light).Select(f => f.Title).Distinct().ToList();
        return new OverallStatus(level, problems, tips, notChecked);
    }
}
