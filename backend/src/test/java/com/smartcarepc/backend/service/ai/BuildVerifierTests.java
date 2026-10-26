package com.smartcarepc.backend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.smartcarepc.backend.dto.ChatResponse.Line;
import com.smartcarepc.backend.dto.CompatibilityResponse.Issue;

/** Trạng thái phương án theo docs/pc-build-verifier mục 1.3 / 7.2 */
class BuildVerifierTests {

    @Test
    void duMoiGateThiSanSangMua() {
        var v = BuildVerifier.verify(full(), List.of(), List.of(), List.of(), 12_000_000L);
        assertThat(v.status()).isEqualTo("READY");
        assertThat(v.blockers()).isEmpty();
        assertThat(v.conditions()).isEmpty();
    }

    @Test
    void diemChuaXacMinhLaDieuKienCuThe() {
        // CPU Support List / gá tản: app chưa tự xác minh được, ghi thành việc hỏi cửa hàng, không chặn link mua
        var v = BuildVerifier.verify(full(), List.of(), List.of(),
                List.of(new Issue("CPU_SUPPORT_LIST", "Cần kiểm tra CPU có trong CPU Support List của bo.")), 12_000_000L);
        assertThat(v.status()).isEqualTo("CONDITIONAL");
        assertThat(v.purchase()).isEqualTo("READY");
        assertThat(v.conditions()).singleElement().asString().startsWith("Ask the store");
        // Cập nhật BIOS cũng là điều kiện
        var bios = BuildVerifier.verify(full(), List.of(), List.of(new Issue("CPU_BIOS_UPDATE", "Bo đời 600 có thể cần BIOS mới.")),
                List.of(), null);
        assertThat(bios.technical()).isEqualTo("CONDITIONAL");
    }

    @Test
    void thieuLinkHoacGiaLaChuaDuThongTin() {
        var lines = new ArrayList<>(full());
        lines.set(0, line("cpu", "Intel Core i3-12100", 2_890_000L, null, true, true));
        var v = BuildVerifier.verify(lines, List.of(), List.of(), List.of(), 12_000_000L);
        assertThat(v.status()).isEqualTo("HOLD");
        assertThat(v.blockers()).anySatisfy(b -> assertThat(b).contains("No purchase link"));
        var noPrice = new ArrayList<>(full());
        noPrice.set(1, line("ram", "RAM 8GB", null, "https://gearvn.com/products/ram", true, true));
        assertThat(BuildVerifier.verify(noPrice, List.of(), List.of(), List.of(), 12_000_000L).purchase()).isEqualTo("HOLD");
    }

    @Test
    void hetHangVuotTranHoacLoiLapLaChuaMuaDuoc() {
        var out = new ArrayList<>(full());
        out.set(2, line("ssd", "SSD 512GB", 1_200_000L, "https://gearvn.com/products/ssd", false, true));
        assertThat(BuildVerifier.verify(out, List.of(), List.of(), List.of(), 12_000_000L).status()).isEqualTo("BLOCKED");
        var over = BuildVerifier.verify(full(), List.of(), List.of(), List.of(), 5_000_000L);
        assertThat(over.status()).isEqualTo("BLOCKED");
        assertThat(over.blockers()).anySatisfy(b -> assertThat(b).contains("exceeds the limit"));
        // Ca T18: CPU không có đồ họa, không có card
        var noGpu = BuildVerifier.verify(full(), List.of(new Issue("required.gpu", "CPU này không có sẵn đồ họa, cần thêm card.")),
                List.of(), List.of(), 12_000_000L);
        assertThat(noGpu.technical()).isEqualTo("BLOCKED");
    }

    @Test
    void giaChuaDocTrenTrangThiCanXemLai() {
        var lines = new ArrayList<>(full());
        lines.set(3, line("psu", "Nguồn 450W", 450_000L, "https://hacom.vn/nguon", true, false));
        var v = BuildVerifier.verify(lines, List.of(), List.of(), List.of(), 12_000_000L);
        assertThat(v.purchase()).isEqualTo("CONDITIONAL");
        assertThat(v.conditions()).anySatisfy(c -> assertThat(c).contains("Recheck the price"));
    }

    @Test
    void danhSachTrongKhongPhaiLaKhongCoLoi() {
        // Ca T28: không có dòng nào thì không được READY
        var v = BuildVerifier.verify(List.of(), List.of(), List.of(), List.of(), 12_000_000L);
        assertThat(v.status()).isNotEqualTo("READY");
        assertThat(v.blockers()).anySatisfy(b -> assertThat(b).contains("missing CPU"));
    }

    private static List<Line> full() {
        return List.of(
                line("cpu", "Intel Core i3-12100", 2_890_000L, "https://gearvn.com/products/i3", true, true),
                line("ram", "RAM 8GB DDR4", 1_250_000L, "https://gearvn.com/products/ram", true, true),
                line("ssd", "SSD 512GB", 1_200_000L, "https://gearvn.com/products/ssd", true, true),
                line("psu", "Nguồn 450W", 450_000L, "https://hacom.vn/nguon", true, true),
                line("motherboard", "Mainboard H610M DDR4", 1_550_000L, "https://gearvn.com/products/mb", true, true),
                line("case", "Vỏ mATX", 290_000L, "https://gearvn.com/products/case", true, true));
    }

    private static Line line(String category, String name, Long price, String url, Boolean inStock, Boolean checked) {
        return new Line(category, category, category, 1, null, null, price, price, null, url, name, "GEARVN", inStock, null,
                null, List.of(), checked);
    }
}
