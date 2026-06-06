package com.example.wallet_service.controller;

import com.example.wallet_service.config.GatewayAuthenticationFilter;
import com.example.wallet_service.config.SecurityConfig;
import com.example.wallet_service.dto.*;
import com.example.wallet_service.enums.Currency;
import com.example.wallet_service.enums.TransactionType;
import com.example.wallet_service.enums.WalletStatus;
import com.example.wallet_service.exception.*;
import com.example.wallet_service.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@Import({SecurityConfig.class, GatewayAuthenticationFilter.class})
@DisplayName("WalletController Integration Tests")
class WalletControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private WalletService walletService;

    private WalletResponse sampleWallet;
    private WalletTransactionResponse sampleTxn;

    @BeforeEach
    void setUp() {
        sampleWallet = WalletResponse.builder()
                .id(1L).userId(42L)
                .walletNumber("WLT-20240101-ABCD1234")
                .ownerName("Jean Dupont")
                .email("jean@example.com")
                .phoneNumber("+237600000001")
                .balance(new BigDecimal("5000.00"))
                .currency(Currency.XAF)
                .status(WalletStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        sampleTxn = WalletTransactionResponse.builder()
                .id(1L).type(TransactionType.DEPOSIT)
                .amount(new BigDecimal("1000.00"))
                .balanceBefore(new BigDecimal("5000.00"))
                .balanceAfter(new BigDecimal("6000.00"))
                .currency(Currency.XAF)
                .referenceCode("DEP-20240101-ABCD1234")
                .description("Test deposit")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // Helper: add gateway headers simulating an authenticated request
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    withGatewayHeaders(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req) {
        return req
                .header("X-Auth-User-Id",    "42")
                .header("X-Auth-User-Email", "jean@example.com")
                .header("X-Auth-User-Role",  "USER");
    }

    // ================================================================
    // GET /api/v1/wallet/me
    // ================================================================

    @Test
    @DisplayName("GET /me — 200 with gateway headers")
    void getMyWallet_withGatewayHeaders_returns200() throws Exception {
        when(walletService.getMyWallet(42L)).thenReturn(sampleWallet);

        mockMvc.perform(withGatewayHeaders(get("/api/v1/wallet/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletNumber").value("WLT-20240101-ABCD1234"))
                .andExpect(jsonPath("$.balance").value(5000.00))
                .andExpect(jsonPath("$.currency").value("XAF"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /me — 401 without gateway headers")
    void getMyWallet_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/wallet/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /me — 404 when wallet does not exist")
    void getMyWallet_notFound_returns404() throws Exception {
        when(walletService.getMyWallet(42L))
                .thenThrow(new WalletNotFoundException("No wallet found"));

        mockMvc.perform(withGatewayHeaders(get("/api/v1/wallet/me")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Wallet Not Found"));
    }

    // ================================================================
    // GET /api/v1/wallet/number/{walletNumber}
    // ================================================================

    @Test
    @DisplayName("GET /number/{walletNumber} — 200 for known wallet")
    void getByWalletNumber_returns200() throws Exception {
        when(walletService.getWalletByNumber("WLT-20240101-ABCD1234"))
                .thenReturn(sampleWallet);

        mockMvc.perform(withGatewayHeaders(get("/api/v1/wallet/number/WLT-20240101-ABCD1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletNumber").value("WLT-20240101-ABCD1234"));
    }

    @Test
    @DisplayName("GET /number/{walletNumber} — 404 for unknown wallet")
    void getByWalletNumber_notFound_returns404() throws Exception {
        when(walletService.getWalletByNumber("WLT-UNKNOWN"))
                .thenThrow(new WalletNotFoundException("Not found"));

        mockMvc.perform(withGatewayHeaders(get("/api/v1/wallet/number/WLT-UNKNOWN")))
                .andExpect(status().isNotFound());
    }

    // ================================================================
    // POST /api/v1/wallet/deposit
    // ================================================================

    @Test
    @DisplayName("POST /deposit — 200 on valid request")
    void deposit_valid_returns200() throws Exception {
        when(walletService.deposit(eq(42L), any(DepositRequest.class))).thenReturn(sampleTxn);

        DepositRequest req = new DepositRequest();
        req.setAmount(new BigDecimal("1000.00"));
        req.setDescription("Test deposit");

        mockMvc.perform(withGatewayHeaders(post("/api/v1/wallet/deposit"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1000.00))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.referenceCode").value("DEP-20240101-ABCD1234"));
    }

    @Test
    @DisplayName("POST /deposit — 400 when amount is missing")
    void deposit_missingAmount_returns400() throws Exception {
        DepositRequest req = new DepositRequest();
        req.setDescription("Test");
        // amount not set

        mockMvc.perform(withGatewayHeaders(post("/api/v1/wallet/deposit"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    @DisplayName("POST /deposit — 422 when limit exceeded")
    void deposit_limitExceeded_returns422() throws Exception {
        when(walletService.deposit(eq(42L), any()))
                .thenThrow(new LimitExceededException("Exceeds max deposit"));

        DepositRequest req = new DepositRequest();
        req.setAmount(new BigDecimal("99999999.00"));
        req.setDescription("Too much");

        mockMvc.perform(withGatewayHeaders(post("/api/v1/wallet/deposit"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Limit Exceeded"));
    }

    // ================================================================
    // POST /api/v1/wallet/withdraw
    // ================================================================

    @Test
    @DisplayName("POST /withdraw — 200 on valid request")
    void withdraw_valid_returns200() throws Exception {
        WalletTransactionResponse wdrTxn = WalletTransactionResponse.builder()
                .id(2L).type(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("500.00"))
                .balanceBefore(new BigDecimal("5000.00"))
                .balanceAfter(new BigDecimal("4500.00"))
                .currency(Currency.XAF)
                .referenceCode("WDR-20240101-EFGH5678")
                .description("Test withdrawal")
                .createdAt(LocalDateTime.now())
                .build();

        when(walletService.withdraw(eq(42L), any(WithdrawRequest.class))).thenReturn(wdrTxn);

        WithdrawRequest req = new WithdrawRequest();
        req.setAmount(new BigDecimal("500.00"));
        req.setDescription("Test withdrawal");

        mockMvc.perform(withGatewayHeaders(post("/api/v1/wallet/withdraw"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.balanceAfter").value(4500.00));
    }

    @Test
    @DisplayName("POST /withdraw — 422 on insufficient funds")
    void withdraw_insufficientFunds_returns422() throws Exception {
        when(walletService.withdraw(eq(42L), any()))
                .thenThrow(new InsufficientFundsException("Insufficient funds"));

        WithdrawRequest req = new WithdrawRequest();
        req.setAmount(new BigDecimal("99999.00"));
        req.setDescription("Too much");

        mockMvc.perform(withGatewayHeaders(post("/api/v1/wallet/withdraw"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Insufficient Funds"));
    }

    @Test
    @DisplayName("POST /withdraw — 401 without auth")
    void withdraw_unauthenticated_returns401() throws Exception {
        WithdrawRequest req = new WithdrawRequest();
        req.setAmount(new BigDecimal("500.00"));
        req.setDescription("Test");

        mockMvc.perform(post("/api/v1/wallet/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    // GET /api/v1/wallet/transactions
    // ================================================================

    @Test
    @DisplayName("GET /transactions — 200 with paginated results")
    void getTransactionHistory_returns200() throws Exception {
        PagedResponse<WalletTransactionResponse> paged = PagedResponse.<WalletTransactionResponse>builder()
                .content(List.of(sampleTxn))
                .page(0).size(20).totalElements(1).totalPages(1).last(true)
                .build();

        when(walletService.getTransactionHistory(eq(42L), any())).thenReturn(paged);

        mockMvc.perform(withGatewayHeaders(get("/api/v1/wallet/transactions")
                        .param("page", "0").param("size", "20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].referenceCode").value("DEP-20240101-ABCD1234"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    // ================================================================
    // Admin endpoints
    // ================================================================

    @Test
    @DisplayName("POST /admin/{walletNumber}/suspend — 403 for non-admin user")
    void suspendWallet_nonAdmin_returns403() throws Exception {
        // USER role — not ADMIN
        mockMvc.perform(post("/api/v1/wallet/admin/WLT-20240101-ABCD1234/suspend")
                        .header("X-Auth-User-Id",    "42")
                        .header("X-Auth-User-Email", "jean@example.com")
                        .header("X-Auth-User-Role",  "USER")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /admin/{walletNumber}/suspend — 200 for admin user")
    void suspendWallet_admin_returns200() throws Exception {
        when(walletService.suspendWallet("WLT-20240101-ABCD1234")).thenReturn(
                WalletResponse.builder().walletNumber("WLT-20240101-ABCD1234")
                        .status(WalletStatus.SUSPENDED).build());

        mockMvc.perform(post("/api/v1/wallet/admin/WLT-20240101-ABCD1234/suspend")
                        .header("X-Auth-User-Id",    "1")
                        .header("X-Auth-User-Email", "admin@example.com")
                        .header("X-Auth-User-Role",  "ADMIN")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }
}
