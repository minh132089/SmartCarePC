using SmartCarePC.Helper.Core;
using SmartCarePC.Helper.Core.Health;
using Xunit;

namespace SmartCarePC.Helper.Tests;

public class RulesTests
{
    const string Today = "2026-11-28";

    [Fact]
    public void Cpu_hot_on_two_days_points_to_dust()
    {
        var r = Rules.CpuHot(true, ["2026-11-24", "2026-11-28"], Today, lastDustDone: "2026-05-01");
        Assert.Equal(RuleStatus.Alert, r.Status);
        Assert.Equal(MaintenanceTask.Dust, r.Finding!.GuideTask);
        Assert.Contains("twice this week", r.Finding.Body);
        Assert.Contains("Dust inside usually causes this", r.Finding.Body);
    }

    [Fact]
    public void Cpu_hot_after_recent_clean_points_to_paste()
    {
        var r = Rules.CpuHot(true, ["2026-11-24", "2026-11-28"], Today, lastDustDone: "2026-11-10");
        Assert.Equal(MaintenanceTask.Paste, r.Finding!.GuideTask);
    }

    [Theory]
    [InlineData("2026-11-28")]              // một ngày
    [InlineData("2026-11-28,2026-11-28")]   // cùng một ngày hai lần
    [InlineData("2026-11-28,2026-11-21")]   // ngày kia đã ngoài 7 ngày
    [InlineData("")]
    public void Cpu_hot_no_alert(string days) =>
        Assert.Equal(RuleStatus.Ok, Rules.CpuHot(true, days.Split(',', StringSplitOptions.RemoveEmptyEntries), Today, null).Status);

    [Fact]
    public void Cpu_hot_not_available_without_sensor() =>
        Assert.Equal(RuleStatus.NotAvailable, Rules.CpuHot(false, ["2026-11-27", "2026-11-28"], Today, null).Status);

    [Fact]
    public void Gpu_hot_alert_ok_and_not_available()
    {
        Assert.Equal(MaintenanceTask.Dust, Rules.GpuHot(true, ["2026-11-22", "2026-11-28"], Today).Finding!.GuideTask);
        Assert.Contains("while gaming", Rules.GpuHot(true, ["2026-11-22", "2026-11-28"], Today).Finding!.Body);
        Assert.Equal(RuleStatus.Ok, Rules.GpuHot(true, ["2026-11-28"], Today).Status);
        Assert.Equal(RuleStatus.NotAvailable, Rules.GpuHot(false, [], Today).Status);
    }

    [Theory]
    [InlineData(80.0, 0.0, true, true)]
    [InlineData(65.0, 0.0, true, false)]    // CPU chưa nóng: quạt có thể đang tắt theo chế độ êm
    [InlineData(80.0, 900.0, true, false)]
    [InlineData(80.0, 0.0, false, false)]   // cổng chưa từng thấy quạt quay: có thể không cắm quạt
    [InlineData(null, 0.0, true, false)]    // không đọc được nhiệt CPU
    public void Fan_looks_stopped(double? temp, double? rpm, bool seen, bool expected) =>
        Assert.Equal(expected, Rules.FanLooksStopped(temp, rpm, seen));

    [Fact]
    public void Fan_stopped_is_urgent_and_points_to_check()
    {
        var r = Rules.FanStopped(true, true, 82.4);
        Assert.Equal(Urgency.Urgent, r.Finding!.Urgency);
        Assert.Equal(MaintenanceTask.Check, r.Finding.GuideTask);
        Assert.Equal("The CPU fan seems to have stopped. Turn off the PC and check it.", r.Finding.Body);
        Assert.Equal(82.0, r.Finding.Data["cpuTempC"]);
        Assert.Equal(RuleStatus.Ok, Rules.FanStopped(true, false, 60).Status);
        Assert.Equal(RuleStatus.NotAvailable, Rules.FanStopped(false, false, null).Status);
    }

    [Fact]
    public void Disk_failing()
    {
        var r = Rules.DiskFailing(["Healthy", "Unhealthy"]);
        Assert.Equal("Back up your files now", r.Finding!.Title);
        Assert.Null(r.Finding.GuideTask); // không dẫn vào bài bảo dưỡng
        Assert.Equal(Urgency.Urgent, r.Finding.Urgency);
        Assert.Equal(RuleStatus.Alert, Rules.DiskFailing(["Warning"]).Status);
        Assert.Equal(RuleStatus.Ok, Rules.DiskFailing(["Healthy", "Unknown"]).Status);
        Assert.Equal(RuleStatus.NotAvailable, Rules.DiskFailing(null).Status);
        Assert.Equal(RuleStatus.NotAvailable, Rules.DiskFailing([]).Status);
    }

