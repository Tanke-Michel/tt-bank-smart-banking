package com.example.api_gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "jwt.secret=3d7f4a9b2c8e1f5a6d9b3c7e2f8a1d4b7e3c9f2a6b5d8e1c4f7a2b3d6e9f0c1",
    "spring.cloud.gateway.routes[0].id=auth-test",
    "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/api/v1/auth/**"
})
class ApiGatewayApplicationTests {

    @Test
    @DisplayName("Spring WebFlux + Gateway context loads without errors")
    void contextLoads() {
    }
}
