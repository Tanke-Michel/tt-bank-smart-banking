package com.example.merchant_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MerchantPaymentRequest {

    /**
     * The merchant code decoded from the QR code by the customer's app.
     * Identifies which merchant to pay.
     */
    @NotBlank(message = "Merchant code is required")
    private String merchantCode;

    /**
     * Customer's wallet number (from X-Auth-User-Wallet header).
     * The amount will be debited from this wallet.
     */
    @NotBlank(message = "Customer wallet number is required")
    private String customerWalletNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1", message = "Minimum payment amount is 1")
    @Digits(integer = 15, fraction = 4, message = "Amount must have at most 4 decimal places")
    private BigDecimal amount;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
