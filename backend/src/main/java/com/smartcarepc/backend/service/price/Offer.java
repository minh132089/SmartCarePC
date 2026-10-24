package com.smartcarepc.backend.service.price;

import java.time.Instant;

/**
 * Một sản phẩm đang bán ở một cửa hàng, đọc thẳng từ web cửa hàng.
 *
 * @param priceVnd       giá đang bán (đồng)
 * @param oldPriceVnd    giá gốc trước giảm, null nếu không có
 * @param inStock        còn hàng; null khi trang không cho biết (vd chỉ đọc trang tìm kiếm)
 * @param warrantyMonths bảo hành (tháng) nếu trang có ghi
 * @param checkedAt      lúc đọc giá
 */
public record Offer(String title, String shop, Long priceVnd, Long oldPriceVnd, String url, String imageUrl,
        Boolean inStock, Integer warrantyMonths, Instant checkedAt) {

    /** Bổ sung thông tin còn thiếu (còn hàng, bảo hành, ảnh) từ lần đọc trang sản phẩm */
    Offer merge(Offer page) {
        if (page == null) return this;
        return new Offer(page.title(), shop, page.priceVnd(),
                oldPriceVnd != null ? oldPriceVnd : page.oldPriceVnd(), url,
                page.imageUrl(), page.inStock(),
                page.warrantyMonths() != null ? page.warrantyMonths() : warrantyMonths, page.checkedAt());
    }
}
