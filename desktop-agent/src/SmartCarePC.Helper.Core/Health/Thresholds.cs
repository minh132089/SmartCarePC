namespace SmartCarePC.Helper.Core.Health;

/// <summary>
/// Ngưỡng cho các luật sức khoẻ. Đây là giá trị khởi đầu lấy từ docs/maintenance-agent-spec.md mục 5.2, đã đối chiếu nguồn bên dưới.
///
/// CPU 90 °C khi tải &gt; 80% kéo dài 2 phút:
///   - Intel: nhiệt độ tối đa (TjMax) tuỳ đời chip, thường 100–110 °C, chip tự hạ xung khi chạm mức này.
///     https://www.intel.com/content/www/us/en/support/articles/000005597/processors.html
///   - AMD Ryzen 7000 (không phải X3D) chạy tới khoảng 95 °C khi tải nặng là theo thiết kế, bản X3D giới hạn thấp hơn (7800X3D: 89 °C).
///     Theo các bài review (Guru3D, TechSpot "How hot is too hot"), chưa tìm được tài liệu gốc của AMD.
///   => 90 °C nằm dưới TjMax của Intel 10 °C, nhưng có thể báo nhầm trên Ryzen 7000. CHƯA CHỐT: cần thử trên máy thật.
///   Thực tế khi chạy bằng tài khoản thường thì không đọc được nhiệt CPU (xem README), nên luật này thường bị bỏ qua.
///
/// GPU 85 °C khi chạy nặng kéo dài 2 phút:
///   - NVIDIA ghi "Maximum GPU Temperature" trên trang thông số từng card, dòng RTX 50 khoảng 85–90 °C (RTX 5070: 85 °C).
///     Nguồn thứ cấp: igorslab.de (bảng thông số của NVIDIA). https://www.igorslab.de/en/blackwell-hotspots-and-the-endless-search-for-the-one-reliable-value/4/
///   => 85 °C kéo dài nghĩa là card đã ở sát mức tối đa của hãng.
///
/// Quạt đứng: 0 RPM trong khi CPU &gt; 70 °C (spec). Giữ 30 giây để bỏ qua lúc quạt đang tăng tốc.
/// Ổ hệ thống còn trống &lt; 10%, RAM &gt; 90% trong &gt; 50% thời gian 7 ngày, tắt máy đột ngột ≥ 2 lần / 7 ngày: theo spec.
/// </summary>
public static class Thresholds
{
    public const double CpuHotC = 90;
    public const double CpuHotLoad = 0.80;
    public const double GpuHotC = 85;
    /// <summary>GPU tải trên mức này coi là đang chơi game / chạy nặng</summary>
    public const double GpuBusyLoad = 0.70;
    public static readonly TimeSpan HotHold = TimeSpan.FromMinutes(2);
    public const int HotDaysNeeded = 2;
    public const int HotWindowDays = 7;
    /// <summary>Đã vệ sinh trong 30 ngày qua mà vẫn nóng thì dẫn tới paste (giống cleanedButStillHot của web)</summary>
    public const int RecentCleanDays = 30;

    public const double FanStopCpuC = 70;
    public static readonly TimeSpan FanStopHold = TimeSpan.FromSeconds(30);

    public const double SystemDriveFreeFraction = 0.10;

    public const int ShutdownsNeeded = 2;
    public const int ShutdownWindowDays = 7;

    public const double RamHighFraction = 0.90;
    public const double RamHighTimeFraction = 0.50;
    public const int RamWindowDays = 7;
    /// <summary>Cần ít nhất ngần này giờ máy chạy trong 7 ngày mới kết luận, tránh kết luận từ vài phút đo</summary>
    public const double RamMinHours = 10;

    /// <summary>Lấy mẫu: 5 giây khi chạy nặng, 60 giây khi rảnh (spec 5.2)</summary>
    public static readonly TimeSpan BusyInterval = TimeSpan.FromSeconds(5);
    public static readonly TimeSpan IdleInterval = TimeSpan.FromSeconds(60);
    public const double BusyCpuLoad = 0.50;
    public const double BusyGpuLoad = 0.40;
}
