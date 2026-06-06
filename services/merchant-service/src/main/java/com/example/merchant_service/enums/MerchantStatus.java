package com.example.merchant_service.enums;

/**
 * Lifecycle of a merchant account.
 *
 * PENDING   — registered, awaiting admin approval.
 * ACTIVE    — approved, can accept payments and generate QR codes.
 * SUSPENDED — temporarily blocked by admin; cannot accept payments.
 * REJECTED  — registration rejected by admin.
 */
public enum MerchantStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    REJECTED
}
