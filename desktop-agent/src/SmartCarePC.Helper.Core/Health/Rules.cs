namespace SmartCarePC.Helper.Core.Health;

/// <summary>Urgent: báo ngay, không tính vào giới hạn 1 thông báo / ngày. Maintenance: việc bảo dưỡng. Light: gợi ý nhẹ, không phải bảo dưỡng phần cứng.</summary>
public enum Urgency { Urgent, Maintenance, Light }

public enum RuleStatus { Ok, Alert, NotAvailable }

/// <summary>Một lời nhắc theo triệu chứng. GuideTask là bài hướng dẫn cần mở (dust / paste / check), Path là trang khác (ví dụ /upgrade).</summary>
public sealed record Finding(string Rule, Urgency Urgency, MaintenanceTask? GuideTask, string? Path, string Title, string Body, IReadOnlyDictionary<string, object> Data);

public sealed record RuleResult(RuleStatus Status, Finding? Finding = null)
{
    public static readonly RuleResult Ok = new(RuleStatus.Ok);
    /// <summary>Cảm biến không đọc được trên máy này: bỏ qua luật, các luật khác vẫn chạy</summary>
    public static readonly RuleResult NotAvailable = new(RuleStatus.NotAvailable);
    public static RuleResult Alert(Finding f) => new(RuleStatus.Alert, f);
}

/// <summary>
/// Mỗi luật là một hàm thuần: nhận số đo, trả ổn / nhắc việc + câu lý do. Chữ hiện cho người dùng bằng tiếng Anh, lời đời thường,
/// không dùng từ kỹ thuật như "thermal throttling", "SMART", "Kernel-Power". Không bảo người dùng mở bộ nguồn.
/// </summary>
public static class Rules
{
    public const string CpuHotRule = "cpu_hot";
    public const string GpuHotRule = "gpu_hot";
    public const string FanStoppedRule = "fan_stopped";
    public const string DiskFailingRule = "disk_failing";
    public const string DriveLowRule = "drive_low";
    public const string ShutdownRule = "unexpected_shutdown";
    public const string RamFullRule = "ram_full";

    static int CountDaysInWindow(IEnumerable<string> days, string today, int window) =>
        days.Distinct().Count(d => LocalDay.TryParse(d, out _) && LocalDay.DaysBetween(d, today) is >= 0 and var n && n < window);

    static string Times(int n) => n == 2 ? "twice" : $"on {n} days";

    /// <summary>CPU nóng khi chạy nặng, ở ≥ 2 ngày khác nhau trong 7 ngày. Vừa vệ sinh trong 30 ngày mà vẫn nóng thì dẫn tới paste.</summary>
    public static RuleResult CpuHot(bool sensorAvailable, IEnumerable<string> hotDays, string today, string? lastDustDone)
    {
        if (!sensorAvailable) return RuleResult.NotAvailable;
        var n = CountDaysInWindow(hotDays, today, Thresholds.HotWindowDays);
        if (n < Thresholds.HotDaysNeeded) return RuleResult.Ok;
        var data = new Dictionary<string, object> { ["days"] = n };
        var recentlyCleaned = lastDustDone is not null && LocalDay.TryParse(lastDustDone, out _)
            && LocalDay.DaysBetween(lastDustDone, today) is >= 0 and <= Thresholds.RecentCleanDays;
        if (recentlyCleaned)
            return RuleResult.Alert(new Finding(CpuHotRule, Urgency.Maintenance, MaintenanceTask.Paste, null,
                "Your PC still runs very hot",
                $"Your PC got very hot under heavy use {Times(n)} this week, even after the recent clean. Fresh thermal paste on the processor often fixes this. It takes about an hour.",
                data));
        return RuleResult.Alert(new Finding(CpuHotRule, Urgency.Maintenance, MaintenanceTask.Dust, null,
            "Your PC is running hot",
            $"Your PC got very hot under heavy use {Times(n)} this week. Dust inside usually causes this. Cleaning takes about 30 minutes.",
            data));
    }

    /// <summary>GPU nóng khi chơi game, ở ≥ 2 ngày khác nhau trong 7 ngày</summary>
    public static RuleResult GpuHot(bool sensorAvailable, IEnumerable<string> hotDays, string today)
    {
        if (!sensorAvailable) return RuleResult.NotAvailable;
        var n = CountDaysInWindow(hotDays, today, Thresholds.HotWindowDays);
        if (n < Thresholds.HotDaysNeeded) return RuleResult.Ok;
        return RuleResult.Alert(new Finding(GpuHotRule, Urgency.Maintenance, MaintenanceTask.Dust, null,
            "Your PC is running hot",
            $"Your PC got very hot while gaming {Times(n)} this week. Dust inside usually causes this. Cleaning takes about 30 minutes.",
            new Dictionary<string, object> { ["days"] = n }));
    }

    /// <summary>Một mẫu đo có cho thấy quạt CPU đứng không. Chỉ xét quạt đã từng quay (cổng không cắm quạt luôn báo 0).</summary>
    public static bool FanLooksStopped(double? cpuTempC, double? fanRpm, bool fanSeenSpinning) =>
        fanSeenSpinning && fanRpm is { } rpm && rpm < 1 && cpuTempC is { } t && t > Thresholds.FanStopCpuC;

