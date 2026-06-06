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

@DisplayName("ResponseHeaderFilter Unit Tests")
@ExtendWith(MockitoExtension.class)
class ResponseHeaderFilterTest {

    @Mock
    private GatewayFilterChain chain;

    private ResponseHeaderFilter responseHeaderFilter;

    @BeforeEach
    void setUp() {
        responseHeaderFilter = new ResponseHeaderFilter();
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Filter adds X-Content-Type-Options: nosniff to every response")
    void filter_addsContentTypeOptions() {
        var exchange = exchangeForGet("/api/v1/wallet/balance");

        StepVerifier.create(responseHeaderFilter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst("X-Content-Type-Options"))
                .isEqualTo("nosniff");
    }

    @Test
    @DisplayName("Filter adds X-Frame-Options: DENY to every response")
    void filter_addsFrameOptions() {
        var exchange = exchangeForGet("/api/v1/auth/login");

        StepVerifier.create(responseHeaderFilter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst("X-Frame-Options"))
                .isEqualTo("DENY");
    }

    @Test
    @DisplayName("Filter adds X-XSS-Protection header")
    void filter_addsXssProtection() {
        var exchange = exchangeForGet("/api/v1/transactions/history");

        StepVerifier.create(responseHeaderFilter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst("X-XSS-Protection"))
                .isEqualTo("1; mode=block");
    }

    @Test
    @DisplayName("Filter adds Strict-Transport-Security header")
    void filter_addsHsts() {
        var exchange = exchangeForGet("/api/v1/wallet/balance");

        StepVerifier.create(responseHeaderFilter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security"))
                .isEqualTo("max-age=31536000; includeSubDomains");
    }

    @Test
    @DisplayName("Filter adds Cache-Control: no-store header")
    void filter_addsCacheControl() {
        var exchange = exchangeForGet("/api/v1/wallet/balance");

        StepVerifier.create(responseHeaderFilter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst("Cache-Control"))
                .contains("no-store");
    }

    @Test
    @DisplayName("Filter adds Referrer-Policy header")
    void filter_addsReferrerPolicy() {
        var exchange = exchangeForGet("/api/v1/merchants/list");

        StepVerifier.create(responseHeaderFilter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst("Referrer-Policy"))
                .isEqualTo("strict-origin-when-cross-origin");
    }

    @Test
    @DisplayName("Filter has LOWEST_PRECEDENCE order — runs after all other filters")
    void filter_hasCorrectOrder() {
        assertThat(responseHeaderFilter.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    @DisplayName("Filter still passes the request through the chain")
    void filter_callsChain() {
        var exchange = exchangeForGet("/api/v1/savings/groups");

        StepVerifier.create(responseHeaderFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, times(1)).filter(any());
    }

    private MockServerWebExchange exchangeForGet(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
    }
}
