using System.Text.Json;
using SmartCarePC.Helper.Core;
using SmartCarePC.Helper.Core.Health;
using Xunit;

namespace SmartCarePC.Helper.Tests;

public class StudyEventTests
{
    static readonly DateTimeOffset Now = new(2026, 11, 28, 12, 0, 0, TimeSpan.FromHours(7));

    [Fact]
    public void No_events_outside_the_study()
    {
        var plan = PlannerTests.Plan();
        Assert.Null(StudyEvents.Linked(plan, Now));
        Assert.Null(StudyEvents.Shown(plan, Planner.ScheduleToast(plan, MaintenanceTask.Dust, "2026-11-28"), Now));
        Assert.Null(StudyEvents.Create(null, "agent_linked", null, Now));
    }

    [Fact]
    public void Events_match_the_backend_shape()
    {
        var plan = PlannerTests.Plan(study: new StudyInfo("P07", "before"));
        var hot = Planner.Decide(new AgentState { Plan = plan }, Now, [Rules.FanStopped(true, true, 81.6).Finding!], false).First(t => t.Source == ToastSource.Health);
        var e = StudyEvents.Shown(plan, hot, Now)!;
        Assert.Equal("health_alert_shown", e.EventType);
        Assert.Equal("check", e.Task);
        Assert.Equal("P07", e.ParticipantCode);
        Assert.Equal("before", e.Phase);
        Assert.Equal("gaming", e.Machine);
        Assert.Equal("fan_stopped", e.Data["rule"]);
        Assert.Equal(82.0, e.Data["cpuTempC"]); // làm tròn
        Assert.Matches("^[A-Za-z0-9-]{8,64}$", e.EventId);
        var json = JsonSerializer.Serialize(e, StateStore.Json);
        foreach (var key in new[] { "\"eventId\"", "\"eventType\"", "\"task\"", "\"participantCode\"", "\"phase\"", "\"machine\"", "\"occurredAt\"", "\"data\"" })
            Assert.Contains(key, json);
        Assert.Contains("Z\"", json); // giờ UTC

        var click = StudyEvents.Clicked(plan, hot, Now)!;
        Assert.Equal("agent_notification_clicked", click.EventType);
        Assert.Equal("health", click.Data["source"]);
        var snooze = StudyEvents.Snoozed(plan, Planner.ScheduleToast(plan, MaintenanceTask.Dust, "2026-11-28"), Now)!;
        Assert.Equal("agent_snoozed", snooze.EventType);
        Assert.Equal("schedule", snooze.Data["source"]);
        Assert.False(snooze.Data.ContainsKey("rule"));
        Assert.Equal("1.0.0", StudyEvents.Linked(plan, Now)!.Data["version"]);
    }

    [Fact]
    public void Data_is_capped_at_twenty_keys()
    {
        var plan = PlannerTests.Plan(study: new StudyInfo("P07", "before"));
        var big = Enumerable.Range(0, 30).ToDictionary(i => $"k{i}", i => (object)i);
        Assert.Equal(20, StudyEvents.Create(plan, "health_alert_shown", null, Now, big)!.Data.Count);
    }

    [Fact]
    public async Task Queue_keeps_event_ids_on_retry_and_moves_rejected_aside()
    {
        var dir = Directory.CreateTempSubdirectory();
        var q = new EventQueue(Path.Combine(dir.FullName, "events.json"));
        var plan = PlannerTests.Plan(study: new StudyInfo("P07", "before"));
        var a = StudyEvents.Linked(plan, Now)!; var b = StudyEvents.Linked(plan, Now)!; var c = StudyEvents.Linked(plan, Now)!;
        q.Enqueue(a); q.Enqueue(b); q.Enqueue(c); q.Enqueue(null);
        Assert.Equal(3, q.Pending.Count);

        var seen = new List<string>();
        await q.FlushAsync(e => { seen.Add(e.EventId); return Task.FromResult(SendResult.Failed); }); // mất mạng
        Assert.Equal([a.EventId], seen);
        Assert.Equal(3, q.Pending.Count);

        seen.Clear();
        await q.FlushAsync(e => { seen.Add(e.EventId); return Task.FromResult(e.EventId == b.EventId ? SendResult.Rejected : SendResult.Sent); });
        Assert.Equal([a.EventId, b.EventId, c.EventId], seen); // gửi lại đúng eventId cũ
        Assert.Empty(q.Pending);
        Assert.Equal(b.EventId, Assert.Single(q.Rejected).EventId);
    }

    [Fact]
    public async Task Sender_refuses_unknown_origins_without_calling_the_network()
    {
        var plan = PlannerTests.Plan(study: new StudyInfo("P07", "before"));
        var handler = new CountingHandler();
        var sender = new EventSender(new HttpClient(handler));
        Assert.Equal(SendResult.Rejected, await sender.SendAsync("https://evil.example", StudyEvents.Linked(plan, Now)!));
        Assert.Equal(0, handler.Calls);
        Assert.Equal(SendResult.Sent, await sender.SendAsync("https://smartcarepc.pages.dev", StudyEvents.Linked(plan, Now)!));
        Assert.Equal(1, handler.Calls);
        Assert.Equal("https://smartcarepc.pages.dev/api/maintenance/events", handler.LastUri);
    }

    sealed class CountingHandler : HttpMessageHandler
    {
        public int Calls; public string? LastUri;
        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken ct)
        {
            Calls++; LastUri = request.RequestUri!.ToString();
            return Task.FromResult(new HttpResponseMessage(System.Net.HttpStatusCode.Created));
        }
    }
}
