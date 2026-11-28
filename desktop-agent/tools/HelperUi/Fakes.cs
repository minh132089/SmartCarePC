using SmartCarePC.Helper.Core;
using SmartCarePC.Helper.Core.Display;
using SmartCarePC.Helper.Core.Health;

namespace SmartCarePC.HelperUi;

/// <summary>
/// Dữ liệu giả cho 4 mức kết luận, dùng chính các hàm luật thật để chữ giống hệt lúc chạy.
/// Màu cam và đỏ khó tạo bằng tay trên máy thật, nên chụp từ đây.
/// </summary>
static class Fakes
{
    static readonly DateTimeOffset Now = DateTimeOffset.Now;
    static readonly string Today = LocalDay.Of(Now);
    const long GB = 1L << 30;

    public static IEnumerable<(string Name, StatusSnapshot? Snapshot)> All() =>
    [
        ("checking", null),
        ("running-well", RunningWell()),
        ("needs-attention", NeedsAttention()),
        ("needs-help-now", NeedsHelpNow()),
    ];

    /// <summary>Máy chạy bằng tài khoản thường: không đọc được nhiệt độ và quạt, phần còn lại ổn</summary>
    public static StatusSnapshot RunningWell() => Snap(
        Rules(cpuHot: RuleResult.NotAvailable, gpuHot: RuleResult.NotAvailable, fan: RuleResult.NotAvailable),
        new HealthSample(Now, 0.08, null, null, null, null, 0.46),
        drive: (148 * GB, 476 * GB));

    public static StatusSnapshot NeedsAttention() => Snap(
        Rules(blue: Helper.Core.Health.Rules.BlueScreens([Now.AddDays(-1)], Now, null),
              noRestart: Helper.Core.Health.Rules.NoRestart(Now.AddDays(-20), Now),
              driveLow: Helper.Core.Health.Rules.SystemDriveLow(38 * GB, 476 * GB)),
        new HealthSample(Now, 0.23, 64.2, 1240, 0.05, 51, 0.62),
        drive: (38 * GB, 476 * GB), blueScreens: 1, lastBoot: Now.AddDays(-20),
        plan: Plan(null), paused: Now.AddHours(20));

    public static StatusSnapshot NeedsHelpNow() => Snap(
        Rules(disk: Helper.Core.Health.Rules.DiskFailing(["Healthy", "Warning"]),
              cpuHot: Helper.Core.Health.Rules.CpuHot(true, [Today, LocalDay.AddDays(Today, -2)], Today, null)),
        new HealthSample(Now, 0.71, 91.4, 2150, 0.4, 77, 0.81),
        drive: (201 * GB, 932 * GB), disks: ["Healthy", "Warning"], diskErrors: 4,
        plan: Plan(new StudyInfo("P07", "before")), baseline: null);

    static Dictionary<string, RuleResult> Rules(RuleResult? cpuHot = null, RuleResult? gpuHot = null, RuleResult? fan = null,
        RuleResult? disk = null, RuleResult? driveLow = null, RuleResult? blue = null, RuleResult? noRestart = null) => new()
    {
        [Helper.Core.Health.Rules.CpuHotRule] = cpuHot ?? RuleResult.Ok,
        [Helper.Core.Health.Rules.GpuHotRule] = gpuHot ?? RuleResult.Ok,
        [Helper.Core.Health.Rules.FanStoppedRule] = fan ?? RuleResult.Ok,
        [Helper.Core.Health.Rules.DiskFailingRule] = disk ?? RuleResult.Ok,
        [Helper.Core.Health.Rules.DriveLowRule] = driveLow ?? RuleResult.Ok,
        [Helper.Core.Health.Rules.ShutdownRule] = RuleResult.Ok,
        [Helper.Core.Health.Rules.RamFullRule] = RuleResult.Ok,
        [Helper.Core.Health.Rules.BlueScreenRule] = blue ?? RuleResult.Ok,
        [Helper.Core.Health.Rules.DiskErrorsRule] = RuleResult.Ok,
        [Helper.Core.Health.Rules.HardwareErrorsRule] = RuleResult.Ok,
        [Helper.Core.Health.Rules.IdleBusyRule] = RuleResult.Ok,
        [Helper.Core.Health.Rules.NoRestartRule] = noRestart ?? RuleResult.Ok,
    };

    static StatusSnapshot Snap(Dictionary<string, RuleResult> rules, HealthSample last, (long, long) drive,
        IReadOnlyList<string>? disks = null, int blueScreens = 0, int diskErrors = 0, DateTimeOffset? lastBoot = null,
        MaintenancePlan? plan = null, DateTimeOffset? paused = null, string? baseline = null) =>
        new(last, OverallStatus.Of(rules), rules, disks ?? ["Healthy", "Healthy"], drive, 0, blueScreens, diskErrors, 0,
            lastBoot ?? Now.AddDays(-3), plan, paused, new AgentSettings(), baseline, plan?.Study is null ? 0 : 2);

    static MaintenancePlan Plan(StudyInfo? study) => new()
    {
        Pc = new PcInfo("standard_tower", "air", true),
        Dates = { [MaintenanceTask.Dust] = LocalDay.AddDays(Today, 7), [MaintenanceTask.Paste] = LocalDay.AddDays(Today, 900), [MaintenanceTask.Check] = LocalDay.AddDays(Today, 30) },
        Study = study,
        Origin = "https://smartcarepc.pages.dev",
    };
}
