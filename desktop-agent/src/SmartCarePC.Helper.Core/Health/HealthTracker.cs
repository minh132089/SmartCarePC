namespace SmartCarePC.Helper.Core.Health;

/// <summary>
/// Một lần đo. Giá trị null = cảm biến không đọc được trên máy này.
/// UserIdleSeconds: số giây từ lần cuối người dùng chạm chuột / bàn phím (GetLastInputInfo).
/// </summary>
public sealed record HealthSample(
    DateTimeOffset Time,
    double CpuLoad,
    double? CpuTempC,
    double? CpuFanRpm,
    double? GpuLoad,
    double? GpuTempC,
    double RamUsed,
    double? UserIdleSeconds = null);

/// <summary>Thời gian máy chạy trong một ngày và phần thời gian RAM gần đầy</summary>
public sealed class RamDay
{
    public string Day { get; set; } = "";
    public double Seconds { get; set; }
    public double HighSeconds { get; set; }
}

/// <summary>Nhiệt độ lúc chạy nặng trong một ngày, để làm mốc trước / sau khi vệ sinh</summary>
public sealed class LoadDay
{
    public string Day { get; set; } = "";
    public double CpuSum { get; set; }
    public int CpuCount { get; set; }
    public double GpuSum { get; set; }
    public int GpuCount { get; set; }
}

/// <summary>Mốc nhiệt độ ngay trước một lần vệ sinh</summary>
public sealed class CleanBaseline
{
    public string CleanDay { get; set; } = "";
    public double? CpuBeforeC { get; set; }
    public double? GpuBeforeC { get; set; }
}

/// <summary>Lịch sử đo, chỉ nằm trên máy (%LOCALAPPDATA%\SmartCarePC\state.json). Giữ tối đa 45 ngày.</summary>
public sealed class HealthHistory
{
    public List<string> CpuHotDays { get; set; } = new();
    public List<string> GpuHotDays { get; set; } = new();
    public List<RamDay> RamDays { get; set; } = new();
    public List<LoadDay> LoadDays { get; set; } = new();
    /// <summary>Ngày có ít nhất một đợt CPU bận &gt; 60% suốt 30 phút khi không ai dùng máy</summary>
    public List<string> IdleBusyDays { get; set; } = new();
    public bool CpuFanSeenSpinning { get; set; }
    public DateTimeOffset? ShutdownAlertedUpTo { get; set; }
    /// <summary>Sự kiện Event Log mới nhất đã báo, theo từng luật (blue_screen, disk_errors, hardware_errors)</summary>
    public Dictionary<string, DateTimeOffset> EventAlertedUpTo { get; set; } = new();
    public CleanBaseline? Baseline { get; set; }
}

/// <summary>Đếm điều kiện giữ liên tục bao lâu. Mất mẫu quá lâu (máy ngủ) thì đếm lại từ đầu. Báo một lần cho mỗi đợt.</summary>
public sealed class SustainedDetector(TimeSpan hold, TimeSpan maxGap)
{
    DateTimeOffset? start, last;
    bool fired;

    public bool Feed(DateTimeOffset t, bool condition)
    {
        var gap = last is { } l && t - l > maxGap;
        last = t;
        if (!condition) { start = null; fired = false; return false; }
        if (start is null || gap) { start = t; fired = false; }
        if (fired || t - start < hold) return false;
        fired = true;
        return true;
    }
}

/// <summary>Nhận mẫu đo, ghi lại các đợt nóng / RAM / quạt vào HealthHistory. Không đọc cảm biến, không hiện thông báo.</summary>
public sealed class HealthTracker(HealthHistory history)
{
    static readonly TimeSpan MaxGap = TimeSpan.FromSeconds(90);
    readonly SustainedDetector cpuHot = new(Thresholds.HotHold, MaxGap);
    readonly SustainedDetector gpuHot = new(Thresholds.HotHold, MaxGap);
    readonly SustainedDetector fanStop = new(Thresholds.FanStopHold, MaxGap);
    DateTimeOffset? lastTime;
    // Đợt không có thao tác hiện tại: lúc người dùng chạm máy lần cuối, số giây đã đo, tổng tải CPU nhân thời gian
    DateTimeOffset? awaySince;
    double awaySeconds, awayLoad;
    bool awayFired;

