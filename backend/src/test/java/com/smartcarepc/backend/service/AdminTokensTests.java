package com.smartcarepc.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import com.smartcarepc.backend.repository.FeedbackRepository;
import tools.jackson.databind.json.JsonMapper;

class AdminTokensTests {
    private static final Instant NOW = Instant.parse("2026-11-13T00:00:00Z");

    @Test
    void veHopLeTrongHanVaHetHanSau12Gio() {
        var tokens = new AdminTokens(Clock.fixed(NOW, ZoneOffset.UTC));
        String token = tokens.issue("admin@smartcarepc.local").token();
        assertThat(tokens.valid(token)).isTrue();
        var later = new AdminTokens(Clock.fixed(NOW.plus(Duration.ofHours(13)), ZoneOffset.UTC));
        assertThat(later.valid(token)).isFalse(); // khoá khác (khởi động lại) cũng không nhận
    }

    @Test
    void veBiSuaHoacKhongPhaiVeThiBiTuChoi() {
        var tokens = new AdminTokens();
        String token = tokens.issue("admin@smartcarepc.local").token();
        assertThat(tokens.valid(token.substring(0, token.length() - 2) + "xx")).isFalse();
        assertThat(tokens.valid("adm.abc")).isFalse();
        assertThat(tokens.valid("khoa-bi-mat")).isFalse();
        assertThat(tokens.valid(null)).isFalse();
    }

    @Test
    void veChoXemDanhSachNhungKhongChoTaiCsv() {
        var repo = mock(FeedbackRepository.class);
        var service = new FeedbackService(repo, JsonMapper.builder().build(), "khoa-bi-mat");
        var tokens = new AdminTokens();
        service.setAdminTokens(tokens);
        String token = tokens.issue("admin@smartcarepc.local").token();
        assertThat(service.list(token)).isEmpty();
        assertThatThrownBy(() -> service.exportCsv(token)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.list("adm.gia.mao")).isInstanceOf(ResponseStatusException.class);
        assertThat(service.list("khoa-bi-mat")).isEmpty();
    }
}
