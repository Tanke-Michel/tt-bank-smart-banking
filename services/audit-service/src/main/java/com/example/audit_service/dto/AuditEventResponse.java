package com.example.audit_service.dto;

import com.example.audit_service.entity.AuditEvent;
import com.example.audit_service.enums.EventDomain;
import com.example.audit_service.enums.EventType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditEventResponse {

    private Long id;
    private EventDomain eventDomain;
    private EventType eventType;
    private String referenceCode;
    private String actorEmail;
    private String summary;
    private String rawPayload;
    private LocalDateTime eventTimestamp;
    private LocalDateTime receivedAt;

    public static AuditEventResponse from(AuditEvent e) {
        return AuditEventResponse.builder()
                .id(e.getId())
                .eventDomain(e.getEventDomain())
                .eventType(e.getEventType())
                .referenceCode(e.getReferenceCode())
                .actorEmail(e.getActorEmail())
                .summary(e.getSummary())
                .rawPayload(e.getRawPayload())
                .eventTimestamp(e.getEventTimestamp())
                .receivedAt(e.getReceivedAt())
                .build();
    }
}
