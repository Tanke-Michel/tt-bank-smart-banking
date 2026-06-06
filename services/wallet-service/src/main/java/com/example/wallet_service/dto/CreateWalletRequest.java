package com.example.wallet_service.dto;

import com.example.wallet_service.enums.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateWalletRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Phone number must be valid")
    private String phoneNumber;

    @NotNull(message = "Currency is required")
    private Currency currency;
}
