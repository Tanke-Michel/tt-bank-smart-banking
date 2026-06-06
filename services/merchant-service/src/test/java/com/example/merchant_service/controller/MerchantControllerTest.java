package com.example.merchant_service.controller;

import com.example.merchant_service.config.GatewayAuthenticationFilter;
import com.example.merchant_service.config.SecurityConfig;
import com.example.merchant_service.dto.*;
import com.example.merchant_service.enums.BusinessCategory;
import com.example.merchant_service.enums.MerchantStatus;
import com.example.merchant_service.enums.PaymentStatus;
import com.example.merchant_service.exception.*;
import com.example.merchant_service.service.MerchantService;
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

@WebMvcTest(MerchantController.class)
@Import({SecurityConfig.class, GatewayAuthenticationFilter.class})
@DisplayName("MerchantController Integration Tests")
class MerchantControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private MerchantService merchantService;

    private MerchantResponse sampleMerchant;
    private MerchantPaymentResponse samplePayment;

    @BeforeEach
    void setUp() {
        sampleMerchant = MerchantResponse.builder()
                .id(1L).merchantCode("MCH-20240101-ABCD1234")
                .ownerUserId(10L).ownerEmail("merchant@example.com")
                .businessName("Jean's Shop").businessEmail("shop@example.com")
                .businessPhone("+237600000010").businessAddress("Yaoundé, Cameroon")
                .businessCategory(BusinessCategory.RETAIL)
                .walletNumber("WLT-MERCHANT-001").qrCodeBase64("base64qr")
                .status(MerchantStatus.PENDING).createdAt(LocalDateTime.now())
                .build();

        samplePayment = MerchantPaymentResponse.builder()
                .id(1L).referenceCode("PAY-20240101-ABCD1234")
                .merchantId(1L).merchantCode("MCH-20240101-ABCD1234")
                .businessName("Jean's Shop")
                .customerUserId(20L).customerEmail("customer@example.com")
                .customerWalletNumber("WLT-CUSTOMER-001")
                .merchantWalletNumber("WLT-MERCHANT-001")
                .amount(new BigDecimal("500.00")).currency("XAF")
                .description("Payment to Jean's Shop")
                .status(PaymentStatus.COMPLETED).createdAt(LocalDateTime.now())
                .build();
    }

    private MockHttpServletRequestBuilder withMerchantHeaders(MockHttpServletRequestBuilder req) {
        return req
                .header("X-Auth-User-Id",     "10")
                .header("X-Auth-User-Email",  "merchant@example.com")
                .header("X-Auth-User-Role",   "MERCHANT")
                .header("X-Auth-User-Wallet", "WLT-MERCHANT-001");
    }

    private MockHttpServletRequestBuilder withCustomerHeaders(MockHttpServletRequestBuilder req) {
        return req
                .header("X-Auth-User-Id",     "20")
                .header("X-Auth-User-Email",  "customer@example.com")
                .header("X-Auth-User-Role",   "USER")
                .header("X-Auth-User-Wallet", "WLT-CUSTOMER-001");
    }

    private MockHttpServletRequestBuilder withAdminHeaders(MockHttpServletRequestBuilder req) {
        return req
                .header("X-Auth-User-Id",     "1")
                .header("X-Auth-User-Email",  "admin@example.com")
                .header("X-Auth-User-Role",   "ADMIN")
                .header("X-Auth-User-Wallet", "WLT-ADMIN-001");
    }

    // ================================================================
    // POST /register
    // ================================================================

    @Test
    @DisplayName("POST /register — 201 on valid request")
    void register_valid_returns201() throws Exception {
        when(merchantService.register(eq(10L), eq("merchant@example.com"),
                any(RegisterMerchantRequest.class))).thenReturn(sampleMerchant);

        RegisterMerchantRequest req = new RegisterMerchantRequest();
        req.setBusinessName("Jean's Shop");
        req.setBusinessEmail("shop@example.com");
        req.setBusinessPhone("+237600000010");
        req.setBusinessAddress("Yaoundé, Cameroon");
        req.setBusinessCategory(BusinessCategory.RETAIL);
        req.setWalletNumber("WLT-MERCHANT-001");

        mockMvc.perform(withMerchantHeaders(post("/api/v1/merchants/register"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchantCode").value("MCH-20240101-ABCD1234"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.qrCodeBase64").value("base64qr"));
    }

    @Test
    @DisplayName("POST /register — 400 when businessName is blank")
    void register_blankName_returns400() throws Exception {
        RegisterMerchantRequest req = new RegisterMerchantRequest();
        req.setBusinessEmail("shop@example.com");
        req.setBusinessPhone("+237600000010");
        req.setBusinessAddress("Yaoundé");
        req.setBusinessCategory(BusinessCategory.RETAIL);
        req.setWalletNumber("WLT-001");
        // businessName not set

        mockMvc.perform(withMerchantHeaders(post("/api/v1/merchants/register"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.businessName").exists());
    }

    @Test
    @DisplayName("POST /register — 409 when merchant already exists")
    void register_alreadyExists_returns409() throws Exception {
        when(merchantService.register(any(), any(), any()))
                .thenThrow(new MerchantAlreadyExistsException("Already registered"));

        RegisterMerchantRequest req = new RegisterMerchantRequest();
        req.setBusinessName("Shop"); req.setBusinessEmail("shop@example.com");
        req.setBusinessPhone("+237600000010"); req.setBusinessAddress("Yaoundé");
        req.setBusinessCategory(BusinessCategory.RETAIL); req.setWalletNumber("WLT-001");

        mockMvc.perform(withMerchantHeaders(post("/api/v1/merchants/register"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Merchant Already Exists"));
    }

    @Test
    @DisplayName("POST /register — 401 without auth")
    void register_unauthenticated_returns401() throws Exception {
        RegisterMerchantRequest req = new RegisterMerchantRequest();
        req.setBusinessName("Shop"); req.setBusinessEmail("s@e.com");
        req.setBusinessPhone("+237600000010"); req.setBusinessAddress("Y");
        req.setBusinessCategory(BusinessCategory.RETAIL); req.setWalletNumber("W");

        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    // GET /me
    // ================================================================

    @Test
    @DisplayName("GET /me — 200 with merchant profile")
    void getMyMerchant_returns200() throws Exception {
        when(merchantService.getMyMerchant(10L)).thenReturn(sampleMerchant);

        mockMvc.perform(withMerchantHeaders(get("/api/v1/merchants/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantCode").value("MCH-20240101-ABCD1234"))
                .andExpect(jsonPath("$.businessName").value("Jean's Shop"));
    }

    @Test
    @DisplayName("GET /me — 404 when no merchant account")
    void getMyMerchant_notFound_returns404() throws Exception {
        when(merchantService.getMyMerchant(10L))
                .thenThrow(new MerchantNotFoundException("No merchant account"));

        mockMvc.perform(withMerchantHeaders(get("/api/v1/merchants/me")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Merchant Not Found"));
    }

    @Test
    @DisplayName("GET /me — 401 without auth")
    void getMyMerchant_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/me"))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    // GET /me/dashboard
    // ================================================================

    @Test
    @DisplayName("GET /me/dashboard — 200 with revenue stats")
    void getDashboard_returns200() throws Exception {
        MerchantDashboardResponse dashboard = MerchantDashboardResponse.builder()
                .merchantCode("MCH-20240101-ABCD1234")
                .businessName("Jean's Shop")
                .walletNumber("WLT-MERCHANT-001")
                .qrCodeBase64("base64qr")
                .todayRevenue(new BigDecimal("5000.00"))
                .monthRevenue(new BigDecimal("150000.00"))
                .totalRevenue(new BigDecimal("2000000.00"))
                .todayTransactionCount(5L)
                .monthTransactionCount(150L)
                .totalTransactionCount(2000L)
                .build();

        when(merchantService.getDashboard(10L)).thenReturn(dashboard);

        mockMvc.perform(withMerchantHeaders(get("/api/v1/merchants/me/dashboard")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayRevenue").value(5000.00))
                .andExpect(jsonPath("$.todayTransactionCount").value(5));
    }

    // ================================================================
    // POST /pay
    // ================================================================

    @Test
    @DisplayName("POST /pay — 201 on successful QR payment")
    void pay_success_returns201() throws Exception {
        when(merchantService.pay(eq(20L), eq("customer@example.com"),
                eq("WLT-CUSTOMER-001"), any(MerchantPaymentRequest.class)))
                .thenReturn(samplePayment);

        MerchantPaymentRequest req = new MerchantPaymentRequest();
        req.setMerchantCode("MCH-20240101-ABCD1234");
        req.setCustomerWalletNumber("WLT-CUSTOMER-001");
        req.setAmount(new BigDecimal("500.00"));

        mockMvc.perform(withCustomerHeaders(post("/api/v1/merchants/pay"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.referenceCode").value("PAY-20240101-ABCD1234"));
    }

    @Test
    @DisplayName("POST /pay — 400 when merchantCode is blank")
    void pay_blankMerchantCode_returns400() throws Exception {
        MerchantPaymentRequest req = new MerchantPaymentRequest();
        req.setCustomerWalletNumber("WLT-CUSTOMER-001");
        req.setAmount(new BigDecimal("500.00"));
        // merchantCode not set

        mockMvc.perform(withCustomerHeaders(post("/api/v1/merchants/pay"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.merchantCode").exists());
    }

    @Test
    @DisplayName("POST /pay — 403 when merchant is not active")
    void pay_merchantNotActive_returns403() throws Exception {
        when(merchantService.pay(any(), any(), any(), any()))
                .thenThrow(new MerchantNotActiveException("Merchant is PENDING"));

        MerchantPaymentRequest req = new MerchantPaymentRequest();
        req.setMerchantCode("MCH-PENDING"); req.setCustomerWalletNumber("WLT-CUSTOMER-001");
        req.setAmount(new BigDecimal("500.00"));

        mockMvc.perform(withCustomerHeaders(post("/api/v1/merchants/pay"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Merchant Not Active"));
    }

    @Test
    @DisplayName("POST /pay — 401 without auth")
    void pay_unauthenticated_returns401() throws Exception {
        MerchantPaymentRequest req = new MerchantPaymentRequest();
        req.setMerchantCode("MCH-001"); req.setCustomerWalletNumber("WLT-001");
        req.setAmount(new BigDecimal("500.00"));

        mockMvc.perform(post("/api/v1/merchants/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    // GET /{merchantCode}
    // ================================================================

    @Test
    @DisplayName("GET /{merchantCode} — 200 for known merchant")
    void getByCode_found_returns200() throws Exception {
        when(merchantService.getByCode("MCH-20240101-ABCD1234")).thenReturn(sampleMerchant);

        mockMvc.perform(withCustomerHeaders(get("/api/v1/merchants/MCH-20240101-ABCD1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Jean's Shop"));
    }

    @Test
    @DisplayName("GET /{merchantCode} — 404 for unknown merchant")
    void getByCode_notFound_returns404() throws Exception {
        when(merchantService.getByCode("UNKNOWN"))
                .thenThrow(new MerchantNotFoundException("Not found"));

        mockMvc.perform(withCustomerHeaders(get("/api/v1/merchants/UNKNOWN")))
                .andExpect(status().isNotFound());
    }

    // ================================================================
    // GET /admin/all — ADMIN ONLY
    // ================================================================

    @Test
    @DisplayName("GET /admin/all — 200 for ADMIN role")
    void listAll_adminRole_returns200() throws Exception {
        PagedResponse<MerchantResponse> paged = PagedResponse.<MerchantResponse>builder()
                .content(List.of(sampleMerchant))
                .page(0).size(20).totalElements(1).totalPages(1).last(true)
                .build();
        when(merchantService.listAll(any(), any())).thenReturn(paged);

        mockMvc.perform(withAdminHeaders(get("/api/v1/merchants/admin/all")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].merchantCode")
                        .value("MCH-20240101-ABCD1234"));
    }

    @Test
    @DisplayName("GET /admin/all — 403 for USER role")
    void listAll_userRole_returns403() throws Exception {
        mockMvc.perform(withCustomerHeaders(get("/api/v1/merchants/admin/all")))
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // PUT /admin/{merchantId}/status — ADMIN ONLY
    // ================================================================

    @Test
    @DisplayName("PUT /admin/{merchantId}/status — 200 ADMIN approves merchant")
    void updateStatus_admin_returns200() throws Exception {
        MerchantResponse approved = MerchantResponse.builder()
                .id(1L).merchantCode("MCH-20240101-ABCD1234")
                .status(MerchantStatus.ACTIVE).build();
        when(merchantService.updateStatus(eq(1L), any())).thenReturn(approved);

        AdminStatusRequest req = new AdminStatusRequest();
        req.setStatus(MerchantStatus.ACTIVE);
        req.setReason("Documents verified");

        mockMvc.perform(withAdminHeaders(put("/api/v1/merchants/admin/1/status"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("PUT /admin/{merchantId}/status — 403 for non-admin")
    void updateStatus_nonAdmin_returns403() throws Exception {
        AdminStatusRequest req = new AdminStatusRequest();
        req.setStatus(MerchantStatus.ACTIVE);

        mockMvc.perform(withMerchantHeaders(put("/api/v1/merchants/admin/1/status"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
