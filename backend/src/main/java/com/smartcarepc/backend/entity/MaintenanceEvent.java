package com.smartcarepc.backend.entity;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** Sự kiện ẩn danh của thử nghiệm bảo trì; event_id chống trùng khi gửi lại. */
@Getter @Setter @SuperBuilder @NoArgsConstructor @Entity
@Table(name = "maintenance_event", indexes = {
    @Index(name = "uk_maintenance_event", columnList = "event_id", unique = true),
    @Index(name = "idx_maintenance_participant", columnList = "participant_code")})
public class MaintenanceEvent extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "event_id", nullable = false, length = 64, unique = true) private String eventId;
    @Column(name = "event_type", nullable = false, length = 40) private String eventType;
    @Column(nullable = false, length = 10) private String task;
    @Column(name = "participant_code", nullable = false, length = 20) private String participantCode;
    @Column(nullable = false, length = 10) private String phase;
    @Column(nullable = false, length = 60) private String machine;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(nullable = false, columnDefinition = "TEXT") private String details;
}
