using SmartCarePC.Helper.Core.Health;

namespace SmartCarePC.Helper.Core;

public enum ToastSource { Schedule, Health, Setup }

/// <summary>Một thông báo cần hiện. Key dùng để biết bấm vào thông báo nào: "schedule:dust", "health:cpu_hot", "setup".</summary>
public sealed record ToastRequest(
    string Key,
    ToastSource Source,
    Urgency Urgency,
    MaintenanceTask? Task,
    string? Rule,
    string Title,
    string Body,
    string? OpenUrl,
    bool CanSnooze,
    int? OverdueDays,
    IReadOnlyDictionary<string, object> Data);

/// <summary>
/// Quyết định hiện thông báo nào. Hàm thuần trên AgentState, không đọc đồng hồ hay cảm biến, để test được.
/// Luật (spec 5.3):
/// - Tối đa 1 thông báo bảo dưỡng mỗi ngày. Ổ cứng sắp hỏng và quạt đứng được báo ngay, kể cả giờ yên lặng hay đang tạm dừng.
/// - Không báo trong giờ yên lặng, khi đang tạm dừng, khi đang chơi game toàn màn hình / trình chiếu.
/// - Remind me in 1 week: im đúng 7 ngày. Với nhắc theo lịch thì đổi ngày hẹn thành hôm nay + 7, giống snooze của web.
/// - Đã hiện mà người dùng không bấm gì: nhắc lại sau 3 ngày. Đã bấm Start guide: im 7 ngày (chờ web gửi lịch mới).
/// </summary>
public static class Planner
{
    public const int RepeatAfterDays = 3;
    public const int SnoozeDays = 7;
    public static readonly TimeSpan UrgentRepeat = TimeSpan.FromHours(24);
    public static readonly TimeSpan LightRepeat = TimeSpan.FromDays(7);

    public static bool InQuietHours(DateTime local, AgentSettings s)
    {
        int h = local.Hour, a = s.QuietStartHour, b = s.QuietEndHour;
        if (a == b) return false;
        return a < b ? h >= a && h < b : h >= a || h < b;
    }

    public static List<ToastRequest> Decide(AgentState st, DateTimeOffset now, IEnumerable<Finding> findings, bool userBusy)
    {
        var today = LocalDay.Of(now);
        var shown = new List<ToastRequest>();
        // Đã báo cho đúng các sự kiện này rồi: chỉ hiện trên Status, không thông báo lại
        var list = findings.Where(f => !f.AlreadyShown).ToList();
        var origin = st.Plan?.Origin ?? PlanLink.DefaultOrigin;

        bool RuleQuiet(string rule) => st.RuleQuietUntil.TryGetValue(rule, out var until) && until > now;

        foreach (var f in list.Where(f => f.Urgency == Urgency.Urgent && !RuleQuiet(f.Rule)))
        {
            shown.Add(FromFinding(f, st, origin));
            st.RuleQuietUntil[f.Rule] = now + UrgentRepeat;
        }

        var paused = st.PausedUntil is { } p && p > now;
        if (paused || userBusy || InQuietHours(now.DateTime, st.Settings)) return shown;

        if (st.LastMaintenanceToastDay != today)
        {
            ToastRequest? pick = null;
            var health = list.Where(f => f.Urgency == Urgency.Maintenance && !RuleQuiet(f.Rule))
                .OrderBy(f => f.Rule switch
                {
                    Rules.DiskErrorsRule => 0, // có thể mất dữ liệu
                    Rules.BlueScreenRule => 1,
                    Rules.ShutdownRule => 2,
                    Rules.HardwareErrorsRule => 3,
                    Rules.CpuHotRule => 4,
                    Rules.GpuHotRule => 5,
                    _ => 6,
                })
                .FirstOrDefault();
            if (health is not null)
            {
                pick = FromFinding(health, st, origin);
                st.RuleQuietUntil[health.Rule] = now + TimeSpan.FromDays(RepeatAfterDays);
            }
            else if (st.Plan is null)
            {
                // Không có lịch là bình thường: nhắc bảo dưỡng theo lịch giờ do email của web lo, app chỉ kiểm tra sức khoẻ.
                // Chỉ chào một lần để người dùng biết app đang chạy, không nhắc lại.
                if (st.SetupPromptLastDay is null)
                {
                    pick = WelcomeToast();
                    st.SetupPromptLastDay = today;
                }
            }
            else if (DueSchedule(st, today).Select(d => ((MaintenanceTask Task, int OverdueDays)?)d).FirstOrDefault() is { } due)
            {
                pick = ScheduleToast(st.Plan, due.Task, today);
                st.ScheduleLastShown[due.Task.Id()] = today;
            }
            if (pick is not null) { shown.Add(pick); st.LastMaintenanceToastDay = today; }
        }

        if (st.LastLightToastDay != today && list.FirstOrDefault(f => f.Urgency == Urgency.Light && !RuleQuiet(f.Rule)) is { } light)
        {
            shown.Add(FromFinding(light, st, origin));
            st.RuleQuietUntil[light.Rule] = now + LightRepeat;
            st.LastLightToastDay = today;
        }
        return shown;
    }

