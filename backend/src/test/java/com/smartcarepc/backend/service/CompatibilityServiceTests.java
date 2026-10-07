package com.smartcarepc.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.smartcarepc.backend.dto.BuildCheckRequest;
import com.smartcarepc.backend.dto.BuildCheckRequest.Item;
import com.smartcarepc.backend.dto.CompatibilityResponse;
import com.smartcarepc.backend.dto.CompatibilityResponse.Issue;
import com.smartcarepc.backend.entity.component.CaseFan;
import com.smartcarepc.backend.entity.component.Component;
import com.smartcarepc.backend.entity.component.Cooler;
import com.smartcarepc.backend.entity.component.Cpu;
import com.smartcarepc.backend.entity.component.Gpu;
import com.smartcarepc.backend.entity.component.Motherboard;
import com.smartcarepc.backend.entity.component.PcCase;
import com.smartcarepc.backend.entity.component.Psu;
import com.smartcarepc.backend.entity.component.Ram;
import com.smartcarepc.backend.entity.component.Ssd;
import com.smartcarepc.backend.entity.enums.CoolerType;
import com.smartcarepc.backend.entity.enums.FormFactor;
import com.smartcarepc.backend.entity.enums.GpuMount;
import com.smartcarepc.backend.entity.enums.PsuType;
import com.smartcarepc.backend.entity.enums.RamType;
import com.smartcarepc.backend.entity.enums.SidePanel;
import com.smartcarepc.backend.entity.enums.SsdInterface;
import com.smartcarepc.backend.exception.ResourceNotFoundException;
import com.smartcarepc.backend.repository.ComponentRepository;
import com.smartcarepc.backend.repository.PcBuildRepository;

/** Kiểm tra từng quy tắc tương thích với linh kiện giống dữ liệu mẫu (DataInitializer). */
class CompatibilityServiceTests {

    private final List<Component> catalog = new ArrayList<>();
    private CompatibilityService service;

    @BeforeEach
    void setUp() {
        catalog.add(PcCase.builder().code("case_standard_tower").widthMm(230).heightMm(460).depthMm(430)
                .maxFormFactor(FormFactor.ATX).sidePanel(SidePanel.GLASS).psuType(PsuType.ATX)
                .maxGpuLengthMm(350).maxCoolerHeightMm(175).maxRadiatorMm(240).gpuMount(GpuMount.HORIZONTAL).fanSlots(3).build());
        catalog.add(PcCase.builder().code("case_compact_tower").widthMm(210).heightMm(380).depthMm(370)
                .maxFormFactor(FormFactor.MATX).sidePanel(SidePanel.SOLID).psuType(PsuType.ATX)
                .maxGpuLengthMm(280).maxCoolerHeightMm(160).gpuMount(GpuMount.HORIZONTAL).fanSlots(3).build());
        catalog.add(PcCase.builder().code("case_mini_sff").widthMm(180).heightMm(250).depthMm(350)
                .maxFormFactor(FormFactor.ITX).sidePanel(SidePanel.MESH).psuType(PsuType.SFX)
                .maxGpuLengthMm(260).maxCoolerHeightMm(55).gpuMount(GpuMount.SANDWICH_RISER).fanSlots(1).build());
        catalog.add(Motherboard.builder().code("mb_atx").formFactor(FormFactor.ATX).ramType(RamType.DDR4)
                .ramSlots(4).pcieX16Slots(3).hasArgbHeader(true).build());
        catalog.add(Motherboard.builder().code("mb_itx").formFactor(FormFactor.ITX).ramType(RamType.DDR4)
                .ramSlots(2).pcieX16Slots(1).build());
        catalog.add(Cpu.builder().code("cpu_std").build());
        catalog.add(Cooler.builder().code("tower_air_155").coolerType(CoolerType.TOWER_AIR).heightMm(155).build());
        catalog.add(Cooler.builder().code("low_profile_47").coolerType(CoolerType.LOW_PROFILE).heightMm(47).build());
        catalog.add(Cooler.builder().code("aio_240").coolerType(CoolerType.AIO).radiatorMm(240).build());
        catalog.add(Ram.builder().code("ram_ddr4").ramType(RamType.DDR4).build());
        catalog.add(Ram.builder().code("ram_ddr5").ramType(RamType.DDR5).build());
        catalog.add(Gpu.builder().code("gpu_280").lengthMm(280).heightMm(125).thicknessMm(55).fanCount(3).build());
        catalog.add(Gpu.builder().code("gpu_240").lengthMm(240).heightMm(120).thicknessMm(45).fanCount(2).build());
        catalog.add(Ssd.builder().code("ssd_m2_2280").interfaceType(SsdInterface.M2_NVME).build());
        catalog.add(Psu.builder().code("psu_atx").psuType(PsuType.ATX).build());
        catalog.add(Psu.builder().code("psu_sfx").psuType(PsuType.SFX).build());
        catalog.add(CaseFan.builder().code("fan120").sizeMm(120).build());

        ComponentRepository repo = mock(ComponentRepository.class);
        when(repo.findByCodeIn(any())).thenAnswer(inv -> {
            java.util.Collection<?> codes = inv.getArgument(0);
            return catalog.stream().filter(c -> codes.contains(c.getCode())).toList();
        });
        service = new CompatibilityService(repo, mock(PcBuildRepository.class));
    }

