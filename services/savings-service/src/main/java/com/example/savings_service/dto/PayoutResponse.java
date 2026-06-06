package com.example.savings_service.dto;

import com.example.savings_service.entity.Payout;
import com.example.savings_service.enums.PayoutStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PayoutResponse {

    private Long id;
    private Long groupId;
    private String groupName;
    private Long recipientMemberId;
    private String recipientEmail;
    private int roundNumber;
    private BigDecimal amount;
    private String currency;
    private String recipientWalletNumber;
    private String referenceCode;
    private PayoutStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static PayoutResponse from(Payout p) {
        return PayoutResponse.builder()
                .id(p.getId())
                .groupId(p.getGroup().getId())
                .groupName(p.getGroup().getName())
                .recipientMemberId(p.getRecipientMember().getId())
                .recipientEmail(p.getRecipientMember().getUserEmail())
                .roundNumber(p.getRoundNumber())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .recipientWalletNumber(p.getRecipientWalletNumber())
                .referenceCode(p.getReferenceCode())
                .status(p.getStatus())
                .failureReason(p.getFailureReason())
                .createdAt(p.getCreatedAt())
                .completedAt(p.getCompletedAt())
                .build();
    }
}
