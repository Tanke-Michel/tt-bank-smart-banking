package com.example.transaction_service.dto;

import com.example.transaction_service.entity.Transaction;
import com.example.transaction_service.enums.TransactionStatus;
import com.example.transaction_service.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {

    private Long id;
    private String referenceCode;
    private TransactionType type;
    private TransactionStatus status;

    private String senderWalletNumber;
    private String senderEmail;

    private String receiverWalletNumber;
    private String receiverEmail;

    private BigDecimal amount;
    private String currency;
    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    /** Factory method — converts entity to response DTO */
    public static TransactionResponse from(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .referenceCode(t.getReferenceCode())
                .type(t.getType())
                .status(t.getStatus())
                .senderWalletNumber(t.getSenderWalletNumber())
                .senderEmail(t.getSenderEmail())
                .receiverWalletNumber(t.getReceiverWalletNumber())
                .receiverEmail(t.getReceiverEmail())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .completedAt(t.getCompletedAt())
                .build();
    }
}