    /// <summary>Quạt đứng kéo dài (đã qua bộ đếm 30 giây). Báo ngay.</summary>
    public static RuleResult FanStopped(bool sensorAvailable, bool stoppedLongEnough, double? cpuTempC)
    {
        if (!sensorAvailable) return RuleResult.NotAvailable;
        if (!stoppedLongEnough) return RuleResult.Ok;
        return RuleResult.Alert(new Finding(FanStoppedRule, Urgency.Urgent, MaintenanceTask.Check, null,
            "The CPU fan seems to have stopped",
            "The CPU fan seems to have stopped. Turn off the PC and check it.",
            new Dictionary<string, object> { ["cpuTempC"] = Math.Round(cpuTempC ?? 0) }));
    }

    /// <summary>
    /// Tình trạng ổ đĩa từ MSFT_PhysicalDisk.HealthStatus ("Healthy", "Warning", "Unhealthy", "Unknown").
    /// null = không đọc được. "Unknown" không tính là hỏng.
    /// </summary>
    public static RuleResult DiskFailing(IReadOnlyList<string>? healthStatuses)
    {
        if (healthStatuses is null || healthStatuses.Count == 0) return RuleResult.NotAvailable;
        var bad = healthStatuses.Count(s => s is "Warning" or "Unhealthy");
        if (bad == 0) return RuleResult.Ok;
        return RuleResult.Alert(new Finding(DiskFailingRule, Urgency.Urgent, null, null,
            "Back up your files now",
            "One of your drives reports a problem and may stop working soon. Copy your important files to another drive or to online storage today.",
            new Dictionary<string, object> { ["drives"] = bad }));
    }

    /// <summary>Ổ hệ thống còn trống &lt; 10%. Gợi ý nhẹ, không phải việc bảo dưỡng phần cứng.</summary>
    public static RuleResult SystemDriveLow(long? freeBytes, long? totalBytes)
    {
        if (freeBytes is not { } free || totalBytes is not { } total || total <= 0) return RuleResult.NotAvailable;
        var fraction = (double)free / total;
        if (fraction >= Thresholds.SystemDriveFreeFraction) return RuleResult.Ok;
        var pct = Math.Max(0, (int)Math.Floor(fraction * 100));
        return RuleResult.Alert(new Finding(DriveLowRule, Urgency.Light, null, null,
            "Your main drive is almost full",
            $"Only {pct}% of space is left. Deleting old downloads or emptying the Recycle Bin can help your PC run smoothly.",
            new Dictionary<string, object> { ["freePercent"] = pct }));
    }

    /// <summary>
    /// Tắt máy đột ngột (Event Log System, nguồn Kernel-Power, mã 41) ≥ 2 lần trong 7 ngày. Chỉ báo khi có lần mới từ lần báo trước.
    /// null = không đọc được Event Log.
    /// </summary>
    public static RuleResult UnexpectedShutdowns(IReadOnlyList<DateTimeOffset>? events, DateTimeOffset now, DateTimeOffset? alreadyAlertedUpTo)
    {
        if (events is null) return RuleResult.NotAvailable;
        var recent = events.Where(e => e <= now && now - e <= TimeSpan.FromDays(Thresholds.ShutdownWindowDays)).ToList();
        if (recent.Count < Thresholds.ShutdownsNeeded) return RuleResult.Ok;
        if (alreadyAlertedUpTo is { } seen && recent.Max() <= seen) return RuleResult.Ok;
        var n = recent.Count;
        return RuleResult.Alert(new Finding(ShutdownRule, Urgency.Maintenance, MaintenanceTask.Dust, null,
            "Your PC turned off by itself",
            $"Your PC shut down unexpectedly {n} times this week. A PC that gets too hot can switch itself off, and dust inside is a common cause. Cleaning takes about 30 minutes.",
            new Dictionary<string, object> { ["count"] = n }));
    }

    /// <summary>RAM dùng &gt; 90% trong &gt; 50% thời gian máy chạy của 7 ngày gần nhất. Gợi ý trang /upgrade, không phải bảo dưỡng.</summary>
    public static RuleResult RamFull(IEnumerable<RamDay> days, string today)
    {
        var window = days.Where(d => LocalDay.TryParse(d.Day, out _) && LocalDay.DaysBetween(d.Day, today) is >= 0 and var n && n < Thresholds.RamWindowDays).ToList();
        var seconds = window.Sum(d => d.Seconds);
        if (seconds < Thresholds.RamMinHours * 3600) return RuleResult.Ok;
        var high = window.Sum(d => d.HighSeconds) / seconds;
        if (high <= Thresholds.RamHighTimeFraction) return RuleResult.Ok;
        return RuleResult.Alert(new Finding(RamFullRule, Urgency.Light, null, "/upgrade",
            "Your PC often runs out of memory",
            "This week your PC used almost all of its memory more than half of the time. This can make it slow. Adding memory may help.",
            new Dictionary<string, object> { ["highPercent"] = (int)Math.Round(high * 100) }));
    }
}
