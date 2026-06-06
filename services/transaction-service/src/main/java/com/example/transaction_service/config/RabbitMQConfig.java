package com.example.transaction_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for the Transaction Service.
 *
 * Uses the same exchange as the wallet service (smart-banking.exchange)
 * so that a single exchange fans out all banking domain events.
 *
 * Queues bound by this service:
 *   transaction.initiated  → notification service listens (sends "transfer pending" email)
 *   transaction.completed  → notification service + audit service
 *   transaction.failed     → notification service
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.transaction.initiated-routing-key}")
    private String txnInitiatedKey;

    @Value("${app.rabbitmq.transaction.completed-routing-key}")
    private String txnCompletedKey;

    @Value("${app.rabbitmq.transaction.failed-routing-key}")
    private String txnFailedKey;

    // -----------------------------------------------
    // Exchange — shared with wallet-service
    // -----------------------------------------------

    @Bean
    public TopicExchange smartBankingExchange() {
        return ExchangeBuilder.topicExchange(exchangeName).durable(true).build();
    }

    // -----------------------------------------------
    // Queues
    // -----------------------------------------------

    @Bean
    public Queue transactionInitiatedQueue() {
        return QueueBuilder.durable("transaction.initiated.queue").build();
    }

    @Bean
    public Queue transactionCompletedQueue() {
        return QueueBuilder.durable("transaction.completed.queue").build();
    }

    @Bean
    public Queue transactionFailedQueue() {
        return QueueBuilder.durable("transaction.failed.queue").build();
    }

    // -----------------------------------------------
    // Bindings
    // -----------------------------------------------

    @Bean
    public Binding txnInitiatedBinding() {
        return BindingBuilder.bind(transactionInitiatedQueue())
                .to(smartBankingExchange()).with(txnInitiatedKey);
    }

    @Bean
    public Binding txnCompletedBinding() {
        return BindingBuilder.bind(transactionCompletedQueue())
                .to(smartBankingExchange()).with(txnCompletedKey);
    }

    @Bean
    public Binding txnFailedBinding() {
        return BindingBuilder.bind(transactionFailedQueue())
                .to(smartBankingExchange()).with(txnFailedKey);
    }

    // -----------------------------------------------
    // JSON serialization
    // -----------------------------------------------

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
}
