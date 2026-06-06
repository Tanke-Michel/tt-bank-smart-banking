package com.example.savings_service.controller;

import com.example.savings_service.dto.*;
import com.example.savings_service.enums.GroupStatus;
import com.example.savings_service.service.SavingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Community Savings (Tontine / Njangi) REST Controller.
 *
 * Base path: /api/v1/savings
 *
 * Endpoints:
 *   POST   /groups                              — Create a savings group
 *   GET    /groups                              — Browse available groups (filter by status)
 *   GET    /groups/mine                         — My groups (as member)
 *   GET    /groups/{groupId}                    — Group detail
 *   POST   /groups/{groupId}/join               — Join a group
 *   GET    /groups/{groupId}/members            — List all members
 *   POST   /groups/{groupId}/contribute         — Pay this round's contribution
 *   POST   /groups/{groupId}/payout             — Trigger round payout (creator only)
 *   GET    /groups/{groupId}/contributions      — All contributions for a group
 *   GET    /groups/{groupId}/contributions/mine — My contributions in a group
 *   GET    /groups/{groupId}/payouts            — All payouts for a group
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/savings")
@RequiredArgsConstructor
public class SavingsController {

    private final SavingsService savingsService;

    // ================================================================
    // POST /api/v1/savings/groups
    // Create a new savings group. Creator is auto-added as member #1.
    // ================================================================
    @PostMapping("/groups")
    public ResponseEntity<GroupResponse> createGroup(
            @RequestHeader("X-Auth-User-Id")    String userIdStr,
            @RequestHeader("X-Auth-User-Email") String email,
            @RequestHeader(value = "X-Auth-User-Name", required = false) String fullName,
            @Valid @RequestBody CreateGroupRequest request) {

        Long userId = Long.parseLong(userIdStr);
        String name = (fullName != null && !fullName.isBlank()) ? fullName : email.split("@")[0];
        GroupResponse response = savingsService.createGroup(userId, email, name, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ================================================================
    // GET /api/v1/savings/groups
    // Browse all groups. Optional status filter.
    // ================================================================
    @GetMapping("/groups")
    public ResponseEntity<PagedResponse<GroupResponse>> listGroups(
            @RequestParam(required = false) GroupStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(
                page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(savingsService.listGroups(status, pageable));
    }

    // ================================================================
    // GET /api/v1/savings/groups/mine
    // List groups the authenticated user belongs to.
    // ================================================================
    @GetMapping("/groups/mine")
    public ResponseEntity<PagedResponse<GroupResponse>> getMyGroups(
            @RequestHeader("X-Auth-User-Id") String userIdStr,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = Long.parseLong(userIdStr);
        Pageable pageable = PageRequest.of(
                page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(savingsService.getMyGroups(userId, pageable));
    }

    // ================================================================
    // GET /api/v1/savings/groups/{groupId}
    // Get group details including current round and member count.
    // ================================================================
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(savingsService.getGroup(groupId));
    }

    // ================================================================
    // POST /api/v1/savings/groups/{groupId}/join
    // Join a savings group (only during FORMING status).
    // ================================================================
    @PostMapping("/groups/{groupId}/join")
    public ResponseEntity<MemberResponse> joinGroup(
            @PathVariable Long groupId,
            @RequestHeader("X-Auth-User-Id")    String userIdStr,
            @RequestHeader("X-Auth-User-Email") String email,
            @RequestHeader(value = "X-Auth-User-Name", required = false) String fullName,
            @Valid @RequestBody JoinGroupRequest request) {

        Long userId = Long.parseLong(userIdStr);
        String name = (fullName != null && !fullName.isBlank()) ? fullName : email.split("@")[0];
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savingsService.joinGroup(groupId, userId, email, name, request));
    }

    // ================================================================
    // GET /api/v1/savings/groups/{groupId}/members
    // List all members in payout order.
    // ================================================================
    @GetMapping("/groups/{groupId}/members")
    public ResponseEntity<List<MemberResponse>> getMembers(@PathVariable Long groupId) {
        return ResponseEntity.ok(savingsService.getGroupMembers(groupId));
    }

    // ================================================================
    // POST /api/v1/savings/groups/{groupId}/contribute
    // Pay this round's contribution from the member's wallet.
    // ================================================================
    @PostMapping("/groups/{groupId}/contribute")
    public ResponseEntity<ContributionResponse> contribute(
            @PathVariable Long groupId,
            @RequestHeader("X-Auth-User-Id") String userIdStr,
            @Valid @RequestBody ContributeRequest request) {

        Long userId = Long.parseLong(userIdStr);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savingsService.contribute(groupId, userId, request));
    }

    // ================================================================
    // POST /api/v1/savings/groups/{groupId}/payout
    // Trigger the payout for the current round.
    // Only the group creator can call this.
    // ================================================================
    @PostMapping("/groups/{groupId}/payout")
    public ResponseEntity<PayoutResponse> processPayout(
            @PathVariable Long groupId,
            @RequestHeader("X-Auth-User-Id") String userIdStr) {

        Long userId = Long.parseLong(userIdStr);
        return ResponseEntity.ok(savingsService.processRoundPayout(groupId, userId));
    }

    // ================================================================
    // GET /api/v1/savings/groups/{groupId}/contributions
    // All contributions for a group (paginated, newest first).
    // ================================================================
    @GetMapping("/groups/{groupId}/contributions")
    public ResponseEntity<PagedResponse<ContributionResponse>> getContributions(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(savingsService.getGroupContributions(groupId, pageable));
    }

    // ================================================================
    // GET /api/v1/savings/groups/{groupId}/contributions/mine
    // The authenticated user's own contributions within a group.
    // ================================================================
    @GetMapping("/groups/{groupId}/contributions/mine")
    public ResponseEntity<PagedResponse<ContributionResponse>> getMyContributions(
            @PathVariable Long groupId,
            @RequestHeader("X-Auth-User-Id") String userIdStr,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = Long.parseLong(userIdStr);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(savingsService.getMyContributions(userId, groupId, pageable));
    }

    // ================================================================
    // GET /api/v1/savings/groups/{groupId}/payouts
    // All payouts for a group, in round order.
    // ================================================================
    @GetMapping("/groups/{groupId}/payouts")
    public ResponseEntity<List<PayoutResponse>> getPayouts(@PathVariable Long groupId) {
        return ResponseEntity.ok(savingsService.getGroupPayouts(groupId));
    }
}
