package com.example.audit_service.controller;

import com.example.audit_service.config.GatewayAuthenticationFilter;
import com.example.audit_service.config.SecurityConfig;
import com.example.audit_service.dto.*;
import com.example.audit_service.enums.EventDomain;
import com.example.audit_service.enums.EventType;
import com.example.audit_service.exception.AuditEventNotFoundException;
import com.example.audit_service.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditController.class)
@Import({SecurityConfig.class, GatewayAuthenticationFilter.class})
@DisplayName("AuditController Integration Tests")
class AuditControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private AuditService auditService;

    private AuditEventResponse sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = AuditEventResponse.builder()
                .id(1L)
                .eventDomain(EventDomain.TRANSACTION)
                .eventType(EventType.TRANSACTION_COMPLETED)
                .referenceCode("TXN-20240101-ABCD1234")
                .actorEmail("sender@example.com")
                .summary("Transfer completed 500.00 XAF ref=TXN-20240101-ABCD1234")
                .rawPayload("{\"amount\":\"500.00\"}")
                .receivedAt(LocalDateTime.now())
                .build();
    }

    private MockHttpServletRequestBuilder withAdminHeaders(MockHttpServletRequestBuilder req) {
        return req
                .header("X-Auth-User-Id",    "1")
                .header("X-Auth-User-Email", "admin@example.com")
                .header("X-Auth-User-Role",  "ADMIN");
    }

    private MockHttpServletRequestBuilder withUserHeaders(MockHttpServletRequestBuilder req) {
        return req
                .header("X-Auth-User-Id",    "10")
                .header("X-Auth-User-Email", "user@example.com")
                .header("X-Auth-User-Role",  "USER");
    }

    // ================================================================
    // GET /api/v1/admin/audit/events
    // ================================================================

    @Test
    @DisplayName("GET /audit/events — 200 for ADMIN with paged results")
    void searchEvents_adminRole_returns200() throws Exception {
        PagedResponse<AuditEventResponse> paged = PagedResponse.<AuditEventResponse>builder()
                .content(List.of(sampleEvent))
                .page(0).size(50).totalElements(1).totalPages(1).last(true)
                .build();

        when(auditService.search(any(), any(), any(), any(), any(), any())).thenReturn(paged);

        mockMvc.perform(withAdminHeaders(get("/api/v1/admin/audit/events")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].referenceCode")
                        .value("TXN-20240101-ABCD1234"))
                .andExpect(jsonPath("$.content[0].eventDomain").value("TRANSACTION"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /audit/events — 200 with domain filter")
    void searchEvents_withDomainFilter_returns200() throws Exception {
        PagedResponse<AuditEventResponse> paged = PagedResponse.<AuditEventResponse>builder()
                .content(List.of(sampleEvent))
                .page(0).size(50).totalElements(1).totalPages(1).last(true)
                .build();

        when(auditService.search(eq(EventDomain.TRANSACTION), any(), any(), any(), any(), any()))
                .thenReturn(paged);

        mockMvc.perform(withAdminHeaders(get("/api/v1/admin/audit/events")
                        .param("domain", "TRANSACTION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventDomain").value("TRANSACTION"));
    }

    @Test
    @DisplayName("GET /audit/events — 403 for USER role")
    void searchEvents_userRole_returns403() throws Exception {
        mockMvc.perform(withUserHeaders(get("/api/v1/admin/audit/events")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /audit/events — 401 without auth")
    void searchEvents_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit/events"))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    // GET /api/v1/admin/audit/events/{id}
    // ================================================================

    @Test
    @DisplayName("GET /audit/events/{id} — 200 for known ID")
    void getById_found_returns200() throws Exception {
        when(auditService.getById(1L)).thenReturn(sampleEvent);

        mockMvc.perform(withAdminHeaders(get("/api/v1/admin/audit/events/1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.eventType").value("TRANSACTION_COMPLETED"));
    }

    @Test
    @DisplayName("GET /audit/events/{id} — 404 for unknown ID")
    void getById_notFound_returns404() throws Exception {
        when(auditService.getById(999L))
                .thenThrow(new AuditEventNotFoundException("Not found: 999"));

        mockMvc.perform(withAdminHeaders(get("/api/v1/admin/audit/events/999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Audit Event Not Found"));
    }

    @Test
    @DisplayName("GET /audit/events/{id} — 403 for USER role")
    void getById_userRole_returns403() throws Exception {
        mockMvc.perform(withUserHeaders(get("/api/v1/admin/audit/events/1")))
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // GET /api/v1/admin/audit/ref/{referenceCode}
    // ================================================================

    @Test
    @DisplayName("GET /audit/ref/{ref} — 200 for known reference code")
    void getByRef_found_returns200() throws Exception {
        when(auditService.getByReferenceCode("TXN-20240101-ABCD1234")).thenReturn(sampleEvent);

        mockMvc.perform(withAdminHeaders(get("/api/v1/admin/audit/ref/TXN-20240101-ABCD1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceCode").value("TXN-20240101-ABCD1234"));
    }

    @Test
    @DisplayName("GET /audit/ref/{ref} — 404 for unknown reference")
    void getByRef_notFound_returns404() throws Exception {
        when(auditService.getByReferenceCode("UNKNOWN"))
                .thenThrow(new AuditEventNotFoundException("Not found: UNKNOWN"));

        mockMvc.perform(withAdminHeaders(get("/api/v1/admin/audit/ref/UNKNOWN")))
                .andExpect(status().isNotFound());
    }

    // ================================================================
    // GET /api/v1/admin/audit/domain/{domain}
    // ================================================================

    @Test
    @DisplayName("GET /audit/domain/{domain} — 200 for ADMIN")
    void getByDomain_returns200() throws Exception {
        PagedResponse<AuditEventResponse> paged = PagedResponse.<AuditEventResponse>builder()
                .content(List.of(sampleEvent))
                .page(0).size(50).totalElements(1).totalPages(1).last(true)
                .build();

        when(auditService.getByDomain(eq(EventDomain.TRANSACTION), any())).thenReturn(paged);

        mockMvc.perform(withAdminHeaders(get("/api/v1/admin/audit/domain/TRANSACTION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventDomain").value("TRANSACTION"));
    }

    @Test
    @DisplayName("GET /audit/domain/{domain} — 403 for USER role")
    void getByDomain_userRole_returns403() throws Exception {
        mockMvc.perform(withUserHeaders(get("/api/v1/admin/audit/domain/TRANSACTION")))
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // GET /api/v1/admin/audit/actor/{email}
    // ================================================================

    @Test
    @DisplayName("GET /audit/actor/{email} — 200 for ADMIN")
    void getByActor_returns200() throws Exception {
        PagedResponse<AuditEventResponse> paged = PagedResponse.<AuditEventResponse>builder()
                .content(List.of(sampleEvent))
                .page(0).size(50).totalElements(1).totalPages(1).last(true)
                .build();

        when(auditService.getByActorEmail(eq("sender@example.com"), any())).thenReturn(paged);

        mockMvc.perform(withAdminHeaders(get("/api/v1/admin/audit/actor/sender@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].actorEmail").value("sender@example.com"));
    }

    // ================================================================
    // GET /api/v1/admin/audit/stats
    // ================================================================

    @Test
    @DisplayName("GET /audit/stats — 200 with stats for ADMIN")
    void getStats_adminRole_returns200() throws Exception {
        AuditStatsResponse stats = AuditStatsResponse.builder()
                .totalEvents(250L)
                .todayEvents(15L)
                .byDomain(Map.of("WALLET", 100L, "TRANSACTION", 150L))
                .todayByType(Map.of("TRANSACTION_COMPLETED", 10L))
                .build();

        when(auditService.getStats()).thenReturn(stats);

        mockMvc.perform(withAdminHeaders(get("/api/v1/admin/audit/stats")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(250))
                .andExpect(jsonPath("$.todayEvents").value(15))
                .andExpect(jsonPath("$.byDomain.WALLET").value(100));
    }

    @Test
    @DisplayName("GET /audit/stats — 403 for USER role")
    void getStats_userRole_returns403() throws Exception {
        mockMvc.perform(withUserHeaders(get("/api/v1/admin/audit/stats")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /audit/stats — 401 without auth")
    void getStats_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit/stats"))
                .andExpect(status().isUnauthorized());
    }
}
