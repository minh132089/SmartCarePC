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
import com.smartcarepc.backend.dto.ChatResponse.BuildCard;
import com.smartcarepc.backend.dto.ChatResponse.Line;
import com.smartcarepc.backend.dto.CompatibilityResponse;
import com.smartcarepc.backend.dto.ComponentResponse;
import com.smartcarepc.backend.dto.PcBuildResponse;
import com.smartcarepc.backend.exception.ResourceNotFoundException;
import com.smartcarepc.backend.service.CompatibilityService;
import com.smartcarepc.backend.service.ComponentService;
import com.smartcarepc.backend.service.PcBuildService;

import tools.jackson.databind.json.JsonMapper;

/**
 * Trợ lý tư vấn build PC trong khung chat. Model được dùng 3 công cụ để trả lời bằng dữ liệu thật của app
 * thay vì đoán: xem danh mục linh kiện, xem một máy có sẵn, và chạy bộ kiểm tra tương thích.
 * Cấu hình được kiểm tra cuối cùng trong lượt trả lời được gửi kèm để khung chat vẽ thành thẻ.
 * Không mở transaction ở đây vì mỗi lượt chờ AI có thể mất vài giây; từng service tự mở khi đọc DB.
 */
@Service
public class ChatService {

    /** Số lượt gọi công cụ tối đa cho một câu hỏi, tránh vòng lặp vô tận */
    private static final int MAX_ROUNDS = 8;

    /** Trang 1 "Tư vấn chọn máy": người dùng tự kể nhu cầu, trợ lý phân tích rồi chọn linh kiện */
    private static final String ADVICE = """
            Bạn là trợ lý tư vấn build PC của PC SmartCare, một app 3D giúp người không rành kỹ thuật chọn rồi tự lắp \
            máy tính để bàn. Người dùng kể nhu cầu bằng lời của họ, thường không có thuật ngữ, ví dụ "mình học online, \
            hay chơi game nhẹ, có khoảng 15 triệu, bàn hơi chật".

            Việc của bạn:
            1. Phân tích lời kể thành: dùng máy để làm gì, ngân sách, kích thước hay kiểu dáng mong muốn, có cần nâng cấp \
            về sau không. Mở đầu câu trả lời bằng một dòng tóm tắt bạn hiểu nhu cầu thế nào, để người dùng sửa nếu sai.
            2. Thiếu điều quan trọng (thường là ngân sách hoặc mục đích) thì hỏi lại, gộp thành một câu ngắn. \
            Đủ để chọn thì làm luôn.
            3. Chọn đủ linh kiện cho một máy và chạy check_build như phần dưới. Luôn chọn một vỏ máy (case) trong danh mục: \
            app sẽ mở mô hình 3D đúng kiểu vỏ đó để hướng dẫn người dùng tự lắp từng bước.
            4. Lắp được rồi thì gọi propose_build đúng một lần với cấu hình cuối cùng. Mỗi linh kiện ghi:
               - spec: thông số cụ thể nên mua, viết dễ hiểu (vd "16 GB, 2 thanh 8 GB, DDR4 3200", "CPU 6 nhân 12 luồng có sẵn đồ họa");
               - reason: một câu vì sao hợp với nhu cầu người dùng;
               - price_min_vnd, price_max_vnd: khoảng giá tham khảo ở Việt Nam cho cả số lượng của dòng đó.
            5. Giá là ước tính theo thông số phổ thông, không gắn hãng. Tổng giá phải nằm trong ngân sách. Ngân sách quá thấp \
            so với nhu cầu thì vẫn đề xuất phương án gần nhất, nói rõ thiếu khoảng bao nhiêu hoặc nên bớt món nào.
            6. App sẽ vẽ bảng chi tiết từng linh kiện và tổng giá so với ngân sách, nên trong câu trả lời đừng liệt kê lại. \
            Chỉ nói tổng ước tính so với ngân sách, 2 hoặc 3 món quan trọng nhất, rồi mời bấm "Xem và lắp máy này".
            """;

