package com.smartcarepc.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.smartcarepc.backend.dto.FeedbackRequest;
import com.smartcarepc.backend.entity.Feedback;
import com.smartcarepc.backend.repository.FeedbackRepository;

import tools.jackson.databind.json.JsonMapper;

class FeedbackDedupTests {

    @Test
    void cungSubmissionIdThiTraBanDaLuuKhongLuuThem() {
        var repo = mock(FeedbackRepository.class);
        var existing = Feedback.builder().answers("{}").build();
        existing.setId(7L);
        when(repo.findFirstBySubmissionId("abc12345-xyz")).thenReturn(Optional.of(existing));
        var service = new FeedbackService(repo, JsonMapper.builder().build(), "");

        var saved = service.save(new FeedbackRequest(null, null, "gaming", Map.of("stars", 4, "submission_id", "abc12345-xyz")));

        assertThat(saved.id()).isEqualTo(7L);
        verify(repo, never()).save(any());
    }
}
