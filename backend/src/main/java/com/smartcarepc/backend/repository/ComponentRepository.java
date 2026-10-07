package com.smartcarepc.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcarepc.backend.entity.component.Component;

/** Truy vấn trên lớp gốc Component trả về đúng lớp con (PcCase, Gpu, ...) nhờ kế thừa JOINED. */
public interface ComponentRepository extends JpaRepository<Component, Long> {

    @EntityGraph(attributePaths = "category")
    List<Component> findByActiveTrueOrderByIdAsc();

    @EntityGraph(attributePaths = "category")
    List<Component> findByCategoryCodeAndActiveTrueOrderByIdAsc(String categoryCode);

    @EntityGraph(attributePaths = { "category", "hotspots" })
    Optional<Component> findByCode(String code);

    @EntityGraph(attributePaths = "category")
    List<Component> findByCodeIn(Collection<String> codes);
}
