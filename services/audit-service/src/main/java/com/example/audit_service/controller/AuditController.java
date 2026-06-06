package com.example.audit_service.controller;

import com.example.audit_service.dto.*;
import com.example.audit_service.enums.EventDomain;
import com.example.audit_service.enums.EventType;
import com.example.audit_service.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Audit & Admin REST Controller.
 *
 * Base path: /api/v1/admin
 *
 * ALL endpoints are ADMIN-only, enforced by @PreAuthorize.
 * The gateway routes /api/v1/admin/** to this service.
 *
 * Endpoints:
 *   GET /api/v1/admin/audit/events         — search/list all audit events (paginated)
 *   GET /api/v1/admin/audit/events/{id}    — get a single event by DB id
 *   GET /api/v1/admin/audit/ref/{ref}      — get event by reference code
 *   GET /api/v1/admin/audit/domain/{d}     — events filtered by domain
 *   GET /api/v1/admin/audit/actor/{email}  — events by actor email
 *   GET /api/v1/admin/audit/stats          — dashboard statistics
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    // ================================================================
    // GET /api/v1/admin/audit/events
    // Full-text search with optional filters.
    // All parameters are optional — omitting them returns everything.
    // ================================================================
    @GetMapping("/audit/events")
    public ResponseEntity<PagedResponse<AuditEventResponse>> searchEvents(
            @RequestParam(required = false) EventDomain domain,
            @RequestParam(required = false) EventType    eventType,
            @RequestParam(required = false) String       actorEmail,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "50")  int size) {

        Pageable pageable = PageRequest.of(
                page, Math.min(size, 200),
                Sort.by(Sort.Direction.DESC, "receivedAt"));

        return ResponseEntity.ok(
                auditService.search(domain, eventType, actorEmail, from, to, pageable));
    }

    // ================================================================
    // GET /api/v1/admin/audit/events/{id}
    // Retrieve a single audit event by its database ID.
    // ================================================================
    @GetMapping("/audit/events/{id}")
    public ResponseEntity<AuditEventResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(auditService.getById(id));
    }

    // ================================================================
    // GET /api/v1/admin/audit/ref/{referenceCode}
    // Retrieve an audit event by its business reference code
    // (e.g. TXN-20240101-ABCD1234, PAY-..., CONT-..., POUT-...).
    // ================================================================
    @GetMapping("/audit/ref/{referenceCode}")
    public ResponseEntity<AuditEventResponse> getByRef(@PathVariable String referenceCode) {
        return ResponseEntity.ok(auditService.getByReferenceCode(referenceCode));
    }

    // ================================================================
    // GET /api/v1/admin/audit/domain/{domain}
    // All events from a specific service domain.
    // ================================================================
    @GetMapping("/audit/domain/{domain}")
    public ResponseEntity<PagedResponse<AuditEventResponse>> getByDomain(
            @PathVariable EventDomain domain,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(
                page, Math.min(size, 200),
                Sort.by(Sort.Direction.DESC, "receivedAt"));
        return ResponseEntity.ok(auditService.getByDomain(domain, pageable));
    }

    // ================================================================
    // GET /api/v1/admin/audit/actor/{email}
    // All events caused by a specific user.
    // ================================================================
    @GetMapping("/audit/actor/{email}")
    public ResponseEntity<PagedResponse<AuditEventResponse>> getByActor(
            @PathVariable String email,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(
                page, Math.min(size, 200),
                Sort.by(Sort.Direction.DESC, "receivedAt"));
        return ResponseEntity.ok(auditService.getByActorEmail(email, pageable));
    }

    // ================================================================
    // GET /api/v1/admin/audit/stats
    // Counts and statistics for the admin dashboard.
    // ================================================================
    @GetMapping("/audit/stats")
    public ResponseEntity<AuditStatsResponse> getStats() {
        return ResponseEntity.ok(auditService.getStats());
    }
}
