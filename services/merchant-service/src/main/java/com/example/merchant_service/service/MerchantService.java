package com.example.merchant_service.service;

import com.example.merchant_service.dto.*;
import com.example.merchant_service.entity.Merchant;
import com.example.merchant_service.entity.MerchantPayment;
import com.example.merchant_service.enums.MerchantStatus;
import com.example.merchant_service.enums.PaymentStatus;
import com.example.merchant_service.exception.*;
import com.example.merchant_service.repository.MerchantPaymentRepository;
import com.example.merchant_service.repository.MerchantRepository;
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
import java.time.YearMonth;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final MerchantPaymentRepository paymentRepository;
    private final QrCodeService qrCodeService;
    private final WalletServiceClient walletClient;
    private final MerchantEventPublisher eventPublisher;

    @Value("${app.merchant.max-payment-amount}")
    private BigDecimal maxPaymentAmount;

    @Value("${app.merchant.min-payment-amount}")
    private BigDecimal minPaymentAmount;

    // ================================================================
    // REGISTER MERCHANT
    // ================================================================

    @Transactional
    public MerchantResponse register(
            Long ownerUserId, String ownerEmail,
            RegisterMerchantRequest request) {

        log.info("Merchant registration: userId={} businessName={}",
                ownerUserId, request.getBusinessName());

        // One merchant account per user
        if (merchantRepository.existsByOwnerUserId(ownerUserId)) {
            throw new MerchantAlreadyExistsException(
                    "You already have a merchant account registered");
        }

        // One account per business email
        if (merchantRepository.existsByBusinessEmail(request.getBusinessEmail())) {
            throw new MerchantAlreadyExistsException(
                    "A merchant account with email '" + request.getBusinessEmail() +
                    "' already exists");
        }

        // Validate the wallet exists and belongs to this user
        WalletInfo wallet = walletClient.getWalletByNumber(request.getWalletNumber());
        if (!wallet.getUserId().equals(ownerUserId)) {
            throw new IllegalStateException(
                    "The wallet number provided does not belong to your account");
        }
        if (!"ACTIVE".equals(wallet.getStatus())) {
            throw new IllegalStateException(
                    "The provided wallet is not active and cannot receive payments");
        }

        String merchantCode = generateMerchantCode();

        // Generate QR code immediately — encoded payload = merchantCode
        String qrBase64 = qrCodeService.generateQrCodeBase64(merchantCode);

        Merchant merchant = Merchant.builder()
                .merchantCode(merchantCode)
                .ownerUserId(ownerUserId)
                .ownerEmail(ownerEmail)
                .businessName(request.getBusinessName().trim())
                .businessEmail(request.getBusinessEmail().toLowerCase().trim())
                .businessPhone(request.getBusinessPhone().trim())
                .businessAddress(request.getBusinessAddress().trim())
                .businessCategory(request.getBusinessCategory())
                .description(request.getDescription())
                .walletNumber(request.getWalletNumber())
                .qrCodeBase64(qrBase64)
                .status(MerchantStatus.PENDING)
                .build();

        Merchant saved = merchantRepository.save(merchant);
        log.info("Merchant registered PENDING: code={} userId={}", merchantCode, ownerUserId);

        eventPublisher.publishMerchantRegistered(saved);

        return MerchantResponse.from(saved);
    }

    // ================================================================
    // GET MY MERCHANT PROFILE
    // ================================================================

    @Transactional(readOnly = true)
    public MerchantResponse getMyMerchant(Long ownerUserId) {
        Merchant merchant = merchantRepository.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "You do not have a merchant account. Please register first."));
        return MerchantResponse.from(merchant);
    }

    // ================================================================
    // GET MERCHANT BY CODE (public — for QR scanning)
    // ================================================================

    @Transactional(readOnly = true)
    public MerchantResponse getByCode(String merchantCode) {
        Merchant merchant = merchantRepository.findByMerchantCode(merchantCode)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Merchant not found: " + merchantCode));
        return MerchantResponse.from(merchant);
    }

    // ================================================================
    // GET QR CODE
    // ================================================================

    @Transactional
    public String getQrCode(Long ownerUserId) {
        Merchant merchant = merchantRepository.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "You do not have a merchant account."));

        assertMerchantActive(merchant);

        // Regenerate if somehow missing (defensive)
        if (merchant.getQrCodeBase64() == null || merchant.getQrCodeBase64().isBlank()) {
            merchant.setQrCodeBase64(
                    qrCodeService.generateQrCodeBase64(merchant.getMerchantCode()));
            merchantRepository.save(merchant);
        }

        return merchant.getQrCodeBase64();
    }

    // ================================================================
    // PROCESS QR PAYMENT
    // ================================================================

    @Transactional
    public MerchantPaymentResponse pay(
            Long customerUserId, String customerEmail,
            String customerWalletNumber, MerchantPaymentRequest request) {

        log.info("QR payment: customer={} merchant={} amount={}",
                customerEmail, request.getMerchantCode(), request.getAmount());

        // Validate amount
        if (request.getAmount().compareTo(minPaymentAmount) < 0) {
            throw new IllegalStateException(
                    "Minimum payment amount is " + minPaymentAmount);
        }
        if (request.getAmount().compareTo(maxPaymentAmount) > 0) {
            throw new IllegalStateException(
                    "Maximum payment amount is " + maxPaymentAmount);
        }

        // Resolve merchant
        Merchant merchant = merchantRepository.findByMerchantCode(request.getMerchantCode())
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Merchant not found: " + request.getMerchantCode()));

        assertMerchantActive(merchant);

        // Cannot pay yourself
        if (merchant.getOwnerUserId().equals(customerUserId)) {
            throw new IllegalStateException("You cannot pay your own merchant account");
        }

        // Validate customer wallet
        WalletInfo customerWallet = walletClient.getWalletByNumber(customerWalletNumber);
        if (!customerWallet.getUserId().equals(customerUserId)) {
            throw new IllegalStateException(
                    "The wallet number provided does not belong to your account");
        }

        // Create PENDING payment record
        String referenceCode = generatePaymentCode();

        MerchantPayment payment = MerchantPayment.builder()
                .referenceCode(referenceCode)
                .merchant(merchant)
                .customerUserId(customerUserId)
                .customerEmail(customerEmail)
                .customerWalletNumber(customerWalletNumber)
                .merchantWalletNumber(merchant.getWalletNumber())
                .amount(request.getAmount())
                .currency(customerWallet.getCurrency())
                .description(request.getDescription() != null
                        ? request.getDescription()
                        : "Payment to " + merchant.getBusinessName())
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment created PENDING: ref={}", referenceCode);

        eventPublisher.publishPaymentInitiated(payment);

        // Execute saga
        return executePaymentSaga(payment, customerWalletNumber,
                merchant.getWalletNumber(), request.getAmount(), referenceCode);
    }

    // ================================================================
    // MERCHANT DASHBOARD
    // ================================================================

    @Transactional(readOnly = true)
    public MerchantDashboardResponse getDashboard(Long ownerUserId) {
        Merchant merchant = merchantRepository.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "You do not have a merchant account."));

        LocalDateTime todayStart  = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd    = todayStart.plusDays(1);
        LocalDateTime monthStart  = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime monthEnd    = YearMonth.now().atEndOfMonth().atTime(23, 59, 59);
        LocalDateTime allStart    = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime allEnd      = LocalDateTime.now().plusDays(1);

        BigDecimal todayRevenue = paymentRepository.sumCompletedByMerchantAndDateRange(
                merchant.getId(), PaymentStatus.COMPLETED, todayStart, todayEnd);
        BigDecimal monthRevenue = paymentRepository.sumCompletedByMerchantAndDateRange(
                merchant.getId(), PaymentStatus.COMPLETED, monthStart, monthEnd);
        BigDecimal totalRevenue = paymentRepository.sumCompletedByMerchantAndDateRange(
                merchant.getId(), PaymentStatus.COMPLETED, allStart, allEnd);

        long todayCount  = paymentRepository.countByMerchantAndDateRange(
                merchant.getId(), PaymentStatus.COMPLETED, todayStart, todayEnd);
        long monthCount  = paymentRepository.countByMerchantAndDateRange(
                merchant.getId(), PaymentStatus.COMPLETED, monthStart, monthEnd);
        long totalCount  = paymentRepository.countByMerchantAndDateRange(
                merchant.getId(), PaymentStatus.COMPLETED, allStart, allEnd);

        return MerchantDashboardResponse.builder()
                .merchantCode(merchant.getMerchantCode())
                .businessName(merchant.getBusinessName())
                .walletNumber(merchant.getWalletNumber())
                .qrCodeBase64(merchant.getQrCodeBase64())
                .todayRevenue(todayRevenue)
                .monthRevenue(monthRevenue)
                .totalRevenue(totalRevenue)
                .todayTransactionCount(todayCount)
                .monthTransactionCount(monthCount)
                .totalTransactionCount(totalCount)
                .build();
    }

    // ================================================================
    // GET PAYMENT HISTORY (merchant view)
    // ================================================================

    @Transactional(readOnly = true)
    public PagedResponse<MerchantPaymentResponse> getPaymentHistory(
            Long ownerUserId, Pageable pageable) {
        Merchant merchant = merchantRepository.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "You do not have a merchant account."));
        Page<MerchantPayment> page = paymentRepository
                .findByMerchantIdOrderByCreatedAtDesc(merchant.getId(), pageable);
        return PagedResponse.from(page, MerchantPaymentResponse::from);
    }

    // ================================================================
    // GET PAYMENT HISTORY (customer view)
    // ================================================================

    @Transactional(readOnly = true)
    public PagedResponse<MerchantPaymentResponse> getMyPayments(
            Long customerUserId, Pageable pageable) {
        Page<MerchantPayment> page = paymentRepository
                .findByCustomerUserIdOrderByCreatedAtDesc(customerUserId, pageable);
        return PagedResponse.from(page, MerchantPaymentResponse::from);
    }

    // ================================================================
    // GET PAYMENT BY REFERENCE
    // ================================================================

    @Transactional(readOnly = true)
    public MerchantPaymentResponse getPaymentByReference(String referenceCode) {
        MerchantPayment payment = paymentRepository.findByReferenceCode(referenceCode)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found: " + referenceCode));
        return MerchantPaymentResponse.from(payment);
    }

    // ================================================================
    // ADMIN: Update merchant status
    // ================================================================

    @Transactional
    public MerchantResponse updateStatus(Long merchantId, AdminStatusRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Merchant not found: " + merchantId));

        MerchantStatus oldStatus = merchant.getStatus();
        merchant.setStatus(request.getStatus());
        merchant.setStatusReason(request.getReason());

        if (request.getStatus() == MerchantStatus.ACTIVE && oldStatus != MerchantStatus.ACTIVE) {
            merchant.setApprovedAt(LocalDateTime.now());
        }

        Merchant saved = merchantRepository.save(merchant);
        log.info("Merchant status updated: id={} {} → {}", merchantId, oldStatus, request.getStatus());
        return MerchantResponse.from(saved);
    }

    // ================================================================
    // ADMIN: List all merchants (filterable by status)
    // ================================================================

    @Transactional(readOnly = true)
    public PagedResponse<MerchantResponse> listAll(MerchantStatus status, Pageable pageable) {
        Page<Merchant> page = (status != null)
                ? merchantRepository.findByStatus(status, pageable)
                : merchantRepository.findAll(pageable);
        return PagedResponse.from(page, MerchantResponse::from);
    }

    // ================================================================
    // Private — saga
    // ================================================================

    private MerchantPaymentResponse executePaymentSaga(
            MerchantPayment payment,
            String customerWalletNumber,
            String merchantWalletNumber,
            BigDecimal amount,
            String referenceCode) {

        // Phase 1: Debit customer
        try {
            walletClient.debitWallet(customerWalletNumber, amount, referenceCode);
        } catch (Exception e) {
            log.error("Payment debit failed ref={}: {}", referenceCode, e.getMessage());
            payment = markPaymentFailed(payment, "Debit failed: " + e.getMessage());
            eventPublisher.publishPaymentFailed(payment);
            if (e instanceof com.example.merchant_service.exception.WalletServiceException wse) {
                throw wse;
            }
            throw new IllegalStateException("Payment debit failed: " + e.getMessage());
        }

        // Phase 2: Credit merchant
        try {
            walletClient.creditWallet(merchantWalletNumber, amount, referenceCode);
        } catch (Exception e) {
            log.error("Payment credit failed ref={}: {} — reversing", referenceCode, e.getMessage());
            walletClient.reversalCredit(customerWalletNumber, amount, referenceCode);
            payment = markPaymentFailed(payment,
                    "Credit failed: " + e.getMessage() + ". Your funds have been returned.");
            eventPublisher.publishPaymentFailed(payment);
            throw new com.example.merchant_service.exception.WalletServiceException(
                    "Payment failed. Your funds have been returned.", 422);
        }

        // Success
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        MerchantPayment saved = paymentRepository.save(payment);

        log.info("Payment COMPLETED: ref={} amount={}", referenceCode, amount);
        eventPublisher.publishPaymentCompleted(saved);
        return MerchantPaymentResponse.from(saved);
    }

    private MerchantPayment markPaymentFailed(MerchantPayment payment, String reason) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        payment.setCompletedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    // ================================================================
    // Private — guards
    // ================================================================

    private void assertMerchantActive(Merchant merchant) {
        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new MerchantNotActiveException(
                    "Merchant '" + merchant.getBusinessName() +
                    "' is not active (status=" + merchant.getStatus() + "). " +
                    "Please ensure your account has been approved before accepting payments.");
        }
    }

    // ================================================================
    // Private — code generators
    // ================================================================

    private String generateMerchantCode() {
        String date   = LocalDate.now().toString().replace("-", "");
        String suffix = UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase();
        return "MCH-" + date + "-" + suffix;
    }

    private String generatePaymentCode() {
        String date   = LocalDate.now().toString().replace("-", "");
        String suffix = UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase();
        return "PAY-" + date + "-" + suffix;
    }
}
