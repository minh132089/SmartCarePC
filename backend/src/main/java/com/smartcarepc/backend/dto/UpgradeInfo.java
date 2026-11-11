package com.smartcarepc.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Máy hiện tại của người muốn nâng cấp, nhập theo từng ô ở trang Nâng cấp. Ô bỏ trống là "không biết".
 * gpu = "none" khi máy không có card rời. caseSize: mini / compact / standard / large / unknown.
 * purpose là mục đích bằng lời của người dùng, budgetVnd là số tiền dành cho phần nâng cấp (không phải cả máy).
 */
public record UpgradeInfo(
        @Size(max = 160) String cpu,
        @Size(max = 160) String motherboard,
        @Size(max = 160) String ram,
        @Size(max = 160) String gpu,
        @Size(max = 160) String storage,
        @Size(max = 160) String psu,
        @Pattern(regexp = "mini|compact|standard|large|unknown", message = "caseSize must be mini, compact, standard, large or unknown") String caseSize,
        @NotBlank(message = "Tell us what you mainly use the PC for") @Size(max = 600) String purpose,
        @NotNull(message = "Enter your upgrade budget") @Min(value = 300_000, message = "The upgrade budget must be at least VND 300,000")
        @Max(value = 200_000_000, message = "The upgrade budget is too large") Long budgetVnd) {

    /** Ô có thông tin thật (không trống, không ghi "không biết") */
    public static boolean known(String s) {
        return s != null && !s.isBlank() && !s.strip().matches("(?i)unknown|not sure|i don.?t know|don.?t know|không biết|n/?a|-");
    }
}
