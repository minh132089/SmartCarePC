package com.smartcarepc.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcarepc.backend.entity.PartCategory;

public interface PartCategoryRepository extends JpaRepository<PartCategory, String> {

    List<PartCategory> findAllByOrderBySortOrderAsc();
}
