package com.smartcarepc.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.smartcarepc.backend.dto.BuildCheckRequest;
import com.smartcarepc.backend.dto.BuildCheckRequest.Item;
import com.smartcarepc.backend.dto.CompatibilityResponse;
import com.smartcarepc.backend.dto.CompatibilityResponse.Issue;
import com.smartcarepc.backend.dto.ComponentResponse;
import com.smartcarepc.backend.dto.PcBuildResponse;
import com.smartcarepc.backend.dto.PcBuildSummaryResponse;
import com.smartcarepc.backend.entity.enums.BuildType;
import com.smartcarepc.backend.exception.ResourceNotFoundException;

/** Cấu hình trợ lý chọn phải khớp máy mẫu 3D của vỏ, với hai máy mẫu giống dữ liệu thật (DataInitializer). */
class ModelMatchServiceTests {

    private final PcBuildService builds = mock(PcBuildService.class);
    private final ComponentService components = mock(ComponentService.class);
    private final List<ComponentResponse> catalog = new ArrayList<>();
    private ModelMatchService service;

    @BeforeEach
    void setUp() {
        for (String[] p : new String[][] {
                { "case_standard_tower", "case", "Vỏ Tower tiêu chuẩn" }, { "case_gaming", "case", "Vỏ Gaming" },
                { "case_custom", "case", "Vỏ chưa có mô hình" },
                { "mb_atx", "motherboard", "Bo mạch ATX" }, { "mb_matx", "motherboard", "Bo mạch Micro-ATX" },
                { "cpu_std", "cpu", "CPU" }, { "tower_air_155", "cooler", "Tản khí tháp" }, { "aio_240", "cooler", "Tản nước 240" },
                { "gpu_280", "gpu", "Card dài 280" }, { "gpu_240", "gpu", "Card 240" }, { "psu_atx", "psu", "Nguồn ATX" },
                { "ram_ddr4", "ram", "RAM DDR4" }, { "ssd_m2_2280", "ssd", "SSD M.2" }, { "fan120", "fans", "Quạt 120" } }) {
            catalog.add(new ComponentResponse(1L, p[0], p[1], p[2], null, null, null, null, Map.of(), List.of()));
        }
        when(components.findAll(null)).thenReturn(catalog);
        when(builds.findPresets()).thenReturn(List.of(
                new PcBuildSummaryResponse(1L, "standard_tower", "Tower tiêu chuẩn", "", null),
                new PcBuildSummaryResponse(6L, "gaming", "Gaming", "", null)));
        doThrow(new ResourceNotFoundException("bộ máy", "?")).when(builds).findByCode(anyString());
        model("standard_tower", "case_standard_tower", "mb_atx", "cpu_std", "tower_air_155", "gpu_280", "psu_atx",
                "2:ram_ddr4", "ssd_m2_2280", "3:fan120");
        model("gaming", "case_gaming", "mb_atx", "cpu_std", "aio_240", "gpu_280", "psu_atx", "2:ram_ddr4", "ssd_m2_2280",
                "4:fan120");
        service = new ModelMatchService(builds, components);
    }

    @Test
    void dungMayMauThiKhongLoiVaKhongGhiChu() {
        var r = service.check(request("case_standard_tower", "mb_atx", "cpu_std", "tower_air_155", "gpu_280", "psu_atx",
                "2:ram_ddr4", "ssd_m2_2280", "3:fan120"));
        assertThat(r.errors()).isEmpty();
        assertThat(r.note()).isNull();
    }

    @Test
    void tanKhiTrongVoGamingThiBaoLoiVaGoiYVoKhac() {
        // Ca thật: hướng dẫn của vỏ Gaming dạy lắp tản nước trong khi người dùng mua tản khí
        var r = service.check(request("case_gaming", "mb_matx", "cpu_std", "tower_air_155", "gpu_280", "psu_atx",
                "1:ram_ddr4", "ssd_m2_2280"));
        assertThat(r.errors()).extracting(Issue::rule).containsExactly("model.motherboard", "model.cooler");
        assertThat(r.errors().get(1).message()).contains("Tản nước 240").contains("Tản khí tháp")
                .contains("case_standard_tower");
        assertThat(r.note()).contains("2 thanh RAM").contains("4 quạt").contains("1 thanh RAM").contains("không lắp thêm quạt");
    }

