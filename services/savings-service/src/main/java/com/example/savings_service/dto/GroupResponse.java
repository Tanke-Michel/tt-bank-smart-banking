package com.example.savings_service.dto;

import com.example.savings_service.entity.SavingsGroup;
import com.example.savings_service.enums.GroupStatus;
import com.example.savings_service.enums.PayoutCycle;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class GroupResponse {

    private Long id;
    private String name;
    private String description;
    private Long creatorUserId;
    private String creatorEmail;
    private BigDecimal contributionAmount;
    private String currency;
    private PayoutCycle payoutCycle;
    private int maxMembers;
    private int currentMemberCount;
    private int currentRound;
    private int totalRounds;
    private GroupStatus status;
    private LocalDate startDate;
    private LocalDateTime createdAt;

    public static GroupResponse from(SavingsGroup g, int memberCount) {
        return GroupResponse.builder()
                .id(g.getId())
                .name(g.getName())
                .description(g.getDescription())
                .creatorUserId(g.getCreatorUserId())
                .creatorEmail(g.getCreatorEmail())
                .contributionAmount(g.getContributionAmount())
                .currency(g.getCurrency())
                .payoutCycle(g.getPayoutCycle())
                .maxMembers(g.getMaxMembers())
                .currentMemberCount(memberCount)
                .currentRound(g.getCurrentRound())
                .totalRounds(g.getMaxMembers())
                .status(g.getStatus())
                .startDate(g.getStartDate())
                .createdAt(g.getCreatedAt())
                .build();
    }
}
