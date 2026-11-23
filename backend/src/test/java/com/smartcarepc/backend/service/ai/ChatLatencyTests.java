package com.smartcarepc.backend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.smartcarepc.backend.dto.ChatRequest;
import com.smartcarepc.backend.dto.CompatibilityResponse;
import com.smartcarepc.backend.dto.ComponentResponse;
import com.smartcarepc.backend.exception.ResourceNotFoundException;
import com.smartcarepc.backend.service.CompatibilityService;
import com.smartcarepc.backend.service.ComponentService;
import com.smartcarepc.backend.service.ModelMatchService;
import com.smartcarepc.backend.service.PcBuildService;
import com.smartcarepc.backend.service.price.Offer;
import com.smartcarepc.backend.service.Io;
import com.smartcarepc.backend.exception.AiUnavailableException;
import tools.jackson.databind.json.JsonMapper;

class ChatLatencyTests {
    @Test
    void moiLanGoiAiChuaLaiThoiGianDungThe() {
        // Chờ tới hạn chung trừ 8 giây cho bước dựng thẻ (không còn cắt 40 giây mỗi lần); còn 5 giây: không gọi nữa
        assertThat(Io.within(java.time.Duration.ofSeconds(85), ChatService::aiWait).toSeconds()).isBetween(76L, 77L);
        assertThat(Io.within(java.time.Duration.ofSeconds(20), ChatService::aiWait).toSeconds()).isBetween(11L, 12L);
        assertThat(Io.within(java.time.Duration.ofSeconds(5), ChatService::aiWait)).isZero();
        assertThat(Io.within(java.time.Duration.ofSeconds(20), ChatService::wrapUp)).isTrue();
        assertThat(Io.within(java.time.Duration.ofSeconds(85), ChatService::wrapUp)).isFalse();
    }

    @Test
    void daNeuCardRamHayDoPhanGiaiThiKhongHoiLaiMucGame() {
        // Ca kiểm tra 23/11 (GPT-6.1-sol hỏi lại thay vì làm): người dùng đã nêu cấu hình cụ thể
        assertThat(ChatService.knownWorkloadTopics(List.of("PC game 31 triệu, bắt buộc đúng RTX 4060 8GB, RAM 32GB, SSD 1TB.")) & 2).isEqualTo(2);
        assertThat(ChatService.knownWorkloadTopics(List.of("Build PC chơi game Full HD 27 triệu, card nvida, RAM 16GB.")) & 2).isEqualTo(2);
        assertThat(ChatService.knownWorkloadTopics(List.of("Cần PC Mini-ITX chơi game 33 triệu, RAM 16GB.")) & 2).isEqualTo(2);
        // Chỉ nói "chơi game" và ngân sách: vẫn hỏi mức game
        assertThat(ChatService.knownWorkloadTopics(List.of("PC chơi game 20 triệu")) & 2).isZero();
    }

    @Test
    void voVaTanMiniItxCoThatKhongBiLoaiNham() {
        // Ca kiểm tra 23/11 (h12, h13 PC Mini-ITX game): hàng thật bị loại nên không dựng được cấu hình nào
        assertThat(ChatService.fitsCode("case_mini_sff", "Vỏ Case LIAN-LI Q58X4 Black")).isTrue();
        assertThat(ChatService.fitsCode("low_profile_47", "Tản nhiệt khí CPU Thermalright AXP90 - X36 cho máy tính iTX | mITX | HTPC")).isTrue();
        assertThat(ChatService.fitsCode("low_profile_47", "Tản nhiệt khí Thermalright AXP120-X67")).isTrue();
        assertThat(ChatService.fitsCode("case_mini_sff", "Vỏ máy tính Mini LIAN LI A4H2O Black Pcie 4.0 (MINI TOWER/MÀU ĐEN/PCIE 4.0)")).isTrue();
        assertThat(ChatService.fitsCode("psu_sfx", "Cooler Master V650 SFX Gold 650W 80 PLUS Gold Modular SFX Power Supply 12V v3.42")).isTrue();
        assertThat(com.smartcarepc.backend.service.price.PriceEngine.kind(com.smartcarepc.backend.service.price.PriceEngine.norm(
                "Cooler Master V650 SFX Gold 650W 80 PLUS Gold Modular SFX Power Supply 12V v3.42"))).isEqualTo("psu");
        assertThat(com.smartcarepc.backend.service.price.PriceEngine.kind(com.smartcarepc.backend.service.price.PriceEngine.norm(
                "Tản nhiệt khí Cooler Master Hyper 212 Black"))).isEqualTo("cooler");
        // Vẫn loại tản tháp và tản nước
        assertThat(ChatService.fitsCode("low_profile_47", "Tản nhiệt khí CPU Deepcool AK400")).isFalse();
        assertThat(ChatService.fitsCode("low_profile_47", "Tản nhiệt nước AIO 240mm ITX")).isFalse();
        assertThat(ChatService.fitsCode("case_mini_sff", "Vỏ máy tính Xigmatek Gaming X mATX")).isFalse();
    }

