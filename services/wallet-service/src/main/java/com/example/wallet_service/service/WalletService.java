package com.example.wallet_service.service;

import com.example.wallet_service.dto.*;
import com.example.wallet_service.entity.Wallet;
import com.example.wallet_service.entity.WalletTransaction;
import com.example.wallet_service.enums.Currency;
import com.example.wallet_service.enums.TransactionType;
import com.example.wallet_service.enums.WalletStatus;
import com.example.wallet_service.exception.*;
import com.example.wallet_service.repository.WalletRepository;
import com.example.wallet_service.repository.WalletTransactionRepository;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final CamPayService camPayService;

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final WalletEventPublisher eventPublisher;

    @Value("${app.wallet.max-deposit-amount}")
    private BigDecimal maxDepositAmount;

    @Value("${app.wallet.max-withdrawal-amount}")
    private BigDecimal maxWithdrawalAmount;

    @Value("${app.wallet.daily-withdrawal-limit}")
    private BigDecimal dailyWithdrawalLimit;

    @Value("${app.wallet.minimum-balance}")
    private BigDecimal minimumBalance;

    // ================================================================
    // CREATE WALLET
    // ================================================================

    @Transactional
    public WalletResponse createWallet(Long userId, String ownerName,
                                       String email, CreateWalletRequest request) {
        log.info("Creating wallet for userId={} email={}", userId, email);

        if (walletRepository.existsByUserId(userId)) {
            throw new WalletAlreadyExistsException(
                    "A wallet already exists for this account");
        }
        if (walletRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new WalletAlreadyExistsException(
                    "Phone number " + request.getPhoneNumber() + " is already linked to a wallet");
        }

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .walletNumber(generateWalletNumber())
                .ownerName(ownerName)
                .email(email)
                .phoneNumber(request.getPhoneNumber())
                .balance(BigDecimal.ZERO)
                .currency(request.getCurrency() != null ? request.getCurrency() : Currency.XAF)
                .status(WalletStatus.ACTIVE)
                .build();

        Wallet saved = walletRepository.save(wallet);
        log.info("Wallet created: walletNumber={} userId={}", saved.getWalletNumber(), userId);

        eventPublisher.publishWalletCreated(saved);

        return WalletResponse.from(saved);
    }

    // ================================================================
    // GET MY WALLET
    // ================================================================

    @Transactional(readOnly = true)
    public WalletResponse getMyWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(
                        "No wallet found for this account. Please create one first."));
        return WalletResponse.from(wallet);
    }

    // ================================================================
    // GET WALLET BY NUMBER (used by Transaction Service)
    // ================================================================

    @Transactional(readOnly = true)
    public WalletResponse getWalletByNumber(String walletNumber) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new WalletNotFoundException(
                        "Wallet not found: " + walletNumber));
        return WalletResponse.from(wallet);
    }

    // ================================================================
    // GET WALLET BY PHONE (used by peer-to-peer transfers)
    // ================================================================

    @Transactional(readOnly = true)
    public WalletResponse getWalletByPhone(String phoneNumber) {
        Wallet wallet = walletRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new WalletNotFoundException(
                        "No wallet found for phone number: " + phoneNumber));
        return WalletResponse.from(wallet);
    }

    // ================================================================
    // GET WALLET BY EMAIL (used by transaction-service for transfers)
    // ================================================================

    @Transactional(readOnly = true)
    public WalletResponse getWalletByEmail(String email) {
        Wallet wallet = walletRepository.findByEmail(email)
                .orElseThrow(() -> new WalletNotFoundException(
                        "No wallet found for email: " + email));
        return WalletResponse.from(wallet);
    }

    // ================================================================
    // DEPOSIT
    // ================================================================

    @Transactional
    public WalletTransactionResponse deposit(Long userId, DepositRequest request) {
        log.info("Deposit request: userId={} amount={}", userId, request.getAmount());

        // Validate amount limits
        if (request.getAmount().compareTo(maxDepositAmount) > 0) {
            throw new LimitExceededException(
                    "Deposit amount exceeds maximum allowed: " + maxDepositAmount);
        }

        // Acquire pessimistic lock to prevent concurrent balance corruption
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new WalletNotFoundException(
                        "No wallet found. Please create a wallet first."));

        wallet.assertActive();

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter  = balanceBefore.add(request.getAmount());

        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .currency(wallet.getCurrency())
                .referenceCode(generateReferenceCode("DEP"))
                .description(request.getDescription())
                .build();

        WalletTransaction saved = transactionRepository.save(txn);
        log.info("Deposit completed: walletNumber={} amount={} newBalance={}",
                wallet.getWalletNumber(), request.getAmount(), balanceAfter);

        eventPublisher.publishWalletFunded(wallet, saved);

        return WalletTransactionResponse.from(saved);
    }

    // ================================================================
    // WITHDRAW
    // ================================================================

    @Transactional
    public WalletTransactionResponse withdraw(Long userId, WithdrawRequest request) {
        log.info("Withdrawal request: userId={} amount={}", userId, request.getAmount());

        // Validate single transaction limit
        if (request.getAmount().compareTo(maxWithdrawalAmount) > 0) {
            throw new LimitExceededException(
                    "Withdrawal amount exceeds maximum allowed per transaction: " + maxWithdrawalAmount);
        }

        // Acquire pessimistic lock
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new WalletNotFoundException(
                        "No wallet found. Please create a wallet first."));

        wallet.assertActive();

        // Check daily withdrawal limit
        BigDecimal withdrawnToday = getTodayWithdrawalTotal(wallet.getId());
        BigDecimal projectedTotal = withdrawnToday.add(request.getAmount());
        if (projectedTotal.compareTo(dailyWithdrawalLimit) > 0) {
            throw new LimitExceededException(
                    "Daily withdrawal limit of " + dailyWithdrawalLimit
                    + " exceeded. Already withdrawn today: " + withdrawnToday);
        }

        // Check sufficient balance
        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter  = balanceBefore.subtract(request.getAmount());
        if (balanceAfter.compareTo(minimumBalance) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Available balance: " + balanceBefore
                    + " " + wallet.getCurrency().name());
        }

        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .currency(wallet.getCurrency())
                .referenceCode(generateReferenceCode("WDR"))
                .description(request.getDescription())
                .build();

        WalletTransaction saved = transactionRepository.save(txn);
        log.info("Withdrawal completed: walletNumber={} amount={} newBalance={}",
                wallet.getWalletNumber(), request.getAmount(), balanceAfter);

        eventPublisher.publishWalletWithdrawn(wallet, saved);

        return WalletTransactionResponse.from(saved);
    }

    // ================================================================
    // TRANSACTION HISTORY
    // ================================================================

    @Transactional(readOnly = true)
    public PagedResponse<WalletTransactionResponse> getTransactionHistory(
            Long userId, Pageable pageable) {

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(
                        "No wallet found for this account."));

        Page<WalletTransaction> page = transactionRepository
                .findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable);

        return PagedResponse.from(page, WalletTransactionResponse::from);
    }

    // ================================================================
    // INTERNAL: Credit / Debit used by Transaction Service
    // These methods are called via internal API (not exposed publicly)
    // ================================================================

    @Transactional
    public WalletResponse creditWallet(String walletNumber, BigDecimal amount, String referenceCode) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletNumber));

        wallet.assertActive();

        BigDecimal before = wallet.getBalance();
        BigDecimal after  = before.add(amount);
        wallet.setBalance(after);
        walletRepository.save(wallet);

        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .currency(wallet.getCurrency())
                .referenceCode(referenceCode + "-CR")
                .description("Transfer credit")
                .build();
        transactionRepository.save(txn);

        log.info("Credit applied: walletNumber={} amount={} ref={}",
                walletNumber, amount, referenceCode);
        return WalletResponse.from(wallet);
    }

    @Transactional
    public WalletResponse debitWallet(String walletNumber, BigDecimal amount, String referenceCode) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletNumber));

        wallet.assertActive();

        BigDecimal before = wallet.getBalance();
        if (before.subtract(amount).compareTo(minimumBalance) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds in wallet " + walletNumber
                    + ". Available: " + before);
        }

        BigDecimal after = before.subtract(amount);
        wallet.setBalance(after);
        walletRepository.save(wallet);

        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.WITHDRAWAL)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .currency(wallet.getCurrency())
                .referenceCode(referenceCode + "-DR")
                .description("Transfer debit")
                .build();
        transactionRepository.save(txn);

        log.info("Debit applied: walletNumber={} amount={} ref={}",
                walletNumber, amount, referenceCode);
        return WalletResponse.from(wallet);
    }

    // ================================================================
    // ADMIN: Suspend / Reactivate wallet
    // ================================================================

    @Transactional
    public WalletResponse suspendWallet(String walletNumber) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletNumber));
        wallet.setStatus(WalletStatus.SUSPENDED);
        return WalletResponse.from(walletRepository.save(wallet));
    }

    @Transactional
    public WalletResponse reactivateWallet(String walletNumber) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletNumber));
        wallet.setStatus(WalletStatus.ACTIVE);
        return WalletResponse.from(walletRepository.save(wallet));
    }

    // ================================================================
    // Private helpers
    // ================================================================

    private BigDecimal getTodayWithdrawalTotal(Long walletId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = startOfDay.plusDays(1);
        BigDecimal total = transactionRepository.sumAmountByWalletIdAndTypeAndCreatedAtBetween(
                walletId, TransactionType.WITHDRAWAL, startOfDay, endOfDay);
        return total != null ? total : BigDecimal.ZERO;
    }

    private String generateWalletNumber() {
        // Format: WLT-YYYYMMDD-XXXXXXXX (8 hex chars from UUID)
        String date = LocalDate.now().toString().replace("-", "");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "WLT-" + date + "-" + suffix;
    }

    private String generateReferenceCode(String prefix) {
        // Format: DEP-YYYYMMDD-XXXXXXXX
        String date = LocalDate.now().toString().replace("-", "");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + "-" + date + "-" + suffix;
    }


    public java.util.Map<String, Object> topUpViaMomo(Long userId, com.example.wallet_service.dto.MomoTopUpRequest request) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("No wallet found. Please create a wallet first."));
        wallet.assertActive();

        String ref = camPayService.collect(
                request.getPhoneNumber(),
                request.getAmount().toBigInteger().toString(),
                request.getDescription() == null ? "TT-BANK Top-Up" : request.getDescription(),
                "TOPUP-" + System.currentTimeMillis());

        String status = "PENDING";
        for (int i = 0; i < 10; i++) {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            status = camPayService.status(ref);
            if (!"PENDING".equalsIgnoreCase(status)) break;
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("reference", ref);
        result.put("status", status);

        if ("SUCCESSFUL".equalsIgnoreCase(status)) {
            creditWallet(wallet.getWalletNumber(), request.getAmount(), ref);
            wallet = walletRepository.findByUserId(userId).orElse(wallet);
            result.put("message", "Top-up successful");
            result.put("balance", wallet.getBalance());
        } else {
            result.put("message", "Mobile money payment " + status.toLowerCase());
        }
        return result;
    }

}
