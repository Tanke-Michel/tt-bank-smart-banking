package com.example.api_gateway.filter;

import com.example.api_gateway.config.JwtConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("JwtAuthenticationFilter Unit Tests")
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private GatewayFilterChain chain;

    private JwtAuthenticationFilter filter;

    private static final String SECRET =
            "3d7f4a9b2c8e1f5a6d9b3c7e2f8a1d4b7e3c9f2a6b5d8e1c4f7a2b3d6e9f0c1";

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtConfig);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public paths — no token required
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Public path /login — passes through without JWT validation")
    void publicPath_login_passesThrough() {
        var exchange = exchangeForPath("POST", "/api/v1/auth/login", null);
        addRouteMetadata(exchange, "/api/v1/auth/login,/api/v1/auth/register");

        GatewayFilter gf = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gf.filter(exchange, chain)).verifyComplete();

        verify(jwtConfig, never()).isTokenValid(anyString());
        verify(chain, times(1)).filter(any());
    }

    @Test
    @DisplayName("Public path /register — passes through without token")
    void publicPath_register_passesThrough() {
        var exchange = exchangeForPath("POST", "/api/v1/auth/register", null);
        addRouteMetadata(exchange,
                "/api/v1/auth/register,/api/v1/auth/login,/api/v1/auth/refresh");

        GatewayFilter gf = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gf.filter(exchange, chain)).verifyComplete();

        verify(jwtConfig, never()).isTokenValid(anyString());
        verify(chain, times(1)).filter(any());
    }

    @Test
    @DisplayName("Public path /forgot-password — passes through without token")
    void publicPath_forgotPassword_passesThrough() {
        var exchange = exchangeForPath("POST", "/api/v1/auth/forgot-password", null);
        addRouteMetadata(exchange, "/api/v1/auth/forgot-password,/api/v1/auth/reset-password");

        GatewayFilter gf = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gf.filter(exchange, chain)).verifyComplete();

        verify(jwtConfig, never()).isTokenValid(anyString());
    }

    @Test
    @DisplayName("Public path with a token — still skips validation (public = no check)")
    void publicPath_withToken_skipsValidation() {
        String token = buildTestToken("user@example.com", "USER", 1L);
        var exchange = exchangeForPath("POST", "/api/v1/auth/login",
                "Bearer " + token);
        addRouteMetadata(exchange, "/api/v1/auth/login");

        GatewayFilter gf = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gf.filter(exchange, chain)).verifyComplete();

        verify(jwtConfig, never()).isTokenValid(anyString());
        verify(chain, times(1)).filter(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Protected paths — missing / malformed token
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Protected path with no Authorization header — returns 401")
    void protectedPath_noAuthHeader_returns401() {
        var exchange = exchangeForPath("GET", "/api/v1/wallet/balance", null);
        addRouteMetadata(exchange, ""); // no public paths for wallet

        GatewayFilter gf = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gf.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
        verify(jwtConfig, never()).isTokenValid(anyString());
    }

    @Test
    @DisplayName("Protected path with Basic auth (not Bearer) — returns 401")
    void protectedPath_basicAuth_returns401() {
        var exchange = exchangeForPath("GET", "/api/v1/wallet/balance",
                "Basic dXNlcjpwYXNz");
        addRouteMetadata(exchange, "");

        GatewayFilter gf = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gf.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(jwtConfig, never()).isTokenValid(anyString());
    }

    @Test
    @DisplayName("Protected path with 'Bearer ' only (empty token) — returns 401")
    void protectedPath_bearerPrefixOnly_returns401() {
        var exchange = exchangeForPath("GET", "/api/v1/wallet/balance", "Bearer ");
        addRouteMetadata(exchange, "");

        when(jwtConfig.isTokenValid("")).thenReturn(false);

        GatewayFilter gf = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gf.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Protected paths — invalid token
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Protected path with tampered token — returns 401")
    void protectedPath_tamperedToken_returns401() {
        when(jwtConfig.isTokenValid("tampered.token.here")).thenReturn(false);

        var exchange = exchangeForPath("GET", "/api/v1/wallet/balance",
                "Bearer tampered.token.here");
        addRouteMetadata(exchange, "");

        GatewayFilter gf = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gf.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Protected path with expired token — returns 401")
    void protectedPath_expiredToken_returns401() {
        when(jwtConfig.isTokenValid("expired-token")).thenReturn(false);

        var exchange = exchangeForPath("GET", "/api/v1/transactions/history",
                "Bearer expired-token");
        addRouteMetadata(exchange, "");

        GatewayFilter gf = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gf.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Protected paths — valid token
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Protected wallet path with valid USER token — passes through")
    void protectedWalletPath_validUserToken_passesThrough() {
        String token = buildTestToken("jean@example.com", "USER", 1L);
        when(jwtConfig.isTokenValid(token)).thenReturn(true);
        when(jwtConfig.extractEmail(token)).thenReturn("jean@example.com");
        when(jwtConfig.extractRole(token)).thenReturn("USER");
        when(jwtConfig.extractUserId(token)).thenReturn("1");

        var exchange = exchangeForPath("GET", "/api/v1/wallet/balance",
                "Bearer " + token);
        addRouteMetadata(exchange, "");

        GatewayFilter gf = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gf.filter(exchange, chain)).verifyComplete();

        verify(chain, times(1)).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Admin path with ADMIN token — passes through")
    void adminPath_adminToken_passesThrough() {
        String token = buildTestToken("admin@example.com", "ADMIN", 100L);
        when(jwtConfig.isTokenValid(token)).thenReturn(true);
        when(jwtConfig.extractEmail(token)).thenReturn("admin@example.com");
        when(jwtConfig.extractRole(token)).thenReturn("ADMIN");
        when(jwtConfig.extractUserId(token)).thenReturn("100");

        var exchange = exchangeForPath("GET", "/api/v1/admin/users",
                "Bearer " + token);
        addRouteMetadata(exchange, "");

        GatewayFilter gf = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gf.filter(exchange, chain)).verifyComplete();

        verify(chain, times(1)).filter(any());
    }

    @Test
    @DisplayName("Savings path with MERCHANT token — passes through (role check is downstream)")
    void savingsPath_merchantToken_passesThrough() {
        String token = buildTestToken("merchant@example.com", "MERCHANT", 50L);
        when(jwtConfig.isTokenValid(token)).thenReturn(true);
        when(jwtConfig.extractEmail(token)).thenReturn("merchant@example.com");
        when(jwtConfig.extractRole(token)).thenReturn("MERCHANT");
        when(jwtConfig.extractUserId(token)).thenReturn("50");

        var exchange = exchangeForPath("GET", "/api/v1/savings/groups",
                "Bearer " + token);
        addRouteMetadata(exchange, "");

        GatewayFilter gf = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gf.filter(exchange, chain)).verifyComplete();

        verify(chain, times(1)).filter(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Protected /logout (auth-service, not in public-paths)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Auth /logout is NOT in public-paths — requires valid token")
    void authLogout_notPublic_requiresToken() {
        var exchange = exchangeForPath("POST", "/api/v1/auth/logout", null);
        // public-paths for auth-service does NOT include /logout
        addRouteMetadata(exchange,
                "/api/v1/auth/register,/api/v1/auth/login,/api/v1/auth/refresh," +
                "/api/v1/auth/verify-email,/api/v1/auth/resend-verification," +
                "/api/v1/auth/forgot-password,/api/v1/auth/reset-password");

        GatewayFilter gf = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gf.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // No route metadata at all (route has no public-paths key)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Route with no metadata at all — protected path requires token")
    void noMetadata_protectedPath_requires401() {
        var exchange = exchangeForPath("GET", "/api/v1/wallet/balance", null);
        // no metadata added → no public-paths → everything is protected

        GatewayFilter gf = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gf.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a MockServerWebExchange for the given HTTP method, path, and
     * optional Authorization header value.
     */
    private MockServerWebExchange exchangeForPath(String method, String path,
                                                   String authHeaderValue) {
        MockServerHttpRequest.BaseBuilder<?> builder = switch (method) {
            case "POST"   -> MockServerHttpRequest.post(path);
            case "PUT"    -> MockServerHttpRequest.put(path);
            case "DELETE" -> MockServerHttpRequest.delete(path);
            default       -> MockServerHttpRequest.get(path);
        };

        if (authHeaderValue != null) {
            builder.header(HttpHeaders.AUTHORIZATION, authHeaderValue);
        }

        return MockServerWebExchange.from(((MockServerHttpRequest.BaseBuilder<?>) builder).build());
    }

    /**
     * Injects a Route with the given public-paths metadata string into the
     * exchange attributes, exactly as Spring Cloud Gateway does at runtime.
     *
     * Uses Route.builder() — the correct API for Spring Cloud 2023.x / Gateway 4.x.
     * Route.async() was removed in Gateway 4.0.
     */
    private void addRouteMetadata(MockServerWebExchange exchange, String publicPaths) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("public-paths", publicPaths);

        // Route.builder() is the stable API in Spring Cloud Gateway 4.x
        Route route = Route.builder()
                .id("test-route")
                .uri(URI.create("http://localhost:8081"))
                .predicate(exch -> true)
                .metadata(metadata)
                .build();

        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);
    }

    private String buildTestToken(String email, String role, Long userId) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Map<String, Object> claims = new HashMap<>();
        claims.put("role",     role);
        claims.put("userId",   userId);
        claims.put("fullName", "Test User");
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86_400_000L))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
