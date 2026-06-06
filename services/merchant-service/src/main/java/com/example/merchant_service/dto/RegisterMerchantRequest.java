package com.example.merchant_service.dto;

import com.example.merchant_service.enums.BusinessCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterMerchantRequest {

    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 100, message = "Business name must be between 2 and 100 characters")
    private String businessName;

    @NotBlank(message = "Business email is required")
    @Email(message = "Business email must be valid")
    private String businessEmail;

    @NotBlank(message = "Business phone is required")
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Business phone must be valid")
    private String businessPhone;

    @NotBlank(message = "Business address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String businessAddress;

    @NotNull(message = "Business category is required")
    private BusinessCategory businessCategory;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * The merchant's wallet number for receiving payments.
     * The merchant must already have a wallet created via the wallet-service.
     */
    @NotBlank(message = "Wallet number is required")
    private String walletNumber;
}
