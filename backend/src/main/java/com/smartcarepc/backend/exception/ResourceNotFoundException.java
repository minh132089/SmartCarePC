package com.smartcarepc.backend.exception;

/** Ném ra khi không tìm thấy dữ liệu; GlobalExceptionHandler đổi thành HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String what, Object key) {
        super("Not found: " + what + ": " + key);
    }
}
