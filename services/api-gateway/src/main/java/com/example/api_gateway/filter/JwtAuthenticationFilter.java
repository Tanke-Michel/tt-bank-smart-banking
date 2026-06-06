package com.example.api_gateway.filter;

import com.example.api_gateway.config.JwtConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT Authentication Gateway Filter Factory.
 *
 * Applied per-route in application.yml as:
 *   filters:
 *     - name: JwtAuthenticationFilter
 *
 * Behaviour:
 *  1. Reads "public-paths" from the matched route's metadata.
 *     If the incoming path matches any public path, the request
 *     passes through immediately — no token required.
 *
 *  2. For all other paths, requires "Authorization: Bearer <token>".
 *     Missing or malformed header → 401 immediately.
 *
 *  3. Validates the token using JwtConfig (same secret as auth-service).
 *     Invalid or expired token → 401 immediately.
 *
 *  4. On success: extracts email, role, userId from JWT claims and
 *     forwards them as downstream-only request headers:
 *       X-Auth-User-Email  — user's email address
 *       X-Auth-User-Role   — e.g. "USER", "MERCHANT", "ADMIN"
 *       X-Auth-User-Id     — user's database id (as string)
 *     Downstream services trust these headers and skip JWT validation.
 *     The gateway is the single security boundary.
 *
 *  5. Adds X-Correlation-Id (already set by RequestLoggingFilter) if not present.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter
        extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private static final String BEARER_PREFIX  = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final ObjectMapper   OBJECT_MAPPER = new ObjectMapper();

    private final JwtConfig jwtConfig;

    public JwtAuthenticationFilter(JwtConfig jwtConfig) {
        super(Config.class);
        this.jwtConfig = jwtConfig;
    }

    /** Config class — no per-route config fields needed beyond route metadata. */
    public static class Config {
        // intentionally empty
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();

            // ── Step 1: Read public paths from this route's metadata ──────────
            List<String> publicPaths = resolvePublicPaths(exchange);

            // ── Step 2: Skip JWT check for public paths ───────────────────────
            if (isPublicPath(path, publicPaths)) {
                log.debug("Public path — passing through without token check: {}", path);
                return chain.filter(exchange);
            }

            // ── Step 3: Require Authorization: Bearer <token> ─────────────────
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                log.warn("Missing or malformed Authorization header — path: {}", path);
                return sendError(exchange, HttpStatus.UNAUTHORIZED,
                        "Missing or malformed Authorization header. " +
                        "Expected: Authorization: Bearer <token>");
            }

            // ── Step 4: Validate token ────────────────────────────────────────
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            if (!jwtConfig.isTokenValid(token)) {
                log.warn("Invalid or expired JWT — path: {}", path);
                return sendError(exchange, HttpStatus.UNAUTHORIZED,
                        "Token is invalid or has expired. Please login again.");
            }

            // ── Step 5: Extract claims ────────────────────────────────────────
            String email  = jwtConfig.extractEmail(token);
            String role   = jwtConfig.extractRole(token);
            String userId = jwtConfig.extractUserId(token);

            log.debug("JWT valid — user: {} role: {} → {}", email, role, path);

            // ── Step 6: Forward claims as trusted downstream headers ──────────
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Auth-User-Email",  email  != null ? email  : "")
                    .header("X-Auth-User-Role",   role   != null ? role   : "")
                    .header("X-Auth-User-Id",     userId != null ? userId : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read public-paths from matched route metadata
    // ─────────────────────────────────────────────────────────────────────────

    private List<String> resolvePublicPaths(ServerWebExchange exchange) {
        Object routeAttr = exchange.getAttributes()
                .get(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        if (routeAttr instanceof Route route) {
            Object val = route.getMetadata().get("public-paths");
            if (val instanceof String raw && !raw.isBlank()) {
                return Arrays.stream(raw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList();
            }
        }
        return List.of();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ant-path matching — supports wildcards e.g. /api/v1/auth/**
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isPublicPath(String requestPath, List<String> publicPaths) {
        for (String pattern : publicPaths) {
            if (PATH_MATCHER.match(pattern, requestPath) || pattern.equals(requestPath)) {
                return true;
            }
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Structured JSON error response (never an HTML page)
    // ─────────────────────────────────────────────────────────────────────────

    private Mono<Void> sendError(ServerWebExchange exchange,
                                  HttpStatus status,
                                  String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",    status.value());
        body.put("error",     status.getReasonPhrase());
        body.put("message",   message);
        body.put("path",      exchange.getRequest().getPath().value());
        body.put("timestamp", LocalDateTime.now().toString());

        byte[] bytes;
        try {
            bytes = OBJECT_MAPPER.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = ("{\"status\":" + status.value() + ",\"error\":\"" +
                    status.getReasonPhrase() + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
