package com.smartcarepc.backend.entity.component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.smartcarepc.backend.entity.BaseEntity;
import com.smartcarepc.backend.entity.Hotspot;
import com.smartcarepc.backend.entity.PartCategory;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Lớp gốc của mọi linh kiện. Kế thừa kiểu JOINED: bảng "component" giữ cột chung,
 * mỗi lớp con (PcCase, Gpu, ...) có bảng riêng giữ thông số, nối với nhau bằng cùng id.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "component")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Component extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_code", nullable = false)
    private PartCategory category;

    /** Mã dùng trong frontend, vd gpu_280, tower_air_155 */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** File .glb trong public/models của frontend */
    @Column(name = "model_url")
    private String modelUrl;

    @Column(name = "image_url")
    private String imageUrl;

    /** Giá VND, null = chưa có giá */
    @Column(precision = 12, scale = 0)
    private BigDecimal price;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    /** Chấm chú thích "Xem cận cảnh" */
    @Builder.Default
    @OneToMany(mappedBy = "component", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder")
    private List<Hotspot> hotspots = new ArrayList<>();
}