    /** Trang 2 "Hướng dẫn lắp máy": hỏi đáp khi đang lắp dở */
    private static final String ASSEMBLY = """
            Bạn là trợ lý của PC SmartCare, một app 3D giúp người không rành kỹ thuật tự lắp máy tính để bàn theo \
            từng bước. Người hỏi thường đang lắp dở, tay đang cầm linh kiện, chưa biết thuật ngữ và sợ làm hỏng.
            Việc chính của bạn là gỡ rối trong lúc lắp: hiểu chuyện gì đang xảy ra và chỉ cách làm tiếp cho an toàn.

            Gỡ rối khi lắp:
            1. Nếu biết người dùng đang ở bước nào thì trả lời đúng theo bước đó.
            2. Đưa ra cách kiểm tra từ dễ tới khó, mỗi cách một dòng, nói rõ nhìn vào đâu, bấm hay ấn chỗ nào.
            3. Mô tả chưa rõ thì hỏi lại đúng một câu (ví dụ: đèn nào sáng, quạt có quay không, nghe tiếng gì).
            4. Việc có thể làm hỏng linh kiện (ấn mạnh CPU, cắm ngược đầu dây, cắm khi máy đang có điện) thì cảnh báo trước.
            """;

    /** Luật chung cho cả hai trang */
    private static final String RULES = """
            Cách trả lời:
            - Trả lời bằng ngôn ngữ người dùng đang viết (thường là tiếng Việt). Giọng thân thiện, xưng "mình", gọi "bạn".
            - Ngắn gọn, khoảng 150 chữ trở xuống, trừ khi người dùng xin giải thích kỹ. Không dùng bảng, không dùng tiêu đề.
              Được dùng gạch đầu dòng "- " và **chữ đậm** cho ý chính.
            - Gặp thuật ngữ thì giải thích luôn bằng một câu đời thường, ví dụ "RAM là bộ nhớ tạm, càng nhiều thì mở \
            được càng nhiều chương trình cùng lúc".
            - Giải thích vì sao bạn chọn như vậy, để người dùng tự kiểm tra được lời khuyên chứ không chỉ làm theo.

            Khi người dùng hỏi chọn hay đổi linh kiện:
            1. Cần biết ngân sách và máy dùng để làm gì (học, văn phòng, chơi game, dựng phim...). Thiếu thì hỏi lại, \
            gộp thành một câu hỏi ngắn. Có rồi thì làm luôn, đừng hỏi thêm.
            2. Chọn linh kiện từ danh mục của app (list_components). Một máy cần đủ: vỏ, bo mạch chủ, CPU, tản nhiệt, \
            RAM, SSD, nguồn; thêm card đồ họa khi chơi game hoặc làm đồ họa, hoặc khi CPU không có sẵn đồ họa.
            3. Luôn chạy check_build với cấu hình định đề xuất. Còn lỗi (errors) thì đổi linh kiện rồi chạy lại cho tới \
            khi lắp được. Cảnh báo (warnings) thì nói lại cho người dùng bằng lời dễ hiểu.
            4. App sẽ tự vẽ danh sách linh kiện thành một thẻ ngay dưới câu trả lời, nên đừng liệt kê lại cả danh sách. \
            Chỉ nói vài món quan trọng nhất và lý do chọn.
            5. Danh mục của app chỉ là bộ mẫu nhỏ. Nếu nhu cầu vượt quá danh mục thì nói rõ điều đó.
            - Gọi linh kiện bằng tên tiếng Việt trong danh mục, không đọc mã code cho người dùng.
            - Không nêu tên hãng hay mẫu sản phẩm cụ thể, chỉ nói theo thông số (ví dụ "RAM 16 GB DDR4").
            - Danh mục chưa có giá. Giá tham khảo chỉ đưa qua propose_build và luôn nói rõ là ước tính, nên hỏi lại cửa hàng. \
            Không bịa thông số mà danh mục để trống rồi nói như thông tin chắc chắn.

            An toàn:
            - Trước mọi việc mở máy, nhắc: tắt máy, rút dây điện, chạm tay vào phần kim loại của vỏ để xả tĩnh điện.
            - Có mùi khét, khói, tụ phồng, hoặc lỗi ở bộ nguồn thì khuyên mang ra thợ. Không hướng dẫn mở bộ nguồn.

            Phạm vi: chỉ trả lời về máy tính (chọn mua, lắp ráp, nâng cấp, bảo dưỡng, máy chậm / nóng / ồn). \
            Câu hỏi ngoài lề thì từ chối nhẹ nhàng và gợi ý một câu hỏi phù hợp.
            """;

