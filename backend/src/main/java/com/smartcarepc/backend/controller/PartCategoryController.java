package com.smartcarepc.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartcarepc.backend.dto.PartCategoryResponse;
import com.smartcarepc.backend.service.PartCategoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class PartCategoryController {

    private final PartCategoryService partCategoryService;

    /** Danh sách "Linh kiện trong máy" */
    @GetMapping
    public List<PartCategoryResponse> findAll() {
        return partCategoryService.findAll();
    }
}
