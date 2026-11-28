using System.Globalization;
using SmartCarePC.Helper.Core.Health;

namespace SmartCarePC.Helper.Core.Display;

/// <summary>Trạng thái một dòng trong mục Checks. Giao diện luôn hiện kèm hình và chữ, không chỉ dựa vào màu.</summary>
public enum CheckTone { Ok, Tip, Attention, Urgent, NotChecked }

/// <param name="Value">Chữ bên phải. null = không đọc được trên máy này: giao diện hiện dấu gạch xám, câu giải thích nằm ở Notes của mục.</param>
/// <param name="Tone">Chỉ có ở mục Checks</param>
public sealed record StatusRow(string Key, string Label, string? Value, CheckTone? Tone = null)
{
    public bool IsMissing => Value is null;
}

/// <param name="Notes">Câu chữ xám dưới các dòng, mỗi câu một ý</param>
public sealed record StatusSection(string Key, string Title, IReadOnlyList<StatusRow> Rows, IReadOnlyList<string> Notes);

/// <summary>Thẻ đầu cửa sổ: kết luận tổng, câu mở đầu hoặc danh sách vấn đề, gợi ý, và một câu chung cho các luật không đọc được</summary>
public sealed record StatusHero(OverallLevel Level, string Headline, string? Summary, IReadOnlyList<string> Problems, IReadOnlyList<string> Tips, string? NotCheckedNote);

/// <summary>
/// Nội dung cửa sổ Status, tách khỏi WinForms để test được trên mọi máy. Không có luật hay ngưỡng nào ở đây:
/// chỉ đổi số đo và kết quả luật thành chữ cho người không rành kỹ thuật.
/// </summary>
public sealed record StatusPage(StatusHero Hero, IReadOnlyList<StatusSection> Sections)
{
    public const string MissingNote = "A dash (—) means this app cannot read that on this PC.";
    const string NoPlanNote = "This app only checks your PC's health. To get cleaning reminders by email, set them up on the SmartCarePC website (Open SmartCarePC in the tray menu).";
    const string PrivateNote = "Your readings stay on this PC. This app sends nothing over the internet.";
    static readonly CultureInfo Inv = CultureInfo.InvariantCulture;

    public static string ToneText(CheckTone tone) => tone switch
    {
        CheckTone.Ok => "OK",
        CheckTone.Tip => "Tip",
        CheckTone.Attention => "Needs attention",
        CheckTone.Urgent => "Needs help now",
        _ => "Not checked",
    };

    public static StatusPage Build(StatusSnapshot? s, DateTimeOffset now)
    {
        if (s is null)
        {
            var empty = OverallStatus.Of(new Dictionary<string, RuleResult>());
            return new StatusPage(new StatusHero(empty.Level, empty.Headline, "Starting…", [], [], null), []);
        }
        var o = s.Overall;
        var hero = new StatusHero(o.Level, o.Headline, o.Summary, o.Problems, o.Tips, o.NotCheckedNote);
        return new StatusPage(hero, [Checks(s), Readings(s, now), Reminders(s, now), Privacy(s)]);
    }

    /// <summary>Mục cần để ý lên đầu (khẩn, cần chú ý, gợi ý, không kiểm tra được), mục ổn xuống cuối. Cùng mức thì giữ thứ tự cũ.</summary>
    static StatusSection Checks(StatusSnapshot s)
    {
        var rows = s.Rules.Select(kv => Row(kv.Key, Tone(kv.Value))).OrderBy(r => Rank(r.Tone!.Value)).ToList();
        return new StatusSection("checks", "Checks", rows, []);

        static StatusRow Row(string rule, CheckTone tone) => new(rule, Rules.DisplayName(rule), ToneText(tone), tone);
        static int Rank(CheckTone t) => t switch
        {
            CheckTone.Urgent => 0,
            CheckTone.Attention => 1,
            CheckTone.Tip => 2,
            CheckTone.NotChecked => 3,
            _ => 4,
        };
    }

