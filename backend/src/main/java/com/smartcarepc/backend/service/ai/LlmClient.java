package com.smartcarepc.backend.service.ai;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import tools.jackson.databind.json.JsonMapper;

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
        if (res != null && res.get("choices") instanceof List<?> c && !c.isEmpty()) return recoverTextToolCalls(res);
        Object err = res == null ? null : res.get("error");
        String detail = err instanceof Map<?, ?> m && m.get("message") != null ? String.valueOf(m.get("message")) : String.valueOf(err);
        log.warn("API AI trả 200 nhưng không có câu trả lời: {}", detail);
        throw new AiUnavailableException(HttpStatus.BAD_GATEWAY,
                "The external AI service is not returning a response. Please try again in a few minutes.");
    }

    /** Thẻ DSML của DeepSeek, dấu gạch đứng có thể là ｜ (toàn độ rộng) hoặc | */
    private static final String DSML = "<\\s*[｜|]\\s*DSML\\s*[｜|]\\s*";
    private static final String DSML_END = "<\\s*/\\s*[｜|]\\s*DSML\\s*[｜|]\\s*";
    static final Pattern DSML_ANY = Pattern.compile("<\\s*/?\\s*[｜|]\\s*DSML\\s*[｜|]");
    private static final Pattern DSML_INVOKE = Pattern.compile(
            "(?s)" + DSML + "invoke\\s+name\\s*=\\s*\"([^\"]+)\"\\s*>(.*?)" + DSML_END + "invoke\\s*>");
    private static final Pattern DSML_INVOKE_OPEN = Pattern.compile(DSML + "invoke\\s+name");
    private static final Pattern DSML_PARAM = Pattern.compile(
            "(?s)" + DSML + "parameter\\s+name\\s*=\\s*\"([^\"]+)\"(?:\\s+string\\s*=\\s*\"(true|false)\")?\\s*>(.*?)" + DSML_END + "parameter\\s*>");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    /**
     * DeepSeek qua cổng trung gian có lúc trả lệnh gọi công cụ dạng chữ DSML trong content (vd
     * {@code <｜DSML｜invoke name="search_products">}) thay vì tool_calls, và người dùng thấy nguyên đoạn đó trong khung chat.
     * Đọc lại thành tool_calls chuẩn để vòng chat chạy công cụ như bình thường; chỉ giữ phần lời trước thẻ đầu tiên.
     * Phản hồi bị cắt giữa chừng (finish_reason "length", hoặc có lệnh mở mà không đóng): không chạy nửa nhóm lệnh, giữ
     * dấu hiệu "length" để vòng chat xử lý như câu trả lời bị cắt (thử lại), thay vì chạy thiếu một lượt tra cứu.
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> recoverTextToolCalls(Map<String, Object> res) {
        var choices = (List<Map<String, Object>>) res.get("choices");
        var choice = choices.getFirst();
        if (!(choice.get("message") instanceof Map<?, ?> m)) return res;
        var message = (Map<String, Object>) m;
        if (message.get("tool_calls") instanceof List<?> l && !l.isEmpty()) return res;
        String content = message.get("content") == null ? "" : message.get("content").toString();
        Matcher any = DSML_ANY.matcher(content);
        if (!any.find()) return res;
        String prose = content.substring(0, any.start()).strip();
        List<Map<String, Object>> calls = new ArrayList<>();
        String base = "dsml_" + Long.toHexString(System.nanoTime());
        Matcher inv = DSML_INVOKE.matcher(content);
        while (inv.find()) {
            Map<String, Object> args = new LinkedHashMap<>();
            Matcher p = DSML_PARAM.matcher(inv.group(2));
            while (p.find()) {
                String raw = p.group(3).strip();
                Object value = raw;
                if ("false".equals(p.group(2))) {
                    try { value = JSON.readValue(raw, Object.class); } catch (RuntimeException e) { value = raw; }
                }
                args.put(p.group(1), value);
            }
            calls.add(Map.of("id", base + "_" + calls.size(), "type", "function",
                    "function", Map.of("name", inv.group(1).strip(), "arguments", JSON.writeValueAsString(args))));
        }
        int opened = 0;
        for (Matcher o = DSML_INVOKE_OPEN.matcher(content); o.find(); ) opened++;
        boolean truncated = "length".equals(choice.get("finish_reason")) || opened > calls.size();
        Map<String, Object> fixedMessage = new LinkedHashMap<>(message);
        fixedMessage.put("content", prose);
        Map<String, Object> fixedChoice = new LinkedHashMap<>(choice);
        if (truncated) {
            fixedChoice.put("finish_reason", "length");
            log.warn("AI trả lệnh gọi công cụ dạng chữ DSML bị cắt ({} lệnh mở, {} đọc được): giữ finish_reason length", opened, calls.size());
        } else if (!calls.isEmpty()) {
            fixedMessage.put("tool_calls", calls);
            fixedChoice.put("finish_reason", "tool_calls");
            log.info("AI trả lệnh gọi công cụ dạng chữ DSML, đọc lại thành {} tool_calls: {}", calls.size(),
                    calls.stream().map(c -> ((Map<?, ?>) c.get("function")).get("name")).toList());
        } else {
            log.warn("AI trả thẻ DSML không đọc được, chỉ giữ phần lời trước thẻ");
        }
        fixedChoice.put("message", fixedMessage);
        List<Map<String, Object>> fixedChoices = new ArrayList<>(choices);
        fixedChoices.set(0, fixedChoice);
        Map<String, Object> fixed = new LinkedHashMap<>(res);
        fixed.put("choices", fixedChoices);
        return fixed;
    }
}