    @Test
    void nganSachVietHoaCoDauVanNhanDuoc() {
        // Ca thật 23/11: "tôi muốn build PC 20 TRIỆU" bị hỏi lại ngân sách (Ệ hoa không khớp "triệu" khi thiếu UNICODE_CASE)
        assertThat(ChatService.hasBudget("tôi muốn build PC 20 TRIỆU và chơi được game nặng AAA")).isTrue();
        assertThat(ChatService.hasBudget("NGÂN SÁCH 15 Triệu")).isTrue();
        assertThat(ChatService.hasBudget("chơi game AAA")).isFalse();
    }

    @Test
    void slowModelCannotExtendTheWholeRequestDeadline() {
        var client = mock(LlmClient.class);
        var search = mock(ProductSearch.class);
        var components = mock(ComponentService.class);
        var builds = mock(PcBuildService.class);
        when(components.findAll(null)).thenReturn(List.of());
        when(builds.findPresets()).thenReturn(List.of());
        var delayed = new java.util.concurrent.CompletableFuture<Map<String, Object>>();
        when(client.complete(any())).thenAnswer(i -> delayed.join());
        var service = new ChatService(client, search, components, mock(CompatibilityService.class), builds,
                new ModelMatchService(builds, components), JsonMapper.builder().build(), "test", 4096, true);
        try {
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(1), () ->
                    assertThatThrownBy(() -> Io.within(java.time.Duration.ofMillis(60), () -> service.reply(new ChatRequest(
                            List.of(new ChatRequest.Message("user", "RAM là gì?")), null, null, "assembly"))))
                            .isInstanceOf(AiUnavailableException.class));
        } finally {
            delayed.complete(Map.of());
        }
    }

    @Test
    void completeVerifiedOptionsDoNotNeedAnotherModelRound() {
        run(true, 2);
    }

    @Test
    void missingTierKeepsTheNormalModelAndRepairPath() {
        run(false, 3);
    }

    private void run(boolean includeQuality, int expectedModelCalls) {
        var client = mock(LlmClient.class);
        var search = mock(ProductSearch.class);
        var components = mock(ComponentService.class);
        var compatibility = mock(CompatibilityService.class);
        var builds = mock(PcBuildService.class);
        var json = JsonMapper.builder().build();
        when(builds.findPresets()).thenReturn(List.of());
        when(builds.findByCode(any())).thenThrow(new ResourceNotFoundException("build", "test"));
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(search.enabled()).thenReturn(true);
        when(search.pageChecked(any())).thenReturn(true);
        when(search.directLink(any(), any(), any())).thenAnswer(i -> i.getArgument(2));

        String[][] catalog = {
                {"cpu_std", "cpu", "Intel Core i3-12100 BOX", "2400000"},
                {"mb_matx", "motherboard", "MSI PRO H610M-E DDR4", "1600000"},
                {"ram_ddr4", "ram", "Kingston Fury Beast 8GB DDR4 3200", "550000"},
                {"psu_atx", "psu", "Corsair CV550 550W 80 Plus Bronze", "900000"},
                {"case_compact_tower", "case", "Case Xigmatek XA-20 Micro-ATX", "700000"},
                {"ssd_m2_2280", "ssd", "Kingston NV2 256GB M.2 NVMe", "400000"},
                {"ssd_m2_2280", "ssd", "Kingston NV2 512GB M.2 NVMe", "900000"},
                {"ssd_m2_2280", "ssd", "Kingston NV2 1TB M.2 NVMe", "1400000"},
        };
        var parts = new LinkedHashMap<String, ComponentResponse>();
        var products = new ArrayList<ProductSearch.Product>();
        var offers = new LinkedHashMap<String, Offer>();
        for (int n = 0; n < catalog.length; n++) {
            var p = catalog[n];
            parts.putIfAbsent(p[0], new ComponentResponse((long) n, p[0], p[1], p[2], null, null, null, null,
                    Map.of("socket", "LGA1700", "ramType", "DDR4", "ramSlots", 4), List.of()));
            var url = "https://shop.test/product-" + n;
            products.add(new ProductSearch.Product(p[2], "Store", Long.valueOf(p[3]), url, null));
            offers.put(url, new Offer(p[2], "Store", Long.valueOf(p[3]), null, url, null, true, 36, Instant.now()));
        }
        when(components.findAll(null)).thenReturn(new ArrayList<>(parts.values()));
        when(components.findByCode(any())).thenAnswer(i -> parts.get(i.getArgument(0)));
        when(search.search(any())).thenReturn(products);
        when(search.offer(any())).thenAnswer(i -> offers.get(i.getArgument(0)));
        when(search.checkedOffer(any(), any())).thenAnswer(i -> offers.get(i.getArgument(0)));
        when(search.refreshQuote(any(), any())).thenAnswer(i -> offers.get(i.getArgument(0)));

        var proposals = new ArrayList<Map<String, Object>>();
        var tiers = includeQuality ? List.of("cheap", "balanced", "quality") : List.of("cheap", "balanced");
        for (int tier = 0; tier < tiers.size(); tier++) {
            var items = new ArrayList<Map<String, Object>>();
            for (int item = 0; item < 5; item++) items.add(Map.of("code", catalog[item][0], "product_id", "s1-" + (item + 1)));
            items.add(Map.of("code", "ssd_m2_2280", "product_id", "s1-" + (tier + 6)));
            proposals.add(call("p" + tier, "propose_build", json.writeValueAsString(Map.of(
                    "tier", tiers.get(tier), "budget_vnd", 8_000_000, "summary", "Study and office", "items", items))));
        }
        when(client.complete(any())).thenReturn(
                result(List.of(call("s", "search_products", "{\"query\":\"components\"}"))),
                result(proposals),
                Map.of("choices", List.of(Map.of("message", Map.of("role", "assistant", "content", "Here are your options.")))));
        var service = new ChatService(client, search, components, compatibility, builds, new ModelMatchService(builds, components),
                json, "test-model", 4096, true);
        var answer = service.reply(new ChatRequest(List.of(new ChatRequest.Message("user", "Máy học Word Excel, ngân sách 8 triệu")),
                null, null, "advice"));
        assertThat(answer.build()).isNotNull();
        if (includeQuality) assertThat(answer.options()).extracting(o -> o.tier()).containsExactly("cheap", "balanced", "quality");
        assertThat(answer.reply().split("\\s+")).hasSizeLessThan(100);
        assertThat(answer.reply()).doesNotContain("Kingston", "Corsair", "MSI", "khác Phù hợp");
        verify(client, times(expectedModelCalls)).complete(any());
        verify(search).warmCompare(any(), eq(0L));
    }

    private Map<String, Object> result(List<Map<String, Object>> calls) {
        return Map.of("choices", List.of(Map.of("finish_reason", "tool_calls", "message",
                Map.of("role", "assistant", "content", "", "tool_calls", calls))));
    }

    private Map<String, Object> call(String id, String name, String args) {
        return Map.of("id", id, "type", "function", "function", Map.of("name", name, "arguments", args));
    }
}
