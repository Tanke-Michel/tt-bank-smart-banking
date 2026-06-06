package com.example.merchant_service.dto;

import com.example.merchant_service.entity.MerchantPayment;
import com.example.merchant_service.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MerchantPaymentResponse {

    private Long id;
    private String referenceCode;
    private Long merchantId;
    private String merchantCode;
    private String businessName;
    private Long customerUserId;
    private String customerEmail;
    private String customerWalletNumber;
    private String merchantWalletNumber;
    private BigDecimal amount;
    private String currency;
    private String description;
    private PaymentStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static MerchantPaymentResponse from(MerchantPayment p) {
        return MerchantPaymentResponse.builder()
                .id(p.getId())
                .referenceCode(p.getReferenceCode())
                .merchantId(p.getMerchant().getId())
                .merchantCode(p.getMerchant().getMerchantCode())
                .businessName(p.getMerchant().getBusinessName())
                .customerUserId(p.getCustomerUserId())
                .customerEmail(p.getCustomerEmail())
                .customerWalletNumber(p.getCustomerWalletNumber())
                .merchantWalletNumber(p.getMerchantWalletNumber())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .description(p.getDescription())
                .status(p.getStatus())
                .failureReason(p.getFailureReason())
                .createdAt(p.getCreatedAt())
                .completedAt(p.getCompletedAt())
                .build();
    }
}
