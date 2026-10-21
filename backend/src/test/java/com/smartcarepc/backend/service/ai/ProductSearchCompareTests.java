package com.smartcarepc.backend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.smartcarepc.backend.service.price.Offer;
import com.smartcarepc.backend.service.price.PriceEngine;

/** Bảng so giá cùng model ở cửa hàng khác: chỉ nơi đã đọc trang sản phẩm, còn hàng, đúng biến thể (ca thật vòng đo v4) */
class ProductSearchCompareTests {

    private final PriceEngine engine = mock(PriceEngine.class);
    private final ProductSearch search = new ProductSearch("", "", engine);
    private final Instant at = Instant.parse("2026-10-21T10:00:00Z");

    @Test
    void chiHienNoiDaKiemTrangConHangDungBienThe() {
        var pick = new ProductSearch.Product("Card ASUS Dual RTX 4060 EVO OC 8GB", "GEARVN", 7_990_000L, "https://gearvn.com/products/4060", null);
        var google = new ProductSearch.Product("ASUS Dual RTX 4060 EVO OC 8GB", "Shop X", 7_500_000L, "https://www.google.com/search?ibp=oshop&q=4060", null);
        var listing = new ProductSearch.Product("Card ASUS Dual RTX 4060 EVO OC 8GB", "Phong Vũ", 7_700_000L, "https://phongvu.vn/4060", null);
        var soldOut = new ProductSearch.Product("Card ASUS Dual RTX 4060 EVO OC 8GB", "Hacom", 7_600_000L, "https://hacom.vn/4060", null);
        var ti = new ProductSearch.Product("Card ASUS Dual RTX 4060 Ti EVO OC 8GB", "An Phát", 9_900_000L, "https://anphatpc.com.vn/4060ti.html", null);
        var good = new ProductSearch.Product("VGA ASUS Dual RTX 4060 EVO OC 8GB", "An Khang", 7_800_000L, "https://ankhang.vn/4060.html", null);
        // Phong Vũ: mới thấy trên trang tìm kiếm (chưa kiểm trang); Hacom: trang báo hết hàng; An Phát: bản Ti
        when(engine.offer(listing.url())).thenReturn(offer(listing, null));
        when(engine.offer(soldOut.url())).thenReturn(offer(soldOut, false));
        when(engine.pageChecked(soldOut.url())).thenReturn(true);
        when(engine.offer(ti.url())).thenReturn(offer(ti, true));
        when(engine.pageChecked(ti.url())).thenReturn(true);
        when(engine.offer(good.url())).thenReturn(offer(good, true));
        when(engine.pageChecked(good.url())).thenReturn(true);

        var shown = search.compare(pick, List.of(pick, google, listing, soldOut, ti, good));

        assertThat(shown).extracting(Offer::url).containsExactly(good.url());
    }

    private Offer offer(ProductSearch.Product p, Boolean inStock) {
        return new Offer(p.title(), p.shop(), p.priceVnd(), null, p.url(), null, inStock, null, at);
    }
}
