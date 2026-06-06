package com.example.auth_service.security;

import com.example.auth_service.entity.User;
import com.example.auth_service.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET =
            "3d7f4a9b2c8e1f5a6d9b3c7e2f8a1d4b7e3c9f2a6b5d8e1c4f7a2b3d6e9f0c1";
    private static final long ACCESS_EXP  = 86_400_000L; // 24h
    private static final long REFRESH_EXP = 604_800_000L; // 7 days

    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration",  ACCESS_EXP);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", REFRESH_EXP);

        testUser = User.builder()
                .id(1L)
                .fullName("Test User")
                .email("test@example.com")
                .phoneNumber("+237600000001")
                .password("hashed_password")
                .role(UserRole.USER)
                .enabled(true)
                .emailVerified(true)
                .build();
    }

    // ------------------------------------------------------------------
    // generateAccessToken
    // ------------------------------------------------------------------

    @Test
    @DisplayName("generateAccessToken returns a non-null, non-blank string")
    void generateAccessToken_returnsToken() {
        String token = jwtService.generateAccessToken(testUser);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("extractEmail from access token returns the user's email")
    void extractEmail_fromAccessToken_returnsEmail() {
        String token = jwtService.generateAccessToken(testUser);
        String extracted = jwtService.extractEmail(token);
        assertThat(extracted).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("generateRefreshTokenString returns a non-null, non-blank string")
    void generateRefreshToken_returnsToken() {
        String token = jwtService.generateRefreshTokenString(testUser);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("Access token and refresh token are different strings")
    void accessAndRefreshTokensAreDifferent() {
        String access  = jwtService.generateAccessToken(testUser);
        String refresh = jwtService.generateRefreshTokenString(testUser);
        assertThat(access).isNotEqualTo(refresh);
    }

    // ------------------------------------------------------------------
    // isTokenValid
    // ------------------------------------------------------------------

    @Test
    @DisplayName("isTokenValid returns true for a valid, non-expired token")
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtService.generateAccessToken(testUser);
        UserDetails userDetails = buildUserDetails(testUser.getEmail());
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid returns false when email does not match")
    void isTokenValid_wrongEmail_returnsFalse() {
        String token = jwtService.generateAccessToken(testUser);
        UserDetails wrongUser = buildUserDetails("other@example.com");
        assertThat(jwtService.isTokenValid(token, wrongUser)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid returns false for a tampered token")
    void isTokenValid_tamperedToken_returnsFalse() {
        String token = jwtService.generateAccessToken(testUser) + "tampered";
        UserDetails userDetails = buildUserDetails(testUser.getEmail());
        assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid returns false for a blank string")
    void isTokenValid_blankToken_returnsFalse() {
        UserDetails userDetails = buildUserDetails(testUser.getEmail());
        assertThat(jwtService.isTokenValid("", userDetails)).isFalse();
    }

    // ------------------------------------------------------------------
    // isTokenExpired
    // ------------------------------------------------------------------

    @Test
    @DisplayName("isTokenExpired returns false for a freshly issued token")
    void isTokenExpired_freshToken_returnsFalse() {
        String token = jwtService.generateAccessToken(testUser);
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    // ------------------------------------------------------------------
    // getAccessTokenExpiration
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getAccessTokenExpiration returns configured value")
    void getAccessTokenExpiration_returnsConfiguredValue() {
        assertThat(jwtService.getAccessTokenExpiration()).isEqualTo(ACCESS_EXP);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private UserDetails buildUserDetails(String email) {
        return org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password("irrelevant")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }
}