    /// <summary>Việc đã tới hạn theo lịch (ngày hẹn &lt;= hôm nay; máy tắt đúng ngày hẹn thì lần bật sau vẫn tính), quá hạn lâu nhất trước</summary>
    public static IEnumerable<(MaintenanceTask Task, int OverdueDays)> DueSchedule(AgentState st, string today)
    {
        if (st.Plan is null) yield break;
        var due = new List<(MaintenanceTask, int)>();
        foreach (var t in Tasks.All)
        {
            if (!st.Plan.Dates.TryGetValue(t, out var date) || string.CompareOrdinal(date, today) > 0) continue;
            if (st.ScheduleQuietUntil.TryGetValue(t.Id(), out var q) && string.CompareOrdinal(q, today) > 0) continue;
            if (st.ScheduleLastShown.TryGetValue(t.Id(), out var last) && LocalDay.DaysBetween(last, today) < RepeatAfterDays) continue;
            due.Add((t, LocalDay.DaysBetween(date, today)));
        }
        foreach (var d in due.OrderByDescending(d => d.Item2).ThenBy(d => (int)d.Item1)) yield return d;
    }

    public static ToastRequest ScheduleToast(MaintenancePlan plan, MaintenanceTask task, string today)
    {
        plan.Dates.TryGetValue(task, out var date);
        var overdue = date is null ? 0 : Math.Max(0, LocalDay.DaysBetween(date, today));
        plan.Completed.TryGetValue(task, out var done);
        var months = done is null ? 0 : LocalDay.WholeMonthsBetween(done, today);
        string Since(string what) => months >= 1 ? $"It has been {months} month{(months == 1 ? "" : "s")} since {what}." : "";
        var (title, body) = task switch
        {
            MaintenanceTask.Dust => ("Time to clean your PC",
                Join(Since("the last clean"), done is null ? "Your plan says it is time to clean out the dust." : "", "Cleaning takes about 30 minutes.")),
            MaintenanceTask.Paste => ("Time to replace the thermal paste",
                Join(Since("the paste was last replaced"), done is null ? "Your plan says it is time to refresh the paste on the processor." : "", "It takes about an hour.")),
            _ => ("Time for a quick PC check",
                "A 5-minute check each month helps you notice new noise, dust or problems early. You don't need to open the case."),
        };
        return new ToastRequest($"schedule:{task.Id()}", ToastSource.Schedule, Urgency.Maintenance, task, null, title, body,
            GuideUrl(plan.Origin, task, plan.Study), CanSnooze: true, overdue, new Dictionary<string, object> { ["overdueDays"] = overdue });
    }

    static string Join(params string[] parts) => string.Join(" ", parts.Where(p => p.Length > 0));

