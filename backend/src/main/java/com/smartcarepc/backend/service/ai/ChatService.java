package com.smartcarepc.backend.service.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.smartcarepc.backend.dto.BuildCheckRequest;
import com.smartcarepc.backend.dto.ChatRequest;
import com.smartcarepc.backend.dto.ChatResponse;
import com.smartcarepc.backend.dto.ComponentResponse;
import com.smartcarepc.backend.dto.PcBuildResponse;
import com.smartcarepc.backend.exception.ResourceNotFoundException;
import com.smartcarepc.backend.service.CompatibilityService;
import com.smartcarepc.backend.service.ComponentService;
import com.smartcarepc.backend.service.PcBuildService;

import tools.jackson.databind.json.JsonMapper;

/**
 * Trợ lý tư vấn trong khung chat. Claude được dùng 3 công cụ để trả lời bằng dữ liệu thật của app
 * thay vì đoán: xem danh mục linh kiện, xem một máy có sẵn, và chạy bộ kiểm tra tương thích.
 * Không mở transaction ở đây vì mỗi lượt chờ AI có thể mất vài giây; từng service tự mở khi đọc DB.
 */
@Service
public class ChatService {

    /** Số lượt gọi công cụ tối đa cho một câu hỏi, tránh vòng lặp vô tận */
    private static final int MAX_ROUNDS = 6;

    private static final String RULES = """
            Bạn là trợ lý tư vấn của PC SmartCare, một app 3D giúp người không rành kỹ thuật hiểu, chọn, lắp và \
            bảo dưỡng máy tính để bàn. Người hỏi thường chưa biết thuật ngữ và hay ngại hỏi lại.

            Cách trả lời:
            - Trả lời bằng ngôn ngữ người dùng đang viết (thường là tiếng Việt). Giọng thân thiện, xưng "mình", gọi "bạn".
            - Ngắn gọn, khoảng 120 chữ trở xuống, trừ khi người dùng xin giải thích kỹ. Không dùng bảng, không dùng tiêu đề.
              Được dùng gạch đầu dòng "- " và **chữ đậm** cho ý chính.
            - Gặp thuật ngữ thì giải thích luôn bằng một câu đời thường, ví dụ "RAM là bộ nhớ tạm, càng nhiều thì mở \
            được càng nhiều chương trình cùng lúc".
            - Thiếu thông tin quan trọng (ngân sách, dùng để làm gì, máy đang bị gì) thì hỏi lại đúng một câu.
            - Giải thích vì sao bạn khuyên như vậy, để người dùng tự kiểm tra được lời khuyên chứ không chỉ làm theo.

            Dữ liệu:
            - Chỉ gợi ý linh kiện có trong danh mục của app (dùng công cụ list_components). Gọi linh kiện bằng tên \
            tiếng Việt trong danh mục, không đọc mã code cho người dùng.
            - Trước khi khuyên một cấu hình đầy đủ hoặc trả lời "lắp chung được không", luôn chạy check_build rồi \
            giải thích kết quả bằng lời dễ hiểu.
            - Không nêu tên hãng hay mẫu sản phẩm cụ thể, chỉ nói theo thông số (ví dụ "RAM 16 GB DDR4").
            - Danh mục chưa có giá. Nếu được hỏi giá, chỉ nói khoảng tham khảo chung và nhắc kiểm tra lại ở cửa hàng. \
            Không bịa số liệu mà danh mục để trống.

            An toàn:
            - Trước mọi việc mở máy, nhắc: tắt máy, rút dây điện, chạm tay vào phần kim loại của vỏ để xả tĩnh điện.
            - Có mùi khét, khói, tụ phồng, hoặc lỗi ở bộ nguồn thì khuyên mang ra thợ. Không hướng dẫn mở bộ nguồn.

            Phạm vi: chỉ trả lời về máy tính (chọn mua, lắp ráp, nâng cấp, bảo dưỡng, máy chậm / nóng / ồn). \
            Câu hỏi ngoài lề thì từ chối nhẹ nhàng và gợi ý một câu hỏi phù hợp.
            """;

    private static final List<Map<String, Object>> TOOLS = List.of(
            Map.of("name", "list_components",
                    "description", "Danh mục linh kiện của app kèm thông số. Lọc theo loại hoặc bỏ trống để lấy tất cả. "
                            + "Loại: case, motherboard, cpu, cooler, ram, gpu, ssd, psu, fans, tools.",
                    "input_schema", Map.of("type", "object",
                            "properties", Map.of("category", Map.of("type", "string", "description", "Mã loại linh kiện")))),
            Map.of("name", "get_build",
                    "description", "Xem toàn bộ linh kiện của một máy có sẵn trong app, theo mã máy.",
                    "input_schema", Map.of("type", "object",
                            "properties", Map.of("code", Map.of("type", "string", "description", "Mã máy, vd standard_tower")),
                            "required", List.of("code"))),
            Map.of("name", "check_build",
                    "description", "Kiểm tra các linh kiện có lắp chung được không. Trả về errors (không lắp được) "
                            + "và warnings (lắp được nhưng nên xem lại). Cần đủ vỏ, bo mạch, CPU, nguồn, RAM, SSD.",
                    "input_schema", Map.of("type", "object",
                            "properties", Map.of("items", Map.of("type", "array",
                                    "items", Map.of("type", "object",
                                            "properties", Map.of(
                                                    "code", Map.of("type", "string"),
                                                    "quantity", Map.of("type", "integer", "minimum", 1)),
                                            "required", List.of("code")))),
                            "required", List.of("items"))));

    private final AnthropicClient client;
    private final ComponentService componentService;
    private final CompatibilityService compatibilityService;
    private final PcBuildService pcBuildService;
    private final JsonMapper json;
    private final String model;
    private final int maxTokens;

