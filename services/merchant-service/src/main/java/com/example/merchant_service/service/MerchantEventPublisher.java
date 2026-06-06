package com.example.merchant_service.service;

import com.example.merchant_service.entity.Merchant;
import com.example.merchant_service.entity.MerchantPayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Publishes merchant domain events to RabbitMQ.
 *
 * Consumers:
 *   - Notification Service: payment receipts, registration notifications
 *   - Audit Service: compliance logging
 *
 * All publish errors are caught and logged — never rethrown.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.merchant.payment-initiated-routing-key}")
    private String paymentInitiatedKey;

    @Value("${app.rabbitmq.merchant.payment-completed-routing-key}")
    private String paymentCompletedKey;

    @Value("${app.rabbitmq.merchant.payment-failed-routing-key}")
    private String paymentFailedKey;

    @Value("${app.rabbitmq.merchant.registered-routing-key}")
    private String merchantRegisteredKey;

    public void publishMerchantRegistered(Merchant merchant) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType",     "MERCHANT_REGISTERED");
        event.put("merchantId",    merchant.getId());
        event.put("merchantCode",  merchant.getMerchantCode());
        event.put("businessName",  merchant.getBusinessName());
        event.put("ownerEmail",    merchant.getOwnerEmail());
        event.put("status",        merchant.getStatus().name());
        event.put("timestamp",     LocalDateTime.now().toString());
        publish(merchantRegisteredKey, event);
    }

    public void publishPaymentInitiated(MerchantPayment payment) {
        publish(paymentInitiatedKey, buildPaymentEvent("MERCHANT_PAYMENT_INITIATED", payment));
    }

    public void publishPaymentCompleted(MerchantPayment payment) {
        publish(paymentCompletedKey, buildPaymentEvent("MERCHANT_PAYMENT_COMPLETED", payment));
    }

    public void publishPaymentFailed(MerchantPayment payment) {
        publish(paymentFailedKey, buildPaymentEvent("MERCHANT_PAYMENT_FAILED", payment));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Map<String, Object> buildPaymentEvent(String eventType, MerchantPayment p) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType",              eventType);
        event.put("paymentId",              p.getId());
        event.put("referenceCode",          p.getReferenceCode());
        event.put("merchantId",             p.getMerchant().getId());
        event.put("merchantCode",           p.getMerchant().getMerchantCode());
        event.put("businessName",           p.getMerchant().getBusinessName());
        event.put("customerUserId",         p.getCustomerUserId());
        event.put("customerEmail",          p.getCustomerEmail());
        event.put("customerWalletNumber",   p.getCustomerWalletNumber());
        event.put("merchantWalletNumber",   p.getMerchantWalletNumber());
        event.put("amount",                 p.getAmount().toPlainString());
        event.put("currency",               p.getCurrency());
        event.put("description",            p.getDescription());
        event.put("status",                 p.getStatus().name());
        event.put("failureReason",          p.getFailureReason());
        event.put("timestamp",              LocalDateTime.now().toString());
        return event;
    }

    private void publish(String routingKey, Map<String, Object> event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Event published: key={} type={}", routingKey, event.get("eventType"));
        } catch (Exception e) {
            log.error("Failed to publish event key={}: {}", routingKey, e.getMessage());
        }
    }
}
