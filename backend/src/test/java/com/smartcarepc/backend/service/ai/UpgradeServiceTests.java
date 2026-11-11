package com.smartcarepc.backend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.smartcarepc.backend.dto.ChatRequest;
import com.smartcarepc.backend.dto.ChatRequest.Message;
import com.smartcarepc.backend.dto.CompatibilityResponse.Issue;
import com.smartcarepc.backend.dto.UpgradeInfo;
import com.smartcarepc.backend.service.price.Offer;

import tools.jackson.databind.json.JsonMapper;

/** Tư vấn nâng cấp: app tự kiểm giá, còn hàng, ngân sách và lắp được với máy cũ; AI giả lập bằng mock. */
class UpgradeServiceTests {

    private final LlmClient client = mock(LlmClient.class);
    private final ProductSearch search = mock(ProductSearch.class);
    private final List<Map<String, Object>> sent = new ArrayList<>();
    private UpgradeService service;

    private static final ProductSearch.Product RTX4070 = product("VGA ASUS Dual GeForce RTX 4070 12GB GDDR6X", 15_490_000L);
    private static final ProductSearch.Product RTX4060 = product("VGA GIGABYTE GeForce RTX 4060 WINDFORCE OC 8GB", 7_990_000L);
    private static final ProductSearch.Product PSU650 = product("Nguồn MSI MAG A650BN 650W 80 Plus Bronze", 1_390_000L);
    private static final ProductSearch.Product DDR5 = product("RAM Kingston Fury Beast 16GB DDR5 5600", 1_590_000L);
    private static final ProductSearch.Product DDR4 = product("RAM Kingston Fury Beast 16GB (2x8GB) DDR4 3200", 1_150_000L);

    @BeforeEach
    void setUp() {
        when(search.enabled()).thenReturn(true);
        when(search.pageChecked(any())).thenReturn(true);
        for (var p : List.of(RTX4070, RTX4060, PSU650, DDR5, DDR4)) {
            when(search.offer(p.url())).thenReturn(new Offer(p.title(), p.shop(), p.priceVnd(), null, p.url(), null, true, 36, Instant.now()));
        }
        when(search.search(any())).thenReturn(List.of(RTX4070, RTX4060, PSU650, DDR5, DDR4));
        service = new UpgradeService(client, search, JsonMapper.builder().build(), "test-model", 512, false);
    }

    private static UpgradeInfo pc(String psu, long budget) {
        return new UpgradeInfo("Intel Core i5-10400", "ASUS PRIME B460M-A", "8GB DDR4 2666", "none", "HDD 1TB", psu,
                "standard", "Gaming: Valorant and GTA V at Full HD", budget);
    }

    private ChatRequest ask(UpgradeInfo info) {
        return new ChatRequest(List.of(new Message("user", "Please suggest an upgrade for my PC.")), null, null, "upgrade", info);
    }

    @Test
    void cardMoiCanNguonDuCongSuatVaTongKhongVuotNganSach() {
        reply(toolCall("s", "search_products", "{\"query\":\"RTX 4070\"}"),
                toolCall("p1", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-1\"}]}"),
                toolCall("p2", "propose_upgrade", """
                        {"summary":"Smoother Full HD gaming","items":[{"category":"gpu","product_id":"s1-1"},{"category":"psu","product_id":"s1-3"}]}"""),
                answer("Upgrade the graphics card and power supply."));
        var res = service.reply(ask(pc("450W", 18_000_000L)), s -> { });

        assertThat(toolResult(2)).contains("450W power supply is below the 650W");
        assertThat(res.build()).isNotNull();
        assertThat(res.build().items()).extracting(l -> l.category()).containsExactly("gpu", "psu");
        assertThat(res.build().items()).allSatisfy(l -> {
            assertThat(l.sourceUrl()).startsWith("https://");
            assertThat(l.priceChecked()).isTrue();
        });
        assertThat(res.build().budgetVnd()).isEqualTo(18_000_000L);
        assertThat(res.build().unknowns()).extracting(Issue::rule).contains("GPU_CLEARANCE");
        assertThat(res.reply()).isEqualTo("Upgrade the graphics card and power supply.");
    }

    @Test
    void vuotNganSachThiBaoLoiDeChonLai() {
        reply(toolCall("s", "search_products", "{\"query\":\"RTX\"}"),
                toolCall("p1", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-1\"},{\"category\":\"psu\",\"product_id\":\"s1-3\"}]}"),
                toolCall("p2", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-2\"}]}"),
                answer("An RTX 4060 fits your budget."));
        var res = service.reply(ask(pc("550W", 9_000_000L)), s -> { });

        assertThat(toolResult(2)).contains("over the budget");
        assertThat(res.build().items()).singleElement().satisfies(l -> assertThat(l.productName()).contains("RTX 4060"));
    }

    @Test
    void ramKhacLoaiDdrVoiBoMachBiTuChoi() {
        reply(toolCall("s", "search_products", "{\"query\":\"RAM\"}"),
                toolCall("p1", "propose_upgrade", "{\"items\":[{\"category\":\"ram\",\"product_id\":\"s1-4\"}]}"),
                toolCall("p2", "propose_upgrade", "{\"items\":[{\"category\":\"ram\",\"product_id\":\"s1-5\"}]}"),
                answer("Replace the RAM with a 16 GB DDR4 kit."));
        var res = service.reply(ask(pc("550W", 3_000_000L)), s -> { });

        assertThat(toolResult(2)).contains("DDR4").contains("DDR5");
        assertThat(res.build().items()).singleElement().satisfies(l -> assertThat(l.productName()).contains("DDR4"));
    }

