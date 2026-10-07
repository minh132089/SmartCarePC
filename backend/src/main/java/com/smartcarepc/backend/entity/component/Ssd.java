package com.smartcarepc.backend.entity.component;

import com.smartcarepc.backend.entity.enums.SsdInterface;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Ổ SSD. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "ssd")
public class Ssd extends Component {

    @Enumerated(EnumType.STRING)
    @Column(name = "interface_type", nullable = false, length = 10)
    private SsdInterface interfaceType;

    /** Vd "2280" */
    @Column(name = "form_size", length = 10)
    private String formSize;

    @Column(name = "capacity_gb")
    private Integer capacityGb;
}
