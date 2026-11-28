using SmartCarePC.Helper.Core;
using SmartCarePC.Helper.Core.Display;
using SmartCarePC.Helper.Core.Health;
using Xunit;

namespace SmartCarePC.Helper.Tests;

/// <summary>Nội dung cửa sổ Status: chữ cho người không rành kỹ thuật, không lặp "not available" ở từng dòng</summary>
public class StatusPageTests
{
    static readonly DateTimeOffset Now = new(2026, 11, 28, 20, 0, 0, TimeSpan.FromHours(7));

    static StatusSnapshot Snap(Dictionary<string, RuleResult>? rules = null, HealthSample? last = null)
    {
        rules ??= new() { [Rules.CpuHotRule] = RuleResult.Ok, [Rules.DriveLowRule] = RuleResult.Ok };
        return new StatusSnapshot(
            last ?? new HealthSample(Now, 0.123, 61.6, 1180, null, 54.4, 0.48),
            OverallStatus.Of(rules), rules,
            new[] { "Healthy", "Healthy" }, (370L, 1000L),
            0, 0, 0, 0, Now.AddDays(-3), null, null, new AgentSettings(), null, 0);
    }

    static StatusSection Section(StatusPage page, string key) => Assert.Single(page.Sections, s => s.Key == key);
    static StatusRow Row(StatusPage page, string section, string key) => Assert.Single(Section(page, section).Rows, r => r.Key == key);

    static MaintenancePlan Plan(StudyInfo? study = null) => new()
    {
        Pc = new PcInfo("standard_tower", "air", true),
        Dates = { [MaintenanceTask.Check] = "2026-12-28", [MaintenanceTask.Dust] = "2026-12-05", [MaintenanceTask.Paste] = "2029-05-28" },
        Study = study,
        Origin = "https://smartcarepc.pages.dev",
    };

    [Fact]
    public void BeforeTheFirstReadingOnlyTheCheckingHeaderShows()
    {
        var page = StatusPage.Build(null, Now);
        Assert.Equal(OverallLevel.Checking, page.Hero.Level);
        Assert.Equal("Checking your PC", page.Hero.Headline);
        Assert.NotNull(page.Hero.Summary);
        Assert.Empty(page.Sections);
    }

    [Fact]
    public void EveryCheckSaysItsStatusInWords()
    {
        var rules = new Dictionary<string, RuleResult>
        {
            [Rules.CpuHotRule] = RuleResult.NotAvailable,
            [Rules.DiskFailingRule] = Rules.DiskFailing(new[] { "Warning" }),
            [Rules.BlueScreenRule] = Rules.BlueScreens(new[] { Now.AddDays(-1) }, Now, null),
            [Rules.NoRestartRule] = Rules.NoRestart(Now.AddDays(-20), Now),
            [Rules.DriveLowRule] = RuleResult.Ok,
        };
        var page = StatusPage.Build(Snap(rules), Now);

        Assert.Equal((CheckTone.NotChecked, "Not checked"), (Row(page, "checks", Rules.CpuHotRule).Tone!.Value, Row(page, "checks", Rules.CpuHotRule).Value));
        Assert.Equal((CheckTone.Urgent, "Needs help now"), (Row(page, "checks", Rules.DiskFailingRule).Tone!.Value, Row(page, "checks", Rules.DiskFailingRule).Value));
        Assert.Equal((CheckTone.Attention, "Needs attention"), (Row(page, "checks", Rules.BlueScreenRule).Tone!.Value, Row(page, "checks", Rules.BlueScreenRule).Value));
        Assert.Equal((CheckTone.Tip, "Tip"), (Row(page, "checks", Rules.NoRestartRule).Tone!.Value, Row(page, "checks", Rules.NoRestartRule).Value));
        Assert.Equal((CheckTone.Ok, "OK"), (Row(page, "checks", Rules.DriveLowRule).Tone!.Value, Row(page, "checks", Rules.DriveLowRule).Value));
        Assert.Equal("Processor heat", Row(page, "checks", Rules.CpuHotRule).Label);
    }

