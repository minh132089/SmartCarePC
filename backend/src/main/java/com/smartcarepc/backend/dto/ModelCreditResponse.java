package com.smartcarepc.backend.dto;

import com.smartcarepc.backend.entity.ModelCredit;

public record ModelCreditResponse(String part, String title, String author, String url, String license) {

    public static ModelCreditResponse from(ModelCredit c) {
        return new ModelCreditResponse(c.getPartLabel(), c.getTitle(), c.getAuthor(), c.getSourceUrl(), c.getLicense());
    }
}
