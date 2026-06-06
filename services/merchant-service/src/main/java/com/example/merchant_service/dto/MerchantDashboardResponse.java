package com.example.merchant_service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MerchantDashboardResponse {
    private String merchantCode;
    private String businessName;
    private String walletNumber;
    private String qrCodeBase64;
    private BigDecimal todayRevenue;
    private BigDecimal monthRevenue;
    private BigDecimal totalRevenue;
    private long todayTransactionCount;
    private long monthTransactionCount;
    private long totalTransactionCount;
}
