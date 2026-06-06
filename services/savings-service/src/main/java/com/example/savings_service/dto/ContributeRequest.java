package com.example.savings_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContributeRequest {

    /**
     * Member's wallet number to debit.
     * Must match the wallet registered when the member joined.
     */
    @NotBlank(message = "Wallet number is required")
    private String walletNumber;
}
