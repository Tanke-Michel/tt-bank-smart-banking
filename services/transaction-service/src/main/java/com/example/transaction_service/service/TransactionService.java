package com.example.transaction_service.service;

import com.example.transaction_service.dto.*;
import com.example.transaction_service.entity.Transaction;
import com.example.transaction_service.enums.TransactionStatus;
import com.example.transaction_service.enums.TransactionType;
import com.example.transaction_service.exception.*;
import com.example.transaction_service.repository.TransactionRepository;
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

/**
 * Core business logic for peer-to-peer transfers.
 *
 * Transfer saga pattern:
 *  1. Validate: sender != receiver, amounts within limits, daily limit not exceeded.
 *  2. Resolve wallets: look up sender wallet (by number from JWT) and receiver wallet (by email).
 *  3. Create a PENDING Transaction record first — idempotency safety net.
 *  4. Debit sender's wallet via wallet-service HTTP call.
 *  5. Credit receiver's wallet via wallet-service HTTP call.
 *  6. If step 5 fails: compensate by re-crediting sender (reversal), mark FAILED.
 *  7. On success: mark COMPLETED, publish event.
 *  8. On any failure: mark FAILED, publish failure event.
 *
 * The Transaction record is saved BEFORE the wallet operations.
 * This means if the service crashes mid-saga, the PENDING record exists
 * and can be retried or manually resolved by operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletServiceClient walletClient;
    private final TransactionEventPublisher eventPublisher;

    @Value("${app.transaction.max-transfer-amount}")
    private BigDecimal maxTransferAmount;

    @Value("${app.transaction.min-transfer-amount}")
    private BigDecimal minTransferAmount;

    @Value("${app.transaction.daily-transfer-limit}")
    private BigDecimal dailyTransferLimit;

    // ================================================================
    // INITIATE TRANSFER
    // ================================================================

    /**
     * Executes a peer-to-peer money transfer.
     *
     * @param senderUserId     from X-Auth-User-Id header (trusted gateway header)
     * @param senderEmail      from X-Auth-User-Email header (trusted gateway header)
     * @param senderWalletNumber from X-Auth-User-Wallet header set by the gateway
     *                           OR resolved from sender's wallet lookup
     * @param request          the transfer request body
     */
    @Transactional
    public TransactionResponse transfer(
            Long senderUserId,
            String senderEmail,
            String senderWalletNumber,
            TransferRequest request) {

        log.info("Transfer initiated: sender={} amount={} recipient={}",
                senderEmail, request.getAmount(), request.getRecipientEmail());

        // ── 1. Amount validation ─────────────────────────────────────────────
        validateAmount(request.getAmount());

        // ── 2. Self-transfer check ───────────────────────────────────────────
        if (senderEmail.equalsIgnoreCase(request.getRecipientEmail().trim())) {
            throw new SelfTransferException("You cannot transfer money to yourself");
        }

        // ── 3. Resolve receiver wallet by email ──────────────────────────────
        WalletInfo receiverWallet = walletClient.getWalletByEmail(
                request.getRecipientEmail().toLowerCase().trim());

        if ("SUSPENDED".equals(receiverWallet.getStatus()) ||
                "CLOSED".equals(receiverWallet.getStatus())) {
            throw new WalletServiceException(
                    "Recipient's wallet is not active and cannot receive transfers.",
                    422);
        }

        // ── 4. Resolve sender wallet ─────────────────────────────────────────
        WalletInfo senderWallet = walletClient.getWalletByNumber(senderWalletNumber);

        if ("SUSPENDED".equals(senderWallet.getStatus()) ||
                "CLOSED".equals(senderWallet.getStatus())) {
            throw new WalletServiceException(
                    "Your wallet is not active. Please contact support.", 422);
        }

        // ── 5. Additional self-transfer check via wallet number ──────────────
        if (senderWalletNumber.equals(receiverWallet.getWalletNumber())) {
            throw new SelfTransferException("You cannot transfer money to yourself");
        }

        // ── 6. Daily limit check ─────────────────────────────────────────────
        validateDailyLimit(senderUserId, request.getAmount());

        // ── 7. Create PENDING transaction record ─────────────────────────────
        String referenceCode = generateReferenceCode();
        Transaction txn = Transaction.builder()
                .referenceCode(referenceCode)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING)
                .senderWalletNumber(senderWalletNumber)
                .senderUserId(senderUserId)
                .senderEmail(senderEmail)
                .receiverWalletNumber(receiverWallet.getWalletNumber())
                .receiverUserId(receiverWallet.getUserId())
                .receiverEmail(receiverWallet.getEmail())
                .amount(request.getAmount())
                .currency(senderWallet.getCurrency())
                .description(request.getDescription())
                .build();

        txn = transactionRepository.save(txn);
        log.info("Transaction created PENDING: ref={}", referenceCode);

        // Publish initiated event (notification service can send "transfer pending" email)
        eventPublisher.publishInitiated(txn);

        // ── 8. Execute saga ──────────────────────────────────────────────────
        return executeSaga(txn, senderWalletNumber, receiverWallet.getWalletNumber(),
                request.getAmount(), referenceCode);
    }

    // ================================================================
    // GET TRANSACTION BY REFERENCE CODE
    // ================================================================

    @Transactional(readOnly = true)
    public TransactionResponse getByReferenceCode(String referenceCode) {
        Transaction txn = transactionRepository.findByReferenceCode(referenceCode)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found: " + referenceCode));
        return TransactionResponse.from(txn);
    }

    // ================================================================
    // TRANSACTION HISTORY — all (sent and received)
    // ================================================================

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getHistory(Long userId, Pageable pageable) {
        Page<Transaction> page = transactionRepository.findAllByUserId(userId, pageable);
        return PagedResponse.from(page, TransactionResponse::from);
    }

    // ================================================================
    // SENT TRANSACTIONS
    // ================================================================

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getSent(Long userId, Pageable pageable) {
        Page<Transaction> page = transactionRepository
                .findBySenderUserIdOrderByCreatedAtDesc(userId, pageable);
        return PagedResponse.from(page, TransactionResponse::from);
    }

    // ================================================================
    // RECEIVED TRANSACTIONS
    // ================================================================

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getReceived(Long userId, Pageable pageable) {
        Page<Transaction> page = transactionRepository
                .findByReceiverUserIdOrderByCreatedAtDesc(userId, pageable);
        return PagedResponse.from(page, TransactionResponse::from);
    }

    // ================================================================
    // ADMIN: Reverse a completed transaction
    // ================================================================

    @Transactional
    public TransactionResponse reverseTransaction(String referenceCode, Long adminUserId) {
        Transaction txn = transactionRepository.findByReferenceCode(referenceCode)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found: " + referenceCode));

        if (txn.getStatus() != TransactionStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Only COMPLETED transactions can be reversed. Status: " + txn.getStatus());
        }

        log.info("Reversing transaction ref={} by admin={}", referenceCode, adminUserId);

        // Debit the receiver (take back the money)
        walletClient.debitWallet(
                txn.getReceiverWalletNumber(), txn.getAmount(),
                referenceCode + "-REV-DR");

        // Credit the sender (return their money)
        walletClient.creditWallet(
                txn.getSenderWalletNumber(), txn.getAmount(),
                referenceCode + "-REV-CR");

        txn.setStatus(TransactionStatus.REVERSED);
        txn.setCompletedAt(LocalDateTime.now());
        Transaction saved = transactionRepository.save(txn);

        log.info("Transaction reversed: ref={}", referenceCode);
        return TransactionResponse.from(saved);
    }

    // ================================================================
    // Private — saga execution
    // ================================================================

    /**
     * Executes the two-phase commit:
     *   Phase 1: Debit sender
     *   Phase 2: Credit receiver
     *   Compensation: If phase 2 fails, re-credit the sender
     */
    private TransactionResponse executeSaga(
            Transaction txn,
            String senderWalletNumber,
            String receiverWalletNumber,
            BigDecimal amount,
            String referenceCode) {

        // Phase 1: Debit sender
        try {
            walletClient.debitWallet(senderWalletNumber, amount, referenceCode);
        } catch (WalletServiceException e) {
            log.error("Debit failed ref={}: {}", referenceCode, e.getMessage());
            txn = markFailed(txn, "Debit failed: " + e.getMessage());
            eventPublisher.publishFailed(txn);
            throw e; // Re-throw so controller returns appropriate HTTP status
        }

        // Phase 2: Credit receiver
        try {
            walletClient.creditWallet(receiverWalletNumber, amount, referenceCode);
        } catch (WalletServiceException e) {
            log.error("Credit failed ref={}: {} — attempting reversal", referenceCode, e.getMessage());

            // Compensating transaction: return money to sender
            walletClient.reversalCredit(senderWalletNumber, amount, referenceCode);

            txn = markFailed(txn, "Credit failed: " + e.getMessage()
                    + ". Sender's funds have been returned.");
            eventPublisher.publishFailed(txn);
            throw new WalletServiceException(
                    "Transfer failed. Your funds have been returned to your wallet.", 422);
        }

        // Both phases succeeded — mark COMPLETED
        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setCompletedAt(LocalDateTime.now());
        Transaction saved = transactionRepository.save(txn);

        log.info("Transfer COMPLETED: ref={} amount={} {} → {}",
                referenceCode, amount, senderWalletNumber, receiverWalletNumber);

        eventPublisher.publishCompleted(saved);
        return TransactionResponse.from(saved);
    }

    // ================================================================
    // Private — validations
    // ================================================================

    private void validateAmount(BigDecimal amount) {
        if (amount.compareTo(minTransferAmount) < 0) {
            throw new LimitExceededException(
                    "Minimum transfer amount is " + minTransferAmount);
        }
        if (amount.compareTo(maxTransferAmount) > 0) {
            throw new LimitExceededException(
                    "Maximum transfer amount per transaction is " + maxTransferAmount);
        }
    }

    private void validateDailyLimit(Long userId, BigDecimal newAmount) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = startOfDay.plusDays(1);

        BigDecimal alreadySentToday = transactionRepository
                .sumCompletedAmountByUserAndDateRange(
                        userId, TransactionStatus.COMPLETED, startOfDay, endOfDay);

        if (alreadySentToday == null) alreadySentToday = BigDecimal.ZERO;

        BigDecimal projectedTotal = alreadySentToday.add(newAmount);
        if (projectedTotal.compareTo(dailyTransferLimit) > 0) {
            throw new LimitExceededException(
                    "Daily transfer limit of " + dailyTransferLimit + " exceeded. " +
                    "Already transferred today: " + alreadySentToday);
        }
    }

    private Transaction markFailed(Transaction txn, String reason) {
        txn.setStatus(TransactionStatus.FAILED);
        txn.setFailureReason(reason);
        txn.setCompletedAt(LocalDateTime.now());
        return transactionRepository.save(txn);
    }

    // ================================================================
    // Private — reference code generation
    // ================================================================

    private String generateReferenceCode() {
        String date   = LocalDate.now().toString().replace("-", "");
        String suffix = UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase();
        return "TXN-" + date + "-" + suffix;
    }
}
