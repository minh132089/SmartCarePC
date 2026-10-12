package com.smartcarepc.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Một lần gửi form phản hồi của người tham gia thử nghiệm. Chỉ gắn với mã người tham gia (vd P07),
 * không lưu tên hay số điện thoại. Câu trả lời để dạng JSON để thêm hoặc sửa câu hỏi không phải đổi bảng.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "feedback", indexes = @Index(name = "idx_feedback_participant", columnList = "participant_code"))
public class Feedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** null = người không tham gia thử nghiệm */
    @Column(name = "participant_code", length = 20)
    private String participantCode;

    /** before = lần đầu; after = sau 4 tuần */
    @Column(length = 10)
    private String phase;

    /** Máy đang xem lúc gửi (mã họ case hoặc gaming) */
    @Column(length = 60)
    private String machine;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answers;
}
