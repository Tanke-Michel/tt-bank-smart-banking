package com.example.audit_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for the Audit Service.
 *
 * The audit service is a PURE CONSUMER — it never publishes events.
 * It uses SEPARATE queues from the notification service so that both
 * services independently receive every event.
 *
 * Each queue is named with an ".audit" suffix to distinguish from the
 * notification service queues. Both sets of queues are bound to the
 * same smart-banking.exchange with the same routing keys.
 *
 * 14 audit queues total:
 *   Wallet (3): wallet.created.audit, wallet.funded.audit, wallet.withdrawn.audit
 *   Transaction (3): transaction.initiated.audit, transaction.completed.audit,
 *                    transaction.failed.audit
 *   Merchant (4): merchant.registered.audit, merchant.payment.initiated.audit,
 *                 merchant.payment.completed.audit, merchant.payment.failed.audit
 *   Savings (4): savings.group.created.audit, savings.member.joined.audit,
 *                savings.contribution.made.audit, savings.payout.processed.audit
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Bean
    public TopicExchange smartBankingExchange() {
        return ExchangeBuilder.topicExchange(exchangeName).durable(true).build();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    // ================================================================
    // WALLET audit queues
    // ================================================================
    @Bean public Queue walletCreatedAuditQueue() {
        return QueueBuilder.durable("wallet.created.audit").build();
    }
    @Bean public Queue walletFundedAuditQueue() {
        return QueueBuilder.durable("wallet.funded.audit").build();
    }
    @Bean public Queue walletWithdrawnAuditQueue() {
        return QueueBuilder.durable("wallet.withdrawn.audit").build();
    }
    @Bean public Binding walletCreatedAuditBinding() {
        return BindingBuilder.bind(walletCreatedAuditQueue())
                .to(smartBankingExchange()).with("wallet.created");
    }
    @Bean public Binding walletFundedAuditBinding() {
        return BindingBuilder.bind(walletFundedAuditQueue())
                .to(smartBankingExchange()).with("wallet.funded");
    }
    @Bean public Binding walletWithdrawnAuditBinding() {
        return BindingBuilder.bind(walletWithdrawnAuditQueue())
                .to(smartBankingExchange()).with("wallet.withdrawn");
    }

    // ================================================================
    // TRANSACTION audit queues
    // ================================================================
    @Bean public Queue transactionInitiatedAuditQueue() {
        return QueueBuilder.durable("transaction.initiated.audit").build();
    }
    @Bean public Queue transactionCompletedAuditQueue() {
        return QueueBuilder.durable("transaction.completed.audit").build();
    }
    @Bean public Queue transactionFailedAuditQueue() {
        return QueueBuilder.durable("transaction.failed.audit").build();
    }
    @Bean public Binding transactionInitiatedAuditBinding() {
        return BindingBuilder.bind(transactionInitiatedAuditQueue())
                .to(smartBankingExchange()).with("transaction.initiated");
    }
    @Bean public Binding transactionCompletedAuditBinding() {
        return BindingBuilder.bind(transactionCompletedAuditQueue())
                .to(smartBankingExchange()).with("transaction.completed");
    }
    @Bean public Binding transactionFailedAuditBinding() {
        return BindingBuilder.bind(transactionFailedAuditQueue())
                .to(smartBankingExchange()).with("transaction.failed");
    }

    // ================================================================
    // MERCHANT audit queues
    // ================================================================
    @Bean public Queue merchantRegisteredAuditQueue() {
        return QueueBuilder.durable("merchant.registered.audit").build();
    }
    @Bean public Queue merchantPaymentInitiatedAuditQueue() {
        return QueueBuilder.durable("merchant.payment.initiated.audit").build();
    }
    @Bean public Queue merchantPaymentCompletedAuditQueue() {
        return QueueBuilder.durable("merchant.payment.completed.audit").build();
    }
    @Bean public Queue merchantPaymentFailedAuditQueue() {
        return QueueBuilder.durable("merchant.payment.failed.audit").build();
    }
    @Bean public Binding merchantRegisteredAuditBinding() {
        return BindingBuilder.bind(merchantRegisteredAuditQueue())
                .to(smartBankingExchange()).with("merchant.registered");
    }
    @Bean public Binding merchantPaymentInitiatedAuditBinding() {
        return BindingBuilder.bind(merchantPaymentInitiatedAuditQueue())
                .to(smartBankingExchange()).with("merchant.payment.initiated");
    }
    @Bean public Binding merchantPaymentCompletedAuditBinding() {
        return BindingBuilder.bind(merchantPaymentCompletedAuditQueue())
                .to(smartBankingExchange()).with("merchant.payment.completed");
    }
    @Bean public Binding merchantPaymentFailedAuditBinding() {
        return BindingBuilder.bind(merchantPaymentFailedAuditQueue())
                .to(smartBankingExchange()).with("merchant.payment.failed");
    }

    // ================================================================
    // SAVINGS audit queues
    // ================================================================
    @Bean public Queue savingsGroupCreatedAuditQueue() {
        return QueueBuilder.durable("savings.group.created.audit").build();
    }
    @Bean public Queue savingsMemberJoinedAuditQueue() {
        return QueueBuilder.durable("savings.member.joined.audit").build();
    }
    @Bean public Queue savingsContributionMadeAuditQueue() {
        return QueueBuilder.durable("savings.contribution.made.audit").build();
    }
    @Bean public Queue savingsPayoutProcessedAuditQueue() {
        return QueueBuilder.durable("savings.payout.processed.audit").build();
    }
    @Bean public Binding savingsGroupCreatedAuditBinding() {
        return BindingBuilder.bind(savingsGroupCreatedAuditQueue())
                .to(smartBankingExchange()).with("savings.group.created");
    }
    @Bean public Binding savingsMemberJoinedAuditBinding() {
        return BindingBuilder.bind(savingsMemberJoinedAuditQueue())
                .to(smartBankingExchange()).with("savings.member.joined");
    }
    @Bean public Binding savingsContributionMadeAuditBinding() {
        return BindingBuilder.bind(savingsContributionMadeAuditQueue())
                .to(smartBankingExchange()).with("savings.contribution.made");
    }
    @Bean public Binding savingsPayoutProcessedAuditBinding() {
        return BindingBuilder.bind(savingsPayoutProcessedAuditQueue())
                .to(smartBankingExchange()).with("savings.payout.processed");
    }
}
