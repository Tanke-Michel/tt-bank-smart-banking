package com.example.wallet_service.dto;

import com.example.wallet_service.entity.WalletTransaction;
import com.example.wallet_service.enums.Currency;
import com.example.wallet_service.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WalletTransactionResponse {
    private Long id;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private Currency currency;
    private String referenceCode;
    private String description;
    private LocalDateTime createdAt;

    public static WalletTransactionResponse from(WalletTransaction txn) {
        return WalletTransactionResponse.builder()
                .id(txn.getId())
                .type(txn.getType())
                .amount(txn.getAmount())
                .balanceBefore(txn.getBalanceBefore())
                .balanceAfter(txn.getBalanceAfter())
                .currency(txn.getCurrency())
                .referenceCode(txn.getReferenceCode())
                .description(txn.getDescription())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
