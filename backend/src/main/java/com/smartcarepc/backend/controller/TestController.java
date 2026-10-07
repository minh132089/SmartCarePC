package com.smartcarepc.backend.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

// API test: kiểm tra backend nhận được request từ frontend
@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        log.info("Nhận request GET /api/test/ping");
        return Map.of(
                "ok", true,
                "message", "SmartCarePC backend đang chạy",
                "time", LocalDateTime.now().toString());
    }

    // Gửi JSON lên, backend trả lại đúng nội dung đó
    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody Map<String, Object> body) {
        log.info("Nhận request POST /api/test/echo: {}", body);
        return Map.of("ok", true, "received", body);
    }
}
