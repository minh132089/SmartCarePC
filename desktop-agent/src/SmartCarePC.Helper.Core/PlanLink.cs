using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;

namespace SmartCarePC.Helper.Core;

/// <summary>
/// Đọc link smartcarepc://plan?d=&lt;base64url của JSON&gt;. Ai cũng tạo được link này, nên coi là dữ liệu không tin cậy:
/// giới hạn độ dài, đúng kiểu, chỉ nhận origin trong danh sách. Sai một chỗ là bỏ cả link, không nhận một nửa.
/// </summary>
public static partial class PlanLink
{
    public const string Scheme = "smartcarepc";
    public const int MaxLinkLength = 4096;
    public const int MaxJsonBytes = 2048;

    /// <summary>Chỉ hai địa chỉ web này được mở lại từ thông báo</summary>
    public static readonly string[] AllowedOrigins = ["https://smartcarepc.pages.dev", "http://localhost:5173"];
    public const string DefaultOrigin = "https://smartcarepc.pages.dev";

    /// <summary>FAMILY_IDS trong src/data/families.ts + 'gaming'</summary>
    public static readonly string[] Machines = ["standard_tower", "compact_tower", "showcase_dual_chamber", "mini_sff", "full_tower", "gaming"];
    public static readonly string[] Coolers = ["air", "lowprofile", "aio", "unknown"];

    [GeneratedRegex("^[A-Za-z0-9_-]{1,20}$")] private static partial Regex StudyCode(); // CODE_PATTERN trong src/ui/study/study.ts

    public static bool TryParse(string? link, out MaintenancePlan? plan, out string error)
    {
        plan = null;
        try { return Parse(link, out plan, out error); }
        catch (Exception) { error = "invalid"; plan = null; return false; }
    }

    private static bool Parse(string? link, out MaintenancePlan? plan, out string error)
    {
        plan = null;
        if (string.IsNullOrEmpty(link)) { error = "empty"; return false; }
        if (link.Length > MaxLinkLength) { error = "too long"; return false; }
        var prefix = Scheme + "://";
        if (!link.StartsWith(prefix, StringComparison.OrdinalIgnoreCase)) { error = "wrong scheme"; return false; }
        var rest = link[prefix.Length..];
        var q = rest.IndexOf('?');
        if (q < 0) { error = "no query"; return false; }
        var host = rest[..q].TrimEnd('/');
        if (!host.Equals("plan", StringComparison.OrdinalIgnoreCase)) { error = "unknown action"; return false; }

        string? d = null;
        foreach (var part in rest[(q + 1)..].Split('&', StringSplitOptions.RemoveEmptyEntries))
        {
            var eq = part.IndexOf('=');
            if (eq > 0 && part[..eq] == "d") d = Uri.UnescapeDataString(part[(eq + 1)..]);
        }
        if (string.IsNullOrEmpty(d)) { error = "no data"; return false; }

        var bytes = FromBase64Url(d);
        if (bytes is null) { error = "bad base64"; return false; }
        if (bytes.Length > MaxJsonBytes) { error = "too long"; return false; }

        using var doc = JsonDocument.Parse(bytes, new JsonDocumentOptions { MaxDepth = 4 });
        var root = doc.RootElement;
        if (root.ValueKind != JsonValueKind.Object) { error = "not an object"; return false; }

        if (root.TryGetProperty("v", out var v) && (v.ValueKind != JsonValueKind.Number || v.GetInt32() != 1)) { error = "unknown version"; return false; }

        if (!root.TryGetProperty("pc", out var pcEl) || pcEl.ValueKind != JsonValueKind.Object) { error = "no pc"; return false; }
        var machine = Str(pcEl, "machine");
        var cooler = Str(pcEl, "cooler");
        if (machine is null || !Machines.Contains(machine)) { error = "bad machine"; return false; }
        if (cooler is null || !Coolers.Contains(cooler)) { error = "bad cooler"; return false; }
        if (!pcEl.TryGetProperty("hasGpu", out var gpuEl) || gpuEl.ValueKind is not (JsonValueKind.True or JsonValueKind.False)) { error = "bad hasGpu"; return false; }

        if (!TryDays(root, "dates", out var dates)) { error = "bad dates"; return false; }
        if (!TryDays(root, "completed", out var completed)) { error = "bad completed"; return false; }
        if (dates.Count == 0) { error = "no dates"; return false; }

        StudyInfo? study = null;
        if (root.TryGetProperty("study", out var st) && st.ValueKind != JsonValueKind.Null)
        {
            if (st.ValueKind != JsonValueKind.Object) { error = "bad study"; return false; }
            var code = Str(st, "code"); var phase = Str(st, "phase");
            if (code is null || !StudyCode().IsMatch(code)) { error = "bad study code"; return false; }
            if (phase is not ("before" or "after")) { error = "bad phase"; return false; }
            study = new StudyInfo(code, phase);
        }

        var origin = Str(root, "origin");
        if (origin is null || !AllowedOrigins.Contains(origin)) { error = "origin not allowed"; return false; }

        plan = new MaintenancePlan
        {
            Pc = new PcInfo(machine, cooler, gpuEl.GetBoolean()),
            Dates = dates,
            Completed = completed,
            Study = study,
            Origin = origin,
        };
        error = "";
        return true;
    }

    private static string? Str(JsonElement obj, string name) =>
        obj.TryGetProperty(name, out var e) && e.ValueKind == JsonValueKind.String ? e.GetString() : null;

    private static bool TryDays(JsonElement root, string name, out Dictionary<MaintenanceTask, string> result)
    {
        result = new();
        if (!root.TryGetProperty(name, out var el) || el.ValueKind == JsonValueKind.Null) return true;
        if (el.ValueKind != JsonValueKind.Object) return false;
        foreach (var p in el.EnumerateObject())
        {
            if (!Tasks.TryParse(p.Name, out var t)) return false;
            if (p.Value.ValueKind != JsonValueKind.String || !LocalDay.TryParse(p.Value.GetString(), out _)) return false;
            result[t] = p.Value.GetString()!;
        }
        return true;
    }

    private static byte[]? FromBase64Url(string s)
    {
        if (s.Length > MaxLinkLength) return null;
        foreach (var c in s)
            if (!(char.IsAsciiLetterOrDigit(c) || c is '-' or '_' or '=')) return null;
        var b = s.TrimEnd('=').Replace('-', '+').Replace('_', '/');
        b += (b.Length % 4) switch { 2 => "==", 3 => "=", 0 => "", _ => "!" };
        try { return Convert.FromBase64String(b); } catch (FormatException) { return null; }
    }

    /// <summary>Tạo link (dùng trong test và để viết tài liệu cho phía web)</summary>
    public static string Build(string json) =>
        $"{Scheme}://plan?d=" + Convert.ToBase64String(Encoding.UTF8.GetBytes(json)).TrimEnd('=').Replace('+', '-').Replace('/', '_');
}
