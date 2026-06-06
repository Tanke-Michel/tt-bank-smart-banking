package com.example.savings_service.controller;

import com.example.savings_service.config.GatewayAuthenticationFilter;
import com.example.savings_service.config.SecurityConfig;
import com.example.savings_service.dto.*;
import com.example.savings_service.enums.*;
import com.example.savings_service.exception.*;
import com.example.savings_service.service.SavingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SavingsController.class)
@Import({SecurityConfig.class, GatewayAuthenticationFilter.class})
@DisplayName("SavingsController Integration Tests")
class SavingsControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private SavingsService savingsService;

    private ObjectMapper objectMapper;
    private GroupResponse sampleGroup;
    private MemberResponse sampleMember;
    private ContributionResponse sampleContribution;
    private PayoutResponse samplePayout;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleGroup = GroupResponse.builder()
                .id(1L).name("Njangi Circle")
                .creatorUserId(10L).creatorEmail("creator@example.com")
                .contributionAmount(new BigDecimal("5000.00")).currency("XAF")
                .payoutCycle(PayoutCycle.MONTHLY).maxMembers(3)
                .currentMemberCount(1).currentRound(1).totalRounds(3)
                .status(GroupStatus.FORMING).startDate(LocalDate.now().plusDays(7))
                .createdAt(LocalDateTime.now()).build();

        sampleMember = MemberResponse.builder()
                .id(1L).groupId(1L).userId(10L)
                .userEmail("creator@example.com").fullName("Creator User")
                .walletNumber("WLT-CREATOR-001").payoutOrder(1)
                .status(MemberStatus.ACTIVE).hasReceivedPayout(false)
                .joinedAt(LocalDateTime.now()).build();

        sampleContribution = ContributionResponse.builder()
                .id(1L).groupId(1L).groupName("Njangi Circle")
                .memberId(1L).memberEmail("creator@example.com")
                .roundNumber(1).amount(new BigDecimal("5000.00")).currency("XAF")
                .walletNumber("WLT-CREATOR-001").referenceCode("CONT-20240101-ABCD1234")
                .status(ContributionStatus.PAID).createdAt(LocalDateTime.now())
                .paidAt(LocalDateTime.now()).build();

        samplePayout = PayoutResponse.builder()
                .id(1L).groupId(1L).groupName("Njangi Circle")
                .recipientMemberId(1L).recipientEmail("creator@example.com")
                .roundNumber(1).amount(new BigDecimal("15000.00")).currency("XAF")
                .recipientWalletNumber("WLT-CREATOR-001").referenceCode("POUT-20240101-ABCD1234")
                .status(PayoutStatus.COMPLETED).createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now()).build();
    }

    private MockHttpServletRequestBuilder withUserHeaders(MockHttpServletRequestBuilder req) {
        return req
                .header("X-Auth-User-Id",    "10")
                .header("X-Auth-User-Email", "creator@example.com")
                .header("X-Auth-User-Role",  "USER")
                .header("X-Auth-User-Name",  "Creator User");
    }

    private MockHttpServletRequestBuilder withAdminHeaders(MockHttpServletRequestBuilder req) {
        return req
                .header("X-Auth-User-Id",    "1")
                .header("X-Auth-User-Email", "admin@example.com")
                .header("X-Auth-User-Role",  "ADMIN");
    }

    // ================================================================
    // POST /groups
    // ================================================================

    @Test
    @DisplayName("POST /groups — 201 on valid request")
    void createGroup_valid_returns201() throws Exception {
        when(savingsService.createGroup(eq(10L), eq("creator@example.com"),
                eq("Creator User"), any(CreateGroupRequest.class)))
                .thenReturn(sampleGroup);

        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("Njangi Circle");
        req.setContributionAmount(new BigDecimal("5000.00"));
        req.setPayoutCycle(PayoutCycle.MONTHLY);
        req.setMaxMembers(3);
        req.setStartDate(LocalDate.now().plusDays(7));
        req.setWalletNumber("WLT-CREATOR-001");

        mockMvc.perform(withUserHeaders(post("/api/v1/savings/groups"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Njangi Circle"))
                .andExpect(jsonPath("$.status").value("FORMING"))
                .andExpect(jsonPath("$.payoutCycle").value("MONTHLY"));
    }

    @Test
    @DisplayName("POST /groups — 400 when name is blank")
    void createGroup_blankName_returns400() throws Exception {
        CreateGroupRequest req = new CreateGroupRequest();
        req.setContributionAmount(new BigDecimal("5000.00"));
        req.setPayoutCycle(PayoutCycle.MONTHLY);
        req.setMaxMembers(3);
        req.setStartDate(LocalDate.now().plusDays(7));
        req.setWalletNumber("WLT-001");
        // name not set

        mockMvc.perform(withUserHeaders(post("/api/v1/savings/groups"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    @DisplayName("POST /groups — 400 when startDate is in the past")
    void createGroup_pastStartDate_returns400() throws Exception {
        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("Test Group");
        req.setContributionAmount(new BigDecimal("5000.00"));
        req.setPayoutCycle(PayoutCycle.MONTHLY);
        req.setMaxMembers(3);
        req.setStartDate(LocalDate.now().minusDays(1)); // past date
        req.setWalletNumber("WLT-001");

        mockMvc.perform(withUserHeaders(post("/api/v1/savings/groups"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.startDate").exists());
    }

    @Test
    @DisplayName("POST /groups — 401 without auth")
    void createGroup_unauthenticated_returns401() throws Exception {
        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("Test"); req.setContributionAmount(new BigDecimal("1000"));
        req.setPayoutCycle(PayoutCycle.MONTHLY); req.setMaxMembers(3);
        req.setStartDate(LocalDate.now().plusDays(7)); req.setWalletNumber("WLT-001");

        mockMvc.perform(post("/api/v1/savings/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    // GET /groups
    // ================================================================

    @Test
    @DisplayName("GET /groups — 200 with paginated results")
    void listGroups_returns200() throws Exception {
        PagedResponse<GroupResponse> paged = PagedResponse.<GroupResponse>builder()
                .content(List.of(sampleGroup))
                .page(0).size(20).totalElements(1).totalPages(1).last(true)
                .build();

        when(savingsService.listGroups(any(), any())).thenReturn(paged);

        mockMvc.perform(withUserHeaders(get("/api/v1/savings/groups")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Njangi Circle"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /groups — 401 without auth")
    void listGroups_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/savings/groups"))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    // GET /groups/mine
    // ================================================================

    @Test
    @DisplayName("GET /groups/mine — 200 with user's groups")
    void getMyGroups_returns200() throws Exception {
        PagedResponse<GroupResponse> paged = PagedResponse.<GroupResponse>builder()
                .content(List.of(sampleGroup))
                .page(0).size(20).totalElements(1).totalPages(1).last(true)
                .build();

        when(savingsService.getMyGroups(eq(10L), any())).thenReturn(paged);

        mockMvc.perform(withUserHeaders(get("/api/v1/savings/groups/mine")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].creatorEmail")
                        .value("creator@example.com"));
    }

    // ================================================================
    // GET /groups/{groupId}
    // ================================================================

    @Test
    @DisplayName("GET /groups/{groupId} — 200 for known group")
    void getGroup_found_returns200() throws Exception {
        when(savingsService.getGroup(1L)).thenReturn(sampleGroup);

        mockMvc.perform(withUserHeaders(get("/api/v1/savings/groups/1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.maxMembers").value(3));
    }

    @Test
    @DisplayName("GET /groups/{groupId} — 404 for unknown group")
    void getGroup_notFound_returns404() throws Exception {
        when(savingsService.getGroup(999L))
                .thenThrow(new GroupNotFoundException("Not found"));

        mockMvc.perform(withUserHeaders(get("/api/v1/savings/groups/999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Group Not Found"));
    }

    // ================================================================
    // POST /groups/{groupId}/join
    // ================================================================

    @Test
    @DisplayName("POST /groups/{groupId}/join — 201 when joining successfully")
    void joinGroup_success_returns201() throws Exception {
        when(savingsService.joinGroup(eq(1L), eq(10L), anyString(), anyString(),
                any(JoinGroupRequest.class))).thenReturn(sampleMember);

        JoinGroupRequest req = new JoinGroupRequest();
        req.setWalletNumber("WLT-CREATOR-001");

        mockMvc.perform(withUserHeaders(post("/api/v1/savings/groups/1/join"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payoutOrder").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /groups/{groupId}/join — 409 when already a member")
    void joinGroup_alreadyMember_returns409() throws Exception {
        when(savingsService.joinGroup(any(), any(), any(), any(), any()))
                .thenThrow(new AlreadyMemberException("Already a member"));

        JoinGroupRequest req = new JoinGroupRequest();
        req.setWalletNumber("WLT-001");

        mockMvc.perform(withUserHeaders(post("/api/v1/savings/groups/1/join"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Already a Member"));
    }

    @Test
    @DisplayName("POST /groups/{groupId}/join — 409 when group is full")
    void joinGroup_groupFull_returns409() throws Exception {
        when(savingsService.joinGroup(any(), any(), any(), any(), any()))
                .thenThrow(new GroupFullException("Group is full"));

        JoinGroupRequest req = new JoinGroupRequest();
        req.setWalletNumber("WLT-001");

        mockMvc.perform(withUserHeaders(post("/api/v1/savings/groups/1/join"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Group Full"));
    }

    // ================================================================
    // POST /groups/{groupId}/contribute
    // ================================================================

    @Test
    @DisplayName("POST /contribute — 201 on successful contribution")
    void contribute_success_returns201() throws Exception {
        when(savingsService.contribute(eq(1L), eq(10L), any(ContributeRequest.class)))
                .thenReturn(sampleContribution);

        ContributeRequest req = new ContributeRequest();
        req.setWalletNumber("WLT-CREATOR-001");

        mockMvc.perform(withUserHeaders(post("/api/v1/savings/groups/1/contribute"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.referenceCode").value("CONT-20240101-ABCD1234"));
    }

    @Test
    @DisplayName("POST /contribute — 403 when not a group member")
    void contribute_notMember_returns403() throws Exception {
        when(savingsService.contribute(any(), any(), any()))
                .thenThrow(new NotGroupMemberException("Not a member"));

        ContributeRequest req = new ContributeRequest();
        req.setWalletNumber("WLT-001");

        mockMvc.perform(withUserHeaders(post("/api/v1/savings/groups/1/contribute"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Not a Group Member"));
    }

    @Test
    @DisplayName("POST /contribute — 401 without auth")
    void contribute_unauthenticated_returns401() throws Exception {
        ContributeRequest req = new ContributeRequest();
        req.setWalletNumber("WLT-001");

        mockMvc.perform(post("/api/v1/savings/groups/1/contribute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    // POST /groups/{groupId}/payout
    // ================================================================

    @Test
    @DisplayName("POST /payout — 200 when creator processes payout")
    void processPayout_creator_returns200() throws Exception {
        when(savingsService.processRoundPayout(1L, 10L)).thenReturn(samplePayout);

        mockMvc.perform(withUserHeaders(post("/api/v1/savings/groups/1/payout"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(15000.00));
    }

    @Test
    @DisplayName("POST /payout — 409 when payout already processed")
    void processPayout_alreadyDone_returns409() throws Exception {
        when(savingsService.processRoundPayout(any(), any()))
                .thenThrow(new InvalidGroupStateException("Already processed"));

        mockMvc.perform(withUserHeaders(post("/api/v1/savings/groups/1/payout"))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Invalid Group State"));
    }

    // ================================================================
    // GET /groups/{groupId}/members
    // ================================================================

    @Test
    @DisplayName("GET /groups/{groupId}/members — 200 with member list")
    void getMembers_returns200() throws Exception {
        when(savingsService.getGroupMembers(1L)).thenReturn(List.of(sampleMember));

        mockMvc.perform(withUserHeaders(get("/api/v1/savings/groups/1/members")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].payoutOrder").value(1))
                .andExpect(jsonPath("$[0].userEmail").value("creator@example.com"));
    }

    // ================================================================
    // GET /groups/{groupId}/payouts
    // ================================================================

    @Test
    @DisplayName("GET /groups/{groupId}/payouts — 200 with payouts list")
    void getPayouts_returns200() throws Exception {
        when(savingsService.getGroupPayouts(1L)).thenReturn(List.of(samplePayout));

        mockMvc.perform(withUserHeaders(get("/api/v1/savings/groups/1/payouts")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roundNumber").value(1))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }
}
