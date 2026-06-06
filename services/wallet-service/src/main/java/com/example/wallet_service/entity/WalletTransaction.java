package com.example.wallet_service.entity;

import com.example.wallet_service.enums.Currency;
import com.example.wallet_service.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records every balance change on a wallet (deposits and withdrawals).
 *
 * This is distinct from the Transaction Service's Transfer entity which
 * handles peer-to-peer transfers. WalletTransaction is a local audit log
 * of balance movements within this service only.
 */
@Entity
@Table(name = "wallet_transactions", indexes = {
        @Index(name = "idx_wtxn_wallet_id",  columnList = "wallet_id"),
        @Index(name = "idx_wtxn_reference",  columnList = "reference_code", unique = true),
        @Index(name = "idx_wtxn_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TransactionType type;

    /** Amount of this transaction — always positive */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /** Balance BEFORE this transaction was applied */
    @Column(name = "balance_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceBefore;

    /** Balance AFTER this transaction was applied */
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Currency currency;

    /** Unique reference code, e.g. "DEP-20240101-ABCD1234" */
    @Column(name = "reference_code", nullable = false, unique = true, length = 40)
    private String referenceCode;

    /** Human-readable description, e.g. "Top-up via Mobile Money" */
    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
