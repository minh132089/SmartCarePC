using System.Collections.Concurrent;
using System.Net.NetworkInformation;
using SmartCarePC.Helper.Core;
using SmartCarePC.Helper.Core.Health;
using SmartCarePC.Helper.Sensors;

namespace SmartCarePC.Helper;

/// <summary>Ảnh chụp trạng thái cho cửa sổ Status</summary>
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

/// <summary>
/// Vòng chạy nền, tất cả trên một luồng: đo, ghi lịch sử, quyết định thông báo, gửi sự kiện nghiên cứu.
/// Việc từ giao diện (bấm thông báo, menu khay, link smartcarepc://) được xếp vào hàng rồi chạy trên luồng này,
/// nên không cần khoá quanh AgentState.
/// </summary>
public sealed class AgentHost : IDisposable
{
    readonly StateStore store = new(Path.Combine(WindowsIntegration.DataDir, "state.json"));
    readonly EventQueue queue = new(Path.Combine(WindowsIntegration.DataDir, "events.json"));
    readonly BlockingCollection<Action> inbox = new();
    readonly CancellationTokenSource stop = new();
    readonly AgentState state;
    readonly HealthTracker tracker;
    Thread? thread;
    EventSender? sender; // chỉ tạo khi đang thử nghiệm: không thử nghiệm thì không có HttpClient nào

    IReadOnlyList<string>? disks;
    (long? Free, long? Total) drive;
    IReadOnlyList<DateTimeOffset>? shutdowns, blueScreens, diskErrors, hardwareErrors;
    DateTimeOffset? lastBoot;
    Dictionary<string, RuleResult> lastRules = new();
    // Quạt đứng chỉ báo một lần mỗi đợt; giữ lời báo trên Status thêm 1 giờ để người dùng kịp thấy
    (Finding Finding, DateTimeOffset At)? lastFanAlert;
    volatile StatusSnapshot? snapshot;
    int pendingEvents;

    public StatusSnapshot? Snapshot => snapshot;

    public AgentHost()
    {
        state = store.Load();
        tracker = new HealthTracker(state.Health);
    }

    public void Start()
    {
        thread = new Thread(Run) { IsBackground = true, Name = "agent", Priority = ThreadPriority.BelowNormal };
        thread.Start();
        NetworkChange.NetworkAvailabilityChanged += OnNetwork;
    }

    void OnNetwork(object? s, NetworkAvailabilityEventArgs e) { if (e.IsAvailable) Post(Flush); }

    public void Post(Action a) { if (!inbox.IsAddingCompleted) inbox.Add(a); }

    void Run()
    {
        var sensors = new SensorReader();
        if (sensors.LibraryError is { } err) Log.Write("sensor library unavailable: " + err);
        DateTimeOffset nextSlow = DateTimeOffset.MinValue, nextDecide = DateTimeOffset.MinValue, nextSave = DateTimeOffset.Now.AddMinutes(5), nextFlush = DateTimeOffset.MinValue;
        var interval = TimeSpan.FromSeconds(5);
        try
        {
            while (!stop.IsCancellationRequested)
            {
                var now = DateTimeOffset.Now;
                try
                {
                    var sample = sensors.Read(now);
                    tracker.Feed(sample);
                    interval = HealthTracker.NextInterval(sample);

                    if (now >= nextSlow) { SlowChecks(); nextSlow = now.AddMinutes(30); }
                    if (now >= nextDecide) { Decide(now); nextDecide = now.AddSeconds(60); }
                    if (now >= nextSave) { Save(); nextSave = now.AddMinutes(5); }
                    if (now >= nextFlush) { Flush(); nextFlush = now.AddMinutes(10); }
                    UpdateSnapshot();
                }
                catch (Exception e) { Log.Write("loop: " + e.GetType().Name + " " + e.Message); }

                // Chờ tới lần đo sau, nhưng làm ngay việc giao diện gửi sang
                var until = DateTimeOffset.Now + interval;
                while (!stop.IsCancellationRequested)
                {
                    var wait = until - DateTimeOffset.Now;
                    if (wait <= TimeSpan.Zero) break;
                    try
                    {
                        if (inbox.TryTake(out var a, (int)Math.Ceiling(wait.TotalMilliseconds), stop.Token))
                        {
                            try { a(); UpdateSnapshot(); } catch (Exception e) { Log.Write("action: " + e.GetType().Name); }
                        }
                    }
                    catch (OperationCanceledException) { break; }
                }
            }
        }
        finally
        {
            Save();
            sensors.Dispose();
        }
    }

