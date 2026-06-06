package com.example.savings_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for the Community Savings Service.
 * Uses the shared smart-banking.exchange (topic exchange).
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.savings.group-created-routing-key}")
    private String groupCreatedKey;

    @Value("${app.rabbitmq.savings.contribution-made-routing-key}")
    private String contributionMadeKey;

    @Value("${app.rabbitmq.savings.payout-processed-routing-key}")
    private String payoutProcessedKey;

    @Value("${app.rabbitmq.savings.member-joined-routing-key}")
    private String memberJoinedKey;

    @Bean
    public TopicExchange smartBankingExchange() {
        return ExchangeBuilder.topicExchange(exchangeName).durable(true).build();
    }

    @Bean
    public Queue savingsGroupCreatedQueue() {
        return QueueBuilder.durable("savings.group.created.queue").build();
    }

    @Bean
    public Queue savingsContributionMadeQueue() {
        return QueueBuilder.durable("savings.contribution.made.queue").build();
    }

    @Bean
    public Queue savingsPayoutProcessedQueue() {
        return QueueBuilder.durable("savings.payout.processed.queue").build();
    }

    @Bean
    public Queue savingsMemberJoinedQueue() {
        return QueueBuilder.durable("savings.member.joined.queue").build();
    }

    @Bean
    public Binding groupCreatedBinding() {
        return BindingBuilder.bind(savingsGroupCreatedQueue())
                .to(smartBankingExchange()).with(groupCreatedKey);
    }

    @Bean
    public Binding contributionMadeBinding() {
        return BindingBuilder.bind(savingsContributionMadeQueue())
                .to(smartBankingExchange()).with(contributionMadeKey);
    }

    @Bean
    public Binding payoutProcessedBinding() {
        return BindingBuilder.bind(savingsPayoutProcessedQueue())
                .to(smartBankingExchange()).with(payoutProcessedKey);
    }

    @Bean
    public Binding memberJoinedBinding() {
        return BindingBuilder.bind(savingsMemberJoinedQueue())
                .to(smartBankingExchange()).with(memberJoinedKey);
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
