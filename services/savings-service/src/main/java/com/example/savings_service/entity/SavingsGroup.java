package com.example.savings_service.entity;

import com.example.savings_service.enums.GroupStatus;
import com.example.savings_service.enums.PayoutCycle;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A community savings group (Tontine / Njangi).
 *
 * Tontine mechanics:
 *  - N members each contribute a fixed amount every cycle.
 *  - Each cycle, one member receives the total pot (N × contributionAmount).
 *  - Over N cycles every member receives the pot exactly once.
 *  - The order of payouts is determined at group creation (payoutOrder
 *    field on GroupMember).
 *
 * Key design decisions:
 *  - creatorUserId: plain Long — cross-service reference to auth-service user.
 *  - contributionAmount: BigDecimal — never float/double for money.
 *  - currentRound: 1-based; increments after each full contribution + payout cycle.
 *  - maxMembers: enforced in service layer, stored here for display.
 *  - @Version: optimistic locking on round progression.
 */
@Entity
@Table(
    name = "savings_groups",
    indexes = {
        @Index(name = "idx_sg_creator",  columnList = "creator_user_id"),
        @Index(name = "idx_sg_status",   columnList = "status"),
        @Index(name = "idx_sg_name",     columnList = "name")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /** User who created and administers the group */
    @Column(name = "creator_user_id", nullable = false)
    private Long creatorUserId;

    @Column(name = "creator_email", nullable = false)
    private String creatorEmail;

    /**
     * Fixed amount each member contributes per cycle.
     * The payout recipient receives: contributionAmount × memberCount.
     */
    @Column(name = "contribution_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal contributionAmount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "XAF";

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_cycle", nullable = false, length = 15)
    private PayoutCycle payoutCycle;

    /**
     * Maximum number of members allowed.
     * Also determines total number of rounds (one payout per member).
     */
    @Column(name = "max_members", nullable = false)
    private int maxMembers;

    /**
     * Current round number (1-based).
     * Incremented after each payout is completed.
     * When currentRound > maxMembers, the group is COMPLETED.
     */
    @Column(name = "current_round", nullable = false)
    @Builder.Default
    private int currentRound = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private GroupStatus status = GroupStatus.FORMING;

    /** Date the first contribution cycle begins */
    @Column(name = "start_date")
    private LocalDate startDate;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GroupMember> members = new ArrayList<>();

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
}
