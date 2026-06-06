package com.example.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * GlobalFilter — runs for EVERY request through the gateway.
 *
 * Responsibilities:
 *   1. Logs each incoming request (method, path, remote address).
 *   2. Adds a unique correlation ID header (X-Correlation-Id) so that
 *      distributed logs across services can be tied together.
 *   3. Logs response status and total processing time after the
 *      downstream response is received.
 *
 * Order: Ordered.HIGHEST_PRECEDENCE + 1 so it runs first (before JWT filter).
 */
@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = Instant.now().toEpochMilli();

        String method  = request.getMethod().name();
        String path    = request.getPath().value();
        String remote  = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

        // Generate a simple correlation ID from timestamp + hash
        String correlationId = Long.toHexString(System.nanoTime());

        log.info("→ {} {} from {} [correlationId={}]", method, path, remote, correlationId);

        // Mutate the request to add the correlation ID header
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Correlation-Id", correlationId)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doFinally(signal -> {
                    long duration = Instant.now().toEpochMilli() - startTime;
                    int status = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 0;
                    log.info("← {} {} → {} in {}ms [correlationId={}]",
                            method, path, status, duration, correlationId);
                });
    }
}
