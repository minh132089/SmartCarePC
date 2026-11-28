package com.smartcarepc.backend.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Đăng ký nhận email nhắc bảo dưỡng. Server không kiểm tra đăng nhập, nên chỉ gửi nhắc khi chủ hộp thư đã bấm link xác nhận
 * (confirmed). manage_token nằm ở trình duyệt đã bật, để cập nhật ngày hẹn và tắt. Không lưu mã người tham gia nghiên cứu
 * cạnh email, để dữ liệu nghiên cứu vẫn ẩn danh.
 */
@Getter @Setter @SuperBuilder @NoArgsConstructor @Entity
@Table(name = "email_reminder", indexes = {
    @Index(name = "uk_email_reminder_manage", columnList = "manage_token", unique = true),
    @Index(name = "uk_email_reminder_confirm", columnList = "confirm_token", unique = true),
    @Index(name = "idx_email_reminder_email", columnList = "email")})
public class EmailReminder extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 150) private String email;
    @Column(name = "manage_token", nullable = false, length = 64) private String manageToken;
    @Column(name = "confirm_token", nullable = false, length = 64) private String confirmToken;
    @Column(nullable = false) private boolean confirmed;
    @Column(name = "confirmed_at") private LocalDateTime confirmedAt;
    /** Địa chỉ web để link trong thư trỏ về (chỉ nhận các địa chỉ trong app.reminders.origins) */
    @Column(nullable = false, length = 60) private String origin;
    /** Ngày hẹn từng việc, vd {"dust":"2026-12-05","check":"2026-12-28"} */
    @Column(nullable = false, columnDefinition = "TEXT") private String dates;
    /** Chu kỳ lặp từng việc (tháng), vd {"dust":6,"paste":30,"check":1}. Server tự nhắc lần sau theo chu kỳ này */
    @Column(columnDefinition = "TEXT") private String months;
    /** Đã gửi nhắc cho ngày hẹn nào, vd {"dust":{"due":"2026-12-05","on":"2026-12-05","n":1}} */
    @Column(nullable = false, columnDefinition = "TEXT") private String sent;
}
