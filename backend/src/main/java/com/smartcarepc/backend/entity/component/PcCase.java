package com.smartcarepc.backend.entity.component;

import com.smartcarepc.backend.entity.enums.FormFactor;
import com.smartcarepc.backend.entity.enums.GpuMount;
import com.smartcarepc.backend.entity.enums.PsuType;
import com.smartcarepc.backend.entity.enums.SidePanel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Vỏ máy. Các cột max* là giới hạn lắp đặt (design/families/00_families_spec.md),
 * dùng để kiểm tra linh kiện có vừa vỏ không. Giá trị null = chưa có số liệu.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "pc_case")
public class PcCase extends Component {

    @Column(name = "width_mm", nullable = false)
    private Integer widthMm;

    @Column(name = "height_mm", nullable = false)
    private Integer heightMm;

    @Column(name = "depth_mm", nullable = false)
    private Integer depthMm;

    /** Bo lớn nhất lắp được */
    @Enumerated(EnumType.STRING)
    @Column(name = "max_form_factor", nullable = false, length = 10)
    private FormFactor maxFormFactor;

    @Enumerated(EnumType.STRING)
    @Column(name = "side_panel", nullable = false, length = 10)
    private SidePanel sidePanel;

    @Enumerated(EnumType.STRING)
    @Column(name = "psu_type", nullable = false, length = 10)
    private PsuType psuType;

    @Column(name = "max_gpu_length_mm")
    private Integer maxGpuLengthMm;

    /** Tản khí cao tối đa, tính từ mặt CPU */
    @Column(name = "max_cooler_height_mm")
    private Integer maxCoolerHeightMm;

    /** Két tản nước lớn nhất (240, 360); null = không lắp được tản nước */
    @Column(name = "max_radiator_mm")
    private Integer maxRadiatorMm;

    @Enumerated(EnumType.STRING)
    @Column(name = "gpu_mount", nullable = false, length = 20)
    private GpuMount gpuMount;

    @Builder.Default
    @Column(name = "fan_slots", nullable = false)
    private int fanSlots = 0;

    @Builder.Default
    @Column(name = "has_fan_hub", nullable = false)
    private boolean hasFanHub = false;
}
