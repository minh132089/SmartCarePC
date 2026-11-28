using System.Globalization;

namespace SmartCarePC.Helper.Core;

/// <summary>
/// Ngày địa phương dạng "yyyy-MM-dd", giống hàm day / addDays / addMonths trong src/data/maintenanceSchedule.ts.
/// So sánh ngày bằng so sánh chuỗi, giống web (dates[task] &lt;= today).
/// </summary>
public static class LocalDay
{
    public static string Of(DateTime local) => local.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);
    public static string Of(DateOnly d) => d.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);
    public static string Of(DateTimeOffset local) => Of(local.DateTime);

    /// <summary>Chỉ nhận đúng dạng yyyy-MM-dd, ngày có thật, năm 2000–2100. Dữ liệu từ link là dữ liệu không tin cậy.</summary>
    public static bool TryParse(string? s, out DateOnly d)
    {
        d = default;
        if (s is null || s.Length != 10) return false;
        if (!DateOnly.TryParseExact(s, "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.None, out d)) return false;
        return d.Year is >= 2000 and <= 2100;
    }

    public static DateOnly Parse(string s) =>
        TryParse(s, out var d) ? d : throw new FormatException($"Not a day: {s}");

    public static string AddDays(string day, int n) => Of(Parse(day).AddDays(n));

    /// <summary>
    /// Web: về ngày 1, cộng tháng, rồi lấy min(ngày gốc, số ngày của tháng mới). DateOnly.AddMonths cũng kẹp về ngày cuối tháng,
    /// nên ra cùng kết quả. Test so với kết quả chạy thật của web ở tests/.../fixtures/web-dates.json.
    /// </summary>
    public static string AddMonths(string day, int n) => Of(Parse(day).AddMonths(n));

    public static int DaysBetween(string from, string to) => Parse(to).DayNumber - Parse(from).DayNumber;

    /// <summary>Số tháng tròn đã qua (để viết câu "It has been 6 months since the last clean.")</summary>
    public static int WholeMonthsBetween(string from, string to)
    {
        var a = Parse(from); var b = Parse(to);
        var months = (b.Year - a.Year) * 12 + b.Month - a.Month;
        if (b.Day < a.Day && b.Day != DateTime.DaysInMonth(b.Year, b.Month)) months--;
        return Math.Max(0, months);
    }
}
