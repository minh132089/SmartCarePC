package com.smartcarepc.backend.dto;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Form phản hồi gửi từ app. answers là cặp mã câu hỏi / câu trả lời (câu hỏi nằm ở src/ui/study/questions.ts). */
public record FeedbackRequest(
        @Pattern(regexp = "[A-Za-z0-9_-]{1,20}") String participantCode,
        @Pattern(regexp = "before|after") String phase,
        @Size(max = 60) String machine,
        @NotEmpty @Size(max = 40) Map<String, Object> answers) {
}
