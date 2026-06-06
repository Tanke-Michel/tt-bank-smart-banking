package com.example.audit_service.enums;

/**
 * All possible event types across all banking domains.
 * These match the eventType field in the Map payloads published
 * by each service's EventPublisher.
 */
public enum EventType {
    // Wallet domain
    WALLET_CREATED,
    WALLET_FUNDED,
    WALLET_WITHDRAWN,

    // Transaction domain
    TRANSACTION_INITIATED,
    TRANSACTION_COMPLETED,
    TRANSACTION_FAILED,

    // Merchant domain
    MERCHANT_REGISTERED,
    MERCHANT_PAYMENT_INITIATED,
    MERCHANT_PAYMENT_COMPLETED,
    MERCHANT_PAYMENT_FAILED,

    // Savings domain
    SAVINGS_GROUP_CREATED,
    SAVINGS_MEMBER_JOINED,
    SAVINGS_CONTRIBUTION_MADE,
    SAVINGS_PAYOUT_PROCESSED,

    // Fallback for unknown events
    UNKNOWN
}
