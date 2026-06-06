package com.example.transaction_service.controller;

import com.example.transaction_service.dto.*;
import com.example.transaction_service.service.TransactionService;
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

/**
 * Transaction REST Controller.
 *
 * All endpoints are protected — the gateway validates JWT and injects
 * X-Auth-* headers. GatewayAuthenticationFilter reads those headers
 * and populates Spring SecurityContext.
 *
 * Base path: /api/v1/transactions
 *
 * Design note: senderWalletNumber is passed as a request header
 * (X-Auth-User-Wallet) so the frontend/gateway can supply it after
 * the user selects their wallet. If not supplied, the service resolves
 * the sender's wallet by userId via the wallet-service.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // ================================================================
    // POST /api/v1/transactions/transfer
    // Initiate a peer-to-peer money transfer.
    // ================================================================
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @RequestHeader("X-Auth-User-Id")           String userIdStr,
            @RequestHeader("X-Auth-User-Email")        String email,
            @RequestHeader("X-Auth-User-Wallet")       String senderWalletNumber,
            @Valid @RequestBody TransferRequest request) {

        Long userId = Long.parseLong(userIdStr);
        TransactionResponse response = transactionService.transfer(
                userId, email, senderWalletNumber, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ================================================================
    // GET /api/v1/transactions/{referenceCode}
    // Get a single transaction by reference code.
    // ================================================================
    @GetMapping("/{referenceCode}")
    public ResponseEntity<TransactionResponse> getByReference(
            @PathVariable String referenceCode) {
        return ResponseEntity.ok(transactionService.getByReferenceCode(referenceCode));
    }

    // ================================================================
    // GET /api/v1/transactions/history
    // All transactions (sent and received) for the authenticated user.
    // ================================================================
    @GetMapping("/history")
    public ResponseEntity<PagedResponse<TransactionResponse>> getHistory(
            @RequestHeader("X-Auth-User-Id") String userIdStr,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = Long.parseLong(userIdStr);
        Pageable pageable = PageRequest.of(
                page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(transactionService.getHistory(userId, pageable));
    }

    // ================================================================
    // GET /api/v1/transactions/sent
    // Outgoing transfers only.
    // ================================================================
    @GetMapping("/sent")
    public ResponseEntity<PagedResponse<TransactionResponse>> getSent(
            @RequestHeader("X-Auth-User-Id") String userIdStr,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = Long.parseLong(userIdStr);
        Pageable pageable = PageRequest.of(
                page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(transactionService.getSent(userId, pageable));
    }

    // ================================================================
    // GET /api/v1/transactions/received
    // Incoming transfers only.
    // ================================================================
    @GetMapping("/received")
    public ResponseEntity<PagedResponse<TransactionResponse>> getReceived(
            @RequestHeader("X-Auth-User-Id") String userIdStr,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = Long.parseLong(userIdStr);
        Pageable pageable = PageRequest.of(
                page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(transactionService.getReceived(userId, pageable));
    }

    // ================================================================
    // POST /api/v1/transactions/{referenceCode}/reverse
    // ADMIN only — reverse a completed transaction.
    // ================================================================
    @PostMapping("/{referenceCode}/reverse")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionResponse> reverse(
            @PathVariable String referenceCode,
            @RequestHeader("X-Auth-User-Id") String adminUserIdStr) {

        Long adminUserId = Long.parseLong(adminUserIdStr);
        return ResponseEntity.ok(
                transactionService.reverseTransaction(referenceCode, adminUserId));
    }
}
