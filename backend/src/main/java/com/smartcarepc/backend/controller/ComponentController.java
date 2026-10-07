package com.smartcarepc.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartcarepc.backend.dto.ComponentResponse;
import com.smartcarepc.backend.service.ComponentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
public class ComponentController {

    private final ComponentService componentService;

    /** GET /api/components hoặc /api/components?category=gpu */
    @GetMapping
    public List<ComponentResponse> findAll(@RequestParam(required = false) String category) {
        return componentService.findAll(category);
    }

    /** GET /api/components/gpu_280: chi tiết kèm chú thích "Xem cận cảnh" */
    @GetMapping("/{code}")
    public ComponentResponse findByCode(@PathVariable String code) {
        return componentService.findByCode(code);
    }
}
