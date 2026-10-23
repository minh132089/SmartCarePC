package com.smartcarepc.backend.service.price;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class PriceEngineTests {

    private static final Instant AT = Instant.parse("2026-10-20T10:00:00Z");

    @Test
    void docNguonDuPhongHtcKhongLayGiaGachVaHangLienQuan() {
        var offers = ShopSearch.HTCGEARS.parse("""
                <div class="listProduct-filter"><div class="product-loop">
                <h3><a href="/products/vga-card-man-hinh-5">VGA ASUS Dual RX 6600 V3 8GB</a></h3>
                <div class="proloop--price"><span class="price">5,450,000₫</span><span class="price-del">5,950,000₫</span></div>
                </div></div><div class="product-loop"><h3><a href="/products/other">VGA khác</a></h3>
                <div class="proloop--price"><span class="price">1,000,000₫</span></div></div>
                """, AT);
        assertThat(offers).hasSize(1);
        assertThat(offers.getFirst().priceVnd()).isEqualTo(5_450_000L);
        assertThat(offers.getFirst().url()).isEqualTo("https://htcgears.com/products/vga-card-man-hinh-5");
        assertThat(ShopSearch.HTCGEARS.url("RX 6600")).isEqualTo("https://htcgears.com/collections/vga");
        assertThat(ShopSearch.HTCGEARS.supports("VGA RX 6600")).isTrue();
        assertThat(ShopSearch.HTCGEARS.supports("RAM 16GB")).isFalse();
        var page = ProductPage.parseLoose("""
                <meta property="og:title" content="VGA ASUS Dual RX 6600 V3 8GB">
                <meta property="og:price:amount" content="5450000"><button>Thêm vào giỏ</button>
                <script>var product={"price":545000000.0}</script>
                """, offers.getFirst().url(), AT);
        assertThat(page.priceVnd()).isEqualTo(5_450_000L);
        assertThat(page.inStock()).isTrue();
    }

    private static String fixture(String name) throws IOException {
        try (InputStream in = PriceEngineTests.class.getResourceAsStream("/shops/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void docTrangTimKiemGearvn() throws IOException {
        var offers = ShopSearch.GEARVN.parse(fixture("gearvn-search.html"), AT);
        assertThat(offers).hasSize(2);
        var card = offers.getFirst();
        assertThat(card.title()).isEqualTo("Card màn hình MSI GeForce RTX 4060 VENTUS 2X WHITE 8G OC");
        assertThat(card.priceVnd()).isEqualTo(8_990_000L);
        assertThat(card.oldPriceVnd()).isEqualTo(9_390_000L);
        assertThat(card.url()).isEqualTo("https://gearvn.com/products/card-man-hinh-msi-geforce-rtx-4060-ventus-2x-white-8g-oc");
        assertThat(card.shop()).isEqualTo("GEARVN");
        assertThat(card.checkedAt()).isEqualTo(AT);
    }

    @Test
    void docTrangTimKiemAnPhat() throws IOException {
        var offers = ShopSearch.ANPHAT.parse(fixture("anphat-search.html"), AT);
        assertThat(offers).singleElement().satisfies(o -> {
            assertThat(o.title()).isEqualTo("Card màn hình ASUS Dual GeForce RTX 4060 EVO OC Edition 8GB GDDR6");
            assertThat(o.priceVnd()).isEqualTo(8_190_000L);
            assertThat(o.oldPriceVnd()).isEqualTo(8_990_000L);
            assertThat(o.url()).isEqualTo("https://www.anphatpc.com.vn/vga-asus-dual-rtx4060-o8g-evo.html");
            assertThat(o.imageUrl()).isEqualTo("https://anphat.com.vn/media/product/4060.jpg");
        });
    }

    @Test
    void docTrangSanPhamQuaJsonLd() throws IOException {
        var o = ProductPage.parse(fixture("product-page.html"), "https://gearvn.com/products/asus-4060-evo", AT);
        assertThat(o.priceVnd()).isEqualTo(7_990_000L);
        assertThat(o.inStock()).isFalse();
        assertThat(o.warrantyMonths()).isEqualTo(36);
        assertThat(o.imageUrl()).isEqualTo("https://product.hstatic.net/x/asus.png");
        assertThat(o.shop()).isEqualTo("GEARVN");
        assertThat(ProductPage.parse("<html><body>Không có sản phẩm</body></html>", "https://gearvn.com/", AT)).isNull();
    }

    @Test
    void docSoTien() {
        assertThat(Prices.parse("8.990.000đ")).isEqualTo(8_990_000L);
        assertThat(Prices.parse("8,990,000 ₫")).isEqualTo(8_990_000L);
        assertThat(Prices.parse("7990000")).isEqualTo(7_990_000L);
        assertThat(Prices.parse("7990000.00")).isEqualTo(7_990_000L);
        assertThat(Prices.parse("Liên hệ")).isNull();
        assertThat(ProductPage.warranty("Bảo hành: 24 tháng")).isEqualTo(24);
        assertThat(ProductPage.warranty("không ghi")).isNull();
    }

    @Test
    void lamTheoRobotsTxt() {
        var anphat = PoliteFetcher.Robots.parse("""
                User-agent: *
                Disallow: /admin/
                Disallow: /ajax/
                Crawl-delay: 5
                User-agent: CCBot
                Disallow: /
                """, "smartcarepc");
        assertThat(anphat.delay().toSeconds()).isEqualTo(5);
        assertThat(anphat.allowed("/tim?q=rtx")).isTrue();
        assertThat(anphat.allowed("/ajax/price")).isFalse();
        var phongvu = PoliteFetcher.Robots.parse("User-agent: *\nAllow: /\nDisallow: /search?*\n", "smartcarepc");
        assertThat(phongvu.allowed("/search?query=rtx")).isFalse();
        assertThat(phongvu.allowed("/card-msi-rtx-4060--s123")).isTrue();
        assertThat(PoliteFetcher.Robots.parse("", "smartcarepc").allowed("/bat-ky")).isTrue();
    }

    @Test
    void khopDungModel() {
        assertThat(PriceEngine.relevance("RTX 4060 8GB", "Card màn hình MSI GeForce RTX 4060 VENTUS 2X 8G OC")).isPositive();
        // Laptop / PC lắp sẵn có RTX 4060 không phải card rời
        assertThat(PriceEngine.relevance("RTX 4060 8GB", "Laptop Acer Swift X (Ultra 7/ RTX 4060 8GB)")).isZero();
        assertThat(PriceEngine.relevance("RTX 4060", "PC GVN AMD R7-7700X/ RTX 4060")).isZero();
        assertThat(PriceEngine.relevance("RTX 4060", "Card màn hình MSI GeForce RTX 4060 Ti 8G")).isPositive();
        assertThat(PriceEngine.relevance("Intel Core i5-12400F", "CPU Intel Core i5-12400F (Up to 4.4GHz)")).isPositive();
        assertThat(PriceEngine.relevance("Intel Core i5-12400F", "CPU Intel Core i5-12400 (Up to 4.4GHz)")).isZero();
        assertThat(PriceEngine.relevance("RAM DDR4 16GB 3200", "RAM Corsair Vengeance LPX 16GB (2x8GB) DDR4 3200MHz")).isPositive();
        // RAM laptop không lắp vào máy bàn
        assertThat(PriceEngine.relevance("RAM DDR4 16GB 3200", "Ram Laptop Crucial DDR4 16GB Bus 3200")).isZero();
        // Từ khoá chung chung: đúng loại mới lấy
        assertThat(PriceEngine.relevance("Tản nhiệt khí CPU", "Tản nhiệt Cooler Master Hyper 612 Apex")).isPositive();
        assertThat(PriceEngine.relevance("Tản nhiệt khí CPU", "Vỏ máy tính Zotac Gaming ALLOY MATX White")).isZero();
        assertThat(PriceEngine.relevance("Tản nhiệt khí CPU", "Bo mạch chủ ASUS PRIME B760M-A D4 CSM")).isZero();
        // Bộ quạt case không phải tản nhiệt; tản có chữ "quạt" vẫn là tản
        assertThat(PriceEngine.relevance("Tản khí dạng tháp", "Bộ 3 quạt Cooler Master SickleFlow 120 ARGB")).isZero();
        assertThat(PriceEngine.kind(PriceEngine.norm("Tản nhiệt khí ID-COOLING SE-214-XT 1 quạt"))).isEqualTo("cooler");
        assertThat(PriceEngine.kind(PriceEngine.norm("Vỏ máy tính Gamdias ATHENA M3M - 3 FAN ARGB"))).isEqualTo("case");
        assertThat(PriceEngine.kind(PriceEngine.norm("Bộ 3 quạt Corsair RS120 ARGB"))).isEqualTo("fans");
        assertThat(PriceEngine.relevance("Vỏ case mid tower kính cường lực", "Vỏ máy tính Corsair 3500X TG Mid Tower Black")).isPositive();
    }

    @Test
    void tuKhoaDaiVaNhieuModel() {
        // "A hoặc B": tìm từng model, phần sau mượn tên loại của phần đầu
        assertThat(PriceEngine.alternatives("Card màn hình RTX 3050 8GB hoặc GTX 1660 Super giá rẻ"))
                .containsExactly("Card màn hình RTX 3050 8GB", "Card màn hình GTX 1660 Super giá rẻ");
        assertThat(PriceEngine.alternatives("RAM DDR4 16GB 3200")).containsExactly("RAM DDR4 16GB 3200");
        assertThat(PriceEngine.alternatives("Bộ nhớ trong/ RAM TeamGroup Vulcan Z 16GB")).containsExactly("Bộ nhớ trong/ RAM TeamGroup Vulcan Z 16GB");
        assertThat(PriceEngine.alternatives("Card màn hình RTX 3050 / GTX 1660")).containsExactly("Card màn hình RTX 3050", "Card màn hình GTX 1660");
        // Ô tìm kiếm của cửa hàng: bỏ "giá rẻ", số đo, socket
        assertThat(PriceEngine.shopQuery("Tản nhiệt khí CPU tháp 155mm 1 quạt giá rẻ LGA1700 AM4")).isEqualTo("Tản nhiệt khí CPU tháp");
        assertThat(PriceEngine.shopQuery("Card màn hình RTX 3050 8GB")).isEqualTo("Card màn hình RTX 3050");
        assertThat(PriceEngine.shopQuery("Tản nước 240mm")).isEqualTo("tản nhiệt AIO");
        assertThat(PriceEngine.shopQuery("tản khí tháp")).isEqualTo("tản nhiệt khí tháp");
        assertThat(PriceEngine.shopQuery("Case ATX kính cường lực")).isEqualTo("vỏ máy tính ATX kính");
        assertThat(PriceEngine.relevance("Tản nước 240mm", "Tản nhiệt AIO Cooler Master MasterLiquid 240L Core")).isPositive();
        // Thông số không bắt buộc khớp, mã model thì bắt buộc
        assertThat(PriceEngine.relevance("Tản nhiệt khí CPU tháp 155mm LGA1700", "Tản nhiệt khí Thermalright Peerless Assassin 120")).isPositive();
        assertThat(PriceEngine.relevance("Card màn hình RTX 3050 8GB", "Card màn hình MSI GeForce RTX 3060 12G")).isZero();
        assertThat(PriceEngine.isModel("3050")).isTrue();
        assertThat(PriceEngine.isModel("12400f")).isTrue();
        assertThat(PriceEngine.isModel("155mm")).isFalse();
        assertThat(PriceEngine.isModel("lga1700")).isFalse();
        assertThat(PriceEngine.isModel("650w")).isFalse();
    }

    @Test
    void xacMinhTrangDungSanPham() {
        // Cùng model nhưng khác dung lượng: không phải trang đúng (lỗi SSD 1TB nhưng link ra bản 4TB)
        assertThat(PriceEngine.samePage("Kingston NV2 M.2 2280 PCIe 4.0 x4 NVMe SSD 1TB SNV2S/1000G",
                "Ổ cứng SSD Kingston NV2 4TB M.2 2280 PCIe Gen4 x4 NVMe (SNV2S/4000G)")).isFalse();
        assertThat(PriceEngine.samePage("Kingston NV2 M.2 2280 PCIe 4.0 x4 NVMe SSD 1TB SNV2S/1000G",
                "Ổ cứng SSD Kingston NV2 1TB M.2 2280 PCIe Gen4 x4 NVMe (SNV2S/1000G)")).isTrue();
        // Tên dài kiểu Google Shopping và tên ngắn trên trang: so theo mã có số
        assertThat(PriceEngine.samePage("NZXT CC-H51E H5 Elite Vỏ máy Mid-Tower Cao Cấp Kính",
                "Vỏ case NZXT H5 Elite White (CC-H51EW-01)")).isTrue();
        assertThat(PriceEngine.samePage("Bộ nhớ/ RAM Corsair Dominator Platinum White RGB 16GB DDR4-3200Mhz",
                "RAM Corsair Dominator Platinum RGB 16GB (2x8GB) DDR4 3200MHz White")).isTrue();
        // Trang bán cả bộ không phải trang CPU lẻ
        assertThat(PriceEngine.samePage("CPU Intel Core i5-12400F", "Bộ PC CPU Intel Core i5-12400F Tray + Mainboard Infinity B760M")).isFalse();
        assertThat(PriceEngine.samePage("CPU Intel Core i5-12400F", "Combo CPU Intel Core i5-12400F + Mainboard MSI B760M")).isFalse();
        assertThat(PriceEngine.samePage("CPU Intel Core i5-12400F", "CPU Intel Core i5-12400F (Tặng kèm quạt tản nhiệt)")).isTrue();
        // Khác loại linh kiện / khác model
        assertThat(PriceEngine.samePage("Card màn hình MSI RTX 4060 Ventus 2X 8G OC", "Bo mạch chủ MSI PRO B760M-E DDR4")).isFalse();
        assertThat(PriceEngine.samePage("CPU Intel Core i5-12400F", "CPU Intel Core i5-14400F")).isFalse();
        assertThat(PriceEngine.samePage("Bộ vi xử lý Intel Core i5 12400F", "CPU Intel Core i5-12400F (Up to 4.4GHz)")).isTrue();
        // Link không phải trang sản phẩm
        assertThat(Shops.productLike("https://hacom.vn/")).isFalse();
        assertThat(Shops.productLike("https://gearvn.com/collections/ssd-nvme")).isFalse();
        assertThat(Shops.productLike("https://phongvu.vn/c/ssd")).isFalse();
        assertThat(Shops.productLike("https://www.phucanh.vn/chinh-sach-kiem-hang.html")).isFalse();
        assertThat(Shops.productLike("https://shop.vn/search?q=ssd")).isFalse();
        assertThat(Shops.productLike("https://gearvn.com/products/kingston-nv2-1tb")).isTrue();
        assertThat(Shops.productLike("https://hacom.vn/o-cung-ssd-kingston-nv2-1tb")).isTrue();
    }

    @Test
    void trangNhieuMucGiaThiChuaBietGia() {
        var html = """
                <html><head><script type="application/ld+json">{"@type":"Product","name":"Nguồn Thermaltake Litepower RGB",
                "offers":[{"@type":"Offer","price":"690000"},{"@type":"Offer","price":"990000"}]}</script></head><body></body></html>""";
        var o = ProductPage.parse(html, "https://tinhocngoisao.com/products/nguon-thermaltake-litepower", AT);
        assertThat(o.title()).isEqualTo("Nguồn Thermaltake Litepower RGB");
        assertThat(o.priceVnd()).isNull();
    }

    @Test
    void khongNhanHangCu() {
        assertThat(PriceEngine.usedGoods("Nguồn Thermaltake Litepower RGB 650W",
                "https://tinhocngoisao.com/products/2nd-nguon-thermaltake-litepower-rgb-650w-bh-3t?srsltid=x")).isTrue();
        assertThat(PriceEngine.usedGoods("Card RTX 3060 like new", "https://shop.vn/products/card-rtx-3060")).isTrue();
        assertThat(PriceEngine.usedGoods("Card RTX 3060 cũ", "https://shop.vn/products/card-rtx-3060")).isTrue();
        assertThat(PriceEngine.usedGoods("Nguồn MSI MAG A650BN", "https://memoryzone.com.vn/nguon-may-tinh-msi-mag-a650bn")).isFalse();
        // "2nd" chỉ tính khi đứng riêng trong đường dẫn
        assertThat(PriceEngine.usedGoods("SSD Samsung 2nd Gen", null)).isTrue();
        assertThat(PriceEngine.usedGoods("Tản nhiệt", "https://shop.vn/products/tan-nhiet-2nd-gen-x")).isTrue();
    }

    @Test
    void docTinhTrangHang() {
        assertThat(ProductPage.stock(org.jsoup.Jsoup.parse("<html><body><h1>SSD</h1><button>Mua ngay</button></body></html>"))).isTrue();
        assertThat(ProductPage.stock(org.jsoup.Jsoup.parse("<html><body><h1>SSD</h1><span>Tạm hết hàng</span></body></html>"))).isFalse();
        assertThat(ProductPage.stock(org.jsoup.Jsoup.parse(
                "<html><head><meta property=\"product:availability\" content=\"out of stock\"/></head><body><button>Mua ngay</button></body></html>")))
                .isFalse();
        assertThat(ProductPage.stock(org.jsoup.Jsoup.parse("<html><body><button disabled>Mua ngay</button></body></html>"))).isNull();
    }

    @Test
    void docTrangKhongCoJsonLdNhungCoMetaGia() {
        var html = """
                <html><head><meta property="og:type" content="product"/>
                <meta property="og:title" content="Ổ cứng SSD Kingston NV2 1TB"/>
                <meta property="product:price:amount" content="1450000"/></head><body>Bảo hành 36 tháng</body></html>""";
        var o = ProductPage.parseLoose(html, "https://hacom.vn/ssd-kingston-nv2-1tb", AT);
        assertThat(o.title()).isEqualTo("Ổ cứng SSD Kingston NV2 1TB");
        assertThat(o.priceVnd()).isEqualTo(1_450_000L);
        assertThat(o.warrantyMonths()).isEqualTo(36);
        // Trang không có dấu hiệu sản phẩm (trang tin): không nhận
        assertThat(ProductPage.parseLoose("<html><head><meta property=\"og:title\" content=\"Tin tức\"/></head></html>",
                "https://hacom.vn/tin-tuc/abc", AT)).isNull();
    }

    @Test
    void cungModelOCuaHangKhac() {
        assertThat(PriceEngine.sameModel("Card màn hình ASUS Dual GeForce RTX 4060 EVO OC Edition 8GB GDDR6",
                "VGA ASUS Dual RTX 4060 EVO OC 8GB")).isTrue();
        assertThat(PriceEngine.sameModel("Card màn hình MSI GeForce RTX 4060 VENTUS 2X 8G OC",
                "Card màn hình MSI GeForce RTX 4060 GAMING X 8G")).isFalse();
        assertThat(PriceEngine.sameModel("RTX 4060 Ti 8GB", "RTX 4060 8GB")).isFalse();
    }

    @Test
    void xepHangBoHangCuVaHetHang() {
        var o = List.of(
                offer("Card MSI RTX 4060 Ventus 2X 8G", 8_500_000L, false),
                offer("Card ASUS Dual RTX 4060 8G", 8_200_000L, true),
                offer("Card Gigabyte RTX 4060 Windforce 8G", 7_900_000L, null),
                offer("Card Zotac RTX 4060 8G cũ", 5_000_000L, true),
                offer("Card Colorful RTX 4060 8G", 7_700_000L, true));
        var ranked = PriceEngine.rank("RTX 4060", o);
        // Còn đủ 3 món còn bán: bỏ món hết hàng; hàng cũ bị loại; còn hàng trước, rẻ trước
        assertThat(ranked).extracting(Offer::title).containsExactly(
                "Card Colorful RTX 4060 8G", "Card ASUS Dual RTX 4060 8G", "Card Gigabyte RTX 4060 Windforce 8G");
    }

    @Test
    void searxngChiLayTrangSanPhamCuaCuaHangDaBiet() {
        var root = JsonMapper.builder().build().readTree("""
                {"results":[
                  {"url":"https://gearvn.com/collections/vga-rtx-4060"},
                  {"url":"https://gearvn.com/products/card-msi-rtx-4060"},
                  {"url":"https://gearvn.com/blogs/cong-nghe/review-4060"},
                  {"url":"https://phongvu.vn/c/vga-rtx-4060"},
                  {"url":"https://phongvu.vn/card-msi-rtx-4060--s123"},
                  {"url":"https://shopee.vn/card-4060"},
                  {"url":"https://www.anphatpc.com.vn/vga-asus-4060.html"}]}""");
        assertThat(Searxng.pick(root, 5)).containsExactly("https://gearvn.com/products/card-msi-rtx-4060",
                "https://phongvu.vn/card-msi-rtx-4060--s123", "https://www.anphatpc.com.vn/vga-asus-4060.html");
    }

    @Test
    void cpuKhongDuoiFThiKhongLayBanF() {
        // Bản F không có đồ họa tích hợp: máy học / văn phòng chọn nó là phải mua thêm card rời
        assertThat(PriceEngine.relevance("CPU Intel Core i3 12100", "Bộ vi xử lý Intel Core i3-12100F")).isZero();
        assertThat(PriceEngine.relevance("Core i5 12400", "CPU Intel Core i5 12400KF")).isZero();
        assertThat(PriceEngine.relevance("CPU Intel Core i3 12100", "Bộ vi xử lý Intel Core i3-12100 / 3.3GHz")).isPositive();
        assertThat(PriceEngine.relevance("Core i3 12100F", "Bộ vi xử lý Intel Core i3-12100F")).isPositive();
    }

    @Test
    void nhanDungLoaiTenCpuVaBoMacBook() {
        // "bộ nhớ cache" trong tên CPU không phải RAM
        assertThat(PriceEngine.kind(PriceEngine.norm(
                "bộ xử lý máy tính để bàn Core i3-12100 bốn nhân 12 MB bộ nhớ cache L3 Socket LGA-1700"))).isEqualTo("cpu");
        assertThat(PriceEngine.kind(PriceEngine.norm("Bộ nhớ/ RAM Corsair DDR4 16GB"))).isEqualTo("ram");
        // Tìm "CPU" chung chung không được ra máy MacBook nguyên chiếc
        assertThat(PriceEngine.relevance("CPU", "Macbook Pro Apple M5 chip with 10-core CPU and 10-core GPU, 16GB")).isZero();
    }

    @Test
    void trangMayNguyenBoKhongPhaiTrangLinhKien() {
        // Ca thật: link CPU i3-12100 trỏ vào trang "PC Văn Phòng i3 12100" giá 6,99 triệu
        assertThat(PriceEngine.samePage("Bộ vi xử lý/ CPU Intel Core i3-12100", "PC Văn Phòng i3 12100 / 8GB / SSD 256GB")).isFalse();
        assertThat(PriceEngine.samePage("Bộ vi xử lý/ CPU Intel Core i3-12100", "Máy tính bộ Intel Core i3-12100")).isFalse();
        assertThat(PriceEngine.samePage("Bộ vi xử lý/ CPU Intel Core i3-12100", "CPU Intel Core i3-12100 (Socket 1700)")).isTrue();
        // Chữ PCIe không bị nhầm là máy bộ
        assertThat(PriceEngine.samePage("SSD Kingston NV3 1TB PCIe Gen4", "SSD Kingston NV3 1TB M.2 PCIe Gen4 x4")).isTrue();
    }

    @Test
    void trangPhienBanKhacKhongPhaiTrangCuaMonDaChon() {
        // Ca thật: "RTX 4060" mở ra trang "RTX 4060 Ti Gaming X 16G" giá 14,3 triệu
        assertThat(PriceEngine.samePage("MSI NVIDIA GeForce RTX 4060 Graphic Card", "MSI GeForce RTX 4060 Ti GAMING X 16G")).isFalse();
        assertThat(PriceEngine.samePage("Card MSI RTX 4060 Ventus 2X 8G", "Card màn hình MSI GeForce RTX 4060 VENTUS 2X BLACK 8G OC")).isTrue();
        assertThat(PriceEngine.samePage("Bộ vi xử lý Intel Core i3-12100", "CPU Intel Core i3-12100F")).isFalse();
        assertThat(PriceEngine.samePage("Card Sapphire RX 7600 8GB", "Card Sapphire PULSE RX 7600 XT 16GB")).isFalse();
    }

    private static Offer offer(String title, long price, Boolean inStock) {
        return new Offer(title, "Shop", price, null, "https://gearvn.com/products/" + title.hashCode(), null, inStock, null, AT);
    }
}
