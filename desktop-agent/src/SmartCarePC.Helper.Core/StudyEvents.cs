using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;

namespace SmartCarePC.Helper.Core;

/// <summary>Cùng dạng với MaintenanceEvent của web và MaintenanceEventRequest của backend</summary>
public sealed record StudyEvent(
    string EventId,
    string EventType,
    string Task,
    string ParticipantCode,
    string Phase,
    string Machine,
    /// <summary>Giờ UTC dạng 2026-11-28T05:00:00.000Z, giống toISOString() của web</summary>
    string OccurredAt,
    Dictionary<string, object> Data);

/// <summary>
/// Tạo sự kiện nghiên cứu. Chỉ tạo khi lịch nhận từ web có study (đang thử nghiệm); không thì trả null và app không gọi mạng.
/// Các loại mới (spec mục 7): agent_linked, agent_reminder_shown, health_alert_shown, agent_notification_clicked, agent_snoozed.
/// </summary>
public static class StudyEvents
{
    public const string Version = "1.0.0";
    public const int MaxDataKeys = 20; // giới hạn của DTO: data tối đa 20 khoá

    public static StudyEvent? Create(MaintenancePlan? plan, string eventType, MaintenanceTask? task, DateTimeOffset now, IReadOnlyDictionary<string, object>? data = null)
    {
        if (plan?.Study is not { } study) return null;
        var d = new Dictionary<string, object>();
        foreach (var (k, v) in data ?? new Dictionary<string, object>())
        {
            if (d.Count >= MaxDataKeys) break;
            d[k] = v is double x ? Math.Round(x) : v; // số đo làm tròn
        }
        return new StudyEvent(Guid.NewGuid().ToString(), eventType, task?.Id() ?? "none", study.Code, study.Phase, plan.Pc.Machine,
            now.ToUniversalTime().ToString("yyyy-MM-dd'T'HH:mm:ss.fff'Z'", System.Globalization.CultureInfo.InvariantCulture), d);
    }

    public static StudyEvent? Linked(MaintenancePlan plan, DateTimeOffset now) =>
        Create(plan, "agent_linked", null, now, new Dictionary<string, object> { ["version"] = Version });

    public static StudyEvent? Shown(MaintenancePlan? plan, ToastRequest t, DateTimeOffset now) => t.Source switch
    {
        ToastSource.Schedule => Create(plan, "agent_reminder_shown", t.Task, now, new Dictionary<string, object> { ["overdueDays"] = t.OverdueDays ?? 0 }),
        ToastSource.Health => Create(plan, "health_alert_shown", t.Task, now, Merge(new() { ["rule"] = t.Rule! }, t.Data)),
        _ => null,
    };

    public static StudyEvent? Clicked(MaintenancePlan? plan, ToastRequest t, DateTimeOffset now) =>
        t.Source == ToastSource.Setup ? null : Create(plan, "agent_notification_clicked", t.Task, now, SourceData(t));

    public static StudyEvent? Snoozed(MaintenancePlan? plan, ToastRequest t, DateTimeOffset now) =>
        t.Source == ToastSource.Setup ? null : Create(plan, "agent_snoozed", t.Task, now, SourceData(t));

    static Dictionary<string, object> SourceData(ToastRequest t)
    {
        var d = new Dictionary<string, object> { ["source"] = t.Source == ToastSource.Schedule ? "schedule" : "health" };
        if (t.Rule is not null) d["rule"] = t.Rule;
        return d;
    }

    static Dictionary<string, object> Merge(Dictionary<string, object> a, IReadOnlyDictionary<string, object> b)
    {
        foreach (var (k, v) in b) a.TryAdd(k, v);
        return a;
    }
}

public enum SendResult { Sent, Rejected, Failed }

/// <summary>
/// Hàng đợi sự kiện trên đĩa, giống flushQueue của web: xếp hàng trước, gửi lần lượt, lỗi mạng thì dừng và gửi lại sau,
/// giữ nguyên eventId để máy chủ không ghi trùng. Máy chủ từ chối hẳn (4xx) thì chuyển sang rejected để không chặn cả hàng.
/// </summary>
public sealed class EventQueue(string path)
{
    const int MaxQueued = 2000;
    readonly object gate = new();
    int flushing;

    sealed class Saved
    {
        public List<StudyEvent> Pending { get; set; } = new();
        public List<StudyEvent> Rejected { get; set; } = new();
    }

    Saved Read()
    {
        try { return File.Exists(path) ? JsonSerializer.Deserialize<Saved>(File.ReadAllText(path), StateStore.Json) ?? new() : new(); }
        catch { return new(); }
    }

    void Write(Saved s)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(path)!);
        var tmp = path + ".tmp";
        File.WriteAllText(tmp, JsonSerializer.Serialize(s, StateStore.Json));
        File.Move(tmp, path, overwrite: true);
    }

    public void Enqueue(StudyEvent? e)
    {
        if (e is null) return;
        lock (gate)
        {
            var s = Read();
            s.Pending.Add(e);
            if (s.Pending.Count > MaxQueued) s.Pending.RemoveRange(0, s.Pending.Count - MaxQueued);
            Write(s);
        }
    }

    public IReadOnlyList<StudyEvent> Pending { get { lock (gate) return Read().Pending; } }
    public IReadOnlyList<StudyEvent> Rejected { get { lock (gate) return Read().Rejected; } }

    public async Task FlushAsync(Func<StudyEvent, Task<SendResult>> send)
    {
        if (Interlocked.Exchange(ref flushing, 1) == 1) return;
        try
        {
            while (true)
            {
                StudyEvent? next;
                lock (gate) next = Read().Pending.FirstOrDefault();
                if (next is null) return;
                var r = await send(next).ConfigureAwait(false);
                if (r == SendResult.Failed) return;
                lock (gate)
                {
                    var s = Read();
                    s.Pending.RemoveAll(x => x.EventId == next.EventId);
                    if (r == SendResult.Rejected) { s.Rejected.Add(next); if (s.Rejected.Count > 500) s.Rejected.RemoveAt(0); }
                    Write(s);
                }
            }
        }
        finally { Volatile.Write(ref flushing, 0); }
    }
}

/// <summary>Gửi sự kiện tới &lt;origin&gt;/api/maintenance/events. User-Agent riêng vì Cloudflare chặn một số User-Agent mặc định (lỗi 1010).</summary>
public sealed class EventSender(HttpClient http)
{
    public const string UserAgent = "SmartCarePCHelper/1.0";

    public static HttpClient CreateClient()
    {
        var c = new HttpClient { Timeout = TimeSpan.FromSeconds(10) };
        c.DefaultRequestHeaders.UserAgent.ParseAdd(UserAgent);
        return c;
    }

    public async Task<SendResult> SendAsync(string origin, StudyEvent e)
    {
        if (!PlanLink.AllowedOrigins.Contains(origin)) return SendResult.Rejected;
        try
        {
            var body = new StringContent(JsonSerializer.Serialize(e, StateStore.Json), Encoding.UTF8);
            body.Headers.ContentType = new MediaTypeHeaderValue("application/json");
            using var r = await http.PostAsync(origin + "/api/maintenance/events", body).ConfigureAwait(false);
            if (r.IsSuccessStatusCode) return SendResult.Sent;
            var code = (int)r.StatusCode;
            return code is >= 400 and < 500 && r.StatusCode is not (HttpStatusCode.RequestTimeout or HttpStatusCode.TooManyRequests)
                ? SendResult.Rejected : SendResult.Failed;
        }
        catch (Exception) { return SendResult.Failed; }
    }
}
