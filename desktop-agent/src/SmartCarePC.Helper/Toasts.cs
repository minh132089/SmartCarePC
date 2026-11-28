using Microsoft.Toolkit.Uwp.Notifications;
using SmartCarePC.Helper.Core;
using SmartCarePC.Helper.Core.Health;

namespace SmartCarePC.Helper;

/// <summary>Hành động khi người dùng bấm vào thông báo</summary>
public sealed record ToastClick(string Action, string Key, ToastSource Source, MaintenanceTask? Task, string? Rule, string? Url);

/// <summary>
/// Hiện thông báo Windows (toast) bằng Microsoft.Toolkit.Uwp.Notifications. Mỗi thông báo một ý, một việc, có nút bấm.
/// Bấm vào thân thông báo hoặc nút Start guide: mở web vào thẳng bài hướng dẫn.
/// </summary>
public static class Toasts
{
    public static void Show(ToastRequest r)
    {
        var b = new ToastContentBuilder()
            .AddArgument("action", "open")
            .AddArgument("key", r.Key)
            .AddArgument("src", r.Source.ToString())
            .AddText(r.Title)
            .AddText(r.Body);
        if (r.Task is { } task) b.AddArgument("task", task.Id());
        if (r.Rule is not null) b.AddArgument("rule", r.Rule);
        if (r.OpenUrl is not null) b.AddArgument("url", r.OpenUrl);
        if (r.Urgency == Urgency.Urgent) b.SetToastScenario(ToastScenario.Reminder); // ở lại trên màn hình tới khi người dùng bấm

        var main = r.Source switch
        {
            _ when r.Task is not null => "Start guide",
            _ when r.Rule == Rules.RamFullRule => "See upgrade options",
            _ => null,
        };
        if (main is not null && r.OpenUrl is not null)
            b.AddButton(Button(main, "start", r));
        if (r.Rule == Rules.DriveLowRule)
            b.AddButton(new ToastButton().SetContent("Free up space").SetProtocolActivation(new Uri("ms-settings:storagesense")));
        if (r.CanSnooze)
            b.AddButton(Button("Remind me in 1 week", "snooze", r).SetBackgroundActivation());
        if (r.Rule == Rules.DiskFailingRule)
            b.AddButton(new ToastButton().SetContent("OK").SetDismissActivation());

        b.Show(t => { t.Tag = Tag(r.Key); t.Group = "smartcarepc"; });
    }

    static ToastButton Button(string text, string action, ToastRequest r)
    {
        var button = new ToastButton().SetContent(text).AddArgument("action", action).AddArgument("key", r.Key).AddArgument("src", r.Source.ToString());
        if (r.Task is { } task) button.AddArgument("task", task.Id());
        if (r.Rule is not null) button.AddArgument("rule", r.Rule);
        if (r.OpenUrl is not null) button.AddArgument("url", r.OpenUrl);
        return button;
    }

    static string Tag(string key) => key.Length <= 64 ? key : key[..64];

    /// <summary>Thông báo xác nhận ngay sau khi nhận lịch từ web (người dùng vừa bấm, nên không tính vào giới hạn mỗi ngày)</summary>
    public static void ShowLinked(MaintenancePlan plan)
    {
        var next = plan.Dates.OrderBy(kv => kv.Value, StringComparer.Ordinal).First();
        var when = LocalDay.Parse(next.Value).ToString("d MMMM yyyy", System.Globalization.CultureInfo.InvariantCulture);
        var what = next.Key switch { MaintenanceTask.Dust => "clean your PC", MaintenanceTask.Paste => "replace the thermal paste", _ => "do a quick PC check" };
        new ToastContentBuilder()
            .AddArgument("action", "none")
            .AddText("Your reminders are on")
            .AddText($"We will remind you to {what} on {when}. You can close the website now.")
            .Show(t => { t.Tag = "linked"; t.Group = "smartcarepc"; });
    }

    /// <summary>Đọc lại tham số khi bấm. Tham số do app khác giả thì bị bỏ (khoá lạ, url ngoài danh sách).</summary>
    public static ToastClick? Parse(string argument)
    {
        try
        {
            var a = ToastArguments.Parse(argument);
            if (!a.TryGetValue("action", out var action) || action is not ("open" or "start" or "snooze")) return null;
            if (!a.TryGetValue("key", out var key) || key.Length > 64) return null;
            if (!a.TryGetValue("src", out var src) || !Enum.TryParse<ToastSource>(src, out var source)) return null;
            MaintenanceTask? task = a.TryGetValue("task", out var t) && Tasks.TryParse(t, out var mt) ? mt : null;
            a.TryGetValue("rule", out var rule);
            a.TryGetValue("url", out var url);
            if (url is not null && !WindowsIntegration.IsSafeUrl(url)) url = null;
            return new ToastClick(action, key, source, task, rule is { Length: <= 40 } ? rule : null, url);
        }
        catch (Exception) { return null; }
    }
}
