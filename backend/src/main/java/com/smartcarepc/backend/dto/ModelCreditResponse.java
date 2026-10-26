package com.smartcarepc.backend.dto;

import com.smartcarepc.backend.entity.ModelCredit;
import com.smartcarepc.backend.support.EnglishCopy;

public record ModelCreditResponse(String part, String title, String author, String url, String license) {

    public static ModelCreditResponse from(ModelCredit c) {
        return new ModelCreditResponse(EnglishCopy.text(c.getPartLabel()), c.getTitle(), c.getAuthor(), c.getSourceUrl(), c.getLicense());
    }
}
