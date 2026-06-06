package com.example.transaction_service.service;

import com.example.transaction_service.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Publishes transaction domain events to RabbitMQ.
 *
 * Consumers:
 *   - Notification Service: sends emails for initiated/completed/failed transfers
 *   - Audit Service: records all transfer events for compliance
 *
 * Messages are plain Map<String,Object> serialised as JSON.
 * This avoids shared DTO dependencies between services (loose coupling).
 *
 * All publish errors are caught and logged — never re-thrown.
 * The transaction is already persisted; event failure must never
 * cause a rollback or a 500 error to the client.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.transaction.initiated-routing-key}")
    private String initiatedKey;

    @Value("${app.rabbitmq.transaction.completed-routing-key}")
    private String completedKey;

    @Value("${app.rabbitmq.transaction.failed-routing-key}")
    private String failedKey;

    public void publishInitiated(Transaction txn) {
        publish(initiatedKey, buildEvent("TRANSACTION_INITIATED", txn));
    }

    public void publishCompleted(Transaction txn) {
        publish(completedKey, buildEvent("TRANSACTION_COMPLETED", txn));
    }

    public void publishFailed(Transaction txn) {
        publish(failedKey, buildEvent("TRANSACTION_FAILED", txn));
    }

    // -----------------------------------------------
    // Helpers
    // -----------------------------------------------

    private Map<String, Object> buildEvent(String eventType, Transaction txn) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType",            eventType);
        event.put("transactionId",        txn.getId());
        event.put("referenceCode",        txn.getReferenceCode());
        event.put("type",                 txn.getType().name());
        event.put("status",               txn.getStatus().name());
        event.put("senderUserId",         txn.getSenderUserId());
        event.put("senderEmail",          txn.getSenderEmail());
        event.put("senderWalletNumber",   txn.getSenderWalletNumber());
        event.put("receiverUserId",       txn.getReceiverUserId());
        event.put("receiverEmail",        txn.getReceiverEmail());
        event.put("receiverWalletNumber", txn.getReceiverWalletNumber());
        event.put("amount",               txn.getAmount().toPlainString());
        event.put("currency",             txn.getCurrency());
        event.put("description",          txn.getDescription());
        event.put("failureReason",        txn.getFailureReason());
        event.put("timestamp",            LocalDateTime.now().toString());
        return event;
    }

    private void publish(String routingKey, Map<String, Object> event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Event published: key={} ref={}",
                    routingKey, event.get("referenceCode"));
        } catch (Exception e) {
            log.error("Failed to publish event key={} ref={}: {}",
                    routingKey, event.get("referenceCode"), e.getMessage());
        }
    }
}
