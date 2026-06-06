package com.example.merchant_service.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Subset of wallet-service WalletResponse.
 * Jackson ignores extra fields in the response body.
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
