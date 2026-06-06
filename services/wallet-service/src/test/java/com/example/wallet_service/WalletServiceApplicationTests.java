package com.example.wallet_service;

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
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=5672",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
    "app.rabbitmq.exchange=test.exchange",
    "app.rabbitmq.wallet.created-routing-key=wallet.created",
    "app.rabbitmq.wallet.funded-routing-key=wallet.funded",
    "app.rabbitmq.wallet.withdrawn-routing-key=wallet.withdrawn",
    "app.wallet.max-deposit-amount=10000000",
    "app.wallet.max-withdrawal-amount=5000000",
    "app.wallet.daily-withdrawal-limit=10000000",
    "app.wallet.minimum-balance=0"
})
class WalletServiceApplicationTests {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
    }
}
