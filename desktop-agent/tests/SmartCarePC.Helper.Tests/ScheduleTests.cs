using System.Text.Json;
using SmartCarePC.Helper.Core;
using SmartCarePC.Helper.Core.Health;
using Xunit;

namespace SmartCarePC.Helper.Tests;

public class LocalDayTests
{
    record WebCase(string Fn, string Day, int N, string Result);

    [Fact]
    public void Matches_web_addDays_and_addMonths()
    {
        var json = File.ReadAllText(Path.Combine(AppContext.BaseDirectory, "fixtures", "web-dates.json"));
        var cases = JsonSerializer.Deserialize<List<WebCase>>(json, new JsonSerializerOptions { PropertyNameCaseInsensitive = true })!;
        Assert.True(cases.Count > 100);
        foreach (var c in cases)
        {
            var got = c.Fn == "addMonths" ? LocalDay.AddMonths(c.Day, c.N) : LocalDay.AddDays(c.Day, c.N);
            Assert.True(got == c.Result, $"{c.Fn}({c.Day}, {c.N}) web={c.Result} app={got}");
        }
    }

    [Theory]
    [InlineData("2026-02-30")]
    [InlineData("2026-1-01")]
    [InlineData("26-01-01")]
    [InlineData("2026-01-01T00:00")]
    [InlineData("1900-01-01")]
    [InlineData("")]
    public void Rejects_bad_days(string s) => Assert.False(LocalDay.TryParse(s, out _));

    [Theory]
    [InlineData("2026-05-28", "2026-11-28", 6)]
    [InlineData("2026-05-29", "2026-11-28", 5)]
    [InlineData("2026-08-31", "2027-02-28", 6)]
    [InlineData("2026-11-28", "2026-11-01", 0)]
    public void Whole_months(string from, string to, int expected) => Assert.Equal(expected, LocalDay.WholeMonthsBetween(from, to));
}

public class PlannerTests
{
    static readonly DateTimeOffset Noon = new(2026, 11, 28, 12, 0, 0, TimeSpan.FromHours(7));

    internal static MaintenancePlan Plan(string dust = "2026-11-28", string? doneDust = "2026-05-28", StudyInfo? study = null) => new()
    {
        Pc = new PcInfo("gaming", "air", true),
        Dates = new() { [MaintenanceTask.Dust] = dust, [MaintenanceTask.Paste] = "2029-05-28", [MaintenanceTask.Check] = "2026-12-28" },
        Completed = doneDust is null ? new() : new() { [MaintenanceTask.Dust] = doneDust },
        Study = study,
        Origin = "https://smartcarepc.pages.dev",
    };

    static AgentState State(MaintenancePlan? p = null) => new() { Plan = p ?? Plan() };
    static List<ToastRequest> Decide(AgentState st, DateTimeOffset at, bool busy = false, params Finding[] f) => Planner.Decide(st, at, f, busy);

    [Fact]
    public void Shows_schedule_reminder_on_due_day_with_plain_reason_and_guide_link()
    {
        var t = Assert.Single(Decide(State(), Noon));
        Assert.Equal("schedule:dust", t.Key);
        Assert.Equal("Time to clean your PC", t.Title);
        Assert.Contains("It has been 6 months since the last clean.", t.Body);
        Assert.Equal("https://smartcarepc.pages.dev/?baotri=dust", t.OpenUrl);
        Assert.True(t.CanSnooze);
    }

    [Fact]
    public void Not_shown_before_due_day()
    {
        var st = State(Plan(dust: "2026-11-29"));
        Assert.Empty(Decide(st, Noon));
    }

    [Fact]
    public void PC_off_on_due_day_shows_on_next_start()
    {
        var st = State(Plan(dust: "2026-11-20"));
        var t = Assert.Single(Decide(st, Noon));
        Assert.Equal(8, t.OverdueDays);
    }

    [Fact]
    public void At_most_one_maintenance_toast_per_day()
    {
        var p = Plan();
        p.Dates[MaintenanceTask.Check] = "2026-11-01";
        var st = State(p);
        var first = Assert.Single(Decide(st, Noon));
        Assert.Equal("schedule:check", first.Key); // quá hạn lâu nhất trước
        Assert.Empty(Decide(st, Noon.AddHours(2)));
        Assert.Equal("schedule:dust", Assert.Single(Decide(st, Noon.AddDays(1))).Key);
    }

