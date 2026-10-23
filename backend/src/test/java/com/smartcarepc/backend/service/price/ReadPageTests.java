package com.smartcarepc.backend.service.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

/** Xác minh trang sản phẩm (vòng đo v5: trang không đọc được bị loại vĩnh viễn, 29 lần thiếu link) */
class ReadPageTests {

    private final PoliteFetcher fetcher = mock(PoliteFetcher.class);
    private final Searxng searxng = mock(Searxng.class);
    private final PriceEngine engine = new PriceEngine(fetcher, searxng, true);
    private static final String URL = "https://gearvn.com/products/ssd-kingston-nv3-1tb";

    @Test
    void h1GiuMaKitBiMetadataSeoLuocBo() {
        String html = """
                <h1>RAM Kingston Fury 32 <span>GB-DDR4-3200</span> MHz (KF432C16BB1AK2/32)</h1>
                <script type="application/ld+json">{"@type":"Product","name":"RAM Kingston Fury 32 GB-DDR4-3200 MHz",
                "offers":{"price":"4390000","availability":"https://schema.org/InStock"}}</script>
                """;
        var offer = ProductPage.parse(html, "https://fptshop.com.vn/linh-kien/ram-kingston-fury-32-gb-ddr4-3200-mhz", java.time.Instant.now());
        assertThat(offer.title()).contains("KF432C16BB1AK2/32");
        assertThat(offer.priceVnd()).isEqualTo(4_390_000L);
        assertThat(ProductPage.parse(html.replace("<h1>", "<h2>").replace("</h1>", "</h2>"), URL, java.time.Instant.now()).title())
                .doesNotContain("KF432C16BB1AK2/32");
    }

    @Test
    void giaOnlineHienThiGhiDeMetadataCuVaGiaMuaCaBo() {
        String html = """
                <script type="application/ld+json">{"@type":"Product","name":"CPU Intel Core i5-14400F",
                "offers":{"price":"4699000","availability":"https://schema.org/InStock"}}</script>
                <p class="price-sale">Giá khuyến mại: <strong itemprop="price">4.999.000 VND</strong></p>
                <p class="price-online">Giá mua online: <strong>5.299.000 VND</strong></p>
                """;
        var offer = ProductPage.parse(html, "https://hacom.vn/cpu-intel-core-i5-14400f", java.time.Instant.now());
        assertThat(offer.priceVnd()).isEqualTo(5_299_000L);
        assertThat(offer.inStock()).isTrue();
        assertThat(ProductPage.parse(html + "<p class='price-online'><strong>5.999.000 VND</strong></p>",
                "https://hacom.vn/cpu-intel-core-i5-14400f", java.time.Instant.now()).priceVnd()).isNull();
    }

    @Test
    void hangDatTruocKhongThanhConHangViCoNutMua() {
        for (String status : java.util.List.of("PreOrder", "BackOrder")) {
            String html = """
                    <script type="application/ld+json">{"@type":"Product","name":"SSD Kingston NV3 1TB",
                    "offers":{"price":"1590000","availability":"https://schema.org/%s"}}</script>
                    <button>Mua ngay</button>
                    """.formatted(status);
            assertThat(ProductPage.parse(html, URL, java.time.Instant.now()).inStock()).as(status).isFalse();
        }
        String html = "<meta itemprop='availability' content='https://schema.org/PreOrder'><button>Mua ngay</button>";
        assertThat(ProductPage.stock(org.jsoup.Jsoup.parse(html))).isFalse();
    }

    @Test
    void trangKhongDocDuocThiChuaXacMinhVaThuLaiSau() {
        // Trang vẽ bằng JavaScript: tải được nhưng không có tên / giá. Không bị loại, không tải lại ngay
        when(fetcher.page(eq(URL), any())).thenReturn(new PoliteFetcher.Page("<html><body><div id=app></div></body></html>", URL));

        assertThat(engine.readPage(URL, "SSD Kingston NV3 1TB")).isNull();
        assertThat(engine.readPage(URL, "SSD Kingston NV3 1TB")).isNull();

        verify(fetcher, times(1)).page(eq(URL), any());
        assertThat(engine.pageChecked(URL)).isFalse();
    }

