package com.example.audit_service.exception;
public class AuditEventNotFoundException extends RuntimeException {
    public AuditEventNotFoundException(String message) { super(message); }
}
