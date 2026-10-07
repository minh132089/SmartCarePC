package com.smartcarepc.backend.entity.component;

import com.smartcarepc.backend.entity.enums.PsuType;

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

/** Bộ nguồn. Phải cùng loại ATX / SFX với vỏ. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "psu")
public class Psu extends Component {

    @Enumerated(EnumType.STRING)
    @Column(name = "psu_type", nullable = false, length = 10)
    private PsuType psuType;

    private Integer wattage;

    /** Vd "80+ Bronze" */
    @Column(length = 20)
    private String efficiency;

    @Builder.Default
    @Column(nullable = false)
    private boolean modular = false;
}
