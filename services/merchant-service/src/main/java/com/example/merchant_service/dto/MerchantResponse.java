package com.example.merchant_service.dto;

import com.example.merchant_service.entity.Merchant;
import com.example.merchant_service.enums.BusinessCategory;
import com.example.merchant_service.enums.MerchantStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MerchantResponse {

    private Long id;
    private String merchantCode;
    private Long ownerUserId;
    private String ownerEmail;
    private String businessName;
    private String businessEmail;
    private String businessPhone;
    private String businessAddress;
    private BusinessCategory businessCategory;
    private String description;
    private String walletNumber;
    private String qrCodeBase64;
    private MerchantStatus status;
    private String statusReason;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    public static MerchantResponse from(Merchant m) {
        return MerchantResponse.builder()
                .id(m.getId())
                .merchantCode(m.getMerchantCode())
                .ownerUserId(m.getOwnerUserId())
                .ownerEmail(m.getOwnerEmail())
                .businessName(m.getBusinessName())
                .businessEmail(m.getBusinessEmail())
                .businessPhone(m.getBusinessPhone())
                .businessAddress(m.getBusinessAddress())
                .businessCategory(m.getBusinessCategory())
                .description(m.getDescription())
                .walletNumber(m.getWalletNumber())
                .qrCodeBase64(m.getQrCodeBase64())
                .status(m.getStatus())
                .statusReason(m.getStatusReason())
                .createdAt(m.getCreatedAt())
                .approvedAt(m.getApprovedAt())
                .build();
    }
}
