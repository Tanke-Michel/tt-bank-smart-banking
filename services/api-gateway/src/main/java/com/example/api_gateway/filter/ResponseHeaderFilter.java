package com.example.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * GlobalFilter — adds security response headers to every response.
 *
 * These headers harden the application against common web vulnerabilities:
 *   - X-Content-Type-Options   : prevents MIME-sniffing attacks
 *   - X-Frame-Options          : prevents clickjacking
 *   - X-XSS-Protection         : enables browser XSS filter (legacy browsers)
 *   - Strict-Transport-Security: forces HTTPS in browsers that have visited before
 *   - Referrer-Policy          : controls what's sent in the Referer header
 *   - Cache-Control            : prevents caching of API responses
 *
 * Order: Ordered.LOWEST_PRECEDENCE — runs last so it always applies,
 *        even if earlier filters short-circuit (e.g. rate limit 429).
 */
@Component
public class ResponseHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            exchange.getResponse().getHeaders().add(
                    "X-Content-Type-Options", "nosniff");
            exchange.getResponse().getHeaders().add(
                    "X-Frame-Options", "DENY");
            exchange.getResponse().getHeaders().add(
                    "X-XSS-Protection", "1; mode=block");
            exchange.getResponse().getHeaders().add(
                    "Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            exchange.getResponse().getHeaders().add(
                    "Referrer-Policy", "strict-origin-when-cross-origin");
            exchange.getResponse().getHeaders().add(
                    "Cache-Control", "no-store, no-cache, must-revalidate");
        }));
    }
}
