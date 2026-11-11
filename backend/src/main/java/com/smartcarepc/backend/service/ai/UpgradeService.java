package com.smartcarepc.backend.service.ai;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.smartcarepc.backend.dto.ChatRequest;
import com.smartcarepc.backend.dto.ChatResponse;
import com.smartcarepc.backend.dto.ChatResponse.BuildCard;
import com.smartcarepc.backend.dto.ChatResponse.Line;
import com.smartcarepc.backend.dto.CompatibilityResponse.Issue;
import com.smartcarepc.backend.dto.UpgradeInfo;
import com.smartcarepc.backend.exception.AiUnavailableException;
import com.smartcarepc.backend.service.Io;
import com.smartcarepc.backend.service.price.Offer;
import com.smartcarepc.backend.service.price.OfferEvidence;
import com.smartcarepc.backend.service.price.PriceEngine;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tư vấn nâng cấp máy đang có (trang "Upgrade my PC"). Người dùng nhập từng linh kiện hiện tại (ô trống là không biết),
 * mục đích và số tiền dành cho nâng cấp. Trợ lý tìm hàng thật bằng search_products rồi chốt bằng propose_upgrade;
 * app (không phải AI) kiểm tra từng món: đúng loại, hàng mới, giá đọc trên trang cửa hàng và còn hàng, tổng không vượt
 * ngân sách, và lắp được với máy cũ (socket, loại DDR theo ProductRules, công suất nguồn cho card mới theo GPU_PSU).
 * Sai thì trả lỗi để trợ lý sửa; không bao giờ tự bịa giá hay link.
 */
@Slf4j
@Service
public class UpgradeService {

    static final int MAX_ROUNDS = 7;
    static final int MAX_SEARCH_ROUNDS = 2;
    static final int MAX_PROPOSE_ROUNDS = 4;
    /** Mỗi lượt chỉ chạy từng này lần tìm, tránh trợ lý gọi cả chục từ khoá một lúc */
    static final int MAX_SEARCHES_PER_ROUND = 8;

    static final String STAGE_READ = "Reading your current PC…";
    static final String STAGE_SEARCH = "Checking store prices…";
    static final String STAGE_CHECK = "Checking the parts fit your PC…";
    static final String STAGE_WRITE = "Writing your upgrade plan…";

    /** Loại linh kiện được đề xuất nâng cấp và tên hiển thị trên thẻ */
    static final Map<String, String> CATEGORIES = new LinkedHashMap<>();
    static {
        CATEGORIES.put("gpu", "Graphics card");
        CATEGORIES.put("ram", "Memory (RAM)");
        CATEGORIES.put("ssd", "Storage (SSD)");
        CATEGORIES.put("cpu", "Processor (CPU)");
        CATEGORIES.put("motherboard", "Motherboard");
        CATEGORIES.put("psu", "Power supply");
        CATEGORIES.put("cooler", "CPU cooler");
        CATEGORIES.put("fans", "Case fans");
        CATEGORIES.put("case", "Case");
    }

    private static final String PROMPT = """
            You are the upgrade adviser of PC SmartCare, an app that helps people without technical training look after \
            their own desktop PC. The owner wants to upgrade the PC they already have instead of buying a new one. You \
            receive their current parts (blank means they do not know), what they mainly use the PC for, and the money \
            they want to spend on the upgrade in VND. The budget is for the new parts only.

            1. Work out the bottleneck for their use. Typical order: gaming → graphics card first, then RAM up to 16 GB, \
            then an SSD if the PC still starts from a hard drive; study / office → SSD and RAM first; photo / video / 3D → \
            RAM 32 GB and a graphics card with more VRAM; programming → RAM and SSD; AI → NVIDIA card with the most VRAM \
            the budget allows. Do not replace a part that already meets the need. Prefer the fewest parts that make a \
            noticeable difference. If the PC is too old for a sensible upgrade (for example a CPU needs a new motherboard \
            and RAM too), say so plainly and suggest the best single step or saving towards a new PC.
            2. Respect the existing platform. New RAM must match the motherboard's DDR type. A new CPU must fit the current \
            motherboard socket; otherwise the plan also needs a motherboard (and RAM if the DDR type changes). A new \
            graphics card needs a power supply with enough wattage. Add a power supply ONLY when the current one is \
            known and too small for the new card; if it is known and big enough, keep it; if it is unknown, do not buy \
            one: the app asks the owner to read the label. Count a needed power supply in the card's cost: a card that runs \
            on the current power supply is often the better buy (for example RTX 4060 needs 450W, RTX 3060 needs 550W). Mini cases have limited space for graphics cards, coolers and \
            power supplies (usually SFX). Recommend RAM only when the PC's RAM type is known (from the RAM, motherboard or \
            CPU). When adding sticks, match the size of the current sticks so they stay in pairs; for programming, video \
            and AI aim for 32 GB in total. If most parts are unknown, suggest only parts that fit almost any desktop (a \
            graphics card when power allows, a 2.5-inch SATA SSD rather than M.2 when the motherboard is unknown or older \
            than about 2016) and tell the owner how to find the rest (Windows: \
            Settings › System › About, Task Manager › Performance). For AI use, only an NVIDIA card with at least 12 GB \
            of VRAM is worth buying.
            3. In your FIRST round, call search_products for every part you consider, all at once, with short queries \
            ("RAM DDR4 16GB 3200", "RTX 4060 8GB", "SSD NVMe 1TB", "PSU 650W 80 Plus"). Prefer results marked \
            verified = true and in_stock = true from Vietnamese computer stores. Skip used, open-box and tray items.
            4. Then call propose_upgrade with only the parts to buy. Choose each product by its id. The app checks price, \
            stock, the budget and compatibility with the current parts, and returns errors: fix exactly what it reports and \
            call propose_upgrade again. The total must not exceed the budget. If no useful upgrade fits the budget, call \
            propose_upgrade with an empty items list and explain why.
            5. The final answer is at most 90 words of plain English for a beginner: one line restating their use and \
            budget; what to upgrade and why it helps their use; which current parts they can keep; and anything to check \
            before buying (for example reading the power supply label). Do not list prices or links: the app shows them \
            on a card. Explain any technical term in a few words. Never invent prices, stock or products.
            """;

