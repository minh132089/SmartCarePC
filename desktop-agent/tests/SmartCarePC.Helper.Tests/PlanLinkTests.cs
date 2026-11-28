using SmartCarePC.Helper.Core;
using Xunit;

namespace SmartCarePC.Helper.Tests;

public class PlanLinkTests
{
    const string Good = """{"v":1,"pc":{"machine":"mini_sff","cooler":"lowprofile","hasGpu":false},"dates":{"dust":"2026-12-05","paste":"2029-05-28","check":"2026-12-28"},"completed":{"dust":"2026-06-05"},"origin":"https://smartcarepc.pages.dev"}""";

    [Fact]
    public void Reads_a_valid_plan()
    {
        Assert.True(PlanLink.TryParse(PlanLink.Build(Good), out var p, out var err), err);
        Assert.Equal("mini_sff", p!.Pc.Machine);
        Assert.Equal("lowprofile", p.Pc.Cooler);
        Assert.False(p.Pc.HasGpu);
        Assert.Equal("2026-12-05", p.Dates[MaintenanceTask.Dust]);
        Assert.Equal("2026-06-05", p.Completed[MaintenanceTask.Dust]);
        Assert.Null(p.Study);
        Assert.Equal("https://smartcarepc.pages.dev", p.Origin);
    }

    [Fact]
    public void Reads_study_info_and_localhost_origin()
    {
        var json = Good.Replace("\"origin\":\"https://smartcarepc.pages.dev\"", "\"origin\":\"http://localhost:5173\",\"study\":{\"code\":\"P07\",\"phase\":\"after\"}");
        Assert.True(PlanLink.TryParse(PlanLink.Build(json), out var p, out var err), err);
        Assert.Equal(new StudyInfo("P07", "after"), p!.Study);
        Assert.Equal("http://localhost:5173", p.Origin);
    }

    [Fact]
    public void Accepts_trailing_slash_and_upper_case_scheme_from_browsers()
    {
        var link = PlanLink.Build(Good).Replace("smartcarepc://plan?", "SmartCarePC://plan/?");
        Assert.True(PlanLink.TryParse(link, out _, out var err), err);
    }

    public static IEnumerable<object[]> BadJson() => new[]
    {
        Good.Replace("https://smartcarepc.pages.dev", "https://evil.example"),
        Good.Replace("https://smartcarepc.pages.dev", "https://smartcarepc.pages.dev.evil.example"),
        Good.Replace("https://smartcarepc.pages.dev", "http://smartcarepc.pages.dev"),
        Good.Replace("\"mini_sff\"", "\"laptop\""),
        Good.Replace("\"lowprofile\"", "\"liquid nitrogen\""),
        Good.Replace("false", "\"no\""),
        Good.Replace("2026-12-05", "2026-13-05"),
        Good.Replace("2026-12-05", "next week"),
        Good.Replace("\"dust\":\"2026-12-05\"", "\"dust\":20261205"),
        Good.Replace("\"check\":", "\"format_disk\":"),
        Good.Replace("\"v\":1", "\"v\":2"),
        Good.Replace("\"dates\":{\"dust\":\"2026-12-05\",\"paste\":\"2029-05-28\",\"check\":\"2026-12-28\"}", "\"dates\":{}"),
        Good.Replace(",\"origin\":\"https://smartcarepc.pages.dev\"", ""),
        Good.Replace("\"origin\"", "\"study\":{\"code\":\"P 07; DROP\",\"phase\":\"before\"},\"origin\""),
        Good.Replace("\"origin\"", "\"study\":{\"code\":\"P07\",\"phase\":\"later\"},\"origin\""),
        Good.Replace("\"origin\"", "\"study\":\"P07\",\"origin\""),
        """{"pc":{"machine":{"a":{"b":{"c":{"d":1}}}}}}""",
        "[1,2,3]",
        "not json at all",
        "",
    }.Select(j => new object[] { j });

    [Theory]
    [MemberData(nameof(BadJson))]
    public void Ignores_bad_plans_without_crashing(string json)
    {
        Assert.False(PlanLink.TryParse(PlanLink.Build(json), out var p, out _));
        Assert.Null(p);
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("https://smartcarepc.pages.dev/?d=abc")]
    [InlineData("smartcarepc://delete?d=abc")]
    [InlineData("smartcarepc://plan")]
    [InlineData("smartcarepc://plan?x=1")]
    [InlineData("smartcarepc://plan?d=%%%")]
    [InlineData("smartcarepc://plan?d=a")]
    [InlineData("smartcarepc://plan?d=<script>")]
    public void Ignores_bad_links(string? link) => Assert.False(PlanLink.TryParse(link, out _, out _));

    [Fact]
    public void Ignores_links_that_are_too_long()
    {
        var padded = Good.Replace("\"origin\"", $"\"pad\":\"{new string('x', 5000)}\",\"origin\"");
        Assert.False(PlanLink.TryParse(PlanLink.Build(padded), out _, out var err));
        Assert.Equal("too long", err);
        Assert.False(PlanLink.TryParse("smartcarepc://plan?d=" + new string('A', 100_000), out _, out _));
    }
}