    public ChatService(AnthropicClient client, ComponentService componentService,
            CompatibilityService compatibilityService, PcBuildService pcBuildService, JsonMapper json,
            @Value("${app.ai.model:claude-sonnet-5-5}") String model,
            @Value("${app.ai.max-tokens:1024}") int maxTokens) {
        this.client = client;
        this.componentService = componentService;
        this.compatibilityService = compatibilityService;
        this.pcBuildService = pcBuildService;
        this.json = json;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    @SuppressWarnings("unchecked")
    public ChatResponse reply(ChatRequest request) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (var m : request.messages()) {
            // Lời chào mở đầu của khung chat là tin "assistant" đứng đầu: Claude cần hội thoại bắt đầu bằng người dùng
            if (messages.isEmpty() && m.role().equals("assistant")) continue;
            messages.add(Map.of("role", m.role(), "content", m.content()));
        }
        if (messages.isEmpty() || !messages.getLast().get("role").equals("user")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tin cuối cùng phải là câu hỏi của người dùng");
        }

        String system = RULES + "\n" + context(request.buildCode());
        for (int round = 0; round < MAX_ROUNDS; round++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("max_tokens", maxTokens);
            body.put("system", system);
            body.put("tools", TOOLS);
            body.put("messages", messages);

            Map<String, Object> res = client.createMessage(body);
            var content = (List<Map<String, Object>>) res.getOrDefault("content", List.of());
            if (!"tool_use".equals(res.get("stop_reason"))) {
                return new ChatResponse(text(content));
            }
            messages.add(Map.of("role", "assistant", "content", content));
            messages.add(Map.of("role", "user", "content", content.stream()
                    .filter(b -> "tool_use".equals(b.get("type")))
                    .map(this::runTool)
                    .toList()));
        }
        return new ChatResponse("Câu này mình phải tra cứu nhiều quá. Bạn hỏi cụ thể hơn một chút nhé.");
    }

    /** Máy người dùng đang xem và danh sách máy có sẵn, gắn vào cuối system prompt */
    private String context(String buildCode) {
        StringBuilder sb = new StringBuilder("Các máy có sẵn trong app (mã: tên, mô tả):\n");
        pcBuildService.findPresets().forEach(b ->
                sb.append("- ").append(b.code()).append(": ").append(b.name()).append(", ").append(b.tagline()).append('\n'));
        if (buildCode != null && !buildCode.isBlank()) {
            try {
                PcBuildResponse b = pcBuildService.findByCode(buildCode);
                sb.append("\nNgười dùng đang xem máy \"").append(b.name()).append("\" (mã ").append(b.code())
                        .append(") trên màn hình 3D. Linh kiện: ")
                        .append(b.items().stream()
                                .map(i -> i.quantity() + " × " + i.component().name() + " [" + i.component().code() + "]")
                                .collect(Collectors.joining(", ")))
                        .append(".\nKhi người dùng nói \"máy này\" là nói về máy đó.\n");
            } catch (ResourceNotFoundException ignored) {
                // Máy chưa có trong DB: trả lời chung, không kèm ngữ cảnh
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> runTool(Map<String, Object> call) {
        var input = (Map<String, Object>) call.getOrDefault("input", Map.of());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "tool_result");
        result.put("tool_use_id", call.get("id"));
        try {
            Object out = switch (String.valueOf(call.get("name"))) {
                case "list_components" -> componentService.findAll(blankToNull(input.get("category"))).stream()
                        .map(ChatService::brief).toList();
                case "get_build" -> {
                    PcBuildResponse b = pcBuildService.findByCode(String.valueOf(input.get("code")));
                    yield Map.of("code", b.code(), "name", b.name(), "tagline", b.tagline(), "items", b.items().stream()
                            .map(i -> Map.of("quantity", i.quantity(), "component", brief(i.component()))).toList());
                }
                case "check_build" -> compatibilityService.check(new BuildCheckRequest(
                        ((List<Map<String, Object>>) input.getOrDefault("items", List.of())).stream()
                                .filter(i -> i.get("code") != null)
                                .map(i -> new BuildCheckRequest.Item(String.valueOf(i.get("code")),
                                        i.get("quantity") instanceof Number n ? n.intValue() : null))
                                .toList()));
                default -> throw new IllegalArgumentException("Không có công cụ " + call.get("name"));
            };
            result.put("content", json.writeValueAsString(out));
        } catch (RuntimeException e) {
            result.put("content", Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()));
            result.put("is_error", true);
        }
        return result;
    }

    /** Bản rút gọn của linh kiện cho AI: bỏ đường dẫn model / ảnh và các thông số còn trống */
    private static Map<String, Object> brief(ComponentResponse c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", c.code());
        m.put("category", c.category());
        m.put("name", c.name());
        if (c.description() != null) m.put("description", c.description());
        if (c.price() != null) m.put("price", c.price());
        Map<String, Object> specs = new LinkedHashMap<>();
        c.specs().forEach((k, v) -> {
            if (v != null) specs.put(k, v);
        });
        m.put("specs", specs);
        return m;
    }

    private static String blankToNull(Object o) {
        return o == null || o.toString().isBlank() ? null : o.toString();
    }

    private static String text(List<Map<String, Object>> content) {
        String t = content.stream()
                .filter(b -> "text".equals(b.get("type")))
                .map(b -> String.valueOf(b.get("text")))
                .collect(Collectors.joining("\n"))
                .strip();
        return t.isEmpty() ? "Xin lỗi, mình chưa trả lời được câu này. Bạn thử hỏi theo cách khác nhé." : t;
    }
}
