package com.example.transaction_service.controller;

import com.example.transaction_service.config.GatewayAuthenticationFilter;
import com.example.transaction_service.config.SecurityConfig;
import com.example.transaction_service.dto.*;
import com.example.transaction_service.enums.TransactionStatus;
import com.example.transaction_service.enums.TransactionType;
import com.example.transaction_service.exception.*;
import com.example.transaction_service.service.TransactionService;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, GatewayAuthenticationFilter.class})
@DisplayName("TransactionController Integration Tests")
class TransactionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private TransactionService transactionService;

    private TransactionResponse sampleTxn;

    @BeforeEach
    void setUp() {
        sampleTxn = TransactionResponse.builder()
                .id(1L)
                .referenceCode("TXN-20240101-ABCD1234")
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .senderWalletNumber("WLT-20240101-SENDER01")
                .senderEmail("sender@example.com")
                .receiverWalletNumber("WLT-20240101-RECV0001")
                .receiverEmail("receiver@example.com")
                .amount(new BigDecimal("500.00"))
                .currency("XAF")
                .description("Test transfer")
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
    }

    /** Adds USER-role gateway headers */
    private MockHttpServletRequestBuilder withUserHeaders(MockHttpServletRequestBuilder req) {
        return req
                .header("X-Auth-User-Id",     "10")
                .header("X-Auth-User-Email",  "sender@example.com")
                .header("X-Auth-User-Role",   "USER")
                .header("X-Auth-User-Wallet", "WLT-20240101-SENDER01");
    }

    /** Adds ADMIN-role gateway headers */
    private MockHttpServletRequestBuilder withAdminHeaders(MockHttpServletRequestBuilder req) {
        return req
                .header("X-Auth-User-Id",     "1")
                .header("X-Auth-User-Email",  "admin@example.com")
                .header("X-Auth-User-Role",   "ADMIN")
                .header("X-Auth-User-Wallet", "WLT-ADMIN-001");
    }

    // ================================================================
    // POST /api/v1/transactions/transfer
    // ================================================================

    @Test
    @DisplayName("POST /transfer — 201 on successful transfer")
    void transfer_success_returns201() throws Exception {
        when(transactionService.transfer(eq(10L), eq("sender@example.com"),
                eq("WLT-20240101-SENDER01"), any(TransferRequest.class)))
                .thenReturn(sampleTxn);

        TransferRequest req = new TransferRequest();
        req.setRecipientEmail("receiver@example.com");
        req.setAmount(new BigDecimal("500.00"));
        req.setDescription("Test");

        mockMvc.perform(withUserHeaders(post("/api/v1/transactions/transfer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referenceCode").value("TXN-20240101-ABCD1234"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.currency").value("XAF"));
    }

    @Test
    @DisplayName("POST /transfer — 400 when recipientEmail is invalid")
    void transfer_invalidEmail_returns400() throws Exception {
        TransferRequest req = new TransferRequest();
        req.setRecipientEmail("not-an-email");
        req.setAmount(new BigDecimal("500.00"));

        mockMvc.perform(withUserHeaders(post("/api/v1/transactions/transfer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.recipientEmail").exists());
    }

    @Test
    @DisplayName("POST /transfer — 400 when amount is null")
    void transfer_nullAmount_returns400() throws Exception {
        TransferRequest req = new TransferRequest();
        req.setRecipientEmail("receiver@example.com");
        // amount not set

        mockMvc.perform(withUserHeaders(post("/api/v1/transactions/transfer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    @DisplayName("POST /transfer — 400 on self-transfer")
    void transfer_selfTransfer_returns400() throws Exception {
        when(transactionService.transfer(any(), any(), any(), any()))
                .thenThrow(new SelfTransferException("You cannot transfer to yourself"));

        TransferRequest req = new TransferRequest();
        req.setRecipientEmail("sender@example.com");
        req.setAmount(new BigDecimal("500.00"));

        mockMvc.perform(withUserHeaders(post("/api/v1/transactions/transfer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Self Transfer Not Allowed"));
    }

    @Test
    @DisplayName("POST /transfer — 422 on limit exceeded")
    void transfer_limitExceeded_returns422() throws Exception {
        when(transactionService.transfer(any(), any(), any(), any()))
                .thenThrow(new LimitExceededException("Daily limit exceeded"));

        TransferRequest req = new TransferRequest();
        req.setRecipientEmail("receiver@example.com");
        req.setAmount(new BigDecimal("9999999.00"));

        mockMvc.perform(withUserHeaders(post("/api/v1/transactions/transfer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Limit Exceeded"));
    }

    @Test
    @DisplayName("POST /transfer — 401 without gateway headers")
    void transfer_unauthenticated_returns401() throws Exception {
        TransferRequest req = new TransferRequest();
        req.setRecipientEmail("receiver@example.com");
        req.setAmount(new BigDecimal("500.00"));

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    // GET /api/v1/transactions/{referenceCode}
    // ================================================================

    @Test
    @DisplayName("GET /{referenceCode} — 200 for known reference")
    void getByReference_found_returns200() throws Exception {
        when(transactionService.getByReferenceCode("TXN-20240101-ABCD1234"))
                .thenReturn(sampleTxn);

        mockMvc.perform(withUserHeaders(get("/api/v1/transactions/TXN-20240101-ABCD1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceCode").value("TXN-20240101-ABCD1234"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /{referenceCode} — 404 for unknown reference")
    void getByReference_notFound_returns404() throws Exception {
        when(transactionService.getByReferenceCode("UNKNOWN"))
                .thenThrow(new TransactionNotFoundException("Not found"));

        mockMvc.perform(withUserHeaders(get("/api/v1/transactions/UNKNOWN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Transaction Not Found"));
    }

    @Test
    @DisplayName("GET /{referenceCode} — 401 without auth")
    void getByReference_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/TXN-20240101-ABCD1234"))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    // GET /api/v1/transactions/history
    // ================================================================

    @Test
    @DisplayName("GET /history — 200 with paginated results")
    void getHistory_returns200() throws Exception {
        PagedResponse<TransactionResponse> paged = PagedResponse.<TransactionResponse>builder()
                .content(List.of(sampleTxn))
                .page(0).size(20).totalElements(1).totalPages(1).last(true)
                .build();

        when(transactionService.getHistory(eq(10L), any())).thenReturn(paged);

        mockMvc.perform(withUserHeaders(get("/api/v1/transactions/history")
                        .param("page", "0").param("size", "20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].referenceCode")
                        .value("TXN-20240101-ABCD1234"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("GET /history — 401 without auth")
    void getHistory_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/history"))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    // GET /api/v1/transactions/sent
    // ================================================================

    @Test
    @DisplayName("GET /sent — 200 with outgoing transactions")
    void getSent_returns200() throws Exception {
        PagedResponse<TransactionResponse> paged = PagedResponse.<TransactionResponse>builder()
                .content(List.of(sampleTxn))
                .page(0).size(20).totalElements(1).totalPages(1).last(true)
                .build();

        when(transactionService.getSent(eq(10L), any())).thenReturn(paged);

        mockMvc.perform(withUserHeaders(get("/api/v1/transactions/sent")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].senderEmail")
                        .value("sender@example.com"));
    }

    // ================================================================
    // GET /api/v1/transactions/received
    // ================================================================

    @Test
    @DisplayName("GET /received — 200 with incoming transactions")
    void getReceived_returns200() throws Exception {
        PagedResponse<TransactionResponse> paged = PagedResponse.<TransactionResponse>builder()
                .content(List.of(sampleTxn))
                .page(0).size(20).totalElements(1).totalPages(1).last(true)
                .build();

        when(transactionService.getReceived(eq(10L), any())).thenReturn(paged);

        mockMvc.perform(withUserHeaders(get("/api/v1/transactions/received")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].receiverEmail")
                        .value("receiver@example.com"));
    }

    // ================================================================
    // POST /api/v1/transactions/{referenceCode}/reverse (ADMIN only)
    // ================================================================

    @Test
    @DisplayName("POST /reverse — 403 for USER role")
    void reverse_userRole_returns403() throws Exception {
        mockMvc.perform(withUserHeaders(
                        post("/api/v1/transactions/TXN-20240101-ABCD1234/reverse"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /reverse — 200 for ADMIN role")
    void reverse_adminRole_returns200() throws Exception {
        TransactionResponse reversed = TransactionResponse.builder()
                .referenceCode("TXN-20240101-ABCD1234")
                .status(TransactionStatus.REVERSED)
                .type(TransactionType.TRANSFER)
                .amount(new BigDecimal("500.00"))
                .currency("XAF")
                .build();

        when(transactionService.reverseTransaction(
                eq("TXN-20240101-ABCD1234"), eq(1L))).thenReturn(reversed);

        mockMvc.perform(withAdminHeaders(
                        post("/api/v1/transactions/TXN-20240101-ABCD1234/reverse"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVERSED"));
    }

    @Test
    @DisplayName("POST /reverse — 401 without auth")
    void reverse_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/TXN-20240101-ABCD1234/reverse")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
