package com.example.savings_service.service;

import com.example.savings_service.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Publishes savings domain events to RabbitMQ.
 *
 * Consumers:
 *   - Notification Service: contribution receipts, payout notifications, join confirmations
 *   - Audit Service: compliance logging for financial group activity
 *
 * All errors caught and logged — never rethrown.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.savings.group-created-routing-key}")
    private String groupCreatedKey;

    @Value("${app.rabbitmq.savings.contribution-made-routing-key}")
    private String contributionMadeKey;

    @Value("${app.rabbitmq.savings.payout-processed-routing-key}")
    private String payoutProcessedKey;

    @Value("${app.rabbitmq.savings.member-joined-routing-key}")
    private String memberJoinedKey;

    public void publishGroupCreated(SavingsGroup group) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType",           "SAVINGS_GROUP_CREATED");
        event.put("groupId",             group.getId());
        event.put("groupName",           group.getName());
        event.put("creatorUserId",       group.getCreatorUserId());
        event.put("creatorEmail",        group.getCreatorEmail());
        event.put("contributionAmount",  group.getContributionAmount().toPlainString());
        event.put("currency",            group.getCurrency());
        event.put("payoutCycle",         group.getPayoutCycle().name());
        event.put("maxMembers",          group.getMaxMembers());
        event.put("startDate",           group.getStartDate() != null ? group.getStartDate().toString() : null);
        event.put("timestamp",           LocalDateTime.now().toString());
        publish(groupCreatedKey, event);
    }

    public void publishMemberJoined(SavingsGroup group, GroupMember member) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType",    "SAVINGS_MEMBER_JOINED");
        event.put("groupId",      group.getId());
        event.put("groupName",    group.getName());
        event.put("memberId",     member.getId());
        event.put("userId",       member.getUserId());
        event.put("userEmail",    member.getUserEmail());
        event.put("payoutOrder",  member.getPayoutOrder());
        event.put("timestamp",    LocalDateTime.now().toString());
        publish(memberJoinedKey, event);
    }

    public void publishContributionMade(Contribution contribution) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType",      "SAVINGS_CONTRIBUTION_MADE");
        event.put("contributionId", contribution.getId());
        event.put("groupId",        contribution.getGroup().getId());
        event.put("groupName",      contribution.getGroup().getName());
        event.put("memberId",       contribution.getMember().getId());
        event.put("userEmail",      contribution.getMember().getUserEmail());
        event.put("roundNumber",    contribution.getRoundNumber());
        event.put("amount",         contribution.getAmount().toPlainString());
        event.put("currency",       contribution.getCurrency());
        event.put("referenceCode",  contribution.getReferenceCode());
        event.put("status",         contribution.getStatus().name());
        event.put("timestamp",      LocalDateTime.now().toString());
        publish(contributionMadeKey, event);
    }

    public void publishPayoutProcessed(Payout payout) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType",       "SAVINGS_PAYOUT_PROCESSED");
        event.put("payoutId",        payout.getId());
        event.put("groupId",         payout.getGroup().getId());
        event.put("groupName",       payout.getGroup().getName());
        event.put("recipientId",     payout.getRecipientMember().getId());
        event.put("recipientEmail",  payout.getRecipientMember().getUserEmail());
        event.put("roundNumber",     payout.getRoundNumber());
        event.put("amount",          payout.getAmount().toPlainString());
        event.put("currency",        payout.getCurrency());
        event.put("referenceCode",   payout.getReferenceCode());
        event.put("status",          payout.getStatus().name());
        event.put("timestamp",       LocalDateTime.now().toString());
        publish(payoutProcessedKey, event);
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
