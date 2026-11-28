namespace SmartCarePC.Helper.Core;

/// <summary>Ba việc bảo dưỡng, cùng id với web (MaintenanceTask trong src/data/maintenanceSchedule.ts)</summary>
public enum MaintenanceTask { Dust, Paste, Check }

public static class Tasks
{
    public static readonly MaintenanceTask[] All = [MaintenanceTask.Dust, MaintenanceTask.Paste, MaintenanceTask.Check];
    public static string Id(this MaintenanceTask t) => t switch
    {
        MaintenanceTask.Dust => "dust",
        MaintenanceTask.Paste => "paste",
        _ => "check",
    };
    public static bool TryParse(string? id, out MaintenanceTask t)
    {
        switch (id)
        {
            case "dust": t = MaintenanceTask.Dust; return true;
            case "paste": t = MaintenanceTask.Paste; return true;
            case "check": t = MaintenanceTask.Check; return true;
            default: t = default; return false;
        }
    }
}

/// <summary>Máy của người dùng, giống MaintenancePC trên web. Không có tên máy, tên người dùng hay số serial.</summary>
public sealed record PcInfo(string Machine, string Cooler, bool HasGpu);

/// <summary>Chỉ có khi người dùng đang ở chế độ thử nghiệm (?thunghiem=). Mã ẩn danh + phase.</summary>
public sealed record StudyInfo(string Code, string Phase);

/// <summary>Lịch nhận từ web qua smartcarepc://plan?d=...</summary>
public sealed class MaintenancePlan
{
    public required PcInfo Pc { get; init; }
    /// <summary>Ngày hẹn từng việc (yyyy-MM-dd). Web tính bằng maintenancePlan, app chỉ đọc.</summary>
    public Dictionary<MaintenanceTask, string> Dates { get; init; } = new();
    /// <summary>Ngày làm xong gần nhất từng việc</summary>
    public Dictionary<MaintenanceTask, string> Completed { get; init; } = new();
    public StudyInfo? Study { get; init; }
    public required string Origin { get; init; }
}
