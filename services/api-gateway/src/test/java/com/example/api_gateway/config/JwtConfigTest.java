package com.example.api_gateway.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtConfig Unit Tests")
class JwtConfigTest {

    private JwtConfig jwtConfig;

    private static final String SECRET =
            "3d7f4a9b2c8e1f5a6d9b3c7e2f8a1d4b7e3c9f2a6b5d8e1c4f7a2b3d6e9f0c1";

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        ReflectionTestUtils.setField(jwtConfig, "secret", SECRET);
    }

    // ------------------------------------------------------------------
    // isTokenValid
    // ------------------------------------------------------------------

    @Test
    @DisplayName("isTokenValid — returns true for a well-formed, non-expired token")
    void isTokenValid_validToken_returnsTrue() {
        String token = buildToken("user@example.com", "USER", 1L, 86_400_000L);
        assertThat(jwtConfig.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid — returns false for an expired token")
    void isTokenValid_expiredToken_returnsFalse() {
        // expiration = -1000ms → already expired when created
        String token = buildToken("user@example.com", "USER", 1L, -1000L);
        assertThat(jwtConfig.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid — returns false for a tampered token")
    void isTokenValid_tamperedToken_returnsFalse() {
        String token = buildToken("user@example.com", "USER", 1L, 86_400_000L) + "tampered";
        assertThat(jwtConfig.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid — returns false for blank string")
    void isTokenValid_blankToken_returnsFalse() {
        assertThat(jwtConfig.isTokenValid("")).isFalse();
    }

    @Test
    @DisplayName("isTokenValid — returns false for a token signed with a different secret")
    void isTokenValid_wrongSecret_returnsFalse() {
        String wrongSecret = "wrongsecretwrongsecretwrongsecretwrongsecret!!";
        Key wrongKey = Keys.hmacShaKeyFor(wrongSecret.getBytes());
        String token = Jwts.builder()
                .setSubject("user@example.com")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86_400_000L))
                .signWith(wrongKey, SignatureAlgorithm.HS256)
                .compact();
        assertThat(jwtConfig.isTokenValid(token)).isFalse();
    }

    // ------------------------------------------------------------------
    // extractEmail
    // ------------------------------------------------------------------

    @Test
    @DisplayName("extractEmail — returns the subject from the token")
    void extractEmail_returnsSubject() {
        String token = buildToken("jean@example.com", "USER", 42L, 86_400_000L);
        assertThat(jwtConfig.extractEmail(token)).isEqualTo("jean@example.com");
    }

    // ------------------------------------------------------------------
    // extractRole
    // ------------------------------------------------------------------

    @Test
    @DisplayName("extractRole — returns the role claim from the token")
    void extractRole_returnsRoleClaim() {
        String token = buildToken("admin@example.com", "ADMIN", 1L, 86_400_000L);
        assertThat(jwtConfig.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("extractRole — returns USER for a USER role token")
    void extractRole_userRole_returnsUser() {
        String token = buildToken("user@example.com", "USER", 5L, 86_400_000L);
        assertThat(jwtConfig.extractRole(token)).isEqualTo("USER");
    }

    @Test
    @DisplayName("extractRole — returns MERCHANT for a MERCHANT role token")
    void extractRole_merchantRole_returnsMerchant() {
        String token = buildToken("merchant@example.com", "MERCHANT", 7L, 86_400_000L);
        assertThat(jwtConfig.extractRole(token)).isEqualTo("MERCHANT");
    }

    // ------------------------------------------------------------------
    // extractUserId
    // ------------------------------------------------------------------

    @Test
    @DisplayName("extractUserId — returns the userId claim as a string")
    void extractUserId_returnsUserId() {
        String token = buildToken("user@example.com", "USER", 99L, 86_400_000L);
        assertThat(jwtConfig.extractUserId(token)).isEqualTo("99");
    }

    // ------------------------------------------------------------------
    // Helper: build a valid test token using the same secret + algorithm
    // as the auth-service JwtService
    // ------------------------------------------------------------------

    private String buildToken(String email, String role, Long userId, long expirationMs) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Map<String, Object> claims = new HashMap<>();
        claims.put("role",     role);
        claims.put("userId",   userId);
        claims.put("fullName", "Test User");
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
