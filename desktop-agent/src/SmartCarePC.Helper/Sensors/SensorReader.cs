using System.Runtime.InteropServices;
using LibreHardwareMonitor.Hardware;
using SmartCarePC.Helper.Core.Health;

namespace SmartCarePC.Helper.Sensors;

/// <summary>
/// Đọc số đo. Tải CPU và RAM lấy thẳng từ Windows (không cần quyền gì). Nhiệt độ và quạt lấy qua LibreHardwareMonitorLib:
/// - Nhiệt CPU, quạt CPU cần driver PawnIO và quyền admin. App không cài driver, không xin admin, nên trên tài khoản thường
///   các giá trị này thường là null và luật tương ứng ghi "not available on this PC".
/// - Nhiệt GPU NVIDIA / AMD đi qua thư viện của hãng card, thường đọc được không cần admin.
/// Mọi lỗi của thư viện đều bị chặn ở đây: cảm biến hỏng không được làm app dừng.
/// </summary>
public sealed class SensorReader : IDisposable
{
    readonly Computer? computer;
    long lastIdle, lastKernel, lastUser;

    public string? LibraryError { get; }

    public SensorReader()
    {
        try
        {
            computer = new Computer
            {
                IsCpuEnabled = true,
                IsGpuEnabled = true,
                IsMotherboardEnabled = true,
                IsControllerEnabled = false,
                IsMemoryEnabled = false,
                IsStorageEnabled = false, // SMART qua thư viện cần admin; tình trạng ổ đọc bằng MSFT_PhysicalDisk
                IsNetworkEnabled = false,
                IsPsuEnabled = false,
                IsBatteryEnabled = false,
            };
            computer.Open();
        }
        catch (Exception e)
        {
            computer = null;
            LibraryError = e.GetType().Name;
        }
        GetSystemTimes(out lastIdle, out lastKernel, out lastUser);
    }

    public HealthSample Read(DateTimeOffset now)
    {
        double? cpuTemp = null, fan = null, gpuTemp = null, gpuLoad = null;
        if (computer is not null)
        {
            foreach (var hw in computer.Hardware)
            {
                try
                {
                    hw.Update();
                    foreach (var sub in hw.SubHardware) sub.Update();
                    switch (hw.HardwareType)
                    {
                        case HardwareType.Cpu:
                            cpuTemp = Max(cpuTemp, CpuTemperature(hw));
                            break;
                        case HardwareType.GpuNvidia or HardwareType.GpuAmd:
                            // Chỉ card rời. Nhân đồ hoạ tích hợp (GpuIntel) bỏ qua.
                            gpuTemp = Max(gpuTemp, Pick(hw, SensorType.Temperature, "GPU Core", null, 5, 125));
                            gpuLoad = Max(gpuLoad, Pick(hw, SensorType.Load, "GPU Core", null, 0, 100) / 100);
                            break;
                        case HardwareType.Motherboard:
                            fan ??= CpuFan(hw);
                            break;
                    }
                }
                catch (Exception) { /* một linh kiện lỗi thì bỏ qua linh kiện đó */ }
            }
        }
        return new HealthSample(now, CpuLoad(), cpuTemp, fan, gpuLoad, gpuTemp, RamUsed());
    }

    static double? Max(double? a, double? b) => a is null ? b : b is null ? a : Math.Max(a.Value, b.Value);

    /// <summary>Nhiệt CPU: ưu tiên cảm biến cả gói (Intel "CPU Package", AMD "Core (Tctl/Tdie)"), rồi "Core Max"</summary>
    static double? CpuTemperature(IHardware cpu)
    {
        foreach (var name in new[] { "CPU Package", "Core (Tctl/Tdie)", "Core (Tctl)", "Core Max", "Core Average" })
            if (Pick(cpu, SensorType.Temperature, name, null, 5, 125) is { } t) return t;
        return null;
    }

    /// <summary>
    /// Quạt CPU nằm ở chip Super I/O của bo mạch chủ. Tên do thư viện đặt: bo đã biết thì có chữ "CPU", không thì "Fan #1"
    /// (theo quy ước, cổng CPU_FAN thường là quạt số 1). Cần thử trên máy thật.
    /// </summary>
    static double? CpuFan(IHardware board)
    {
        foreach (var sub in board.SubHardware)
        {
            if (Pick(sub, SensorType.Fan, null, "CPU", 0, 10000) is { } named) return named;
            if (Pick(sub, SensorType.Fan, "Fan #1", null, 0, 10000) is { } first) return first;
        }
        return null;
    }

    static double? Pick(IHardware hw, SensorType type, string? exact, string? contains, double min, double max)
    {
        foreach (var s in hw.Sensors)
        {
            if (s.SensorType != type || s.Value is not { } v) continue;
            if (exact is not null && s.Name != exact) continue;
            if (contains is not null && !s.Name.Contains(contains, StringComparison.OrdinalIgnoreCase)) continue;
            // Không có driver thì thư viện hay trả 0 cho nhiệt độ: coi là không đọc được
            if (type == SensorType.Temperature && v <= 0) continue;
            if (v < min || v > max || float.IsNaN(v)) continue;
            return v;
        }
        return null;
    }

    double CpuLoad()
    {
        if (!GetSystemTimes(out var idle, out var kernel, out var user)) return 0;
        long di = idle - lastIdle, dk = kernel - lastKernel, du = user - lastUser;
        lastIdle = idle; lastKernel = kernel; lastUser = user;
        var total = dk + du; // thời gian kernel đã gồm thời gian rảnh
        return total <= 0 ? 0 : Math.Clamp(1.0 - (double)di / total, 0, 1);
    }

    static double RamUsed()
    {
        var m = new MEMORYSTATUSEX { dwLength = (uint)Marshal.SizeOf<MEMORYSTATUSEX>() };
        return GlobalMemoryStatusEx(ref m) ? m.dwMemoryLoad / 100.0 : 0;
    }

    public void Dispose()
    {
        try { computer?.Close(); } catch { }
    }

    [DllImport("kernel32.dll", SetLastError = true)]
    static extern bool GetSystemTimes(out long idle, out long kernel, out long user);

    [StructLayout(LayoutKind.Sequential)]
    struct MEMORYSTATUSEX
    {
        public uint dwLength, dwMemoryLoad;
        public ulong ullTotalPhys, ullAvailPhys, ullTotalPageFile, ullAvailPageFile, ullTotalVirtual, ullAvailVirtual, ullAvailExtendedVirtual;
    }

    [DllImport("kernel32.dll", SetLastError = true)]
    static extern bool GlobalMemoryStatusEx(ref MEMORYSTATUSEX m);
}
