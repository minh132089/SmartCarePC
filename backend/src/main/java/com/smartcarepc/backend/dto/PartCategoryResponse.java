package com.smartcarepc.backend.dto;

import com.smartcarepc.backend.entity.PartCategory;

public record PartCategoryResponse(String code, String name, String about, boolean inspectable) {

    public static PartCategoryResponse from(PartCategory c) {
        return new PartCategoryResponse(c.getCode(), c.getName(), c.getAbout(), c.isInspectable());
    }
}
