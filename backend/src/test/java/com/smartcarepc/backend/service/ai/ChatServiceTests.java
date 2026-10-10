package com.smartcarepc.backend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import com.smartcarepc.backend.dto.BuildCheckRequest;
import com.smartcarepc.backend.dto.ChatRequest;
import com.smartcarepc.backend.dto.ChatRequest.Message;
import com.smartcarepc.backend.dto.CompatibilityResponse;
import com.smartcarepc.backend.dto.CompatibilityResponse.Issue;
import com.smartcarepc.backend.dto.PcBuildResponse;
import com.smartcarepc.backend.entity.enums.BuildType;
import com.smartcarepc.backend.service.CompatibilityService;
import com.smartcarepc.backend.service.ComponentService;
import com.smartcarepc.backend.service.PcBuildService;

import tools.jackson.databind.json.JsonMapper;

/** Vòng gọi công cụ của trợ lý, giả lập Claude bằng mock nên không cần mạng hay khoá API. */
class ChatServiceTests {

    private final AnthropicClient client = mock(AnthropicClient.class);
    private final CompatibilityService compatibility = mock(CompatibilityService.class);
    private final PcBuildService builds = mock(PcBuildService.class);
    /** Mockito giữ tham chiếu tới cùng một Map messages qua các lượt, nên chụp lại nội dung ngay lúc gọi */
    private final List<Map<String, Object>> sent = new ArrayList<>();
    private ChatService service;

    @BeforeEach
    void setUp() {
        when(builds.findPresets()).thenReturn(List.of());
        service = new ChatService(client, mock(ComponentService.class), compatibility, builds,
                JsonMapper.builder().build(), "test-model", 512);
    }

    @Test
    void traLoiThangKhiKhongCanCongCu() {
        reply(Map.of("stop_reason", "end_turn", "content", List.of(text("Chào bạn!"))));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "xin chào")), null));

        assertThat(res.reply()).isEqualTo("Chào bạn!");
        assertThat(sent.getFirst()).containsEntry("model", "test-model").containsEntry("max_tokens", 512);
    }

    @Test
    void chayCheckBuildRoiGuiKetQuaChoClaude() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(false,
                List.of(new Issue("case.psu", "Nguồn ATX quá to cho vỏ này.")), List.of()));
        when(builds.findByCode("mini_sff")).thenReturn(new PcBuildResponse(4L, "mini_sff", "Mini / SFF", "Hộp nhôm rất nhỏ.",
                BuildType.PRESET, null, List.of()));
        reply(Map.of("stop_reason", "tool_use", "content", List.of(
                        text("Để mình kiểm tra."),
                        Map.of("type", "tool_use", "id", "t1", "name", "check_build",
                                "input", Map.of("items", List.of(Map.of("code", "case_mini_sff"),
                                        Map.of("code", "psu_atx", "quantity", 1)))))),
                Map.of("stop_reason", "end_turn", "content", List.of(text("Nguồn này không vừa vỏ."))));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "lắp nguồn ATX vào vỏ mini được không?")), "mini_sff"));

        assertThat(res.reply()).isEqualTo("Nguồn này không vừa vỏ.");
        assertThat((String) sent.getFirst().get("system")).contains("đang xem máy \"Mini / SFF\"");
        var req = ArgumentCaptor.forClass(BuildCheckRequest.class);
        verify(compatibility).check(req.capture());
        assertThat(req.getValue().items()).extracting(BuildCheckRequest.Item::code).containsExactly("case_mini_sff", "psu_atx");

        // Lượt 2 gửi lại lời gọi công cụ và kết quả của nó
        @SuppressWarnings("unchecked")
        var messages = (List<Map<String, Object>>) sent.get(1).get("messages");
        assertThat(messages).hasSize(3);
        @SuppressWarnings("unchecked")
        var result = ((List<Map<String, Object>>) messages.get(2).get("content")).getFirst();
        assertThat(result).containsEntry("type", "tool_result").containsEntry("tool_use_id", "t1");
        assertThat((String) result.get("content")).contains("Nguồn ATX quá to");
    }

    @Test
    void congCuLoiThiBaoLaiChoClaudeThayViNemLoi() {
        reply(Map.of("stop_reason", "tool_use", "content", List.of(
                        Map.of("type", "tool_use", "id", "t1", "name", "khong_ton_tai", "input", Map.of()))),
                Map.of("stop_reason", "end_turn", "content", List.of(text("Ok."))));

        service.reply(new ChatRequest(List.of(new Message("user", "?")), null));

        @SuppressWarnings("unchecked")
        var messages = (List<Map<String, Object>>) sent.get(1).get("messages");
        @SuppressWarnings("unchecked")
        var result = ((List<Map<String, Object>>) messages.get(2).get("content")).getFirst();
        assertThat(result).containsEntry("is_error", true);
    }

    @Test
    void boLoiChaoMoDauCuaTroLy() {
        reply(Map.of("stop_reason", "end_turn", "content", List.of(text("Ok."))));

        service.reply(new ChatRequest(List.of(new Message("assistant", "Chào bạn, mình giúp gì được?"),
                new Message("user", "máy nóng quá")), null));

        @SuppressWarnings("unchecked")
        var messages = (List<Map<String, Object>>) sent.getFirst().get("messages");
        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst()).containsEntry("role", "user");
    }

    @Test
    void tinCuoiPhaiLaCauHoi() {
        assertThatThrownBy(() -> service.reply(new ChatRequest(List.of(
                new Message("user", "a"), new Message("assistant", "b")), null)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    private void reply(Map<String, Object>... responses) {
        var queue = new ArrayList<>(List.of(responses));
        when(client.createMessage(any())).thenAnswer(inv -> {
            Map<String, Object> body = inv.getArgument(0);
            var copy = new java.util.LinkedHashMap<>(body);
            copy.put("messages", new ArrayList<>((List<Object>) body.get("messages")));
            sent.add(copy);
            return queue.removeFirst();
        });
    }

    private static Map<String, Object> text(String t) {
        return Map.of("type", "text", "text", t);
    }
}
