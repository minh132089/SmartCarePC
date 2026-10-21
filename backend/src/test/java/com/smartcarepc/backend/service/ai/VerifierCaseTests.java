package com.smartcarepc.backend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.smartcarepc.backend.service.price.PriceEngine;
import com.smartcarepc.backend.service.price.Shops;

/**
 * Các ca kiểm thử hành vi ở mục 12 của docs/pc-build-verifier, áp lên phần app tự làm bằng code (đọc trang, chọn món
 * thay, kiểm tra lắp chung). Ca chỉ liên quan lời văn của AI được kiểm bằng lượt chạy thật, không ở đây.
 */
class VerifierCaseTests {

    @Test
    void t01TrangBanKhacKhongNhanLaDungSanPham() {
        assertThat(PriceEngine.samePage("Card Gigabyte RTX 4060 WINDFORCE OC 8G", "Card Gigabyte RTX 4060 Ti WINDFORCE OC 16G")).isFalse();
        assertThat(PriceEngine.samePage("SSD Samsung 990 EVO Plus 1TB", "SSD Samsung 990 EVO Plus 2TB")).isFalse();
    }

    @Test
    void t02Kit2x16KhongThayBangMotThanh32() {
        var kit = new ProductSearch.Product("RAM Corsair Vengeance 32GB (2x16GB) DDR5 6000", "GEARVN", 3_000_000L, "https://gearvn.com/kit", null);
        var single = new ProductSearch.Product("RAM Kingston Fury 32GB DDR5 6000", "GEARVN", 2_500_000L, "https://gearvn.com/one", null);
        assertThat(ChatService.candidates("ram", kit, List.of(single), true, "cheap")).isEmpty();
        // Dòng 2 thanh 16GB mua lẻ: thanh 32GB không thay được
        var stick = new ProductSearch.Product("RAM Kingston Fury 16GB DDR5 6000", "GEARVN", 1_500_000L, "https://gearvn.com/16", null);
        assertThat(ChatService.candidates("ram", stick, List.of(single), false, "cheap")).isEmpty();
    }

    @Test
    void t04TrangDanhMucKhongPhaiLinkMua() {
        assertThat(Shops.productLike("https://gearvn.com/collections/ram-pc")).isFalse();
        assertThat(Shops.productLike("https://gearvn.com/products/ram-kingston-fury-16gb-ddr5")).isTrue();
    }

    @Test
    void t12ThieuQvlKhongTuThanhLoi() {
        // Cùng nền tảng, chưa xác minh CPU Support List: UNKNOWN (cần hỏi), không phải ERROR
        var f = ProductRules.findings(Map.of("cpu", "Intel Core i3-12100", "motherboard", "MSI PRO H610M-E DDR4",
                "ram", "RAM Kingston 8GB DDR4 3200"));
        assertThat(f).noneSatisfy(x -> assertThat(x.severity()).isEqualTo(ProductRules.Severity.ERROR));
        assertThat(f).anySatisfy(x -> assertThat(x.severity()).isEqualTo(ProductRules.Severity.UNKNOWN));
    }

    @Test
    void t18CpuKhongDoHoaVaKhongCardBiChan() {
        assertThat(ProductRules.hasIgpu("Bộ vi xử lý Intel Core i5-12400F")).isFalse();
        // Có card rời thì không chặn; không có thì ChatService giữ lỗi required.gpu (xem ChatServiceTests)
        assertThat(ProductRules.hasIgpu("Bộ vi xử lý Intel Core i5-12400")).isTrue();
    }

    @Test
    void t23ComboKhongPhaiTrangLinhKienLe() {
        assertThat(PriceEngine.samePage("CPU Intel Core i5-12400F", "Combo CPU Intel Core i5-12400F + Mainboard B760M")).isFalse();
        assertThat(PriceEngine.samePage("CPU Intel Core i5-12400F", "Bộ PC Gaming i5 12400F RTX 4060")).isFalse();
    }
}
