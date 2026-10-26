package com.smartcarepc.backend.dto;

import com.smartcarepc.backend.entity.PartCategory;
import com.smartcarepc.backend.support.EnglishCopy;

public record PartCategoryResponse(String code, String name, String about, boolean inspectable) {

    public static PartCategoryResponse from(PartCategory c) {
        return new PartCategoryResponse(c.getCode(), EnglishCopy.text(c.getName()), EnglishCopy.text(c.getAbout()), c.isInspectable());
    }
}
