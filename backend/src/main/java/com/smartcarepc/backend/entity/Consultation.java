package com.smartcarepc.backend.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Giai đoạn sau: một lần hỏi AI tư vấn chọn máy theo nhu cầu (HANDOFF.md). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "consultation")
public class Consultation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** null = khách chưa đăng nhập */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    /** Văn phòng, học tập, gaming, đồ họa... */
    @Column(length = 100)
    private String purpose;

    @Column(precision = 12, scale = 0)
    private BigDecimal budget;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_build_id")
    private PcBuild suggestedBuild;
}
