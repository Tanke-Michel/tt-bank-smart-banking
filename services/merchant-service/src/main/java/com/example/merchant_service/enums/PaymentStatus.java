package com.example.merchant_service.enums;

/**
 * Lifecycle of a merchant QR payment.
 *
 * PENDING   — payment record created, debit not yet attempted.
 * COMPLETED — customer's wallet was debited, merchant's wallet credited.
 * FAILED    — debit or credit failed; no money moved (or reversal applied).
 */
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED
}
