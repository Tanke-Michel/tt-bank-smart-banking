package com.example.transaction_service.service;

import com.example.transaction_service.dto.*;
import com.example.transaction_service.entity.Transaction;
import com.example.transaction_service.enums.TransactionStatus;
import com.example.transaction_service.enums.TransactionType;
import com.example.transaction_service.exception.*;
import com.example.transaction_service.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("TransactionService Unit Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletServiceClient walletClient;
    @Mock private TransactionEventPublisher eventPublisher;

    @InjectMocks private TransactionService transactionService;

    private WalletInfo senderWallet;
    private WalletInfo receiverWallet;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(transactionService, "maxTransferAmount", new BigDecimal("5000000"));
        ReflectionTestUtils.setField(transactionService, "minTransferAmount", new BigDecimal("1"));
        ReflectionTestUtils.setField(transactionService, "dailyTransferLimit", new BigDecimal("10000000"));

        senderWallet = new WalletInfo();
        senderWallet.setId(1L);
        senderWallet.setUserId(10L);
        senderWallet.setWalletNumber("WLT-20240101-SENDER01");
        senderWallet.setEmail("sender@example.com");
        senderWallet.setBalance(new BigDecimal("5000.00"));
        senderWallet.setCurrency("XAF");
        senderWallet.setStatus("ACTIVE");

        receiverWallet = new WalletInfo();
        receiverWallet.setId(2L);
        receiverWallet.setUserId(20L);
        receiverWallet.setWalletNumber("WLT-20240101-RECV0001");
        receiverWallet.setEmail("receiver@example.com");
        receiverWallet.setBalance(new BigDecimal("1000.00"));
        receiverWallet.setCurrency("XAF");
        receiverWallet.setStatus("ACTIVE");

        transferRequest = new TransferRequest();
        transferRequest.setRecipientEmail("receiver@example.com");
        transferRequest.setAmount(new BigDecimal("500.00"));
        transferRequest.setDescription("Test transfer");
    }

    // ================================================================
    // TRANSFER — success path
    // ================================================================

    @Test
    @DisplayName("transfer — success creates COMPLETED transaction")
    void transfer_success_completedTransaction() {
        // Repository behaviour: save returns a persisted entity with ID
        when(walletClient.getWalletByEmail("receiver@example.com")).thenReturn(receiverWallet);
        when(walletClient.getWalletByNumber("WLT-20240101-SENDER01")).thenReturn(senderWallet);
        when(transactionRepository.sumCompletedAmountByUserAndDateRange(
                anyLong(), any(), any(), any())).thenReturn(BigDecimal.ZERO);

        // Save returns the entity passed to it (with ID set by the PENDING save)
        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        when(transactionRepository.save(txnCaptor.capture())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getId() == null) t = Transaction.builder()
                    .id(1L)
                    .referenceCode(t.getReferenceCode())
                    .type(t.getType())
                    .status(t.getStatus())
                    .senderWalletNumber(t.getSenderWalletNumber())
                    .senderUserId(t.getSenderUserId())
                    .senderEmail(t.getSenderEmail())
                    .receiverWalletNumber(t.getReceiverWalletNumber())
                    .receiverUserId(t.getReceiverUserId())
                    .receiverEmail(t.getReceiverEmail())
                    .amount(t.getAmount())
                    .currency(t.getCurrency())
                    .description(t.getDescription())
                    .build();
            return t;
        });

        TransactionResponse response = transactionService.transfer(
                10L, "sender@example.com", "WLT-20240101-SENDER01", transferRequest);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.getSenderEmail()).isEqualTo("sender@example.com");
        assertThat(response.getReceiverEmail()).isEqualTo("receiver@example.com");
        assertThat(response.getAmount()).isEqualByComparingTo("500.00");

        verify(walletClient).debitWallet(eq("WLT-20240101-SENDER01"),
                eq(new BigDecimal("500.00")), anyString());
        verify(walletClient).creditWallet(eq("WLT-20240101-RECV0001"),
                eq(new BigDecimal("500.00")), anyString());
        verify(eventPublisher).publishInitiated(any());
        verify(eventPublisher).publishCompleted(any());
        verify(eventPublisher, never()).publishFailed(any());
    }

    // ================================================================
    // TRANSFER — self-transfer
    // ================================================================

    @Test
    @DisplayName("transfer — throws SelfTransferException when recipient is sender (by email)")
    void transfer_selfTransfer_byEmail_throws() {
        transferRequest.setRecipientEmail("sender@example.com");

        assertThatThrownBy(() -> transactionService.transfer(
                10L, "sender@example.com", "WLT-20240101-SENDER01", transferRequest))
                .isInstanceOf(SelfTransferException.class)
                .hasMessageContaining("yourself");

        verify(walletClient, never()).debitWallet(any(), any(), any());
        verify(transactionRepository, never()).save(any());
    }

    // ================================================================
    // TRANSFER — amount validations
    // ================================================================

    @Test
    @DisplayName("transfer — throws LimitExceededException when amount exceeds max")
    void transfer_amountExceedsMax_throws() {
        transferRequest.setAmount(new BigDecimal("9999999.00"));

        assertThatThrownBy(() -> transactionService.transfer(
                10L, "sender@example.com", "WLT-20240101-SENDER01", transferRequest))
                .isInstanceOf(LimitExceededException.class)
                .hasMessageContaining("Maximum");

        verify(walletClient, never()).getWalletByEmail(any());
    }

    @Test
    @DisplayName("transfer — throws LimitExceededException when amount is below minimum")
    void transfer_amountBelowMin_throws() {
        transferRequest.setAmount(new BigDecimal("0.50"));

        assertThatThrownBy(() -> transactionService.transfer(
                10L, "sender@example.com", "WLT-20240101-SENDER01", transferRequest))
                .isInstanceOf(LimitExceededException.class)
                .hasMessageContaining("Minimum");
    }

    @Test
    @DisplayName("transfer — throws LimitExceededException when daily limit would be exceeded")
    void transfer_dailyLimitExceeded_throws() {
        when(walletClient.getWalletByEmail("receiver@example.com")).thenReturn(receiverWallet);
        when(walletClient.getWalletByNumber("WLT-20240101-SENDER01")).thenReturn(senderWallet);
        // Already sent 9,999,600 today — this 500 would exceed 10,000,000
        when(transactionRepository.sumCompletedAmountByUserAndDateRange(
                anyLong(), any(), any(), any()))
                .thenReturn(new BigDecimal("9999600.00"));

        assertThatThrownBy(() -> transactionService.transfer(
                10L, "sender@example.com", "WLT-20240101-SENDER01", transferRequest))
                .isInstanceOf(LimitExceededException.class)
                .hasMessageContaining("Daily");

        verify(walletClient, never()).debitWallet(any(), any(), any());
    }

    // ================================================================
    // TRANSFER — wallet status checks
    // ================================================================

    @Test
    @DisplayName("transfer — throws WalletServiceException when receiver wallet is suspended")
    void transfer_receiverWalletSuspended_throws() {
        receiverWallet.setStatus("SUSPENDED");
        when(walletClient.getWalletByEmail("receiver@example.com")).thenReturn(receiverWallet);

        assertThatThrownBy(() -> transactionService.transfer(
                10L, "sender@example.com", "WLT-20240101-SENDER01", transferRequest))
                .isInstanceOf(WalletServiceException.class)
                .hasMessageContaining("not active");

        verify(walletClient, never()).debitWallet(any(), any(), any());
    }

    @Test
    @DisplayName("transfer — throws WalletServiceException when sender wallet is suspended")
    void transfer_senderWalletSuspended_throws() {
        senderWallet.setStatus("SUSPENDED");
        when(walletClient.getWalletByEmail("receiver@example.com")).thenReturn(receiverWallet);
        when(walletClient.getWalletByNumber("WLT-20240101-SENDER01")).thenReturn(senderWallet);
        when(transactionRepository.sumCompletedAmountByUserAndDateRange(
                anyLong(), any(), any(), any())).thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> transactionService.transfer(
                10L, "sender@example.com", "WLT-20240101-SENDER01", transferRequest))
                .isInstanceOf(WalletServiceException.class)
                .hasMessageContaining("not active");

        verify(walletClient, never()).debitWallet(any(), any(), any());
    }

    // ================================================================
    // TRANSFER — saga failure: debit fails
    // ================================================================

    @Test
    @DisplayName("transfer — marks FAILED and does NOT credit when debit fails")
    void transfer_debitFails_marksFailedNoCredit() {
        when(walletClient.getWalletByEmail("receiver@example.com")).thenReturn(receiverWallet);
        when(walletClient.getWalletByNumber("WLT-20240101-SENDER01")).thenReturn(senderWallet);
        when(transactionRepository.sumCompletedAmountByUserAndDateRange(
                anyLong(), any(), any(), any())).thenReturn(BigDecimal.ZERO);

        // Simulate debit failure (insufficient funds in wallet service)
        doThrow(new WalletServiceException("Insufficient funds", 422))
                .when(walletClient).debitWallet(any(), any(), any());

        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getId() == null) {
                t = Transaction.builder().id(1L)
                        .referenceCode("TXN-TEST").type(TransactionType.TRANSFER)
                        .status(t.getStatus()).senderUserId(10L).receiverUserId(20L)
                        .senderEmail("sender@example.com").receiverEmail("receiver@example.com")
                        .senderWalletNumber("WLT-S").receiverWalletNumber("WLT-R")
                        .amount(new BigDecimal("500")).currency("XAF").build();
            }
            return t;
        });

        assertThatThrownBy(() -> transactionService.transfer(
                10L, "sender@example.com", "WLT-20240101-SENDER01", transferRequest))
                .isInstanceOf(WalletServiceException.class);

        verify(walletClient, never()).creditWallet(any(), any(), any());
        verify(walletClient, never()).reversalCredit(any(), any(), any());
        verify(eventPublisher).publishFailed(any());
    }

    // ================================================================
    // TRANSFER — saga failure: credit fails after debit succeeds
    // ================================================================

    @Test
    @DisplayName("transfer — runs reversal credit and marks FAILED when credit fails")
    void transfer_creditFails_reversalExecuted() {
        when(walletClient.getWalletByEmail("receiver@example.com")).thenReturn(receiverWallet);
        when(walletClient.getWalletByNumber("WLT-20240101-SENDER01")).thenReturn(senderWallet);
        when(transactionRepository.sumCompletedAmountByUserAndDateRange(
                anyLong(), any(), any(), any())).thenReturn(BigDecimal.ZERO);

        // Debit succeeds, credit fails
        doNothing().when(walletClient).debitWallet(any(), any(), any());
        doThrow(new WalletServiceException("Receiver wallet error", 422))
                .when(walletClient).creditWallet(any(), any(), any());

        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getId() == null) {
                t = Transaction.builder().id(1L)
                        .referenceCode("TXN-TEST").type(TransactionType.TRANSFER)
                        .status(t.getStatus()).senderUserId(10L).receiverUserId(20L)
                        .senderEmail("sender@example.com").receiverEmail("receiver@example.com")
                        .senderWalletNumber("WLT-S").receiverWalletNumber("WLT-R")
                        .amount(new BigDecimal("500")).currency("XAF").build();
            }
            return t;
        });

        assertThatThrownBy(() -> transactionService.transfer(
                10L, "sender@example.com", "WLT-20240101-SENDER01", transferRequest))
                .isInstanceOf(WalletServiceException.class)
                .hasMessageContaining("Your funds have been returned");

        // Reversal credit must have been attempted
        verify(walletClient).reversalCredit(eq("WLT-20240101-SENDER01"),
                eq(new BigDecimal("500.00")), anyString());
        verify(eventPublisher).publishFailed(any());
    }

    // ================================================================
    // GET BY REFERENCE CODE
    // ================================================================

    @Test
    @DisplayName("getByReferenceCode — returns transaction for known reference")
    void getByReferenceCode_found_returnsResponse() {
        Transaction txn = buildCompletedTxn();
        when(transactionRepository.findByReferenceCode("TXN-20240101-ABCD1234"))
                .thenReturn(Optional.of(txn));

        TransactionResponse response = transactionService
                .getByReferenceCode("TXN-20240101-ABCD1234");

        assertThat(response.getReferenceCode()).isEqualTo("TXN-20240101-ABCD1234");
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    @DisplayName("getByReferenceCode — throws TransactionNotFoundException for unknown reference")
    void getByReferenceCode_notFound_throws() {
        when(transactionRepository.findByReferenceCode("UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getByReferenceCode("UNKNOWN"))
                .isInstanceOf(TransactionNotFoundException.class);
    }

    // ================================================================
    // HISTORY
    // ================================================================

    @Test
    @DisplayName("getHistory — returns paginated transactions for the user")
    void getHistory_returnsPage() {
        Transaction txn = buildCompletedTxn();
        when(transactionRepository.findAllByUserId(eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of(txn)));

        PagedResponse<TransactionResponse> page = transactionService
                .getHistory(10L, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getReferenceCode())
                .isEqualTo("TXN-20240101-ABCD1234");
    }

    // ================================================================
    // REVERSE
    // ================================================================

    @Test
    @DisplayName("reverseTransaction — marks REVERSED and calls wallet credit/debit")
    void reverseTransaction_success() {
        Transaction txn = buildCompletedTxn();
        when(transactionRepository.findByReferenceCode("TXN-20240101-ABCD1234"))
                .thenReturn(Optional.of(txn));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = transactionService
                .reverseTransaction("TXN-20240101-ABCD1234", 99L);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.REVERSED);
        verify(walletClient).debitWallet(eq("WLT-20240101-RECV0001"),
                eq(new BigDecimal("500.00")), contains("REV-DR"));
        verify(walletClient).creditWallet(eq("WLT-20240101-SENDER01"),
                eq(new BigDecimal("500.00")), contains("REV-CR"));
    }

    @Test
    @DisplayName("reverseTransaction — throws IllegalStateException for non-COMPLETED transaction")
    void reverseTransaction_notCompleted_throws() {
        Transaction txn = buildCompletedTxn();
        txn.setStatus(TransactionStatus.FAILED);
        when(transactionRepository.findByReferenceCode("TXN-20240101-ABCD1234"))
                .thenReturn(Optional.of(txn));

        assertThatThrownBy(() -> transactionService
                .reverseTransaction("TXN-20240101-ABCD1234", 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED");

        verify(walletClient, never()).debitWallet(any(), any(), any());
    }

    // ================================================================
    // Helper
    // ================================================================

    private Transaction buildCompletedTxn() {
        return Transaction.builder()
                .id(1L)
                .referenceCode("TXN-20240101-ABCD1234")
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .senderWalletNumber("WLT-20240101-SENDER01")
                .senderUserId(10L)
                .senderEmail("sender@example.com")
                .receiverWalletNumber("WLT-20240101-RECV0001")
                .receiverUserId(20L)
                .receiverEmail("receiver@example.com")
                .amount(new BigDecimal("500.00"))
                .currency("XAF")
                .description("Test")
                .completedAt(LocalDateTime.now())
                .build();
    }
}
