package com.example.auth_service.controller;

import com.example.auth_service.dto.*;
import com.example.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ================================================================
    // POST /api/v1/auth/register
    // Public. Creates account, sends email-verification OTP, returns tokens.
    // ================================================================
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    // ================================================================
    // POST /api/v1/auth/login
    // Public. Returns access + refresh tokens.
    // ================================================================
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ================================================================
    // POST /api/v1/auth/refresh
    // Public. Exchanges a valid refresh token for a new token pair.
    // ================================================================
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    // ================================================================
    // POST /api/v1/auth/logout
    // Protected. Revokes all refresh tokens for the current user.
    // ================================================================
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @AuthenticationPrincipal UserDetails userDetails) {
        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    // ================================================================
    // GET /api/v1/auth/me
    // Protected. Returns the authenticated user identity.
    // Used by the API Gateway to validate tokens.
    // ================================================================
    @GetMapping("/me")
    public ResponseEntity<MessageResponse> me(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                new MessageResponse("Authenticated as: " + userDetails.getUsername()));
    }

    // ================================================================
    // POST /api/v1/auth/verify-email
    // Public. Verifies the 6-digit OTP sent after registration.
    // ================================================================
    @PostMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    // ================================================================
    // POST /api/v1/auth/resend-verification
    // Public. Resends the email-verification OTP.
    // ================================================================
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(
            @Valid @RequestBody ResendOtpRequest request) {
        return ResponseEntity.ok(authService.resendVerificationOtp(request));
    }

    // ================================================================
    // POST /api/v1/auth/forgot-password
    // Public. Sends a password-reset OTP to the given email.
    // Always returns 200 (never reveals whether email exists).
    // ================================================================
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    // ================================================================
    // POST /api/v1/auth/reset-password
    // Public. Validates OTP and sets a new password.
    // ================================================================
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    // ================================================================
    // POST /api/v1/auth/change-password
    // Protected. Authenticated user changes their own password
    //            by supplying the current password and a new one.
    // ================================================================
    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(
                authService.changePassword(userDetails.getUsername(), request));
    }
}
