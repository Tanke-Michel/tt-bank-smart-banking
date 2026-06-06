package com.example.notification_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for the Notification Service.
 *
 * The notification service is a PURE CONSUMER — it never publishes events.
 * It binds to the queues already declared by the producing services.
 * We re-declare them here as durable so they survive broker restarts
 * regardless of which service starts first.
 *
 * All 14 queues across 4 domains are bound here:
 *   Wallet (3), Transaction (3), Merchant (4), Savings (4)
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    // ------------------------------------------------------------------
    // Shared exchange — same one all services publish to
    // ------------------------------------------------------------------

    @Bean
    public TopicExchange smartBankingExchange() {
        return ExchangeBuilder.topicExchange(exchangeName).durable(true).build();
    }

    // ------------------------------------------------------------------
    // JSON message converter — must match producers
    // ------------------------------------------------------------------

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
    // WALLET queues
    // ================================================================

    @Bean public Queue walletCreatedQueue() {
        return QueueBuilder.durable("wallet.created.queue").build();
    }
    @Bean public Queue walletFundedQueue() {
        return QueueBuilder.durable("wallet.funded.queue").build();
    }
    @Bean public Queue walletWithdrawnQueue() {
        return QueueBuilder.durable("wallet.withdrawn.queue").build();
    }

    @Bean public Binding walletCreatedBinding() {
        return BindingBuilder.bind(walletCreatedQueue())
                .to(smartBankingExchange()).with("wallet.created");
    }
    @Bean public Binding walletFundedBinding() {
        return BindingBuilder.bind(walletFundedQueue())
                .to(smartBankingExchange()).with("wallet.funded");
    }
    @Bean public Binding walletWithdrawnBinding() {
        return BindingBuilder.bind(walletWithdrawnQueue())
                .to(smartBankingExchange()).with("wallet.withdrawn");
    }

    // ================================================================
    // TRANSACTION queues
    // ================================================================

    @Bean public Queue transactionInitiatedQueue() {
        return QueueBuilder.durable("transaction.initiated.queue").build();
    }
    @Bean public Queue transactionCompletedQueue() {
        return QueueBuilder.durable("transaction.completed.queue").build();
    }
    @Bean public Queue transactionFailedQueue() {
        return QueueBuilder.durable("transaction.failed.queue").build();
    }

    @Bean public Binding transactionInitiatedBinding() {
        return BindingBuilder.bind(transactionInitiatedQueue())
                .to(smartBankingExchange()).with("transaction.initiated");
    }
    @Bean public Binding transactionCompletedBinding() {
        return BindingBuilder.bind(transactionCompletedQueue())
                .to(smartBankingExchange()).with("transaction.completed");
    }
    @Bean public Binding transactionFailedBinding() {
        return BindingBuilder.bind(transactionFailedQueue())
                .to(smartBankingExchange()).with("transaction.failed");
    }

    // ================================================================
    // MERCHANT queues
    // ================================================================

    @Bean public Queue merchantRegisteredQueue() {
        return QueueBuilder.durable("merchant.registered.queue").build();
    }
    @Bean public Queue merchantPaymentInitiatedQueue() {
        return QueueBuilder.durable("merchant.payment.initiated.queue").build();
    }
    @Bean public Queue merchantPaymentCompletedQueue() {
        return QueueBuilder.durable("merchant.payment.completed.queue").build();
    }
    @Bean public Queue merchantPaymentFailedQueue() {
        return QueueBuilder.durable("merchant.payment.failed.queue").build();
    }

    @Bean public Binding merchantRegisteredBinding() {
        return BindingBuilder.bind(merchantRegisteredQueue())
                .to(smartBankingExchange()).with("merchant.registered");
    }
    @Bean public Binding merchantPaymentInitiatedBinding() {
        return BindingBuilder.bind(merchantPaymentInitiatedQueue())
                .to(smartBankingExchange()).with("merchant.payment.initiated");
    }
    @Bean public Binding merchantPaymentCompletedBinding() {
        return BindingBuilder.bind(merchantPaymentCompletedQueue())
                .to(smartBankingExchange()).with("merchant.payment.completed");
    }
    @Bean public Binding merchantPaymentFailedBinding() {
        return BindingBuilder.bind(merchantPaymentFailedQueue())
                .to(smartBankingExchange()).with("merchant.payment.failed");
    }

    // ================================================================
    // SAVINGS queues
    // ================================================================

    @Bean public Queue savingsGroupCreatedQueue() {
        return QueueBuilder.durable("savings.group.created.queue").build();
    }
    @Bean public Queue savingsMemberJoinedQueue() {
        return QueueBuilder.durable("savings.member.joined.queue").build();
    }
    @Bean public Queue savingsContributionMadeQueue() {
        return QueueBuilder.durable("savings.contribution.made.queue").build();
    }
    @Bean public Queue savingsPayoutProcessedQueue() {
        return QueueBuilder.durable("savings.payout.processed.queue").build();
    }

    @Bean public Binding savingsGroupCreatedBinding() {
        return BindingBuilder.bind(savingsGroupCreatedQueue())
                .to(smartBankingExchange()).with("savings.group.created");
    }
    @Bean public Binding savingsMemberJoinedBinding() {
        return BindingBuilder.bind(savingsMemberJoinedQueue())
                .to(smartBankingExchange()).with("savings.member.joined");
    }
    @Bean public Binding savingsContributionMadeBinding() {
        return BindingBuilder.bind(savingsContributionMadeQueue())
                .to(smartBankingExchange()).with("savings.contribution.made");
    }
    @Bean public Binding savingsPayoutProcessedBinding() {
        return BindingBuilder.bind(savingsPayoutProcessedQueue())
                .to(smartBankingExchange()).with("savings.payout.processed");
    }
}
