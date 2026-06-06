package com.example.notification_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=5672",
    "app.rabbitmq.exchange=test.exchange",
    "spring.mail.host=localhost",
    "spring.mail.port=25",
    "app.mail.from=test@example.com",
    "app.mail.from-name=Test"
})
class NotificationServiceApplicationTests {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    void contextLoads() {
        // Verifies the Spring context starts without errors
    }
}