    private static final List<Map<String, Object>> TOOLS = List.of(
            tool("list_components",
                    "Danh mục linh kiện của app kèm thông số. Lọc theo loại hoặc bỏ trống để lấy tất cả. "
                            + "Loại: case, motherboard, cpu, cooler, ram, gpu, ssd, psu, fans, tools.",
                    Map.of("type", "object",
                            "properties", Map.of("category", Map.of("type", "string", "description", "Mã loại linh kiện")))),
            tool("get_build",
                    "Xem toàn bộ linh kiện của một máy có sẵn trong app, theo mã máy.",
                    Map.of("type", "object",
                            "properties", Map.of("code", Map.of("type", "string", "description", "Mã máy, vd standard_tower")),
                            "required", List.of("code"))),
            tool("check_build",
                    "Kiểm tra các linh kiện có lắp chung được không. Trả về errors (không lắp được) "
                            + "và warnings (lắp được nhưng nên xem lại). Cần đủ vỏ, bo mạch, CPU, nguồn, RAM, SSD.",
                    Map.of("type", "object",
                            "properties", Map.of("items", Map.of("type", "array",
                                    "items", Map.of("type", "object",
                                            "properties", Map.of(
                                                    "code", Map.of("type", "string", "description", "Mã linh kiện"),
                                                    "quantity", Map.of("type", "integer", "minimum", 1)),
                                            "required", List.of("code")))),
                            "required", List.of("items"))),
            tool("propose_build",
                    "Chốt cấu hình đề xuất cho người dùng (gọi một lần, sau khi check_build không còn lỗi). "
                            + "App vẽ bảng chi tiết từ dữ liệu này. Trả về kết quả kiểm tra tương thích.",
                    Map.of("type", "object",
                            "properties", Map.of(
                                    "summary", Map.of("type", "string",
                                            "description", "Một câu tóm tắt nhu cầu đã hiểu, vd 'Học online và làm văn phòng, máy nhỏ gọn'"),
                                    "budget_vnd", Map.of("type", "integer", "description", "Ngân sách người dùng nói, đơn vị đồng; bỏ trống nếu chưa biết"),
                                    "items", Map.of("type", "array",
                                            "items", Map.of("type", "object",
                                                    "properties", Map.of(
                                                            "code", Map.of("type", "string", "description", "Mã linh kiện trong danh mục"),
                                                            "quantity", Map.of("type", "integer", "minimum", 1),
                                                            "spec", Map.of("type", "string", "description", "Thông số cụ thể nên mua"),
                                                            "reason", Map.of("type", "string", "description", "Vì sao chọn, một câu"),
                                                            "price_min_vnd", Map.of("type", "integer"),
                                                            "price_max_vnd", Map.of("type", "integer")),
                                                    "required", List.of("code", "spec", "reason")))),
                            "required", List.of("summary", "items"))));

    private final LlmClient client;
    private final ComponentService componentService;
    private final CompatibilityService compatibilityService;
    private final PcBuildService pcBuildService;
    private final JsonMapper json;
    private final String model;
    private final int maxTokens;