    [Fact]
    public void ChecksThatNeedALookComeFirstAndOkOnesKeepTheirOrder()
    {
        var rules = new Dictionary<string, RuleResult>
        {
            [Rules.DriveLowRule] = RuleResult.Ok,
            [Rules.CpuHotRule] = RuleResult.NotAvailable,
            [Rules.RamFullRule] = RuleResult.Ok,
            [Rules.NoRestartRule] = Rules.NoRestart(Now.AddDays(-20), Now),
            [Rules.BlueScreenRule] = Rules.BlueScreens(new[] { Now.AddDays(-1) }, Now, null),
            [Rules.DiskFailingRule] = Rules.DiskFailing(new[] { "Warning" }),
            [Rules.ShutdownRule] = RuleResult.Ok,
        };
        var keys = Section(StatusPage.Build(Snap(rules), Now), "checks").Rows.Select(r => r.Key);
        Assert.Equal(new[] { Rules.DiskFailingRule, Rules.BlueScreenRule, Rules.NoRestartRule, Rules.CpuHotRule, Rules.DriveLowRule, Rules.RamFullRule, Rules.ShutdownRule }, keys);
    }

    [Fact]
    public void HeaderCarriesProblemsTipsAndOneNotCheckedSentence()
    {
        var rules = new Dictionary<string, RuleResult>
        {
            [Rules.CpuHotRule] = RuleResult.NotAvailable,
            [Rules.FanStoppedRule] = RuleResult.NotAvailable,
            [Rules.BlueScreenRule] = Rules.BlueScreens(new[] { Now.AddDays(-1) }, Now, null),
            [Rules.NoRestartRule] = Rules.NoRestart(Now.AddDays(-20), Now),
        };
        var hero = StatusPage.Build(Snap(rules), Now).Hero;

        Assert.Equal(OverallLevel.NeedsAttention, hero.Level);
        Assert.Equal("Needs attention", hero.Headline);
        Assert.Null(hero.Summary);
        Assert.Equal(new[] { "Your PC hit a serious error" }, hero.Problems);
        Assert.Equal(new[] { "Time to restart your PC" }, hero.Tips);
        Assert.StartsWith("Not checked on this PC: Processor heat, Processor fan.", hero.NotCheckedNote);
    }

    [Fact]
    public void ReadingsAreWrittenForPeople()
    {
        var page = StatusPage.Build(Snap(), Now);
        Assert.Equal("12%", Row(page, "readings", "cpu_load").Value);
        Assert.Equal("62 °C", Row(page, "readings", "cpu_temp").Value);
        Assert.Equal("1180 RPM", Row(page, "readings", "cpu_fan").Value);
        Assert.Equal("54 °C", Row(page, "readings", "gpu_temp").Value);
        Assert.Equal("48%", Row(page, "readings", "ram").Value);
        Assert.Equal("37%", Row(page, "readings", "drive_free").Value);
        Assert.Equal("Healthy (2 drives)", Row(page, "readings", "drive_health").Value);
        Assert.Equal("0", Row(page, "readings", "shutdowns").Value);
        Assert.Equal("3 days ago", Row(page, "readings", "last_restart").Value);
        Assert.All(Section(page, "readings").Rows, r => Assert.Null(r.Tone));
    }

    [Fact]
    public void MissingReadingsShareOneGreyNoteInsteadOfRepeatingIt()
    {
        var s = Snap(last: new HealthSample(Now, 0.2, null, null, null, null, 0.5)) with
        {
            Disks = null, Drive = (null, null), BlueScreensThisWeek = null, LastBoot = null,
        };
        var page = StatusPage.Build(s, Now);
        var readings = Section(page, "readings");

        foreach (var key in new[] { "cpu_temp", "cpu_fan", "gpu_temp", "drive_free", "drive_health", "blue_screens", "last_restart" })
            Assert.True(Row(page, "readings", key).IsMissing, key);
        Assert.Equal(new[] { StatusPage.MissingNote }, readings.Notes);
        var everything = page.Sections.SelectMany(x => x.Rows.Select(r => r.Value ?? "").Concat(x.Notes));
        Assert.DoesNotContain(everything, t => t.Contains("not available", StringComparison.OrdinalIgnoreCase));
    }