    private static final List<Map<String, Object>> TOOLS = List.of(
            tool("search_products", "Search Vietnamese computer stores for real products. Each result has a short id, title, "
                    + "shop, price_vnd, verified (price read on the store page) and in_stock.",
                    Map.of("type", "object", "properties", Map.of(
                            "query", Map.of("type", "string", "description", "Short query for one part type or one model")),
                            "required", List.of("query"))),
            tool("propose_upgrade", "Submit the parts to buy. The app validates price, stock, budget and compatibility with the "
                    + "owner's current parts, and reports problems to fix. Send an empty items list when no useful upgrade fits.",
                    Map.of("type", "object", "properties", Map.of(
                            "summary", Map.of("type", "string", "description", "At most 10 words: the owner's need, e.g. \"Smoother Full HD gaming\""),
                            "items", Map.of("type", "array", "items", Map.of("type", "object", "properties", Map.of(
                                    "category", Map.of("type", "string", "enum", List.copyOf(CATEGORIES.keySet())),
                                    "action", Map.of("type", "string", "enum", List.of("replace", "add"),
                                            "description", "replace: swaps the current part; add: goes next to it (extra RAM or SSD)"),
                                    "product_id", Map.of("type", "string", "description", "id of a search_products result"),
                                    "buy_qty", Map.of("type", "integer", "description", "Number of this product to buy, default 1"),
                                    "spec", Map.of("type", "string", "description", "At most 8 words, e.g. \"16 GB kit, 2 sticks, DDR4 3200\""),
                                    "reason", Map.of("type", "string", "description", "At most 14 words: why it helps this owner's use")),
                                    "required", List.of("category", "product_id")))),
                            "required", List.of("items"))));

