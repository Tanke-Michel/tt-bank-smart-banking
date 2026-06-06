package com.example.audit_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Audit & Admin Service — dual-purpose:
 *
 *  1. CONSUMER: subscribes to all 14 banking domain events via RabbitMQ
 *     and persists them to the tt_bank_audit database as immutable records.
 *
 *  2. REST API: exposes admin-only endpoints on /api/v1/admin/** so
 *     compliance officers and administrators can query the complete audit trail.
 *
 * All REST endpoints require ADMIN role enforced by @PreAuthorize.
 */
@SpringBootApplication
public class AuditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
