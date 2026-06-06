package com.example.audit_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AuditStatsResponse {
    private long totalEvents;
    private long todayEvents;
    private Map<String, Long> byDomain;
    private Map<String, Long> todayByType;
}