    @Test
    void trangDocDuocThiDaKiemTrang() {
        when(fetcher.page(eq(URL), any())).thenReturn(new PoliteFetcher.Page("""
                <html><head><script type="application/ld+json">{"@type":"Product","name":"SSD Kingston NV3 1TB",
                "offers":{"@type":"Offer","price":"1590000","availability":"https://schema.org/InStock"}}</script></head></html>
                """, URL));

        var page = engine.readPage(URL, "SSD Kingston NV3 1TB");

        assertThat(page).isNotNull();
        assertThat(page.priceVnd()).isEqualTo(1_590_000L);
        assertThat(page.inStock()).isTrue();
        assertThat(engine.pageChecked(URL)).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void trangDaDocQuaMuoiPhutThiCanDocLaiGiaMoi() {
        String html = "<script type=\"application/ld+json\">{\"@type\":\"Product\",\"name\":\"SSD Kingston NV3 1TB\",\"offers\":{\"price\":\"1590000\",\"availability\":\"https://schema.org/InStock\"}}</script>";
        when(fetcher.page(eq(URL), any())).thenReturn(new PoliteFetcher.Page(html, URL));
        engine.readPage(URL, "SSD Kingston NV3 1TB");
        var read = (java.util.Map<String, Long>) org.springframework.test.util.ReflectionTestUtils.getField(engine, "pageRead");
        read.replaceAll((url, at) -> System.currentTimeMillis() - java.time.Duration.ofMinutes(11).toMillis());
        when(fetcher.page(eq(URL), any())).thenReturn(new PoliteFetcher.Page(html.replace("1590000", "1790000"), URL));
        assertThat(engine.pageChecked(URL)).isFalse();
        assertThat(engine.readPage(URL, "SSD Kingston NV3 1TB").priceVnd()).isEqualTo(1_790_000L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void chotBaoGiaThiLamMoiChungCuDaSauPhut() {
        String html = "<script type=\"application/ld+json\">{\"@type\":\"Product\",\"name\":\"SSD Kingston NV3 1TB\",\"offers\":{\"price\":\"1590000\",\"availability\":\"https://schema.org/InStock\"}}</script>";
        when(fetcher.page(eq(URL), any())).thenReturn(new PoliteFetcher.Page(html, URL));
        engine.readPage(URL, "SSD Kingston NV3 1TB");
        var read = (java.util.Map<String, Long>) org.springframework.test.util.ReflectionTestUtils.getField(engine, "pageRead");
        read.replaceAll((url, at) -> System.currentTimeMillis() - java.time.Duration.ofMinutes(6).toMillis());
        when(fetcher.page(eq(URL), any())).thenReturn(new PoliteFetcher.Page(html.replace("1590000", "1790000"), URL));
        assertThat(engine.quote(URL, "SSD Kingston NV3 1TB").priceVnd()).isEqualTo(1_790_000L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void docLaiLoiKhongDoiGioCuVaKhongGiaHanBangChungQuaMuoiPhut() {
        String html = "<script type=\"application/ld+json\">{\"@type\":\"Product\",\"name\":\"SSD Kingston NV3 1TB\",\"offers\":{\"price\":\"1590000\",\"availability\":\"https://schema.org/InStock\"}}</script>";
        when(fetcher.page(eq(URL),any())).thenReturn(new PoliteFetcher.Page(html,URL));
        var before=engine.readPage(URL,"SSD Kingston NV3 1TB");
        var read=(java.util.Map<String,Long>)org.springframework.test.util.ReflectionTestUtils.getField(engine,"pageRead");
        when(fetcher.page(eq(URL),any())).thenReturn(null);
        read.replaceAll((url,at)->System.currentTimeMillis()-java.time.Duration.ofMinutes(6).toMillis());
        assertThat(engine.quote(URL,"SSD Kingston NV3 1TB").checkedAt()).isEqualTo(before.checkedAt());
        assertThat(engine.pageChecked(URL)).isTrue();
        read.replaceAll((url,at)->System.currentTimeMillis()-java.time.Duration.ofMinutes(11).toMillis());
        engine.quote(URL,"SSD Kingston NV3 1TB");
        assertThat(engine.pageChecked(URL)).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void chotGiaThuLaiMotLanSauHtmlTamThoiKhongDocDuoc() {
        String html = "<script type=\"application/ld+json\">{\"@type\":\"Product\",\"name\":\"SSD Kingston NV3 1TB\",\"offers\":{\"price\":\"1590000\",\"availability\":\"https://schema.org/InStock\"}}</script>";
        when(fetcher.page(eq(URL), any())).thenReturn(new PoliteFetcher.Page(html, URL));
        var old = engine.readPage(URL, "SSD Kingston NV3 1TB");
        var read = (java.util.Map<String, Long>) org.springframework.test.util.ReflectionTestUtils.getField(engine, "pageRead");
        read.replaceAll((url, at) -> System.currentTimeMillis() - java.time.Duration.ofMinutes(11).toMillis());
        when(fetcher.page(eq(URL), any())).thenReturn(new PoliteFetcher.Page("<title>Đang tải</title>", URL),
                new PoliteFetcher.Page(html.replace("1590000", "1790000"), URL));

        var quote = engine.quote(URL, "SSD Kingston NV3 1TB");
        assertThat(quote.priceVnd()).isEqualTo(1_790_000L);
        assertThat(quote.checkedAt()).isAfter(old.checkedAt());
        assertThat(engine.pageChecked(URL)).isTrue();
        verify(fetcher, times(3)).page(eq(URL), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void thuLaiVanLoiThiKhongXacMinhGiaHetHan() {
        String html = "<script type=\"application/ld+json\">{\"@type\":\"Product\",\"name\":\"SSD Kingston NV3 1TB\",\"offers\":{\"price\":\"1590000\",\"availability\":\"https://schema.org/InStock\"}}</script>";
        when(fetcher.page(eq(URL), any())).thenReturn(new PoliteFetcher.Page(html, URL));
        var old = engine.readPage(URL, "SSD Kingston NV3 1TB");
        var read = (java.util.Map<String, Long>) org.springframework.test.util.ReflectionTestUtils.getField(engine, "pageRead");
        read.replaceAll((url, at) -> System.currentTimeMillis() - java.time.Duration.ofMinutes(11).toMillis());
        when(fetcher.page(eq(URL), any())).thenReturn(new PoliteFetcher.Page("<title>Đang tải</title>", URL));

        assertThat(engine.quote(URL, "SSD Kingston NV3 1TB").checkedAt()).isEqualTo(old.checkedAt());
        assertThat(engine.pageChecked(URL)).isFalse();
        verify(fetcher, times(3)).page(eq(URL), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void docLaiDongThoiKhongLamMatChungCuCuConHan() throws Exception {
        String html = "<script type=\"application/ld+json\">{\"@type\":\"Product\",\"name\":\"SSD Kingston NV3 1TB\",\"offers\":{\"price\":\"1590000\",\"availability\":\"https://schema.org/InStock\"}}</script>";
        when(fetcher.page(eq(URL), any())).thenReturn(new PoliteFetcher.Page(html, URL));
        var old = engine.readPage(URL, "SSD Kingston NV3 1TB");
        var read = (java.util.Map<String, Long>) org.springframework.test.util.ReflectionTestUtils.getField(engine, "pageRead");
        read.replaceAll((url, at) -> System.currentTimeMillis() - java.time.Duration.ofMinutes(6).toMillis());
        var started = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        when(fetcher.page(eq(URL), any())).thenAnswer(inv -> {
            started.countDown();
            if (!release.await(3, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("test timeout");
            return new PoliteFetcher.Page(html.replace("1590000", "1790000"), URL);
        });
        var future = java.util.concurrent.CompletableFuture.supplyAsync(() -> engine.quote(URL, "SSD Kingston NV3 1TB"));
        try {
            assertThat(started.await(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(engine.pageChecked(URL)).isTrue();
            assertThat(engine.offer(URL).checkedAt()).isEqualTo(old.checkedAt());
        } finally { release.countDown(); }
        assertThat(future.get(3, java.util.concurrent.TimeUnit.SECONDS).priceVnd()).isEqualTo(1_790_000L);
    }

    @Test
    void trangHetHangBiLoaiVaKhongTaiLai() {
        when(fetcher.page(eq(URL), any())).thenReturn(new PoliteFetcher.Page("""
                <html><head><script type="application/ld+json">{"@type":"Product","name":"SSD Kingston NV3 1TB",
                "offers":{"@type":"Offer","price":"1590000","availability":"https://schema.org/OutOfStock"}}</script></head></html>
                """, URL));

        assertThat(engine.readPage(URL, "SSD Kingston NV3 1TB")).isNull();
        assertThat(engine.readPage(URL, "SSD Kingston NV3 1TB")).isNull();

        verify(fetcher, times(1)).page(eq(URL), any());
    }

    @Test
    void timKiemLoiMotNguonVanBoSungModelDungDaBiet() {
        partialGpuSearch(false);
    }

    @Test
    void cacheTimKiemThieuVanDocLaiUrlModelDung() {
        partialGpuSearch(true);
    }

    private void partialGpuSearch(boolean cacheFirst) {
        String query = "VGA RTX 4060 Ti 16GB";
        String gpuUrl = "https://gearvn.com/products/inno3d-rtx-4060-ti-16gb";
        String name = "VGA Inno3D RTX 4060 Ti 16GB";
        when(fetcher.get(any(), any())).thenReturn("""
                <a class="product-card" href="/products/asus-4060-ti-8gb">
                <img alt="VGA ASUS RTX 4060 Ti 8GB"><span>12.890.000đ</span></a>
                """);
        when(searxng.productUrls(any(), org.mockito.ArgumentMatchers.anyInt())).thenThrow(new IllegalStateException("source timeout"));
        if (cacheFirst) assertThat(engine.search(query)).noneMatch(o -> o.url().equals(gpuUrl));
        when(fetcher.page(eq(gpuUrl), any())).thenReturn(new PoliteFetcher.Page("""
                <script type="application/ld+json">{"@type":"Product","name":"VGA Inno3D RTX 4060 Ti 16GB",
                "offers":{"price":"11399000","availability":"https://schema.org/InStock"}}</script>
                """, gpuUrl));
        engine.readPage(gpuUrl, name);

        assertThat(engine.search(query)).anySatisfy(o -> {
            assertThat(o.url()).isEqualTo(gpuUrl);
            assertThat(o.priceVnd()).isEqualTo(11_399_000L);
            assertThat(engine.pageChecked(o.url())).isTrue();
        });
    }

    @Test
    void timKiemLoiThiDocLaiUrlCuNhungKhongDungGiaHetHan() {
        String html = """
                <script type="application/ld+json">{"@type":"Product","name":"SSD Kingston NV3 1TB",
                "offers":{"@type":"Offer","price":"1590000","availability":"https://schema.org/InStock"}}</script>
                """;
        when(fetcher.page(eq(URL), any())).thenReturn(new PoliteFetcher.Page(html, URL));
        engine.readPage(URL, "SSD Kingston NV3 1TB");
        ((java.util.Map<?, ?>) org.springframework.test.util.ReflectionTestUtils.getField(engine, "pageRead")).clear();
        when(fetcher.page(eq(URL), any())).thenReturn(null);
        assertThat(engine.recheckKnown("SSD Kingston NV3 1TB")).isEmpty();
        when(fetcher.page(eq(URL), any())).thenReturn(new PoliteFetcher.Page(html.replace("1590000", "1790000"), URL));
        assertThat(engine.recheckKnown("SSD Kingston NV3 1TB")).singleElement()
                .satisfies(o -> assertThat(o.priceVnd()).isEqualTo(1_790_000L));
    }

    @org.junit.jupiter.api.Test
    void khoaBoNhoDemBoDauCauVaKhoangTrang() {
        org.assertj.core.api.Assertions.assertThat(PriceEngine.cacheKey("RTX 4060 8GB, 2 quạt"))
                .isEqualTo(PriceEngine.cacheKey("  rtx 4060   8gb 2 quạt "));
        org.assertj.core.api.Assertions.assertThat(PriceEngine.cacheKey("Mainboard (ITX) B760I/DDR4")).isEqualTo("mainboard itx b760i ddr4");
    }

    @Test
    void quatDuLichKhongPhaiQuatCase() {
        assertThat(PriceEngine.kind(PriceEngine.norm("Quạt du lịch cầm tay Jisulife F2B White J1975"))).isEqualTo("appliance");
        assertThat(PriceEngine.kind(PriceEngine.norm("Bộ 3 quạt Cooler Master SICKLEFLOW 120 ARGB WHITE EDITION"))).isEqualTo("fans");
    }

    @Test
    void deTanNhietLaptopKhongPhaiTanCpuMayBan() {
        for (String title : java.util.List.of("Đế Tản Nhiệt Cooler Master X-Slim II", "Cooler Master NotePal X-Slim II",
                "Tản nhiệt laptop 15 inch", "Notebook cooler cooling pad")) {
            assertThat(PriceEngine.kind(PriceEngine.norm(title))).as(title).isEqualTo("accessory");
        }
        assertThat(PriceEngine.kind(PriceEngine.norm("Tản nhiệt Cooler Master Hyper 212 Spectrum V3 ARGB"))).isEqualTo("cooler");
    }
}
