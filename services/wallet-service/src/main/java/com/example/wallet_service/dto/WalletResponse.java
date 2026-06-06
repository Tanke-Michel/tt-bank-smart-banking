package com.example.wallet_service.dto;

import com.example.wallet_service.enums.Currency;
import com.example.wallet_service.enums.WalletStatus;
import com.example.wallet_service.entity.Wallet;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WalletResponse {
    private Long id;
    private Long userId;
    private String walletNumber;
    private String ownerName;
    private String email;
    private String phoneNumber;
    private BigDecimal balance;
    private Currency currency;
    private WalletStatus status;
    private LocalDateTime createdAt;

    public static WalletResponse from(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .walletNumber(wallet.getWalletNumber())
                .ownerName(wallet.getOwnerName())
                .email(wallet.getEmail())
                .phoneNumber(wallet.getPhoneNumber())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
}
