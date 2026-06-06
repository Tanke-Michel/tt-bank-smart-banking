package com.example.savings_service.dto;

import com.example.savings_service.entity.Contribution;
import com.example.savings_service.enums.ContributionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ContributionResponse {

    private Long id;
    private Long groupId;
    private String groupName;
    private Long memberId;
    private String memberEmail;
    private int roundNumber;
    private BigDecimal amount;
    private String currency;
    private String walletNumber;
    private String referenceCode;
    private ContributionStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    public static ContributionResponse from(Contribution c) {
        return ContributionResponse.builder()
                .id(c.getId())
                .groupId(c.getGroup().getId())
                .groupName(c.getGroup().getName())
                .memberId(c.getMember().getId())
                .memberEmail(c.getMember().getUserEmail())
                .roundNumber(c.getRoundNumber())
                .amount(c.getAmount())
                .currency(c.getCurrency())
                .walletNumber(c.getWalletNumber())
                .referenceCode(c.getReferenceCode())
                .status(c.getStatus())
                .failureReason(c.getFailureReason())
                .createdAt(c.getCreatedAt())
                .paidAt(c.getPaidAt())
                .build();
    }
}
