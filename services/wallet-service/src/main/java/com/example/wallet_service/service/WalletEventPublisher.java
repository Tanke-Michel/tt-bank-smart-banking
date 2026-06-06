package com.example.wallet_service.service;

import com.example.wallet_service.entity.Wallet;
import com.example.wallet_service.entity.WalletTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Publishes wallet domain events to RabbitMQ.
 *
 * Events are consumed by:
 *   - Notification Service : sends emails/SMS for deposits and withdrawals
 *   - Audit Service        : records all balance movements
 *   - Transaction Service  : listens for wallet.created to register wallets
 *
 * Each event is a plain Map<String, Object> serialised as JSON.
 * This avoids shared DTOs between services (loose coupling).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.wallet.created-routing-key}")
    private String walletCreatedKey;

    @Value("${app.rabbitmq.wallet.funded-routing-key}")
    private String walletFundedKey;

    @Value("${app.rabbitmq.wallet.withdrawn-routing-key}")
    private String walletWithdrawnKey;

    // -----------------------------------------------
    // wallet.created — fired when a wallet is first created
    // -----------------------------------------------

    public void publishWalletCreated(Wallet wallet) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType",    "WALLET_CREATED");
        event.put("walletId",     wallet.getId());
        event.put("walletNumber", wallet.getWalletNumber());
        event.put("userId",       wallet.getUserId());
        event.put("ownerName",    wallet.getOwnerName());
        event.put("email",        wallet.getEmail());
        event.put("phoneNumber",  wallet.getPhoneNumber());
        event.put("currency",     wallet.getCurrency().name());
        event.put("timestamp",    LocalDateTime.now().toString());

        publish(walletCreatedKey, event);
    }

    // -----------------------------------------------
    // wallet.funded — fired after a successful deposit
    // -----------------------------------------------

    public void publishWalletFunded(Wallet wallet, WalletTransaction txn) {
        Map<String, Object> event = buildTransactionEvent("WALLET_FUNDED", wallet, txn);
        publish(walletFundedKey, event);
    }

    // -----------------------------------------------
    // wallet.withdrawn — fired after a successful withdrawal
    // -----------------------------------------------

    public void publishWalletWithdrawn(Wallet wallet, WalletTransaction txn) {
        Map<String, Object> event = buildTransactionEvent("WALLET_WITHDRAWN", wallet, txn);
        publish(walletWithdrawnKey, event);
    }

    // -----------------------------------------------
    // Helpers
    // -----------------------------------------------

    private Map<String, Object> buildTransactionEvent(
            String eventType, Wallet wallet, WalletTransaction txn) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType",     eventType);
        event.put("walletId",      wallet.getId());
        event.put("walletNumber",  wallet.getWalletNumber());
        event.put("userId",        wallet.getUserId());
        event.put("ownerName",     wallet.getOwnerName());
        event.put("email",         wallet.getEmail());
        event.put("amount",        txn.getAmount().toPlainString());
        event.put("balanceBefore", txn.getBalanceBefore().toPlainString());
        event.put("balanceAfter",  txn.getBalanceAfter().toPlainString());
        event.put("currency",      txn.getCurrency().name());
        event.put("referenceCode", txn.getReferenceCode());
        event.put("description",   txn.getDescription());
        event.put("timestamp",     LocalDateTime.now().toString());
        return event;
    }

    private void publish(String routingKey, Map<String, Object> event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Event published: routingKey={} event={}", routingKey, event.get("eventType"));
        } catch (Exception e) {
            // Log but never throw — event publication failure must not roll back
            // the wallet transaction. The money movement already happened.
            log.error("Failed to publish event routingKey={}: {}", routingKey, e.getMessage());
        }
    }
}
