package com.example.merchant_service.controller;

import com.example.merchant_service.dto.*;
import com.example.merchant_service.enums.MerchantStatus;
import com.example.merchant_service.service.MerchantService;
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
 * Merchant REST Controller.
 *
 * Base path: /api/v1/merchants
 *
 * Endpoint summary:
 *   POST   /register                    — Any authenticated USER/MERCHANT registers a merchant account
 *   GET    /me                          — Get own merchant profile
 *   GET    /me/qr-code                  — Get own QR code (Base64 PNG)
 *   GET    /me/dashboard                — Revenue & transaction stats
 *   GET    /me/payments                 — Payment history received by this merchant
 *   POST   /pay                         — Customer pays a merchant via QR code
 *   GET    /my-payments                 — Customer: list my own payments TO merchants
 *   GET    /{merchantCode}              — Public: resolve merchant by code (used by QR scanner)
 *   GET    /payments/{referenceCode}    — Get a specific payment
 *   GET    /admin/all                   — ADMIN: list all merchants
 *   PUT    /admin/{merchantId}/status   — ADMIN: approve/suspend/reject a merchant
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    // ================================================================
    // POST /api/v1/merchants/register
    // Register a new merchant account. Any authenticated user may register.
    // Account starts in PENDING status — admin must approve.
    // ================================================================
    @PostMapping("/register")
    public ResponseEntity<MerchantResponse> register(
            @RequestHeader("X-Auth-User-Id")    String userIdStr,
            @RequestHeader("X-Auth-User-Email") String email,
            @Valid @RequestBody RegisterMerchantRequest request) {

        Long userId = Long.parseLong(userIdStr);
        MerchantResponse response = merchantService.register(userId, email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ================================================================
    // GET /api/v1/merchants/me
    // Get the authenticated user's own merchant profile.
    // ================================================================
    @GetMapping("/me")
    public ResponseEntity<MerchantResponse> getMyMerchant(
            @RequestHeader("X-Auth-User-Id") String userIdStr) {

        Long userId = Long.parseLong(userIdStr);
        return ResponseEntity.ok(merchantService.getMyMerchant(userId));
    }

    // ================================================================
    // GET /api/v1/merchants/me/qr-code
    // Returns the merchant's QR code as a Base64-encoded PNG string.
    // Only ACTIVE merchants can retrieve their QR code.
    // ================================================================
    @GetMapping("/me/qr-code")
    public ResponseEntity<MessageResponse> getQrCode(
            @RequestHeader("X-Auth-User-Id") String userIdStr) {

        Long userId = Long.parseLong(userIdStr);
        String qrBase64 = merchantService.getQrCode(userId);
        return ResponseEntity.ok(new MessageResponse(qrBase64));
    }

    // ================================================================
    // GET /api/v1/merchants/me/dashboard
    // Revenue and transaction statistics for the merchant's dashboard.
    // ================================================================
    @GetMapping("/me/dashboard")
    public ResponseEntity<MerchantDashboardResponse> getDashboard(
            @RequestHeader("X-Auth-User-Id") String userIdStr) {

        Long userId = Long.parseLong(userIdStr);
        return ResponseEntity.ok(merchantService.getDashboard(userId));
    }

    // ================================================================
    // GET /api/v1/merchants/me/payments
    // Paginated list of payments RECEIVED by this merchant.
    // ================================================================
    @GetMapping("/me/payments")
    public ResponseEntity<PagedResponse<MerchantPaymentResponse>> getMerchantPayments(
            @RequestHeader("X-Auth-User-Id") String userIdStr,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = Long.parseLong(userIdStr);
        Pageable pageable = PageRequest.of(
                page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(merchantService.getPaymentHistory(userId, pageable));
    }

    // ================================================================
    // POST /api/v1/merchants/pay
    // A customer pays a merchant. The customer's app decodes the QR code
    // (which contains the merchantCode) and calls this endpoint.
    // ================================================================
    @PostMapping("/pay")
    public ResponseEntity<MerchantPaymentResponse> pay(
            @RequestHeader("X-Auth-User-Id")    String userIdStr,
            @RequestHeader("X-Auth-User-Email") String email,
            @RequestHeader("X-Auth-User-Wallet") String customerWalletNumber,
            @Valid @RequestBody MerchantPaymentRequest request) {

        // Set the wallet number from the header into the request
        // (the request body also has it but we trust the gateway header as source of truth)
        request.setCustomerWalletNumber(customerWalletNumber);

        Long userId = Long.parseLong(userIdStr);
        MerchantPaymentResponse response = merchantService.pay(
                userId, email, customerWalletNumber, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ================================================================
    // GET /api/v1/merchants/my-payments
    // A customer's history of payments MADE TO merchants.
    // ================================================================
    @GetMapping("/my-payments")
    public ResponseEntity<PagedResponse<MerchantPaymentResponse>> getMyPayments(
            @RequestHeader("X-Auth-User-Id") String userIdStr,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = Long.parseLong(userIdStr);
        Pageable pageable = PageRequest.of(
                page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(merchantService.getMyPayments(userId, pageable));
    }

    // ================================================================
    // GET /api/v1/merchants/{merchantCode}
    // Resolve a merchant by their code. Public endpoint — used by the
    // customer's app after scanning the QR code to show merchant info
    // before confirming payment.
    // ================================================================
    @GetMapping("/{merchantCode}")
    public ResponseEntity<MerchantResponse> getByCode(
            @PathVariable String merchantCode) {
        return ResponseEntity.ok(merchantService.getByCode(merchantCode));
    }

    // ================================================================
    // GET /api/v1/merchants/payments/{referenceCode}
    // Get a specific payment by reference code.
    // ================================================================
    @GetMapping("/payments/{referenceCode}")
    public ResponseEntity<MerchantPaymentResponse> getPayment(
            @PathVariable String referenceCode) {
        return ResponseEntity.ok(merchantService.getPaymentByReference(referenceCode));
    }

    // ================================================================
    // GET /api/v1/merchants/admin/all   — ADMIN ONLY
    // List all merchants, optionally filtered by status.
    // ================================================================
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<MerchantResponse>> listAll(
            @RequestParam(required = false) MerchantStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(
                page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(merchantService.listAll(status, pageable));
    }

    // ================================================================
    // PUT /api/v1/merchants/admin/{merchantId}/status   — ADMIN ONLY
    // Approve, suspend, or reject a merchant.
    // ================================================================
    @PutMapping("/admin/{merchantId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MerchantResponse> updateStatus(
            @PathVariable Long merchantId,
            @Valid @RequestBody AdminStatusRequest request) {

        return ResponseEntity.ok(merchantService.updateStatus(merchantId, request));
    }
}
