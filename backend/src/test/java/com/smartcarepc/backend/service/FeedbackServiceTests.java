package com.smartcarepc.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.smartcarepc.backend.dto.FeedbackRequest;
import com.smartcarepc.backend.entity.Feedback;
import com.smartcarepc.backend.repository.FeedbackRepository;

import tools.jackson.databind.json.JsonMapper;

class FeedbackServiceTests {

    private final FeedbackRepository repo = mock(FeedbackRepository.class);
    private final FeedbackService service = new FeedbackService(repo, JsonMapper.builder().build(), "khoa-bi-mat");

    private static Feedback row(long id, String code, String answers) {
        Feedback f = Feedback.builder().id(id).participantCode(code).phase("before").machine("gaming").answers(answers).build();
        f.setCreatedAt(LocalDateTime.of(2026, 10, 12, 9, 30));
        return f;
    }

    @Test
    void luuCauTraLoiThanhJson() {
        when(repo.save(any())).thenAnswer(inv -> {
            Feedback f = inv.getArgument(0);
            assertThat(f.getAnswers()).isEqualTo("{\"budget_fit\":\"4\",\"guide\":\"5\"}");
            assertThat(f.getMachine()).isNull();
            f.setId(3L);
            return f;
        });

        var answers = new LinkedHashMap<String, Object>();
        answers.put("budget_fit", "4");
        answers.put("guide", "5");
        var saved = service.save(new FeedbackRequest("P07", "before", "  ", answers));

        assertThat(saved.id()).isEqualTo(3L);
    }

    @Test
    void cauTraLoiQuaDaiBiTuChoi() {
        var huge = Map.<String, Object>of("suggestions", "x".repeat(FeedbackService.MAX_ANSWERS_CHARS));
        assertThatThrownBy(() -> service.save(new FeedbackRequest(null, null, null, huge)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void xuatCsvMoiCauHoiMotCot() {
        when(repo.findAllByOrderByIdAsc()).thenReturn(List.of(
                row(1, "P07", "{\"budget_fit\":\"4\",\"guide\":\"5\"}"),
                row(2, "P08", "{\"guide\":\"3\",\"difficulties\":\"Khó \\\"cắm\\\" dây\",\"extra\":[\"a\",\"b\"]}")));

        String csv = service.exportCsv("khoa-bi-mat");
        String[] lines = csv.substring(1).split("\r\n");

        assertThat(csv).startsWith("\uFEFF");
        assertThat(lines[0]).isEqualTo("\"id\",\"created_at\",\"participant_code\",\"phase\",\"machine\","
                + "\"a.budget_fit\",\"a.guide\",\"a.difficulties\",\"a.extra\"");
        assertThat(lines[1]).isEqualTo("\"1\",\"2026-10-12 09:30:00\",\"P07\",\"before\",\"gaming\",\"4\",\"5\",\"\",\"\"");
        assertThat(lines[2]).endsWith("\"3\",\"Khó \"\"cắm\"\" dây\",\"[\"\"a\"\",\"\"b\"\"]\"");
    }

    @Test
    void oBatDauBangDauBangKhongThanhCongThuc() {
        assertThat(FeedbackService.escape("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(FeedbackService.escape("-5")).isEqualTo("\"'-5\"");
    }

    @Test
    void saiKhoaHoacChuaDatKhoaThiKhongXuat() {
        assertThatThrownBy(() -> service.exportCsv("doan-bua")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.exportCsv(null)).isInstanceOf(ResponseStatusException.class);
        var noKey = new FeedbackService(repo, JsonMapper.builder().build(), " ");
        assertThatThrownBy(() -> noKey.exportCsv("")).isInstanceOf(ResponseStatusException.class);
    }
}
