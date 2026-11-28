using System.Drawing.Imaging;
using System.Runtime.InteropServices;
using Microsoft.Toolkit.Uwp.Notifications;
using SmartCarePC.Helper;
using SmartCarePC.Helper.Core;
using SmartCarePC.Helper.Core.Display;
using SmartCarePC.Helper.Core.Health;
using SmartCarePC.Helper.Ui;

namespace SmartCarePC.HelperUi;

/// <summary>
/// Chụp ảnh giao diện thật (cửa sổ do Windows vẽ, gồm cả thanh tiêu đề) ở chế độ sáng và tối, với dữ liệu giả cho 4 mức.
/// --live: thêm một ảnh với số đo thật của máy này. --toast: hiện một thông báo mẫu rồi chụp góc màn hình.
/// Ảnh chỉ ở trên máy, không gửi đi đâu.
/// </summary>
static class Shots
{
    public static void Run(string outDir, bool toast, bool live)
    {
        using var backdrop = new Form { FormBorderStyle = FormBorderStyle.None, StartPosition = FormStartPosition.Manual, Bounds = new Rectangle(40, 30, 1100, 960), ShowInTaskbar = false };
        try
        {
            foreach (var theme in new[] { "light", "dark", "contrast" })
            {
                Theme.Override = theme;
                Theme.Refresh();
                backdrop.BackColor = theme == "dark" ? Color.FromArgb(32, 32, 32) : Color.FromArgb(226, 226, 230);
                backdrop.Show();
                // Màu hệ thống: chỉ cần một mức có đủ loại nhãn để soát
                foreach (var (name, snap) in Fakes.All().Where(f => theme != "contrast" || f.Name == "needs-attention"))
                    Status(Path.Combine(outDir, $"status-{name}-{theme}"), () => snap);
                Menu(Path.Combine(outDir, $"menu-{theme}.png"), backdrop, Fakes.NeedsAttention());
            }
            Theme.Override = null;
            Theme.Refresh();
            if (live) Live(outDir);
            if (toast) Toast(outDir);
        }
        finally
        {
            backdrop.Close();
            if (toast || live) { try { ToastNotificationManagerCompat.Uninstall(); } catch { } }
        }
    }

    static void Status(string basePath, Func<StatusSnapshot?> snap)
    {
        using var form = new StatusForm(snap, (_, _) => { }) { StartPosition = FormStartPosition.Manual, Location = new Point(90, 60), TopMost = true };
        form.Show();
        Pump(800);
        Window(form.Handle, basePath + ".png");
        FullPage(form, basePath + "-full.png");
        var stack = form.Controls.OfType<ScrollStack>().First();
        if (stack.AutoScrollMinSize.Height > stack.ClientSize.Height)
        {
            stack.AutoScrollPosition = new Point(0, stack.AutoScrollMinSize.Height);
            Pump(300);
            Window(form.Handle, basePath + "-bottom.png");
        }
        form.Close();
        Pump(100);
    }

    /// <summary>Menu khay với dòng Pause reminders đang được chọn và menu con mở</summary>
    static void Menu(string path, Form backdrop, StatusSnapshot snap)
    {
        backdrop.Activate();
        using var menu = TrayMenu.Build(new TrayActions(() => snap, () => { }, _ => { }, () => { }, () => true, _ => { }, () => { }));
        menu.Show(backdrop, new Point(160, 140));
        Pump(300);
        var pause = menu.Items.OfType<ToolStripMenuItem>().First(i => i.Text == "Pause reminders");
        pause.Select();
        pause.ShowDropDown();
        Pump(500);
        var area = Rectangle.Union(menu.Bounds, pause.DropDown.Visible ? pause.DropDown.Bounds : menu.Bounds);
        area.Inflate(24, 24);
        Screen(area, path);
        menu.Close();
        Pump(100);
    }

    /// <summary>Số đo thật của máy này: chạy vòng đo của app khoảng 15 giây rồi mở cửa sổ Status theo chế độ màu của Windows</summary>
    static void Live(string outDir)
    {
        using var host = new AgentHost();
        host.Start();
        var until = DateTime.UtcNow.AddSeconds(20);
        while (DateTime.UtcNow < until && host.Snapshot is not { Rules.Count: > 0 }) Pump(250);
        Status(Path.Combine(outDir, "status-live"), () => host.Snapshot);
    }

    static void Toast(string outDir)
    {
        var now = DateTimeOffset.Now;
        var today = LocalDay.Of(now);
        var f = Rules.CpuHot(true, [today, LocalDay.AddDays(today, -2)], today, null).Finding!;
        Toasts.Show(new ToastRequest("health:" + f.Rule, ToastSource.Health, f.Urgency, f.GuideTask, f.Rule, f.Title, f.Body,
            "https://smartcarepc.pages.dev/?baotri=dust", true, null, f.Data));
        Pump(3000);
        Console.WriteLine("Toasts in Action Center: " + ToastNotificationManagerCompat.History.GetHistory().Count);
        var work = System.Windows.Forms.Screen.PrimaryScreen!.WorkingArea;
        Screen(new Rectangle(work.Right - 430, work.Bottom - 330, 430, 330), Path.Combine(outDir, "toast.png"));
        ToastNotificationManagerCompat.History.Clear();
    }

    /// <summary>Toàn bộ nội dung đã cuộn, ghép từng thẻ: để xem cả trang mà không phải cuộn</summary>
    static void FullPage(StatusForm form, string path)
    {
        var stack = form.Controls.OfType<ScrollStack>().First();
        var height = Math.Max(stack.AutoScrollMinSize.Height, stack.ClientSize.Height);
        using var page = new Bitmap(stack.ClientSize.Width, height);
        using var g = Graphics.FromImage(page);
        g.Clear(stack.BackColor);
        foreach (Control c in stack.Controls)
        {
            if (!c.Visible || c.Width <= 0 || c.Height <= 0) continue;
            using var part = new Bitmap(c.Width, c.Height);
            c.DrawToBitmap(part, new Rectangle(0, 0, c.Width, c.Height));
            g.DrawImageUnscaled(part, c.Left, c.Top - stack.AutoScrollPosition.Y);
        }
        page.Save(path, ImageFormat.Png);
    }

    static void Window(IntPtr hwnd, string path)
    {
        if (DwmGetWindowAttribute(hwnd, 9, out var r, Marshal.SizeOf<Rect>()) != 0) return;
        Screen(Rectangle.FromLTRB(r.Left, r.Top, r.Right, r.Bottom), path);
    }

    static void Screen(Rectangle area, string path)
    {
        using var bmp = new Bitmap(area.Width, area.Height);
        using (var g = Graphics.FromImage(bmp)) g.CopyFromScreen(area.Location, Point.Empty, area.Size);
        bmp.Save(path, ImageFormat.Png);
    }

    static void Pump(int ms)
    {
        var until = DateTime.UtcNow.AddMilliseconds(ms);
        while (DateTime.UtcNow < until) { Application.DoEvents(); Thread.Sleep(15); }
    }

    [StructLayout(LayoutKind.Sequential)]
    struct Rect { public int Left, Top, Right, Bottom; }

    [DllImport("dwmapi.dll")]
    static extern int DwmGetWindowAttribute(IntPtr hwnd, int attribute, out Rect value, int size);
}
