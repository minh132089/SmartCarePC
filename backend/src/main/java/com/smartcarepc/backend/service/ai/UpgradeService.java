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
    static final int MAX_PROPOSE_ROUNDS = 3;
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
            graphics card needs a power supply with enough wattage: if the current one is too small, include a power \
            supply. Mini cases have limited space for graphics cards, coolers and power supplies (often SFX). When adding \
            RAM, prefer a matching 2-stick kit that replaces the old sticks unless the old type and size are clear.
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
        // Thêm RAM cạnh thanh cũ: phải cùng loại DDR với thanh đang có
        Pick ram = by.get("ram");
        if (ram != null && "add".equals(ram.action()) && UpgradeInfo.known(pc.ram())) {
            String oldType = ProductRules.read("ram", pc.ram()).ramType(), newType = ProductRules.read("ram", ram.product().title()).ramType();
            if (oldType != null && newType != null && !oldType.equals(newType)) {
                errors.add("ram: the current RAM is " + oldType + " but \"" + ram.product().title() + "\" is " + newType
                        + ". Different DDR types cannot be used together.");
            }
            unknowns.add(new Issue("RAM_SLOTS", "Adding sticks needs free RAM slots, and mixed sticks may run at the slower speed. "
                    + "Open the side panel and count the empty slots before buying."));
        }
        // CPU mới mà không biết bo mạch: chưa thể khẳng định lắp được
        Pick cpu = by.get("cpu");
        if (cpu != null && !by.containsKey("motherboard") && !UpgradeInfo.known(pc.motherboard())) {
            String socket = ProductRules.read("cpu", cpu.product().title()).socket();
            unknowns.add(new Issue("CPU_BOARD_UNKNOWN", "Your motherboard model is unknown, so this CPU is not confirmed to fit."
                    + (socket == null ? "" : " It needs a socket " + socket + " motherboard.")
                    + " Find the model in Windows (System Information › BaseBoard Product) before buying."));
        }
        // Card mới: nguồn phải đủ công suất hãng chip khuyên dùng
        Pick gpu = by.get("gpu");
        if (gpu != null) {
            String chip = ChatService.gpuChip(gpu.product().title());
            int need = Math.max(450, chip == null ? 550 : ChatService.GPU_PSU.getOrDefault(chip, 550));
            Pick psu = by.get("psu");
            Integer watts = psu != null ? ChatService.amount("psu", psu.product().title()) : watts(pc.psu());
            if (watts != null && watts < need) {
                errors.add(psu != null
                        ? "psu: \"" + psu.product().title() + "\" (" + watts + "W) is below the " + need + "W recommended for this graphics card."
                        : "gpu: the current " + watts + "W power supply is below the " + need + "W recommended for \"" + gpu.product().title()
                                + "\". Add a power supply of at least " + need + "W, or choose a card that needs less power.");
            } else if (watts == null) {
                unknowns.add(new Issue("PSU_UNKNOWN", "This graphics card needs a power supply of at least " + need + "W with the right "
                        + "PCIe power cable. Read the label on the side of your power supply before buying."));
            }
            unknowns.add(new Issue("GPU_CLEARANCE", "mini".equals(pc.caseSize())
                    ? "Mini cases fit only short cards. Measure the free length in your case and compare it with the card's length."
                    : "Measure the free length inside your case and compare it with the card's length before buying."));
        }
        Pick psu = by.get("psu");
        if (psu != null && "mini".equals(pc.caseSize()) && !SFX.matcher(psu.product().title()).find()) {
            warnings.add(new Issue("PSU_FORM", "Mini cases often take only small SFX power supplies. Check which size your case needs."));
        }
        Pick ssd = by.get("ssd");
        if (ssd != null && NVME.matcher(ssd.product().title()).find()) {
            unknowns.add(new Issue("M2_SLOT", "This SSD needs a free M.2 slot on the motherboard. If there is none, choose a 2.5-inch SATA SSD."));
        }
        Pick cooler = by.get("cooler");
        if (cooler != null) {
            unknowns.add(new Issue("COOLER_HEIGHT", "Check the cooler height fits your case and that it supports your CPU socket."));
        }
        return errors;
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
        String reply = draft == null ? null : draft.replaceAll("(?s)<think>.*?</think>", "").strip();
        if (plan == null) {
            return new ChatResponse("I could not put together an upgrade with confirmed store prices and stock for your PC yet. "
                    + "Please try again in a few minutes, or adjust the budget.");
        }
        if (plan.items().isEmpty()) {
            return new ChatResponse(reply == null || reply.isEmpty()
                    ? "I could not find an upgrade within " + ChatService.millions(turn.pc.budgetVnd()) + " that would make a noticeable "
                            + "difference for your use."
                    : reply);
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

    private static String text(Object o) {
        return o == null || o.toString().isBlank() ? null : o.toString().strip();
    }

    private static Map<String, Object> tool(String name, String description, Map<String, Object> parameters) {
        return Map.of("type", "function", "function", Map.of("name", name, "description", description, "parameters", parameters));
    }
}
