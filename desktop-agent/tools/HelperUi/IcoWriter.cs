using System.Drawing.Imaging;

namespace SmartCarePC.HelperUi;

/// <summary>
/// Ghi file .ico nhiều cỡ. Cỡ nhỏ ghi dạng bitmap 32 bit có kênh trong suốt (Windows nào cũng đọc được),
/// cỡ 256 ghi dạng PNG cho nhẹ, đúng như Windows Vista trở đi hỗ trợ.
/// </summary>
static class IcoWriter
{
    public static void Write(string path, IReadOnlyList<Bitmap> images)
    {
        var blobs = images.Select(img => img.Width >= 256 ? Png(img) : Dib(img)).ToList();
        using var f = File.Create(path);
        using var w = new BinaryWriter(f);
        w.Write((ushort)0); w.Write((ushort)1); w.Write((ushort)images.Count);
        var offset = 6 + 16 * images.Count;
        for (var i = 0; i < images.Count; i++)
        {
            var img = images[i];
            w.Write((byte)(img.Width >= 256 ? 0 : img.Width));
            w.Write((byte)(img.Height >= 256 ? 0 : img.Height));
            w.Write((byte)0); w.Write((byte)0);
            w.Write((ushort)1); w.Write((ushort)32);
            w.Write(blobs[i].Length); w.Write(offset);
            offset += blobs[i].Length;
        }
        foreach (var b in blobs) w.Write(b);
    }

    static byte[] Png(Bitmap img)
    {
        using var ms = new MemoryStream();
        img.Save(ms, ImageFormat.Png);
        return ms.ToArray();
    }

    /// <summary>BITMAPINFOHEADER + điểm ảnh BGRA từ dưới lên + mặt nạ AND 1 bit (để trống vì đã có kênh alpha)</summary>
    static byte[] Dib(Bitmap img)
    {
        int width = img.Width, height = img.Height;
        var maskStride = ((width + 31) / 32) * 4;
        using var ms = new MemoryStream();
        using var w = new BinaryWriter(ms);
        w.Write(40); w.Write(width); w.Write(height * 2);
        w.Write((ushort)1); w.Write((ushort)32); w.Write(0);
        w.Write(width * height * 4 + maskStride * height);
        w.Write(0); w.Write(0); w.Write(0); w.Write(0);
        for (var y = height - 1; y >= 0; y--)
            for (var x = 0; x < width; x++)
            {
                var c = img.GetPixel(x, y);
                w.Write(c.B); w.Write(c.G); w.Write(c.R); w.Write(c.A);
            }
        w.Write(new byte[maskStride * height]);
        return ms.ToArray();
    }
}
