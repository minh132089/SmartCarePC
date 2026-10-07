package com.smartcarepc.backend.entity.component;

import com.smartcarepc.backend.entity.enums.FormFactor;
import com.smartcarepc.backend.entity.enums.RamType;

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

/** Bo mạch chủ. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "motherboard")
public class Motherboard extends Component {

    @Enumerated(EnumType.STRING)
    @Column(name = "form_factor", nullable = false, length = 10)
    private FormFactor formFactor;

    /** Phải trùng socket của CPU */
    @Column(length = 30)
    private String socket;

    @Enumerated(EnumType.STRING)
    @Column(name = "ram_type", nullable = false, length = 10)
    private RamType ramType;

    @Column(name = "ram_slots", nullable = false)
    private Integer ramSlots;

    @Column(name = "pcie_x16_slots", nullable = false)
    private Integer pcieX16Slots;

    @Builder.Default
    @Column(name = "m2_slots", nullable = false)
    private int m2Slots = 1;

    @Builder.Default
    @Column(name = "sys_fan_headers", nullable = false)
    private int sysFanHeaders = 0;

    @Builder.Default
    @Column(name = "has_argb_header", nullable = false)
    private boolean hasArgbHeader = false;

    @Builder.Default
    @Column(name = "has_pump_header", nullable = false)
    private boolean hasPumpHeader = false;

    @Builder.Default
    @Column(name = "has_usb_c_header", nullable = false)
    private boolean hasUsbCHeader = false;
}
