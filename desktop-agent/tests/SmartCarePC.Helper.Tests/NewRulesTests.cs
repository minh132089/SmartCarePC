using System.Text.RegularExpressions;
using SmartCarePC.Helper.Core;
using SmartCarePC.Helper.Core.Health;
using Xunit;

namespace SmartCarePC.Helper.Tests;

/// <summary>Các luật đọc bằng tài khoản thường: màn hình xanh, lỗi ổ đĩa, lỗi phần cứng, CPU bận khi rảnh, lâu không khởi động lại</summary>
public class NewRulesTests
{
    const string Today = "2026-11-28";
    static readonly DateTimeOffset Now = new(2026, 11, 28, 12, 0, 0, TimeSpan.FromHours(7));

    [Fact]
    public void Rule_names_pass_the_backend_pattern()
    {
        // Backend kiểm tra data.rule của health_alert_shown bằng [a-z_]{1,40}
        foreach (var rule in new[] { Rules.BlueScreenRule, Rules.DiskErrorsRule, Rules.HardwareErrorsRule, Rules.IdleBusyRule, Rules.NoRestartRule })
            Assert.Matches(new Regex("^[a-z_]{1,40}$"), rule);
    }

    [Fact]
    public void Blue_screen_once_this_week_alerts()
    {
        var r = Rules.BlueScreens([Now.AddDays(-2)], Now, null);
        Assert.Equal(RuleStatus.Alert, r.Status);
        Assert.Equal(Urgency.Maintenance, r.Finding!.Urgency);
        Assert.Null(r.Finding.GuideTask); // màn hình xanh thường do driver / phần mềm, không phải bụi
        Assert.Contains("Once can happen", r.Finding.Body);
        Assert.Contains("3 times", Rules.BlueScreens([Now, Now.AddDays(-1), Now.AddDays(-2)], Now, null).Finding!.Body);
    }

    [Fact]
    public void Blue_screen_ok_not_available_and_already_shown()
    {
        Assert.Equal(RuleStatus.Ok, Rules.BlueScreens([], Now, null).Status);
        Assert.Equal(RuleStatus.Ok, Rules.BlueScreens([Now.AddDays(-8)], Now, null).Status); // ngoài 7 ngày
        Assert.Equal(RuleStatus.NotAvailable, Rules.BlueScreens(null, Now, null).Status);
        Assert.True(Rules.BlueScreens([Now.AddDays(-2)], Now, Now.AddDays(-2)).Finding!.AlreadyShown);
        Assert.False(Rules.BlueScreens([Now.AddHours(-1), Now.AddDays(-2)], Now, Now.AddDays(-2)).Finding!.AlreadyShown);
    }

    [Fact]
    public void Disk_errors_need_three_this_week()
    {
        Assert.Equal(RuleStatus.Ok, Rules.DiskErrors([Now, Now.AddDays(-1)], Now, null).Status);
        Assert.Equal(RuleStatus.Ok, Rules.DiskErrors([Now, Now.AddDays(-1), Now.AddDays(-9)], Now, null).Status);
        var r = Rules.DiskErrors([Now, Now.AddDays(-1), Now.AddDays(-6)], Now, null);
        Assert.Equal(RuleStatus.Alert, r.Status);
        Assert.Contains("3 times this week", r.Finding!.Body);
        Assert.Contains("Copy your important files", r.Finding.Body);
        Assert.Equal(3, r.Finding.Data["count"]);
        Assert.Equal(RuleStatus.NotAvailable, Rules.DiskErrors(null, Now, null).Status);
    }

    [Fact]
    public void Hardware_errors_once_this_week_alerts()
    {
        var r = Rules.HardwareErrors([Now.AddDays(-3)], Now, null);
        Assert.Equal(RuleStatus.Alert, r.Status);
        Assert.Equal(Urgency.Maintenance, r.Finding!.Urgency);
        Assert.Equal(RuleStatus.Ok, Rules.HardwareErrors([], Now, null).Status);
        Assert.Equal(RuleStatus.NotAvailable, Rules.HardwareErrors(null, Now, null).Status);
        Assert.Equal(RuleStatus.Ok, Rules.HardwareErrors([Now.AddDays(1)], Now, null).Status); // giờ máy sai: sự kiện ở tương lai không tính
    }

