package com.smartcarepc.backend.controller;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import com.smartcarepc.backend.service.MaintenanceEventService;
@WebMvcTest(MaintenanceEventController.class)
class MaintenanceEventControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean MaintenanceEventService service;
    @Test void khongCoMaThuNghiemThiTuChoi() throws Exception {
        mvc.perform(post("/api/maintenance/events").contentType(MediaType.APPLICATION_JSON).content("""
          {"eventId":"test-event-123","eventType":"reminder_shown","task":"dust","phase":"before","machine":"gaming","occurredAt":"2026-10-29T00:00:00Z","data":{}}
          """)).andExpect(status().isBadRequest());
        verify(service, never()).save(any());
    }
    @Test void chiNhanKhoaQuaHeaderKhongQuaQuery() throws Exception {
        when(service.exportCsv(isNull())).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));
        mvc.perform(get("/api/maintenance/export.csv").param("key", "secret")).andExpect(status().isForbidden());
        when(service.exportCsv("secret")).thenReturn("event_id\r\n");
        mvc.perform(get("/api/maintenance/export.csv").header("X-Export-Key", "secret")).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
    }
    @Test void nhanLoaiSuKienCuaAppKhay() throws Exception {
        when(service.save(any())).thenReturn(new MaintenanceEventService.Saved(1));
        for (var type : new String[] { "agent_linked", "agent_reminder_shown", "health_alert_shown", "agent_notification_clicked", "agent_snoozed" })
            mvc.perform(post("/api/maintenance/events").contentType(MediaType.APPLICATION_JSON).content("""
              {"eventId":"test-event-%s","eventType":"%s","task":"none","participantCode":"P07","phase":"before","machine":"standard_tower","occurredAt":"2026-11-28T05:00:00.000Z","data":{"source":"health","rule":"cpu_hot"}}
              """.formatted(type.replace('_', '-'), type))).andExpect(status().is2xxSuccessful());
        mvc.perform(post("/api/maintenance/events").contentType(MediaType.APPLICATION_JSON).content("""
          {"eventId":"test-event-unknown","eventType":"agent_hacked","task":"none","participantCode":"P07","phase":"before","machine":"standard_tower","occurredAt":"2026-11-28T05:00:00.000Z","data":{}}
          """)).andExpect(status().isBadRequest());
    }
}