    public ChatService(LlmClient client, ComponentService componentService,
            CompatibilityService compatibilityService, PcBuildService pcBuildService, JsonMapper json,
            @Value("${app.ai.model:deepseek-v4-flash}") String model,
            @Value("${app.ai.max-tokens:2048}") int maxTokens) {
        this.client = client;
        this.componentService = componentService;
        this.compatibilityService = compatibilityService;
        this.pcBuildService = pcBuildService;
        this.json = json;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    /**
     * Cấu hình kiểm tra gần nhất trong một lượt trả lời. Đề xuất chốt bằng propose_build có thêm
     * thông tin từng dòng (details, theo mã linh kiện), nhu cầu đã hiểu và ngân sách.
     */
    private record Checked(BuildCheckRequest request, CompatibilityResponse result,
            Map<String, Map<String, Object>> details, String summary, Long budgetVnd) {

        Checked(BuildCheckRequest request, CompatibilityResponse result) {
            this(request, result, Map.of(), null, null);
        }
    }

    @SuppressWarnings("unchecked")
    public ChatResponse reply(ChatRequest request) {
        List<Map<String, Object>> messages = new ArrayList<>();
        String step = request.guideStep() == null || request.guideStep().isBlank() ? ""
                : "\nNgười dùng đang làm theo hướng dẫn lắp, hiện ở: " + request.guideStep().strip() + ".\n";
        String intro = "advice".equals(request.mode()) ? ADVICE : ASSEMBLY;
        messages.add(Map.of("role", "system", "content", intro + "\n" + RULES + "\n" + context(request.buildCode()) + step));
        for (var m : request.messages()) {
            // Lời chào mở đầu của khung chat là tin "assistant" đứng đầu: bỏ để hội thoại bắt đầu bằng người dùng
            if (messages.size() == 1 && m.role().equals("assistant")) continue;
            messages.add(Map.of("role", m.role(), "content", m.content()));
        }
        if (messages.size() == 1 || !messages.getLast().get("role").equals("user")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tin cuối cùng phải là câu hỏi của người dùng");
        }

        Checked[] last = { null };
        for (int round = 0; round < MAX_ROUNDS; round++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("max_tokens", maxTokens);
            body.put("messages", messages);
            body.put("tools", TOOLS);

            Map<String, Object> res = client.complete(body);
            var choices = (List<Map<String, Object>>) res.getOrDefault("choices", List.of());
            if (choices.isEmpty()) break;
            var message = (Map<String, Object>) choices.getFirst().getOrDefault("message", Map.of());
            var calls = (List<Map<String, Object>>) message.get("tool_calls");
            if (calls == null || calls.isEmpty()) {
                return new ChatResponse(clean(message.get("content")), last[0] == null ? null : card(last[0]));
            }
            // Gửi lại nguyên tin của model (kể cả reasoning_content của model suy luận) rồi tới kết quả từng công cụ
            messages.add(message);
            for (var call : calls) {
                messages.add(Map.of("role", "tool", "tool_call_id", String.valueOf(call.get("id")),
                        "content", runTool((Map<String, Object>) call.get("function"), last)));
            }
        }
        return new ChatResponse("Câu này mình phải tra cứu nhiều quá. Bạn hỏi cụ thể hơn một chút nhé.",
                last[0] == null ? null : card(last[0]));
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

    /** Chạy một lời gọi công cụ, trả về chuỗi JSON (hoặc lời báo lỗi) để model đọc */
    @SuppressWarnings("unchecked")
    private String runTool(Map<String, Object> function, Checked[] last) {
        try {
            String name = String.valueOf(function.get("name"));
            Object rawArgs = function.get("arguments");
            Map<String, Object> input = rawArgs == null || rawArgs.toString().isBlank()
                    ? Map.of()
                    : json.readValue(rawArgs.toString(), Map.class);
            Object out = switch (name) {
                case "list_components" -> componentService.findAll(blankToNull(input.get("category"))).stream()
                        .map(ChatService::brief).toList();
                case "get_build" -> {
                    PcBuildResponse b = pcBuildService.findByCode(String.valueOf(input.get("code")));
                    yield Map.of("code", b.code(), "name", b.name(), "tagline", b.tagline(), "items", b.items().stream()
                            .map(i -> Map.of("quantity", i.quantity(), "component", brief(i.component()))).toList());
                }
                case "check_build" -> {
                    var req = checkRequest(input);
                    var result = compatibilityService.check(req);
                    // Đã chốt đề xuất thì giữ thẻ đề xuất, check_build sau đó chỉ để model tự thử
                    if (last[0] == null || last[0].summary() == null) last[0] = new Checked(req, result);
                    yield result;
                }
                case "propose_build" -> {
                    var req = checkRequest(input);
                    var result = compatibilityService.check(req);
                    Map<String, Map<String, Object>> details = new LinkedHashMap<>();
                    ((List<Map<String, Object>>) input.getOrDefault("items", List.of())).stream()
                            .filter(i -> i.get("code") != null)
                            .forEach(i -> details.put(String.valueOf(i.get("code")), i));
                    last[0] = new Checked(req, result, details, String.valueOf(input.getOrDefault("summary", "")),
                            input.get("budget_vnd") instanceof Number n ? n.longValue() : null);
                    yield Map.of("saved", true, "compatibility", result,
                            "note", result.compatible() ? "Đã lưu đề xuất, app sẽ vẽ bảng chi tiết."
                                    : "Cấu hình còn lỗi: đổi linh kiện rồi gọi lại propose_build.");
                }
                default -> throw new IllegalArgumentException("Không có công cụ " + name);
            };
            return json.writeValueAsString(out);
        } catch (RuntimeException e) {
            return "Lỗi: " + Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName());
        }
    }

    @SuppressWarnings("unchecked")
    private static BuildCheckRequest checkRequest(Map<String, Object> input) {
        return new BuildCheckRequest(((List<Map<String, Object>>) input.getOrDefault("items", List.of())).stream()
                .filter(i -> i.get("code") != null)
                .map(i -> new BuildCheckRequest.Item(String.valueOf(i.get("code")),
                        i.get("quantity") instanceof Number n ? n.intValue() : null))
                .toList());
    }

    /** Thẻ cấu hình cho khung chat: tên tiếng Việt và loại của từng linh kiện, kèm kết quả kiểm tra */
    private BuildCard card(Checked c) {
        Map<String, Integer> qty = new LinkedHashMap<>();
        c.request().items().forEach(i -> qty.merge(i.code(), i.qty(), Integer::sum));
        List<Line> lines = new ArrayList<>();
        qty.forEach((code, n) -> {
            ComponentResponse p = componentService.findByCode(code);
            Map<String, Object> d = c.details().getOrDefault(code, Map.of());
            lines.add(new Line(code, p.category(), p.name(), n, text(d.get("spec")), text(d.get("reason")),
                    vnd(d.get("price_min_vnd")), vnd(d.get("price_max_vnd"))));
        });
        var r = c.result();
        return new BuildCard(lines, r.compatible(), r.errors(), r.warnings(), c.summary(), c.budgetVnd());
    }

    private static String text(Object o) {
        return o == null || o.toString().isBlank() ? null : o.toString().strip();
    }

    /** Giá âm hay không phải số thì bỏ, để giao diện không cộng nhầm */
    private static Long vnd(Object o) {
        return o instanceof Number n && n.longValue() >= 0 ? n.longValue() : null;
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

    private static Map<String, Object> tool(String name, String description, Map<String, Object> parameters) {
        return Map.of("type", "function",
                "function", Map.of("name", name, "description", description, "parameters", parameters));
    }

    private static String blankToNull(Object o) {
        return o == null || o.toString().isBlank() ? null : o.toString();
    }

    /** Bỏ phần suy nghĩ <think>...</think> mà vài model suy luận để lẫn trong câu trả lời */
    private static String clean(Object content) {
        String t = content == null ? "" : content.toString().replaceAll("(?s)<think>.*?</think>", "").strip();
        return t.isEmpty() ? "Xin lỗi, mình chưa trả lời được câu này. Bạn thử hỏi theo cách khác nhé." : t;
    }
}
