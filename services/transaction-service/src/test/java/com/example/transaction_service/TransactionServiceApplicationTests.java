package com.example.transaction_service;

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
    "app.rabbitmq.transaction.initiated-routing-key=transaction.initiated",
    "app.rabbitmq.transaction.completed-routing-key=transaction.completed",
    "app.rabbitmq.transaction.failed-routing-key=transaction.failed",
    "app.wallet-service.base-url=http://localhost:8082",
    "app.transaction.max-transfer-amount=5000000",
    "app.transaction.min-transfer-amount=1",
    "app.transaction.daily-transfer-limit=10000000"
})
class TransactionServiceApplicationTests {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
        // Verifies the Spring context starts without errors
    }
}
