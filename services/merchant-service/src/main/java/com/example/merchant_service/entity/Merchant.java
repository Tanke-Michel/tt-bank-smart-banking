package com.example.merchant_service.entity;

import com.example.merchant_service.enums.BusinessCategory;
import com.example.merchant_service.enums.MerchantStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A registered merchant who can accept QR-code payments.
 *
 * Key design decisions:
 *  - ownerUserId: plain Long — links to the user in auth-service by database ID.
 *    No JPA cross-service relation.
 *  - walletNumber: the merchant's receiving wallet (from wallet-service).
 *    Stored as a denormalised string — avoids cross-service joins.
 *  - merchantCode: unique, human-readable code (e.g. "MCH-20240101-ABC123").
 *    Used as the QR code payload — a customer's app decodes this and
 *    calls POST /api/v1/merchants/{merchantCode}/pay.
 *  - qrCodeBase64: the PNG QR code image stored as a Base64 string.
 *    Regenerated on demand if null; cached in this column.
 *  - status: PENDING by default — must be approved by an ADMIN.
 */
@Entity
@Table(
    name = "merchants",
    indexes = {
        @Index(name = "idx_merchant_code",         columnList = "merchant_code",   unique = true),
        @Index(name = "idx_merchant_owner",        columnList = "owner_user_id"),
        @Index(name = "idx_merchant_email",        columnList = "business_email",  unique = true),
        @Index(name = "idx_merchant_wallet",       columnList = "wallet_number")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Format: MCH-YYYYMMDD-XXXXXXXX */
    @Column(name = "merchant_code", nullable = false, unique = true, length = 30)
    private String merchantCode;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "owner_email", nullable = false)
    private String ownerEmail;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "business_email", nullable = false, unique = true)
    private String businessEmail;

    @Column(name = "business_phone", nullable = false)
    private String businessPhone;

    @Column(name = "business_address", nullable = false)
    private String businessAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_category", nullable = false, length = 30)
    private BusinessCategory businessCategory;

    @Column(name = "description", length = 500)
    private String description;

    /**
     * The merchant's receiving wallet number (from wallet-service).
     * All customer payments are credited to this wallet.
     */
    @Column(name = "wallet_number", nullable = false, length = 30)
    private String walletNumber;

    /**
     * Base64-encoded PNG of the QR code.
     * Encoded payload = merchantCode.
     * Stored here so we don't regenerate it on every request.
     */
    @Column(name = "qr_code_base64", columnDefinition = "TEXT")
    private String qrCodeBase64;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private MerchantStatus status = MerchantStatus.PENDING;

    /** Reason supplied by admin when rejecting or suspending */
    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt  = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