    @Test
    void khongCoCardRoiThiChiGhiChuBoQuaBuocLapCard() {
        // Máy học / văn phòng dùng đồ họa tích hợp: không bắt thêm card của máy mẫu (CPU không có đồ họa thì
        // kiểm tra tương thích báo lỗi riêng)
        var r = service.check(request("case_standard_tower", "mb_atx", "cpu_std", "tower_air_155", "psu_atx",
                "2:ram_ddr4", "ssd_m2_2280", "3:fan120"));
        assertThat(r.errors()).isEmpty();
        assertThat(r.note()).contains("đồ họa tích hợp").contains("bỏ qua bước lắp card");
    }

    @Test
    void cardKhacMayMauVanBaoLoi() {
        var r = service.check(request("case_standard_tower", "mb_atx", "cpu_std", "tower_air_155", "gpu_240", "psu_atx",
                "2:ram_ddr4", "ssd_m2_2280", "3:fan120"));
        assertThat(r.errors()).extracting(Issue::rule).containsExactly("model.gpu");
    }

    @Test
    void voChuaCoMoHinhHoacThieuVoThiKhongSo() {
        assertThat(service.check(request("case_custom", "mb_matx", "aio_240")).errors()).isEmpty();
        assertThat(service.check(request("mb_matx", "aio_240")).errors()).isEmpty();
    }

    @Test
    void gopLoiVaoKetQuaTuongThich() {
        var ok = new CompatibilityResponse(true, List.of(), List.of(new Issue("w", "cảnh báo")));
        var merged = ModelMatchService.merge(ok, List.of(new Issue("model.cooler", "lệch")));
        assertThat(merged.compatible()).isFalse();
        assertThat(merged.errors()).extracting(Issue::rule).containsExactly("model.cooler");
        assertThat(merged.warnings()).hasSize(1);
        assertThat(ModelMatchService.merge(ok, List.of())).isSameAs(ok);
    }

    @Test
    void timVoMauKhopMonDaChon() {
        // Ca thật: vỏ Gaming (tản nước) nhưng chọn tản khí tháp: vỏ Tower tiêu chuẩn dựng đúng các món này
        assertThat(service.matchingCase(request("case_gaming", "mb_atx", "cpu_std", "tower_air_155", "gpu_280", "psu_atx")))
                .contains("case_standard_tower");
        // Không có card rời vẫn tìm được
        assertThat(service.matchingCase(request("case_gaming", "mb_atx", "cpu_std", "tower_air_155", "psu_atx")))
                .contains("case_standard_tower");
        // Không vỏ nào khớp: rỗng
        assertThat(service.matchingCase(request("case_gaming", "mb_matx", "cpu_std", "aio_240", "gpu_280", "psu_atx"))).isEmpty();
        assertThat(service.fixedCodes("case_gaming")).containsEntry("cooler", "aio_240").containsEntry("motherboard", "mb_atx");
    }

    @Test
    void lietKeMonDiKemTungVoChoTroLy() {
        assertThat(service.presetParts())
                .contains("[case_standard_tower] Tower tiêu chuẩn: bo mạch Bo mạch ATX [mb_atx], tản nhiệt Tản khí tháp [tower_air_155]")
                .contains("[case_gaming] Gaming:").contains("tản nhiệt Tản nước 240 [aio_240]");
    }

    /** Máy mẫu: mỗi phần tử là mã linh kiện, có thể kèm số lượng dạng "2:ram_ddr4" */
    private void model(String code, String... parts) {
        List<PcBuildResponse.Item> items = Arrays.stream(parts).map(p -> {
            String[] q = p.contains(":") ? p.split(":") : new String[] { "1", p };
            return new PcBuildResponse.Item(Integer.parseInt(q[0]), find(q[1]));
        }).toList();
        doReturn(new PcBuildResponse(1L, code, code, "", BuildType.PRESET, null, items)).when(builds).findByCode(code);
    }

    private static BuildCheckRequest request(String... parts) {
        return new BuildCheckRequest(Arrays.stream(parts).map(p -> {
            String[] q = p.contains(":") ? p.split(":") : new String[] { "1", p };
            return new Item(q[1], Integer.parseInt(q[0]));
        }).toList());
    }

    private ComponentResponse find(String code) {
        return catalog.stream().filter(c -> c.code().equals(code)).findFirst().orElseThrow();
    }
}
