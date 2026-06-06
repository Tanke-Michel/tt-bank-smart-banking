package com.example.transaction_service.enums;

/**
 * Lifecycle of a transfer:
 *
 *  PENDING   → The transfer record is created; money has not moved yet.
 *  COMPLETED → Debit from sender AND credit to receiver both succeeded.
 *  FAILED    → One or both wallet operations failed; no money moved.
 *  REVERSED  → A completed transfer was reversed by admin action.
 */
public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REVERSED
}