    [Fact]
    public void NoDashNoteWhenEverythingCanBeRead()
    {
        Assert.Empty(Section(StatusPage.Build(Snap(), Now), "readings").Notes);
    }

    [Fact]
    public void CleaningComparisonIsShownWithTheReadings()
    {
        var page = StatusPage.Build(Snap() with { BaselineSentence = "Since the clean, your PC runs 6 °C cooler." }, Now);
        Assert.Equal(new[] { "Since the clean, your PC runs 6 °C cooler." }, Section(page, "readings").Notes);
    }

    [Theory]
    [InlineData(0.2, "today")]
    [InlineData(1.5, "1 day ago")]
    [InlineData(9.9, "9 days ago")]
    public void LastRestartReadsNaturally(double daysAgo, string expected)
    {
        var page = StatusPage.Build(Snap() with { LastBoot = Now.AddDays(-daysAgo) }, Now);
        Assert.Equal(expected, Row(page, "readings", "last_restart").Value);
    }

    [Fact]
    public void DrivesInDifferentStatesAreEachListed()
    {
        var page = StatusPage.Build(Snap() with { Disks = new[] { "Healthy", "Warning" } }, Now);
        Assert.Equal("Healthy, Warning", Row(page, "readings", "drive_health").Value);
        var one = StatusPage.Build(Snap() with { Disks = new[] { "Healthy" } }, Now);
        Assert.Equal("Healthy", Row(one, "readings", "drive_health").Value);
    }

    [Fact]
    public void WithoutAPlanRemindersPointToTheWebsite()
    {
        var reminders = Section(StatusPage.Build(Snap(), Now), "reminders");
        Assert.Empty(reminders.Rows);
        Assert.Contains(reminders.Notes, n => n.Contains("SmartCarePC website"));
    }

    [Fact]
    public void PlanDatesAreListedInTaskOrder()
    {
        var page = StatusPage.Build(Snap() with { Plan = Plan() }, Now);
        var rows = Section(page, "reminders").Rows;
        Assert.Equal(new[] { "dust", "paste", "check" }, rows.Select(r => r.Key));
        Assert.Equal(("Clean out dust", "5 Dec 2026"), (rows[0].Label, rows[0].Value));
        Assert.Empty(Section(page, "reminders").Notes);
    }

    [Fact]
    public void PauseIsShownOnlyWhileItLasts()
    {
        var paused = StatusPage.Build(Snap() with { PausedUntil = Now.AddHours(30) }, Now);
        Assert.Equal("until 30 Nov, 02:00", Row(paused, "reminders", "paused").Value);
        var over = StatusPage.Build(Snap() with { PausedUntil = Now.AddHours(-1) }, Now);
        Assert.DoesNotContain(Section(over, "reminders").Rows, r => r.Key == "paused");
    }

    [Fact]
    public void PrivacyTalksAboutTheStudyOnlyWhenTakingPart()
    {
        var plain = Section(StatusPage.Build(Snap(), Now), "privacy");
        Assert.Equal(new[] { "Your readings stay on this PC. This app sends nothing over the internet." }, plain.Notes);

        var study = Section(StatusPage.Build(Snap() with { Plan = Plan(new StudyInfo("P07", "before")), PendingEvents = 2 }, Now), "privacy");
        Assert.Contains(study.Notes, n => n.Contains("anonymous code") && n.Contains("Waiting to send: 2."));
    }

    [Fact]
    public void SectionsKeepAFixedOrderSoTheWindowDoesNotJump()
    {
        var page = StatusPage.Build(Snap() with { Plan = Plan() }, Now);
        Assert.Equal(new[] { "checks", "readings", "reminders", "privacy" }, page.Sections.Select(s => s.Key));
        Assert.Equal(new[] { "Checks", "Latest readings", "Reminders", "Privacy" }, page.Sections.Select(s => s.Title));
    }
}
