package com.smartcarepc.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.smartcarepc.backend.repository.MaintenanceEventRepository;

/** Dùng database kiểm tra hiện có, rollback toàn bộ bản ghi thử nghiệm khi xong. */
@SpringBootTest(properties = {"app.feedback.export-key=maintenance-integration-key", "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc @Transactional
class MaintenancePersistenceTests {
    @Autowired MockMvc mvc;
    @Autowired MaintenanceEventRepository repository;
    @Test void luuThatGuiLaiKhongTrungVaXuatBangHeader() throws Exception {
        long before = repository.count();
        String id = UUID.randomUUID().toString();
        String body = """
          {"eventId":"%s","eventType":"step_done","task":"dust","participantCode":"TEST_MAINT","phase":"before","machine":"mini_sff","occurredAt":"2026-10-29T00:00:00Z","data":{"runId":"test-run-123","stepId":"safe","alone":false}}
          """.formatted(id);
        mvc.perform(post("/api/maintenance/events").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(post("/api/maintenance/events").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        assertThat(repository.count()).isEqualTo(before + 1);
        assertThat(repository.findFirstByEventId(id)).isPresent();
        mvc.perform(get("/api/maintenance/list")).andExpect(status().isForbidden());
        mvc.perform(get("/api/maintenance/export.csv").param("key", "maintenance-integration-key")).andExpect(status().isForbidden());
        mvc.perform(get("/api/maintenance/list").header("X-Export-Key", "maintenance-integration-key")).andExpect(status().isOk());
        mvc.perform(get("/api/maintenance/export.csv").header("X-Export-Key", "maintenance-integration-key")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(id)));
    }
}
