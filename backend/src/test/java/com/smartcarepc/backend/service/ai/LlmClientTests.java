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
    void ma200NhungThanLaLoiThiBaoDichVuAiGapSuCo() {
        Map<String, Object> err = Map.of("error", Map.of("message", "Upstream returned no output", "type", "api_error"), "type", "error");
        assertThatThrownBy(() -> LlmClient.checkReply(err)).isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("The external AI service is not returning a response");
        assertThatThrownBy(() -> LlmClient.checkReply(Map.of("choices", List.of()))).isInstanceOf(AiUnavailableException.class);
        assertThatThrownBy(() -> LlmClient.checkReply(null)).isInstanceOf(AiUnavailableException.class);
    }
}
