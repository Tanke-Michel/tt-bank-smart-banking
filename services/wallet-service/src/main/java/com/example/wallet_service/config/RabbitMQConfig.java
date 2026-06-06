package com.example.wallet_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for the Wallet Service.
 *
 * Exchange: smart-banking.exchange (topic exchange — flexible routing)
 *
 * Queues and routing keys:
 *   wallet.created  -> wallet.created.queue
 *   wallet.funded   -> wallet.funded.queue
 *   wallet.withdrawn-> wallet.withdrawn.queue
 *
 * All queues are durable so messages survive a broker restart.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.wallet.created-routing-key}")
    private String walletCreatedKey;

    @Value("${app.rabbitmq.wallet.funded-routing-key}")
    private String walletFundedKey;

    @Value("${app.rabbitmq.wallet.withdrawn-routing-key}")
    private String walletWithdrawnKey;

    // -----------------------------------------------
    // Exchange
    // -----------------------------------------------

    @Bean
    public TopicExchange smartBankingExchange() {
        return ExchangeBuilder.topicExchange(exchangeName).durable(true).build();
    }

    // -----------------------------------------------
    // Queues
    // -----------------------------------------------

    @Bean
    public Queue walletCreatedQueue() {
        return QueueBuilder.durable("wallet.created.queue").build();
    }

    @Bean
    public Queue walletFundedQueue() {
        return QueueBuilder.durable("wallet.funded.queue").build();
    }

    @Bean
    public Queue walletWithdrawnQueue() {
        return QueueBuilder.durable("wallet.withdrawn.queue").build();
    }

    // -----------------------------------------------
    // Bindings (queue -> exchange via routing key)
    // -----------------------------------------------

    @Bean
    public Binding walletCreatedBinding() {
        return BindingBuilder
                .bind(walletCreatedQueue())
                .to(smartBankingExchange())
                .with(walletCreatedKey);
    }

    @Bean
    public Binding walletFundedBinding() {
        return BindingBuilder
                .bind(walletFundedQueue())
                .to(smartBankingExchange())
                .with(walletFundedKey);
    }

    @Bean
    public Binding walletWithdrawnBinding() {
        return BindingBuilder
                .bind(walletWithdrawnQueue())
                .to(smartBankingExchange())
                .with(walletWithdrawnKey);
    }

    // -----------------------------------------------
    // JSON serialization for messages
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
