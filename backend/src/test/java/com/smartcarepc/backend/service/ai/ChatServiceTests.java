package com.smartcarepc.backend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import com.smartcarepc.backend.exception.AiUnavailableException;
import org.springframework.http.HttpStatus;
import com.smartcarepc.backend.dto.BuildCheckRequest;
import com.smartcarepc.backend.dto.ChatRequest;
import com.smartcarepc.backend.dto.ChatResponse;
import com.smartcarepc.backend.dto.ChatRequest.Message;
import com.smartcarepc.backend.dto.ChatResponse.Line;
import com.smartcarepc.backend.dto.CompatibilityResponse;
import com.smartcarepc.backend.dto.CompatibilityResponse.Issue;
import com.smartcarepc.backend.dto.ComponentResponse;
import com.smartcarepc.backend.dto.PcBuildResponse;
import com.smartcarepc.backend.entity.enums.BuildType;
import com.smartcarepc.backend.service.CompatibilityService;
import com.smartcarepc.backend.service.ComponentService;
import com.smartcarepc.backend.service.ModelMatchService;
import com.smartcarepc.backend.service.PcBuildService;

import tools.jackson.databind.json.JsonMapper;

/** Vòng gọi công cụ của trợ lý, giả lập API AI bằng mock nên không cần mạng hay khoá API. */
class ChatServiceTests {

    private final LlmClient client = mock(LlmClient.class);
    private final ProductSearch search = mock(ProductSearch.class);
    private final ComponentService components = mock(ComponentService.class);
    private final CompatibilityService compatibility = mock(CompatibilityService.class);
    private final PcBuildService builds = mock(PcBuildService.class);
    /** Mockito giữ tham chiếu tới cùng một danh sách messages qua các lượt, nên chụp lại nội dung ngay lúc gọi */
    private final List<Map<String, Object>> sent = new ArrayList<>();
    private ChatService service;

    @BeforeEach
    void setUp() {
        when(builds.findPresets()).thenReturn(List.of());
        service = new ChatService(client, search, components, compatibility, builds, new ModelMatchService(builds, components),
                JsonMapper.builder().build(), "test-model", 512);
    }

