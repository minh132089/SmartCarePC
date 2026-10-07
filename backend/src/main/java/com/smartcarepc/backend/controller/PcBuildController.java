package com.smartcarepc.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartcarepc.backend.dto.BuildCheckRequest;
import com.smartcarepc.backend.dto.CompatibilityResponse;
import com.smartcarepc.backend.dto.PcBuildResponse;
import com.smartcarepc.backend.dto.PcBuildSummaryResponse;
import com.smartcarepc.backend.service.CompatibilityService;
import com.smartcarepc.backend.service.PcBuildService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/builds")
@RequiredArgsConstructor
public class PcBuildController {

    private final PcBuildService pcBuildService;
    private final CompatibilityService compatibilityService;

    /** Các ô trên thanh "Chọn kiểu máy" */
    @GetMapping
    public List<PcBuildSummaryResponse> findPresets() {
        return pcBuildService.findPresets();
    }

    /** GET /api/builds/standard_tower: một máy kèm toàn bộ linh kiện */
    @GetMapping("/{code}")
    public PcBuildResponse findByCode(@PathVariable String code) {
        return pcBuildService.findByCode(code);
    }

    /** POST /api/builds/check: kiểm tra các linh kiện người dùng đang chọn có lắp chung được không */
    @PostMapping("/check")
    public CompatibilityResponse check(@Valid @RequestBody BuildCheckRequest request) {
        return compatibilityService.check(request);
    }

    /** GET /api/builds/mini_sff/check: kiểm tra một máy có sẵn */
    @GetMapping("/{code}/check")
    public CompatibilityResponse checkBuild(@PathVariable String code) {
        return compatibilityService.checkBuild(code);
    }
}
