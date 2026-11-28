package com.smartcarepc.backend.service.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Theo dõi giá ngầm (24/11): món đã lên thẻ được đọc lại ở nền, trợ lý thấy xu hướng giá / còn hàng */
class PriceWatchTests {

    private final PoliteFetcher fetcher = mock(PoliteFetcher.class);
    private final PriceEngine engine = new PriceEngine(fetcher, mock(Searxng.class), true);
    private static final String URL = "https://gearvn.com/products/ssd-kingston-nv3-1tb";
    private static final String TITLE = "SSD Kingston NV3 1TB M.2 NVMe";

    private static Offer offer(long price, Boolean inStock) {
        return new Offer(TITLE, "GEARVN", price, null, URL, null, inStock, null, Instant.now());
    }

    @Test
    void chiGhiLichSuChoMonDangTheoDoiVaBaoXuHuongChoTroLy() {
        // Món chưa từng lên thẻ: không ghi gì
        engine.note(URL, offer(1_000_000, true));
        assertThat(engine.trend(URL)).isNull();

        engine.track(URL, TITLE);
        engine.note(URL, offer(1_000_000, true));
        // Giá và còn hàng không đổi: không thêm điểm, chưa có xu hướng
        engine.note(URL, offer(1_000_000, true));
        assertThat(engine.trend(URL)).isNull();

        engine.note(URL, offer(900_000, true));
        assertThat(engine.trend(URL)).isEqualTo("price down 10% over 1 day");
        engine.note(URL, offer(900_000, false));
        assertThat(engine.trend(URL)).isEqualTo("price down 10% over 1 day; out of stock 1 time this week");
    }

    @Test
    void danhSachTheoDoiLuuRaFileVaNapLaiKhiKhoiDongLai() throws Exception {
        var file = Files.createTempFile("price-cache", ".json");
        Files.delete(file);
        ReflectionTestUtils.setField(engine, "cacheFile", file.toString());
        engine.track(URL, TITLE);
        engine.note(URL, offer(2_000_000, true));
        engine.note(URL, offer(2_200_000, true));
        engine.saveCache();

        var again = new PriceEngine(fetcher, mock(Searxng.class), false);
        ReflectionTestUtils.setField(again, "cacheFile", file.toString());
        again.loadCache();
        assertThat(again.trend(URL)).isEqualTo("price up 10% over 1 day");
        again.saveOnShutdown();
        Files.deleteIfExists(file);
    }

    @Test
    void luotNenDocLaiTrangMonDangTheoDoiVaGhiGiaMoi() {
        when(fetcher.page(eq(URL), any())).thenReturn(new PoliteFetcher.Page("""
                <script type="application/ld+json">{"@type":"Product","name":"SSD Kingston NV3 1TB M.2 NVMe",
                "offers":{"price":"1590000","availability":"https://schema.org/InStock"}}</script>
                <button>Mua ngay</button>
                """, URL));
        engine.track(URL, TITLE);
        engine.note(URL, offer(1_690_000, true));

        engine.refreshWatchedNow();

        verify(fetcher, times(1)).page(eq(URL), any());
        assertThat(engine.pageChecked(URL)).isTrue();
        assertThat(engine.trend(URL)).isEqualTo("price down 6% over 1 day");
        // Trang vừa đọc còn mới: lượt sau không đọc lại
        engine.refreshWatchedNow();
        verify(fetcher, times(1)).page(eq(URL), any());
    }
}