    static CheckTone Tone(RuleResult r) => r.Status switch
    {
        RuleStatus.Ok => CheckTone.Ok,
        RuleStatus.NotAvailable => CheckTone.NotChecked,
        _ => r.Finding?.Urgency switch
        {
            Urgency.Urgent => CheckTone.Urgent,
            Urgency.Light => CheckTone.Tip,
            _ => CheckTone.Attention,
        },
    };

    static StatusSection Readings(StatusSnapshot s, DateTimeOffset now)
    {
        var l = s.Last;
        var rows = new List<StatusRow>
        {
            new("cpu_load", "Processor use", l is null ? null : Percent(l.CpuLoad)),
            new("cpu_temp", "Processor temperature", Temp(l?.CpuTempC)),
            new("cpu_fan", "Processor fan", l?.CpuFanRpm is { } f ? $"{Math.Round(f).ToString(Inv)} RPM" : null),
            new("gpu_temp", "Graphics card temperature", Temp(l?.GpuTempC)),
            new("ram", "Memory in use", l is null ? null : Percent(l.RamUsed)),
            new("drive_free", "Main drive free space", s.Drive is { Free: { } fr, Total: { } t } && t > 0 ? Percent((double)fr / t) : null),
            new("drive_health", "Drive health", s.Disks is { Count: > 0 } d ? Disks(d) : null),
            new("shutdowns", "Unexpected shutdowns this week", Count(s.ShutdownsThisWeek)),
            new("blue_screens", "Blue error screens this week", Count(s.BlueScreensThisWeek)),
            new("disk_errors", "Drive read and write errors this week", Count(s.DiskErrorsThisWeek)),
            new("hardware_errors", "Hardware errors this week", Count(s.HardwareErrorsThisWeek)),
            new("last_restart", "Last restart", s.LastBoot is { } boot ? LastRestart(boot, now) : null),
        };
        var notes = new List<string>();
        if (s.BaselineSentence is { } baseline) notes.Add(baseline);
        if (rows.Any(r => r.IsMissing)) notes.Add(MissingNote);
        return new StatusSection("readings", "Latest readings", rows, notes);
    }

    static StatusSection Reminders(StatusSnapshot s, DateTimeOffset now)
    {
        var rows = new List<StatusRow>();
        if (s.Plan is { } plan)
            foreach (var task in Tasks.All)
                if (plan.Dates.TryGetValue(task, out var date) && LocalDay.TryParse(date, out var day))
                    rows.Add(new StatusRow(task.Id(), TaskName(task), day.ToString("d MMM yyyy", Inv)));
        if (s.PausedUntil is { } p && p > now)
            rows.Add(new StatusRow("paused", "Reminders paused", "until " + p.ToString("d MMM, HH:mm", Inv)));
        return new StatusSection("reminders", "Reminders", rows, s.Plan is null ? [NoPlanNote] : []);
    }

    static StatusSection Privacy(StatusSnapshot s) => new("privacy", "Privacy", [],
        [s.Plan?.Study is null
            ? PrivateNote
            : $"You are taking part in the study. Only reminder and click events are sent, with your anonymous code. Waiting to send: {s.PendingEvents.ToString(Inv)}."]);

    static string Percent(double fraction) => Math.Round(fraction * 100).ToString(Inv) + "%";
    static string? Temp(double? c) => c is { } x ? $"{Math.Round(x).ToString(Inv)} °C" : null;
    static string? Count(int? n) => n?.ToString(Inv);

    /// <summary>"Healthy (2 drives)" khi mọi ổ cùng trạng thái, không thì liệt kê từng ổ</summary>
    static string Disks(IReadOnlyList<string> d) =>
        d.Distinct().Count() == 1 ? (d.Count > 1 ? $"{d[0]} ({d.Count.ToString(Inv)} drives)" : d[0]) : string.Join(", ", d);

    static string TaskName(MaintenanceTask t) => t switch
    {
        MaintenanceTask.Dust => "Clean out dust",
        MaintenanceTask.Paste => "Replace thermal paste",
        _ => "Quick health check",
    };

    static string LastRestart(DateTimeOffset boot, DateTimeOffset now)
    {
        var days = (int)Math.Floor((now - boot).TotalDays);
        return days switch { <= 0 => "today", 1 => "1 day ago", _ => $"{days.ToString(Inv)} days ago" };
    }
}
