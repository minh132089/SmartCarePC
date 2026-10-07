package com.smartcarepc.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcarepc.backend.entity.AssemblyStep;

public interface AssemblyStepRepository extends JpaRepository<AssemblyStep, Integer> {
}
