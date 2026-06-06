package com.example.merchant_service.entity;

import com.example.merchant_service.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records every QR payment made to a merchant.
 *
 * Key design decisions:
 *  - merchantId: FK to Merchant entity (same service/same DB — JPA relation is fine here).
 *  - customerWalletNumber / merchantWalletNumber: plain strings (wallet-service owns wallets).
 *  - referenceCode: idempotency key; unique constraint prevents duplicate payments.
 *  - amount uses BigDecimal — never float/double for money.
 *  - @Version: optimistic locking for concurrent status updates.
 */
@Entity
@Table(
    name = "merchant_payments",
    indexes = {
        @Index(name = "idx_mpay_reference",    columnList = "reference_code",         unique = true),
        @Index(name = "idx_mpay_merchant_id",  columnList = "merchant_id"),
        @Index(name = "idx_mpay_customer",     columnList = "customer_user_id"),
        @Index(name = "idx_mpay_created_at",   columnList = "created_at")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Format: PAY-YYYYMMDD-XXXXXXXX */
    @Column(name = "reference_code", nullable = false, unique = true, length = 30)
    private String referenceCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "customer_user_id", nullable = false)
    private Long customerUserId;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "customer_wallet_number", nullable = false, length = 30)
    private String customerWalletNumber;

    @Column(name = "merchant_wallet_number", nullable = false, length = 30)
    private String merchantWalletNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
