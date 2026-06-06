package com.example.merchant_service.dto;

import com.example.merchant_service.enums.MerchantStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminStatusRequest {

    @NotNull(message = "Status is required")
    private MerchantStatus status;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
