package com.smartcarepc.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcarepc.backend.dto.PcBuildResponse;
import com.smartcarepc.backend.dto.PcBuildSummaryResponse;
import com.smartcarepc.backend.entity.enums.BuildType;
import com.smartcarepc.backend.exception.ResourceNotFoundException;
import com.smartcarepc.backend.repository.PcBuildRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PcBuildService {

    private final PcBuildRepository pcBuildRepository;

    /** Các máy có sẵn trên thanh "Chọn kiểu máy" */
    public List<PcBuildSummaryResponse> findPresets() {
        return pcBuildRepository.findByBuildTypeOrderBySortOrderAsc(BuildType.PRESET).stream()
                .map(PcBuildSummaryResponse::from)
                .toList();
    }

    public PcBuildResponse findByCode(String code) {
        return pcBuildRepository.findByCode(code)
                .map(PcBuildResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("PC build", code));
    }
}