    void SlowChecks()
    {
        disks = SystemChecks.DiskHealth();
        drive = SystemChecks.SystemDrive();
        shutdowns = SystemChecks.UnexpectedShutdowns();
        blueScreens = SystemChecks.BlueScreens();
        diskErrors = SystemChecks.DiskErrors();
        hardwareErrors = SystemChecks.HardwareErrors();
        lastBoot = SystemChecks.LastBoot();
    }

    /// <summary>Danh sách sự kiện Event Log của luật, để nhớ lần mới nhất đã báo</summary>
    IReadOnlyList<DateTimeOffset>? EventsFor(string? rule) => rule switch
    {
        Rules.BlueScreenRule => blueScreens,
        Rules.DiskErrorsRule => diskErrors,
        Rules.HardwareErrorsRule => hardwareErrors,
        _ => null,
    };

    DateTimeOffset? AlertedUpTo(string rule) => state.Health.EventAlertedUpTo.TryGetValue(rule, out var t) ? t : null;

    List<Finding> Evaluate(DateTimeOffset now)
    {
        var today = LocalDay.Of(now);
        string? lastDust = null;
        state.Plan?.Completed.TryGetValue(MaintenanceTask.Dust, out lastDust);
        var fanStopped = tracker.FanStoppedPending;
        tracker.FanStoppedPending = false;
        var results = new Dictionary<string, RuleResult>
        {
            [Rules.CpuHotRule] = Rules.CpuHot(tracker.CpuTempSeen, state.Health.CpuHotDays, today, lastDust),
            [Rules.GpuHotRule] = Rules.GpuHot(tracker.GpuTempSeen, state.Health.GpuHotDays, today),
            [Rules.FanStoppedRule] = Rules.FanStopped(tracker.FanSeen && tracker.CpuTempSeen, fanStopped, tracker.Last?.CpuTempC),
            [Rules.DiskFailingRule] = Rules.DiskFailing(disks),
            [Rules.DriveLowRule] = Rules.SystemDriveLow(drive.Free, drive.Total),
            [Rules.ShutdownRule] = Rules.UnexpectedShutdowns(shutdowns, now, state.Health.ShutdownAlertedUpTo),
            [Rules.RamFullRule] = Rules.RamFull(state.Health.RamDays, today),
            [Rules.BlueScreenRule] = Rules.BlueScreens(blueScreens, now, AlertedUpTo(Rules.BlueScreenRule)),
            [Rules.DiskErrorsRule] = Rules.DiskErrors(diskErrors, now, AlertedUpTo(Rules.DiskErrorsRule)),
            [Rules.HardwareErrorsRule] = Rules.HardwareErrors(hardwareErrors, now, AlertedUpTo(Rules.HardwareErrorsRule)),
            [Rules.IdleBusyRule] = Rules.BusyWhileIdle(tracker.UserInputSeen, state.Health.IdleBusyDays, today),
            [Rules.NoRestartRule] = Rules.NoRestart(lastBoot, now),
        };
        if (results[Rules.FanStoppedRule].Finding is { } fan) lastFanAlert = (fan, now);
        else if (lastFanAlert is { } la && now - la.At < TimeSpan.FromHours(1) && results[Rules.FanStoppedRule].Status == RuleStatus.Ok)
            results[Rules.FanStoppedRule] = RuleResult.Alert(la.Finding with { AlreadyShown = true });
        lastRules = results;
        return results.Values.Where(r => r.Finding is not null).Select(r => r.Finding!).ToList();
    }

