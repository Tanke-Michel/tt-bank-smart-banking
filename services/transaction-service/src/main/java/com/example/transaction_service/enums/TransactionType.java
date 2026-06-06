package com.example.transaction_service.enums;

/**
 * Type of transaction recorded by this service.
 * Only TRANSFER is used for now; PAYMENT is reserved for merchant QR payments
 * which will be added in Phase F (Merchant Service).
 */
public enum TransactionType {
    TRANSFER,
    PAYMENT
}
