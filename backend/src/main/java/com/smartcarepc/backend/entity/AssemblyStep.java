package com.smartcarepc.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Thứ tự lắp máy (src/data/assembly.ts). "Tách rời" chạy ngược từ bước cuối. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "assembly_step")
public class AssemblyStep {

    @Id
    @Column(name = "step_no")
    private Integer stepNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_code", nullable = false)
    private PartCategory category;

    @Column(name = "install_text", nullable = false, length = 150)
    private String installText;

    @Column(name = "remove_text", nullable = false, length = 150)
    private String removeText;
}
