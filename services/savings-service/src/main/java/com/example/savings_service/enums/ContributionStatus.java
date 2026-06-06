package com.example.savings_service.enums;

/**
 * Status of a single contribution record.
 *
 * PENDING   — expected but not yet paid for this round.
 * PAID      — successfully debited from member's wallet.
 * FAILED    — wallet debit failed (insufficient funds, wallet suspended, etc.).
 * WAIVED    — admin waived this contribution (e.g. founder's exemption).
 */
public enum ContributionStatus {
    PENDING,
    PAID,
    FAILED,
    WAIVED
}