    [Fact]
    public void System_drive_low()
    {
        var r = Rules.SystemDriveLow(6, 100);
        Assert.Equal(Urgency.Light, r.Finding!.Urgency);
        Assert.Contains("Only 6% of space is left", r.Finding.Body);
        Assert.Equal(RuleStatus.Ok, Rules.SystemDriveLow(10, 100).Status);
        Assert.Equal(RuleStatus.NotAvailable, Rules.SystemDriveLow(null, null).Status);
    }

    static readonly DateTimeOffset Now = new(2026, 11, 28, 12, 0, 0, TimeSpan.FromHours(7));

    [Fact]
    public void Unexpected_shutdowns_three_times_this_week()
    {
        var r = Rules.UnexpectedShutdowns([Now.AddDays(-1), Now.AddDays(-3), Now.AddDays(-6)], Now, null);
        Assert.Equal(MaintenanceTask.Dust, r.Finding!.GuideTask);
        Assert.StartsWith("Your PC shut down unexpectedly 3 times this week.", r.Finding.Body);
        Assert.DoesNotContain("Kernel", r.Finding.Body);
    }

    [Fact]
    public void Unexpected_shutdowns_no_alert()
    {
        Assert.Equal(RuleStatus.Ok, Rules.UnexpectedShutdowns([Now.AddDays(-1)], Now, null).Status);
        Assert.Equal(RuleStatus.Ok, Rules.UnexpectedShutdowns([Now.AddDays(-1), Now.AddDays(-8)], Now, null).Status);
        // Đã báo cho hai lần này rồi, chưa có lần mới: Status vẫn ghi cần chú ý, nhưng không thông báo lại
        var seen = Rules.UnexpectedShutdowns([Now.AddDays(-1), Now.AddDays(-2)], Now, Now.AddDays(-1));
        Assert.Equal(RuleStatus.Alert, seen.Status);
        Assert.True(seen.Finding!.AlreadyShown);
        var fresh = Rules.UnexpectedShutdowns([Now.AddHours(-1), Now.AddDays(-1), Now.AddDays(-2)], Now, Now.AddDays(-1));
        Assert.False(fresh.Finding!.AlreadyShown);
        Assert.Equal(RuleStatus.NotAvailable, Rules.UnexpectedShutdowns(null, Now, null).Status);
    }

    static RamDay Ram(string day, double hours, double highHours) => new() { Day = day, Seconds = hours * 3600, HighSeconds = highHours * 3600 };

    [Fact]
    public void Ram_full_more_than_half_the_week()
    {
        var r = Rules.RamFull([Ram("2026-11-27", 6, 4), Ram("2026-11-28", 6, 3)], Today);
        Assert.Equal("/upgrade", r.Finding!.Path);
        Assert.Null(r.Finding.GuideTask);
        Assert.Equal(Urgency.Light, r.Finding.Urgency);
    }

    [Fact]
    public void Ram_full_no_alert()
    {
        Assert.Equal(RuleStatus.Ok, Rules.RamFull([Ram("2026-11-27", 6, 3), Ram("2026-11-28", 6, 3)], Today).Status); // đúng 50%
        Assert.Equal(RuleStatus.Ok, Rules.RamFull([Ram("2026-11-28", 2, 2)], Today).Status); // chưa đủ 10 giờ
        Assert.Equal(RuleStatus.Ok, Rules.RamFull([Ram("2026-11-20", 20, 20), Ram("2026-11-28", 1, 0)], Today).Status); // ngoài 7 ngày
    }

    [Fact]
    public void Messages_use_plain_words()
    {
        var all = new[]
        {
            Rules.CpuHot(true, ["2026-11-27", "2026-11-28"], Today, null), Rules.CpuHot(true, ["2026-11-27", "2026-11-28"], Today, "2026-11-20"),
            Rules.GpuHot(true, ["2026-11-27", "2026-11-28"], Today), Rules.FanStopped(true, true, 80), Rules.DiskFailing(["Warning"]),
            Rules.SystemDriveLow(1, 100), Rules.UnexpectedShutdowns([Now, Now.AddDays(-1)], Now, null),
            Rules.RamFull([Ram("2026-11-28", 12, 12)], Today),
            Rules.BlueScreens([Now], Now, null), Rules.BlueScreens([Now, Now.AddDays(-1)], Now, null),
            Rules.DiskErrors([Now, Now, Now], Now, null), Rules.HardwareErrors([Now], Now, null),
            Rules.BusyWhileIdle(true, ["2026-11-27", "2026-11-28"], Today), Rules.NoRestart(Now.AddDays(-20), Now),
        }.Select(r => r.Finding!.Title + " " + r.Finding.Body).ToList();
        foreach (var text in all)
            foreach (var word in new[] { "throttl", "SMART", "Kernel", "power supply", "PSU", "RPM", "°C", "BugCheck", "WHEA", "Event" })
                Assert.DoesNotContain(word, text);
    }
}

public class TrackerTests
{
    static readonly DateTimeOffset Start = new(2026, 11, 27, 20, 0, 0, TimeSpan.FromHours(7));

