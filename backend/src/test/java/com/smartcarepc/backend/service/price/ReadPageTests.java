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
    void giaBuildKemMainChuaDoiChieuDieuKienKhongThanhGiaBanLe() {
        String html = """
                <script type="application/ld+json">{"@type":"Product","name":"CPU Intel Core i5-13400F",
                "offers":{"price":"4090000","availability":"https://schema.org/InStock"}}</script>
                <div class="list-info-offter"><div>Giá Build PC: 4.090.000đ Khi kèm B760M</div>
                <div>Giá bán lẻ rời CPU: 4.990.000đ</div></div>
                <button>Mua ngay</button>
                """;
        var offer = ProductPage.parse(html, URL, java.time.Instant.now());
        assertThat(offer.priceVnd()).isNull();
        assertThat(OfferEvidence.verified(offer.title(), offer, true)).isFalse();
        String fullBuildOnly = html.substring(0, html.indexOf("<div class=\"list-info-offter\""))
                + "<div class='product-short-description'>***Sản phẩm chỉ bán khi build trọn bộ máy***</div><button>Mua ngay</button>";
        assertThat(ProductPage.parse(fullBuildOnly, URL, java.time.Instant.now()).priceVnd()).isNull();
    }

    @Test
    void nutLienHeTrongFormGioHangKhongXacNhanConHang() {
        String html = """
                <script type="application/ld+json">{"@type":"Product","name":"Nguồn JETEK Revolve B650 650W",
                "offers":{"price":"990000","availability":"https://schema.org/InStock"}}</script>
                <form class="cart"><button class="single_add_to_cart_button openContactPopup">Liên Hệ</button></form>
                <div class="related"><button>Mua ngay</button></div>
                """;
        var offer = ProductPage.parse(html, URL, java.time.Instant.now());
        assertThat(offer.priceVnd()).isEqualTo(990_000L);
        assertThat(offer.inStock()).isNull();
        assertThat(OfferEvidence.verified(offer.title(), offer, true)).isFalse();
        String comingSoon = html.replace("<form class=\"cart\"><button class=\"single_add_to_cart_button openContactPopup\">Liên Hệ</button></form>",
                "<div id='register-in-advance-form'><h6>Đăng ký tư vấn</h6><p>Để lại thông tin liên hệ khi chúng tôi có hàng</p></div>");
        assertThat(comingSoon).contains("register-in-advance-form").doesNotContain("openContactPopup");
        assertThat(ProductPage.parse(comingSoon, URL, java.time.Instant.now()).inStock()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void docLaiTrangKhongRoBienTheKhongLamMoiGiaCu() {
        String name="SSD Samsung 970 EVO Plus 500GB / 1TB / 2TB M.2 NVMe";
        String listing="<a class='product-card' href='"+URL+"'><img alt='"+name+"'><p>1.799.000đ</p></a>";
        String page="<script type='application/ld+json'>{\"@type\":\"Product\",\"name\":\""+name
                +"\",\"offers\":{\"price\":\"1799000\",\"availability\":\"https://schema.org/InStock\"}}</script>";
        when(fetcher.get(any(),any())).thenAnswer(inv -> URL.equals(inv.getArgument(0)) ? page : listing);
        var known=(java.util.Map<String,Offer>)org.springframework.test.util.ReflectionTestUtils.getField(engine,"byUrl");
        known.put(URL,new Offer(name,"GEARVN",1_799_000L,null,URL,null,true,36,java.time.Instant.now()));
        assertThat(engine.search("SSD Samsung 970 EVO Plus")).noneSatisfy(o -> assertThat(o.url()).isEqualTo(URL));
        assertThat(engine.offer(URL).priceVnd()).isNull();
        assertThat(OfferEvidence.verified(name,engine.offer(URL),engine.pageChecked(URL))).isFalse();
    }

    @Test
    void cacheParserCuPhaiDocLaiTrangNgungBan(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws java.io.IOException {
        var file = dir.resolve("prices.json");
        var old = new Offer("SSD Kingston NV3 1TB", "GEARVN", 1_590_000L, null, URL, null, true, 36, java.time.Instant.now());
        tools.jackson.databind.json.JsonMapper.builder().build().writeValue(file.toFile(), java.util.Map.of(
                "searches", java.util.Map.of("ssd kingston", java.util.Map.of("at",java.time.Instant.now(),"offers",java.util.List.of(old),"incomplete",false)),
                "offers", java.util.Map.of(URL, old),
                "pageRead", java.util.Map.of(URL, System.currentTimeMillis())));
        org.springframework.test.util.ReflectionTestUtils.setField(engine,"cacheFile",file.toString());
        when(fetcher.page(eq(URL),any())).thenReturn(new PoliteFetcher.Page("""
                <script type="application/ld+json">{"@type":"Product","name":"SSD Kingston NV3 1TB",
                "offers":{"price":"1590000","availability":"https://schema.org/InStock"}}</script>
                <p class="stop-buy">Sản phẩm đã ngừng kinh doanh</p>
                """,URL));
        try {
            engine.loadCache();
            assertThat((java.util.Map<?,?>)org.springframework.test.util.ReflectionTestUtils.getField(engine,"cache")).isEmpty();
            assertThat(engine.pageChecked(URL)).as("timestamp của parser cũ không đủ để xác minh").isFalse();
            assertThat(engine.quote(URL,old.title())).isNull();
            assertThat(engine.offer(URL).inStock()).isFalse();
        } finally { engine.saveOnShutdown(); }
    }

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
