package com.smartcarepc.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcarepc.backend.entity.PcBuild;
import com.smartcarepc.backend.entity.enums.BuildType;

public interface PcBuildRepository extends JpaRepository<PcBuild, Long> {

    List<PcBuild> findByBuildTypeOrderBySortOrderAsc(BuildType buildType);

    /** Lấy luôn danh sách linh kiện trong một câu truy vấn */
    @EntityGraph(attributePaths = { "items", "items.component", "items.component.category" })
    Optional<PcBuild> findByCode(String code);

    List<PcBuild> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
