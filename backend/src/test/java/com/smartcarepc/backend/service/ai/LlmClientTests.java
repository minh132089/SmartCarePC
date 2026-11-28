package com.smartcarepc.backend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.smartcarepc.backend.exception.AiUnavailableException;

class LlmClientTests {

    @Test
    void phanHoiCoChoicesThiDungLuon() {
        Map<String, Object> ok = Map.of("choices", List.of(Map.of("message", Map.of("content", "ok"))));
        assertThat(LlmClient.checkReply(ok)).isSameAs(ok);
    }

    @Test
    @SuppressWarnings("unchecked")
    void lenhGoiCongCuDangChuDsmlDuocDocLaiThanhToolCalls() {
        // Ca thật 23/11: DeepSeek trả lệnh gọi công cụ dạng chữ, người dùng thấy nguyên đoạn DSML trong khung chat
        String content = "I'll pick sensible assumptions: AAA gaming plus coding, no heavy local AI.\n"
                + "<｜DSML｜function_calls>\n"
                + "<｜DSML｜invoke name=\"search_products\">\n"
                + "<｜DSML｜parameter name=\"query\" string=\"true\">Ryzen 7 7800X3D</｜DSML｜parameter>\n"
                + "</｜DSML｜invoke>\n"
                + "<｜DSML｜invoke name=\"search_products\">\n"
                + "<｜DSML｜parameter name=\"query\" string=\"true\">Core i5 14600K</｜DSML｜parameter>\n"
                + "</｜DSML｜invoke>\n"
                + "<| DSML |invoke name=\"propose_build\">\n"
                + "<| DSML |parameter name=\"budget_vnd\" string=\"false\">30000000</| DSML |parameter>\n"
                + "<| DSML |parameter name=\"items\" string=\"false\">[{\"id\":\"s1-2\"}]</| DSML |parameter>\n"
                + "</| DSML |invoke>\n"
                + "</｜DSML｜function_calls>";
        Map<String, Object> res = Map.of("choices", List.of(Map.of("message", Map.of("role", "assistant", "content", content),
                "finish_reason", "stop")));
        var fixed = LlmClient.checkReply(res);
        var choice = ((List<Map<String, Object>>) fixed.get("choices")).getFirst();
        var message = (Map<String, Object>) choice.get("message");
        var calls = (List<Map<String, Object>>) message.get("tool_calls");
        assertThat(message.get("content")).isEqualTo("I'll pick sensible assumptions: AAA gaming plus coding, no heavy local AI.");
        assertThat(choice.get("finish_reason")).isEqualTo("tool_calls");
        assertThat(calls).hasSize(3);
        var first = (Map<String, Object>) calls.getFirst().get("function");
        assertThat(first.get("name")).isEqualTo("search_products");
        assertThat(first.get("arguments")).isEqualTo("{\"query\":\"Ryzen 7 7800X3D\"}");
        var third = (Map<String, Object>) calls.get(2).get("function");
        assertThat(third.get("name")).isEqualTo("propose_build");
        assertThat(third.get("arguments")).isEqualTo("{\"budget_vnd\":30000000,\"items\":[{\"id\":\"s1-2\"}]}");
        assertThat(calls.stream().map(c -> c.get("id")).distinct()).hasSize(3);
    }

    @Test
    @SuppressWarnings("unchecked")
    void theDsmlBiCatDoKhongDocDuocThiChiGiuPhanLoi() {
        String content = "Let me search.\n<｜DSML｜function_calls>\n<｜DSML｜invoke name=\"search_pro";
        var fixed = LlmClient.checkReply(Map.of("choices", List.of(Map.of("message", Map.of("content", content)))));
        var message = (Map<String, Object>) ((List<Map<String, Object>>) fixed.get("choices")).getFirst().get("message");
        assertThat(message.get("content")).isEqualTo("Let me search.");
        assertThat(message.get("tool_calls")).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void motLenhDuMotLenhBiCatThiKhongChayNuaNhomVaGiuLength() {
        // Review 23/11: lệnh đầu đủ, lệnh sau bị cắt. Trước đây giữ lệnh đầu và đổi finish_reason length thành tool_calls
        String content = "Searching.\n<｜DSML｜function_calls>\n"
                + "<｜DSML｜invoke name=\"search_products\">\n"
                + "<｜DSML｜parameter name=\"query\" string=\"true\">RTX 5060</｜DSML｜parameter>\n"
                + "</｜DSML｜invoke>\n"
                + "<｜DSML｜invoke name=\"search_products\">\n"
                + "<｜DSML｜parameter name=\"query\" string=\"true\">Ryzen 5 96";
        for (String reason : new String[] { "length", "stop" }) {
            var fixed = LlmClient.checkReply(Map.of("choices", List.of(Map.of("message", Map.of("content", content), "finish_reason", reason))));
            var choice = ((List<Map<String, Object>>) fixed.get("choices")).getFirst();
            var message = (Map<String, Object>) choice.get("message");
            assertThat(message.get("content")).isEqualTo("Searching.");
            assertThat(message.get("tool_calls")).isNull();
            assertThat(choice.get("finish_reason")).isEqualTo("length");
        }
    }

    @Test
    void ma200NhungThanLaLoiThiBaoDichVuAiGapSuCo() {
        Map<String, Object> err = Map.of("error", Map.of("message", "Upstream returned no output", "type", "api_error"), "type", "error");
        assertThatThrownBy(() -> LlmClient.checkReply(err)).isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("The external AI service is not returning a response");
        assertThatThrownBy(() -> LlmClient.checkReply(Map.of("choices", List.of()))).isInstanceOf(AiUnavailableException.class);
        assertThatThrownBy(() -> LlmClient.checkReply(null)).isInstanceOf(AiUnavailableException.class);
    }
}
