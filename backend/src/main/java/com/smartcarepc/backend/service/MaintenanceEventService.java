package com.smartcarepc.backend.service;

import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.smartcarepc.backend.dto.MaintenanceEventRequest;
import com.smartcarepc.backend.entity.MaintenanceEvent;
import com.smartcarepc.backend.repository.MaintenanceEventRepository;
import tools.jackson.databind.json.JsonMapper;

@Service
public class MaintenanceEventService {
    private final MaintenanceEventRepository repository;
    private final FeedbackService feedback;
    private final JsonMapper json;
    public MaintenanceEventService(MaintenanceEventRepository repository, FeedbackService feedback, JsonMapper json) {
        this.repository = repository; this.feedback = feedback; this.json = json;
    }
    public record Saved(long id) {}
    public record Row(long id, String eventId, String eventType, String task, String participantCode, String phase,
                      String machine, String occurredAt, String createdAt, Map<String, Object> data) {}
    @Transactional
    public Saved save(MaintenanceEventRequest r) {
        var existing = repository.findFirstByEventId(r.eventId());
        if (existing.isPresent()) return new Saved(existing.get().getId());
        validate(r);
        String details = json.writeValueAsString(r.data());
        if (details.length() > 10_000) throw bad("Event details are too long");
        var event = repository.save(MaintenanceEvent.builder().eventId(r.eventId()).eventType(r.eventType())
            .task(r.task()).participantCode(r.participantCode()).phase(r.phase()).machine(r.machine())
            .occurredAt(r.occurredAt()).details(details).build());
        return new Saved(event.getId());
    }
    private static ResponseStatusException bad(String s) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, s); }
    private static void validate(MaintenanceEventRequest r) {
        if (r.occurredAt().isAfter(Instant.now().plusSeconds(86400))) throw bad("Event date is in the future");
        var d = r.data();
        if (r.eventType().equals("step_done") && (!(d.get("alone") instanceof Boolean) || !(d.get("stepId") instanceof String id) || !id.matches("[a-z-]{1,40}"))) throw bad("A step needs its id and whether it was completed alone");
        if (r.eventType().equals("maintenance_completed") && (!(d.get("who") instanceof String who) || !Set.of("me-alone", "with-help", "someone-else").contains(who))) throw bad("Who completed the work is required");
        if (r.eventType().equals("reminder_set") && (!(d.get("never_serviced") instanceof Boolean) || !(d.get("answers") instanceof Map))) throw bad("Reminder baseline answers are required");
        if (Set.of("maintenance_started", "step_done", "maintenance_completed", "maintenance_abandoned").contains(r.eventType())
                && (!(d.get("runId") instanceof String id) || !id.matches("[A-Za-z0-9-]{8,64}"))) throw bad("A maintenance run id is required");
    }
    @Transactional(readOnly = true)
    public Optional<Saved> existing(String eventId) { return repository.findFirstByEventId(eventId).map(e -> new Saved(e.getId())); }
    @Transactional(readOnly = true)
    public List<Row> list(String key) {
        feedback.authorizeExport(key);
        return repository.findAllByOrderByIdDesc().stream().map(this::row).toList();
    }
    @SuppressWarnings("unchecked")
    private Row row(MaintenanceEvent e) {
        Map<String, Object> data = json.readValue(e.getDetails(), LinkedHashMap.class);
        return new Row(e.getId(), e.getEventId(), e.getEventType(), e.getTask(), e.getParticipantCode(), e.getPhase(), e.getMachine(),
            e.getOccurredAt().toString(), e.getCreatedAt() == null ? null : e.getCreatedAt().toString(), data);
    }
    @Transactional(readOnly = true)
    public String exportCsv(String key) {
        var rows = list(key);
        var out = new StringBuilder("\ufeff");
        line(out, List.of("event_id", "event_type", "task", "participant_code", "phase", "machine", "occurred_at", "received_at", "run_id", "step_id", "alone", "who", "never_serviced", "details"));
        for (var r : rows) line(out, Arrays.asList(r.eventId(), r.eventType(), r.task(), r.participantCode(), r.phase(), r.machine(), r.occurredAt(), r.createdAt(),
            cell(r.data().get("runId")), cell(r.data().get("stepId")), cell(r.data().get("alone")), cell(r.data().get("who")), cell(r.data().get("never_serviced")), json.writeValueAsString(r.data())));
        return out.toString();
    }
    private static String cell(Object o) { return o == null ? "" : o.toString(); }
    private static void line(StringBuilder out, List<String> values) {
        out.append(values.stream().map(FeedbackService::escape).collect(java.util.stream.Collectors.joining(","))).append("\r\n");
    }
}
