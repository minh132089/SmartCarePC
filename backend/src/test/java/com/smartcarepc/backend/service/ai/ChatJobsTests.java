package com.smartcarepc.backend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.smartcarepc.backend.dto.ChatRequest;
import com.smartcarepc.backend.dto.ChatRequest.Message;
import com.smartcarepc.backend.dto.ChatResponse;
import com.smartcarepc.backend.exception.AiUnavailableException;

class ChatJobsTests {

    private final ChatService chat = mock(ChatService.class);
    private final ChatJobs jobs = new ChatJobs(chat);
    private final ChatRequest req = new ChatRequest(List.of(new Message("user", "học 12 triệu")), null, null, "advice");

    @Test
    @SuppressWarnings("unchecked")
    void chayNenRoiTraKetQuaVaBuocDangLam() throws Exception {
        when(chat.reply(any(), any())).thenAnswer(inv -> {
            ((Consumer<String>) inv.getArgument(1)).accept(ChatService.STAGE_SEARCH);
            return new ChatResponse("Xong.", null, null);
        });

        var started = jobs.start(req);
        var done = jobs.await(started.id(), Duration.ofSeconds(5));

        assertThat(started.status()).isIn(ChatJobs.Status.running, ChatJobs.Status.done);
        assertThat(done.status()).isEqualTo(ChatJobs.Status.done);
        assertThat(done.result().reply()).isEqualTo("Xong.");
        assertThat(done.stage()).isEqualTo(ChatService.STAGE_SEARCH);
    }

    @Test
    void loiCuaDichVuAiThanhLoiCuaViec() throws Exception {
        when(chat.reply(any(), any())).thenThrow(new AiUnavailableException(HttpStatus.BAD_GATEWAY, "Dịch vụ AI lỗi"));
        var done = jobs.await(jobs.start(req).id(), Duration.ofSeconds(5));
        assertThat(done.status()).isEqualTo(ChatJobs.Status.error);
        assertThat(done.error()).isEqualTo("Dịch vụ AI lỗi");
    }

    @Test
    void maViecLaThiBao404() {
        assertThatThrownBy(() -> jobs.get("khong-co")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void loiNgoaiRuntimeKhongDeJobChayMai() throws Exception {
        when(chat.reply(any(), any())).thenThrow(new LinkageError("lỗi liên kết lớp"));
        var done = jobs.await(jobs.start(req).id(), Duration.ofSeconds(5));
        assertThat(done.status()).isEqualTo(ChatJobs.Status.error);
        assertThat(done.error()).contains("The assistant encountered an error");
    }
}
