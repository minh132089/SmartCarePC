package com.smartcarepc.backend.service.price;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class OfferEvidenceTests {

    private static final String TITLE = "VGA ASUS Dual RTX 4060 8GB";
    private static final String URL = "https://shop.vn/products/asus-4060";

    @Test
    void giaXacMinhCanDungTrangDungBienTheVaChuaHetHang() {
        assertThat(OfferEvidence.verified(TITLE, offer(TITLE, 7_990_000L, true), true)).isTrue();
        assertThat(OfferEvidence.verified(TITLE, offer(TITLE, 7_990_000L, true), false)).isFalse();
        assertThat(OfferEvidence.verified(TITLE, offer("VGA ASUS Dual RTX 4060 Ti 8GB", 7_990_000L, true), true)).isFalse();
        assertThat(OfferEvidence.verified(TITLE, offer(TITLE, 7_990_000L, false), true)).isFalse();
        assertThat(OfferEvidence.verified(TITLE, offer(TITLE, null, true), true)).isFalse();
    }

    private Offer offer(String title, Long price, Boolean inStock) {
        return new Offer(title, "Shop", price, null, URL, null, inStock, 36, Instant.now());
    }
}
