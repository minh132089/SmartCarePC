package com.smartcarepc.backend.service.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Kho giá: trả ứng viên ngay từ sản phẩm đã thấy, nhưng không coi giá trong kho là giá đã xác minh */
class PriceIndexTests {

    private final PoliteFetcher fetcher = mock(PoliteFetcher.class);
    private final PriceEngine engine = new PriceEngine(fetcher, mock(Searxng.class), true);

    private static Offer gpu(String name, long price, Boolean inStock, Instant at) {
        String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        return new Offer(name, "Shop", price, null, "https://gearvn.com/products/" + slug, null, inStock, null, at);
    }

    private void seed(Instant at) {
        for (var o : List.of(
                gpu("Card màn hình ASUS Dual GeForce RTX 4060 OC 8GB", 8_190_000L, true, at),
                gpu("Card màn hình MSI GeForce RTX 4060 Ventus 2X Black 8G OC", 7_990_000L, true, at),
                gpu("Card màn hình Gigabyte GeForce RTX 4060 Eagle OC 8G", 8_090_000L, null, at),
                gpu("Card màn hình Zotac Gaming GeForce RTX 4060 8GB Twin Edge", 7_790_000L, true, at))) {
            engine.index(o);
        }
    }

    @Test
    void duUngVienTrongKhoThiTraNgayKhongChoWebCuaHang() {
        seed(Instant.now().minus(Duration.ofHours(1)));
        long t0 = System.nanoTime();
        var found = engine.search("RTX 4060");
        long ms = (System.nanoTime() - t0) / 1_000_000;

        assertThat(found).hasSizeGreaterThanOrEqualTo(PriceEngine.INDEX_MIN);
        assertThat(found).allMatch(o -> o.title().contains("4060"));
        assertThat(ms).as("trả từ kho, không mở trang tìm kiếm của cửa hàng").isLessThan(1000);
        // Giá trong kho chỉ để chọn: chưa ai đọc lại trang sản phẩm nên chưa phải giá đã xác minh (không lên thẻ được)
        assertThat(found).noneMatch(o -> engine.pageChecked(o.url()));
    }

    @Test
    void khoBoQuaHangCuHonMuoiHaiGioVaHangHetHang() {
        seed(Instant.now().minus(PriceEngine.INDEX_AGE).minus(Duration.ofMinutes(5)));
        assertThat(engine.indexed("RTX 4060")).as("quá INDEX_AGE thì không dùng").isEmpty();

        engine.index(gpu("Card màn hình Palit GeForce RTX 4060 Dual 8GB", 7_500_000L, false, Instant.now()));
        assertThat(engine.indexed("RTX 4060")).as("biết là hết hàng thì không đưa vào ứng viên").isEmpty();
    }

    @Test
    void hoiDongTiThiChiTraDongTiKeCaVietLien() {
        seed(Instant.now());
        engine.index(gpu("Card màn hình ASUS Dual GeForce RTX 4060Ti OC 8GB", 10_990_000L, true, Instant.now()));
        engine.index(gpu("Card màn hình MSI GeForce RTX 4060 Ti Ventus 2X 8G", 10_790_000L, true, Instant.now()));
        assertThat(engine.indexed("RTX 4060 Ti")).hasSize(2).allMatch(o -> PriceEngine.tierVariants(o.title()).contains("ti"));
        assertThat(engine.indexed("RTX 4060")).hasSize(4).noneMatch(o -> PriceEngine.tierVariants(o.title()).contains("ti"));
    }

    @Test
    void ssdSaiDungLuongKhongLaUngVien() {
        engine.index(gpu("SSD Kingston NV3 500GB M.2 NVMe", 890_000L, true, Instant.now()));
        engine.index(gpu("SSD Samsung 990 EVO 2TB M.2 NVMe", 3_490_000L, true, Instant.now()));
        assertThat(engine.indexed("SSD NVMe 1TB")).isEmpty();
    }

    @Test
    void khoKhongTraSaiModel() {
        seed(Instant.now());
        assertThat(engine.indexed("RTX 4060 Ti")).as("4060 thường không phải 4060 Ti").isEmpty();
        assertThat(engine.indexed("RX 7600")).isEmpty();
    }

    @Test
    void tuKhoaCoMaModelChiCanHaiUngVien() {
        assertThat(PriceEngine.indexMin("Core i3 12100")).isEqualTo(PriceEngine.INDEX_MIN_MODEL);
        assertThat(PriceEngine.indexMin("tản nhiệt ID-COOLING SE-214")).isEqualTo(PriceEngine.INDEX_MIN_MODEL);
        assertThat(PriceEngine.indexMin("SSD NVMe 256GB")).isEqualTo(PriceEngine.INDEX_MIN);
        assertThat(PriceEngine.indexMin("nguồn 450W")).isEqualTo(PriceEngine.INDEX_MIN);

        engine.index(new Offer("CPU Intel Core i3 12100 / 3.3GHz Turbo 4.3GHz", "GEARVN", 2_390_000L, null,
                "https://gearvn.com/products/core-i3-12100", null, true, null, Instant.now()));
        engine.index(new Offer("Bộ vi xử lý Intel Core i3-12100 Box", "An Phát", 2_450_000L, null,
                "https://www.anphatpc.com.vn/cpu-intel-core-i3-12100.html", null, null, null, Instant.now()));
        long t0 = System.nanoTime();
        var found = engine.search("Core i3 12100");
        assertThat(found).hasSize(2);
        assertThat((System.nanoTime() - t0) / 1_000_000).as("trả từ kho, không mở web cửa hàng").isLessThan(1000);
        verify(fetcher, never()).get(anyString(), any());
    }

    @Test
    void boLuotVaChuaToiLuotKhongPhaiNguonLoiVaBuBangKho() {
        // Chỉ 2 SSD 1TB trong kho (từ khoá chung cần 4): phải tìm thật. Mọi cửa hàng đang phải chờ giãn cách thì bỏ lượt,
        // không coi là nguồn lỗi (không đọc lại trang cũ), kết quả có ứng viên trong kho
        engine.index(new Offer("SSD Kingston NV3 1TB M.2 NVMe", "An Phát", 1_590_000L, null,
                "https://www.anphatpc.com.vn/ssd-kingston-nv3-1tb.html", null, true, null, Instant.now()));
        engine.index(new Offer("SSD Samsung 990 EVO 1TB M.2 NVMe", "GEARVN", 2_290_000L, null,
                "https://gearvn.com/products/ssd-samsung-990-evo-1tb", null, true, null, Instant.now()));
        when(fetcher.mustWait(anyString(), any())).thenReturn(true);

        var found = engine.search("SSD NVMe 1TB");

        assertThat(found).hasSize(2).allMatch(o -> o.title().contains("1TB"));
        verify(fetcher, never()).get(anyString(), any());
        verify(fetcher, never()).page(anyString(), any());
        assertThat(engine.busyWithUsers()).as("vừa có người tìm: tác vụ nền nhường lượt").isTrue();
    }
}