    [Fact]
    public void Busy_while_idle_needs_two_days_this_week()
    {
        var r = Rules.BusyWhileIdle(true, ["2026-11-25", "2026-11-28"], Today);
        Assert.Equal(Urgency.Light, r.Finding!.Urgency);
        Assert.Contains("On 2 days this week", r.Finding.Body);
        Assert.Contains("Task Manager", r.Finding.Body);
        Assert.Equal(RuleStatus.Ok, Rules.BusyWhileIdle(true, ["2026-11-28"], Today).Status);
        Assert.Equal(RuleStatus.Ok, Rules.BusyWhileIdle(true, ["2026-11-20", "2026-11-28"], Today).Status);
        Assert.Equal(RuleStatus.NotAvailable, Rules.BusyWhileIdle(false, ["2026-11-27", "2026-11-28"], Today).Status);
    }

    [Fact]
    public void No_restart_after_fourteen_days()
    {
        var r = Rules.NoRestart(Now.AddDays(-15).AddHours(-2), Now);
        Assert.Equal(Urgency.Light, r.Finding!.Urgency);
        Assert.Contains("15 days", r.Finding.Body);
        Assert.Contains("Restart", r.Finding.Body);
        Assert.Equal(RuleStatus.Alert, Rules.NoRestart(Now.AddDays(-14), Now).Status);
        Assert.Equal(RuleStatus.Ok, Rules.NoRestart(Now.AddDays(-13).AddHours(-23), Now).Status);
        Assert.Equal(RuleStatus.NotAvailable, Rules.NoRestart(null, Now).Status);
        Assert.Equal(RuleStatus.NotAvailable, Rules.NoRestart(Now.AddHours(1), Now).Status);
    }
}

/// <summary>Đếm CPU bận khi không ai dùng máy từ các lần đo</summary>
public class IdleBusyTrackerTests
{
    static readonly DateTimeOffset Start = new(2026, 11, 27, 13, 0, 0, TimeSpan.FromHours(7));

    /// <summary>Đo mỗi 5 giây (CPU bận) trong length. Người dùng chạm máy lần cuối lúc lastInput.</summary>
    static void Away(HealthTracker tr, DateTimeOffset from, TimeSpan length, DateTimeOffset lastInput, double load)
    {
        for (var t = from; t <= from + length; t += TimeSpan.FromSeconds(5))
            tr.Feed(new HealthSample(t, load, null, null, null, null, 0.4, (t - lastInput).TotalSeconds));
    }

    [Fact]
    public void Busy_for_thirty_minutes_while_away_records_the_day()
    {
        var tr = new HealthTracker(new HealthHistory());
        Away(tr, Start, TimeSpan.FromMinutes(29), Start.AddMinutes(-1), 0.8);
        Assert.Empty(tr.History.IdleBusyDays);
        Away(tr, Start.AddMinutes(29).AddSeconds(5), TimeSpan.FromMinutes(2), Start.AddMinutes(-1), 0.8);
        Assert.Equal(["2026-11-27"], tr.History.IdleBusyDays);
        Assert.True(tr.UserInputSeen);
    }

    [Fact]
    public void Touching_the_mouse_restarts_the_count()
    {
        var tr = new HealthTracker(new HealthHistory());
        Away(tr, Start, TimeSpan.FromMinutes(20), Start, 0.9);
        // Người dùng chạm chuột lúc 20 phút: đợt mới bắt đầu
        Away(tr, Start.AddMinutes(20).AddSeconds(5), TimeSpan.FromMinutes(20), Start.AddMinutes(20), 0.9);
        Assert.Empty(tr.History.IdleBusyDays);
    }

    [Fact]
    public void Light_load_while_away_does_not_count_but_later_busy_does()
    {
        var tr = new HealthTracker(new HealthHistory());
        var last = Start.AddMinutes(-1);
        Away(tr, Start, TimeSpan.FromMinutes(31), last, 0.3);
        Assert.Empty(tr.History.IdleBusyDays);
        // Vẫn chưa ai chạm máy, 30 phút sau CPU bận: vẫn ghi
        Away(tr, Start.AddMinutes(31).AddSeconds(5), TimeSpan.FromMinutes(31), last, 0.9);
        Assert.Single(tr.History.IdleBusyDays);
    }

