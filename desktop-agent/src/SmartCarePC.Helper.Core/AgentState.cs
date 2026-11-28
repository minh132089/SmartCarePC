using System.Text.Json;
using System.Text.Json.Serialization;
using SmartCarePC.Helper.Core.Health;

namespace SmartCarePC.Helper.Core;

public sealed class AgentSettings
{
    /// <summary>Giờ yên lặng, mặc định 22:00–08:00 (spec 5.3)</summary>
    public int QuietStartHour { get; set; } = 22;
    public int QuietEndHour { get; set; } = 8;
}

/// <summary>Mọi thứ app nhớ. Chỉ nằm trên máy, không có tên máy, tên người dùng, serial hay IP.</summary>
public sealed class AgentState
{
    public int Version { get; set; } = 1;
    public MaintenancePlan? Plan { get; set; }
    public DateTimeOffset? LinkedAt { get; set; }
    /// <summary>Ngày đã hiện thông báo chào lần đầu (khi chưa có lịch). Có giá trị rồi thì không chào lại.</summary>
    public string? SetupPromptLastDay { get; set; }
    /// <summary>Ngày gần nhất đã hiện nhắc theo lịch cho từng việc (id dust / paste / check)</summary>
    public Dictionary<string, string> ScheduleLastShown { get; set; } = new();
    /// <summary>Đã bấm Start guide: không nhắc lại việc này tới ngày này</summary>
    public Dictionary<string, string> ScheduleQuietUntil { get; set; } = new();
    /// <summary>Luật sức khoẻ im tới lúc này (đã hiện, đã hoãn hoặc đã bấm)</summary>
    public Dictionary<string, DateTimeOffset> RuleQuietUntil { get; set; } = new();
    public string? LastMaintenanceToastDay { get; set; }
    public string? LastLightToastDay { get; set; }
    public DateTimeOffset? PausedUntil { get; set; }
    public AgentSettings Settings { get; set; } = new();
    public HealthHistory Health { get; set; } = new();
}

/// <summary>Đọc / ghi state.json. Ghi ra file tạm rồi đổi tên, để mất điện giữa chừng không hỏng file.</summary>
public sealed class StateStore(string path)
{
    public static readonly JsonSerializerOptions Json = new()
    {
        WriteIndented = true,
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        Converters = { new JsonStringEnumConverter(JsonNamingPolicy.CamelCase) },
    };

    public string Path => path;

    public AgentState Load()
    {
        try
        {
            if (!File.Exists(path)) return new AgentState();
            return JsonSerializer.Deserialize<AgentState>(File.ReadAllText(path), Json) ?? new AgentState();
        }
        catch (Exception)
        {
            // File hỏng: giữ lại bản hỏng để xem, chạy tiếp với trạng thái trống (người dùng gửi lại lịch là xong)
            try { File.Copy(path, path + ".broken", overwrite: true); } catch { }
            return new AgentState();
        }
    }

    public void Save(AgentState state)
    {
        Directory.CreateDirectory(System.IO.Path.GetDirectoryName(path)!);
        var tmp = path + ".tmp";
        File.WriteAllText(tmp, JsonSerializer.Serialize(state, Json));
        File.Move(tmp, path, overwrite: true);
    }
}
