package com.smartcarepc.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcarepc.backend.entity.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findAllByOrderByIdAsc();

    /** Bản đã lưu có cùng mã lần gửi: chống lưu trùng khi gửi lại */
    java.util.Optional<Feedback> findFirstBySubmissionId(String submissionId);
}
