package com.example.transaction_service.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Subset of the wallet-service WalletResponse that the transaction
 * service cares about when resolving sender/receiver wallets.
 * Only the fields we actually need are mapped — extra fields in the
 * JSON response are safely ignored by Jackson.
 */
@Data
public class WalletInfo {
    private Long id;
    private Long userId;
    private String walletNumber;
    private String ownerName;
    private String email;
    private String phoneNumber;
    private BigDecimal balance;
    private String currency;
    private String status;
}
