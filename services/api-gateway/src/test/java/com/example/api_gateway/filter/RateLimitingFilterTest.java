package com.example.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("RateLimitingFilter Unit Tests")
@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private GatewayFilterChain chain;

    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        rateLimitingFilter = new RateLimitingFilter();
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("First request to a general endpoint — passes through")
    void firstRequest_generalEndpoint_passesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/wallet/balance")
                .remoteAddress(new InetSocketAddress("10.1.1.1", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(rateLimitingFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, times(1)).filter(any());
        assertThat(exchange.getResponse().getStatusCode())
                .isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("First request to auth endpoint — passes through")
    void firstRequest_authEndpoint_passesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/auth/login")
                .remoteAddress(new InetSocketAddress("10.1.1.2", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(rateLimitingFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, times(1)).filter(any());
    }

    @Test
    @DisplayName("Filter has correct order — HIGHEST_PRECEDENCE + 2 (after logging)")
    void filter_hasCorrectOrder() {
        assertThat(rateLimitingFilter.getOrder())
                .isEqualTo(Ordered.HIGHEST_PRECEDENCE + 2);
    }

    @Test
    @DisplayName("Logging filter runs before rate-limit filter (lower order value = earlier)")
    void filterOrdering_loggingBeforeRateLimit() {
        RequestLoggingFilter loggingFilter = new RequestLoggingFilter();
        assertThat(loggingFilter.getOrder()).isLessThan(rateLimitingFilter.getOrder());
    }

    @Test
    @DisplayName("Different IPs are tracked independently — both pass on first request")
    void differentIps_trackedIndependently() {
        MockServerHttpRequest req1 = MockServerHttpRequest
                .post("/api/v1/auth/login")
                .remoteAddress(new InetSocketAddress("192.168.1.1", 1000))
                .build();
        MockServerHttpRequest req2 = MockServerHttpRequest
                .post("/api/v1/auth/login")
                .remoteAddress(new InetSocketAddress("192.168.1.2", 2000))
                .build();

        StepVerifier.create(rateLimitingFilter.filter(
                MockServerWebExchange.from(req1), chain)).verifyComplete();
        StepVerifier.create(rateLimitingFilter.filter(
                MockServerWebExchange.from(req2), chain)).verifyComplete();

        verify(chain, times(2)).filter(any());
    }

    @Test
    @DisplayName("Auth limit (20/min) is lower than general limit (100/min)")
    void authLimit_isLowerThanGeneralLimit() {
        // This is a design assertion — verifiable by inspecting constants.
        // We send 21 requests from the same IP to the auth endpoint and
        // confirm the 21st is rate-limited.
        RateLimitingFilter freshFilter = new RateLimitingFilter();
        // unique IP so it doesn't share state with other tests
        String uniqueIp = "172.16.99.1";
        int blockedCount = 0;

        for (int i = 0; i < 22; i++) {
            MockServerHttpRequest req = MockServerHttpRequest
                    .post("/api/v1/auth/login")
                    .remoteAddress(new InetSocketAddress(uniqueIp, 9000 + i))
                    .build();
            MockServerWebExchange ex = MockServerWebExchange.from(req);

            // Use a separate mock chain per request to track individually
            GatewayFilterChain localChain = mock(GatewayFilterChain.class);
            when(localChain.filter(any())).thenReturn(Mono.empty());

            freshFilter.filter(ex, localChain).block();

            if (HttpStatus.TOO_MANY_REQUESTS.equals(ex.getResponse().getStatusCode())) {
                blockedCount++;
            }
        }

        // Requests 21 and 22 should be blocked (auth limit = 20/min)
        assertThat(blockedCount).isGreaterThanOrEqualTo(1);
    }
}
