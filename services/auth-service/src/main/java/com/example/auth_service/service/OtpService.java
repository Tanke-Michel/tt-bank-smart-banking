package com.example.auth_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * Manages one-time passwords (OTPs) for email verification and login.
 *
 * Storage strategy: Redis with TTL.
 *   Key format : "otp:<purpose>:<email>"
 *   Value      : 6-digit numeric string
 *   TTL        : otp.expiration-minutes (default 10)
 *
 * Purposes:
 *   - EMAIL_VERIFY  : sent after registration
 *   - PASSWORD_RESET: sent when user requests password reset
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final String OTP_PREFIX = "otp:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${otp.expiration-minutes:10}")
    private int expirationMinutes;

    @Value("${otp.length:6}")
    private int otpLength;

    // -----------------------------------------------
    // Generate and store
    // -----------------------------------------------

    /**
     * Generates a new OTP for the given purpose and email, stores it in Redis,
     * and returns the code so the caller (EmailService) can send it.
     */
    public String generateAndStore(String purpose, String email) {
        String otp = generateCode();
        String key = buildKey(purpose, email);

        redisTemplate.opsForValue().set(key, otp, expirationMinutes, TimeUnit.MINUTES);
        log.debug("OTP stored for purpose={} email={} expiresInMinutes={}", purpose, email, expirationMinutes);

        return otp;
    }

    // -----------------------------------------------
    // Validate
    // -----------------------------------------------

    /**
     * Returns true if the supplied code matches the stored OTP and has not expired.
     * Always deletes the stored OTP after a successful match (single-use).
     */
    public boolean validate(String purpose, String email, String code) {
        String key = buildKey(purpose, email);
        String stored = redisTemplate.opsForValue().get(key);

        if (stored == null) {
            log.warn("OTP validation failed — not found or expired: purpose={} email={}", purpose, email);
            return false;
        }

        if (!stored.equals(code)) {
            log.warn("OTP validation failed — wrong code: purpose={} email={}", purpose, email);
            return false;
        }

        // Delete immediately after use — OTPs are single-use
        redisTemplate.delete(key);
        log.debug("OTP validated and deleted: purpose={} email={}", purpose, email);
        return true;
    }

    // -----------------------------------------------
    // Invalidate (e.g. user requests a new code)
    // -----------------------------------------------

    public void invalidate(String purpose, String email) {
        String key = buildKey(purpose, email);
        redisTemplate.delete(key);
        log.debug("OTP invalidated: purpose={} email={}", purpose, email);
    }

    // -----------------------------------------------
    // Helpers
    // -----------------------------------------------

    private String generateCode() {
        int max = (int) Math.pow(10, otpLength);
        int code = SECURE_RANDOM.nextInt(max);
        // Zero-pad to ensure the code is always exactly otpLength digits
        return String.format("%0" + otpLength + "d", code);
    }

    private String buildKey(String purpose, String email) {
        return OTP_PREFIX + purpose + ":" + email.toLowerCase();
    }

    // -----------------------------------------------
    // Purpose constants — used by callers
    // -----------------------------------------------

    public static final String PURPOSE_EMAIL_VERIFY  = "EMAIL_VERIFY";
    public static final String PURPOSE_PASSWORD_RESET = "PASSWORD_RESET";
}
