package com.smartcarepc.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Loại linh kiện, khớp PartId của frontend (src/data/parts.ts):
 * case, motherboard, cpu, cooler, ram, gpu, ssd, psu, fans, cables, tools.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "part_category")
public class PartCategory {

    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    /** Lời giải thích cho người không rành công nghệ */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String about;

    /** Có chế độ "Xem cận cảnh" không */
    @Builder.Default
    @Column(nullable = false)
    private boolean inspectable = false;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