    static HealthSample S(DateTimeOffset t, double load = 0.9, double? cpu = 92, double? fan = 1200, double? gpuLoad = null, double? gpu = null, double ram = 0.5) =>
        new(t, load, cpu, fan, gpuLoad, gpu, ram);

    static void Run(HealthTracker tr, DateTimeOffset from, TimeSpan length, Func<DateTimeOffset, HealthSample> make)
    {
        for (var t = from; t <= from + length; t += TimeSpan.FromSeconds(5)) tr.Feed(make(t));
    }

    [Fact]
    public void Hot_must_last_two_minutes()
    {
        var tr = new HealthTracker(new HealthHistory());
        Run(tr, Start, TimeSpan.FromSeconds(100), t => S(t));
        Assert.Empty(tr.History.CpuHotDays);
        Run(tr, Start.AddSeconds(105), TimeSpan.FromSeconds(30), t => S(t));
        Assert.Equal(["2026-11-27"], tr.History.CpuHotDays);
    }

    [Fact]
    public void Light_load_or_cooler_temps_do_not_count()
    {
        var tr = new HealthTracker(new HealthHistory());
        Run(tr, Start, TimeSpan.FromMinutes(5), t => S(t, load: 0.5));
        Run(tr, Start.AddMinutes(10), TimeSpan.FromMinutes(5), t => S(t, cpu: 85));
        Assert.Empty(tr.History.CpuHotDays);
    }

    [Fact]
    public void Sleep_gap_restarts_the_count()
    {
        var tr = new HealthTracker(new HealthHistory());
        Run(tr, Start, TimeSpan.FromSeconds(90), t => S(t));
        Run(tr, Start.AddMinutes(30), TimeSpan.FromSeconds(60), t => S(t));
        Assert.Empty(tr.History.CpuHotDays);
    }

    [Fact]
    public void Two_hot_days_trigger_the_rule_end_to_end()
    {
        var tr = new HealthTracker(new HealthHistory());
        Run(tr, Start, TimeSpan.FromMinutes(3), t => S(t, cpu: null, gpuLoad: 0.95, gpu: 87));
        Run(tr, Start.AddDays(1), TimeSpan.FromMinutes(3), t => S(t, cpu: null, gpuLoad: 0.95, gpu: 87));
        Assert.Equal(2, tr.History.GpuHotDays.Count);
        Assert.Equal(RuleStatus.Alert, Rules.GpuHot(tr.GpuTempSeen, tr.History.GpuHotDays, "2026-11-28").Status);
        Assert.False(tr.CpuTempSeen);
        Assert.Equal(RuleStatus.NotAvailable, Rules.CpuHot(tr.CpuTempSeen, tr.History.CpuHotDays, "2026-11-28", null).Status);
    }

    [Fact]
    public void Fan_stop_needs_a_fan_seen_spinning_and_thirty_seconds()
    {
        var tr = new HealthTracker(new HealthHistory());
        Run(tr, Start, TimeSpan.FromMinutes(1), t => S(t, fan: 0, cpu: 80));
        Assert.False(tr.FanStoppedPending); // chưa từng thấy quay
        Run(tr, Start.AddMinutes(2), TimeSpan.FromSeconds(10), t => S(t, fan: 1100, cpu: 60));
        Run(tr, Start.AddMinutes(3), TimeSpan.FromSeconds(20), t => S(t, fan: 0, cpu: 80));
        Assert.False(tr.FanStoppedPending);
        Run(tr, Start.AddMinutes(3).AddSeconds(25), TimeSpan.FromSeconds(10), t => S(t, fan: 0, cpu: 80));
        Assert.True(tr.FanStoppedPending);
    }

    [Fact]
    public void Ram_time_is_weighted_by_sample_interval()
    {
        var tr = new HealthTracker(new HealthHistory());
        for (var i = 0; i < 60; i++) tr.Feed(S(Start.AddMinutes(i), load: 0.1, ram: i < 40 ? 0.95 : 0.5));
        var d = Assert.Single(tr.History.RamDays);
        Assert.Equal(60 * 60, d.Seconds, 0);
        Assert.Equal(40 * 60, d.HighSeconds, 0);
    }

    [Fact]
    public void Sampling_interval_is_5s_busy_and_60s_idle()
    {
        Assert.Equal(TimeSpan.FromSeconds(5), HealthTracker.NextInterval(S(Start, load: 0.7)));
        Assert.Equal(TimeSpan.FromSeconds(5), HealthTracker.NextInterval(S(Start, load: 0.1, gpuLoad: 0.8)));
        Assert.Equal(TimeSpan.FromSeconds(60), HealthTracker.NextInterval(S(Start, load: 0.1)));
    }

    [Fact]
    public void Old_history_is_dropped()
    {
        var h = new HealthHistory();
        h.CpuHotDays.Add("2026-09-01");
        new HealthTracker(h).Feed(S(Start, load: 0.1));
        Assert.Empty(h.CpuHotDays);
    }
}
