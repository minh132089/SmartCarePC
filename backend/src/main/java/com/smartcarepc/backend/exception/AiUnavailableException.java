package com.smartcarepc.backend.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Trợ lý AI không trả lời được: chưa đặt khoá API (503) hoặc dịch vụ AI báo lỗi (502).
 * Message là câu hiện thẳng trong khung chat nên viết cho người dùng đọc.
 */
@Getter
public class AiUnavailableException extends RuntimeException {

    private final HttpStatus status;

    public AiUnavailableException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
