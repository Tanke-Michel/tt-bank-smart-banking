package com.example.audit_service.service;

import com.example.audit_service.dto.*;
import com.example.audit_service.entity.AuditEvent;
import com.example.audit_service.enums.EventDomain;
import com.example.audit_service.enums.EventType;
import com.example.audit_service.exception.AuditEventNotFoundException;
import com.example.audit_service.repository.AuditEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    // ================================================================
    // PERSIST — called by listeners
    // ================================================================

    /**
     * Persists a raw event payload as an immutable audit record.
     * Called by all four listener classes.
     *
     * @param domain     the originating service domain
     * @param eventType  the specific event type
     * @param event      the raw Map payload from RabbitMQ
     */
    @Transactional
    public void persistEvent(EventDomain domain, EventType eventType, Map<String, Object> event) {
        try {
            String rawJson = objectMapper.writeValueAsString(event);

            AuditEvent auditEvent = AuditEvent.builder()
                    .eventDomain(domain)
                    .eventType(eventType)
                    .referenceCode(extractReferenceCode(event))
                    .actorEmail(extractActorEmail(event, domain))
                    .summary(buildSummary(eventType, event))
                    .rawPayload(rawJson)
                    .eventTimestamp(parseTimestamp(event))
                    .build();

            auditEventRepository.save(auditEvent);
            log.debug("Audit event persisted: domain={} type={}", domain, eventType);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event payload for audit: domain={} type={} error={}",
                    domain, eventType, e.getMessage());
            // Persist with raw toString as fallback so no event is ever lost
            AuditEvent fallback = AuditEvent.builder()
                    .eventDomain(domain)
                    .eventType(eventType)
                    .rawPayload(event.toString())
                    .summary("Serialization error — stored as toString")
                    .build();
            auditEventRepository.save(fallback);
        }
    }

    // ================================================================
    // QUERIES — admin REST endpoints
    // ================================================================

    @Transactional(readOnly = true)
    public AuditEventResponse getById(Long id) {
        AuditEvent event = auditEventRepository.findById(id)
                .orElseThrow(() -> new AuditEventNotFoundException(
                        "Audit event not found: " + id));
        return AuditEventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public AuditEventResponse getByReferenceCode(String referenceCode) {
        AuditEvent event = auditEventRepository.findByReferenceCode(referenceCode)
                .orElseThrow(() -> new AuditEventNotFoundException(
                        "No audit event found for reference: " + referenceCode));
        return AuditEventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditEventResponse> search(
            EventDomain domain,
            EventType eventType,
            String actorEmail,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {

        Page<AuditEvent> page = auditEventRepository.searchAuditEvents(
                domain, eventType, actorEmail, from, to, pageable);
        return PagedResponse.from(page, AuditEventResponse::from);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditEventResponse> getByDomain(EventDomain domain, Pageable pageable) {
        Page<AuditEvent> page = auditEventRepository.findByEventDomain(domain, pageable);
        return PagedResponse.from(page, AuditEventResponse::from);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditEventResponse> getByActorEmail(String email, Pageable pageable) {
        Page<AuditEvent> page = auditEventRepository.findByActorEmail(email, pageable);
        return PagedResponse.from(page, AuditEventResponse::from);
    }

    @Transactional(readOnly = true)
    public AuditStatsResponse getStats() {
        long totalEvents = auditEventRepository.count();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayEvents = auditEventRepository.countByReceivedAtBetween(
                todayStart, LocalDateTime.now());

        // Count by domain
        List<Object[]> domainCounts = auditEventRepository.countByDomain();
        Map<String, Long> byDomain = new LinkedHashMap<>();
        for (Object[] row : domainCounts) {
            byDomain.put(row[0].toString(), (Long) row[1]);
        }

        // Count by event type today
        List<Object[]> typeCounts = auditEventRepository.countByTypeFrom(todayStart);
        Map<String, Long> todayByType = new LinkedHashMap<>();
        for (Object[] row : typeCounts) {
            todayByType.put(row[0].toString(), (Long) row[1]);
        }

        return AuditStatsResponse.builder()
                .totalEvents(totalEvents)
                .todayEvents(todayEvents)
                .byDomain(byDomain)
                .todayByType(todayByType)
                .build();
    }

    // ================================================================
    // Private helpers — payload extraction
    // ================================================================

    private String extractReferenceCode(Map<String, Object> event) {
        for (String key : List.of("referenceCode", "reference_code")) {
            Object val = event.get(key);
            if (val != null && !val.toString().isBlank()) return val.toString();
        }
        return null;
    }

    /**
     * Extract the acting user's email based on domain conventions.
     * Each publisher uses different field names for the actor email.
     */
    private String extractActorEmail(Map<String, Object> event, EventDomain domain) {
        // Try domain-specific keys first, then fall back to generic ones
        List<String> candidates = switch (domain) {
            case WALLET      -> List.of("email", "ownerEmail");
            case TRANSACTION -> List.of("senderEmail", "email");
            case MERCHANT    -> List.of("customerEmail", "ownerEmail", "email");
            case SAVINGS     -> List.of("creatorEmail", "userEmail", "recipientEmail", "email");
        };
        for (String key : candidates) {
            Object val = event.get(key);
            if (val != null && !val.toString().isBlank()) return val.toString();
        }
        return null;
    }

    /**
     * Build a concise human-readable summary for the audit record.
     */
    private String buildSummary(EventType eventType, Map<String, Object> event) {
        String ref   = str(event, "referenceCode");
        String amt   = str(event, "amount");
        String curr  = str(event, "currency");
        String email = str(event, "senderEmail") != null
                ? str(event, "senderEmail") : str(event, "email");

        return switch (eventType) {
            case WALLET_CREATED       -> "Wallet created for " + str(event, "email");
            case WALLET_FUNDED        -> "Deposit " + amt + " " + curr + " ref=" + ref;
            case WALLET_WITHDRAWN     -> "Withdrawal " + amt + " " + curr + " ref=" + ref;
            case TRANSACTION_INITIATED-> "Transfer initiated " + amt + " " + curr
                    + " from=" + email + " ref=" + ref;
            case TRANSACTION_COMPLETED-> "Transfer completed " + amt + " " + curr + " ref=" + ref;
            case TRANSACTION_FAILED   -> "Transfer failed " + amt + " " + curr + " ref=" + ref;
            case MERCHANT_REGISTERED  -> "Merchant registered: " + str(event, "businessName")
                    + " code=" + str(event, "merchantCode");
            case MERCHANT_PAYMENT_INITIATED  -> "Payment initiated " + amt + " " + curr
                    + " merchant=" + str(event, "businessName") + " ref=" + ref;
            case MERCHANT_PAYMENT_COMPLETED  -> "Payment completed " + amt + " " + curr
                    + " merchant=" + str(event, "businessName") + " ref=" + ref;
            case MERCHANT_PAYMENT_FAILED     -> "Payment failed " + amt + " " + curr
                    + " merchant=" + str(event, "businessName") + " ref=" + ref;
            case SAVINGS_GROUP_CREATED       -> "Savings group created: " + str(event, "groupName");
            case SAVINGS_MEMBER_JOINED       -> "Member joined group: " + str(event, "groupName")
                    + " user=" + str(event, "userEmail");
            case SAVINGS_CONTRIBUTION_MADE   -> "Contribution " + amt + " " + curr
                    + " group=" + str(event, "groupName")
                    + " round=" + str(event, "roundNumber") + " ref=" + ref;
            case SAVINGS_PAYOUT_PROCESSED    -> "Payout " + amt + " " + curr
                    + " group=" + str(event, "groupName")
                    + " round=" + str(event, "roundNumber") + " ref=" + ref;
            default -> eventType.name();
        };
    }

    private LocalDateTime parseTimestamp(Map<String, Object> event) {
        Object ts = event.get("timestamp");
        if (ts == null) return null;
        try {
            return LocalDateTime.parse(ts.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String str(Map<String, Object> event, String key) {
        Object val = event.get(key);
        return val != null ? val.toString() : null;
    }
}
