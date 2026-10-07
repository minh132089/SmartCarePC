package com.smartcarepc.backend.entity.component;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** CPU (bộ xử lý). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "cpu")
public class Cpu extends Component {

    @Column(length = 30)
    private String socket;

    private Integer cores;

    private Integer threads;

    @Column(name = "base_ghz", precision = 3, scale = 1)
    private BigDecimal baseGhz;

    @Column(name = "boost_ghz", precision = 3, scale = 1)
    private BigDecimal boostGhz;

    @Column(name = "tdp_w")
    private Integer tdpW;

    /** Có sẵn đồ họa thì không bắt buộc card rời */
    @Builder.Default
    @Column(name = "has_igpu", nullable = false)
    private boolean hasIgpu = false;
}
