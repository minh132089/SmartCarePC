package com.smartcarepc.backend.controller;

import java.nio.charset.StandardCharsets;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.smartcarepc.backend.dto.MaintenanceEventRequest;
import com.smartcarepc.backend.service.MaintenanceEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/api/maintenance") @RequiredArgsConstructor
public class MaintenanceEventController {
    private final MaintenanceEventService service;
    @PostMapping("/events") @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceEventService.Saved create(@Valid @RequestBody MaintenanceEventRequest r) {
        try { return service.save(r); }
        catch (DataIntegrityViolationException e) { return service.existing(r.eventId()).orElseThrow(() -> e); }
    }
    @GetMapping("/list")
    public ResponseEntity<java.util.List<MaintenanceEventService.Row>> list(@RequestHeader(value = "X-Export-Key", required = false) String key) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(key));
    }
    @GetMapping("/export.csv")
    public ResponseEntity<String> export(@RequestHeader(value = "X-Export-Key", required = false) String key) {
        return ResponseEntity.ok().contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"pcsmartcare-maintenance.csv\"")
            .cacheControl(CacheControl.noStore()).body(service.exportCsv(key));
    }
}
