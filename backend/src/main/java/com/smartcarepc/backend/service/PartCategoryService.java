package com.smartcarepc.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcarepc.backend.dto.PartCategoryResponse;
import com.smartcarepc.backend.repository.PartCategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartCategoryService {

    private final PartCategoryRepository partCategoryRepository;

    public List<PartCategoryResponse> findAll() {
        return partCategoryRepository.findAllByOrderBySortOrderAsc().stream().map(PartCategoryResponse::from).toList();
    }
}
