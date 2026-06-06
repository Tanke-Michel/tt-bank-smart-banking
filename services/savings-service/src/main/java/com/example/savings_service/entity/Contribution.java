package com.example.savings_service.entity;

import com.example.savings_service.enums.ContributionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records a single contribution payment by a group member for a specific round.
 *
 * One Contribution row per (group, member, round) combination.
 * Status starts as PENDING and moves to PAID or FAILED after wallet debit.
 *
 * referenceCode: unique idempotency key for the wallet debit call.
 */
@Entity
@Table(
    name = "contributions",
    indexes = {
        @Index(name = "idx_contrib_group",    columnList = "group_id"),
        @Index(name = "idx_contrib_member",   columnList = "member_id"),
        @Index(name = "idx_contrib_round",    columnList = "round_number"),
        @Index(name = "idx_contrib_ref",      columnList = "reference_code", unique = true)
    },
    uniqueConstraints = {
        // Exactly one contribution record per (member, round)
        @UniqueConstraint(name = "uk_contrib_member_round",
                          columnNames = {"member_id", "round_number"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private SavingsGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private GroupMember member;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "wallet_number", nullable = false, length = 30)
    private String walletNumber;

    /** Format: CONT-YYYYMMDD-XXXXXXXX */
    @Column(name = "reference_code", nullable = false, unique = true, length = 35)
    private String referenceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private ContributionStatus status = ContributionStatus.PENDING;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
