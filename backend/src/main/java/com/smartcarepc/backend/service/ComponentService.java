package com.smartcarepc.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcarepc.backend.dto.ComponentResponse;
import com.smartcarepc.backend.exception.ResourceNotFoundException;
import com.smartcarepc.backend.repository.ComponentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComponentService {

    private final ComponentRepository componentRepository;

    /** category = null thì lấy tất cả */
    public List<ComponentResponse> findAll(String category) {
        var list = category == null
                ? componentRepository.findByActiveTrueOrderByIdAsc()
                : componentRepository.findByCategoryCodeAndActiveTrueOrderByIdAsc(category);
        return list.stream().map(ComponentResponse::from).toList();
    }

    public ComponentResponse findByCode(String code) {
        return componentRepository.findByCode(code)
                .map(ComponentResponse::withHotspots)
                .orElseThrow(() -> new ResourceNotFoundException("linh kiện", code));
    }
}
