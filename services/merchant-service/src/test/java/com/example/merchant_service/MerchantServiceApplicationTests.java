package com.example.merchant_service;

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
    "app.rabbitmq.merchant.payment-initiated-routing-key=merchant.payment.initiated",
    "app.rabbitmq.merchant.payment-completed-routing-key=merchant.payment.completed",
    "app.rabbitmq.merchant.payment-failed-routing-key=merchant.payment.failed",
    "app.rabbitmq.merchant.registered-routing-key=merchant.registered",
    "app.wallet-service.base-url=http://localhost:8082",
    "app.qr.width=300",
    "app.qr.height=300",
    "app.merchant.max-payment-amount=50000000",
    "app.merchant.min-payment-amount=1"
})
class MerchantServiceApplicationTests {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
        // Verifies the Spring context starts without errors
    }
}
