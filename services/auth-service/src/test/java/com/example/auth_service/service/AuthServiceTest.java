package com.example.auth_service.service;

import com.example.auth_service.dto.*;
import com.example.auth_service.entity.RefreshToken;
import com.example.auth_service.entity.User;
import com.example.auth_service.enums.UserRole;
import com.example.auth_service.exception.*;
import com.example.auth_service.repository.RefreshTokenRepository;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("AuthService Unit Tests")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private OtpService otpService;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User savedUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604_800_000L);

        savedUser = User.builder()
                .id(1L)
                .fullName("Jean Dupont")
                .email("jean@example.com")
                .phoneNumber("+237600000001")
                .password("$2a$12$hashed")
                .role(UserRole.USER)
                .enabled(true)
                .emailVerified(false)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setFullName("Jean Dupont");
        registerRequest.setEmail("jean@example.com");
        registerRequest.setPhoneNumber("+237600000001");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("jean@example.com");
        loginRequest.setPassword("password123");
    }

    // ================================================================
    // REGISTER
    // ================================================================

    @Test
    @DisplayName("register — success path returns AuthResponse with tokens")
    void register_success_returnsAuthResponse() {
        when(userRepository.existsByEmail("jean@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+237600000001")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(savedUser)).thenReturn("access-token");
        when(jwtService.generateRefreshTokenString(savedUser)).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(86_400_000L);
        when(otpService.generateAndStore(anyString(), anyString())).thenReturn("123456");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getEmail()).isEqualTo("jean@example.com");
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        verify(otpService).generateAndStore(OtpService.PURPOSE_EMAIL_VERIFY, "jean@example.com");
        verify(emailService).sendEmailVerificationOtp(eq("jean@example.com"), anyString(), eq("123456"));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("register — throws EmailAlreadyExistsException when email is taken")
    void register_emailTaken_throwsException() {
        when(userRepository.existsByEmail("jean@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("jean@example.com");

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendEmailVerificationOtp(any(), any(), any());
    }

    @Test
    @DisplayName("register — throws PhoneNumberAlreadyExistsException when phone is taken")
    void register_phoneTaken_throwsException() {
        when(userRepository.existsByEmail("jean@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+237600000001")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(PhoneNumberAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    // ================================================================
    // LOGIN
    // ================================================================

    @Test
    @DisplayName("login — success path returns AuthResponse")
    void login_success_returnsAuthResponse() {
        when(userRepository.findByEmail("jean@example.com")).thenReturn(Optional.of(savedUser));
        when(jwtService.generateAccessToken(savedUser)).thenReturn("access-token");
        when(jwtService.generateRefreshTokenString(savedUser)).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(86_400_000L);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getEmail()).isEqualTo("jean@example.com");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenRepository).revokeAllUserTokens(savedUser);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("login — throws BadCredentialsException when authentication fails")
    void login_wrongCredentials_throwsException() {
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmail(any());
    }

    // ================================================================
    // REFRESH TOKEN
    // ================================================================

    @Test
    @DisplayName("refreshToken — success path issues new token pair")
    void refreshToken_valid_returnsNewTokens() {
        RefreshToken stored = RefreshToken.builder()
                .id(1L)
                .token("valid-refresh-token")
                .user(savedUser)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(stored));
        when(jwtService.generateAccessToken(savedUser)).thenReturn("new-access");
        when(jwtService.generateRefreshTokenString(savedUser)).thenReturn("new-refresh");
        when(jwtService.getAccessTokenExpiration()).thenReturn(86_400_000L);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        assertThat(stored.isRevoked()).isTrue(); // old token was revoked
        verify(refreshTokenRepository, times(2)).save(any()); // save revocation + save new token
    }

    @Test
    @DisplayName("refreshToken — throws InvalidTokenException when token not found")
    void refreshToken_notFound_throwsException() {
        when(refreshTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("bad-token");

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("refreshToken — throws InvalidTokenException when token is revoked")
    void refreshToken_revoked_throwsException() {
        RefreshToken revoked = RefreshToken.builder()
                .token("revoked-token")
                .user(savedUser)
                .revoked(true)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(revoked));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("revoked-token");

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("refreshToken — throws InvalidTokenException when token is expired")
    void refreshToken_expired_throwsException() {
        RefreshToken expired = RefreshToken.builder()
                .token("expired-token")
                .user(savedUser)
                .revoked(false)
                .expiresAt(LocalDateTime.now().minusDays(1)) // in the past
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired-token");

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    // ================================================================
    // LOGOUT
    // ================================================================

    @Test
    @DisplayName("logout — revokes all tokens for the user")
    void logout_success_revokesAllTokens() {
        when(userRepository.findByEmail("jean@example.com")).thenReturn(Optional.of(savedUser));

        authService.logout("jean@example.com");

        verify(refreshTokenRepository).revokeAllUserTokens(savedUser);
    }

    @Test
    @DisplayName("logout — throws UserNotFoundException when user not found")
    void logout_unknownUser_throwsException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout("unknown@example.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ================================================================
    // VERIFY EMAIL
    // ================================================================

    @Test
    @DisplayName("verifyEmail — success marks emailVerified and sends welcome email")
    void verifyEmail_success() {
        when(userRepository.findByEmail("jean@example.com")).thenReturn(Optional.of(savedUser));
        when(otpService.validate(OtpService.PURPOSE_EMAIL_VERIFY, "jean@example.com", "123456"))
                .thenReturn(true);

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("jean@example.com");
        request.setOtp("123456");

        MessageResponse response = authService.verifyEmail(request);

        assertThat(response.getMessage()).contains("verified");
        assertThat(savedUser.isEmailVerified()).isTrue();
        verify(userRepository).save(savedUser);
        verify(emailService).sendWelcomeEmail("jean@example.com", savedUser.getFullName());
    }

    @Test
    @DisplayName("verifyEmail — throws OtpInvalidException when OTP is wrong")
    void verifyEmail_wrongOtp_throwsException() {
        when(userRepository.findByEmail("jean@example.com")).thenReturn(Optional.of(savedUser));
        when(otpService.validate(anyString(), anyString(), anyString())).thenReturn(false);

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("jean@example.com");
        request.setOtp("000000");

        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(OtpInvalidException.class);

        assertThat(savedUser.isEmailVerified()).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("verifyEmail — returns early if already verified")
    void verifyEmail_alreadyVerified_returnsEarlyMessage() {
        savedUser.setEmailVerified(true);
        when(userRepository.findByEmail("jean@example.com")).thenReturn(Optional.of(savedUser));

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("jean@example.com");
        request.setOtp("123456");

        MessageResponse response = authService.verifyEmail(request);

        assertThat(response.getMessage()).contains("already verified");
        verify(otpService, never()).validate(any(), any(), any());
    }

    // ================================================================
    // FORGOT PASSWORD
    // ================================================================

    @Test
    @DisplayName("forgotPassword — sends OTP when user exists")
    void forgotPassword_existingUser_sendsOtp() {
        when(userRepository.findByEmail("jean@example.com")).thenReturn(Optional.of(savedUser));
        when(otpService.generateAndStore(OtpService.PURPOSE_PASSWORD_RESET, "jean@example.com"))
                .thenReturn("654321");

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("jean@example.com");

        MessageResponse response = authService.forgotPassword(request);

        assertThat(response.getMessage()).contains("reset code has been sent");
        verify(otpService).generateAndStore(OtpService.PURPOSE_PASSWORD_RESET, "jean@example.com");
        verify(emailService).sendPasswordResetOtp(eq("jean@example.com"), anyString(), eq("654321"));
    }

    @Test
    @DisplayName("forgotPassword — returns generic message even for unknown email (security)")
    void forgotPassword_unknownEmail_returnsGenericMessage() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("unknown@example.com");

        MessageResponse response = authService.forgotPassword(request);

        assertThat(response.getMessage()).contains("reset code has been sent");
        verify(otpService, never()).generateAndStore(any(), any());
        verify(emailService, never()).sendPasswordResetOtp(any(), any(), any());
    }

    // ================================================================
    // RESET PASSWORD
    // ================================================================

    @Test
    @DisplayName("resetPassword — success sets new password and revokes tokens")
    void resetPassword_success() {
        when(userRepository.findByEmail("jean@example.com")).thenReturn(Optional.of(savedUser));
        when(otpService.validate(OtpService.PURPOSE_PASSWORD_RESET, "jean@example.com", "654321"))
                .thenReturn(true);
        when(passwordEncoder.encode("newpassword123")).thenReturn("$2a$12$newHashed");

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("jean@example.com");
        request.setOtp("654321");
        request.setNewPassword("newpassword123");

        MessageResponse response = authService.resetPassword(request);

        assertThat(response.getMessage()).contains("reset successfully");
        assertThat(savedUser.getPassword()).isEqualTo("$2a$12$newHashed");
        verify(userRepository).save(savedUser);
        verify(refreshTokenRepository).revokeAllUserTokens(savedUser);
        verify(emailService).sendPasswordChangedEmail("jean@example.com", savedUser.getFullName());
    }

    @Test
    @DisplayName("resetPassword — throws OtpInvalidException when OTP is wrong")
    void resetPassword_wrongOtp_throwsException() {
        when(userRepository.findByEmail("jean@example.com")).thenReturn(Optional.of(savedUser));
        when(otpService.validate(anyString(), anyString(), anyString())).thenReturn(false);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("jean@example.com");
        request.setOtp("000000");
        request.setNewPassword("newpassword123");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(OtpInvalidException.class);

        verify(userRepository, never()).save(any());
    }

    // ================================================================
    // CHANGE PASSWORD
    // ================================================================

    @Test
    @DisplayName("changePassword — success updates password for authenticated user")
    void changePassword_success() {
        when(userRepository.findByEmail("jean@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password123", "$2a$12$hashed")).thenReturn(true);
        when(passwordEncoder.encode("newpassword123")).thenReturn("$2a$12$newHashed");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("password123");
        request.setNewPassword("newpassword123");

        MessageResponse response = authService.changePassword("jean@example.com", request);

        assertThat(response.getMessage()).contains("Password changed");
        assertThat(savedUser.getPassword()).isEqualTo("$2a$12$newHashed");
        verify(refreshTokenRepository).revokeAllUserTokens(savedUser);
        verify(emailService).sendPasswordChangedEmail("jean@example.com", savedUser.getFullName());
    }

    @Test
    @DisplayName("changePassword — throws BadCredentialsException when current password is wrong")
    void changePassword_wrongCurrentPassword_throwsException() {
        when(userRepository.findByEmail("jean@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("wrongpassword", "$2a$12$hashed")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongpassword");
        request.setNewPassword("newpassword123");

        assertThatThrownBy(() -> authService.changePassword("jean@example.com", request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(userRepository, never()).save(any());
    }
}
