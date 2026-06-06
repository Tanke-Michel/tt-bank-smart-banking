package com.example.savings_service.service;

import com.example.savings_service.dto.*;
import com.example.savings_service.entity.*;
import com.example.savings_service.enums.*;
import com.example.savings_service.exception.*;
import com.example.savings_service.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("SavingsService Unit Tests")
@ExtendWith(MockitoExtension.class)
class SavingsServiceTest {

    @Mock private SavingsGroupRepository groupRepository;
    @Mock private GroupMemberRepository memberRepository;
    @Mock private ContributionRepository contributionRepository;
    @Mock private PayoutRepository payoutRepository;
    @Mock private WalletServiceClient walletClient;
    @Mock private SavingsEventPublisher eventPublisher;

    @InjectMocks private SavingsService savingsService;

    private SavingsGroup activeGroup;
    private GroupMember creator;
    private GroupMember member2;
    private WalletInfo activeWallet;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(savingsService, "maxMembersPerGroup", 50);
        ReflectionTestUtils.setField(savingsService, "minContributionAmount", new BigDecimal("100"));

        activeGroup = SavingsGroup.builder()
                .id(1L).name("Njangi Circle")
                .creatorUserId(10L).creatorEmail("creator@example.com")
                .contributionAmount(new BigDecimal("5000.00"))
                .currency("XAF").payoutCycle(PayoutCycle.MONTHLY)
                .maxMembers(3).currentRound(1).status(GroupStatus.ACTIVE)
                .startDate(LocalDate.now().plusDays(7))
                .build();

        creator = GroupMember.builder()
                .id(1L).group(activeGroup).userId(10L)
                .userEmail("creator@example.com").fullName("Creator User")
                .walletNumber("WLT-CREATOR-001").payoutOrder(1)
                .status(MemberStatus.ACTIVE).hasReceivedPayout(false)
                .build();

        member2 = GroupMember.builder()
                .id(2L).group(activeGroup).userId(20L)
                .userEmail("member2@example.com").fullName("Member Two")
                .walletNumber("WLT-MEMBER2-001").payoutOrder(2)
                .status(MemberStatus.ACTIVE).hasReceivedPayout(false)
                .build();

