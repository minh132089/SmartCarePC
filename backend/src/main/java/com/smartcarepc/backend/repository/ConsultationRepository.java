package com.smartcarepc.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcarepc.backend.entity.Consultation;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
}