    public HealthHistory History => history;
    /// <summary>Có cảm biến nào từng đọc được ở lần chạy này</summary>
    public bool CpuTempSeen { get; private set; }
    public bool GpuTempSeen { get; private set; }
    public bool FanSeen { get; private set; }
    public bool UserInputSeen { get; private set; }
    /// <summary>Quạt vừa đứng đủ lâu; bên gọi đọc rồi đặt lại false</summary>
    public bool FanStoppedPending { get; set; }
    public HealthSample? Last { get; private set; }

    /// <summary>Lần đo sau nên cách bao lâu: 5 giây khi chạy nặng, 60 giây khi rảnh</summary>
    public static TimeSpan NextInterval(HealthSample s) =>
        s.CpuLoad >= Thresholds.BusyCpuLoad || (s.GpuLoad ?? 0) >= Thresholds.BusyGpuLoad ? Thresholds.BusyInterval : Thresholds.IdleInterval;

    public void Feed(HealthSample s)
    {
        var day = LocalDay.Of(s.Time);
        var gap = lastTime is { } prev && s.Time - prev > MaxGap;
        var dt = lastTime is { } lt ? Math.Clamp((s.Time - lt).TotalSeconds, 0, Thresholds.IdleInterval.TotalSeconds * 2) : Thresholds.IdleInterval.TotalSeconds;
        lastTime = s.Time;
        Last = s;
        CpuTempSeen |= s.CpuTempC.HasValue;
        GpuTempSeen |= s.GpuTempC.HasValue;
        FanSeen |= s.CpuFanRpm.HasValue;

        var ram = history.RamDays.FirstOrDefault(d => d.Day == day);
        if (ram is null) history.RamDays.Add(ram = new RamDay { Day = day });
        ram.Seconds += dt;
        if (s.RamUsed > Thresholds.RamHighFraction) ram.HighSeconds += dt;

        var cpuHotNow = s.CpuTempC is { } ct && s.CpuLoad > Thresholds.CpuHotLoad && ct >= Thresholds.CpuHotC;
        if (cpuHot.Feed(s.Time, cpuHotNow) && !history.CpuHotDays.Contains(day)) history.CpuHotDays.Add(day);

        var gpuHotNow = s.GpuTempC is { } gt && (s.GpuLoad ?? 0) >= Thresholds.GpuBusyLoad && gt >= Thresholds.GpuHotC;
        if (gpuHot.Feed(s.Time, gpuHotNow) && !history.GpuHotDays.Contains(day)) history.GpuHotDays.Add(day);

        if (s.CpuFanRpm is >= 1) history.CpuFanSeenSpinning = true;
        if (fanStop.Feed(s.Time, Rules.FanLooksStopped(s.CpuTempC, s.CpuFanRpm, history.CpuFanSeenSpinning))) FanStoppedPending = true;

        if (s.UserIdleSeconds is { } idle) FeedAway(s, day, TimeSpan.FromSeconds(Math.Max(0, idle)), dt, gap);

        // Mốc nhiệt độ lúc chạy nặng, để so trước / sau khi vệ sinh
        if (s.CpuTempC is { } c && s.CpuLoad > Thresholds.CpuHotLoad) Load(day).Add(c, gpu: false);
        if (s.GpuTempC is { } g && (s.GpuLoad ?? 0) >= Thresholds.GpuBusyLoad) Load(day).Add(g, gpu: true);

        Prune(day);
    }