    [Fact]
    public void Sleep_gap_restarts_the_count()
    {
        var tr = new HealthTracker(new HealthHistory());
        var last = Start.AddMinutes(-1);
        Away(tr, Start, TimeSpan.FromMinutes(20), last, 0.9);
        Away(tr, Start.AddMinutes(60), TimeSpan.FromMinutes(15), last, 0.9); // máy ngủ 40 phút ở giữa
        Assert.Empty(tr.History.IdleBusyDays);
    }

    [Fact]
    public void No_input_reading_means_not_available()
    {
        var tr = new HealthTracker(new HealthHistory());
        tr.Feed(new HealthSample(Start, 0.9, null, null, null, null, 0.4));
        Assert.False(tr.UserInputSeen);
        Assert.Equal(RuleStatus.NotAvailable, Rules.BusyWhileIdle(tr.UserInputSeen, tr.History.IdleBusyDays, "2026-11-27").Status);
    }
}

/// <summary>Kết luận tổng "máy chạy ổn không"</summary>
public class OverallTests
{
    static readonly DateTimeOffset Now = new(2026, 11, 28, 12, 0, 0, TimeSpan.FromHours(7));

    static Dictionary<string, RuleResult> AllOk() => new()
    {
        [Rules.CpuHotRule] = RuleResult.Ok,
        [Rules.DiskFailingRule] = RuleResult.Ok,
        [Rules.DriveLowRule] = RuleResult.Ok,
        [Rules.BlueScreenRule] = RuleResult.Ok,
        [Rules.NoRestartRule] = RuleResult.Ok,
    };

    [Fact]
    public void All_ok_is_running_well()
    {
        var o = OverallStatus.Of(AllOk());
        Assert.Equal(OverallLevel.RunningWell, o.Level);
        Assert.Equal("Running well", o.Headline);
        Assert.Equal("SmartCarePC Helper: Running well", o.TrayText);
        Assert.Empty(o.NotChecked);
    }

    [Fact]
    public void Unreadable_checks_are_listed_and_not_counted_as_ok()
    {
        var r = AllOk();
        r[Rules.CpuHotRule] = RuleResult.NotAvailable;
        r[Rules.FanStoppedRule] = RuleResult.NotAvailable;
        var o = OverallStatus.Of(r);
        Assert.Equal(OverallLevel.RunningWell, o.Level);
        Assert.Equal([Rules.CpuHotRule, Rules.FanStoppedRule], o.NotChecked);
        Assert.Contains(o.Lines, l => l.StartsWith("Not checked on this PC: Processor heat, Processor fan.") && l.Contains("not counted as OK"));
        Assert.Equal("SmartCarePC Helper: Running well, 2 not checked", o.TrayText);
    }

    [Fact]
    public void Maintenance_alert_needs_attention()
    {
        var r = AllOk();
        r[Rules.BlueScreenRule] = Rules.BlueScreens([Now.AddDays(-1)], Now, null);
        var o = OverallStatus.Of(r);
        Assert.Equal(OverallLevel.NeedsAttention, o.Level);
        Assert.Equal(["Your PC hit a serious error"], o.Problems);
        Assert.Contains("• Your PC hit a serious error", o.Lines);
    }

    [Fact]
    public void Already_shown_alert_still_needs_attention()
    {
        var r = AllOk();
        r[Rules.ShutdownRule] = Rules.UnexpectedShutdowns([Now.AddDays(-1), Now.AddDays(-2)], Now, Now.AddDays(-1));
        Assert.Equal(OverallLevel.NeedsAttention, OverallStatus.Of(r).Level);
    }

    [Fact]
    public void Urgent_alert_needs_help_now_and_comes_first()
    {
        var r = AllOk();
        r[Rules.BlueScreenRule] = Rules.BlueScreens([Now.AddDays(-1)], Now, null);
        r[Rules.DiskFailingRule] = Rules.DiskFailing(["Unhealthy"]);
        var o = OverallStatus.Of(r);
        Assert.Equal(OverallLevel.NeedsHelpNow, o.Level);
        Assert.Equal("Back up your files now", o.Problems[0]);
        Assert.Equal("SmartCarePC Helper: Needs help now", o.TrayText);
    }

    [Fact]
    public void Light_tips_do_not_change_the_level()
    {
        var r = AllOk();
        r[Rules.NoRestartRule] = Rules.NoRestart(Now.AddDays(-20), Now);
        r[Rules.DriveLowRule] = Rules.SystemDriveLow(5, 100);
        var o = OverallStatus.Of(r);
        Assert.Equal(OverallLevel.RunningWell, o.Level);
        Assert.Empty(o.Problems);
        Assert.Equal(2, o.Tips.Count);
        Assert.Contains("Tip: Time to restart your PC", o.Lines);
    }

