package com.smartcarepc.backend.entity.component;

import java.util.HashSet;
import java.util.Set;

import com.smartcarepc.backend.entity.enums.CoolerType;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Tản nhiệt CPU: tản khí hoặc tản nước AIO. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "cooler")
public class Cooler extends Component {

    @Enumerated(EnumType.STRING)
    @Column(name = "cooler_type", nullable = false, length = 20)
    private CoolerType coolerType;

    /** Tản khí: chiều cao tính từ mặt CPU */
    @Column(name = "height_mm")
    private Integer heightMm;

    /** Tản nước: cỡ két 240, 360 */
    @Column(name = "radiator_mm")
    private Integer radiatorMm;

    @Builder.Default
    @Column(name = "fan_count", nullable = false)
    private int fanCount = 1;

    @Column(name = "max_tdp_w")
    private Integer maxTdpW;

    /** Các socket tản gá được, lưu ở bảng phụ cooler_socket */
    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "cooler_socket", joinColumns = @JoinColumn(name = "cooler_id"))
    @Column(name = "socket", length = 30)
    private Set<String> supportedSockets = new HashSet<>();
}
