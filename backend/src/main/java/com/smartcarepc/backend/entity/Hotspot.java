package com.smartcarepc.backend.entity;

import java.math.BigDecimal;

import com.smartcarepc.backend.entity.component.Component;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Chấm chú thích trong "Xem cận cảnh" (src/data/*Hotspots.ts).
 * Toạ độ theo gốc của model, đơn vị app (1 = 100 mm).
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "hotspot")
public class Hotspot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id", nullable = false)
    private Component component;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "pos_x", nullable = false, precision = 8, scale = 4)
    private BigDecimal posX;

    @Column(name = "pos_y", nullable = false, precision = 8, scale = 4)
    private BigDecimal posY;

    @Column(name = "pos_z", nullable = false, precision = 8, scale = 4)
    private BigDecimal posZ;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
