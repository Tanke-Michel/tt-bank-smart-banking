package com.example.savings_service.enums;

/**
 * Status of a group member.
 *
 * ACTIVE    — currently participating and contributing.
 * DEFAULTED — missed a contribution in the current round.
 * REMOVED   — removed by the group admin.
 */
public enum MemberStatus {
    ACTIVE,
    DEFAULTED,
    REMOVED
}
