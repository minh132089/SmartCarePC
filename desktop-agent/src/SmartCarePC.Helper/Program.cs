using SmartCarePC.Helper.Core;

namespace SmartCarePC.Helper;

static class Program
{
    /// <summary>
    /// Lệnh:
    ///   SmartCarePCHelper.exe                     chạy ở khay
    ///   SmartCarePCHelper.exe "smartcarepc://..." trình duyệt mở link; chuyển cho bản đang chạy nếu có
    ///   SmartCarePCHelper.exe --register          đăng ký smartcarepc:// (khi thử bản không cài)
    ///   SmartCarePCHelper.exe --uninstall         bộ cài gọi khi gỡ: xoá khoá Registry, mục khởi động, đăng ký thông báo
    /// </summary>
    [STAThread]
    static int Main(string[] args)
    {
        if (args.Contains("--uninstall")) { WindowsIntegration.Uninstall(); return 0; }
        if (args.Contains("--register")) { WindowsIntegration.RegisterProtocol(); return 0; }

        var link = args.FirstOrDefault(a => a.StartsWith(PlanLink.Scheme + ":", StringComparison.OrdinalIgnoreCase));
        using var mutex = new Mutex(true, WindowsIntegration.MutexName, out var first);
        if (!first)
        {
            if (link is not null) SingleInstance.Send(link);
            return 0;
        }

        Application.SetUnhandledExceptionMode(UnhandledExceptionMode.CatchException);
        Application.ThreadException += (_, e) => Log.Write("ui error: " + e.Exception.GetType().Name);
        ApplicationConfiguration.Initialize();
        Application.Run(new TrayApp(link));
        return 0;
    }
}
