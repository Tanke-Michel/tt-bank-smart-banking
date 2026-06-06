package com.example.transaction_service.entity;

import com.example.transaction_service.enums.TransactionStatus;
import com.example.transaction_service.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records every peer-to-peer transfer.
 *
 * Key design decisions:
 *  - senderWalletNumber / receiverWalletNumber: strings, not FK relations.
 *    Wallets live in a different service/database. Cross-service JPA
 *    relations are an anti-pattern in microservices.
 *  - amount uses BigDecimal — NEVER float/double for money.
 *  - referenceCode is the idempotency key; unique constraint prevents
 *    duplicate transfers if the client retries.
 *  - failureReason: populated only when status = FAILED.
 *  - version: optimistic locking for concurrent status updates.
 */
@Entity
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_txn_reference",          columnList = "reference_code",      unique = true),
        @Index(name = "idx_txn_sender_wallet",      columnList = "sender_wallet_number"),
        @Index(name = "idx_txn_receiver_wallet",    columnList = "receiver_wallet_number"),
        @Index(name = "idx_txn_sender_user",        columnList = "sender_user_id"),
        @Index(name = "idx_txn_created_at",         columnList = "created_at")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique, human-readable reference code.
     * Format: TXN-YYYYMMDD-XXXXXXXX (8 uppercase hex chars).
     * Used as idempotency key and for customer support.
     */
    @Column(name = "reference_code", nullable = false, unique = true, length = 30)
    private String referenceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private TransactionType type = TransactionType.TRANSFER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    /** Sender's wallet number (from wallet-service) */
    @Column(name = "sender_wallet_number", nullable = false, length = 30)
    private String senderWalletNumber;

    /** Sender's user ID (from JWT claim) */
    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    /** Sender's email — denormalised for notifications/display */
    @Column(name = "sender_email", nullable = false)
    private String senderEmail;

    /** Receiver's wallet number */
    @Column(name = "receiver_wallet_number", nullable = false, length = 30)
    private String receiverWalletNumber;

    /** Receiver's user ID */
    @Column(name = "receiver_user_id", nullable = false)
    private Long receiverUserId;

    /** Receiver's email — denormalised for notifications/display */
    @Column(name = "receiver_email", nullable = false)
    private String receiverEmail;

    /** Transfer amount — always positive */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /** ISO currency code matching sender's wallet currency */
    @Column(nullable = false, length = 10)
    private String currency;

    /** Optional note from sender, e.g. "Rent for November" */
    @Column(length = 255)
    private String description;

    /**
     * Populated when status = FAILED.
     * Never shown to users verbatim — used for support/audit.
     */
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