    /// <summary>Lần chạy đầu khi không có lịch: báo app đang kiểm tra máy. Không có nút, không mở web.</summary>
    public static ToastRequest WelcomeToast() =>
        new("setup", ToastSource.Setup, Urgency.Maintenance, null, null,
            "SmartCarePC Helper is checking your PC",
            "It watches your PC's health quietly and only tells you when something needs a look. To see the checks, right-click its icon near the clock and choose Status.",
            null, CanSnooze: false, null, new Dictionary<string, object>());

    static ToastRequest FromFinding(Finding f, AgentState st, string origin)
    {
        var url = f.GuideTask is { } t ? GuideUrl(origin, t, st.Plan?.Study)
            : f.Path is { } path ? WithStudy(origin + path, st.Plan?.Study)
            : null;
        return new ToastRequest($"health:{f.Rule}", ToastSource.Health, f.Urgency, f.GuideTask, f.Rule, f.Title, f.Body, url,
            CanSnooze: f.Urgency != Urgency.Urgent, null, f.Data);
    }

    /// <summary>Giống calendarLink trên web: ?baotri=task, thêm mã thử nghiệm nếu có</summary>
    public static string GuideUrl(string origin, MaintenanceTask task, StudyInfo? study) =>
        WithStudy($"{origin}/?baotri={task.Id()}", study);

    static string WithStudy(string url, StudyInfo? study)
    {
        if (study is null) return url;
        var sep = url.Contains('?') ? '&' : '?';
        return $"{url}{sep}thunghiem={Uri.EscapeDataString(study.Code)}{(study.Phase == "after" ? "&lan=sau" : "")}";
    }

    /// <summary>Remind me in 1 week</summary>
    public static void Snooze(AgentState st, string key, DateTimeOffset now)
    {
        var today = LocalDay.Of(now);
        if (key.StartsWith("schedule:") && Tasks.TryParse(key[9..], out var t) && st.Plan is not null)
        {
            st.Plan.Dates[t] = LocalDay.AddDays(today, SnoozeDays);
            st.ScheduleLastShown.Remove(t.Id());
        }
        else if (key.StartsWith("health:")) st.RuleQuietUntil[key[7..]] = now + TimeSpan.FromDays(SnoozeDays);
    }

    /// <summary>Start guide: đang làm thì không nhắc lại 7 ngày</summary>
    public static void Started(AgentState st, string key, DateTimeOffset now)
    {
        var today = LocalDay.Of(now);
        if (key.StartsWith("schedule:") && Tasks.TryParse(key[9..], out var t)) st.ScheduleQuietUntil[t.Id()] = LocalDay.AddDays(today, SnoozeDays);
        else if (key.StartsWith("health:")) st.RuleQuietUntil[key[7..]] = now + TimeSpan.FromDays(SnoozeDays);
    }

    /// <summary>Nhận lịch mới từ web. Trả true nếu là lần nối đầu tiên.</summary>
    public static bool ApplyPlan(AgentState st, MaintenancePlan plan, DateTimeOffset now)
    {
        var old = st.Plan;
        foreach (var t in Tasks.All)
        {
            string? before = null, after = null;
            old?.Dates.TryGetValue(t, out before);
            plan.Dates.TryGetValue(t, out after);
            if (before != after) { st.ScheduleLastShown.Remove(t.Id()); st.ScheduleQuietUntil.Remove(t.Id()); }
        }
        string? oldClean = null;
        old?.Completed.TryGetValue(MaintenanceTask.Dust, out oldClean);
        if (plan.Completed.TryGetValue(MaintenanceTask.Dust, out var clean) && clean != oldClean)
        {
            // Vừa vệ sinh: lấy số đo 14 ngày trước làm mốc, và các luật nóng bắt đầu đếm lại
            st.Health.Baseline = Baselines.Before(st.Health.LoadDays, clean);
            st.RuleQuietUntil.Remove(Rules.CpuHotRule);
            st.Health.CpuHotDays.RemoveAll(d => string.CompareOrdinal(d, clean) <= 0);
            st.Health.GpuHotDays.RemoveAll(d => string.CompareOrdinal(d, clean) <= 0);
        }
        st.Plan = plan;
        var first = st.LinkedAt is null;
        st.LinkedAt ??= now;
        return first;
    }
}
