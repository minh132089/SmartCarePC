package com.smartcarepc.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcarepc.backend.entity.Hotspot;

public interface HotspotRepository extends JpaRepository<Hotspot, Long> {
}
