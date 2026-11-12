package com.smartcarepc.backend.dto;

import java.time.Instant;
import java.util.Map;
import jakarta.validation.constraints.*;

public record MaintenanceEventRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{8,64}") String eventId,
    @NotBlank @Pattern(regexp = "reminder_set|calendar_added|reminder_shown|reminder_snoozed|opened_from_calendar|maintenance_started|step_done|maintenance_completed|maintenance_abandoned|advice_asked|upgrade_asked|build_guide_opened|build_step_done|build_guide_completed") String eventType,
    @NotBlank @Pattern(regexp = "dust|paste|check|none") String task,
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,20}") String participantCode,
    @NotBlank @Pattern(regexp = "before|after") String phase,
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,60}") String machine,
    @NotNull Instant occurredAt,
    @NotNull @Size(max = 20) Map<String, Object> data) {}