        activeWallet = new WalletInfo();
        activeWallet.setUserId(10L);
        activeWallet.setWalletNumber("WLT-CREATOR-001");
        activeWallet.setStatus("ACTIVE");
        activeWallet.setCurrency("XAF");
        activeWallet.setBalance(new BigDecimal("50000.00"));
    }

    // ================================================================
    // CREATE GROUP
    // ================================================================

    @Test
    @DisplayName("createGroup — success creates FORMING group with creator as first member")
    void createGroup_success() {
        when(walletClient.getWalletByNumber("WLT-CREATOR-001")).thenReturn(activeWallet);
        when(groupRepository.save(any(SavingsGroup.class))).thenAnswer(inv -> {
            SavingsGroup g = inv.getArgument(0);
            g = SavingsGroup.builder().id(1L).name(g.getName())
                    .creatorUserId(g.getCreatorUserId()).creatorEmail(g.getCreatorEmail())
                    .contributionAmount(g.getContributionAmount()).currency(g.getCurrency())
                    .payoutCycle(g.getPayoutCycle()).maxMembers(g.getMaxMembers())
                    .currentRound(1).status(GroupStatus.FORMING)
                    .startDate(g.getStartDate()).build();
            return g;
        });
        when(memberRepository.save(any(GroupMember.class))).thenReturn(creator);

        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("Njangi Circle");
        req.setContributionAmount(new BigDecimal("5000.00"));
        req.setPayoutCycle(PayoutCycle.MONTHLY);
        req.setMaxMembers(3);
        req.setStartDate(LocalDate.now().plusDays(7));
        req.setWalletNumber("WLT-CREATOR-001");

        GroupResponse response = savingsService.createGroup(
                10L, "creator@example.com", "Creator User", req);

        assertThat(response.getName()).isEqualTo("Njangi Circle");
        assertThat(response.getStatus()).isEqualTo(GroupStatus.FORMING);
        verify(memberRepository).save(any(GroupMember.class));
        verify(eventPublisher).publishGroupCreated(any());
    }

    @Test
    @DisplayName("createGroup — throws InvalidGroupStateException when wallet not owned by user")
    void createGroup_walletNotOwned_throws() {
        WalletInfo otherWallet = new WalletInfo();
        otherWallet.setUserId(99L); // belongs to someone else
        otherWallet.setStatus("ACTIVE");
        when(walletClient.getWalletByNumber("WLT-CREATOR-001")).thenReturn(otherWallet);

        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("Test"); req.setContributionAmount(new BigDecimal("1000"));
        req.setPayoutCycle(PayoutCycle.MONTHLY); req.setMaxMembers(3);
        req.setStartDate(LocalDate.now().plusDays(7)); req.setWalletNumber("WLT-CREATOR-001");

        assertThatThrownBy(() -> savingsService.createGroup(10L, "e@e.com", "Name", req))
                .isInstanceOf(InvalidGroupStateException.class)
                .hasMessageContaining("does not belong to your account");
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("createGroup — throws InvalidGroupStateException when amount below minimum")
    void createGroup_amountTooLow_throws() {
        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("Test"); req.setContributionAmount(new BigDecimal("50")); // below 100 min
        req.setPayoutCycle(PayoutCycle.MONTHLY); req.setMaxMembers(3);
        req.setStartDate(LocalDate.now().plusDays(7)); req.setWalletNumber("WLT-001");

        assertThatThrownBy(() -> savingsService.createGroup(10L, "e@e.com", "Name", req))
                .isInstanceOf(InvalidGroupStateException.class)
                .hasMessageContaining("Minimum contribution");
    }

    // ================================================================
    // JOIN GROUP
    // ================================================================

    @Test
    @DisplayName("joinGroup — success adds member and assigns payout order")
    void joinGroup_success_addsMember() {
        SavingsGroup formingGroup = SavingsGroup.builder()
                .id(1L).name("Njangi").creatorUserId(10L)
                .maxMembers(3).currentRound(1).status(GroupStatus.FORMING).build();

        WalletInfo memberWallet = new WalletInfo();
        memberWallet.setUserId(20L); memberWallet.setStatus("ACTIVE");
        memberWallet.setWalletNumber("WLT-M2-001");

        when(groupRepository.findById(1L)).thenReturn(Optional.of(formingGroup));
        when(memberRepository.existsByGroupIdAndUserId(1L, 20L)).thenReturn(false);
        when(memberRepository.countByGroupId(1L)).thenReturn(1); // creator already in
        when(walletClient.getWalletByNumber("WLT-M2-001")).thenReturn(memberWallet);
        when(memberRepository.save(any())).thenReturn(member2);

        JoinGroupRequest req = new JoinGroupRequest();
        req.setWalletNumber("WLT-M2-001");

        MemberResponse response = savingsService.joinGroup(
                1L, 20L, "member2@example.com", "Member Two", req);

        assertThat(response.getUserEmail()).isEqualTo("member2@example.com");
        verify(memberRepository).save(any(GroupMember.class));
        verify(eventPublisher).publishMemberJoined(any(), any());
    }

    @Test
    @DisplayName("joinGroup — group transitions to ACTIVE when last slot filled")
    void joinGroup_lastSlot_groupBecomesActive() {
        SavingsGroup formingGroup = SavingsGroup.builder()
                .id(1L).name("Njangi").creatorUserId(10L)
                .maxMembers(2).currentRound(1).status(GroupStatus.FORMING).build();

        WalletInfo memberWallet = new WalletInfo();
        memberWallet.setUserId(20L); memberWallet.setStatus("ACTIVE");
        memberWallet.setWalletNumber("WLT-M2-001");

        when(groupRepository.findById(1L)).thenReturn(Optional.of(formingGroup));
        when(memberRepository.existsByGroupIdAndUserId(1L, 20L)).thenReturn(false);
        when(memberRepository.countByGroupId(1L)).thenReturn(1); // 1 member already, adding last
        when(walletClient.getWalletByNumber("WLT-M2-001")).thenReturn(memberWallet);
        when(memberRepository.save(any())).thenReturn(member2);
        when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JoinGroupRequest req = new JoinGroupRequest();
        req.setWalletNumber("WLT-M2-001");

        savingsService.joinGroup(1L, 20L, "member2@example.com", "Member Two", req);

        // Group should have been saved with ACTIVE status
        verify(groupRepository).save(argThat(g -> g.getStatus() == GroupStatus.ACTIVE));
    }

    @Test
    @DisplayName("joinGroup — throws AlreadyMemberException when already in group")
    void joinGroup_alreadyMember_throws() {
        SavingsGroup formingGroup = SavingsGroup.builder()
                .id(1L).status(GroupStatus.FORMING).maxMembers(3).build();
        when(groupRepository.findById(1L)).thenReturn(Optional.of(formingGroup));
        when(memberRepository.existsByGroupIdAndUserId(1L, 10L)).thenReturn(true);

        JoinGroupRequest req = new JoinGroupRequest();
        req.setWalletNumber("WLT-001");

        assertThatThrownBy(() -> savingsService.joinGroup(
                1L, 10L, "e@e.com", "Name", req))
                .isInstanceOf(AlreadyMemberException.class);
    }

    @Test
    @DisplayName("joinGroup — throws GroupFullException when group at capacity")
    void joinGroup_groupFull_throws() {
        SavingsGroup formingGroup = SavingsGroup.builder()
                .id(1L).status(GroupStatus.FORMING).maxMembers(3).build();
        when(groupRepository.findById(1L)).thenReturn(Optional.of(formingGroup));
        when(memberRepository.existsByGroupIdAndUserId(1L, 20L)).thenReturn(false);
        when(memberRepository.countByGroupId(1L)).thenReturn(3); // already full

        JoinGroupRequest req = new JoinGroupRequest();
        req.setWalletNumber("WLT-001");

        assertThatThrownBy(() -> savingsService.joinGroup(
                1L, 20L, "e@e.com", "Name", req))
                .isInstanceOf(GroupFullException.class);
    }

    @Test
    @DisplayName("joinGroup — throws GroupNotActiveException when group is ACTIVE (not FORMING)")
    void joinGroup_groupActive_throws() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(activeGroup));

        JoinGroupRequest req = new JoinGroupRequest();
        req.setWalletNumber("WLT-001");

        assertThatThrownBy(() -> savingsService.joinGroup(
                1L, 30L, "e@e.com", "Name", req))
                .isInstanceOf(GroupNotActiveException.class)
                .hasMessageContaining("no longer accepting");
    }

    // ================================================================
    // CONTRIBUTE
    // ================================================================

    @Test
    @DisplayName("contribute — success debits wallet and marks PAID")
    void contribute_success_paidContribution() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(activeGroup));
        when(memberRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(creator));
        when(contributionRepository.findByMemberIdAndRoundNumber(1L, 1)).thenReturn(Optional.empty());

        Contribution savedContrib = Contribution.builder()
                .id(1L).group(activeGroup).member(creator).roundNumber(1)
                .amount(new BigDecimal("5000.00")).currency("XAF")
                .walletNumber("WLT-CREATOR-001").referenceCode("CONT-20240101-ABCD1234")
                .status(ContributionStatus.PAID).paidAt(LocalDateTime.now()).build();

        when(contributionRepository.save(any())).thenReturn(savedContrib);

        ContributeRequest req = new ContributeRequest();
        req.setWalletNumber("WLT-CREATOR-001");

        ContributionResponse response = savingsService.contribute(1L, 10L, req);

        assertThat(response.getStatus()).isEqualTo(ContributionStatus.PAID);
        verify(walletClient).debitWallet(
                eq("WLT-CREATOR-001"), eq(new BigDecimal("5000.00")), anyString());
        verify(eventPublisher).publishContributionMade(any());
    }

    @Test
    @DisplayName("contribute — marks FAILED when wallet debit throws")
    void contribute_debitFails_markedFailed() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(activeGroup));
        when(memberRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(creator));
        when(contributionRepository.findByMemberIdAndRoundNumber(1L, 1)).thenReturn(Optional.empty());

        Contribution failedContrib = Contribution.builder()
                .id(1L).group(activeGroup).member(creator).roundNumber(1)
                .amount(new BigDecimal("5000.00")).currency("XAF")
                .walletNumber("WLT-CREATOR-001").referenceCode("CONT-TEST")
                .status(ContributionStatus.FAILED).failureReason("Insufficient funds").build();

        when(contributionRepository.save(any())).thenReturn(failedContrib);
        doThrow(new WalletServiceException("Insufficient funds", 422))
                .when(walletClient).debitWallet(any(), any(), any());

        ContributeRequest req = new ContributeRequest();
        req.setWalletNumber("WLT-CREATOR-001");

        ContributionResponse response = savingsService.contribute(1L, 10L, req);

        assertThat(response.getStatus()).isEqualTo(ContributionStatus.FAILED);
        verify(eventPublisher).publishContributionMade(any());
    }

    @Test
    @DisplayName("contribute — throws InvalidGroupStateException when already contributed this round")
    void contribute_alreadyPaid_throws() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(activeGroup));
        when(memberRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(creator));
        Contribution existing = Contribution.builder().id(1L).status(ContributionStatus.PAID).build();
        when(contributionRepository.findByMemberIdAndRoundNumber(1L, 1))
                .thenReturn(Optional.of(existing));

        ContributeRequest req = new ContributeRequest();
        req.setWalletNumber("WLT-CREATOR-001");

        assertThatThrownBy(() -> savingsService.contribute(1L, 10L, req))
                .isInstanceOf(InvalidGroupStateException.class)
                .hasMessageContaining("already contributed");
    }

    @Test
    @DisplayName("contribute — throws NotGroupMemberException when user is not in group")
    void contribute_notMember_throws() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(activeGroup));
        when(memberRepository.findByGroupIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        ContributeRequest req = new ContributeRequest();
        req.setWalletNumber("WLT-999");

        assertThatThrownBy(() -> savingsService.contribute(1L, 99L, req))
                .isInstanceOf(NotGroupMemberException.class);
    }

    @Test
    @DisplayName("contribute — throws GroupNotActiveException when group is FORMING")
    void contribute_groupForming_throws() {
        activeGroup.setStatus(GroupStatus.FORMING);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(activeGroup));

        ContributeRequest req = new ContributeRequest();
        req.setWalletNumber("WLT-001");

        assertThatThrownBy(() -> savingsService.contribute(1L, 10L, req))
                .isInstanceOf(GroupNotActiveException.class);
    }

    // ================================================================
    // PROCESS ROUND PAYOUT
    // ================================================================

    @Test
    @DisplayName("processRoundPayout — success credits recipient and advances round")
    void processRoundPayout_success() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(activeGroup));
        when(payoutRepository.findByGroupIdAndRoundNumber(1L, 1)).thenReturn(Optional.empty());
        when(memberRepository.findPayoutRecipientForRound(1L, 1))
                .thenReturn(Optional.of(creator));
        when(contributionRepository.sumPaidAmountForRound(1L, 1))
                .thenReturn(new BigDecimal("15000.00")); // 3 members × 5000

        Payout completedPayout = Payout.builder()
                .id(1L).group(activeGroup).recipientMember(creator).roundNumber(1)
                .amount(new BigDecimal("15000.00")).currency("XAF")
                .recipientWalletNumber("WLT-CREATOR-001").referenceCode("POUT-20240101-ABCD")
                .status(PayoutStatus.COMPLETED).completedAt(LocalDateTime.now()).build();

        when(payoutRepository.save(any())).thenReturn(completedPayout);
        when(memberRepository.save(any())).thenReturn(creator);
        when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PayoutResponse response = savingsService.processRoundPayout(1L, 10L);

        assertThat(response.getStatus()).isEqualTo(PayoutStatus.COMPLETED);
        assertThat(response.getAmount()).isEqualByComparingTo("15000.00");

        verify(walletClient).creditWallet(
                eq("WLT-CREATOR-001"), eq(new BigDecimal("15000.00")), anyString());
        verify(memberRepository).save(argThat(GroupMember::isHasReceivedPayout));
        // Round advanced from 1 to 2 (group has 3 members, not complete)
        verify(groupRepository).save(argThat(g -> g.getCurrentRound() == 2));
        verify(eventPublisher).publishPayoutProcessed(any());
    }

    @Test
    @DisplayName("processRoundPayout — completes group when last round processed")
    void processRoundPayout_lastRound_completesGroup() {
        activeGroup.setCurrentRound(3); // last round for 3-member group
        when(groupRepository.findById(1L)).thenReturn(Optional.of(activeGroup));
        when(payoutRepository.findByGroupIdAndRoundNumber(1L, 3)).thenReturn(Optional.empty());
        when(memberRepository.findPayoutRecipientForRound(1L, 3))
                .thenReturn(Optional.of(creator));
        when(contributionRepository.sumPaidAmountForRound(1L, 3))
                .thenReturn(new BigDecimal("15000.00"));

        Payout payout = Payout.builder()
                .id(3L).group(activeGroup).recipientMember(creator).roundNumber(3)
                .amount(new BigDecimal("15000.00")).currency("XAF")
                .recipientWalletNumber("WLT-CREATOR-001").referenceCode("POUT-R3")
                .status(PayoutStatus.COMPLETED).completedAt(LocalDateTime.now()).build();

        when(payoutRepository.save(any())).thenReturn(payout);
        when(memberRepository.save(any())).thenReturn(creator);
        when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        savingsService.processRoundPayout(1L, 10L);

        // Group should be saved with COMPLETED status (3 > maxMembers=3)
        verify(groupRepository).save(argThat(g -> g.getStatus() == GroupStatus.COMPLETED));
    }

    @Test
    @DisplayName("processRoundPayout — throws InvalidGroupStateException when non-creator calls it")
    void processRoundPayout_nonCreator_throws() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(activeGroup));

        assertThatThrownBy(() -> savingsService.processRoundPayout(1L, 99L))
                .isInstanceOf(InvalidGroupStateException.class)
                .hasMessageContaining("Only the group creator");
    }

    @Test
    @DisplayName("processRoundPayout — throws InvalidGroupStateException when no contributions paid")
    void processRoundPayout_noContributions_throws() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(activeGroup));
        when(payoutRepository.findByGroupIdAndRoundNumber(1L, 1)).thenReturn(Optional.empty());
        when(memberRepository.findPayoutRecipientForRound(1L, 1))
                .thenReturn(Optional.of(creator));
        when(contributionRepository.sumPaidAmountForRound(1L, 1))
                .thenReturn(BigDecimal.ZERO); // nobody paid

        assertThatThrownBy(() -> savingsService.processRoundPayout(1L, 10L))
                .isInstanceOf(InvalidGroupStateException.class)
                .hasMessageContaining("empty payout");
    }

    @Test
    @DisplayName("processRoundPayout — throws InvalidGroupStateException when already processed")
    void processRoundPayout_alreadyProcessed_throws() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(activeGroup));
        Payout existing = Payout.builder().id(1L).roundNumber(1)
                .status(PayoutStatus.COMPLETED).build();
        when(payoutRepository.findByGroupIdAndRoundNumber(1L, 1))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> savingsService.processRoundPayout(1L, 10L))
                .isInstanceOf(InvalidGroupStateException.class)
                .hasMessageContaining("already been processed");
    }
}