    private static final Pattern WATTS = Pattern.compile("(\\d{3,4})\\s*w\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NVME = Pattern.compile("\\b(nvme|m\\.?2|pcie)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SFX = Pattern.compile("\\bsfx", Pattern.CASE_INSENSITIVE);

    private final LlmClient client;
    private final ProductSearch search;
    private final JsonMapper json;
    private final String model;
    private final int maxTokens;
    private final boolean disableThinking;

    public UpgradeService(LlmClient client, ProductSearch search, JsonMapper json,
            @Value("${app.ai.model:deepseek-v4-flash}") String model,
            @Value("${app.ai.max-tokens:4096}") int maxTokens,
            @Value("${app.ai.disable-thinking:false}") boolean disableThinking) {
        this.client = client;
        this.search = search;
        this.json = json;
        this.model = model;
        this.maxTokens = maxTokens;
        this.disableThinking = disableThinking;
    }

    /** Một món trong kế hoạch đã qua kiểm tra: sản phẩm thật, giá đã đọc trên trang cửa hàng */
    record Pick(String category, String action, ProductSearch.Product product, Offer offer, int qty, String spec, String reason) {
        long total() {
            return offer.priceVnd() * qty;
        }
    }

    /** Kế hoạch đã chốt; items rỗng là trợ lý kết luận không có món nâng cấp đáng mua trong ngân sách */
    record Plan(String summary, List<Pick> items, List<Issue> warnings, List<Issue> unknowns) {
    }

    /** Trạng thái một lượt trả lời: kết quả tìm kiếm theo id ngắn và kế hoạch đã chốt */
    static final class Turn {
        final UpgradeInfo pc;
        final Map<String, ProductSearch.Product> results = new ConcurrentHashMap<>();
        final AtomicInteger searches = new AtomicInteger();
        volatile Plan plan;
        /** Các khoản tiền người dùng tự viết trong hội thoại: câu trả lời được nhắc lại, không coi là giá bịa */
        volatile List<Long> userAmounts = List.of();
        /** Lỗi của lần propose_upgrade gần nhất chưa đạt: không chốt được thì nói đúng lý do cho người dùng */
        volatile List<String> lastErrors = List.of();

        Turn(UpgradeInfo pc) {
            this.pc = pc;
        }
    }

    public ChatResponse reply(ChatRequest request, Consumer<String> progress) {
        if (request.upgrade() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tell us about your current PC, its main use and your upgrade budget.");
        }
        return Io.within(ChatService.ANSWER_BUDGET, () -> run(request, progress));
    }

    @SuppressWarnings("unchecked")
    private ChatResponse run(ChatRequest request, Consumer<String> progress) {
        progress.accept(STAGE_READ);
        Turn turn = new Turn(request.upgrade());
        if (!search.enabled()) {
            return new ChatResponse("Store price search is not available right now, so I cannot suggest upgrade parts with real prices "
                    + "and links. Please try again later.");
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", PROMPT + "\n" + describe(turn.pc)
                + "\nLANGUAGE REQUIREMENT: answer in English. Keep brand names and model identifiers intact.\n"));
        for (var m : request.messages()) {
            if (messages.size() == 1 && "assistant".equals(m.role())) continue;
            messages.add(Map.of("role", m.role(), "content", m.content()));
        }
        if (messages.size() == 1 || !"user".equals(messages.getLast().get("role"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The final message must be a user question");
        }
        turn.userAmounts = request.messages().stream().filter(m -> "user".equals(m.role()))
                .flatMap(m -> amounts(m.content()).stream()).toList();
        int searchRounds = 0, proposeRounds = 0;
        boolean nudged = false;
        for (int round = 0; round < MAX_ROUNDS && !Io.expired(); round++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("max_tokens", maxTokens);
            if (disableThinking) body.put("thinking", Map.of("type", "disabled"));
            body.put("messages", messages);
            body.put("tools", searchRounds >= MAX_SEARCH_ROUNDS
                    ? TOOLS.stream().filter(t -> !"search_products".equals(((Map<?, ?>) t.get("function")).get("name"))).toList()
                    : TOOLS);
            if (proposeRounds >= MAX_PROPOSE_ROUNDS) body.put("tool_choice", "none");
            Map<String, Object> res = Io.await(Io.async(() -> client.complete(body)), Duration.ofSeconds(40), null);
            if (res == null) {
                if (turn.plan != null) return respond(null, turn);
                throw new AiUnavailableException(HttpStatus.GATEWAY_TIMEOUT, "The upgrade adviser is responding slowly. Please try again.");
            }
            var choices = (List<Map<String, Object>>) res.getOrDefault("choices", List.of());
            if (choices.isEmpty()) break;
            var message = (Map<String, Object>) choices.getFirst().getOrDefault("message", Map.of());
            var calls = (List<Map<String, Object>>) message.get("tool_calls");
            if (calls == null || calls.isEmpty()) {
                String draft = text(message.get("content"));
                // Chưa chốt kế hoạch qua công cụ: nhắc một lần, không để câu trả lời tự ghi giá chưa kiểm tra
                if (turn.plan == null && !nudged && proposeRounds < MAX_PROPOSE_ROUNDS) {
                    nudged = true;
                    messages.add(Map.of("role", "assistant", "content", Objects.requireNonNullElse(draft, "")));
                    messages.add(Map.of("role", "user", "content", "Submit the plan through the tools first: search_products if "
                            + "needed, then propose_upgrade (an empty items list if nothing useful fits). Do not quote prices without tool results."));
                    continue;
                }
                progress.accept(STAGE_WRITE);
                return respond(draft, turn);
            }
            messages.add(message);
            var names = calls.stream().map(c -> String.valueOf(((Map<?, ?>) c.get("function")).get("name"))).toList();
            boolean searchClosed = searchRounds >= MAX_SEARCH_ROUNDS;
            boolean proposeClosed = proposeRounds >= MAX_PROPOSE_ROUNDS;
            if (names.contains("search_products") && !searchClosed) {
                searchRounds++;
                progress.accept(STAGE_SEARCH);
            }
            if (names.contains("propose_upgrade") && !proposeClosed) {
                proposeRounds++;
                progress.accept(STAGE_CHECK);
            }
            AtomicInteger searchesThisRound = new AtomicInteger();
            var results = calls.stream().map(call -> {
                var fn = (Map<String, Object>) call.get("function");
                String name = String.valueOf(fn.get("name"));
                if ("search_products".equals(name) && (searchClosed || searchesThisRound.incrementAndGet() > MAX_SEARCHES_PER_ROUND)) {
                    return java.util.concurrent.CompletableFuture.completedFuture(
                            "{\"error\":\"The search limit has been reached. Use the results you already have.\"}");
                }
                if ("propose_upgrade".equals(name) && proposeClosed) {
                    return java.util.concurrent.CompletableFuture.completedFuture(
                            "{\"error\":\"The plan is final. Stop calling tools and write the answer in English.\"}");
                }
                return Io.async(() -> runTool(fn, turn));
            }).toList();
            for (int i = 0; i < calls.size(); i++) {
                messages.add(Map.of("role", "tool", "tool_call_id", String.valueOf(calls.get(i).get("id")),
                        "content", Io.await(results.get(i), ChatService.ANSWER_BUDGET,
                                "{\"error\":\"Lookup deadline reached. Use only checked results.\"}")));
            }
            progress.accept(turn.plan != null ? STAGE_WRITE : STAGE_CHECK);
        }
        return respond(null, turn);
    }

    @SuppressWarnings("unchecked")
    String runTool(Map<String, Object> function, Turn turn) {
        try {
            Object raw = function.get("arguments");
            Map<String, Object> input = raw == null || raw.toString().isBlank() ? Map.of() : json.readValue(raw.toString(), Map.class);
            Object out = switch (String.valueOf(function.get("name"))) {
                case "search_products" -> searchTool(String.valueOf(input.get("query")), turn);
                case "propose_upgrade" -> propose(input, turn);
                default -> Map.of("error", "Unknown tool");
            };
            return json.writeValueAsString(out);
        } catch (RuntimeException e) {
            log.warn("Upgrade: công cụ {} lỗi", function.get("name"), e);
            return "{\"error\":\"" + Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()).replace("\"", "'") + "\"}";
        }
    }

    private List<Map<String, Object>> searchTool(String query, Turn turn) {
        var found = search.search(query);
        int n = turn.searches.incrementAndGet();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < found.size(); i++) {
            var p = found.get(i);
            if (p.title() == null || ChatService.JUNK.matcher(p.title()).find()) continue;
            String id = "s" + n + "-" + (i + 1);
            turn.results.put(id, p);
            Offer checked = checked(p);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("title", p.title());
            m.put("shop", p.shop());
            m.put("price_vnd", checked == null ? p.priceVnd() : checked.priceVnd());
            String specs = ProductRules.describeAny(p.title());
            if (!specs.isEmpty()) m.put("specs", specs);
            m.put("verified", checked != null);
            var offer = search.offer(p.url());
            if (offer != null && offer.inStock() != null) m.put("in_stock", offer.inStock());
            rows.add(m);
        }
        return rows;
    }

