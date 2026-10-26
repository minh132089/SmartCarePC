package com.smartcarepc.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import com.smartcarepc.backend.dto.ChatResponse.Line;
import com.smartcarepc.backend.support.EnglishCopy;

class EnglishCopyTests {
    @Test
    void legacyLabelsAndShopDescriptorsAreEnglishWithoutChangingPurchaseData() {
        assertThat(EnglishCopy.text("Bo mạch chủ")).isEqualTo("Motherboard");
        var line = new Line("gpu_240", "gpu", "Card đồ họa (GPU)", 1, "6GB", null,
                4_500_000L, 4_500_000L, null, "https://store.test/asus-3050",
                "Card màn hình ASUS Dual GeForce RTX 3050 OC 6GB GDDR6 Chính Hãng", "Store");
        assertThat(line.productName()).isEqualTo("Graphics card ASUS Dual GeForce RTX 3050 OC 6GB GDDR6 Official");
        assertThat(line.code()).isEqualTo("gpu_240");
        assertThat(line.sourceUrl()).isEqualTo("https://store.test/asus-3050");
        assertThat(line.priceMaxVnd()).isEqualTo(4_500_000L);
        assertThat(line.quantity()).isEqualTo(1);
        assertThat(EnglishCopy.productName("CPU Intel Core i5 12400F (6 nhân 12 luồng), Đen"))
                .isEqualTo("CPU Intel Core i5 12400F (6 cores 12 threads), Black");
    }
}
