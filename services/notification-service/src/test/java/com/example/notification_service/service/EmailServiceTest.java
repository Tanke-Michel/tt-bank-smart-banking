package com.example.notification_service.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailService.
 *
 * We verify that:
 *   1. send() is called on JavaMailSender with the expected number of invocations.
 *   2. SMTP failures are caught silently — no exception propagates.
 *   3. Every notification method can be called without throwing.
 *
 * We do NOT test email body content here — that would be brittle and
 * depend on exact HTML string matches.
 */
@DisplayName("EmailService Unit Tests")
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@smartbanking.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Smart Banking");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    // ================================================================
    // Wallet notifications
    // ================================================================

    @Test
    @DisplayName("sendWalletCreated — calls mailSender.send() once")
    void sendWalletCreated_callsSendOnce() throws Exception {
        emailService.sendWalletCreated("user@example.com", "Jean Dupont",
                "WLT-20240101-ABCD1234", "XAF");
        // @Async runs synchronously in tests when called directly
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendDepositConfirmation — calls mailSender.send() once")
    void sendDepositConfirmation_callsSendOnce() throws Exception {
        emailService.sendDepositConfirmation("user@example.com", "Jean",
                "5000.00", "XAF", "25000.00", "DEP-20240101-ABCD", "Mobile money");
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendWithdrawalConfirmation — calls mailSender.send() once")
    void sendWithdrawalConfirmation_callsSendOnce() throws Exception {
        emailService.sendWithdrawalConfirmation("user@example.com", "Jean",
                "2000.00", "XAF", "18000.00", "WDR-20240101-ABCD", "ATM");
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // ================================================================
    // Transfer notifications
    // ================================================================

    @Test
    @DisplayName("sendTransferInitiated — calls mailSender.send() once")
    void sendTransferInitiated_callsSendOnce() throws Exception {
        emailService.sendTransferInitiated("sender@example.com", "Sender",
                "500.00", "XAF", "receiver@example.com", "TXN-001");
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendTransferCompletedSender — calls mailSender.send() once")
    void sendTransferCompletedSender_callsSendOnce() throws Exception {
        emailService.sendTransferCompletedSender("sender@example.com", "Sender",
                "500.00", "XAF", "receiver@example.com", "TXN-001", "Rent");
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendTransferCompletedReceiver — calls mailSender.send() once")
    void sendTransferCompletedReceiver_callsSendOnce() throws Exception {
        emailService.sendTransferCompletedReceiver("receiver@example.com", "Receiver",
                "500.00", "XAF", "sender@example.com", "TXN-001", "Rent");
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendTransferFailed — calls mailSender.send() once")
    void sendTransferFailed_callsSendOnce() throws Exception {
        emailService.sendTransferFailed("sender@example.com", "Sender",
                "500.00", "XAF", "receiver@example.com", "TXN-001");
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // ================================================================
    // Merchant notifications
    // ================================================================

    @Test
    @DisplayName("sendMerchantRegistered — calls mailSender.send() once")
    void sendMerchantRegistered_callsSendOnce() throws Exception {
        emailService.sendMerchantRegistered("owner@example.com", "Jean",
                "Jean's Shop", "MCH-20240101-ABCD1234");
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendMerchantPaymentReceipt — calls mailSender.send() once")
    void sendMerchantPaymentReceipt_callsSendOnce() throws Exception {
        emailService.sendMerchantPaymentReceipt("customer@example.com", "Customer",
                "Jean's Shop", "500.00", "XAF", "PAY-001", "Lunch");
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendMerchantPaymentReceived — calls mailSender.send() once")
    void sendMerchantPaymentReceived_callsSendOnce() throws Exception {
        emailService.sendMerchantPaymentReceived("owner@example.com", "Jean's Shop",
                "customer@example.com", "500.00", "XAF", "PAY-001");
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendMerchantPaymentFailed — calls mailSender.send() once")
    void sendMerchantPaymentFailed_callsSendOnce() throws Exception {
        emailService.sendMerchantPaymentFailed("customer@example.com", "Customer",
                "Jean's Shop", "500.00", "XAF", "PAY-001");
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // ================================================================
    // Savings notifications
    // ================================================================

    @Test
    @DisplayName("sendSavingsGroupCreated — calls mailSender.send() once")
    void sendSavingsGroupCreated_callsSendOnce() throws Exception {
        emailService.sendSavingsGroupCreated("creator@example.com", "Creator",
                "Njangi Circle", "5000.00", "XAF", "MONTHLY", "2024-02-01");
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendSavingsMemberJoined — calls mailSender.send() once")
    void sendSavingsMemberJoined_callsSendOnce() throws Exception {
        emailService.sendSavingsMemberJoined("member@example.com", "Member",
                "Njangi Circle", 2);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendContributionConfirmation PAID — calls mailSender.send() once")
    void sendContributionConfirmation_paid_callsSendOnce() throws Exception {
        emailService.sendContributionConfirmation("member@example.com", "Member",
                "Njangi Circle", 1, "5000.00", "XAF", "CONT-001", true);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendContributionConfirmation FAILED — calls mailSender.send() once")
    void sendContributionConfirmation_failed_callsSendOnce() throws Exception {
        emailService.sendContributionConfirmation("member@example.com", "Member",
                "Njangi Circle", 1, "5000.00", "XAF", "CONT-001", false);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendPayoutNotification COMPLETED — calls mailSender.send() once")
    void sendPayoutNotification_completed_callsSendOnce() throws Exception {
        emailService.sendPayoutNotification("recipient@example.com", "Recipient",
                "Njangi Circle", 1, "15000.00", "XAF", "POUT-001", true);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendPayoutNotification FAILED — calls mailSender.send() once")
    void sendPayoutNotification_failed_callsSendOnce() throws Exception {
        emailService.sendPayoutNotification("recipient@example.com", "Recipient",
                "Njangi Circle", 1, "15000.00", "XAF", "POUT-001", false);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // ================================================================
    // Failure resilience
    // ================================================================

    @Test
    @DisplayName("SMTP failure — exception is caught and does NOT propagate")
    void smtpFailure_doesNotPropagate() {
        // Simulate SMTP failure
        doThrow(new org.springframework.mail.MailSendException("SMTP unavailable"))
                .when(mailSender).send(any(MimeMessage.class));

        // Must NOT throw — listener threads must never be interrupted by email failures
        org.assertj.core.api.Assertions.assertThatCode(() ->
                emailService.sendWalletCreated("user@example.com", "Jean",
                        "WLT-001", "XAF")
        ).doesNotThrowAnyException();
    }
}
