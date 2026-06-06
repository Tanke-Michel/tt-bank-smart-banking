package com.example.savings_service.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Subset of wallet-service WalletResponse.
 * Jackson ignores extra fields in the HTTP response body.
 */
@Data
public class WalletInfo {
    private Long id;
    private Long userId;
    private String walletNumber;
    private String ownerName;
    private String email;
    private BigDecimal balance;
    private String currency;
    private String status;
}
