package com.example.api_gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * Stateless JWT utility for the API Gateway.
 *
 * The gateway ONLY validates tokens — it never generates them.
 * Generation is exclusively the auth-service's responsibility.
 *
 * The secret key MUST be identical to the one in auth-service
 * application.properties (jwt.secret). Both services share the same
 * signing key so the gateway can verify tokens the auth-service issued.
 *
 * Claims embedded by auth-service generateAccessToken():
 *   sub      -> user email
 *   role     -> e.g. "USER", "MERCHANT", "ADMIN"
 *   fullName -> user's full name
 *   userId   -> user's database id
 *   iat      -> issued at
 *   exp      -> expiration
 */
@Slf4j
@Component
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    /**
     * Returns true when the token is structurally valid, correctly signed
     * by our secret, and has not expired.
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Claims extraction — called by the filter to forward user context
    // ------------------------------------------------------------------

    /** Extracts the email address (JWT subject). */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /** Extracts the role claim (e.g. "USER", "ADMIN"). */
    public String extractRole(String token) {
        return (String) extractAllClaims(token).get("role");
    }

    /** Extracts the userId claim. */
    public String extractUserId(String token) {
        Object userId = extractAllClaims(token).get("userId");
        return userId != null ? userId.toString() : null;
    }

    // ------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
