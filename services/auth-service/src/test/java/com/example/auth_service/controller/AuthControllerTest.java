package com.example.auth_service.controller;

import com.example.auth_service.dto.*;
import com.example.auth_service.enums.UserRole;
import com.example.auth_service.exception.*;
import com.example.auth_service.security.CustomUserDetailsService;
import com.example.auth_service.security.JwtAuthenticationFilter;
import com.example.auth_service.security.JwtService;
import com.example.auth_service.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(com.example.auth_service.config.SecurityConfig.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private JwtService jwtService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    private AuthResponse sampleAuthResponse;

    @BeforeEach
    void setUp() {
        sampleAuthResponse = AuthResponse.builder()
                .accessToken("test-access-token")
                .refreshToken("test-refresh-token")
                .tokenType("Bearer")
                .userId(1L)
                .fullName("Jean Dupont")
                .email("jean@example.com")
                .role(UserRole.USER)
                .expiresIn(86_400_000L)
                .build();
    }

    // ================================================================
    // POST /register
    // ================================================================

    @Test
    @DisplayName("POST /register — 201 on valid input")
    void register_validInput_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jean Dupont");
        request.setEmail("jean@example.com");
        request.setPhoneNumber("+237600000001");
        request.setPassword("password123");

        when(authService.register(any(RegisterRequest.class))).thenReturn(sampleAuthResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.email").value("jean@example.com"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /register — 400 when fullName is blank")
    void register_blankFullName_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("");
        request.setEmail("jean@example.com");
        request.setPhoneNumber("+237600000001");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists());
    }

    @Test
    @DisplayName("POST /register — 400 when email is invalid")
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jean Dupont");
        request.setEmail("not-an-email");
        request.setPhoneNumber("+237600000001");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("POST /register — 400 when password is too short")
    void register_shortPassword_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jean Dupont");
        request.setEmail("jean@example.com");
        request.setPhoneNumber("+237600000001");
        request.setPassword("short");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("POST /register — 409 when email is already taken")
    void register_emailTaken_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jean Dupont");
        request.setEmail("jean@example.com");
        request.setPhoneNumber("+237600000001");
        request.setPassword("password123");

        when(authService.register(any())).thenThrow(
                new EmailAlreadyExistsException("Email already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email Already Exists"));
    }

    // ================================================================
    // POST /login
    // ================================================================

    @Test
    @DisplayName("POST /login — 200 on valid credentials")
    void login_validCredentials_returns200() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("jean@example.com");
        request.setPassword("password123");

        when(authService.login(any(LoginRequest.class))).thenReturn(sampleAuthResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("test-refresh-token"));
    }

    @Test
    @DisplayName("POST /login — 401 on wrong credentials")
    void login_wrongCredentials_returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("jean@example.com");
        request.setPassword("wrongpassword");

        when(authService.login(any())).thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication Failed"));
    }

    @Test
    @DisplayName("POST /login — 400 when email field is missing")
    void login_missingEmail_returns400() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setPassword("password123");
        // email not set

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ================================================================
    // POST /refresh
    // ================================================================

    @Test
    @DisplayName("POST /refresh — 200 on valid refresh token")
    void refresh_validToken_returns200() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        when(authService.refreshToken(any())).thenReturn(sampleAuthResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"));
    }

    @Test
    @DisplayName("POST /refresh — 401 on invalid or revoked token")
    void refresh_invalidToken_returns401() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("bad-token");

        when(authService.refreshToken(any())).thenThrow(new InvalidTokenException("Token revoked"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid Token"));
    }

    // ================================================================
    // POST /logout (protected)
    // ================================================================

    @Test
    @DisplayName("POST /logout — 200 when authenticated")
    @WithMockUser(username = "jean@example.com")
    void logout_authenticated_returns200() throws Exception {
        doNothing().when(authService).logout("jean@example.com");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    @DisplayName("POST /logout — 401 when not authenticated")
    void logout_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    // GET /me (protected)
    // ================================================================

    @Test
    @DisplayName("GET /me — 200 when authenticated")
    @WithMockUser(username = "jean@example.com")
    void me_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Authenticated as: jean@example.com"));
    }

    @Test
    @DisplayName("GET /me — 401 when not authenticated")
    void me_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    // POST /verify-email
    // ================================================================

    @Test
    @DisplayName("POST /verify-email — 200 on valid OTP")
    void verifyEmail_validOtp_returns200() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("jean@example.com");
        request.setOtp("123456");

        when(authService.verifyEmail(any())).thenReturn(
                new MessageResponse("Email verified successfully. Welcome to Smart Banking!"));

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully. Welcome to Smart Banking!"));
    }

    @Test
    @DisplayName("POST /verify-email — 400 on wrong OTP")
    void verifyEmail_wrongOtp_returns400() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("jean@example.com");
        request.setOtp("000000");

        when(authService.verifyEmail(any())).thenThrow(
                new OtpInvalidException("OTP is invalid or has expired"));

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid OTP"));
    }

    // ================================================================
    // POST /forgot-password
    // ================================================================

    @Test
    @DisplayName("POST /forgot-password — always returns 200 (security)")
    void forgotPassword_alwaysReturns200() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("anyone@example.com");

        when(authService.forgotPassword(any())).thenReturn(
                new MessageResponse("If an account with that email exists, a reset code has been sent."));

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    // ================================================================
    // POST /change-password (protected)
    // ================================================================

    @Test
    @DisplayName("POST /change-password — 200 when authenticated and current password is correct")
    @WithMockUser(username = "jean@example.com")
    void changePassword_success_returns200() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("password123");
        request.setNewPassword("newpassword123");

        when(authService.changePassword(eq("jean@example.com"), any()))
                .thenReturn(new MessageResponse("Password changed successfully. Please login again."));

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully. Please login again."));
    }

    @Test
    @DisplayName("POST /change-password — 401 when not authenticated")
    void changePassword_unauthenticated_returns401() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("password123");
        request.setNewPassword("newpassword123");

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}