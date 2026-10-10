package com.smartcarepc.backend.service.ai;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.smartcarepc.backend.exception.AiUnavailableException;

import lombok.extern.slf4j.Slf4j;

/**
 * Gọi Claude Messages API (POST /v1/messages). Khoá API chỉ nằm ở backend, trình duyệt không bao giờ thấy.
 * Lỗi từ phía AI được đổi thành câu tiếng Việt để hiện thẳng trong khung chat.
 */
@Slf4j
@Component
public class AnthropicClient {

    private final RestClient http;
    private final String apiKey;

    public AnthropicClient(@Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.base-url:https://api.anthropic.com}") String baseUrl) {
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(90));
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createMessage(Map<String, Object> body) {
        if (apiKey.isEmpty()) {
            throw new AiUnavailableException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Trợ lý AI chưa được bật vì máy chủ chưa có khoá API (biến môi trường ANTHROPIC_API_KEY).");
        }
        try {
            return http.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (HttpStatusCodeException e) {
            log.warn("Claude API trả lỗi {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new AiUnavailableException(HttpStatus.BAD_GATEWAY, switch (e.getStatusCode().value()) {
                case 401, 403 -> "Khoá API của trợ lý AI không hợp lệ. Hãy kiểm tra lại ANTHROPIC_API_KEY.";
                case 429, 529 -> "Trợ lý đang quá tải, bạn thử lại sau ít phút nhé.";
                default -> "Trợ lý AI đang gặp lỗi, bạn thử lại sau nhé.";
            });
        } catch (ResourceAccessException e) {
            log.warn("Không kết nối được Claude API: {}", e.getMessage());
            throw new AiUnavailableException(HttpStatus.BAD_GATEWAY,
                    "Không kết nối được tới trợ lý AI. Bạn kiểm tra mạng rồi thử lại nhé.");
        }
    }
}
