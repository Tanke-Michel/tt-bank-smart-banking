package com.example.wallet_service.entity;

import com.example.wallet_service.enums.Currency;
import com.example.wallet_service.enums.WalletStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The Wallet entity represents a user's digital wallet.
 *
 * Key design decisions:
 *  - userId is a Long (foreign key by value, not JPA relation) because
 *    the User lives in a different service/database. Cross-service JPA
 *    relations are an anti-pattern in microservices.
 *  - balance uses BigDecimal — NEVER use float/double for money.
 *  - walletNumber is a human-readable unique identifier (not the PK).
 *  - version enables optimistic locking to prevent concurrent balance
 *    corruption when two transactions hit the same wallet simultaneously.
 */
@Entity
@Table(name = "wallets", indexes = {
        @Index(name = "idx_wallet_user_id",     columnList = "user_id"),
        @Index(name = "idx_wallet_number",      columnList = "wallet_number", unique = true),
        @Index(name = "idx_wallet_phone",       columnList = "phone_number",  unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Links this wallet to the user in the auth-service by their DB id. */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** Human-readable wallet number, e.g. "WLT-20240101-000042" */
    @Column(name = "wallet_number", nullable = false, unique = true, length = 30)
    private String walletNumber;

    /** Wallet owner's full name — denormalised for display without cross-service calls */
    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    /** Wallet owner's email — used for notifications and lookups */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /** Phone number for QR / peer-to-peer transfers */
    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    /**
     * Current balance. NEVER modified directly — always via deposit() / withdraw()
     * service methods that apply business rule validation.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Currency currency = Currency.XAF;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;

    /**
     * Optimistic locking version — JPA increments this on every UPDATE.
     * If two concurrent transactions read version=5 and both try to write,
     * the second will see a stale version and throw OptimisticLockException.
     */
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt  = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Guard: throws if wallet is not ACTIVE before any money movement. */
    public void assertActive() {
        if (status != WalletStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Wallet " + walletNumber + " is not active (status=" + status + ")");
        }
    }
}
