package com.example.notification_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Notification Service — subscribes to all banking events over RabbitMQ
 * and sends transactional HTML emails to users.
 *
 * Events consumed (14 total):
 *   Wallet   : wallet.created, wallet.funded, wallet.withdrawn
 *   Transfer : transaction.initiated, transaction.completed, transaction.failed
 *   Merchant : merchant.registered, merchant.payment.initiated,
 *               merchant.payment.completed, merchant.payment.failed
 *   Savings  : savings.group.created, savings.member.joined,
 *               savings.contribution.made, savings.payout.processed
 *
 * @EnableAsync activates @Async so email sending never blocks listener threads.
 */
@SpringBootApplication
@EnableAsync
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
