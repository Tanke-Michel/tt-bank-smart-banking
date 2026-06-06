package com.example.savings_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=5672",
    "app.rabbitmq.exchange=test.exchange",
    "app.rabbitmq.savings.group-created-routing-key=savings.group.created",
    "app.rabbitmq.savings.contribution-made-routing-key=savings.contribution.made",
    "app.rabbitmq.savings.payout-processed-routing-key=savings.payout.processed",
    "app.rabbitmq.savings.member-joined-routing-key=savings.member.joined",
    "app.wallet-service.base-url=http://localhost:8082",
    "app.savings.max-members-per-group=50",
    "app.savings.min-contribution-amount=100"
})
class SavingsServiceApplicationTests {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
        // Verifies the Spring context starts without errors
    }
}
