package com.example.savings_service.enums;

/**
 * Status of a payout disbursement to a group member.
 *
 * SCHEDULED — payout is planned for this round but not yet executed.
 * COMPLETED — payout successfully credited to the member's wallet.
 * FAILED    — payout credit to wallet failed.
 */
public enum PayoutStatus {
    SCHEDULED,
    COMPLETED,
    FAILED
}
