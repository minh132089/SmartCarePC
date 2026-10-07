package com.smartcarepc.backend.entity.component;

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

/** Một thanh RAM. Số thanh trong máy nằm ở PcBuildItem.quantity. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "ram")
public class Ram extends Component {

    @Enumerated(EnumType.STRING)
    @Column(name = "ram_type", nullable = false, length = 10)
    private RamType ramType;

    /** Dung lượng một thanh */
    @Column(name = "capacity_gb")
    private Integer capacityGb;

    @Column(name = "speed_mts")
    private Integer speedMts;

    @Column(name = "height_mm")
    private Integer heightMm;

    @Builder.Default
    @Column(name = "has_heatsink", nullable = false)
    private boolean hasHeatsink = false;
}
