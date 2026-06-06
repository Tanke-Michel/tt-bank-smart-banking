package com.example.wallet_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Message payload published to RabbitMQ after every wallet operation.
 * Consumed by: notification-service, audit-service, transaction-service.
 *
 * All fields are Strings or primitives to ensure the message
 * serialises cleanly to JSON without class-path dependencies.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletEventMessage {

    /** e.g. "WALLET_CREATED", "WALLET_FUNDED", "WALLET_WITHDRAWN", "WALLET_FROZEN" */
    private String eventType;

    private Long walletId;
    private Long userId;
    private String userEmail;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String currency;
    private String referenceCode;
    private String description;
    private LocalDateTime occurredAt;
}
