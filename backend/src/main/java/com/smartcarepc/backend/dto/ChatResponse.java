package com.smartcarepc.backend.dto;

import java.util.List;

import com.smartcarepc.backend.dto.CompatibilityResponse.Issue;

/**
 * Câu trả lời của trợ lý. reply có thể chứa gạch đầu dòng (- ) và chữ đậm (**...**).
 * build: cấu hình trợ lý vừa kiểm tra trong lượt này (null nếu không có), để khung chat vẽ thành thẻ.
 */
public record ChatResponse(String reply, BuildCard build) {

    public ChatResponse(String reply) {
        this(reply, null);
    }

    public record BuildCard(List<Line> items, boolean compatible, List<Issue> errors, List<Issue> warnings) {
    }

    public record Line(String code, String category, String name, int quantity) {
    }
}
