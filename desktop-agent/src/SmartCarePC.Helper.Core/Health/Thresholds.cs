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

    // ---- Các luật đọc Event Log System (tài khoản thường đọc được, giống luật tắt máy đột ngột) ----

    /// <summary>
    /// Màn hình xanh: nguồn BugCheck, mã 1001, từ 1 lần trong 7 ngày.
    /// Microsoft ghi rõ sau một lỗi dừng máy Windows ghi sự kiện 1001, Level Error, Source BugCheck vào System log:
    /// https://learn.microsoft.com/en-us/troubleshoot/windows-server/performance/troubleshoot-stop-errors-best-practices-dump-configuration-recommendations
    /// 1 lần là đủ để nhắc vì người dùng có thể mất bài đang làm. Ngưỡng theo yêu cầu đề bài, CHƯA CHỐT: chưa thử trên máy thật.
    /// </summary>
    public const int BlueScreensNeeded = 1;
    public const int BlueScreenWindowDays = 7;

    /// <summary>
    /// Lỗi đọc / ghi ổ đĩa: nguồn "disk", mã 7 (khối hỏng), 51 (lỗi khi ghi trang bộ nhớ ra ổ), 153 (lệnh đọc / ghi phải thử lại).
    /// Nguồn: https://learn.microsoft.com/en-us/troubleshoot/windows-server/backup-and-storage/troubleshoot-data-corruption-and-disk-errors
    /// và https://learn.microsoft.com/en-us/archive/blogs/ntdebugging/interpreting-event-153-errors
    /// Mã 153 đôi khi do cáp, USB hoặc chế độ tiết kiệm điện chứ không phải ổ sắp hỏng, nên cần 3 lần / 7 ngày.
    /// CHƯA CHỐT: chưa tìm được tài liệu Microsoft nói riêng về mã 7 và 51 cho máy cá nhân, và ổ USB lỏng cũng bị đếm.
    /// </summary>
    public const int DiskErrorsNeeded = 3;
    public const int DiskErrorWindowDays = 7;

    /// <summary>
    /// Lỗi phần cứng: nguồn Microsoft-Windows-WHEA-Logger, mã 17 (PCI Express, đã tự sửa), 18 (lỗi nặng), 19 (CPU, đã tự sửa), 47 (RAM, đã tự sửa).
    /// Chưa tìm được trang tham khảo chính thức của Microsoft liệt kê từng mã. Nguồn thứ cấp: Microsoft Q&amp;A, diễn đàn Intel / HPE / Dell.
    /// CHƯA CHỐT: trên vài bo mạch chủ mã 17 xuất hiện liên tục mà máy vẫn chạy bình thường (theo diễn đàn Dell, Intel),
    /// nên 1 lần / 7 ngày có thể báo quá nhiều. Luật chỉ báo lại khi có sự kiện mới, nhưng vẫn cần thử trên máy thật.
    /// </summary>
    public const int HardwareErrorsNeeded = 1;
    public const int HardwareErrorWindowDays = 7;

    /// <summary>
    /// CPU bận khi không ai dùng máy: tải trung bình &gt; 60% suốt 30 phút không có thao tác chuột / phím (GetLastInputInfo).
    /// Cần 2 ngày trong 7 ngày vì Windows tự chạy bảo trì (quét virus, cập nhật, lập chỉ mục) đúng lúc máy rảnh:
    /// https://learn.microsoft.com/en-us/windows/win32/taskschd/task-maintenence
    /// Chỉ là gợi ý nhẹ. CHƯA CHỐT: 60% / 30 phút / 2 ngày là giá trị khởi đầu, chưa có số đo thật.
    /// </summary>
    public const double IdleBusyLoad = 0.60;
    public static readonly TimeSpan IdleBusyHold = TimeSpan.FromMinutes(30);
    public const int IdleBusyDaysNeeded = 2;
    public const int IdleBusyWindowDays = 7;

    /// <summary>
    /// Không khởi động lại quá 14 ngày (Win32_OperatingSystem.LastBootUpTime). Từ Windows 8, Shut down mặc định là fast startup,
    /// gần giống ngủ đông, còn Restart mới khởi động lại thật:
    /// https://learn.microsoft.com/en-us/windows-hardware/test/weg/delivering-a-great-startup-and-shutdown-experience
    /// Vì vậy người tắt máy mỗi tối vẫn có thể "chưa khởi động lại" nhiều tuần. Gợi ý nhẹ.
    /// CHƯA CHỐT: 14 ngày theo đề bài, chưa có nguồn chính thức cho con số này.
    /// </summary>
    public const int NoRestartDays = 14;

    /// <summary>Lấy mẫu: 5 giây khi chạy nặng, 60 giây khi rảnh (spec 5.2)</summary>
    public static readonly TimeSpan BusyInterval = TimeSpan.FromSeconds(5);
    public static readonly TimeSpan IdleInterval = TimeSpan.FromSeconds(60);
    public const double BusyCpuLoad = 0.50;
    public const double BusyGpuLoad = 0.40;
}
