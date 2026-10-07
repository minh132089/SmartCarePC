package com.smartcarepc.backend.entity;

import java.util.ArrayList;
import java.util.List;

import com.smartcarepc.backend.entity.component.Component;
import com.smartcarepc.backend.entity.enums.BuildType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Một bộ máy hoàn chỉnh. PRESET = các máy trên thanh "Chọn kiểu máy" (code khớp FamilyId / "gaming"),
 * USER = máy người dùng tự lắp trong PC Builder.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "pc_build")
public class PcBuild extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã preset, vd standard_tower; null với máy của người dùng */
    @Column(unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    /** Câu giới thiệu hiện dưới thanh chọn kiểu máy */
    private String tagline;

    @Enumerated(EnumType.STRING)
    @Column(name = "build_type", nullable = false, length = 10)
    private BuildType buildType;

    /** Chủ của máy USER; null với PRESET */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Builder.Default
    @OneToMany(mappedBy = "pcBuild", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PcBuildItem> items = new ArrayList<>();

    /** Thêm linh kiện vào máy, giữ đúng hai chiều của quan hệ */
    public PcBuild addItem(Component component, int quantity) {
        items.add(PcBuildItem.builder().pcBuild(this).component(component).quantity(quantity).build());
        return this;
    }
}
