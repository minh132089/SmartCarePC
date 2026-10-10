package com.smartcarepc.backend.dto;

/** Câu trả lời của trợ lý. Có thể chứa gạch đầu dòng (- ) và chữ đậm (**...**). */
public record ChatResponse(String reply) {
}