    @Test
    void giaChuaDocTrenTrangCuaHangKhongDuocChot() {
        when(search.pageChecked(RTX4060.url())).thenReturn(false);
        reply(toolCall("s", "search_products", "{\"query\":\"RTX 4060\"}"),
                toolCall("p1", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-2\"}]}"),
                answer("Here is the plan."), answer("Here is the plan."));
        var res = service.reply(ask(pc("550W", 9_000_000L)), s -> { });

        assertThat(toolResult(2)).contains("could not be confirmed on the store page");
        assertThat(res.build()).isNull();
        assertThat(res.reply()).contains("could not put together an upgrade");
    }

    @Test
    void khongGoiCongCuThiKhongTuBaoGia() {
        reply(answer("Buy an RTX 4060 for about 8 million."), answer("Buy an RTX 4060 for about 8 million."));
        var res = service.reply(ask(pc("550W", 9_000_000L)), s -> { });

        assertThat(res.build()).isNull();
        assertThat(res.reply()).doesNotContain("8 million");
    }

    @Test
    void cpuMoiMaKhongBietBoMachThiGhiCanKiemTra() {
        var cpu = product("CPU Intel Core i5-12400F", 2_990_000L);
        var offer = new Offer(cpu.title(), "GEARVN", cpu.priceVnd(), null, cpu.url(), null, true, 36, Instant.now());
        List<Issue> warnings = new ArrayList<>(), unknowns = new ArrayList<>();
        var info = new UpgradeInfo("i3-10100", "", "", "", "", "", "unknown", "Office work", 5_000_000L);
        var errors = UpgradeService.fit(info, List.of(new UpgradeService.Pick("cpu", "replace", cpu, offer, 1, null, null)), warnings, unknowns);

        assertThat(errors).isEmpty();
        assertThat(unknowns).extracting(Issue::rule).contains("CPU_BOARD_UNKNOWN");
        assertThat(unknowns.stream().filter(i -> i.rule().equals("CPU_BOARD_UNKNOWN")).findFirst().orElseThrow().message()).contains("LGA1700");
    }

    @Test
    void cpuKhacSocketVoiBoHienTaiLaLoi() {
        var cpu = product("CPU Intel Core i5-12400F", 2_990_000L);
        var offer = new Offer(cpu.title(), "GEARVN", cpu.priceVnd(), null, cpu.url(), null, true, 36, Instant.now());
        var errors = UpgradeService.fit(pc("550W", 5_000_000L), List.of(new UpgradeService.Pick("cpu", "replace", cpu, offer, 1, null, null)),
                new ArrayList<>(), new ArrayList<>());

        assertThat(errors).singleElement().asString().contains("LGA1700").contains("LGA1200");
    }

    @Test
    void docCongSuatNguonNguoiDungGhi() {
        assertThat(UpgradeService.watts("Cooler Master 650 W")).isEqualTo(650);
        assertThat(UpgradeService.watts("550W bronze")).isEqualTo(550);
        assertThat(UpgradeService.watts("I don't know")).isNull();
        assertThat(UpgradeService.watts("")).isNull();
    }

    @Test
    void moTaMayChoTroLyGhiRoOChuaBiet() {
        String text = UpgradeService.describe(new UpgradeInfo("", "B460M", null, "none", "", "", null, "Study", 2_000_000L));
        assertThat(text).contains("- CPU: unknown", "none (uses the CPU's built-in graphics)", "socket LGA1200", "UPGRADE BUDGET");
    }

    // ---- Phản hồi giả theo chuẩn OpenAI chat/completions ---------------

    private static ProductSearch.Product product(String title, long price) {
        return new ProductSearch.Product(title, "GEARVN", price, "https://gearvn.com/products/" + Math.abs(title.hashCode()), null);
    }

    private static Map<String, Object> answer(String text) {
        return Map.of("choices", List.of(Map.of("finish_reason", "stop", "message", Map.of("role", "assistant", "content", text))));
    }

    private static Map<String, Object> toolCall(String id, String name, String arguments) {
        return Map.of("choices", List.of(Map.of("finish_reason", "tool_calls", "message", Map.of("role", "assistant", "content", "",
                "tool_calls", List.of(Map.of("id", id, "type", "function", "function", Map.of("name", name, "arguments", arguments)))))));
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    private void reply(Map<String, Object>... responses) {
        var queue = new ArrayList<>(List.of(responses));
        when(client.complete(any())).thenAnswer(inv -> {
            Map<String, Object> body = inv.getArgument(0);
            var copy = new LinkedHashMap<>(body);
            copy.put("messages", new ArrayList<>((List<Object>) body.get("messages")));
            sent.add(copy);
            return queue.removeFirst();
        });
    }

    /** Nội dung kết quả công cụ cuối cùng mà AI nhận được ở lần gọi thứ call */
    @SuppressWarnings("unchecked")
    private String toolResult(int call) {
        var messages = (List<Map<String, Object>>) sent.get(call).get("messages");
        return String.valueOf(messages.getLast().get("content"));
    }
}
