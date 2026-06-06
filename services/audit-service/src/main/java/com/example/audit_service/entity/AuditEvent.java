package com.example.audit_service.entity;

import com.example.audit_service.enums.EventDomain;
import com.example.audit_service.enums.EventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Immutable audit record for every banking event.
 *
 * Design decisions:
 *  - rawPayload (TEXT): the full JSON event stored verbatim.
 *    Compliance requires the original payload — no lossy mapping.
 *  - referenceCode: the idempotency key from the originating event
 *    (TXN-..., PAY-..., CONT-..., etc.). Null for wallet/group events.
 *  - actorEmail: the user who caused the event. Denormalised from payload.
 *  - No @Version / no updatable fields — audit records are immutable.
 *  - receivedAt: when this service received and persisted the event.
 *  - eventTimestamp: the timestamp from the event payload itself.
 *
 * Indexes on domain + eventType + receivedAt for efficient admin queries.
 */
@Entity
@Table(
    name = "audit_events",
    indexes = {
        @Index(name = "idx_audit_domain",     columnList = "event_domain"),
        @Index(name = "idx_audit_type",       columnList = "event_type"),
        @Index(name = "idx_audit_ref",        columnList = "reference_code"),
        @Index(name = "idx_audit_actor",      columnList = "actor_email"),
        @Index(name = "idx_audit_received",   columnList = "received_at"),
        @Index(name = "idx_audit_domain_time",columnList = "event_domain, received_at")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_domain", nullable = false, length = 20)
    private EventDomain eventDomain;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private EventType eventType;

    /**
     * Business reference code from the originating event.
     * TXN-xxx for transfers, PAY-xxx for payments, CONT-xxx for contributions,
     * POUT-xxx for payouts. Null for entity-creation events.
     */
    @Column(name = "reference_code", length = 40)
    private String referenceCode;

    /**
     * Email of the user who triggered the action.
     * Derived from senderEmail / customerEmail / creatorEmail / ownerEmail
     * depending on the event domain.
     */
    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    /**
     * Brief human-readable summary extracted from the payload.
     * e.g. "Transfer 500 XAF to receiver@example.com"
     */
    @Column(name = "summary", length = 500)
    private String summary;

    /**
     * Full JSON payload as published by the originating service.
     * Stored as TEXT for complete fidelity.
     */
    @Column(name = "raw_payload", columnDefinition = "TEXT", nullable = false)
    private String rawPayload;

    /**
     * Timestamp from the event payload (the eventTimestamp field).
     * May be null if the publisher did not include it.
     */
    @Column(name = "event_timestamp")
    private LocalDateTime eventTimestamp;

    /**
     * When this audit-service received and persisted the event.
     */
    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @PrePersist
    protected void onCreate() {
        receivedAt = LocalDateTime.now();
    }
}
