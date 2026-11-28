package com.smartcarepc.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smartcarepc.backend.entity.EmailReminder;

public interface EmailReminderRepository extends JpaRepository<EmailReminder, Long> {
    Optional<EmailReminder> findByManageToken(String token);
    Optional<EmailReminder> findByConfirmToken(String token);
    long countByEmailAndCreatedAtAfter(String email, LocalDateTime after);
    List<EmailReminder> findByConfirmedTrue();
    List<EmailReminder> findByEmailAndConfirmedTrue(String email);
    void deleteByConfirmedFalseAndCreatedAtBefore(LocalDateTime before);
}
