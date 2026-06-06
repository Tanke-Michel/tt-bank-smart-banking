package com.example.savings_service.enums;

/**
 * Lifecycle of a savings group.
 *
 * FORMING   — group created; members can join before the cycle starts.
 * ACTIVE    — cycle is running; contributions are being collected.
 * COMPLETED — all rounds finished; group has been disbanded naturally.
 * CANCELLED — admin or creator cancelled the group before completion.
 */
public enum GroupStatus {
    FORMING,
    ACTIVE,
    COMPLETED,
    CANCELLED
}
