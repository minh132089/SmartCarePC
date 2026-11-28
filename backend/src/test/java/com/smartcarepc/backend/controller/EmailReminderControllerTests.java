package com.smartcarepc.backend.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.smartcarepc.backend.service.EmailReminderService;

@WebMvcTest(EmailReminderController.class)
class EmailReminderControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean EmailReminderService service;

    @Test void baoMayChuCoGuiMailDuocKhong() throws Exception {
        when(service.available()).thenReturn(false);
        mvc.perform(get("/api/reminders/email/available")).andExpect(status().isOk()).andExpect(jsonPath("$.available").value(false));
    }

    @Test void batTraTokenChoTrinhDuyet() throws Exception {
        when(service.create(eq("a@b.co"), any(), any())).thenReturn(new EmailReminderService.Created("manage-token-abcdefghijk", "pending"));
        mvc.perform(post("/api/reminders/email").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"a@b.co","dates":{"dust":"2026-12-05"},"months":{"dust":3,"paste":30,"check":1},"origin":"https://smartcarepc.pages.dev","proof":"ep.x.y"}
            """)).andExpect(status().isCreated()).andExpect(jsonPath("$.token").value("manage-token-abcdefghijk")).andExpect(jsonPath("$.status").value("pending"));
        verify(service).create("a@b.co", new EmailReminderService.Plan(Map.of("dust", "2026-12-05"), Map.of("dust", 3, "paste", 30, "check", 1), "https://smartcarepc.pages.dev"), "ep.x.y");
    }

    @Test void xacNhanVaHuyQuaNutBam() throws Exception {
        when(service.confirm("confirm-token-abcdefghijk")).thenReturn(new EmailReminderService.Status("active", "a@b.co"));
        mvc.perform(post("/api/reminders/email/confirm").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"confirm-token-abcdefghijk\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("active"));
        mvc.perform(post("/api/reminders/email/stop").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"manage-token-abcdefghijk\"}"))
            .andExpect(status().isNoContent());
        verify(service).stop("manage-token-abcdefghijk");
        mvc.perform(delete("/api/reminders/email/manage-token-abcdefghijk")).andExpect(status().isNoContent());
    }
}
