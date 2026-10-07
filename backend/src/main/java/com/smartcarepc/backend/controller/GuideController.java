package com.smartcarepc.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartcarepc.backend.dto.AssemblyStepResponse;
import com.smartcarepc.backend.dto.ModelCreditResponse;
import com.smartcarepc.backend.service.GuideService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GuideController {

    private final GuideService guideService;

    /** Các bước "Tách rời / Lắp lại" */
    @GetMapping("/assembly-steps")
    public List<AssemblyStepResponse> findAssemblySteps() {
        return guideService.findAssemblySteps();
    }

    /** Mục "Nguồn model 3D" */
    @GetMapping("/credits")
    public List<ModelCreditResponse> findCredits() {
        return guideService.findCredits();
    }
}
