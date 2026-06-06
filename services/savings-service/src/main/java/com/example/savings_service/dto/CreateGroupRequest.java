package com.example.savings_service.dto;

import com.example.savings_service.enums.PayoutCycle;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateGroupRequest {

    @NotBlank(message = "Group name is required")
    @Size(min = 3, max = 100, message = "Group name must be between 3 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Contribution amount is required")
    @DecimalMin(value = "100", message = "Minimum contribution amount is 100")
    @Digits(integer = 15, fraction = 4, message = "Amount must have at most 4 decimal places")
    private BigDecimal contributionAmount;

    @NotNull(message = "Payout cycle is required")
    private PayoutCycle payoutCycle;

    @NotNull(message = "Maximum members is required")
    @Min(value = 2, message = "A group must have at least 2 members")
    @Max(value = 50, message = "A group cannot have more than 50 members")
    private Integer maxMembers;

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    private LocalDate startDate;

    /**
     * The creator's wallet number for contributions and payouts.
     * Creator automatically becomes member with payoutOrder=1.
     */
    @NotBlank(message = "Wallet number is required")
    private String walletNumber;
}
