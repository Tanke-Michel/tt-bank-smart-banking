package com.example.notification_service.listener;

import com.example.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Listens to merchant domain events and sends email notifications.
 *
 * Events handled:
 *   merchant.registered          → merchant owner gets registration confirmation
 *   merchant.payment.initiated   → no email (avoid noise; customer already initiated)
 *   merchant.payment.completed   → customer gets receipt, merchant gets payment received
 *   merchant.payment.failed      → customer gets failure notification
 *
 * Payload fields mapped from MerchantEventPublisher.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantEventListener {

    private final EmailService emailService;

    @RabbitListener(queues = "merchant.registered.queue")
    public void onMerchantRegistered(Map<String, Object> event) {
        try {
            log.info("Event received: MERCHANT_REGISTERED code={}", event.get("merchantCode"));
            String ownerEmail   = str(event, "ownerEmail");
            String businessName = str(event, "businessName");
            String merchantCode = str(event, "merchantCode");
            if (ownerEmail == null) return;

            // Use ownerEmail prefix as fallback name since payload has no ownerName
            String ownerName = ownerEmail.split("@")[0];
            emailService.sendMerchantRegistered(ownerEmail, ownerName, businessName, merchantCode);
        } catch (Exception e) {
            log.error("Error processing MERCHANT_REGISTERED: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "merchant.payment.initiated.queue")
    public void onMerchantPaymentInitiated(Map<String, Object> event) {
        // No email sent for initiated — avoid double-notifying before completion.
        // Just log for traceability.
        log.debug("Event received: MERCHANT_PAYMENT_INITIATED ref={}", event.get("referenceCode"));
    }

    @RabbitListener(queues = "merchant.payment.completed.queue")
    public void onMerchantPaymentCompleted(Map<String, Object> event) {
        try {
            log.info("Event received: MERCHANT_PAYMENT_COMPLETED ref={}", event.get("referenceCode"));
            String customerEmail = str(event, "customerEmail");
            String businessName  = str(event, "businessName");
            String amount        = str(event, "amount");
            String currency      = str(event, "currency");
            String reference     = str(event, "referenceCode");
            String description   = str(event, "description");

            // Notify the customer with a payment receipt
            if (customerEmail != null) {
                emailService.sendMerchantPaymentReceipt(
                        customerEmail,
                        customerEmail.split("@")[0],
                        businessName, amount, currency, reference, description);
            }

            // Notify the merchant that a payment was received
            // The ownerEmail is not in the payment payload — we use businessName as identifier
            // In a production system the merchant owner email would be in the payload.
            // We log this gap; the merchant can see it in their dashboard.
            log.info("Merchant {} received payment of {} {}", businessName, amount, currency);

        } catch (Exception e) {
            log.error("Error processing MERCHANT_PAYMENT_COMPLETED: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "merchant.payment.failed.queue")
    public void onMerchantPaymentFailed(Map<String, Object> event) {
        try {
            log.info("Event received: MERCHANT_PAYMENT_FAILED ref={}", event.get("referenceCode"));
            String customerEmail = str(event, "customerEmail");
            String businessName  = str(event, "businessName");
            String amount        = str(event, "amount");
            String currency      = str(event, "currency");
            String reference     = str(event, "referenceCode");
            if (customerEmail == null) return;

            emailService.sendMerchantPaymentFailed(
                    customerEmail,
                    customerEmail.split("@")[0],
                    businessName, amount, currency, reference);
        } catch (Exception e) {
            log.error("Error processing MERCHANT_PAYMENT_FAILED: {}", e.getMessage(), e);
        }
    }

    private String str(Map<String, Object> event, String key) {
        Object val = event.get(key);
        return val != null ? val.toString() : null;
    }
}