    /// <summary>
    /// CPU bận khi không ai dùng máy. Một đợt bắt đầu từ lần chạm chuột / phím cuối. Người dùng chạm máy (mốc lùi hơn 5 giây)
    /// hoặc mất mẫu lâu (máy ngủ) thì đếm lại. Mỗi 30 phút số đo xét một lần: tải trung bình &gt; 60% thì ghi ngày, mỗi đợt ghi một lần.
    /// </summary>
    void FeedAway(HealthSample s, string day, TimeSpan idle, double dt, bool gap)
    {
        UserInputSeen = true;
        var since = s.Time - idle;
        var touched = awaySince is not { } a || Math.Abs((since - a).TotalSeconds) > 5;
        if (touched || gap || idle < TimeSpan.FromSeconds(dt))
        {
            awaySince = since; awaySeconds = 0; awayLoad = 0; awayFired = false;
            return;
        }
        awaySeconds += dt;
        awayLoad += s.CpuLoad * dt;
        if (awayFired || awaySeconds < Thresholds.IdleBusyHold.TotalSeconds) return;
        if (awayLoad / awaySeconds > Thresholds.IdleBusyLoad)
        {
            awayFired = true;
            if (!history.IdleBusyDays.Contains(day)) history.IdleBusyDays.Add(day);
        }
        else { awaySeconds = 0; awayLoad = 0; } // 30 phút này không bận: xét tiếp 30 phút sau, vẫn trong cùng đợt
    }

    LoadDayWriter Load(string day)
    {
        var d = history.LoadDays.FirstOrDefault(x => x.Day == day);
        if (d is null) history.LoadDays.Add(d = new LoadDay { Day = day });
        return new LoadDayWriter(d);
    }

    readonly struct LoadDayWriter(LoadDay d)
    {
        public void Add(double t, bool gpu)
        {
            if (gpu) { d.GpuSum += t; d.GpuCount++; } else { d.CpuSum += t; d.CpuCount++; }
        }
    }

    void Prune(string today)
    {
        bool Old(string d) => !LocalDay.TryParse(d, out _) || LocalDay.DaysBetween(d, today) > 45;
        history.CpuHotDays.RemoveAll(Old);
        history.GpuHotDays.RemoveAll(Old);
        history.IdleBusyDays.RemoveAll(Old);
        history.RamDays.RemoveAll(x => Old(x.Day));
        history.LoadDays.RemoveAll(x => Old(x.Day));
    }
}

/// <summary>So nhiệt độ lúc chạy nặng trước và sau một lần vệ sinh. Đây là số đo của một máy, không phải kết quả nghiên cứu.</summary>
public static class Baselines
{
    /// <summary>Cần ít nhất 1 phút chạy nặng (12 mẫu cách 5 giây) mới tính</summary>
    public const int MinSamples = 12;

    static double? Avg(IEnumerable<LoadDay> days, bool gpu)
    {
        var list = days.ToList();
        var n = list.Sum(d => gpu ? d.GpuCount : d.CpuCount);
        if (n < MinSamples) return null;
        return list.Sum(d => gpu ? d.GpuSum : d.CpuSum) / n;
    }

    /// <summary>Mốc trước khi vệ sinh: trung bình 14 ngày trước ngày vệ sinh</summary>
    public static CleanBaseline Before(IEnumerable<LoadDay> days, string cleanDay)
    {
        var before = days.Where(d => LocalDay.DaysBetween(d.Day, cleanDay) is >= 1 and <= 14).ToList();
        return new CleanBaseline { CleanDay = cleanDay, CpuBeforeC = Avg(before, false), GpuBeforeC = Avg(before, true) };
    }

    /// <summary>Câu kiểu "After cleaning, your CPU runs 12 °C cooler under load". null khi chưa đủ số đo.</summary>
    public static string? Compare(CleanBaseline? b, IEnumerable<LoadDay> days)
    {
        if (b is null) return null;
        var after = days.Where(d => LocalDay.DaysBetween(b.CleanDay, d.Day) is >= 1 and <= 30).ToList();
        string? Line(string part, double? before, double? now)
        {
            if (before is not { } x || now is not { } y) return null;
            var diff = (int)Math.Round(x - y);
            return diff switch
            {
                >= 1 => $"After cleaning, your {part} runs {diff} °C cooler under load.",
                <= -1 => $"After cleaning, your {part} runs {-diff} °C warmer under load.",
                _ => $"After cleaning, your {part} runs at about the same temperature under load.",
            };
        }
        var lines = new[] { Line("CPU", b.CpuBeforeC, Avg(after, false)), Line("graphics card", b.GpuBeforeC, Avg(after, true)) }
            .Where(l => l is not null).ToList();
        return lines.Count == 0 ? null : string.Join(" ", lines);
    }
}
