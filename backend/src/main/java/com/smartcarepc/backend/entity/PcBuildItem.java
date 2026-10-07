package com.smartcarepc.backend.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Một linh kiện trong một bộ máy, kèm số lượng (vd 2 thanh RAM, 3 quạt). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "pc_build_item", uniqueConstraints = @UniqueConstraint(columnNames = { "build_id", "component_id" }))
public class PcBuildItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "build_id", nullable = false)
    private PcBuild pcBuild;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id", nullable = false)
    private Component component;

    @Builder.Default
    @Column(nullable = false)
    private int quantity = 1;
}
