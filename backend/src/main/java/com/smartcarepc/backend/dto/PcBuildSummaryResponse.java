package com.smartcarepc.backend.dto;

import com.smartcarepc.backend.entity.PcBuild;
import com.smartcarepc.backend.support.EnglishCopy;

/** Một ô trên thanh "Chọn kiểu máy" */
public record PcBuildSummaryResponse(Long id, String code, String name, String tagline, String thumbnailUrl) {

    public static PcBuildSummaryResponse from(PcBuild b) {
        return new PcBuildSummaryResponse(b.getId(), b.getCode(), EnglishCopy.text(b.getName()), EnglishCopy.text(b.getTagline()), b.getThumbnailUrl());
    }
}
