package com.smartcarepc.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import com.smartcarepc.backend.dto.MaintenanceEventRequest;
import com.smartcarepc.backend.entity.MaintenanceEvent;
import com.smartcarepc.backend.repository.MaintenanceEventRepository;
import tools.jackson.databind.json.JsonMapper;

class MaintenanceEventServiceTests {
    final MaintenanceEventRepository repo = mock(MaintenanceEventRepository.class);
    final FeedbackService feedback = mock(FeedbackService.class);
    final MaintenanceEventService service = new MaintenanceEventService(repo, feedback, JsonMapper.builder().build());
    MaintenanceEventRequest request(String type, Map<String, Object> data) {
        return new MaintenanceEventRequest("test-event-123", type, "dust", "P01", "before", "gaming", Instant.now(), data);
    }
    @Test void guiLaiKhongTaoBanTrung() {
        var event = MaintenanceEvent.builder().id(7L).eventId("test-event-123").build();
        when(repo.findFirstByEventId("test-event-123")).thenReturn(Optional.of(event));
        assertThat(service.save(request("maintenance_started", Map.of("runId", "test-run-123"))).id()).isEqualTo(7);
        verify(repo, never()).save(any());
    }
    @Test void ghiNhanNguoiCanGiupVaNguoiDungGiuaChung() {
        when(repo.findFirstByEventId(anyString())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> { MaintenanceEvent e = i.getArgument(0); e.setId(1L); return e; });
        service.save(request("step_done", Map.of("runId", "test-run-123", "stepId", "fans", "alone", false)));
        var captured = org.mockito.ArgumentCaptor.forClass(MaintenanceEvent.class);
        verify(repo).save(captured.capture());
        assertThat(captured.getValue().getDetails()).contains("\"alone\":false");
    }
    @Test void thieuKetQuaKhongLuuThanhCong() {
        when(repo.findFirstByEventId(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.save(request("maintenance_completed", Map.of("runId", "test-run-123")))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.save(request("step_done", Map.of("runId", "test-run-123", "stepId", "fans")))).isInstanceOf(ResponseStatusException.class);
        verify(repo, never()).save(any());
    }
    @Test void xuatDuLieuPhaiQuaCungKhoaFeedback() {
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN)).when(feedback).authorizeExport("wrong");
        assertThatThrownBy(() -> service.list("wrong")).isInstanceOf(ResponseStatusException.class);
        verify(repo, never()).findAllByOrderByIdDesc();
    }
    @Test void csvGiuCaCauTraLoiMocTruoc() {
        var e = MaintenanceEvent.builder().id(1L).eventId("test-event-123").eventType("reminder_set").task("dust")
            .participantCode("P01").phase("before").machine("gaming").occurredAt(Instant.parse("2026-10-29T00:00:00Z"))
            .details("{\"never_serviced\":true,\"answers\":{\"lastCleaned\":\"never\"}}").build();
        when(repo.findAllByOrderByIdDesc()).thenReturn(List.of(e));
        assertThat(service.exportCsv("valid")).contains("never_serviced", "true", "lastCleaned", "P01");
        verify(feedback).authorizeExport("valid");
    }
}
