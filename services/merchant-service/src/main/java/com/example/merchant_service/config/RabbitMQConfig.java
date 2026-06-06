package com.example.merchant_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for the Merchant Service.
 * Uses the same smart-banking.exchange as wallet and transaction services.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.merchant.payment-initiated-routing-key}")
    private String paymentInitiatedKey;

    @Value("${app.rabbitmq.merchant.payment-completed-routing-key}")
    private String paymentCompletedKey;

    @Value("${app.rabbitmq.merchant.payment-failed-routing-key}")
    private String paymentFailedKey;

    @Value("${app.rabbitmq.merchant.registered-routing-key}")
    private String merchantRegisteredKey;

    @Bean
    public TopicExchange smartBankingExchange() {
        return ExchangeBuilder.topicExchange(exchangeName).durable(true).build();
    }

    @Bean
    public Queue merchantPaymentInitiatedQueue() {
        return QueueBuilder.durable("merchant.payment.initiated.queue").build();
    }

    @Bean
    public Queue merchantPaymentCompletedQueue() {
        return QueueBuilder.durable("merchant.payment.completed.queue").build();
    }

    @Bean
    public Queue merchantPaymentFailedQueue() {
        return QueueBuilder.durable("merchant.payment.failed.queue").build();
    }

    @Bean
    public Queue merchantRegisteredQueue() {
        return QueueBuilder.durable("merchant.registered.queue").build();
    }

    @Bean
    public Binding paymentInitiatedBinding() {
        return BindingBuilder.bind(merchantPaymentInitiatedQueue())
                .to(smartBankingExchange()).with(paymentInitiatedKey);
    }

    @Bean
    public Binding paymentCompletedBinding() {
        return BindingBuilder.bind(merchantPaymentCompletedQueue())
                .to(smartBankingExchange()).with(paymentCompletedKey);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(merchantPaymentFailedQueue())
                .to(smartBankingExchange()).with(paymentFailedKey);
    }

    @Bean
    public Binding merchantRegisteredBinding() {
        return BindingBuilder.bind(merchantRegisteredQueue())
                .to(smartBankingExchange()).with(merchantRegisteredKey);
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
}
