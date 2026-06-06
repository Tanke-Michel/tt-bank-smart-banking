package com.example.savings_service.service;

import com.example.savings_service.dto.*;
import com.example.savings_service.entity.*;
import com.example.savings_service.enums.*;
import com.example.savings_service.exception.*;
import com.example.savings_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsService {

    private final SavingsGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final ContributionRepository contributionRepository;
    private final PayoutRepository payoutRepository;
    private final WalletServiceClient walletClient;
    private final SavingsEventPublisher eventPublisher;

    @Value("${app.savings.max-members-per-group}")
    private int maxMembersPerGroup;

    @Value("${app.savings.min-contribution-amount}")
    private BigDecimal minContributionAmount;

    // ================================================================
    // CREATE GROUP
    // ================================================================

    @Transactional
    public GroupResponse createGroup(Long creatorUserId, String creatorEmail,
                                     String creatorFullName, CreateGroupRequest request) {

        log.info("Creating savings group: name={} creator={}", request.getName(), creatorEmail);

        if (request.getContributionAmount().compareTo(minContributionAmount) < 0) {
            throw new InvalidGroupStateException(
                    "Minimum contribution amount is " + minContributionAmount);
        }

        // Validate wallet belongs to this user and is ACTIVE
        WalletInfo wallet = walletClient.getWalletByNumber(request.getWalletNumber());
        if (!wallet.getUserId().equals(creatorUserId)) {
            throw new InvalidGroupStateException(
                    "The wallet number provided does not belong to your account");
        }
        if (!"ACTIVE".equals(wallet.getStatus())) {
            throw new InvalidGroupStateException(
                    "Your wallet must be ACTIVE to create a savings group");
        }

        SavingsGroup group = SavingsGroup.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .creatorUserId(creatorUserId)
                .creatorEmail(creatorEmail)
                .contributionAmount(request.getContributionAmount())
                .currency(wallet.getCurrency() != null ? wallet.getCurrency() : "XAF")
                .payoutCycle(request.getPayoutCycle())
                .maxMembers(request.getMaxMembers())
                .startDate(request.getStartDate())
                .status(GroupStatus.FORMING)
                .build();

        SavingsGroup savedGroup = groupRepository.save(group);

        // Creator automatically joins as member with payoutOrder = 1
        GroupMember creatorMember = GroupMember.builder()
                .group(savedGroup)
                .userId(creatorUserId)
                .userEmail(creatorEmail)
                .fullName(creatorFullName)
                .walletNumber(request.getWalletNumber())
                .payoutOrder(1)
                .status(MemberStatus.ACTIVE)
                .build();

        memberRepository.save(creatorMember);
        log.info("Group created: id={} name={}", savedGroup.getId(), savedGroup.getName());

        eventPublisher.publishGroupCreated(savedGroup);

        return GroupResponse.from(savedGroup, 1);
    }

    // ================================================================
    // JOIN GROUP
    // ================================================================

    @Transactional
    public MemberResponse joinGroup(Long groupId, Long userId, String userEmail,
                                    String fullName, JoinGroupRequest request) {

        log.info("Join request: groupId={} userId={}", groupId, userId);

        SavingsGroup group = getGroupOrThrow(groupId);

        if (group.getStatus() != GroupStatus.FORMING) {
            throw new GroupNotActiveException(
                    "This group is no longer accepting new members (status=" + group.getStatus() + ")");
        }

        if (memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new AlreadyMemberException("You are already a member of this group");
        }

        int currentCount = memberRepository.countByGroupId(groupId);
        if (currentCount >= group.getMaxMembers()) {
            throw new GroupFullException(
                    "This group has reached its maximum capacity of " + group.getMaxMembers() + " members");
        }

        // Validate wallet
        WalletInfo wallet = walletClient.getWalletByNumber(request.getWalletNumber());
        if (!wallet.getUserId().equals(userId)) {
            throw new InvalidGroupStateException(
                    "The wallet number provided does not belong to your account");
        }
        if (!"ACTIVE".equals(wallet.getStatus())) {
            throw new InvalidGroupStateException(
                    "Your wallet must be ACTIVE to join a savings group");
        }

        // Payout order = next available slot
        int payoutOrder = currentCount + 1;

        GroupMember member = GroupMember.builder()
                .group(group)
                .userId(userId)
                .userEmail(userEmail)
                .fullName(fullName)
                .walletNumber(request.getWalletNumber())
                .payoutOrder(payoutOrder)
                .status(MemberStatus.ACTIVE)
                .build();

        GroupMember saved = memberRepository.save(member);

        // If group is now full, automatically transition to ACTIVE
        if (payoutOrder == group.getMaxMembers()) {
            group.setStatus(GroupStatus.ACTIVE);
            groupRepository.save(group);
            log.info("Group {} is now ACTIVE (all {} slots filled)", groupId, group.getMaxMembers());
        }

        log.info("Member joined: groupId={} userId={} payoutOrder={}", groupId, userId, payoutOrder);
        eventPublisher.publishMemberJoined(group, saved);

        return MemberResponse.from(saved);
    }

    // ================================================================
    // CONTRIBUTE (pay this round's contribution)
    // ================================================================

    @Transactional
    public ContributionResponse contribute(Long groupId, Long userId, ContributeRequest request) {

        log.info("Contribution: groupId={} userId={}", groupId, userId);

        SavingsGroup group = getGroupOrThrow(groupId);

        if (group.getStatus() != GroupStatus.ACTIVE) {
            throw new GroupNotActiveException(
                    "Group is not active (status=" + group.getStatus() + "). Contributions are only accepted when the group is ACTIVE.");
        }

        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new NotGroupMemberException(
                        "You are not a member of this savings group"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new NotGroupMemberException(
                    "Your membership is " + member.getStatus() + " and you cannot contribute");
        }

        // Verify the wallet matches the one registered when joining
        if (!member.getWalletNumber().equals(request.getWalletNumber())) {
            throw new InvalidGroupStateException(
                    "The wallet number does not match the one you registered with this group. " +
                    "Please use wallet: " + member.getWalletNumber());
        }

        int currentRound = group.getCurrentRound();

        // Check not already contributed this round
        if (contributionRepository.findByMemberIdAndRoundNumber(member.getId(), currentRound).isPresent()) {
            throw new InvalidGroupStateException(
                    "You have already contributed for round " + currentRound);
        }

        String referenceCode = generateContributionCode(groupId, currentRound, member.getId());

        // Create PENDING contribution record first
        Contribution contribution = Contribution.builder()
                .group(group)
                .member(member)
                .roundNumber(currentRound)
                .amount(group.getContributionAmount())
                .currency(group.getCurrency())
                .walletNumber(request.getWalletNumber())
                .referenceCode(referenceCode)
                .status(ContributionStatus.PENDING)
                .build();

        contribution = contributionRepository.save(contribution);

        // Debit the member's wallet
        try {
            walletClient.debitWallet(
                    request.getWalletNumber(),
                    group.getContributionAmount(),
                    referenceCode);

            contribution.setStatus(ContributionStatus.PAID);
            contribution.setPaidAt(LocalDateTime.now());
            contribution = contributionRepository.save(contribution);

            log.info("Contribution PAID: ref={} round={} member={}",
                    referenceCode, currentRound, userId);

        } catch (Exception e) {
            contribution.setStatus(ContributionStatus.FAILED);
            contribution.setFailureReason(e.getMessage());
            contribution = contributionRepository.save(contribution);

            log.error("Contribution FAILED: ref={} error={}", referenceCode, e.getMessage());
        }

        eventPublisher.publishContributionMade(contribution);

        return ContributionResponse.from(contribution);
    }

    // ================================================================
    // PROCESS ROUND PAYOUT
    // Collects all contributions for the current round and pays out the pot
    // to the member whose payoutOrder matches the current round number.
    // Can only be triggered by the group creator or an ADMIN.
    // ================================================================

    @Transactional
    public PayoutResponse processRoundPayout(Long groupId, Long requestingUserId) {

        log.info("Payout requested: groupId={} by userId={}", groupId, requestingUserId);

        SavingsGroup group = getGroupOrThrow(groupId);

        if (group.getStatus() != GroupStatus.ACTIVE) {
            throw new GroupNotActiveException(
                    "Group is not active — payouts are only allowed for ACTIVE groups");
        }

        // Only the group creator or an admin can trigger the payout
        if (!group.getCreatorUserId().equals(requestingUserId)) {
            throw new InvalidGroupStateException(
                    "Only the group creator can trigger the payout for this round");
        }

        int currentRound = group.getCurrentRound();

        // Check payout has not already been processed for this round
        if (payoutRepository.findByGroupIdAndRoundNumber(groupId, currentRound).isPresent()) {
            throw new InvalidGroupStateException(
                    "Payout for round " + currentRound + " has already been processed");
        }

        // Find the payout recipient for this round
        GroupMember recipient = memberRepository.findPayoutRecipientForRound(groupId, currentRound)
                .orElseThrow(() -> new InvalidGroupStateException(
                        "No active member found for round " + currentRound +
                        " (payoutOrder=" + currentRound + ")"));

        // Sum all PAID contributions for this round
        BigDecimal potAmount = contributionRepository.sumPaidAmountForRound(groupId, currentRound);

        if (potAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new InvalidGroupStateException(
                    "No contributions have been paid for round " + currentRound +
                    ". Cannot process an empty payout.");
        }

        String referenceCode = generatePayoutCode(groupId, currentRound);

        // Create SCHEDULED payout record
        Payout payout = Payout.builder()
                .group(group)
                .recipientMember(recipient)
                .roundNumber(currentRound)
                .amount(potAmount)
                .currency(group.getCurrency())
                .recipientWalletNumber(recipient.getWalletNumber())
                .referenceCode(referenceCode)
                .status(PayoutStatus.SCHEDULED)
                .build();

        payout = payoutRepository.save(payout);

        // Credit recipient's wallet
        try {
            walletClient.creditWallet(recipient.getWalletNumber(), potAmount, referenceCode);

            payout.setStatus(PayoutStatus.COMPLETED);
            payout.setCompletedAt(LocalDateTime.now());
            payout = payoutRepository.save(payout);

            // Mark recipient as having received their payout
            recipient.setHasReceivedPayout(true);
            memberRepository.save(recipient);

            log.info("Payout COMPLETED: ref={} round={} recipient={} amount={}",
                    referenceCode, currentRound, recipient.getUserEmail(), potAmount);

        } catch (Exception e) {
            payout.setStatus(PayoutStatus.FAILED);
            payout.setFailureReason(e.getMessage());
            payout = payoutRepository.save(payout);

            log.error("Payout FAILED: ref={} error={}", referenceCode, e.getMessage());
            eventPublisher.publishPayoutProcessed(payout);
            throw new WalletServiceException(
                    "Payout credit failed: " + e.getMessage(), 422);
        }

        eventPublisher.publishPayoutProcessed(payout);

        // Advance to next round (or complete the group)
        int nextRound = currentRound + 1;
        if (nextRound > group.getMaxMembers()) {
            group.setStatus(GroupStatus.COMPLETED);
            log.info("Group {} COMPLETED after {} rounds", groupId, group.getMaxMembers());
        } else {
            group.setCurrentRound(nextRound);
            log.info("Group {} advanced to round {}", groupId, nextRound);
        }
        groupRepository.save(group);

        return PayoutResponse.from(payout);
    }

    // ================================================================
    // QUERIES — Groups
    // ================================================================

    @Transactional(readOnly = true)
    public GroupResponse getGroup(Long groupId) {
        SavingsGroup group = getGroupOrThrow(groupId);
        int count = memberRepository.countByGroupId(groupId);
        return GroupResponse.from(group, count);
    }

    @Transactional(readOnly = true)
    public PagedResponse<GroupResponse> listGroups(GroupStatus status, Pageable pageable) {
        Page<SavingsGroup> page = (status != null)
                ? groupRepository.findByStatus(status, pageable)
                : groupRepository.findAll(pageable);
        return PagedResponse.from(page, g -> {
            int count = memberRepository.countByGroupId(g.getId());
            return GroupResponse.from(g, count);
        });
    }

    @Transactional(readOnly = true)
    public PagedResponse<GroupResponse> getMyGroups(Long userId, Pageable pageable) {
        Page<SavingsGroup> page = groupRepository.findGroupsByMemberUserId(userId, pageable);
        return PagedResponse.from(page, g -> {
            int count = memberRepository.countByGroupId(g.getId());
            return GroupResponse.from(g, count);
        });
    }

    // ================================================================
    // QUERIES — Members
    // ================================================================

    @Transactional(readOnly = true)
    public List<MemberResponse> getGroupMembers(Long groupId) {
        getGroupOrThrow(groupId); // validates group exists
        return memberRepository.findByGroupIdOrderByPayoutOrder(groupId)
                .stream().map(MemberResponse::from).toList();
    }

    // ================================================================
    // QUERIES — Contributions
    // ================================================================

    @Transactional(readOnly = true)
    public PagedResponse<ContributionResponse> getGroupContributions(Long groupId, Pageable pageable) {
        getGroupOrThrow(groupId);
        Page<Contribution> page = contributionRepository
                .findByGroupIdOrderByRoundNumberDescCreatedAtDesc(groupId, pageable);
        return PagedResponse.from(page, ContributionResponse::from);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ContributionResponse> getMyContributions(Long userId, Long groupId,
                                                                   Pageable pageable) {
        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new NotGroupMemberException(
                        "You are not a member of group " + groupId));
        Page<Contribution> page = contributionRepository
                .findByMemberIdOrderByRoundNumberDesc(member.getId(), pageable);
        return PagedResponse.from(page, ContributionResponse::from);
    }

    // ================================================================
    // QUERIES — Payouts
    // ================================================================

    @Transactional(readOnly = true)
    public List<PayoutResponse> getGroupPayouts(Long groupId) {
        getGroupOrThrow(groupId);
        return payoutRepository.findByGroupIdOrderByRoundNumber(groupId)
                .stream().map(PayoutResponse::from).toList();
    }

    // ================================================================
    // Private helpers
    // ================================================================

    private SavingsGroup getGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(
                        "Savings group not found: " + groupId));
    }

    private String generateContributionCode(Long groupId, int round, Long memberId) {
        String date   = LocalDate.now().toString().replace("-", "");
        String suffix = UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase();
        return "CONT-" + date + "-" + suffix;
    }

    private String generatePayoutCode(Long groupId, int round) {
        String date   = LocalDate.now().toString().replace("-", "");
        String suffix = UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase();
        return "POUT-" + date + "-" + suffix;
    }
}
