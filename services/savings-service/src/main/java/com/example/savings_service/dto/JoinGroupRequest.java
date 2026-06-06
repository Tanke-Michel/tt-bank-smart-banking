package com.example.savings_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinGroupRequest {

    /**
     * Member's wallet number for contribution debits and (if their turn) payout credit.
     */
    @NotBlank(message = "Wallet number is required")
    private String walletNumber;
}