    /** "ram_ddr4:2" = 2 thanh RAM */
    private CompatibilityResponse check(String... codes) {
        List<Item> items = Arrays.stream(codes).map(s -> {
            String[] p = s.split(":");
            return new Item(p[0], p.length > 1 ? Integer.valueOf(p[1]) : null);
        }).toList();
        return service.check(new BuildCheckRequest(items));
    }

    private static List<String> rules(List<Issue> issues) {
        return issues.stream().map(Issue::rule).toList();
    }

    @Test
    void mayMiniSffMauLapDuoc() {
        var r = check("case_mini_sff", "mb_itx", "cpu_std", "low_profile_47", "ram_ddr4:2", "gpu_240", "ssd_m2_2280", "psu_sfx", "fan120");
        assertThat(r.compatible()).isTrue();
        assertThat(r.errors()).isEmpty();
    }

    @Test
    void boAtxKhongVuaVoMini() {
        var r = check("case_mini_sff", "mb_atx", "cpu_std", "low_profile_47", "ram_ddr4:2", "ssd_m2_2280", "psu_sfx");
        assertThat(r.compatible()).isFalse();
        assertThat(rules(r.errors())).contains("case.form_factor");
    }

    @Test
    void nguonAtxKhongVuaVoSfx() {
        var r = check("case_mini_sff", "mb_itx", "cpu_std", "low_profile_47", "ram_ddr4:2", "ssd_m2_2280", "psu_atx");
        assertThat(rules(r.errors())).contains("case.psu");
    }

    @Test
    void nguonSfxTrongVoAtxChiCanhBao() {
        var r = check("case_standard_tower", "mb_atx", "cpu_std", "tower_air_155", "ram_ddr4:2", "gpu_280", "ssd_m2_2280", "psu_sfx");
        assertThat(r.compatible()).isTrue();
        assertThat(rules(r.warnings())).contains("case.psu");
    }

    @Test
    void cardQuaDaiVaTanQuaCao() {
        var r = check("case_mini_sff", "mb_itx", "cpu_std", "tower_air_155", "ram_ddr4:2", "gpu_280", "ssd_m2_2280", "psu_sfx");
        assertThat(rules(r.errors())).contains("gpu.length", "cooler.height");
    }

    @Test
    void voKhongCoChoGanKetNuoc() {
        var r = check("case_compact_tower", "mb_itx", "cpu_std", "aio_240", "ram_ddr4:2", "ssd_m2_2280", "psu_atx");
        assertThat(rules(r.errors())).contains("cooler.radiator");
    }

    @Test
    void ramQuaSoKheVaSaiLoai() {
        var r = check("case_mini_sff", "mb_itx", "cpu_std", "low_profile_47", "ram_ddr5:3", "ssd_m2_2280", "psu_sfx");
        assertThat(rules(r.errors())).contains("ram.slots", "ram.type");
    }

    @Test
    void thieuLinhKienVaChonTrungVo() {
        var r = check("case_mini_sff", "case_compact_tower", "cpu_std");
        assertThat(rules(r.errors())).contains("duplicate.case", "required.motherboard", "required.psu", "required.ram", "required.ssd");
        assertThat(rules(r.warnings())).contains("cooler.missing");
    }

    @Test
    void quaNhieuQuat() {
        var r = check("case_mini_sff", "mb_itx", "cpu_std", "low_profile_47", "ram_ddr4:2", "gpu_240", "ssd_m2_2280", "psu_sfx", "fan120:2");
        assertThat(rules(r.errors())).containsExactly("fans.slots");
    }

    @Test
    void maLinhKienKhongTonTai() {
        assertThatThrownBy(() -> check("case_mini_sff", "khong_co")).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("khong_co");
    }
}
