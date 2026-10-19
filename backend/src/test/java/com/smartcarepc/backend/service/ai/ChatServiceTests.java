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

import com.smartcarepc.backend.dto.BuildCheckRequest;
import com.smartcarepc.backend.dto.ChatRequest;
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