    @Test
    void traLoiThangKhiKhongCanCongCu() {
        reply(answer("Chào bạn!"));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "xin chào")), null));

        assertThat(res.reply()).isEqualTo("Chào bạn!");
        assertThat(res.build()).isNull();
        assertThat(sent.getFirst()).containsEntry("model", "test-model").containsEntry("max_tokens", 512);
        assertThat(messages(0).getFirst()).containsEntry("role", "system");
    }

    @Test
    void chayCheckBuildRoiGuiKetQuaVaTraVeTheCauHinh() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(false,
                List.of(new Issue("case.psu", "Nguồn ATX quá to cho vỏ này.")), List.of()));
        when(builds.findByCode("mini_sff")).thenReturn(new PcBuildResponse(4L, "mini_sff", "Mini / SFF", "Hộp nhôm rất nhỏ.",
                BuildType.PRESET, null, List.of()));
        when(components.findByCode("case_mini_sff")).thenReturn(part("case_mini_sff", "case", "Vỏ Mini / SFF"));
        when(components.findByCode("psu_atx")).thenReturn(part("psu_atx", "psu", "Nguồn ATX"));
        reply(toolCall("t1", "check_build",
                        "{\"items\":[{\"code\":\"case_mini_sff\"},{\"code\":\"psu_atx\",\"quantity\":1}]}"),
                answer("<think>nguồn to quá</think>Nguồn này không vừa vỏ."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "lắp nguồn ATX vào vỏ mini được không?")), "mini_sff"));

        assertThat(res.reply()).isEqualTo("Nguồn này không vừa vỏ.");
        assertThat((String) messages(0).getFirst().get("content")).contains("đang xem máy \"Mini / SFF\"");
        var req = ArgumentCaptor.forClass(BuildCheckRequest.class);
        verify(compatibility).check(req.capture());
        assertThat(req.getValue().items()).extracting(BuildCheckRequest.Item::code).containsExactly("case_mini_sff", "psu_atx");

        // Lượt 2 gửi lại tin gọi công cụ của model và kết quả của nó
        var second = messages(1);
        assertThat(second).hasSize(4);
        assertThat(second.get(2)).containsKey("tool_calls");
        assertThat(second.get(3)).containsEntry("role", "tool").containsEntry("tool_call_id", "t1");
        assertThat((String) second.get(3).get("content")).contains("Nguồn ATX quá to");

        // Thẻ cấu hình cho khung chat
        assertThat(res.build().compatible()).isFalse();
        assertThat(res.build().items()).extracting(Line::name).containsExactly("Vỏ Mini / SFF", "Nguồn ATX");
        assertThat(res.build().errors()).extracting(Issue::rule).containsExactly("case.psu");
    }

    @Test
    void proposeBuildLayThongTinSanPhamTheoIdKetQuaTimKiem() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("case_compact_tower")).thenReturn(part("case_compact_tower", "case", "Vỏ Tower nhỏ gọn"));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM DDR4"));
        when(search.enabled()).thenReturn(true);
        when(search.search("RAM DDR4 16GB")).thenReturn(List.of(new ProductSearch.Product(
                "RAM 16GB (2x8GB) DDR4 3200", "Shop A", 1_050_000L, "https://www.google.com/search?ibp=oshop&q=ram", "https://img/ram.jpg")));
        when(search.directLink(any(), any(), any())).thenReturn("https://shop-a.vn/ram-16gb");
        reply(toolCall("s1", "search_products", "{\"query\":\"RAM DDR4 16GB\"}"),
                toolCall("p1", "propose_build", """
                {"summary":"Học online, máy nhỏ gọn","budget_vnd":12000000,"items":[
                  {"code":"case_compact_tower","spec":"Vỏ mATX nhỏ","reason":"Vừa bàn học","price_min_vnd":700000,"price_max_vnd":1000000,"product_id":"s9-9"},
                  {"code":"ram_ddr4","quantity":2,"spec":"16 GB, 2 thanh 8 GB","reason":"Mở nhiều tab vẫn mượt","product_id":"s1-1","buy_qty":1}]}
                """), answer("Tổng khoảng 1,8 - 2,1 triệu."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học online 12 triệu")), null, null, "advice"));

        var card = res.build();
        assertThat(card.summary()).isEqualTo("Học online, máy nhỏ gọn");
        var ram = card.items().get(1);
        assertThat(ram.quantity()).isEqualTo(2);
        assertThat(ram.productName()).isEqualTo("RAM 16GB (2x8GB) DDR4 3200");
        assertThat(ram.shop()).isEqualTo("Shop A");
        assertThat(ram.priceMinVnd()).as("bộ kit: mua 1 sản phẩm").isEqualTo(1_050_000L);
        assertThat(ram.sourceUrl()).as("đã đổi sang link cửa hàng").isEqualTo("https://shop-a.vn/ram-16gb");
        assertThat(ram.imageUrl()).isEqualTo("https://img/ram.jpg");
        var vo = card.items().getFirst();
        assertThat(vo.productName()).as("id không có trong kết quả tìm kiếm").isNull();
        assertThat(vo.sourceUrl()).isNull();
        assertThat(vo.priceMinVnd()).as("không có sản phẩm thì dùng giá ước tính").isEqualTo(700_000L);
        // Trợ lý chỉ thấy id, tên, cửa hàng, giá; không thấy link dài
        String toolResult = (String) messages(1).get(3).get("content");
        assertThat(toolResult).contains("s1-1").contains("1050000").doesNotContain("google.com");
        // Tổng do backend cộng: 700k-1tr ước tính + 1,05tr giá cửa hàng
        assertThat((String) messages(2).get(5).get("content")).contains("\"total_min_vnd\":1750000").contains("\"total_max_vnd\":2050000");
    }

    @Test
    void monChuaCoSanPhamThiAppTuTimVaGan() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("low_profile_47")).thenReturn(part("low_profile_47", "cooler", "Tản khí thấp 47 mm"));
        when(components.findByCode("tool_kit")).thenReturn(part("tool_kit", "tools", "Bộ đồ nghề"));
        when(search.enabled()).thenReturn(true);
        // Tìm theo thông số ra 3 món: phụ kiện rẻ bất thường, món hợp khoảng giá, món đắt; chọn món rẻ nhất trong khoảng
        when(search.search("Tản khí thấp 47 mm, socket AM4")).thenReturn(List.of(
                new ProductSearch.Product("Keo tản nhiệt", "Shop X", 40_000L, "https://x/keo", null),
                new ProductSearch.Product("Tản ID-Cooling IS-40X", "Shop B", 450_000L, "https://b/is40", "https://img/is40.jpg"),
                new ProductSearch.Product("Tản Noctua L9a", "Shop C", 1_300_000L, "https://c/l9a", null)));
        // Thông số không ra gì thì tìm theo tên loại linh kiện
        when(search.search("Bộ đồ nghề")).thenReturn(List.of(new ProductSearch.Product("Bộ tua vít 24 in 1", "Shop D", 150_000L, "https://d/tuavit", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("p1", "propose_build", """
                {"summary":"Văn phòng nhỏ","items":[
                  {"code":"low_profile_47","spec":"Tản khí thấp 47 mm, socket AM4","reason":"Vừa vỏ Mini","price_min_vnd":350000,"price_max_vnd":600000},
                  {"code":"tool_kit","spec":"Tua vít đầu từ","reason":"Để tự lắp"}]}
                """), answer("Xong."));

        var card = service.reply(new ChatRequest(List.of(new Message("user", "máy văn phòng nhỏ")), null, null, "advice")).build();

        assertThat(card.items().get(0).productName()).isEqualTo("Tản ID-Cooling IS-40X");
        assertThat(card.items().get(0).sourceUrl()).isEqualTo("https://b/is40");
        assertThat(card.items().get(0).priceMinVnd()).isEqualTo(450_000L);
        assertThat(card.items().get(1).productName()).isEqualTo("Bộ tua vít 24 in 1");
        // Trợ lý được báo món nào app đã tự gắn, để không nói "chưa tìm được hàng"
        String toolResult = (String) messages(1).get(3).get("content");
        assertThat(toolResult).contains("auto_filled").contains("Tản ID-Cooling IS-40X").contains("\"total_min_vnd\":600000");
    }

    @Test
    void baPhuongAnTraVeOptionsVaLayPhuHopLamTheChinh() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM DDR4"));
        String item = "{\"code\":\"ram_ddr4\",\"spec\":\"16 GB\",\"reason\":\"Đủ dùng\",\"price_min_vnd\":%d,\"price_max_vnd\":%d}";
        var calls = new ArrayList<Map<String, Object>>();
        for (var t : List.of(new Object[] { "cheap", 800_000 }, new Object[] { "balanced", 1_000_000 }, new Object[] { "quality", 1_500_000 })) {
            calls.add(Map.of("id", "p-" + t[0], "type", "function", "function", Map.of("name", "propose_build",
                    "arguments", "{\"tier\":\"" + t[0] + "\",\"summary\":\"Học\",\"items\":[" + item.formatted(t[1], t[1]) + "]}")));
        }
        reply(Map.of("choices", List.of(Map.of("finish_reason", "tool_calls",
                "message", Map.of("role", "assistant", "content", "", "tool_calls", calls)))), answer("Ba phương án."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học 12 triệu")), null, null, "advice"));

        assertThat(res.options()).extracting(o -> o.tier()).containsExactly("cheap", "balanced", "quality");
        assertThat(res.options()).extracting(o -> o.items().getFirst().priceMinVnd()).containsExactly(800_000L, 1_000_000L, 1_500_000L);
        assertThat(res.build().tier()).isEqualTo("balanced");
    }

    @Test
    void motPhuongAnThiKhongCoOptions() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM DDR4"));
        reply(toolCall("p1", "propose_build", """
                {"summary":"Học","items":[{"code":"ram_ddr4","spec":"16 GB","reason":"Đủ dùng"}]}
                """), answer("Một phương án."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học")), null, null, "advice"));

        assertThat(res.options()).isNull();
        assertThat(res.build().tier()).isEqualTo("balanced");
    }

    @Test
    void build2Va3DoiSangSanPhamCungNhomGiaGan() {
        var chosen = new ProductSearch.Product("RAM Kingston 16GB (2x8GB) 3200", "Phong Vũ", 1_000_000L, "https://phongvu.vn/a", null);
        var group = List.of(chosen,
                new ProductSearch.Product("RAM Corsair 16GB (2x8GB) 3200", "Hacom", 1_050_000L, "https://hacom.vn/b", null),
                new ProductSearch.Product("RAM Gskill 16GB (2x8GB) 3200", "GEARVN", 900_000L, "https://gearvn.com/c", null),
                new ProductSearch.Product("RAM Kingston 8GB 3200", "X", 980_000L, "https://x.vn/d", null),
                new ProductSearch.Product("RAM 16GB 2x8GB like new", "Y", 1_000_000L, "https://y.vn/e", null),
                new ProductSearch.Product("RAM TeamGroup 16GB (2x8GB)", "Z", 2_000_000L, "https://z.vn/f", null));
        // Bộ 2 thanh: chỉ đổi sang bộ khác; bỏ hàng like new và món đắt gấp đôi; gần giá nhất trước
        assertThat(ChatService.alternatives(chosen, group, true, "quality")).extracting(ProductSearch.Product::title)
                .containsExactly("RAM Corsair 16GB (2x8GB) 3200", "RAM Gskill 16GB (2x8GB) 3200");
        // Không phải bộ: lấy cả thanh lẻ, nhưng cửa hàng biết tên miền (Hacom, GEARVN) đứng trước shop lạ dù giá gần hơn
        assertThat(ChatService.alternatives(chosen, group, false, "quality")).extracting(ProductSearch.Product::title)
                .containsExactly("RAM Corsair 16GB (2x8GB) 3200", "RAM Gskill 16GB (2x8GB) 3200");
        // Phương án Rẻ: không lấy món đắt hơn 15%; Chất lượng: không lấy món rẻ hơn 15%
        assertThat(ChatService.alternatives(chosen, group, true, "cheap")).extracting(ProductSearch.Product::title)
                .containsExactly("RAM Corsair 16GB (2x8GB) 3200", "RAM Gskill 16GB (2x8GB) 3200");
        assertThat(ChatService.candidates(chosen, group, false, "quality")).extracting(ProductSearch.Product::title)
                .containsExactly("RAM Corsair 16GB (2x8GB) 3200", "RAM Gskill 16GB (2x8GB) 3200", "RAM Kingston 8GB 3200");
        var cheapOnly = List.of(chosen, new ProductSearch.Product("RAM X 16GB (2x8GB)", "S", 800_000L, "https://s.vn/x", null));
        assertThat(ChatService.alternatives(chosen, cheapOnly, true, "quality")).isEmpty();
    }

    @Test
    void moiTierCoBaBuildDeThamKhao() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(search.enabled()).thenReturn(true);
        when(search.search("SSD 1TB")).thenReturn(List.of(
                new ProductSearch.Product("SSD A 1TB", "Shop A", 1_500_000L, "https://a.vn/1", null),
                new ProductSearch.Product("SSD B 1TB", "Shop B", 1_550_000L, "https://b.vn/1", null),
                new ProductSearch.Product("SSD C 1TB", "Shop C", 1_400_000L, "https://c.vn/1", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        String item = "{\"code\":\"ssd_m2_2280\",\"spec\":\"1 TB\",\"reason\":\"Đủ chỗ\",\"product_id\":\"s1-1\"}";
        var calls = new ArrayList<Map<String, Object>>();
        for (String t : List.of("cheap", "balanced", "quality")) {
            calls.add(Map.of("id", "p-" + t, "type", "function", "function", Map.of("name", "propose_build",
                    "arguments", "{\"tier\":\"" + t + "\",\"summary\":\"Học\",\"items\":[" + item + "]}")));
        }
        reply(toolCall("s", "search_products", "{\"query\":\"SSD 1TB\"}"),
                Map.of("choices", List.of(Map.of("finish_reason", "tool_calls", "message", Map.of("role", "assistant", "content", "", "tool_calls", calls)))),
                answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học 12 triệu")), null, null, "advice"));

        assertThat(res.options()).hasSize(9);
        var balanced = res.options().stream().filter(o -> "balanced".equals(o.tier())).toList();
        assertThat(balanced).extracting(o -> o.variant()).containsExactly(1, 2, 3);
        assertThat(balanced).extracting(o -> o.items().getFirst().productName()).containsExactly("SSD A 1TB", "SSD B 1TB", "SSD C 1TB");
        assertThat(res.build().variant()).isEqualTo(1);
        assertThat(res.build().tier()).isEqualTo("balanced");
    }

    @Test
    void monKhongCoLinkCuaHangThiDoiSangMonCungNhomCoLink() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        when(search.search("RTX 3060")).thenReturn(List.of(
                new ProductSearch.Product("RTX 3060 Shop Nho", "Tân Thành", 7_000_000L, "https://www.google.com/x", null),
                new ProductSearch.Product("RTX 3060 Phong Vu", "Phong Vũ", 7_200_000L, "https://www.google.com/y", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> "Phong Vũ".equals(inv.getArgument(1)) ? "https://phongvu.vn/rtx3060" : null);
        reply(toolCall("s", "search_products", "{\"query\":\"RTX 3060\"}"),
                toolCall("p1", "propose_build", """
                {"summary":"Game","items":[{"code":"gpu_280","spec":"RTX 3060","reason":"Chơi game","product_id":"s1-1"}]}
                """), answer("Xong."));

        var line = service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice")).build().items().getFirst();

        assertThat(line.productName()).isEqualTo("RTX 3060 Phong Vu");
        assertThat(line.sourceUrl()).isEqualTo("https://phongvu.vn/rtx3060");
        assertThat(line.priceMinVnd()).isEqualTo(7_200_000L);
    }

    @Test
    void linkChiLaTrangChuThiDoiSangMonCoTrangSanPham() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        when(search.search("RTX 3060")).thenReturn(List.of(
                new ProductSearch.Product("RTX 3060 Shop Nho", "hotgear.vn", 7_000_000L, "https://www.google.com/x", null),
                new ProductSearch.Product("RTX 3060 Phong Vu", "Phong Vũ", 7_200_000L, "https://www.google.com/y", null)));
        // Shop nhỏ tìm không ra trang sản phẩm nên chỉ có trang chủ
        when(search.directLink(any(), any(), any())).thenAnswer(inv ->
                "Phong Vũ".equals(inv.getArgument(1)) ? "https://phongvu.vn/rtx3060" : "https://hotgear.vn/");
        reply(toolCall("s", "search_products", "{\"query\":\"RTX 3060\"}"),
                toolCall("p1", "propose_build", """
                {"summary":"Game","items":[{"code":"gpu_280","spec":"RTX 3060","reason":"Chơi game","product_id":"s1-1"}]}
                """), answer("Xong."));

        var line = service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice")).build().items().getFirst();

        assertThat(line.productName()).isEqualTo("RTX 3060 Phong Vu");
        assertThat(line.sourceUrl()).isEqualTo("https://phongvu.vn/rtx3060");
    }

    @Test
    void phuongAnReKhongBaoGioVuotNganSach() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(search.enabled()).thenReturn(true);
        // Ngân sách 1 triệu nhưng trợ lý chọn món 1,1 triệu cho phương án Rẻ (và không sửa dù được báo)
        when(search.search("SSD")).thenReturn(List.of(
                new ProductSearch.Product("SSD A", "A", 1_100_000L, "https://a.vn/1", null),
                new ProductSearch.Product("SSD B", "B", 950_000L, "https://b.vn/1", null),
                new ProductSearch.Product("SSD C", "C", 900_000L, "https://c.vn/1", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"SSD\"}"),
                toolCall("p1", "propose_build", """
                {"tier":"cheap","summary":"Học","budget_vnd":1000000,"items":[{"code":"ssd_m2_2280","spec":"SSD","reason":"Đủ","product_id":"s1-1"}]}
                """), toolCall("p2", "propose_build", """
                {"tier":"balanced","summary":"Học","budget_vnd":1000000,"items":[{"code":"ssd_m2_2280","spec":"SSD","reason":"Đủ","product_id":"s1-2"}]}
                """), answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học")), null, null, "advice"));

        // Build vượt ngân sách bị bỏ, option đánh số lại từ 1
        var cheap = res.options().stream().filter(o -> "cheap".equals(o.tier())).toList();
        // Rẻ tối đa 90% ngân sách (900 nghìn): chỉ SSD C còn hợp lệ
        assertThat(cheap).extracting(o -> o.items().getFirst().productName()).containsExactly("SSD C");
        assertThat(cheap).extracting(ChatResponse.BuildCard::variant).containsExactly(1);
        assertThat(cheap).allSatisfy(o -> assertThat(o.items().getFirst().priceMaxVnd()).isLessThanOrEqualTo(900_000L));
        // Phù hợp: option 1 không vượt ngân sách, option sau được đắt hơn tối đa 2 triệu
        var balanced = res.options().stream().filter(o -> "balanced".equals(o.tier())).toList();
        assertThat(balanced.getFirst().items().getFirst().priceMaxVnd()).isLessThanOrEqualTo(1_000_000L);
        assertThat(balanced).allSatisfy(o -> assertThat(o.items().getFirst().priceMaxVnd()).isLessThanOrEqualTo(3_000_000L));
    }

    @Test
    void doiMonLayLinkKhongDayPhuongAnQuaNganSach() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(search.enabled()).thenReturn(true);
        // Món trợ lý chọn không có link; món giá gần nhất (B) làm vượt ngân sách nên phải lấy món rẻ hơn (C)
        when(search.search("SSD")).thenReturn(List.of(
                new ProductSearch.Product("SSD A", "A", 950_000L, "https://www.google.com/a", null),
                new ProductSearch.Product("SSD B", "B", 1_020_000L, "https://b.vn/1", null),
                new ProductSearch.Product("SSD C", "C", 870_000L, "https://c.vn/1", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> "SSD A".equals(inv.getArgument(0)) ? null : inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"SSD\"}"),
                toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Học","budget_vnd":1000000,"items":[{"code":"ssd_m2_2280","spec":"SSD","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học")), null, null, "advice"));

        assertThat(res.build().items().getFirst().productName()).isEqualTo("SSD C");
        assertThat(res.build().items().getFirst().sourceUrl()).isEqualTo("https://c.vn/1");
    }

    @Test
    void aiLoiOVongCuoiVanGuiPhuongAnDaDung() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(search.enabled()).thenReturn(true);
        when(search.search("SSD")).thenReturn(List.of(new ProductSearch.Product("SSD A", "A", 1_000_000L, "https://a.vn/1", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        when(client.complete(any())).thenReturn(toolCall("s", "search_products", "{\"query\":\"SSD\"}"),
                toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Học","items":[{"code":"ssd_m2_2280","spec":"SSD","reason":"Đủ","product_id":"s1-1"}]}
                """)).thenThrow(new AiUnavailableException(HttpStatus.BAD_GATEWAY, "AI lỗi"));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học")), null, null, "advice"));

        assertThat(res.reply()).isEqualTo(ChatService.BUILDS_READY);
        assertThat(res.build().items().getFirst().productName()).isEqualTo("SSD A");
    }

    @Test
    void aiLoiNgayVongDauThiBaoLoi() {
        when(client.complete(any())).thenThrow(new AiUnavailableException(HttpStatus.BAD_GATEWAY, "AI lỗi"));
        assertThatThrownBy(() -> service.reply(new ChatRequest(List.of(new Message("user", "học")), null, null, "advice")))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void serperHetLuotThiTamTatTimKiem() {
        assertThat(ProductSearch.accountProblem(400, "{\"message\":\"Not enough credits\",\"statusCode\":400}")).isTrue();
        assertThat(ProductSearch.accountProblem(403, "")).isTrue();
        assertThat(ProductSearch.accountProblem(400, "{\"message\":\"bad query\"}")).isFalse();
    }

    @Test
    void boDdr5MaRamDdr4ThiBaoLoiVaBatTroLyChonLai() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("mb_atx")).thenReturn(part("mb_atx", "motherboard", "Bo mạch chủ ATX"));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM DDR4"));
        when(search.enabled()).thenReturn(true);
        when(search.search("B760")).thenReturn(List.of(
                new ProductSearch.Product("Mainboard MSI B760 GAMING PLUS WIFI DDR5", "Hacom", 4_300_000L, "https://hacom.vn/b760", null)));
        when(search.search("RAM DDR4")).thenReturn(List.of(
                new ProductSearch.Product("RAM Corsair Vengeance LPX 16GB DDR4 3200", "FPT", 1_600_000L, "https://fptshop.com.vn/ram", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s1", "search_products", "{\"query\":\"B760\"}"),
                toolCall("s2", "search_products", "{\"query\":\"RAM DDR4\"}"),
                toolCall("p1", "propose_build", """
                {"summary":"Game","items":[{"code":"mb_atx","spec":"B760","reason":"Bền","product_id":"s1-1"},
                {"code":"ram_ddr4","spec":"16 GB DDR4","reason":"Đủ","product_id":"s2-1"}]}
                """), answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice"));

        // Thẻ báo không lắp được dù danh mục (đều DDR4) lắp được
        assertThat(res.build().compatible()).isFalse();
        assertThat(res.build().errors()).anySatisfy(i -> assertThat(i.message()).contains("DDR5").contains("DDR4"));
        // Trợ lý được báo lỗi để chọn lại, và thấy thông số khi tìm
        String toolResults = sent.getLast().get("messages").toString();
        assertThat(toolResults).contains("product_errors").contains("chipset B760, DDR5");
    }

    @Test
    void giaTrenTheLaGiaDocTuTrangCuaHang() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        when(search.search("SSD")).thenReturn(List.of(new ProductSearch.Product("SSD Kingston NV2 1TB", "A", 1_400_000L, "https://a.vn/ssd", null)));
        when(search.search("Card")).thenReturn(List.of(new ProductSearch.Product("Card RTX 4060", "B", 8_000_000L, "https://b.vn/gpu", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        var at = java.time.Instant.parse("2026-10-20T10:00:00Z");
        // Trang SSD bán giá khác Google Shopping: thẻ dùng giá trang
        when(search.offer("https://a.vn/ssd")).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                "SSD Kingston NV2 1TB", "A", 1_550_000L, null, "https://a.vn/ssd", null, true, 36, at));
        // Trang card ghi giá sai đơn vị (x100): bỏ, giữ giá cũ và ghi là chưa kiểm tra
        when(search.offer("https://b.vn/gpu")).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                "Card RTX 4060", "B", 800_000_000L, null, "https://b.vn/gpu", null, null, null, at));
        reply(toolCall("s1", "search_products", "{\"query\":\"SSD\"}"), toolCall("s2", "search_products", "{\"query\":\"Card\"}"),
                toolCall("p1", "propose_build", """
                {"summary":"Game","items":[{"code":"ssd_m2_2280","spec":"SSD 1TB","reason":"Nhanh","product_id":"s1-1"},
                {"code":"gpu_280","spec":"RTX 4060","reason":"Chơi game","product_id":"s2-1"}]}
                """), answer("Xong."));

        var items = service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice")).build().items();

        var ssd = items.stream().filter(i -> i.category().equals("ssd")).findFirst().orElseThrow();
        assertThat(ssd.priceMinVnd()).isEqualTo(1_550_000L);
        assertThat(ssd.priceChecked()).isTrue();
        var gpu = items.stream().filter(i -> i.category().equals("gpu")).findFirst().orElseThrow();
        assertThat(gpu.priceMinVnd()).isEqualTo(8_000_000L);
    }

    @Test
    void chiChotMonConHangVaTuTimThemKhiNhomKhongCoLink() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(components.findByCode("case_gaming")).thenReturn(part("case_gaming", "case", "Vỏ"));
        when(search.enabled()).thenReturn(true);
        var at = java.time.Instant.parse("2026-10-20T10:00:00Z");
        // SSD: món trợ lý chọn đã hết hàng, món cùng nhóm còn hàng -> đổi
        when(search.search("SSD")).thenReturn(List.of(
                new ProductSearch.Product("SSD A 1TB", "A", 1_500_000L, "https://a.vn/ssd-a", null),
                new ProductSearch.Product("SSD B 1TB", "B", 1_520_000L, "https://b.vn/ssd-b", null)));
        when(search.offer("https://a.vn/ssd-a")).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                "SSD A 1TB", "A", 1_500_000L, null, "https://a.vn/ssd-a", null, false, null, at));
        when(search.offer("https://b.vn/ssd-b")).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                "SSD B 1TB", "B", 1_520_000L, null, "https://b.vn/ssd-b", null, true, null, at));
        // Vỏ: cả nhóm không có trang bán -> tìm thêm theo thông số và lấy món có link
        when(search.search("Vỏ kính")).thenReturn(List.of(new ProductSearch.Product("Vỏ Jonsbo D32", "X", 1_300_000L, "https://www.google.com/x", null)));
        when(search.search("Vỏ mid tower mặt kính")).thenReturn(List.of(
                new ProductSearch.Product("Vỏ Montech Air 100", "C", 1_350_000L, "https://c.vn/vo", null)));
        when(search.offer("https://c.vn/vo")).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                "Vỏ Montech Air 100", "C", 1_350_000L, null, "https://c.vn/vo", null, true, null, at));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> {
            String url = inv.getArgument(2);
            return url.contains("google") ? null : url;
        });
        reply(toolCall("s1", "search_products", "{\"query\":\"SSD\"}"), toolCall("s2", "search_products", "{\"query\":\"Vỏ kính\"}"),
                toolCall("p1", "propose_build", """
                {"summary":"Game","items":[{"code":"ssd_m2_2280","spec":"SSD 1TB","reason":"Nhanh","product_id":"s1-1"},
                {"code":"case_gaming","spec":"Vỏ mid tower mặt kính","reason":"Đẹp","product_id":"s2-1"}]}
                """), answer("Xong."));

        var items = service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice")).build().items();

        var ssd = items.stream().filter(i -> i.category().equals("ssd")).findFirst().orElseThrow();
        assertThat(ssd.productName()).isEqualTo("SSD B 1TB");
        assertThat(ssd.inStock()).isTrue();
        var pcCase = items.stream().filter(i -> i.category().equals("case")).findFirst().orElseThrow();
        assertThat(pcCase.productName()).isEqualTo("Vỏ Montech Air 100");
        assertThat(pcCase.sourceUrl()).isEqualTo("https://c.vn/vo");
    }

    @Test
    void tierReVaChatLuongLayThongSoLyDoTuPhuHopKhiBoTrong() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        String balanced = "{\"code\":\"ssd_m2_2280\",\"spec\":\"1 TB NVMe\",\"reason\":\"Mở game nhanh\"}";
        String cheap = "{\"code\":\"ssd_m2_2280\"}";
        var calls = new ArrayList<Map<String, Object>>();
        calls.add(Map.of("id", "p-c", "type", "function", "function", Map.of("name", "propose_build",
                "arguments", "{\"tier\":\"cheap\",\"summary\":\"Game\",\"items\":[" + cheap + "]}")));
        calls.add(Map.of("id", "p-b", "type", "function", "function", Map.of("name", "propose_build",
                "arguments", "{\"tier\":\"balanced\",\"summary\":\"Game\",\"items\":[" + balanced + "]}")));
        reply(Map.of("choices", List.of(Map.of("finish_reason", "tool_calls", "message", Map.of("role", "assistant", "content", "", "tool_calls", calls)))),
                answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice"));

        var cheapLine = res.options().stream().filter(o -> "cheap".equals(o.tier())).findFirst().orElseThrow().items().getFirst();
        assertThat(cheapLine.spec()).isEqualTo("1 TB NVMe");
        assertThat(cheapLine.reason()).isEqualTo("Mở game nhanh");
    }

    @Test
    void sauHaiVongTimThiPhaiChot() {
        when(search.enabled()).thenReturn(true);
        when(search.search(any())).thenReturn(List.of());
        reply(toolCall("s1", "search_products", "{\"query\":\"SSD\"}"), toolCall("s2", "search_products", "{\"query\":\"RAM\"}"),
                toolCall("s3", "search_products", "{\"query\":\"Card\"}"), answer("Xong."));

        service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice"));

        // Lần tìm thứ 3 (trợ lý vẫn gọi dù công cụ đã bị bỏ) không chạy
        org.mockito.Mockito.verify(search, org.mockito.Mockito.never()).search("Card");
        assertThat(sent.get(3).get("messages").toString()).contains("Đã hết lượt tìm");
        // Vòng 3 (sau 2 vòng tìm) không còn công cụ search_products
        assertThat(sent.get(1).get("tools").toString()).contains("name=search_products");
        assertThat(sent.get(2).get("tools").toString()).doesNotContain("name=search_products");
    }

    @Test
    void tranGiaTheoPhuongAn() {
        // Rẻ: tối đa 90% ngân sách; Phù hợp: option 1 đúng ngân sách, option 2 / 3 vượt tối đa 2 triệu; Chất lượng: + 3 triệu
        assertThat(ChatService.tierCap(25_000_000L, "cheap")).isEqualTo(22_500_000L);
        assertThat(ChatService.altCap(25_000_000L, "cheap")).isEqualTo(22_500_000L);
        assertThat(ChatService.tierCap(25_000_000L, "balanced")).isEqualTo(25_000_000L);
        assertThat(ChatService.altCap(25_000_000L, "balanced")).isEqualTo(27_000_000L);
        assertThat(ChatService.tierCap(25_000_000L, "quality")).isEqualTo(28_000_000L);
        assertThat(ChatService.altCap(25_000_000L, "quality")).isEqualTo(28_000_000L);
        assertThat(ChatService.tierCap(null, "quality")).isNull();
    }

    @Test
    void tenNganVaNhanBietTrangChu() {
        assertThat(ProductSearch.shortTitle("CPU Intel Core i5-12400F (Up to 4.4GHz, 6 nhân 12 luồng) - Chính hãng"))
                .isEqualTo("CPU Intel Core i5-12400F");
        assertThat(ProductSearch.shortTitle("Card màn hình MSI GeForce RTX 4060 Ventus 2X Black 8G OC"))
                .isEqualTo("Card màn hình MSI GeForce RTX 4060 Ventus");
        assertThat(ProductSearch.shortTitle("(Hot) RAM")).isEqualTo("(Hot) RAM");
        assertThat(ProductSearch.homePage("https://hotgear.vn/")).isTrue();
        assertThat(ProductSearch.homePage("https://hotgear.vn")).isTrue();
        assertThat(ProductSearch.homePage("https://phongvu.vn/rtx3060")).isFalse();
    }

    @Test
    void buildThamKhaoCuaPhuongAnPhuHopKhongVuotNganSach() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(search.enabled()).thenReturn(true);
        // Ngân sách 1 triệu, trợ lý chọn món 1 triệu: món 1,2 triệu (gần giá nhất) làm vượt ngân sách nên phải dùng món rẻ hơn
        when(search.search("SSD")).thenReturn(List.of(
                new ProductSearch.Product("SSD A", "A", 1_000_000L, "https://a.vn/1", null),
                new ProductSearch.Product("SSD Dat", "B", 1_200_000L, "https://b.vn/1", null),
                new ProductSearch.Product("SSD Re", "C", 850_000L, "https://c.vn/1", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"SSD\"}"),
                toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Học","budget_vnd":1000000,"items":[{"code":"ssd_m2_2280","spec":"SSD","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học")), null, null, "advice"));

        // Option 1 do trợ lý chọn, không vượt ngân sách; option 2, 3 của Phù hợp được đắt hơn ngân sách tối đa 2 triệu
        assertThat(res.options()).extracting(o -> o.items().getFirst().productName()).containsExactly("SSD A", "SSD Re", "SSD Dat");
        assertThat(res.build().items().getFirst().productName()).isEqualTo("SSD A");
    }

    @Test
    void nhanTenCuaHangRaTenMien() {
        assertThat(ProductSearch.domainOf("Phong Vũ")).isEqualTo("phongvu.vn");
        assertThat(ProductSearch.domainOf("Nguyễn Vũ Store")).isEqualTo("nguyenvu.store");
        assertThat(ProductSearch.domainOf("Fptshop.com.vn")).isEqualTo("fptshop.com.vn");
        assertThat(ProductSearch.domainOf("GEARVN")).isEqualTo("gearvn.com");
        assertThat(ProductSearch.domainOf("Cửa hàng lạ")).isNull();
        assertThat(ProductSearch.isGoogle("https://www.google.com/search?ibp=oshop&q=ram")).isTrue();
        assertThat(ProductSearch.isGoogle("https://phongvu.vn/ram-16gb")).isFalse();
    }

    @Test
    void chonSanPhamTheoKhoangGia() {
        var junk = new ProductSearch.Product("Phụ kiện", "S", 30_000L, "https://a", null);
        var mid = new ProductSearch.Product("Hợp", "S", 500_000L, "https://b", null);
        var noPrice = new ProductSearch.Product("Không giá", "S", null, "https://c", null);
        assertThat(ChatService.choose(List.of(junk, mid), 400_000L, 600_000L)).isEqualTo(mid);
        assertThat(ChatService.choose(List.of(noPrice, junk), null, null)).as("không có ước tính: món có giá đầu tiên").isEqualTo(junk);
        assertThat(ChatService.choose(List.of(noPrice), 400_000L, null)).as("không món nào có giá").isEqualTo(noPrice);
        assertThat(ChatService.choose(List.of(), 1L, 2L)).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void chuaCoKhoaTimKiemThiKhongDuaCongCuTimKiem() {
        reply(answer("Ok."));

        service.reply(new ChatRequest(List.of(new Message("user", "?")), null, null, "advice"));

        var tools = (List<Map<String, Object>>) sent.getFirst().get("tools");
        assertThat(tools).extracting(t -> ((Map<String, Object>) t.get("function")).get("name")).doesNotContain("search_products");
        assertThat((String) messages(0).getFirst().get("content")).contains("tìm kiếm sản phẩm đang tắt");
    }

    @Test
    void modelVanGoiTimKiemKhiTatThiNhanLoiNhac() {
        reply(toolCall("s1", "search_products", "{\"query\":\"SSD\"}"), answer("Ok."));

        service.reply(new ChatRequest(List.of(new Message("user", "?")), null, null, "advice"));

        assertThat((String) messages(1).get(3).get("content")).contains("Chưa bật tìm kiếm");
    }

    @Test
    void docGiaTienDong() {
        assertThat(ProductSearch.vnd("1.290.000 ₫")).isEqualTo(1_290_000L);
        assertThat(ProductSearch.vnd("₫1,290,000")).isEqualTo(1_290_000L);
        assertThat(ProductSearch.vnd("1290000.00")).isEqualTo(1_290_000L);
        assertThat(ProductSearch.vnd("$129")).as("không phải tiền đồng").isNull();
    }

    @Test
    void congCuLoiThiBaoLaiChoModelThayViNemLoi() {
        reply(toolCall("t1", "khong_ton_tai", "{}"), answer("Ok."));

        service.reply(new ChatRequest(List.of(new Message("user", "?")), null));

        assertThat((String) messages(1).get(3).get("content")).startsWith("Lỗi:");
    }

    @Test
    void thamSoKhongPhaiJsonCungKhongLamHongCuocChat() {
        reply(toolCall("t1", "check_build", "{items: hỏng"), answer("Ok."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "?")), null));

        assertThat(res.reply()).isEqualTo("Ok.");
        assertThat((String) messages(1).get(3).get("content")).startsWith("Lỗi:");
    }

    @Test
    void ghiBuocDangLapVaoSystemPrompt() {
        reply(answer("Ok."));

        service.reply(new ChatRequest(List.of(new Message("user", "CPU không nằm phẳng")), null, "Bước 3/10: Đặt CPU vào socket"));

        assertThat((String) messages(0).getFirst().get("content")).contains("hiện ở: Bước 3/10: Đặt CPU vào socket");
    }

    @Test
    void trangTuVanDungLuatTuVan() {
        reply(answer("Ok."));

        service.reply(new ChatRequest(List.of(new Message("user", "mình học online, có 12 triệu")), null, null, "advice"));

        String system = (String) messages(0).getFirst().get("content");
        assertThat(system).contains("trợ lý tư vấn build PC").doesNotContain("Gỡ rối khi lắp");
    }

    @Test
    void boLoiChaoMoDauCuaTroLy() {
        reply(answer("Ok."));

        service.reply(new ChatRequest(List.of(new Message("assistant", "Chào bạn, mình giúp gì được?"),
                new Message("user", "máy nóng quá")), null));

        assertThat(messages(0)).hasSize(2);
        assertThat(messages(0).get(1)).containsEntry("role", "user");
    }

    @Test
    void tinCuoiPhaiLaCauHoi() {
        assertThatThrownBy(() -> service.reply(new ChatRequest(List.of(
                new Message("user", "a"), new Message("assistant", "b")), null)))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ---- Dựng phản hồi giả theo chuẩn OpenAI chat/completions ---------------

    private static Map<String, Object> answer(String text) {
        return Map.of("choices", List.of(Map.of("finish_reason", "stop",
                "message", Map.of("role", "assistant", "content", text))));
    }

    private static Map<String, Object> toolCall(String id, String name, String arguments) {
        return Map.of("choices", List.of(Map.of("finish_reason", "tool_calls",
                "message", Map.of("role", "assistant", "content", "",
                        "tool_calls", List.of(Map.of("id", id, "type", "function",
                                "function", Map.of("name", name, "arguments", arguments)))))));
    }

    private static ComponentResponse part(String code, String category, String name) {
        return new ComponentResponse(1L, code, category, name, null, null, null, null, Map.of(), List.of());
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> messages(int call) {
        return (List<Map<String, Object>>) sent.get(call).get("messages");
    }
}
