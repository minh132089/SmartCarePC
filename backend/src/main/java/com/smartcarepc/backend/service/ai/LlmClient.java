package com.smartcarepc.backend.service.ai;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
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
 * Gọi API chat theo chuẩn OpenAI (POST {base-url}/chat/completions).
 * Mặc định là DeepSeek V4 qua AgentRouter; đổi AI_BASE_URL / AI_MODEL là dùng được nhà cung cấp khác cùng chuẩn
 * (DeepSeek trực tiếp, OpenRouter...). Khoá API chỉ nằm ở backend, trình duyệt không bao giờ thấy.
 * Lỗi từ phía AI được đổi thành câu tiếng Việt để hiện thẳng trong khung chat.
 */
@Slf4j
@Component
public class LlmClient {

    private final RestClient http;
    private final String apiKey;

    public LlmClient(@Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.base-url:https://agentrouter.org/v1}") String baseUrl) {
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        // Model suy luận có thể nghĩ khá lâu trước khi trả lời
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.http = RestClient.builder()
                .baseUrl(baseUrl.replaceAll("/+$", ""))
                .requestFactory(factory)
                .build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    /** Gửi body chat/completions, trả về cả JSON phản hồi */
    @SuppressWarnings("unchecked")
    public Map<String, Object> complete(Map<String, Object> body) {
        if (apiKey.isEmpty()) {
            throw new AiUnavailableException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The AI assistant is unavailable because its server credentials are not configured.");
        }
        try {
            return checkReply(http.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class));
        } catch (HttpStatusCodeException e) {
            log.warn("API AI trả lỗi {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new AiUnavailableException(HttpStatus.BAD_GATEWAY, switch (e.getStatusCode().value()) {
                case 401, 403 -> "The AI provider rejected the server credentials. "
                        + "Ask the administrator to check the configuration.";
                case 400, 404 -> "The AI provider could not accept the request. Ask the administrator to check the model and endpoint configuration.";
                case 402 -> "The AI provider account has no remaining quota or credit.";
                case 429, 503, 529 -> "The assistant is busy. Please try again in a few minutes.";
                default -> "The AI service encountered an error. Please try again.";
            });
        } catch (ResourceAccessException e) {
            log.warn("Không kết nối được API AI: {}", e.getMessage());
            throw new AiUnavailableException(HttpStatus.BAD_GATEWAY,
                    "Cannot connect to the AI service. Check your connection and try again.");
        }
    }

    /**
     * Có nhà cung cấp trả mã 200 nhưng thân là lỗi (vd {"error": {"message": "Upstream returned no output"}}) hoặc không
     * có choices: coi là lỗi của dịch vụ AI, không để vòng chat hiểu nhầm thành "tra cứu nhiều quá"
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> checkReply(Map<String, Object> res) {
        if (res != null && res.get("choices") instanceof List<?> c && !c.isEmpty()) return res;
        Object err = res == null ? null : res.get("error");
        String detail = err instanceof Map<?, ?> m && m.get("message") != null ? String.valueOf(m.get("message")) : String.valueOf(err);
        log.warn("API AI trả 200 nhưng không có câu trả lời: {}", detail);
        throw new AiUnavailableException(HttpStatus.BAD_GATEWAY,
                "The external AI service is not returning a response. Please try again in a few minutes.");
    }
}
