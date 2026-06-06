package com.example.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory sliding-window rate limiter.
 *
 * Limits per remote IP address:
 *   - General endpoints  : 100 requests per minute
 *   - Auth endpoints     : 20 requests per minute (stricter — prevents brute force)
 *
 * For production, replace with Redis-backed rate limiting
 * (e.g. Spring Cloud Gateway's built-in RequestRateLimiter with RedisRateLimiter).
 * This in-memory implementation is correct and sufficient for development.
 *
 * Order: HIGHEST_PRECEDENCE + 2 — runs after logging but before JWT validation,
 *        so rate-limited requests never reach the JWT filter.
 */
@Slf4j
@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private static final int GENERAL_LIMIT   = 100;
    private static final int AUTH_LIMIT      = 20;
    private static final long WINDOW_MILLIS  = 60_000L; // 1 minute

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    // key: remoteIp | value: [windowStartMs, requestCount]
    private final Map<String, long[]> requestCounts = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String remoteIp = getRemoteIp(exchange);
        String path     = exchange.getRequest().getPath().value();

        int limit = path.startsWith("/api/v1/auth/") ? AUTH_LIMIT : GENERAL_LIMIT;

        if (isRateLimited(remoteIp, limit)) {
            log.warn("Rate limit exceeded for IP: {} path: {}", remoteIp, path);
            return sendTooManyRequests(exchange);
        }

        return chain.filter(exchange);
    }

    // ------------------------------------------------------------------
    // Sliding window logic
    // ------------------------------------------------------------------

    private boolean isRateLimited(String key, int limit) {
        long now = Instant.now().toEpochMilli();
        long[] entry = requestCounts.compute(key, (k, v) -> {
            if (v == null || now - v[0] > WINDOW_MILLIS) {
                // New window
                return new long[]{ now, 1L };
            }
            v[1]++;
            return v;
        });
        return entry[1] > limit;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String getRemoteIp(ServerWebExchange exchange) {
        // Respect X-Forwarded-For if behind a load balancer / Nginx
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    private Mono<Void> sendTooManyRequests(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().add("Retry-After", "60");

        String body = """
                {"status":429,"error":"Too Many Requests",\
                "message":"Rate limit exceeded. Please wait before retrying."}""";

        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
