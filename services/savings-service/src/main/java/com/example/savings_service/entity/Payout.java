package com.example.savings_service.entity;

import com.example.savings_service.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records the pot disbursement to the payout recipient for a given round.
 *
 * One Payout row per (group, round).
 * The payout amount = contributionAmount × activeMembers (who paid this round).
 *
 * referenceCode: unique idempotency key for the wallet credit call.
 */
@Entity
@Table(
    name = "payouts",
    indexes = {
        @Index(name = "idx_payout_group",     columnList = "group_id"),
        @Index(name = "idx_payout_recipient",  columnList = "recipient_member_id"),
        @Index(name = "idx_payout_ref",        columnList = "reference_code", unique = true)
    },
    uniqueConstraints = {
        // Exactly one payout per (group, round)
        @UniqueConstraint(name = "uk_payout_group_round",
                          columnNames = {"group_id", "round_number"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private SavingsGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_member_id", nullable = false)
    private GroupMember recipientMember;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    /** Actual amount paid out (sum of successful contributions this round) */
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "recipient_wallet_number", nullable = false, length = 30)
    private String recipientWalletNumber;

    /** Format: POUT-YYYYMMDD-XXXXXXXX */
    @Column(name = "reference_code", nullable = false, unique = true, length = 35)
    private String referenceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private PayoutStatus status = PayoutStatus.SCHEDULED;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
