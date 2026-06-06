package com.example.auth_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OtpService Unit Tests")
@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "expirationMinutes", 10);
        ReflectionTestUtils.setField(otpService, "otpLength", 6);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ------------------------------------------------------------------
    // generateAndStore
    // ------------------------------------------------------------------

    @Test
    @DisplayName("generateAndStore returns a 6-digit numeric string")
    void generateAndStore_returnsSixDigitCode() {
        String otp = otpService.generateAndStore(OtpService.PURPOSE_EMAIL_VERIFY, "user@example.com");

        assertThat(otp).hasSize(6);
        assertThat(otp).matches("\\d{6}");
    }

    @Test
    @DisplayName("generateAndStore stores the OTP in Redis with the correct TTL")
    void generateAndStore_storesInRedisWithTtl() {
        String otp = otpService.generateAndStore(OtpService.PURPOSE_EMAIL_VERIFY, "user@example.com");

        verify(valueOps, times(1))
                .set(eq("otp:EMAIL_VERIFY:user@example.com"), eq(otp), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("generateAndStore lowercases the email in the Redis key")
    void generateAndStore_lowercasesEmail() {
        otpService.generateAndStore(OtpService.PURPOSE_EMAIL_VERIFY, "User@Example.COM");

        verify(valueOps).set(contains("user@example.com"), anyString(), anyLong(), any());
    }

    // ------------------------------------------------------------------
    // validate
    // ------------------------------------------------------------------

    @Test
    @DisplayName("validate returns true and deletes key when code matches")
    void validate_correctCode_returnsTrue() {
        when(valueOps.get("otp:EMAIL_VERIFY:user@example.com")).thenReturn("123456");

        boolean result = otpService.validate(OtpService.PURPOSE_EMAIL_VERIFY, "user@example.com", "123456");

        assertThat(result).isTrue();
        verify(redisTemplate).delete("otp:EMAIL_VERIFY:user@example.com");
    }

    @Test
    @DisplayName("validate returns false when code does not match")
    void validate_wrongCode_returnsFalse() {
        when(valueOps.get("otp:EMAIL_VERIFY:user@example.com")).thenReturn("123456");

        boolean result = otpService.validate(OtpService.PURPOSE_EMAIL_VERIFY, "user@example.com", "000000");

        assertThat(result).isFalse();
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("validate returns false when key does not exist in Redis (expired or never set)")
    void validate_noKeyInRedis_returnsFalse() {
        when(valueOps.get("otp:EMAIL_VERIFY:user@example.com")).thenReturn(null);

        boolean result = otpService.validate(OtpService.PURPOSE_EMAIL_VERIFY, "user@example.com", "123456");

        assertThat(result).isFalse();
    }

    // ------------------------------------------------------------------
    // invalidate
    // ------------------------------------------------------------------

    @Test
    @DisplayName("invalidate deletes the correct Redis key")
    void invalidate_deletesKey() {
        otpService.invalidate(OtpService.PURPOSE_PASSWORD_RESET, "user@example.com");

        verify(redisTemplate).delete("otp:PASSWORD_RESET:user@example.com");
    }
}