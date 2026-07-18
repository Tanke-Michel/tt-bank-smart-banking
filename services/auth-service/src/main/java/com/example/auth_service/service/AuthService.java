package com.example.auth_service.service;

import com.example.auth_service.dto.*;
import com.example.auth_service.entity.RefreshToken;
import com.example.auth_service.entity.User;
import com.example.auth_service.exception.*;
import com.example.auth_service.repository.RefreshTokenRepository;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final EmailService emailService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    // ================================================================
    // REGISTER
    // ================================================================

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "An account with email '" + request.getEmail() + "' already exists");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new PhoneNumberAlreadyExistsException(
                    "An account with phone number '" + request.getPhoneNumber() + "' already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);
        log.info("User saved id={}", savedUser.getId());

        // Send email verification OTP asynchronously
        String otp = otpService.generateAndStore(OtpService.PURPOSE_EMAIL_VERIFY, savedUser.getEmail());
        emailService.sendEmailVerificationOtp(savedUser.getEmail(), savedUser.getFullName(), otp);

        // Issue tokens — user can use the app while email is being verified
        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshTokenString = jwtService.generateRefreshTokenString(savedUser);
        saveRefreshToken(savedUser, refreshTokenString);

        return buildAuthResponse(savedUser, accessToken, refreshTokenString);
    }

    // ================================================================
    // LOGIN
    // ================================================================

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt: {}", request.getEmail());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase().trim(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            log.warn("Failed login: {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        refreshTokenRepository.revokeAllUserTokens(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenString = jwtService.generateRefreshTokenString(user);
        saveRefreshToken(user, refreshTokenString);

        log.info("Login success: {}", user.getEmail());
        return buildAuthResponse(user, accessToken, refreshTokenString);
    }

    // ================================================================
    // REFRESH TOKEN
    // ================================================================

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (stored.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }
        if (stored.isExpired()) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new InvalidTokenException("Refresh token has expired. Please login again");
        }

        User user = stored.getUser();

        // One-time use — revoke the consumed token
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String newAccess = jwtService.generateAccessToken(user);
        String newRefresh = jwtService.generateRefreshTokenString(user);
        saveRefreshToken(user, newRefresh);

        log.info("Tokens refreshed: {}", user.getEmail());
        return buildAuthResponse(user, newAccess, newRefresh);
    }

    // ================================================================
    // LOGOUT
    // ================================================================

    @Transactional
    public void logout(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userEmail));
        refreshTokenRepository.revokeAllUserTokens(user);
        log.info("Logout: all refresh tokens revoked for {}", userEmail);
    }

    // ================================================================
    // VERIFY EMAIL (OTP)
    // ================================================================

    @Transactional
    public MessageResponse verifyEmail(VerifyEmailRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));

        if (user.isEmailVerified()) {
            return new MessageResponse("Email is already verified");
        }

        boolean valid = otpService.validate(OtpService.PURPOSE_EMAIL_VERIFY, email, request.getOtp());
        if (!valid) {
            throw new OtpInvalidException("OTP is invalid or has expired. Please request a new one.");
        }

        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Email verified: {}", email);

        // Send welcome email asynchronously
        emailService.sendWelcomeEmail(email, user.getFullName());

        return new MessageResponse("Email verified successfully. Welcome to Smart Banking!");
    }

    // ================================================================
    // RESEND EMAIL VERIFICATION OTP
    // ================================================================

    @Transactional
    public MessageResponse resendVerificationOtp(ResendOtpRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));

        if (user.isEmailVerified()) {
            return new MessageResponse("Email is already verified");
        }

        // Invalidate any previous OTP before issuing a new one
        otpService.invalidate(OtpService.PURPOSE_EMAIL_VERIFY, email);

        String otp = otpService.generateAndStore(OtpService.PURPOSE_EMAIL_VERIFY, email);
        emailService.sendEmailVerificationOtp(email, user.getFullName(), otp);

        log.info("Verification OTP resent to: {}", email);
        return new MessageResponse("A new verification code has been sent to your email");
    }

    // ================================================================
    // FORGOT PASSWORD (request OTP)
    // ================================================================

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        // Always return success — never reveal whether the email exists
        userRepository.findByEmail(email).ifPresent(user -> {
            otpService.invalidate(OtpService.PURPOSE_PASSWORD_RESET, email);
            String otp = otpService.generateAndStore(OtpService.PURPOSE_PASSWORD_RESET, email);
            emailService.sendPasswordResetOtp(email, user.getFullName(), otp);
            log.info("Password reset OTP sent to: {}", email);
        });

        return new MessageResponse(
                "If an account with that email exists, a reset code has been sent.");
    }

    // ================================================================
    // RESET PASSWORD (with OTP)
    // ================================================================

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));

        boolean valid = otpService.validate(OtpService.PURPOSE_PASSWORD_RESET, email, request.getOtp());
        if (!valid) {
            throw new OtpInvalidException("OTP is invalid or has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all active refresh tokens — forces re-login with new password
        refreshTokenRepository.revokeAllUserTokens(user);

        emailService.sendPasswordChangedEmail(email, user.getFullName());
        log.info("Password reset completed: {}", email);

        return new MessageResponse("Password reset successfully. Please login with your new password.");
    }

    // ================================================================
    // CHANGE PASSWORD (authenticated user knows current password)
    // ================================================================

    @Transactional
    public MessageResponse changePassword(String userEmail, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userEmail));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens — forces re-login on other devices
        refreshTokenRepository.revokeAllUserTokens(user);

        emailService.sendPasswordChangedEmail(userEmail, user.getFullName());
        log.info("Password changed by authenticated user: {}", userEmail);

        return new MessageResponse("Password changed successfully. Please login again.");
    }

    // ================================================================
    // Private helpers
    // ================================================================

    private void saveRefreshToken(User user, String tokenString) {
        RefreshToken rt = RefreshToken.builder()
                .token(tokenString)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();
        refreshTokenRepository.save(rt);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .expiresIn(jwtService.getAccessTokenExpiration())
                .build();
    }
}
