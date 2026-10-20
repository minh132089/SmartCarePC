package com.smartcarepc.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartcarepc.backend.dto.ChatRequest;
import com.smartcarepc.backend.dto.ChatResponse;
import com.smartcarepc.backend.service.ai.ChatJobs;
import com.smartcarepc.backend.service.ai.ChatService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Khung chat tư vấn: gửi cả đoạn hội thoại, nhận lại câu trả lời của trợ lý (trả thẳng, hoặc chạy nền qua /jobs) */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatJobs chatJobs;

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatService.reply(request);
    }

    /** Gửi câu hỏi chạy nền: trả mã việc ngay (202), rồi hỏi lại bằng GET /api/chat/jobs/{id} */
    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ChatJobs.JobView start(@Valid @RequestBody ChatRequest request) {
        return chatJobs.start(request);
    }

    /** Trạng thái câu hỏi: running (kèm bước đang làm), done (kèm câu trả lời) hoặc error */
    @GetMapping("/jobs/{id}")
    public ChatJobs.JobView job(@PathVariable String id) {
        return chatJobs.get(id);
    }
}
