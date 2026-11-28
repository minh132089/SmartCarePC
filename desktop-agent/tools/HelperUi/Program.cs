using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using SmartCarePC.Helper.Core.Health;
using SmartCarePC.Helper.Ui;

namespace SmartCarePC.HelperUi;

static class Program
{
    [STAThread]
    static int Main(string[] args)
    {
        ApplicationConfiguration.Initialize();
        var root = Root();
        switch (args.FirstOrDefault())
        {
            case "assets":
                Assets(root);
                return 0;
            case "shots":
                var folder = args.Skip(1).FirstOrDefault(a => !a.StartsWith("--", StringComparison.Ordinal));
                var outDir = Path.GetFullPath(folder ?? Path.Combine(root, "artifacts", "ui-shots"));
                Directory.CreateDirectory(outDir);
                IconSheet(Path.Combine(outDir, "tray-icons.png"));
                AppSheet(Path.Combine(outDir, "app-icon.png"));
                Shots.Run(outDir, args.Contains("--toast"), args.Contains("--live"));
                Console.WriteLine("Saved to " + outDir);
                return 0;
            default:
                Console.WriteLine("Usage: HelperUi assets | shots [folder] [--toast] [--live]");
                return 1;
        }
    }

    /// <summary>Thư mục desktop-agent: đi ngược lên tới khi gặp file .sln</summary>
    static string Root()
    {
        for (var d = new DirectoryInfo(AppContext.BaseDirectory); d is not null; d = d.Parent)
            if (File.Exists(Path.Combine(d.FullName, "SmartCarePC.Helper.sln"))) return d.FullName;
        throw new DirectoryNotFoundException("desktop-agent folder not found");
    }

    static void Assets(string root)
    {
        var assets = Path.Combine(root, "src", "SmartCarePC.Helper", "Assets");
        var images = Path.Combine(root, "installer", "images");
        Directory.CreateDirectory(assets);
        Directory.CreateDirectory(images);

        var sizes = new[] { 16, 20, 24, 32, 40, 48, 64, 256 };
        var frames = sizes.Select(s => IconArt.App(s)).ToList();
        IcoWriter.Write(Path.Combine(assets, "app.ico"), frames);
        frames.ForEach(f => f.Dispose());
        Save(IconArt.App(256, fullBleed: true), Path.Combine(assets, "toast-logo.png"), ImageFormat.Png);

        // Bộ cài: ảnh ở 100% và 200%. Inno chọn ảnh hợp DPI nhất rồi co giãn.
        foreach (var scale in new[] { 1, 2 })
        {
            Save(IconArt.WizardTall(164 * scale, 314 * scale), Path.Combine(images, $"wizard-{164 * scale}.bmp"), ImageFormat.Bmp);
            Save(IconArt.WizardSmall(55 * scale), Path.Combine(images, $"wizard-small-{55 * scale}.bmp"), ImageFormat.Bmp);
        }
        Console.WriteLine("Assets written to " + assets + " and " + images);
    }

    /// <summary>Icon khay 4 mức ở các cỡ thật, phóng to 6 lần (không làm mịn) trên nền taskbar sáng và tối</summary>
    static void IconSheet(string path)
    {
        int[] sizes = [16, 20, 24, 32];
        var levels = Enum.GetValues<OverallLevel>();
        const int zoom = 6, cell = 32 * zoom + 32 + 28;
        using var sheet = new Bitmap(cell * sizes.Length, cell * levels.Length * 2, PixelFormat.Format32bppArgb);
        using var g = Graphics.FromImage(sheet);
        g.InterpolationMode = InterpolationMode.NearestNeighbor;
        g.PixelOffsetMode = PixelOffsetMode.Half;
        for (var band = 0; band < 2; band++)
        {
            using var bg = new SolidBrush(band == 0 ? Color.FromArgb(243, 243, 243) : Color.FromArgb(32, 32, 32));
            g.FillRectangle(bg, 0, band * cell * levels.Length, sheet.Width, cell * levels.Length);
            for (var li = 0; li < levels.Length; li++)
                for (var si = 0; si < sizes.Length; si++)
                {
                    using var icon = IconArt.Status(levels[li], sizes[si]);
                    var x = si * cell + 12;
                    var y = (band * levels.Length + li) * cell + 12;
                    g.DrawImage(icon, new Rectangle(x, y, sizes[si] * zoom, sizes[si] * zoom));
                    g.DrawImageUnscaled(icon, x + sizes[si] * zoom + 4, y);
                }
        }
        sheet.Save(path, ImageFormat.Png);
    }

    static void AppSheet(string path)
    {
        int[] sizes = [16, 20, 24, 32, 48, 64, 256];
        int[] zoomed = [16, 20, 24, 32];
        const int zoom = 6;
        using var sheet = new Bitmap(sizes.Sum() + 24 * sizes.Length + 260, 300 + 32 * zoom + 24, PixelFormat.Format32bppArgb);
        using var g = Graphics.FromImage(sheet);
        g.Clear(Color.FromArgb(245, 245, 247));
        var x = 12;
        foreach (var s in sizes)
        {
            using var icon = IconArt.App(s);
            g.DrawImageUnscaled(icon, x, 12);
            x += s + 24;
        }
        g.InterpolationMode = InterpolationMode.NearestNeighbor;
        g.PixelOffsetMode = PixelOffsetMode.Half;
        var zx = 12;
        foreach (var s in zoomed)
        {
            using var icon = IconArt.App(s);
            g.DrawImage(icon, new Rectangle(zx, 300, s * zoom, s * zoom));
            zx += s * zoom + 24;
        }
        g.InterpolationMode = InterpolationMode.HighQualityBicubic;
        g.PixelOffsetMode = PixelOffsetMode.HighQuality;
        using var logo = IconArt.App(256, fullBleed: true);
        using var clip = new GraphicsPath();
        clip.AddEllipse(x, 12, 256, 256);
        g.SetClip(clip);
        g.DrawImageUnscaled(logo, x, 12);
        sheet.Save(path, ImageFormat.Png);
    }

    static void Save(Bitmap bmp, string path, ImageFormat format)
    {
        using (bmp) bmp.Save(path, format);
    }
}
