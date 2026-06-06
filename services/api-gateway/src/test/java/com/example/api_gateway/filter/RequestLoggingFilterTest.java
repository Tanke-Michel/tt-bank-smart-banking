package com.example.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("RequestLoggingFilter Unit Tests")
@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTest {

    @Mock
    private GatewayFilterChain chain;

    private RequestLoggingFilter loggingFilter;

    @BeforeEach
    void setUp() {
        loggingFilter = new RequestLoggingFilter();
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Filter runs to completion and passes the request through")
    void filter_passesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/auth/login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(loggingFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, times(1)).filter(any());
    }

    @Test
    @DisplayName("Filter has order HIGHEST_PRECEDENCE + 1")
    void filter_hasCorrectOrder() {
        assertThat(loggingFilter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
    }

    @Test
    @DisplayName("Filter runs for any path — wallet endpoint")
    void filter_walletPath_passesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/wallet/balance")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(loggingFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, times(1)).filter(any());
    }

    @Test
    @DisplayName("Filter runs for POST requests")
    void filter_postRequest_passesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/auth/register")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(loggingFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, times(1)).filter(any());
    }
}
