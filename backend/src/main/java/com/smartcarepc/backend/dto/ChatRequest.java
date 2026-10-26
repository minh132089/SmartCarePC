package com.smartcarepc.backend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cả đoạn hội thoại trong khung chat, tin cũ trước, tin cuối là câu người dùng vừa gửi.
 * buildCode là máy đang xem trên màn hình 3D (vd "mini_sff"), để trợ lý biết người dùng đang nói về máy nào.
 * guideStep là bước hướng dẫn lắp đang mở (vd "Bước 3/10: Đặt CPU vào socket"), null nếu không mở hướng dẫn.
 * mode: "advice" ở trang tư vấn chọn máy, "assembly" (mặc định) ở khung hỏi đáp khi lắp.
 * { "buildCode": "gaming", "messages": [ { "role": "user", "content": "Máy này chơi game được không?" } ] }
 */
public record ChatRequest(
        @NotEmpty(message = "No messages provided") @Size(max = 30, message = "This conversation is too long. Start a new conversation.")
        List<@Valid Message> messages,
        String buildCode,
        @Size(max = 2000) String guideStep,
        @Pattern(regexp = "advice|assembly", message = "mode must be advice or assembly") String mode) {

    public ChatRequest(List<Message> messages, String buildCode) {
        this(messages, buildCode, null, null);
    }

    public ChatRequest(List<Message> messages, String buildCode, String guideStep) {
        this(messages, buildCode, guideStep, null);
    }


    public record Message(
            @NotBlank @Pattern(regexp = "user|assistant", message = "role must be user or assistant") String role,
            @NotBlank(message = "The message is empty") @Size(max = 2000, message = "Messages must not exceed 2000 characters") String content) {
    }
}
