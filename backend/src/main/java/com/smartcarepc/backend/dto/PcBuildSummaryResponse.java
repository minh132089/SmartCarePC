package com.smartcarepc.backend.dto;

import com.smartcarepc.backend.entity.PcBuild;

/** Một ô trên thanh "Chọn kiểu máy" */
public record PcBuildSummaryResponse(Long id, String code, String name, String tagline, String thumbnailUrl) {

    public static PcBuildSummaryResponse from(PcBuild b) {
        return new PcBuildSummaryResponse(b.getId(), b.getCode(), b.getName(), b.getTagline(), b.getThumbnailUrl());
    }
}
