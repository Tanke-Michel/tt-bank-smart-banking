package com.example.notification_service.listener;

import com.example.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Listens to savings/tontine domain events and sends email notifications.
 *
 * Events handled:
 *   savings.group.created    → creator gets group creation confirmation
 *   savings.member.joined    → new member gets joining confirmation
 *   savings.contribution.made → member gets contribution receipt (paid or failed)
 *   savings.payout.processed  → recipient gets payout notification
 *
 * Payload fields mapped from SavingsEventPublisher.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SavingsEventListener {

    private final EmailService emailService;

    @RabbitListener(queues = "savings.group.created.queue")
    public void onGroupCreated(Map<String, Object> event) {
        try {
            log.info("Event received: SAVINGS_GROUP_CREATED group={}", event.get("groupName"));
            String creatorEmail        = str(event, "creatorEmail");
            String groupName           = str(event, "groupName");
            String contributionAmount  = str(event, "contributionAmount");
            String currency            = str(event, "currency");
            String payoutCycle         = str(event, "payoutCycle");
            String startDate           = str(event, "startDate");
            if (creatorEmail == null) return;

            emailService.sendSavingsGroupCreated(
                    creatorEmail,
                    creatorEmail.split("@")[0],
                    groupName, contributionAmount, currency, payoutCycle,
                    startDate != null ? startDate : "TBD");
        } catch (Exception e) {
            log.error("Error processing SAVINGS_GROUP_CREATED: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "savings.member.joined.queue")
    public void onMemberJoined(Map<String, Object> event) {
        try {
            log.info("Event received: SAVINGS_MEMBER_JOINED group={} user={}",
                    event.get("groupName"), event.get("userEmail"));
            String userEmail  = str(event, "userEmail");
            String groupName  = str(event, "groupName");
            Object orderObj   = event.get("payoutOrder");
            if (userEmail == null || orderObj == null) return;

            int payoutOrder = Integer.parseInt(orderObj.toString());
            emailService.sendSavingsMemberJoined(
                    userEmail,
                    userEmail.split("@")[0],
                    groupName, payoutOrder);
        } catch (Exception e) {
            log.error("Error processing SAVINGS_MEMBER_JOINED: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "savings.contribution.made.queue")
    public void onContributionMade(Map<String, Object> event) {
        try {
            log.info("Event received: SAVINGS_CONTRIBUTION_MADE ref={}", event.get("referenceCode"));
            String userEmail    = str(event, "userEmail");
            String groupName    = str(event, "groupName");
            Object roundObj     = event.get("roundNumber");
            String amount       = str(event, "amount");
            String currency     = str(event, "currency");
            String reference    = str(event, "referenceCode");
            String status       = str(event, "status");
            if (userEmail == null || roundObj == null) return;

            int roundNumber = Integer.parseInt(roundObj.toString());
            boolean paid    = "PAID".equals(status);

            emailService.sendContributionConfirmation(
                    userEmail,
                    userEmail.split("@")[0],
                    groupName, roundNumber, amount, currency, reference, paid);
        } catch (Exception e) {
            log.error("Error processing SAVINGS_CONTRIBUTION_MADE: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "savings.payout.processed.queue")
    public void onPayoutProcessed(Map<String, Object> event) {
        try {
            log.info("Event received: SAVINGS_PAYOUT_PROCESSED ref={}", event.get("referenceCode"));
            String recipientEmail = str(event, "recipientEmail");
            String groupName      = str(event, "groupName");
            Object roundObj       = event.get("roundNumber");
            String amount         = str(event, "amount");
            String currency       = str(event, "currency");
            String reference      = str(event, "referenceCode");
            String status         = str(event, "status");
            if (recipientEmail == null || roundObj == null) return;

            int roundNumber  = Integer.parseInt(roundObj.toString());
            boolean completed = "COMPLETED".equals(status);

            emailService.sendPayoutNotification(
                    recipientEmail,
                    recipientEmail.split("@")[0],
                    groupName, roundNumber, amount, currency, reference, completed);
        } catch (Exception e) {
            log.error("Error processing SAVINGS_PAYOUT_PROCESSED: {}", e.getMessage(), e);
        }
    }

    private String str(Map<String, Object> event, String key) {
        Object val = event.get(key);
        return val != null ? val.toString() : null;
    }
}
