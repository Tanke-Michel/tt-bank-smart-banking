package com.example.merchant_service.service;

import com.example.merchant_service.dto.*;
import com.example.merchant_service.entity.Merchant;
import com.example.merchant_service.entity.MerchantPayment;
import com.example.merchant_service.enums.BusinessCategory;
import com.example.merchant_service.enums.MerchantStatus;
import com.example.merchant_service.enums.PaymentStatus;
import com.example.merchant_service.exception.*;
import com.example.merchant_service.repository.MerchantPaymentRepository;
import com.example.merchant_service.repository.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

@DisplayName("MerchantService Unit Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {

    @Mock private MerchantRepository merchantRepository;
    @Mock private MerchantPaymentRepository paymentRepository;
    @Mock private QrCodeService qrCodeService;
    @Mock private WalletServiceClient walletClient;
    @Mock private MerchantEventPublisher eventPublisher;

    @InjectMocks private MerchantService merchantService;

    private Merchant activeMerchant;
    private WalletInfo customerWallet;
    private WalletInfo merchantWallet;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(merchantService, "maxPaymentAmount", new BigDecimal("50000000"));
        ReflectionTestUtils.setField(merchantService, "minPaymentAmount", new BigDecimal("1"));

        activeMerchant = Merchant.builder()
                .id(1L)
                .merchantCode("MCH-20240101-ABCD1234")
                .ownerUserId(10L)
                .ownerEmail("merchant@example.com")
                .businessName("Jean's Shop")
                .businessEmail("shop@example.com")
                .businessPhone("+237600000010")
                .businessAddress("Yaoundé, Cameroon")
                .businessCategory(BusinessCategory.RETAIL)
                .walletNumber("WLT-MERCHANT-001")
                .qrCodeBase64("base64qr")
                .status(MerchantStatus.ACTIVE)
                .build();

        customerWallet = new WalletInfo();
        customerWallet.setId(2L);
        customerWallet.setUserId(20L);
        customerWallet.setWalletNumber("WLT-CUSTOMER-001");
        customerWallet.setEmail("customer@example.com");
        customerWallet.setCurrency("XAF");
        customerWallet.setStatus("ACTIVE");
        customerWallet.setBalance(new BigDecimal("10000.00"));

        merchantWallet = new WalletInfo();
        merchantWallet.setId(1L);
        merchantWallet.setUserId(10L);
        merchantWallet.setWalletNumber("WLT-MERCHANT-001");
        merchantWallet.setStatus("ACTIVE");
    }

    // ================================================================
    // REGISTER
    // ================================================================

    @Test
    @DisplayName("register — success creates PENDING merchant with QR code")
    void register_success_createsPending() {
        when(merchantRepository.existsByOwnerUserId(10L)).thenReturn(false);
        when(merchantRepository.existsByBusinessEmail("shop@example.com")).thenReturn(false);
        when(walletClient.getWalletByNumber("WLT-MERCHANT-001")).thenReturn(merchantWallet);
        when(qrCodeService.generateQrCodeBase64(anyString())).thenReturn("base64qr");
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(inv -> {
            Merchant m = inv.getArgument(0);
            m = Merchant.builder()
                    .id(1L).merchantCode(m.getMerchantCode())
                    .ownerUserId(m.getOwnerUserId()).ownerEmail(m.getOwnerEmail())
                    .businessName(m.getBusinessName()).businessEmail(m.getBusinessEmail())
                    .businessPhone(m.getBusinessPhone()).businessAddress(m.getBusinessAddress())
                    .businessCategory(m.getBusinessCategory()).walletNumber(m.getWalletNumber())
                    .qrCodeBase64(m.getQrCodeBase64()).status(MerchantStatus.PENDING).build();
            return m;
        });

        RegisterMerchantRequest req = new RegisterMerchantRequest();
        req.setBusinessName("Jean's Shop");
        req.setBusinessEmail("shop@example.com");
        req.setBusinessPhone("+237600000010");
        req.setBusinessAddress("Yaoundé, Cameroon");
        req.setBusinessCategory(BusinessCategory.RETAIL);
        req.setWalletNumber("WLT-MERCHANT-001");

        MerchantResponse response = merchantService.register(10L, "merchant@example.com", req);

        assertThat(response.getStatus()).isEqualTo(MerchantStatus.PENDING);
        assertThat(response.getQrCodeBase64()).isEqualTo("base64qr");
        assertThat(response.getMerchantCode()).startsWith("MCH-");

        verify(qrCodeService).generateQrCodeBase64(anyString());
        verify(eventPublisher).publishMerchantRegistered(any());
    }

    @Test
    @DisplayName("register — throws MerchantAlreadyExistsException if user already has merchant")
    void register_alreadyExists_throws() {
        when(merchantRepository.existsByOwnerUserId(10L)).thenReturn(true);

        RegisterMerchantRequest req = new RegisterMerchantRequest();
        req.setBusinessName("Shop"); req.setBusinessEmail("s@e.com");
        req.setBusinessPhone("+237600000010"); req.setBusinessAddress("Yaoundé");
        req.setBusinessCategory(BusinessCategory.RETAIL); req.setWalletNumber("WLT-001");

        assertThatThrownBy(() -> merchantService.register(10L, "m@e.com", req))
                .isInstanceOf(MerchantAlreadyExistsException.class);
        verify(merchantRepository, never()).save(any());
    }

    @Test
    @DisplayName("register — throws MerchantAlreadyExistsException if business email taken")
    void register_emailTaken_throws() {
        when(merchantRepository.existsByOwnerUserId(10L)).thenReturn(false);
        when(merchantRepository.existsByBusinessEmail("shop@example.com")).thenReturn(true);

        RegisterMerchantRequest req = new RegisterMerchantRequest();
        req.setBusinessName("Shop"); req.setBusinessEmail("shop@example.com");
        req.setBusinessPhone("+237600000010"); req.setBusinessAddress("Yaoundé");
        req.setBusinessCategory(BusinessCategory.RETAIL); req.setWalletNumber("WLT-001");

        assertThatThrownBy(() -> merchantService.register(10L, "m@e.com", req))
                .isInstanceOf(MerchantAlreadyExistsException.class)
                .hasMessageContaining("shop@example.com");
    }

    @Test
    @DisplayName("register — throws IllegalStateException if wallet belongs to different user")
    void register_walletNotOwned_throws() {
        when(merchantRepository.existsByOwnerUserId(10L)).thenReturn(false);
        when(merchantRepository.existsByBusinessEmail("shop@example.com")).thenReturn(false);
        WalletInfo otherWallet = new WalletInfo();
        otherWallet.setUserId(99L); // belongs to someone else
        otherWallet.setStatus("ACTIVE");
        when(walletClient.getWalletByNumber("WLT-001")).thenReturn(otherWallet);

        RegisterMerchantRequest req = new RegisterMerchantRequest();
        req.setBusinessName("Shop"); req.setBusinessEmail("shop@example.com");
        req.setBusinessPhone("+237600000010"); req.setBusinessAddress("Yaoundé");
        req.setBusinessCategory(BusinessCategory.RETAIL); req.setWalletNumber("WLT-001");

        assertThatThrownBy(() -> merchantService.register(10L, "m@e.com", req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not belong to your account");
    }

    // ================================================================
    // GET MY MERCHANT
    // ================================================================

    @Test
    @DisplayName("getMyMerchant — returns merchant for valid owner")
    void getMyMerchant_found_returnsResponse() {
        when(merchantRepository.findByOwnerUserId(10L)).thenReturn(Optional.of(activeMerchant));
        MerchantResponse response = merchantService.getMyMerchant(10L);
        assertThat(response.getMerchantCode()).isEqualTo("MCH-20240101-ABCD1234");
        assertThat(response.getStatus()).isEqualTo(MerchantStatus.ACTIVE);
    }

    @Test
    @DisplayName("getMyMerchant — throws MerchantNotFoundException when no merchant")
    void getMyMerchant_notFound_throws() {
        when(merchantRepository.findByOwnerUserId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> merchantService.getMyMerchant(99L))
                .isInstanceOf(MerchantNotFoundException.class);
    }

    // ================================================================
    // PAY (QR payment)
    // ================================================================

    @Test
    @DisplayName("pay — success debits customer and credits merchant")
    void pay_success_completesPayment() {
        when(merchantRepository.findByMerchantCode("MCH-20240101-ABCD1234"))
                .thenReturn(Optional.of(activeMerchant));
        when(walletClient.getWalletByNumber("WLT-CUSTOMER-001")).thenReturn(customerWallet);

        MerchantPayment savedPayment = MerchantPayment.builder()
                .id(1L).referenceCode("PAY-20240101-TEST0001")
                .merchant(activeMerchant).customerUserId(20L)
                .customerEmail("customer@example.com")
                .customerWalletNumber("WLT-CUSTOMER-001")
                .merchantWalletNumber("WLT-MERCHANT-001")
                .amount(new BigDecimal("500.00")).currency("XAF")
                .description("Payment to Jean's Shop")
                .status(PaymentStatus.PENDING).build();

        when(paymentRepository.save(any())).thenAnswer(inv -> {
            MerchantPayment p = inv.getArgument(0);
            if (p.getId() == null) p = savedPayment;
            p.setStatus(PaymentStatus.COMPLETED);
            p.setCompletedAt(LocalDateTime.now());
            return p;
        });

        MerchantPaymentRequest req = new MerchantPaymentRequest();
        req.setMerchantCode("MCH-20240101-ABCD1234");
        req.setCustomerWalletNumber("WLT-CUSTOMER-001");
        req.setAmount(new BigDecimal("500.00"));

        MerchantPaymentResponse response = merchantService.pay(
                20L, "customer@example.com", "WLT-CUSTOMER-001", req);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(walletClient).debitWallet(eq("WLT-CUSTOMER-001"),
                eq(new BigDecimal("500.00")), anyString());
        verify(walletClient).creditWallet(eq("WLT-MERCHANT-001"),
                eq(new BigDecimal("500.00")), anyString());
        verify(eventPublisher).publishPaymentCompleted(any());
    }

    @Test
    @DisplayName("pay — throws MerchantNotActiveException when merchant is PENDING")
    void pay_merchantPending_throws() {
        activeMerchant.setStatus(MerchantStatus.PENDING);
        when(merchantRepository.findByMerchantCode("MCH-20240101-ABCD1234"))
                .thenReturn(Optional.of(activeMerchant));

        MerchantPaymentRequest req = new MerchantPaymentRequest();
        req.setMerchantCode("MCH-20240101-ABCD1234");
        req.setCustomerWalletNumber("WLT-CUSTOMER-001");
        req.setAmount(new BigDecimal("500.00"));

        assertThatThrownBy(() -> merchantService.pay(
                20L, "customer@example.com", "WLT-CUSTOMER-001", req))
                .isInstanceOf(MerchantNotActiveException.class);
        verify(walletClient, never()).debitWallet(any(), any(), any());
    }

    @Test
    @DisplayName("pay — throws IllegalStateException when customer tries to pay own merchant")
    void pay_selfPayment_throws() {
        when(merchantRepository.findByMerchantCode("MCH-20240101-ABCD1234"))
                .thenReturn(Optional.of(activeMerchant));
        // customerUserId = ownerUserId = 10L
        when(walletClient.getWalletByNumber("WLT-MERCHANT-001")).thenReturn(merchantWallet);

        MerchantPaymentRequest req = new MerchantPaymentRequest();
        req.setMerchantCode("MCH-20240101-ABCD1234");
        req.setCustomerWalletNumber("WLT-MERCHANT-001");
        req.setAmount(new BigDecimal("500.00"));

        assertThatThrownBy(() -> merchantService.pay(
                10L, "merchant@example.com", "WLT-MERCHANT-001", req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot pay your own");
        verify(walletClient, never()).debitWallet(any(), any(), any());
    }

    @Test
    @DisplayName("pay — runs reversal and marks FAILED when credit fails after debit")
    void pay_creditFails_reversalExecuted() {
        when(merchantRepository.findByMerchantCode("MCH-20240101-ABCD1234"))
                .thenReturn(Optional.of(activeMerchant));
        when(walletClient.getWalletByNumber("WLT-CUSTOMER-001")).thenReturn(customerWallet);

        MerchantPayment pendingPayment = MerchantPayment.builder()
                .id(1L).referenceCode("PAY-TEST").merchant(activeMerchant)
                .customerUserId(20L).customerEmail("customer@example.com")
                .customerWalletNumber("WLT-CUSTOMER-001")
                .merchantWalletNumber("WLT-MERCHANT-001")
                .amount(new BigDecimal("500.00")).currency("XAF")
                .status(PaymentStatus.PENDING).build();

        when(paymentRepository.save(any())).thenReturn(pendingPayment);
        doNothing().when(walletClient).debitWallet(any(), any(), any());
        doThrow(new WalletServiceException("Merchant wallet error", 422))
                .when(walletClient).creditWallet(any(), any(), any());

        MerchantPaymentRequest req = new MerchantPaymentRequest();
        req.setMerchantCode("MCH-20240101-ABCD1234");
        req.setCustomerWalletNumber("WLT-CUSTOMER-001");
        req.setAmount(new BigDecimal("500.00"));

        assertThatThrownBy(() -> merchantService.pay(
                20L, "customer@example.com", "WLT-CUSTOMER-001", req))
                .isInstanceOf(WalletServiceException.class);

        verify(walletClient).reversalCredit(eq("WLT-CUSTOMER-001"),
                eq(new BigDecimal("500.00")), anyString());
        verify(eventPublisher).publishPaymentFailed(any());
    }

    // ================================================================
    // ADMIN STATUS UPDATE
    // ================================================================

    @Test
    @DisplayName("updateStatus — ADMIN approves a PENDING merchant")
    void updateStatus_approve_setsActive() {
        activeMerchant.setStatus(MerchantStatus.PENDING);
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(activeMerchant));
        when(merchantRepository.save(activeMerchant)).thenReturn(activeMerchant);

        AdminStatusRequest req = new AdminStatusRequest();
        req.setStatus(MerchantStatus.ACTIVE);
        req.setReason("Documents verified");

        MerchantResponse response = merchantService.updateStatus(1L, req);

        assertThat(response.getStatus()).isEqualTo(MerchantStatus.ACTIVE);
        assertThat(activeMerchant.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateStatus — ADMIN suspends an ACTIVE merchant")
    void updateStatus_suspend_setsSuspended() {
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(activeMerchant));
        when(merchantRepository.save(activeMerchant)).thenReturn(activeMerchant);

        AdminStatusRequest req = new AdminStatusRequest();
        req.setStatus(MerchantStatus.SUSPENDED);
        req.setReason("Policy violation");

        MerchantResponse response = merchantService.updateStatus(1L, req);
        assertThat(response.getStatus()).isEqualTo(MerchantStatus.SUSPENDED);
    }

    @Test
    @DisplayName("updateStatus — throws MerchantNotFoundException for unknown ID")
    void updateStatus_notFound_throws() {
        when(merchantRepository.findById(999L)).thenReturn(Optional.empty());

        AdminStatusRequest req = new AdminStatusRequest();
        req.setStatus(MerchantStatus.ACTIVE);

        assertThatThrownBy(() -> merchantService.updateStatus(999L, req))
                .isInstanceOf(MerchantNotFoundException.class);
    }

    // ================================================================
    // GET BY CODE
    // ================================================================

    @Test
    @DisplayName("getByCode — returns merchant for known code")
    void getByCode_found_returnsResponse() {
        when(merchantRepository.findByMerchantCode("MCH-20240101-ABCD1234"))
                .thenReturn(Optional.of(activeMerchant));
        MerchantResponse response = merchantService.getByCode("MCH-20240101-ABCD1234");
        assertThat(response.getBusinessName()).isEqualTo("Jean's Shop");
    }

    @Test
    @DisplayName("getByCode — throws MerchantNotFoundException for unknown code")
    void getByCode_notFound_throws() {
        when(merchantRepository.findByMerchantCode("UNKNOWN")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> merchantService.getByCode("UNKNOWN"))
                .isInstanceOf(MerchantNotFoundException.class);
    }
}
