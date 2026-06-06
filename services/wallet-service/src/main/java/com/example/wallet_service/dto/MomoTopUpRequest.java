package com.example.wallet_service.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MomoTopUpRequest {
    @NotNull @DecimalMin("1.0")
    private BigDecimal amount;

    @NotBlank
    private String phoneNumber;

    private String description;
}