    /** Giá đọc trên chính trang cửa hàng, đúng sản phẩm và còn hàng; null nếu chưa đủ chứng cứ */
    private Offer checked(ProductSearch.Product p) {
        if (p == null || p.url() == null) return null;
        Offer offer = search.offer(p.url());
        return OfferEvidence.verified(p.title(), offer, search.pageChecked(p.url())) ? offer : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> propose(Map<String, Object> input, Turn turn) {
        var raw = (List<Map<String, Object>>) input.getOrDefault("items", List.of());
        List<String> errors = new ArrayList<>();
        List<Pick> picks = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (var item : raw) {
            String category = String.valueOf(item.get("category")).toLowerCase(Locale.ROOT);
            String id = String.valueOf(item.get("product_id"));
            if (!CATEGORIES.containsKey(category)) {
                errors.add("Unknown category \"" + category + "\".");
                continue;
            }
            if (!seen.add(category)) {
                errors.add("Only one " + category + " line is allowed; use buy_qty for several of the same product.");
                continue;
            }
            var p = turn.results.get(id);
            if (p == null) {
                errors.add(category + ": product_id \"" + id + "\" is not a search result. Pick an id returned by search_products.");
                continue;
            }
            String kind = PriceEngine.kind(PriceEngine.norm(p.title()));
            String want = "motherboard".equals(category) ? "mainboard" : category;
            if (kind != null && !kind.equals(want)) {
                errors.add(category + ": \"" + p.title() + "\" is not a " + CATEGORIES.get(category).toLowerCase(Locale.ROOT) + ".");
                continue;
            }
            if ("ram".equals(category) && ChatService.serverRam(p.title())) {
                errors.add("ram: \"" + p.title() + "\" is server memory (ECC / registered) and does not suit a desktop PC.");
                continue;
            }
            Offer offer = checked(p);
            if (offer == null) {
                errors.add(category + ": the price or stock of \"" + p.title() + "\" could not be confirmed on the store page. "
                        + "Choose a result with verified = true and in_stock = true.");
                continue;
            }
            int qty = item.get("buy_qty") instanceof Number q ? Math.max(1, Math.min(4, q.intValue())) : 1;
            if (!ChatService.plausible(category, offer.priceVnd())) {
                errors.add(category + ": the price of \"" + p.title() + "\" looks wrong. Choose another result.");
                continue;
            }
            String action = "add".equals(item.get("action")) && Set.of("ram", "ssd", "fans").contains(category) ? "add" : "replace";
            picks.add(new Pick(category, action, p, offer, qty, text(item.get("spec")), text(item.get("reason"))));
        }
        List<Issue> warnings = new ArrayList<>(), unknowns = new ArrayList<>();
        if (errors.isEmpty()) errors.addAll(fit(turn.pc, picks, warnings, unknowns));
        long total = picks.stream().mapToLong(Pick::total).sum();
        long budget = turn.pc.budgetVnd();
        if (errors.isEmpty() && total > budget) {
            errors.add("The total " + ChatService.millions(total) + " is over the budget " + ChatService.millions(budget)
                    + " by " + ChatService.millions(total - budget) + ". Drop the least useful part or choose cheaper products.");
        }
        if (!errors.isEmpty()) {
            turn.lastErrors = List.copyOf(errors);
            log.info("Upgrade: đề xuất chưa đạt: {}", errors);
            return Map.of("ok", false, "errors", errors);
        }
        turn.plan = new Plan(text(input.get("summary")), List.copyOf(picks), warnings, unknowns);
        log.info("Upgrade: chốt {} món, tổng {}", picks.size(), total);
        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("ok", true);
        ok.put("total_vnd", total);
        ok.put("budget_left_vnd", budget - total);
        if (!warnings.isEmpty() || !unknowns.isEmpty()) {
            ok.put("check_before_buying", java.util.stream.Stream.concat(warnings.stream(), unknowns.stream()).map(Issue::message).toList());
        }
        String power = power(turn.pc, picks);
        if (power != null) ok.put("power_check", power + " State this correctly in the answer.");
        ok.put("next", picks.isEmpty() ? "No upgrade card will be shown. Explain why in the answer."
                : "The plan is accepted. Write the final answer now; do not call tools again.");
        return ok;
    }

    /**
     * Lắp được với máy cũ không. Trả về lỗi (phải đổi món); cảnh báo và điểm cần tự kiểm tra ghi vào warnings / unknowns.
     * Chỉ báo những gì liên quan tới món mới: máy cũ của người dùng không phải việc của kế hoạch này.
     */
    static List<String> fit(UpgradeInfo pc, List<Pick> picks, List<Issue> warnings, List<Issue> unknowns) {
        List<String> errors = new ArrayList<>();
        Map<String, Pick> by = new LinkedHashMap<>();
        picks.forEach(p -> by.put(p.category(), p));
        // Tên của từng món sau nâng cấp: món mới thay món cũ; món cũ không rõ thì bỏ trống (không đoán)
        Map<String, String> titles = new LinkedHashMap<>();
        if (UpgradeInfo.known(pc.cpu())) titles.put("cpu", pc.cpu());
        if (UpgradeInfo.known(pc.motherboard())) titles.put("motherboard", pc.motherboard());
        if (UpgradeInfo.known(pc.ram())) titles.put("ram", pc.ram());
        by.forEach((cat, p) -> {
            if (Set.of("cpu", "motherboard", "ram").contains(cat)) titles.put(cat, p.product().title());
        });
        for (var f : ProductRules.findings(titles)) {
            if (f.affectedParts().stream().noneMatch(by::containsKey)) continue;
            switch (f.severity()) {
                case ERROR -> errors.add(f.message());
                case WARNING -> warnings.add(new Issue(f.ruleId(), f.message()));
                case UNKNOWN -> unknowns.add(new Issue(f.ruleId(), f.message()));
                default -> { }
            }
        }
        // RAM mới (thêm hay thay) giữ bo cũ thì phải đúng loại DDR bo đang chạy; không biết loại thì không đề xuất
        Pick ram = by.get("ram");
        if (ram != null && !by.containsKey("motherboard")) {
            String type = ramType(pc), newType = ProductRules.read("ram", ram.product().title()).ramType();
            if (type == null) {
                errors.add("ram: this PC's RAM type (DDR3, DDR4 or DDR5) is unknown, so RAM cannot be matched. Remove the RAM line and "
                        + "tell the owner to check the sticker on a RAM stick or the motherboard model first.");
            } else if (newType != null && !type.equals(newType) && errors.stream().noneMatch(e -> e.contains("DDR"))) {
                errors.add("ram: this PC uses " + type + " but \"" + ram.product().title() + "\" is " + newType
                        + ". Choose " + type + " RAM; different DDR types do not fit.");
            }
        }
        if (ram != null && "add".equals(ram.action())) {
            unknowns.add(new Issue("RAM_SLOTS", "Adding sticks needs free RAM slots, and mixed sticks may run at the slower speed. "
                    + "Open the side panel and count the empty slots before buying."));
        }
        // CPU mới giữ bo cũ: socket theo bo (nếu đọc được tên), không thì theo CPU đang dùng (bo đang chạy được CPU đó).
        // Không xác định được socket nào (vd bo H81 đời cũ app không có trong bảng) thì không đề xuất CPU.
        Pick cpu = by.get("cpu");
        if (cpu != null && !by.containsKey("motherboard")) {
            String socket = ProductRules.read("cpu", cpu.product().title()).socket();
            String board = UpgradeInfo.known(pc.motherboard()) ? ProductRules.read("motherboard", pc.motherboard()).socket() : null;
            String current = board != null ? board : UpgradeInfo.known(pc.cpu()) ? ProductRules.read("cpu", pc.cpu()).socket() : null;
            if (current == null) {
                errors.add("cpu: this PC's CPU socket could not be identified from the motherboard or the current CPU, so a new CPU cannot be "
                        + "matched. Remove the CPU line and tell the owner to check the motherboard model first.");
            } else if (socket == null) {
                errors.add("cpu: the socket of \"" + cpu.product().title() + "\" could not be read. Choose a CPU listed with its exact model.");
            } else if (!socket.equals(current)) {
                if (errors.stream().noneMatch(e -> e.contains("socket"))) {
                    errors.add("cpu: \"" + cpu.product().title() + "\" uses socket " + socket + ", but this PC's motherboard uses " + current
                            + ". Choose a " + current + " CPU, or include a matching motherboard.");
                }
            } else if (board == null) {
                unknowns.add(new Issue("CPU_BOARD_UNKNOWN", "Your motherboard model is unknown. Its socket matches your current CPU, but it may "
                        + "need a BIOS update or may not support this CPU. Find the model in Windows (System Information › BaseBoard Product) "
                        + "and check its CPU support list before buying."));
            }
        }
        // CPU mới không có đồ họa tích hợp (đuôi F) mà máy không có card rời: lắp xong màn hình không lên
        Pick newGpu = by.get("gpu");
        if (cpu != null && newGpu == null) {
            Boolean igpu = ProductRules.hasIgpu(cpu.product().title());
            boolean noCard = "none".equalsIgnoreCase(Objects.requireNonNullElse(pc.gpu(), "").strip());
            if (noCard && Boolean.FALSE.equals(igpu)) {
                errors.add("cpu: \"" + cpu.product().title() + "\" has no built-in graphics and this PC has no graphics card, so the screen "
                        + "would stay black. Choose a CPU with built-in graphics (not an F model) or add a graphics card.");
            } else if (noCard && igpu == null) {
                unknowns.add(new Issue("CPU_IGPU", "Your PC has no graphics card, so the new CPU must have built-in graphics. Check this on "
                        + "the CPU's product page before buying."));
            } else if (!UpgradeInfo.known(pc.gpu()) && Boolean.FALSE.equals(igpu)) {
                unknowns.add(new Issue("CPU_IGPU", "This CPU has no built-in graphics and works only with a separate graphics card. "
                        + "Check that your PC has one."));
            }
        }
        // Card mới: nguồn phải đủ công suất hãng chip khuyên dùng
        Pick gpu = by.get("gpu");
        Pick psu = by.get("psu");
        Integer current = watts(pc.psu());
        // Nguồn cần cho cả máy: theo card mới, không thì theo card đang dùng (không card rời: 300W là đủ)
        boolean oldGpu = UpgradeInfo.known(pc.gpu()) && !"none".equalsIgnoreCase(pc.gpu().strip());
        int need = gpu != null ? gpuNeed(gpu.product().title()) : oldGpu ? gpuNeed(pc.gpu()) : 300;
        if (psu != null && current != null && current >= need) {
            errors.add("psu: the current " + current + "W power supply already covers the " + need + "W this PC needs. Remove the power "
                    + "supply line: buying one would waste the budget.");
        }
        Integer fresh = psu == null ? null : ChatService.amount("psu", psu.product().title());
        if (fresh != null && fresh < need) {
            errors.add("psu: \"" + psu.product().title() + "\" (" + fresh + "W) is below the " + need + "W this PC's graphics card needs.");
        }
        if (psu != null && SFX.matcher(Objects.requireNonNullElse(pc.psu(), "")).find() && !SFX.matcher(psu.product().title()).find()) {
            errors.add("psu: the current power supply is the small SFX size, so a standard ATX unit will not fit this case. Choose an SFX "
                    + "power supply.");
        }
        if (gpu != null) {
            Integer watts = psu != null ? fresh : current;
            if (psu == null && watts != null && watts < need) {
                errors.add("gpu: the current " + watts + "W power supply is below the " + need + "W recommended for \"" + gpu.product().title()
                        + "\". Add a power supply of at least " + need + "W, or choose a card that needs less power.");
            } else if (watts == null) {
                unknowns.add(new Issue("PSU_UNKNOWN", "This graphics card needs a power supply of at least " + need + "W with the right "
                        + "PCIe power cable. Read the label on the side of your power supply before buying."));
            }
            unknowns.add(new Issue("GPU_CLEARANCE", "mini".equals(pc.caseSize())
                    ? "Mini cases fit only short cards. Measure the free length in your case and compare it with the card's length."
                    : "Measure the free length inside your case and compare it with the card's length before buying."));
        }
        if (psu != null && "mini".equals(pc.caseSize()) && !SFX.matcher(psu.product().title()).find()
                && !SFX.matcher(Objects.requireNonNullElse(pc.psu(), "")).find()) {
            warnings.add(new Issue("PSU_FORM", "Mini cases often take only small SFX power supplies. Check which size your case needs."));
        }
        if (gpu != null && AI_USE.matcher(pc.purpose()).find()) {
            String t = gpu.product().title().toLowerCase(Locale.ROOT);
            Integer vram = ChatService.vram(gpu.product().title());
            boolean nvidia = t.matches(".*\\b(rtx|gtx|geforce|nvidia)\\b.*");
            if (!nvidia || vram == null || vram < 12) {
                errors.add("gpu: for AI work choose an NVIDIA card with at least 12 GB of VRAM (for example RTX 3060 12GB or RTX 4060 Ti / "
                        + "5060 Ti 16GB). \"" + gpu.product().title() + "\" does not qualify. If none fits the budget, send an empty plan and explain.");
            }
        }
        Pick ssd = by.get("ssd");
        if (ssd != null && NVME.matcher(ssd.product().title()).find()) {
            unknowns.add(new Issue("M2_SLOT", "This SSD needs a free M.2 slot on the motherboard. Look up your motherboard model (Windows: System Information › "
                    + "BaseBoard Product) to see whether it has one; if not, choose a 2.5-inch SATA SSD."));
        }
        Pick cooler = by.get("cooler");
        if (cooler != null) {
            unknowns.add(new Issue("COOLER_HEIGHT", "Check the cooler height fits your case and that it supports your CPU socket."));
        }
        return errors;
    }

    private static final Pattern AI_USE = Pattern.compile(
            "\\b(ai|machine learning|deep learning|stable diffusion|llms?|cuda|pytorch|tensorflow|comfyui)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OLD_GT = Pattern.compile("\\bgt\\s?(710|730|1030)\\b", Pattern.CASE_INSENSITIVE);
    /**
     * Công suất nguồn hãng khuyên cho cả máy, bổ sung GPU_PSU cho card yếu / mạnh mà máy cũ hay có. GPU_PSU của trang lắp máy
     * mới để mặc định 550W và sàn 450W khi chọn nguồn mới; với nguồn ĐANG CÓ thì phải theo đúng mức của card (GTX 1650,
     * RTX 3050 6GB chạy với nguồn 300W), không thì chặn oan máy văn phòng nguồn 300 - 400W.
     */
    static final Map<String, Integer> MORE_PSU = Map.ofEntries(
            Map.entry("gtx1630", 300), Map.entry("gtx1050", 300), Map.entry("gtx1050ti", 300), Map.entry("rx6400", 300),
            Map.entry("rx6500xt", 400), Map.entry("arca380", 300), Map.entry("arca580", 500), Map.entry("arca750", 550),
            Map.entry("gtx1660", 450), Map.entry("gtx1660super", 450), Map.entry("gtx1660ti", 450), Map.entry("rtx2060", 500),
            Map.entry("rx580", 500), Map.entry("rx6750xt", 650), Map.entry("rtx3070ti", 750), Map.entry("rtx3080", 750),
            Map.entry("rtx4080", 750), Map.entry("rtx4080super", 750), Map.entry("rtx5080", 850), Map.entry("rtx4090", 850),
            Map.entry("rx7900xt", 750), Map.entry("rx7900xtx", 800), Map.entry("rtx5090", 1000));

    /** Nguồn (W) cần cho cả máy khi dùng card này; không đọc được chip thì coi như 550W */
    static int gpuNeed(String title) {
        if (title == null) return 550;
        if (OLD_GT.matcher(title).find()) return 300;
        String chip = ChatService.gpuChip(title);
        if (chip == null) return 550;
        if ("rtx3050".equals(chip) && Integer.valueOf(6).equals(ChatService.vram(title))) return 300;
        return MORE_PSU.getOrDefault(chip, ChatService.GPU_PSU.getOrDefault(chip, 550));
    }

    /** Loại RAM máy đang chạy: theo thanh RAM người dùng ghi, không có thì theo bo, rồi theo CPU (AM4 / AM5); null nếu không rõ */
    static String ramType(UpgradeInfo pc) {
        if (UpgradeInfo.known(pc.ram())) {
            String t = ProductRules.read("ram", pc.ram()).ramType();
            if (t != null) return t;
        }
        if (UpgradeInfo.known(pc.motherboard())) {
            String t = ProductRules.read("motherboard", pc.motherboard()).ramType();
            if (t != null) return t;
        }
        return UpgradeInfo.known(pc.cpu()) ? ProductRules.read("cpu", pc.cpu()).ramType() : null;
    }

    /** Kết luận về nguồn của kế hoạch đã chốt, gửi cho trợ lý để câu trả lời không nói sai (vd "nguồn 450W quá yếu" khi vẫn đủ) */
    static String power(UpgradeInfo pc, List<Pick> picks) {
        Pick gpu = picks.stream().filter(p -> "gpu".equals(p.category())).findFirst().orElse(null);
        if (gpu == null) return null;
        int need = gpuNeed(gpu.product().title());
        Pick psu = picks.stream().filter(p -> "psu".equals(p.category())).findFirst().orElse(null);
        if (psu != null) return "The new power supply in the plan covers the " + need + "W this graphics card needs.";
        Integer current = watts(pc.psu());
        return current != null
                ? "The current " + current + "W power supply already covers the " + need + "W this graphics card needs, so it is kept."
                : "The current power supply is unknown: the owner must check its label shows at least " + need + "W before buying.";
    }

    /** Công suất nguồn người dùng ghi, vd "550W", "Cooler Master 650 W"; null nếu không đọc được */
    static Integer watts(String psu) {
        if (!UpgradeInfo.known(psu)) return null;
        var m = WATTS.matcher(psu);
        if (!m.find()) return null;
        int w = Integer.parseInt(m.group(1));
        return w >= 150 && w <= 2000 ? w : null;
    }

    /** Thông tin máy hiện tại cho trợ lý, ô trống ghi rõ là không biết */
    static String describe(UpgradeInfo pc) {
        StringBuilder s = new StringBuilder("\nTHE OWNER'S CURRENT PC (\"unknown\" = they do not know):\n");
        line(s, "CPU", pc.cpu());
        line(s, "Motherboard", pc.motherboard());
        line(s, "RAM", pc.ram());
        line(s, "Graphics card", "none".equalsIgnoreCase(Objects.requireNonNullElse(pc.gpu(), "").strip())
                ? "none (uses the CPU's built-in graphics)" : pc.gpu());
        line(s, "Storage", pc.storage());
        line(s, "Power supply", pc.psu());
        s.append("- Case size: ").append(pc.caseSize() == null ? "unknown" : pc.caseSize()).append('\n');
        String board = ProductRules.read("motherboard", pc.motherboard()).describe(), cpu = ProductRules.read("cpu", pc.cpu()).describe();
        if (!board.isEmpty()) s.append("- App reading of the motherboard: ").append(board).append('\n');
        if (!cpu.isEmpty()) s.append("- App reading of the CPU: ").append(cpu).append('\n');
        s.append("MAIN USE: ").append(pc.purpose().strip()).append('\n');
        s.append("UPGRADE BUDGET: ").append(ChatService.millions(pc.budgetVnd())).append(" (").append(pc.budgetVnd()).append(" VND), new parts only.\n");
        return s.toString();
    }

    private static void line(StringBuilder s, String label, String value) {
        s.append("- ").append(label).append(": ").append(UpgradeInfo.known(value) ? value.strip() : "unknown").append('\n');
    }

    /** Câu trả lời và thẻ kế hoạch. Không có kế hoạch đã kiểm tra thì không đưa giá nào. */
    private ChatResponse respond(String draft, Turn turn) {
        Plan plan = turn.plan;
        String reply = draft == null ? null : checkedReply(draft.replaceAll("(?s)<think>.*?</think>", ""), turn);
        if (plan == null) return new ChatResponse(noPlan(turn));
        if (plan.items().isEmpty()) {
            // Câu của trợ lý bị bỏ hết (toàn số tiền chưa kiểm tra): có lỗi đề xuất trước đó thì nói đúng lý do đó
            if (reply == null || reply.isEmpty()) {
                return new ChatResponse(!turn.lastErrors.isEmpty() ? noPlan(turn) : "I could not find an upgrade within "
                        + ChatService.millions(turn.pc.budgetVnd()) + " that would make a noticeable difference for your use.");
            }
            return new ChatResponse(reply);
        }
        if (reply == null || reply.isEmpty()) {
            reply = "Here is an upgrade plan for your PC. The card lists each part, its store price and what to check before buying.";
        }
        List<Line> lines = plan.items().stream().map(p -> new Line("upgrade_" + p.category(), p.category(),
                ("add".equals(p.action()) ? "Extra " : "New ") + CATEGORIES.get(p.category()).toLowerCase(Locale.ROOT), p.qty(),
                ChatService.purchasedSpec(p.category(), p.product().title(), p.spec()), p.reason(), p.total(), p.total(),
                p.offer().imageUrl() != null ? p.offer().imageUrl() : p.product().imageUrl(), p.product().url(), p.product().title(),
                p.product().shop(), p.offer().inStock(), p.offer().warrantyMonths(), p.offer().checkedAt(), List.of(), true)).toList();
        String summary = plan.summary() == null || plan.summary().isBlank() ? "Your upgrade plan" : plan.summary();
        var card = new BuildCard(lines, true, List.of(), plan.warnings(), summary, turn.pc.budgetVnd(), null, null, 1,
                plan.unknowns(), List.of(), null);
        return new ChatResponse(reply, card);
    }

    private static final Pattern MILLIONS = Pattern.compile("(?<![\\p{L}\\d.,])(\\d+(?:[.,]\\d+)?)\\s*(million|mil|triệu|tr|m)\\b(?!\\.\\d)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DONG = Pattern.compile(
            "(?:(?:vnd|₫)\\s*(\\d{1,3}(?:[.,]\\d{3})+|\\d{4,})(?![\\d.,]*\\s*(?:million|mil|triệu|tr|m)\\b))"
                    + "|(?:(?<![\\p{L}\\d.,])(\\d{1,3}(?:[.,]\\d{3})+|\\d{4,})\\s*(?:vnd|₫|đồng|đ|dong)(?![\\p{L}\\d]))", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]*)\\]\\(\\s*https?://[^)]*\\)|https?://\\S+|www\\.\\S+");

    /** Mọi khoản tiền (đồng) viết trong câu: "8.45 million", "1.2M", "VND 500,000", "1.190.000đ" */
    static List<Long> amounts(String text) {
        List<Long> out = new ArrayList<>();
        if (text == null) return out;
        var m = MILLIONS.matcher(text);
        while (m.find()) {
            try {
                out.add(Math.round(Double.parseDouble(m.group(1).replace(',', '.')) * 1_000_000));
            } catch (NumberFormatException ignored) { }
        }
        var d = DONG.matcher(text);
        while (d.find()) {
            String n = d.group(1) != null ? d.group(1) : d.group(2);
            out.add(Long.parseLong(n.replaceAll("[.,]", "")));
        }
        return out;
    }

    /**
     * Câu trả lời cuối của trợ lý chỉ được nêu số tiền có trong dữ liệu đã kiểm tra (ngân sách, số người dùng viết, giá từng
     * món, tổng, tiền còn lại) và không được có link (link nằm trên thẻ, lấy từ kết quả tìm kiếm). Câu nào có số tiền khác
     * thì bỏ cả câu; bỏ hết thì trả null để dùng câu dẫn có sẵn.
     */
    static String checkedReply(String draft, Turn turn) {
        if (draft == null) return null;
        List<Long> allowed = new ArrayList<>(turn.userAmounts);
        allowed.add(turn.pc.budgetVnd());
        Plan plan = turn.plan;
        if (plan != null && !plan.items().isEmpty()) {
            long total = 0;
            for (var p : plan.items()) {
                allowed.add(p.total());
                allowed.add(p.offer().priceVnd());
                total += p.total();
            }
            allowed.add(total);
            allowed.add(turn.pc.budgetVnd() - total);
        }
        StringBuilder out = new StringBuilder();
        for (String line : draft.strip().split("\n", -1)) {
            String kept = java.util.Arrays.stream(LINK.matcher(line).replaceAll(r -> r.group(1) != null ? java.util.regex.Matcher.quoteReplacement(r.group(1)) : "")
                            .split("(?<=[.!?])\\s+"))
                    .filter(sentence -> {
                        boolean ok = amounts(sentence).stream().allMatch(a -> allowed.stream()
                                .anyMatch(known -> Math.abs(a - known) <= Math.max(60_000, known * 0.03)));
                        if (!ok) log.info("Upgrade: bỏ câu có số tiền chưa kiểm tra: {} (số được phép {})", sentence, allowed);
                        return ok;
                    })
                    .collect(java.util.stream.Collectors.joining(" "));
            if (!kept.isBlank() || line.isBlank()) out.append(kept.strip()).append('\n');
        }
        String text = out.toString().replaceAll("\n{3,}", "\n\n").strip();
        return text.isEmpty() ? null : text;
    }

    /** Câu trả lời khi không chốt được kế hoạch: theo lỗi của lần đề xuất cuối, không đưa giá chưa kiểm tra */
    static String noPlan(Turn turn) {
        List<String> why = new ArrayList<>();
        var errors = turn.lastErrors;
        if (errors.stream().anyMatch(e -> e.contains("over the budget"))) {
            why.add("the parts that would make a real difference cost more than your " + ChatService.millions(turn.pc.budgetVnd()) + " budget");
        }
        if (errors.stream().anyMatch(e -> e.contains("could not be confirmed"))) {
            why.add("store prices or stock could not be confirmed for the parts I found");
        }
        if (errors.stream().anyMatch(e -> e.contains("power supply") || e.contains("socket") || e.contains("DDR"))) {
            why.add("the parts I found would need other changes to fit your PC (power supply, socket or RAM type)");
        }
        if (errors.stream().anyMatch(e -> e.contains("is unknown") || e.contains("not known"))) {
            why.add("some of your current parts are unknown, so I could not match them safely");
        }
        String reason = why.isEmpty() ? "store prices and stock could not be confirmed in time" : String.join("; ", why);
        return "I could not confirm an upgrade for your PC: " + reason + ". You can try again later, raise the budget, or fill in more "
                + "of your current parts (Windows: Settings › System › About, Task Manager › Performance).";
    }

    private static String text(Object o) {
        return o == null || o.toString().isBlank() ? null : o.toString().strip();
    }

    private static Map<String, Object> tool(String name, String description, Map<String, Object> parameters) {
        return Map.of("type", "function", "function", Map.of("name", name, "description", description, "parameters", parameters));
    }
}
