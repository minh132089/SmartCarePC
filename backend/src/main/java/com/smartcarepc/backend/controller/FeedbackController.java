package com.smartcarepc.backend.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartcarepc.backend.dto.FeedbackRequest;
import com.smartcarepc.backend.dto.FeedbackSaved;
import com.smartcarepc.backend.service.FeedbackService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Form phản hồi của thử nghiệm 4 tuần: app gửi lên, nhóm nghiên cứu tải về một file CSV */
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackSaved create(@Valid @RequestBody FeedbackRequest request) {
        return feedbackService.save(request);
    }

    /** Dữ liệu cho trang xem phản hồi (/feedback). Khoá gửi qua header để không lộ trong địa chỉ / log truy cập */
    @GetMapping("/list")
    public ResponseEntity<java.util.List<FeedbackService.Row>> list(@org.springframework.web.bind.annotation.RequestHeader(value = "X-Export-Key", required = false) String key) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(feedbackService.list(key));
    }

    /** Tải CSV: khoá gửi qua header X-Export-Key (trang /feedback); ?key=... vẫn nhận để mở tay trong trình duyệt */
    @GetMapping("/export.csv")
    public ResponseEntity<String> export(@RequestParam(required = false) String key,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Export-Key", required = false) String headerKey) {
        if (headerKey != null && !headerKey.isBlank()) key = headerKey;
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"pcsmartcare-feedback.csv\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(feedbackService.exportCsv(key));
    }
}
