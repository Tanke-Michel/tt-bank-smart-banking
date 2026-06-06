package com.example.wallet_service.enums;

public enum TransactionStatus {
    /** Transaction is being processed */
    PENDING,
    /** Transaction completed successfully */
    COMPLETED,
    /** Transaction failed */
    FAILED,
    /** Transaction was reversed after completion */
    REVERSED
}
