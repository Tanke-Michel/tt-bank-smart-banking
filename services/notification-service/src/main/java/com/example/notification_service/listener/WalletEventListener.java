package com.example.notification_service.listener;

import com.example.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Listens to wallet domain events and dispatches email notifications.
 *
 * Events handled:
 *   wallet.created   → welcome email with wallet number
 *   wallet.funded    → deposit confirmation
 *   wallet.withdrawn → withdrawal confirmation
 *
 * Each @RabbitListener runs on a dedicated Spring-managed thread from the
 * SimpleRabbitListenerContainerFactory thread pool. Exceptions must NOT
 * propagate — doing so causes RabbitMQ to re-queue the message indefinitely.
 * All exceptions are caught and logged here.
 *
 * Payload fields are extracted from Map<String, Object> — the same format
 * used by WalletEventPublisher.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletEventListener {

    private final EmailService emailService;

    @RabbitListener(queues = "wallet.created.queue")
    public void onWalletCreated(Map<String, Object> event) {
        try {
            log.info("Event received: WALLET_CREATED wallet={}", event.get("walletNumber"));
            String email      = str(event, "email");
            String ownerName  = str(event, "ownerName");
            String walletNum  = str(event, "walletNumber");
            String currency   = str(event, "currency");

            if (email != null) {
                emailService.sendWalletCreated(email, ownerName, walletNum, currency);
            }
        } catch (Exception e) {
            log.error("Error processing WALLET_CREATED event: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "wallet.funded.queue")
    public void onWalletFunded(Map<String, Object> event) {
        try {
            log.info("Event received: WALLET_FUNDED ref={}", event.get("referenceCode"));
            String email        = str(event, "email");
            String ownerName    = str(event, "ownerName");
            String amount       = str(event, "amount");
            String currency     = str(event, "currency");
            String balanceAfter = str(event, "balanceAfter");
            String reference    = str(event, "referenceCode");
            String description  = str(event, "description");

            if (email != null) {
                emailService.sendDepositConfirmation(email, ownerName, amount, currency,
                        balanceAfter, reference, description);
            }
        } catch (Exception e) {
            log.error("Error processing WALLET_FUNDED event: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "wallet.withdrawn.queue")
    public void onWalletWithdrawn(Map<String, Object> event) {
        try {
            log.info("Event received: WALLET_WITHDRAWN ref={}", event.get("referenceCode"));
            String email        = str(event, "email");
            String ownerName    = str(event, "ownerName");
            String amount       = str(event, "amount");
            String currency     = str(event, "currency");
            String balanceAfter = str(event, "balanceAfter");
            String reference    = str(event, "referenceCode");
            String description  = str(event, "description");

            if (email != null) {
                emailService.sendWithdrawalConfirmation(email, ownerName, amount, currency,
                        balanceAfter, reference, description);
            }
        } catch (Exception e) {
            log.error("Error processing WALLET_WITHDRAWN event: {}", e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // Helper: safely extract String from the event Map
    // ------------------------------------------------------------------
    private String str(Map<String, Object> event, String key) {
        Object val = event.get(key);
        return val != null ? val.toString() : null;
    }
}
