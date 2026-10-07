package com.smartcarepc.backend.entity.component;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Card đồ họa. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "gpu")
public class Gpu extends Component {

    @Column(name = "length_mm", nullable = false)
    private Integer lengthMm;

    @Column(name = "height_mm", nullable = false)
    private Integer heightMm;

    @Column(name = "thickness_mm", nullable = false)
    private Integer thicknessMm;

    @Column(name = "fan_count", nullable = false)
    private Integer fanCount;

    @Column(name = "tdp_w")
    private Integer tdpW;

    /** Vd "8-pin" */
    @Column(name = "power_connector", length = 30)
    private String powerConnector;
}