    [Fact]
    public void Snooze_waits_exactly_seven_days_like_the_web()
    {
        var st = State();
        var t = Assert.Single(Decide(st, Noon));
        Planner.Snooze(st, t.Key, Noon);
        Assert.Equal("2026-12-05", st.Plan!.Dates[MaintenanceTask.Dust]); // web: addDays(day(), 7)
        for (var d = 1; d < 7; d++) Assert.Empty(Decide(st, Noon.AddDays(d)));
        Assert.Equal("schedule:dust", Assert.Single(Decide(st, Noon.AddDays(7))).Key);
    }

    [Fact]
    public void Unanswered_reminder_repeats_after_three_days()
    {
        var st = State();
        Assert.Single(Decide(st, Noon));
        Assert.Empty(Decide(st, Noon.AddDays(1)));
        Assert.Empty(Decide(st, Noon.AddDays(2)));
        Assert.Single(Decide(st, Noon.AddDays(3)));
    }

    [Fact]
    public void Start_guide_keeps_quiet_for_a_week_and_new_plan_resets()
    {
        var st = State();
        var t = Assert.Single(Decide(st, Noon));
        Planner.Started(st, t.Key, Noon);
        Assert.Empty(Decide(st, Noon.AddDays(4)));
        // Web gửi lịch mới sau khi làm xong: hẹn 6 tháng sau, không nhắc nữa
        Planner.ApplyPlan(st, Plan(dust: "2027-05-28", doneDust: "2026-11-29"), Noon.AddDays(1));
        Assert.Empty(Decide(st, Noon.AddDays(10)));
    }

    [Theory]
    [InlineData(22, true)]
    [InlineData(23, true)]
    [InlineData(3, true)]
    [InlineData(7, true)]
    [InlineData(8, false)]
    [InlineData(21, false)]
    public void Quiet_hours_default_22_to_8(int hour, bool quiet)
    {
        var at = new DateTimeOffset(2026, 11, 28, hour, 30, 0, TimeSpan.Zero);
        Assert.Equal(quiet, Planner.InQuietHours(at.DateTime, new AgentSettings()));
    }

    [Fact]
    public void No_maintenance_toast_in_quiet_hours_paused_or_fullscreen_but_urgent_still_shows()
    {
        var night = new DateTimeOffset(2026, 11, 28, 23, 0, 0, TimeSpan.FromHours(7));
        var fan = Rules.FanStopped(true, true, 85).Finding!;
        var st = State();
        var shown = Decide(st, new DateTimeOffset(night.DateTime, night.Offset), false, fan);
        Assert.Equal("health:fan_stopped", Assert.Single(shown).Key);
        Assert.False(Assert.Single(shown).CanSnooze);

        var st2 = State();
        st2.PausedUntil = Noon.AddDays(1);
        Assert.Empty(Decide(st2, Noon));
        Assert.Empty(Decide(State(), Noon, busy: true));
    }

    [Fact]
    public void Urgent_alert_not_repeated_within_a_day()
    {
        var disk = Rules.DiskFailing(["Warning"]).Finding!;
        var st = State();
        Assert.Contains(Decide(st, Noon, false, disk), t => t.Key == "health:disk_failing");
        Assert.DoesNotContain(Decide(st, Noon.AddHours(5), false, disk), t => t.Key == "health:disk_failing");
        Assert.Contains(Decide(st, Noon.AddHours(25), false, disk), t => t.Key == "health:disk_failing");
    }

    [Fact]
    public void Health_alert_wins_over_schedule_and_counts_toward_daily_limit()
    {
        var hot = Rules.GpuHot(true, ["2026-11-27", "2026-11-28"], "2026-11-28").Finding!;
        var st = State();
        var t = Assert.Single(Decide(st, Noon, false, hot));
        Assert.Equal("health:gpu_hot", t.Key);
        Assert.Equal("https://smartcarepc.pages.dev/?baotri=dust", t.OpenUrl);
        Assert.Empty(Decide(st, Noon.AddHours(1), false, hot));
    }

    [Fact]
    public void Health_snooze_silences_rule_for_seven_days()
    {
        var hot = Rules.GpuHot(true, ["2026-11-27", "2026-11-28"], "2026-11-28").Finding!;
        var st = State(Plan(dust: "2027-05-01"));
        var t = Assert.Single(Decide(st, Noon, false, hot));
        Planner.Snooze(st, t.Key, Noon);
        Assert.Empty(Decide(st, Noon.AddDays(6), false, hot));
        Assert.Single(Decide(st, Noon.AddDays(7).AddMinutes(1), false, hot));
    }

