package com.smartcarepc.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Ghi công tác giả model 3D theo giấy phép CC BY (mục "Nguồn model 3D", src/data/credits.ts). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "model_credit")
public class ModelCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "part_label", nullable = false, length = 100)
    private String partLabel;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String license = "CC BY 4.0";
}
