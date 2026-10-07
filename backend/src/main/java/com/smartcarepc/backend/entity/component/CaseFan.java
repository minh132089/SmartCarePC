package com.smartcarepc.backend.entity.component;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Quạt case. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "case_fan")
public class CaseFan extends Component {

    /** 120 hoặc 140 */
    @Column(name = "size_mm", nullable = false)
    private Integer sizeMm;

    @Builder.Default
    @Column(nullable = false)
    private boolean argb = false;
}