    void Decide(DateTimeOffset now)
    {
        var findings = Evaluate(now);
        var toasts = Planner.Decide(state, now, findings, SystemChecks.UserBusy());
        foreach (var t in toasts)
        {
            try { Toasts.Show(t); } catch (Exception e) { Log.Write("toast failed: " + e.GetType().Name); continue; }
            Log.Write("shown " + t.Key);
            if (t.Rule == Rules.ShutdownRule && shutdowns is { Count: > 0 }) state.Health.ShutdownAlertedUpTo = shutdowns.Max();
            if (t.Rule is { } r && EventsFor(r) is { Count: > 0 } ev) state.Health.EventAlertedUpTo[r] = ev.Max();
            queue.Enqueue(StudyEvents.Shown(state.Plan, t, now));
        }
        if (toasts.Count > 0) { Save(); Flush(); }
    }

    /// <summary>Link smartcarepc://plan?d=... từ trình duyệt. Sai hay quá dài thì bỏ qua, không crash.</summary>
    public void HandleLink(string link) => Post(() =>
    {
        if (!PlanLink.TryParse(link, out var plan, out var error)) { Log.Write("ignored link: " + error); return; }
        var now = DateTimeOffset.Now;
        var first = Planner.ApplyPlan(state, plan!, now);
        if (first) queue.Enqueue(StudyEvents.Linked(plan!, now));
        Save();
        Log.Write(first ? "plan linked" : "plan updated");
        try { Toasts.ShowLinked(plan!); } catch { }
        Flush();
    });

    public void HandleClick(ToastClick c) => Post(() =>
    {
        var now = DateTimeOffset.Now;
        var stub = new ToastRequest(c.Key, c.Source, Urgency.Maintenance, c.Task, c.Rule, "", "", c.Url, true, null, new Dictionary<string, object>());
        if (c.Action == "snooze")
        {
            Planner.Snooze(state, c.Key, now);
            queue.Enqueue(StudyEvents.Snoozed(state.Plan, stub, now));
        }
        else
        {
            Planner.Started(state, c.Key, now);
            queue.Enqueue(StudyEvents.Clicked(state.Plan, stub, now));
            WindowsIntegration.OpenUrl(c.Url);
        }
        Save();
        Flush();
    });

    public void Pause(TimeSpan? span) => Post(() => { state.PausedUntil = span is { } s ? DateTimeOffset.Now + s : null; Save(); });

    public void SetQuietHours(int start, int end) => Post(() =>
    {
        state.Settings.QuietStartHour = Math.Clamp(start, 0, 23);
        state.Settings.QuietEndHour = Math.Clamp(end, 0, 23);
        Save();
    });

    public string Origin => snapshot?.Plan?.Origin ?? PlanLink.DefaultOrigin;

    /// <summary>Chỉ gửi khi lịch hiện tại có study. Không thử nghiệm thì không gọi mạng.</summary>
    void Flush()
    {
        pendingEvents = queue.Pending.Count;
        if (state.Plan is not { Study: not null } plan || queue.Pending.Count == 0) return;
        sender ??= new EventSender(EventSender.CreateClient());
        var origin = plan.Origin;
        _ = queue.FlushAsync(e => sender.SendAsync(origin, e));
    }

    void Save()
    {
        try { store.Save(state); } catch (Exception e) { Log.Write("save failed: " + e.GetType().Name); }
    }

    void UpdateSnapshot()
    {
        int? Week(IReadOnlyList<DateTimeOffset>? list) => list?.Count(t => DateTimeOffset.Now - t <= TimeSpan.FromDays(7));
        var rules = new Dictionary<string, RuleResult>(lastRules);
        snapshot = new StatusSnapshot(tracker.Last, OverallStatus.Of(rules), rules, disks, drive,
            Week(shutdowns), Week(blueScreens), Week(diskErrors), Week(hardwareErrors), lastBoot, state.Plan,
            state.PausedUntil, state.Settings, Baselines.Compare(state.Health.Baseline, state.Health.LoadDays), pendingEvents);
    }

    public void Dispose()
    {
        NetworkChange.NetworkAvailabilityChanged -= OnNetwork;
        stop.Cancel();
        inbox.CompleteAdding();
        thread?.Join(TimeSpan.FromSeconds(5));
    }
}