    [Fact]
    public void Nothing_checked_yet_is_not_running_well()
    {
        Assert.Equal(OverallLevel.Checking, OverallStatus.Of(new Dictionary<string, RuleResult>()).Level);
        var allNa = new Dictionary<string, RuleResult> { [Rules.CpuHotRule] = RuleResult.NotAvailable };
        var o = OverallStatus.Of(allNa);
        Assert.Equal(OverallLevel.Checking, o.Level);
        Assert.Contains("None of the checks could run on this PC yet.", o.Lines);
    }

    [Fact]
    public void Tray_text_fits_windows_limit()
    {
        var r = new Dictionary<string, RuleResult>();
        for (var i = 0; i < 12; i++) r["rule_" + new string('x', i + 1)] = RuleResult.NotAvailable;
        r[Rules.DriveLowRule] = RuleResult.Ok;
        Assert.True(OverallStatus.Of(r).TrayText.Length <= 63);
    }
}

/// <summary>Luật mới đi qua đúng quy tắc không làm phiền có sẵn</summary>
public class NewRulesPlannerTests
{
    static readonly DateTimeOffset Noon = new(2026, 11, 28, 12, 0, 0, TimeSpan.FromHours(7));
    static AgentState State() => new() { Plan = PlannerTests.Plan(dust: "2027-05-01") };

    [Fact]
    public void Already_shown_findings_are_not_toasted_again()
    {
        var st = State();
        var seen = Rules.BlueScreens([Noon.AddDays(-1)], Noon, Noon.AddDays(-1)).Finding!;
        Assert.Empty(Planner.Decide(st, Noon, [seen], false));
    }

    [Fact]
    public void Event_log_rules_respect_quiet_hours_and_one_per_day()
    {
        var st = State();
        var bsod = Rules.BlueScreens([Noon.AddDays(-1)], Noon, null).Finding!;
        var disk = Rules.DiskErrors([Noon, Noon, Noon], Noon, null).Finding!;
        var hw = Rules.HardwareErrors([Noon], Noon, null).Finding!;
        Assert.Empty(Planner.Decide(st, Noon.AddHours(11), [bsod, disk, hw], false)); // 23:00, giờ yên lặng
        Assert.Empty(Planner.Decide(st, Noon, [bsod, disk, hw], userBusy: true)); // đang chơi game toàn màn hình
        // Lỗi ổ đĩa trước (có thể mất dữ liệu), rồi màn hình xanh hôm sau
        Assert.Equal("health:disk_errors", Assert.Single(Planner.Decide(st, Noon, [bsod, disk, hw], false)).Key);
        Assert.Empty(Planner.Decide(st, Noon.AddHours(2), [bsod, disk, hw], false));
        Assert.Equal("health:blue_screen", Assert.Single(Planner.Decide(st, Noon.AddDays(1), [bsod, disk, hw], false)).Key);
    }

    [Fact]
    public void Light_rules_share_the_daily_tip_limit()
    {
        var st = State();
        var restart = Rules.NoRestart(Noon.AddDays(-20), Noon).Finding!;
        var idle = Rules.BusyWhileIdle(true, ["2026-11-27", "2026-11-28"], "2026-11-28").Finding!;
        Assert.Single(Planner.Decide(st, Noon, [restart, idle], false));
        Assert.Empty(Planner.Decide(st, Noon.AddHours(3), [restart, idle], false));
        Assert.Single(Planner.Decide(st, Noon.AddDays(1), [restart, idle], false)); // gợi ý còn lại, hôm sau
        Assert.Empty(Planner.Decide(st, Noon.AddDays(2), [restart, idle], false)); // mỗi luật 1 lần / 7 ngày
    }

    [Fact]
    public void Shown_event_keeps_rule_name_for_the_backend()
    {
        var st = State();
        var t = Assert.Single(Planner.Decide(st, Noon, [Rules.HardwareErrors([Noon], Noon, null).Finding!], false));
        var e = StudyEvents.Shown(PlannerTests.Plan(study: new StudyInfo("P07", "before")), t, Noon)!;
        Assert.Equal("health_alert_shown", e.EventType);
        Assert.Equal("hardware_errors", e.Data["rule"]);
    }
}
