package com.smartcarepc.backend.dto;

import java.util.List;

/**
 * Kết quả kiểm tra một cấu hình.
 * errors: lắp không được, phải đổi linh kiện. warnings: lắp được nhưng nên xem lại.
 */
public record CompatibilityResponse(boolean compatible, List<Issue> errors, List<Issue> warnings) {

    /** rule là mã quy tắc để frontend tô đỏ đúng linh kiện, message là lời giải thích cho người mới */
    public record Issue(String rule, String message) {
    }
}
