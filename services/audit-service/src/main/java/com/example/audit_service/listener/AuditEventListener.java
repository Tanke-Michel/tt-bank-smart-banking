package com.example.audit_service.listener;

import com.example.audit_service.enums.EventDomain;
import com.example.audit_service.enums.EventType;
import com.example.audit_service.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Single listener class consuming all 14 banking domain events
 * and persisting each one as an immutable audit record.
 *
 * Design choices:
 *   - One class (vs four) keeps all audit routing in one place —
 *     simpler to verify completeness against the RabbitMQConfig queues.
 *   - Uses separate *.audit queues so the notification service queues
 *     are not shared. Both services independently receive every event.
 *   - Every @RabbitListener method wraps its body in try/catch(Exception).
 *     If a listener throws, Spring AMQP requeues the message indefinitely.
 *     We never let audit persistence failure block the broker.
 *   - The AuditService.persistEvent() call itself catches serialization
 *     errors and saves a fallback record so no event is ever silently lost.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditService auditService;

    // ================================================================
    // WALLET events
    // ================================================================

    @RabbitListener(queues = "wallet.created.audit")
    public void onWalletCreated(Map<String, Object> event) {
        persist(EventDomain.WALLET, EventType.WALLET_CREATED, event);
    }

    @RabbitListener(queues = "wallet.funded.audit")
    public void onWalletFunded(Map<String, Object> event) {
        persist(EventDomain.WALLET, EventType.WALLET_FUNDED, event);
    }

    @RabbitListener(queues = "wallet.withdrawn.audit")
    public void onWalletWithdrawn(Map<String, Object> event) {
        persist(EventDomain.WALLET, EventType.WALLET_WITHDRAWN, event);
    }

    // ================================================================
    // TRANSACTION events
    // ================================================================

    @RabbitListener(queues = "transaction.initiated.audit")
    public void onTransactionInitiated(Map<String, Object> event) {
        persist(EventDomain.TRANSACTION, EventType.TRANSACTION_INITIATED, event);
    }

    @RabbitListener(queues = "transaction.completed.audit")
    public void onTransactionCompleted(Map<String, Object> event) {
        persist(EventDomain.TRANSACTION, EventType.TRANSACTION_COMPLETED, event);
    }

    @RabbitListener(queues = "transaction.failed.audit")
    public void onTransactionFailed(Map<String, Object> event) {
        persist(EventDomain.TRANSACTION, EventType.TRANSACTION_FAILED, event);
    }

    // ================================================================
    // MERCHANT events
    // ================================================================

    @RabbitListener(queues = "merchant.registered.audit")
    public void onMerchantRegistered(Map<String, Object> event) {
        persist(EventDomain.MERCHANT, EventType.MERCHANT_REGISTERED, event);
    }

    @RabbitListener(queues = "merchant.payment.initiated.audit")
    public void onMerchantPaymentInitiated(Map<String, Object> event) {
        persist(EventDomain.MERCHANT, EventType.MERCHANT_PAYMENT_INITIATED, event);
    }

    @RabbitListener(queues = "merchant.payment.completed.audit")
    public void onMerchantPaymentCompleted(Map<String, Object> event) {
        persist(EventDomain.MERCHANT, EventType.MERCHANT_PAYMENT_COMPLETED, event);
    }

    @RabbitListener(queues = "merchant.payment.failed.audit")
    public void onMerchantPaymentFailed(Map<String, Object> event) {
        persist(EventDomain.MERCHANT, EventType.MERCHANT_PAYMENT_FAILED, event);
    }

    // ================================================================
    // SAVINGS events
    // ================================================================

    @RabbitListener(queues = "savings.group.created.audit")
    public void onSavingsGroupCreated(Map<String, Object> event) {
        persist(EventDomain.SAVINGS, EventType.SAVINGS_GROUP_CREATED, event);
    }

    @RabbitListener(queues = "savings.member.joined.audit")
    public void onSavingsMemberJoined(Map<String, Object> event) {
        persist(EventDomain.SAVINGS, EventType.SAVINGS_MEMBER_JOINED, event);
    }

    @RabbitListener(queues = "savings.contribution.made.audit")
    public void onSavingsContributionMade(Map<String, Object> event) {
        persist(EventDomain.SAVINGS, EventType.SAVINGS_CONTRIBUTION_MADE, event);
    }

    @RabbitListener(queues = "savings.payout.processed.audit")
    public void onSavingsPayoutProcessed(Map<String, Object> event) {
        persist(EventDomain.SAVINGS, EventType.SAVINGS_PAYOUT_PROCESSED, event);
    }

    // ================================================================
    // Shared persist helper — all exceptions caught here
    // ================================================================

    private void persist(EventDomain domain, EventType eventType, Map<String, Object> event) {
        try {
            auditService.persistEvent(domain, eventType, event);
            log.info("Audit persisted: domain={} type={}", domain, eventType);
        } catch (Exception e) {
            log.error("Failed to persist audit event: domain={} type={} error={}",
                    domain, eventType, e.getMessage(), e);
        }
    }
}