    [Fact]
    public void Without_a_plan_says_hello_once_and_never_nags()
    {
        // Không có lịch là bình thường: nhắc theo lịch do email của web lo, app chỉ kiểm tra sức khoẻ
        var st = new AgentState();
        var t = Assert.Single(Decide(st, Noon));
        Assert.Equal("setup", t.Key);
        Assert.Equal("SmartCarePC Helper is checking your PC", t.Title);
        Assert.DoesNotContain("plan", t.Body);
        Assert.Null(t.OpenUrl);
        Assert.False(t.CanSnooze);
        Assert.Empty(Decide(st, Noon.AddDays(1)));
        Assert.Empty(Decide(st, Noon.AddDays(3)));
        Assert.Empty(Decide(st, Noon.AddDays(60)));
    }

    [Fact]
    public void Without_a_plan_health_alerts_still_work()
    {
        var st = new AgentState { SetupPromptLastDay = "2026-11-01" };
        var bsod = Rules.BlueScreens([Noon.AddHours(-3)], Noon, null).Finding!;
        var t = Assert.Single(Decide(st, Noon, false, bsod));
        Assert.Equal("health:blue_screen", t.Key);
        Assert.Null(t.OpenUrl); // không dẫn vào bài bảo dưỡng
    }

    [Fact]
    public void Study_links_keep_the_anonymous_code_like_calendarLink()
    {
        var st = State(Plan(study: new StudyInfo("P07", "after")));
        var t = Assert.Single(Decide(st, Noon));
        Assert.Equal("https://smartcarepc.pages.dev/?baotri=dust&thunghiem=P07&lan=sau", t.OpenUrl);
    }

    [Fact]
    public void Light_tips_have_their_own_weekly_limit()
    {
        var low = Rules.SystemDriveLow(5, 100).Finding!;
        var st = State(Plan(dust: "2027-05-01"));
        Assert.Equal("health:drive_low", Assert.Single(Decide(st, Noon, false, low)).Key);
        Assert.Empty(Decide(st, Noon.AddDays(3), false, low));
        Assert.Single(Decide(st, Noon.AddDays(7).AddMinutes(1), false, low));
    }

    [Fact]
    public void Cleaning_sets_baseline_and_restarts_heat_count()
    {
        var st = State();
        st.Health.CpuHotDays.AddRange(["2026-11-20", "2026-11-25"]);
        for (var i = 0; i < 3; i++)
            st.Health.LoadDays.Add(new LoadDay { Day = LocalDay.AddDays("2026-11-28", -(i + 1)), CpuSum = 92 * 20, CpuCount = 20 });
        Planner.ApplyPlan(st, Plan(dust: "2027-05-28", doneDust: "2026-11-28"), Noon);
        Assert.Empty(st.Health.CpuHotDays);
        Assert.Equal(92, st.Health.Baseline!.CpuBeforeC!.Value, 3);
        st.Health.LoadDays.Add(new LoadDay { Day = "2026-12-02", CpuSum = 80 * 30, CpuCount = 30 });
        Assert.Equal("After cleaning, your CPU runs 12 °C cooler under load.", Baselines.Compare(st.Health.Baseline, st.Health.LoadDays));
    }

    [Fact]
    public void State_round_trips_through_json()
    {
        var dir = Directory.CreateTempSubdirectory();
        var store = new StateStore(Path.Combine(dir.FullName, "state.json"));
        var st = State(Plan(study: new StudyInfo("P07", "before")));
        st.RuleQuietUntil["cpu_hot"] = Noon;
        st.Health.RamDays.Add(new RamDay { Day = "2026-11-28", Seconds = 60, HighSeconds = 30 });
        store.Save(st);
        var back = store.Load();
        Assert.Equal("2026-11-28", back.Plan!.Dates[MaintenanceTask.Dust]);
        Assert.Equal("P07", back.Plan.Study!.Code);
        Assert.Equal(Noon, back.RuleQuietUntil["cpu_hot"]);
        Assert.Equal(30, back.Health.RamDays[0].HighSeconds);
        File.WriteAllText(store.Path, "{ not json");
        Assert.Null(store.Load().Plan);
    }
}
