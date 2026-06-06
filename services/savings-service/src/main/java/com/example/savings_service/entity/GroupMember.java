package com.example.savings_service.entity;

import com.example.savings_service.enums.MemberStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents one member's participation in a savings group.
 *
 * payoutOrder: 1-based position in the payout rotation.
 *   Round 1 → member with payoutOrder=1 receives the pot.
 *   Round 2 → member with payoutOrder=2 receives the pot.
 *   And so on.
 *
 * walletNumber: the member's wallet for contribution debits
 *   and (when it's their turn) payout credits.
 */
@Entity
@Table(
    name = "group_members",
    indexes = {
        @Index(name = "idx_gm_group",     columnList = "group_id"),
        @Index(name = "idx_gm_user",      columnList = "user_id"),
        @Index(name = "idx_gm_wallet",    columnList = "wallet_number")
    },
    uniqueConstraints = {
        // One membership per user per group
        @UniqueConstraint(name = "uk_gm_group_user",
                          columnNames = {"group_id", "user_id"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private SavingsGroup group;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    /** Wallet to debit for contributions and credit for payouts */
    @Column(name = "wallet_number", nullable = false, length = 30)
    private String walletNumber;

    /**
     * 1-based position in the payout rotation.
     * Assigned when the member joins; determines when they receive the pot.
     */
    @Column(name = "payout_order", nullable = false)
    private int payoutOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

    /** True once this member has received their payout */
    @Column(name = "has_received_payout", nullable = false)
    @Builder.Default
    private boolean hasReceivedPayout = false;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }
}
