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
import com.smartcarepc.backend.service.PcBuildService;

import tools.jackson.databind.json.JsonMapper;

/** Vòng gọi công cụ của trợ lý, giả lập API AI bằng mock nên không cần mạng hay khoá API. */
class ChatServiceTests {

    private final LlmClient client = mock(LlmClient.class);
    private final ComponentService components = mock(ComponentService.class);
    private final CompatibilityService compatibility = mock(CompatibilityService.class);
    private final PcBuildService builds = mock(PcBuildService.class);
    /** Mockito giữ tham chiếu tới cùng một danh sách messages qua các lượt, nên chụp lại nội dung ngay lúc gọi */
    private final List<Map<String, Object>> sent = new ArrayList<>();
    private ChatService service;

    @BeforeEach
    void setUp() {
        when(builds.findPresets()).thenReturn(List.of());
        service = new ChatService(client, components, compatibility, builds, JsonMapper.builder().build(), "test-model", 512);
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
