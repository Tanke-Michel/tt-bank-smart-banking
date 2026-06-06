package com.example.wallet_service.controller;

import com.example.wallet_service.dto.*;
import com.example.wallet_service.service.WalletService;
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

import java.math.BigDecimal;

/**
 * Wallet REST Controller — all endpoints protected by the gateway JWT filter.
 *
 * The authenticated user's identity is extracted from Spring Security context
 * (populated by GatewayAuthenticationFilter from X-Auth-* headers).
 *
 * Base path: /api/v1/wallet
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    // ================================================================
    // POST /api/v1/wallet
    // Create wallet for the authenticated user.
    // ================================================================
    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
            @RequestHeader("X-Auth-User-Id")    String userIdStr,
            @RequestHeader("X-Auth-User-Email") String email,
            @RequestHeader("X-Auth-User-Email") String ownerEmail,
            @RequestHeader(value = "X-Auth-User-Role", defaultValue = "USER") String role,
            @RequestHeader("X-Auth-User-Email") String fullNameHeader,
            @Valid @RequestBody CreateWalletRequest request) {

        Long userId = Long.parseLong(userIdStr);
        // ownerName is passed via a dedicated header we add; fallback to email prefix
        String ownerName = email.split("@")[0];

        WalletResponse response = walletService.createWallet(userId, ownerName, email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ================================================================
    // POST /api/v1/wallet/create
    // Create wallet — alternative endpoint that accepts ownerName in body.
    // ================================================================
    @PostMapping("/create")
    public ResponseEntity<WalletResponse> createWalletWithName(
            @RequestHeader("X-Auth-User-Id")    String userIdStr,
            @RequestHeader("X-Auth-User-Email") String email,
            @RequestHeader(value = "X-Auth-User-Name", required = false) String ownerNameHeader,
            @Valid @RequestBody CreateWalletRequest request) {

        Long userId = Long.parseLong(userIdStr);
        String ownerName = (ownerNameHeader != null && !ownerNameHeader.isBlank())
                ? ownerNameHeader : email.split("@")[0];

        WalletResponse response = walletService.createWallet(userId, ownerName, email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ================================================================
    // GET /api/v1/wallet/me
    // Get the authenticated user's wallet details and balance.
    // ================================================================
    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getMyWallet(
            @RequestHeader("X-Auth-User-Id") String userIdStr) {

        Long userId = Long.parseLong(userIdStr);
        return ResponseEntity.ok(walletService.getMyWallet(userId));
    }

    // ================================================================
    // GET /api/v1/wallet/number/{walletNumber}
    // Look up any wallet by its wallet number (used by Transaction Service).
    // ================================================================
    @GetMapping("/number/{walletNumber}")
    public ResponseEntity<WalletResponse> getByWalletNumber(
            @PathVariable String walletNumber) {
        return ResponseEntity.ok(walletService.getWalletByNumber(walletNumber));
    }

    // ================================================================
    // GET /api/v1/wallet/phone/{phoneNumber}
    // Look up a wallet by phone number (for peer-to-peer transfers).
    // ================================================================
    @GetMapping("/phone/{phoneNumber}")
    public ResponseEntity<WalletResponse> getByPhone(
            @PathVariable String phoneNumber) {
        return ResponseEntity.ok(walletService.getWalletByPhone(phoneNumber));
    }

    // ================================================================
    // GET /api/v1/wallet/email/{email}
    // Look up a wallet by email (used by Transaction Service transfers).
    // ================================================================
    @GetMapping("/email/{email}")
    public ResponseEntity<WalletResponse> getByEmail(
            @PathVariable String email) {
        return ResponseEntity.ok(walletService.getWalletByEmail(email));
    }

    // ================================================================
    // POST /api/v1/wallet/deposit
    // Add funds to the authenticated user's wallet.
    // ================================================================
    @PostMapping("/deposit")
    public ResponseEntity<WalletTransactionResponse> deposit(
            @RequestHeader("X-Auth-User-Id") String userIdStr,
            @Valid @RequestBody DepositRequest request) {

        Long userId = Long.parseLong(userIdStr);
        return ResponseEntity.ok(walletService.deposit(userId, request));
    }

    // ================================================================
    // POST /api/v1/wallet/withdraw
    // Withdraw funds from the authenticated user's wallet.
    // ================================================================

    @PostMapping("/momo-topup")
    public ResponseEntity<java.util.Map<String, Object>> momoTopUp(
            @RequestHeader("X-Auth-User-Id") String userIdStr,
            @Valid @RequestBody MomoTopUpRequest request) {
        Long userId = Long.parseLong(userIdStr);
        return ResponseEntity.ok(walletService.topUpViaMomo(userId, request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WalletTransactionResponse> withdraw(
            @RequestHeader("X-Auth-User-Id") String userIdStr,
            @Valid @RequestBody WithdrawRequest request) {

        Long userId = Long.parseLong(userIdStr);
        return ResponseEntity.ok(walletService.withdraw(userId, request));
    }

    // ================================================================
    // GET /api/v1/wallet/transactions
    // Paginated transaction history for the authenticated user's wallet.
    // ================================================================
    @GetMapping("/transactions")
    public ResponseEntity<PagedResponse<WalletTransactionResponse>> getTransactionHistory(
            @RequestHeader("X-Auth-User-Id") String userIdStr,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = Long.parseLong(userIdStr);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(walletService.getTransactionHistory(userId, pageable));
    }

    // ================================================================
    // POST /api/v1/wallet/internal/credit  (internal — service-to-service)
    // POST /api/v1/wallet/internal/debit
    // Called by the Transaction Service to move money during transfers.
    // ================================================================

    @PostMapping("/internal/credit")
    public ResponseEntity<WalletResponse> creditWallet(
            @RequestParam String walletNumber,
            @RequestParam BigDecimal amount,
            @RequestParam String referenceCode) {
        return ResponseEntity.ok(walletService.creditWallet(walletNumber, amount, referenceCode));
    }

    @PostMapping("/internal/debit")
    public ResponseEntity<WalletResponse> debitWallet(
            @RequestParam String walletNumber,
            @RequestParam BigDecimal amount,
            @RequestParam String referenceCode) {
        return ResponseEntity.ok(walletService.debitWallet(walletNumber, amount, referenceCode));
    }

    // ================================================================
    // ADMIN endpoints — ROLE_ADMIN only
    // ================================================================

    @PostMapping("/admin/{walletNumber}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletResponse> suspendWallet(@PathVariable String walletNumber) {
        return ResponseEntity.ok(walletService.suspendWallet(walletNumber));
    }

    @PostMapping("/admin/{walletNumber}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletResponse> reactivateWallet(@PathVariable String walletNumber) {
        return ResponseEntity.ok(walletService.reactivateWallet(walletNumber));
    }
}
