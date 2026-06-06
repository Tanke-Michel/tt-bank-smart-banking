package com.example.notification_service.listener;

import com.example.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Listens to peer-to-peer transfer events and sends email notifications.
 *
 * Events handled:
 *   transaction.initiated → sender gets "transfer in progress" email
 *   transaction.completed → sender gets "sent" email, receiver gets "received" email
 *   transaction.failed    → sender gets "transfer failed" email
 *
 * Payload fields are mapped from TransactionEventPublisher.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventListener {

    private final EmailService emailService;

    @RabbitListener(queues = "transaction.initiated.queue")
    public void onTransactionInitiated(Map<String, Object> event) {
        try {
            log.info("Event received: TRANSACTION_INITIATED ref={}", event.get("referenceCode"));
            String senderEmail = str(event, "senderEmail");
            if (senderEmail == null) return;

            emailService.sendTransferInitiated(
                    senderEmail,
                    senderEmail.split("@")[0],   // fallback name from email prefix
                    str(event, "amount"),
                    str(event, "currency"),
                    str(event, "receiverEmail"),
                    str(event, "referenceCode"));
        } catch (Exception e) {
            log.error("Error processing TRANSACTION_INITIATED: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "transaction.completed.queue")
    public void onTransactionCompleted(Map<String, Object> event) {
        try {
            log.info("Event received: TRANSACTION_COMPLETED ref={}", event.get("referenceCode"));
            String senderEmail   = str(event, "senderEmail");
            String receiverEmail = str(event, "receiverEmail");
            String amount        = str(event, "amount");
            String currency      = str(event, "currency");
            String reference     = str(event, "referenceCode");
            String description   = str(event, "description");

            // Notify sender
            if (senderEmail != null) {
                emailService.sendTransferCompletedSender(
                        senderEmail,
                        senderEmail.split("@")[0],
                        amount, currency, receiverEmail, reference, description);
            }
            // Notify receiver
            if (receiverEmail != null) {
                emailService.sendTransferCompletedReceiver(
                        receiverEmail,
                        receiverEmail.split("@")[0],
                        amount, currency, senderEmail, reference, description);
            }
        } catch (Exception e) {
            log.error("Error processing TRANSACTION_COMPLETED: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "transaction.failed.queue")
    public void onTransactionFailed(Map<String, Object> event) {
        try {
            log.info("Event received: TRANSACTION_FAILED ref={}", event.get("referenceCode"));
            String senderEmail = str(event, "senderEmail");
            if (senderEmail == null) return;

            emailService.sendTransferFailed(
                    senderEmail,
                    senderEmail.split("@")[0],
                    str(event, "amount"),
                    str(event, "currency"),
                    str(event, "receiverEmail"),
                    str(event, "referenceCode"));
        } catch (Exception e) {
            log.error("Error processing TRANSACTION_FAILED: {}", e.getMessage(), e);
        }
    }

    private String str(Map<String, Object> event, String key) {
        Object val = event.get(key);
        return val != null ? val.toString() : null;
    }
}
