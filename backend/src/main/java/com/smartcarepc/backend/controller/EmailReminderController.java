package com.smartcarepc.backend.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.smartcarepc.backend.service.EmailReminderService;
import com.smartcarepc.backend.service.EmailReminderService.*;

/** Email nhắc bảo dưỡng. Token trong đường dẫn là manage token trình duyệt nhận khi bật. */
@RestController @RequestMapping("/api/reminders/email")
public class EmailReminderController {
    private final EmailReminderService service;
    public EmailReminderController(EmailReminderService service) { this.service = service; }

    /** proof: bằng chứng email từ lúc đăng nhập Google (không bắt buộc). Có thì bật ngay, không thì gửi thư xác nhận */
    record CreateRequest(String email, Map<String, String> dates, Map<String, Integer> months, String origin, String proof) {}
    record TokenRequest(String token) {}

    /** Máy chủ đã cấu hình gửi mail chưa (chưa thì web ẩn lựa chọn email) */
    @GetMapping("/available")
    public Map<String, Boolean> available() { return Map.of("available", service.available()); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Created create(@RequestBody CreateRequest r) { return service.create(r.email(), new Plan(r.dates(), r.months(), r.origin()), r.proof()); }

    @GetMapping("/{token}")
    public Status status(@PathVariable String token) { return service.status(token); }

    @PutMapping("/{token}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable String token, @RequestBody Plan plan) { service.update(token, plan); }

    @DeleteMapping("/{token}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stop(@PathVariable String token) { service.stop(token); }

    /** Link "Turn on" trong thư xác nhận mở web, người dùng bấm nút thì web gọi POST này (trình quét link không tự bật được) */
    @PostMapping("/confirm")
    public Status confirm(@RequestBody TokenRequest r) { return service.confirm(r.token()); }

    /** Link "Stop these emails" trong thư nhắc: cũng qua một nút bấm trên web */
    @PostMapping("/stop") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stopFromEmail(@RequestBody TokenRequest r) { service.stop(r.token()); }
}
