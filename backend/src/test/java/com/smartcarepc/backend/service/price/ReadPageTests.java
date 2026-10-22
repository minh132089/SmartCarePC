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
    private final PriceEngine engine = new PriceEngine(fetcher, mock(Searxng.class), true);
    private static final String URL = "https://gearvn.com/products/ssd-kingston-nv3-1tb";

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
    void trangHetHangBiLoaiVaKhongTaiLai() {
        when(fetcher.page(eq(URL), any())).thenReturn(new PoliteFetcher.Page("""
                <html><head><script type="application/ld+json">{"@type":"Product","name":"SSD Kingston NV3 1TB",
                "offers":{"@type":"Offer","price":"1590000","availability":"https://schema.org/OutOfStock"}}</script></head></html>
                """, URL));

        assertThat(engine.readPage(URL, "SSD Kingston NV3 1TB")).isNull();
        assertThat(engine.readPage(URL, "SSD Kingston NV3 1TB")).isNull();

        verify(fetcher, times(1)).page(eq(URL), any());
    }

    @org.junit.jupiter.api.Test
    void khoaBoNhoDemBoDauCauVaKhoangTrang() {
        org.assertj.core.api.Assertions.assertThat(PriceEngine.cacheKey("RTX 4060 8GB, 2 quạt"))
                .isEqualTo(PriceEngine.cacheKey("  rtx 4060   8gb 2 quạt "));
        org.assertj.core.api.Assertions.assertThat(PriceEngine.cacheKey("Mainboard (ITX) B760I/DDR4")).isEqualTo("mainboard itx b760i ddr4");
    }
}
