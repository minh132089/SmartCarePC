package com.smartcarepc.backend.service.price;

/** Một giá chỉ được gọi là đã xác minh khi đọc từ trang sản phẩm đúng biến thể còn hiệu lực. */
public final class OfferEvidence {

    private OfferEvidence() {
    }

    public static boolean verified(String expectedTitle, Offer offer, boolean pageChecked) {
        return pageChecked && offer != null && offer.priceVnd() != null && offer.priceVnd() > 0
                && offer.priceVnd() <= PriceEngine.MAX_PRICE
                && Boolean.TRUE.equals(offer.inStock()) && PriceEngine.samePage(expectedTitle, offer.title());
    }
}
