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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("WalletService Unit Tests")
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private WalletTransactionRepository transactionRepository;
    @Mock private WalletEventPublisher eventPublisher;

    @InjectMocks private WalletService walletService;

    private Wallet activeWallet;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(walletService, "maxDepositAmount",    new BigDecimal("10000000"));
        ReflectionTestUtils.setField(walletService, "maxWithdrawalAmount", new BigDecimal("5000000"));
        ReflectionTestUtils.setField(walletService, "dailyWithdrawalLimit",new BigDecimal("10000000"));
        ReflectionTestUtils.setField(walletService, "minimumBalance",      BigDecimal.ZERO);

        activeWallet = Wallet.builder()
                .id(1L).userId(42L)
                .walletNumber("WLT-20240101-ABCD1234")
                .ownerName("Jean Dupont")
                .email("jean@example.com")
                .phoneNumber("+237600000001")
                .balance(new BigDecimal("5000.00"))
                .currency(Currency.XAF)
                .status(WalletStatus.ACTIVE)
                .version(0L)
                .build();
    }

    // ================================================================
    // CREATE WALLET
    // ================================================================

    @Test
    @DisplayName("createWallet — success returns WalletResponse")
    void createWallet_success() {
        when(walletRepository.existsByUserId(42L)).thenReturn(false);
        when(walletRepository.existsByPhoneNumber("+237600000001")).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenReturn(activeWallet);

        CreateWalletRequest req = new CreateWalletRequest();
        req.setPhoneNumber("+237600000001");
        req.setCurrency(Currency.XAF);

        WalletResponse response = walletService.createWallet(
                42L, "Jean Dupont", "jean@example.com", req);

        assertThat(response.getUserId()).isEqualTo(42L);
        assertThat(response.getBalance()).isEqualByComparingTo("5000.00");
        verify(walletRepository).save(any(Wallet.class));
        verify(eventPublisher).publishWalletCreated(activeWallet);
    }

    @Test
    @DisplayName("createWallet — throws WalletAlreadyExistsException when user already has wallet")
    void createWallet_alreadyExists_throws() {
        when(walletRepository.existsByUserId(42L)).thenReturn(true);

        CreateWalletRequest req = new CreateWalletRequest();
        req.setPhoneNumber("+237600000001");
        req.setCurrency(Currency.XAF);

        assertThatThrownBy(() ->
                walletService.createWallet(42L, "Jean", "jean@example.com", req))
                .isInstanceOf(WalletAlreadyExistsException.class)
                .hasMessageContaining("wallet already exists");

        verify(walletRepository, never()).save(any());
    }

    @Test
    @DisplayName("createWallet — throws WalletAlreadyExistsException when phone already linked")
    void createWallet_phoneTaken_throws() {
        when(walletRepository.existsByUserId(42L)).thenReturn(false);
        when(walletRepository.existsByPhoneNumber("+237600000001")).thenReturn(true);

        CreateWalletRequest req = new CreateWalletRequest();
        req.setPhoneNumber("+237600000001");
        req.setCurrency(Currency.XAF);

        assertThatThrownBy(() ->
                walletService.createWallet(42L, "Jean", "jean@example.com", req))
                .isInstanceOf(WalletAlreadyExistsException.class);

        verify(walletRepository, never()).save(any());
    }

    // ================================================================
    // GET MY WALLET
    // ================================================================

    @Test
    @DisplayName("getMyWallet — returns wallet for valid userId")
    void getMyWallet_success() {
        when(walletRepository.findByUserId(42L)).thenReturn(Optional.of(activeWallet));

        WalletResponse response = walletService.getMyWallet(42L);

        assertThat(response.getWalletNumber()).isEqualTo("WLT-20240101-ABCD1234");
        assertThat(response.getBalance()).isEqualByComparingTo("5000.00");
    }

    @Test
    @DisplayName("getMyWallet — throws WalletNotFoundException when no wallet")
    void getMyWallet_notFound_throws() {
        when(walletRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getMyWallet(99L))
                .isInstanceOf(WalletNotFoundException.class);
    }

    // ================================================================
    // DEPOSIT
    // ================================================================

    @Test
    @DisplayName("deposit — success increases balance and saves transaction")
    void deposit_success() {
        when(walletRepository.findByUserIdWithLock(42L)).thenReturn(Optional.of(activeWallet));

        WalletTransaction savedTxn = WalletTransaction.builder()
                .id(1L).wallet(activeWallet)
                .type(TransactionType.DEPOSIT)
                .amount(new BigDecimal("1000.00"))
                .balanceBefore(new BigDecimal("5000.00"))
                .balanceAfter(new BigDecimal("6000.00"))
                .currency(Currency.XAF)
                .referenceCode("DEP-20240101-ABCD1234")
                .description("Test deposit").build();

        when(walletRepository.save(activeWallet)).thenReturn(activeWallet);
        when(transactionRepository.save(any(WalletTransaction.class))).thenReturn(savedTxn);

        DepositRequest req = new DepositRequest();
        req.setAmount(new BigDecimal("1000.00"));
        req.setDescription("Test deposit");

        WalletTransactionResponse response = walletService.deposit(42L, req);

        assertThat(response.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(response.getBalanceAfter()).isEqualByComparingTo("6000.00");
        assertThat(response.getType()).isEqualTo(TransactionType.DEPOSIT);

        // Verify balance was updated on the wallet object
        assertThat(activeWallet.getBalance()).isEqualByComparingTo("6000.00");
        verify(eventPublisher).publishWalletFunded(eq(activeWallet), any(WalletTransaction.class));
    }

    @Test
    @DisplayName("deposit — throws LimitExceededException when amount exceeds max")
    void deposit_exceedsLimit_throws() {
        DepositRequest req = new DepositRequest();
        req.setAmount(new BigDecimal("99999999.00"));
        req.setDescription("Big deposit");

        assertThatThrownBy(() -> walletService.deposit(42L, req))
                .isInstanceOf(LimitExceededException.class)
                .hasMessageContaining("maximum allowed");

        verify(walletRepository, never()).findByUserIdWithLock(any());
    }

    @Test
    @DisplayName("deposit — throws WalletNotFoundException when wallet missing")
    void deposit_walletNotFound_throws() {
        when(walletRepository.findByUserIdWithLock(42L)).thenReturn(Optional.empty());

        DepositRequest req = new DepositRequest();
        req.setAmount(new BigDecimal("500.00"));
        req.setDescription("Test");

        assertThatThrownBy(() -> walletService.deposit(42L, req))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    @DisplayName("deposit — throws WalletSuspendedException when wallet is suspended")
    void deposit_walletSuspended_throws() {
        activeWallet.setStatus(WalletStatus.SUSPENDED);
        when(walletRepository.findByUserIdWithLock(42L)).thenReturn(Optional.of(activeWallet));

        DepositRequest req = new DepositRequest();
        req.setAmount(new BigDecimal("500.00"));
        req.setDescription("Test");

        assertThatThrownBy(() -> walletService.deposit(42L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not active");
    }

    // ================================================================
    // WITHDRAW
    // ================================================================

    @Test
    @DisplayName("withdraw — success decreases balance and saves transaction")
    void withdraw_success() {
        when(walletRepository.findByUserIdWithLock(42L)).thenReturn(Optional.of(activeWallet));
        when(transactionRepository.sumAmountByWalletIdAndTypeAndCreatedAtBetween(
                anyLong(), any(), any(), any())).thenReturn(BigDecimal.ZERO);

        WalletTransaction savedTxn = WalletTransaction.builder()
                .id(2L).wallet(activeWallet)
                .type(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("1000.00"))
                .balanceBefore(new BigDecimal("5000.00"))
                .balanceAfter(new BigDecimal("4000.00"))
                .currency(Currency.XAF)
                .referenceCode("WDR-20240101-EFGH5678")
                .description("Test withdrawal").build();

        when(walletRepository.save(activeWallet)).thenReturn(activeWallet);
        when(transactionRepository.save(any(WalletTransaction.class))).thenReturn(savedTxn);

        WithdrawRequest req = new WithdrawRequest();
        req.setAmount(new BigDecimal("1000.00"));
        req.setDescription("Test withdrawal");

        WalletTransactionResponse response = walletService.withdraw(42L, req);

        assertThat(response.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(response.getBalanceAfter()).isEqualByComparingTo("4000.00");
        assertThat(response.getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(activeWallet.getBalance()).isEqualByComparingTo("4000.00");

        verify(eventPublisher).publishWalletWithdrawn(eq(activeWallet), any(WalletTransaction.class));
    }

    @Test
    @DisplayName("withdraw — throws InsufficientFundsException when balance is too low")
    void withdraw_insufficientFunds_throws() {
        activeWallet.setBalance(new BigDecimal("100.00"));
        when(walletRepository.findByUserIdWithLock(42L)).thenReturn(Optional.of(activeWallet));
        when(transactionRepository.sumAmountByWalletIdAndTypeAndCreatedAtBetween(
                anyLong(), any(), any(), any())).thenReturn(BigDecimal.ZERO);

        WithdrawRequest req = new WithdrawRequest();
        req.setAmount(new BigDecimal("500.00"));
        req.setDescription("Test");

        assertThatThrownBy(() -> walletService.withdraw(42L, req))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");

        verify(walletRepository, never()).save(any());
    }

    @Test
    @DisplayName("withdraw — throws LimitExceededException when single amount exceeds max")
    void withdraw_exceedsSingleLimit_throws() {
        WithdrawRequest req = new WithdrawRequest();
        req.setAmount(new BigDecimal("9999999.00"));
        req.setDescription("Too much");

        assertThatThrownBy(() -> walletService.withdraw(42L, req))
                .isInstanceOf(LimitExceededException.class)
                .hasMessageContaining("maximum allowed per transaction");

        verify(walletRepository, never()).findByUserIdWithLock(any());
    }

    @Test
    @DisplayName("withdraw — throws LimitExceededException when daily limit would be exceeded")
    void withdraw_exceedsDailyLimit_throws() {
        when(walletRepository.findByUserIdWithLock(42L)).thenReturn(Optional.of(activeWallet));
        // Already withdrawn 9,900,000 today — adding 500 would exceed 10,000,000 limit
        when(transactionRepository.sumAmountByWalletIdAndTypeAndCreatedAtBetween(
                anyLong(), any(), any(), any())).thenReturn(new BigDecimal("9999600.00"));

        WithdrawRequest req = new WithdrawRequest();
        req.setAmount(new BigDecimal("500.00"));
        req.setDescription("One more");

        assertThatThrownBy(() -> walletService.withdraw(42L, req))
                .isInstanceOf(LimitExceededException.class)
                .hasMessageContaining("Daily withdrawal limit");
    }

    // ================================================================
    // CREDIT / DEBIT (internal transfer operations)
    // ================================================================

    @Test
    @DisplayName("creditWallet — adds amount and saves transaction")
    void creditWallet_success() {
        when(walletRepository.findByWalletNumber("WLT-20240101-ABCD1234"))
                .thenReturn(Optional.of(activeWallet));
        when(walletRepository.save(activeWallet)).thenReturn(activeWallet);
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        WalletResponse response = walletService.creditWallet(
                "WLT-20240101-ABCD1234", new BigDecimal("2000.00"), "TXN-REF-001");

        assertThat(activeWallet.getBalance()).isEqualByComparingTo("7000.00");
    }

    @Test
    @DisplayName("debitWallet — subtracts amount and saves transaction")
    void debitWallet_success() {
        when(walletRepository.findByWalletNumber("WLT-20240101-ABCD1234"))
                .thenReturn(Optional.of(activeWallet));
        when(walletRepository.save(activeWallet)).thenReturn(activeWallet);
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        WalletResponse response = walletService.debitWallet(
                "WLT-20240101-ABCD1234", new BigDecimal("1000.00"), "TXN-REF-001");

        assertThat(activeWallet.getBalance()).isEqualByComparingTo("4000.00");
    }

    @Test
    @DisplayName("debitWallet — throws InsufficientFundsException when balance insufficient")
    void debitWallet_insufficientFunds_throws() {
        activeWallet.setBalance(new BigDecimal("50.00"));
        when(walletRepository.findByWalletNumber("WLT-20240101-ABCD1234"))
                .thenReturn(Optional.of(activeWallet));

        assertThatThrownBy(() -> walletService.debitWallet(
                "WLT-20240101-ABCD1234", new BigDecimal("1000.00"), "TXN-REF-001"))
                .isInstanceOf(InsufficientFundsException.class);

        verify(walletRepository, never()).save(any());
    }

    // ================================================================
    // ADMIN operations
    // ================================================================

    @Test
    @DisplayName("suspendWallet — sets status to SUSPENDED")
    void suspendWallet_success() {
        when(walletRepository.findByWalletNumber("WLT-20240101-ABCD1234"))
                .thenReturn(Optional.of(activeWallet));
        when(walletRepository.save(activeWallet)).thenReturn(activeWallet);

        WalletResponse response = walletService.suspendWallet("WLT-20240101-ABCD1234");

        assertThat(activeWallet.getStatus()).isEqualTo(WalletStatus.SUSPENDED);
    }

    @Test
    @DisplayName("reactivateWallet — sets status to ACTIVE")
    void reactivateWallet_success() {
        activeWallet.setStatus(WalletStatus.SUSPENDED);
        when(walletRepository.findByWalletNumber("WLT-20240101-ABCD1234"))
                .thenReturn(Optional.of(activeWallet));
        when(walletRepository.save(activeWallet)).thenReturn(activeWallet);

        walletService.reactivateWallet("WLT-20240101-ABCD1234");

        assertThat(activeWallet.getStatus()).isEqualTo(WalletStatus.ACTIVE);
    }
}
