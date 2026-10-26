package com.smartcarepc.backend.dto;

import com.smartcarepc.backend.entity.AssemblyStep;
import com.smartcarepc.backend.support.EnglishCopy;

public record AssemblyStepResponse(int stepNo, String key, String install, String remove) {

    public static AssemblyStepResponse from(AssemblyStep s) {
        return new AssemblyStepResponse(s.getStepNo(), s.getCategory().getCode(), EnglishCopy.text(s.getInstallText()), EnglishCopy.text(s.getRemoveText()));
    }
}
