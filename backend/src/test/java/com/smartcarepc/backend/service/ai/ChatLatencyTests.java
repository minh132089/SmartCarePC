package com.smartcarepc.backend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
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
import tools.jackson.databind.json.JsonMapper;

class ChatLatencyTests {
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
