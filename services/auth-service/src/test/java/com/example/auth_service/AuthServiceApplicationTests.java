package com.example.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "jwt.secret=3d7f4a9b2c8e1f5a6d9b3c7e2f8a1d4b7e3c9f2a6b5d8e1c4f7a2b3d6e9f0c1",
    "jwt.access-token-expiration=86400000",
    "jwt.refresh-token-expiration=604800000",
    "otp.expiration-minutes=10",
    "otp.length=6",
    "app.mail.from=test@example.com",
    "app.mail.from-name=Test",
    "app.base-url=http://localhost:5173",
    "spring.mail.host=localhost",
    "spring.mail.port=25"
})
class AuthServiceApplicationTests {

    // Mock external dependencies so context loads without real Redis/Mail
    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean(name = "redisTemplate")
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void contextLoads() {
        // Verifies Spring context starts without errors
    }
}