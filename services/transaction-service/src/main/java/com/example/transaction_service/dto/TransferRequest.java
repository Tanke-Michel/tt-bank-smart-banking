package com.example.transaction_service.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {

    /**
     * Recipient identified by their email address.
     * The transaction service resolves this to a wallet number
     * via the wallet service's GET /api/v1/wallet/phone/{phone} endpoint.
     * We use email here since users naturally know each other's emails.
     * Internally we look up the recipient wallet by email via the wallet service.
     */
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Recipient email must be a valid email address")
    private String recipientEmail;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1", message = "Minimum transfer amount is 1")
    @Digits(integer = 15, fraction = 4, message = "Amount must have at most 4 decimal places")
    private BigDecimal amount;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
