using SmartCarePC.Helper.Core.Health;

namespace SmartCarePC.Helper.Core.Display;

/// <summary>Ảnh chụp trạng thái cho cửa sổ Status. AgentHost tạo mới sau mỗi lần đo, giao diện chỉ đọc.</summary>
public sealed record StatusSnapshot(
    HealthSample? Last,
    OverallStatus Overall,
    IReadOnlyDictionary<string, RuleResult> Rules,
    IReadOnlyList<string>? Disks,
    (long? Free, long? Total) Drive,
    int? ShutdownsThisWeek,
    int? BlueScreensThisWeek,
    int? DiskErrorsThisWeek,
    int? HardwareErrorsThisWeek,
    DateTimeOffset? LastBoot,
    MaintenancePlan? Plan,
    DateTimeOffset? PausedUntil,
    AgentSettings Settings,
    string? BaselineSentence,
    int PendingEvents);
