package com.example.notification_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Transactional email sender for the Notification Service.
 *
 * All public methods are @Async — email sending runs on a separate thread
 * so RabbitMQ listener threads are never blocked.
 *
 * If an email fails (SMTP error, wrong config) the error is logged but NOT
 * rethrown. A failed notification must never cause a message acknowledgment
 * failure — that would cause RabbitMQ to requeue and retry indefinitely.
 *
 * Emails sent:
 *   Wallet  : wallet created, deposit received, withdrawal confirmed
 *   Transfer: transfer initiated, transfer completed (sender + receiver),
 *             transfer failed
 *   Merchant: merchant registration confirmation, payment receipt (customer),
 *             payment received (merchant), payment failed
 *   Savings : group created, member joined, contribution paid,
 *             payout received
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    // ==================================================================
    // WALLET EVENTS
    // ==================================================================

    @Async
    public void sendWalletCreated(String toEmail, String ownerName, String walletNumber,
                                   String currency) {
        String subject = "Your Smart Banking wallet is ready";
        String body = buildBody("Wallet Created", ownerName, """
                <p>Your digital wallet has been successfully created.</p>
                <table style="border-collapse:collapse;margin:20px 0">
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Wallet Number</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Currency</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                </table>
                <p style="color:#6b7280">You can now deposit funds and start transacting.</p>
                """.formatted(walletNumber, currency));
        send(toEmail, subject, body);
    }

    @Async
    public void sendDepositConfirmation(String toEmail, String ownerName, String amount,
                                        String currency, String balanceAfter,
                                        String referenceCode, String description) {
        String subject = "Deposit Confirmed — " + amount + " " + currency;
        String body = buildBody("Deposit Confirmed", ownerName, """
                <p>A deposit has been credited to your wallet.</p>
                %s
                """.formatted(buildTransactionTable(amount, currency, balanceAfter,
                        referenceCode, description)));
        send(toEmail, subject, body);
    }

    @Async
    public void sendWithdrawalConfirmation(String toEmail, String ownerName, String amount,
                                            String currency, String balanceAfter,
                                            String referenceCode, String description) {
        String subject = "Withdrawal Confirmed — " + amount + " " + currency;
        String body = buildBody("Withdrawal Confirmed", ownerName, """
                <p>A withdrawal has been processed from your wallet.</p>
                %s
                """.formatted(buildTransactionTable(amount, currency, balanceAfter,
                        referenceCode, description)));
        send(toEmail, subject, body);
    }

    // ==================================================================
    // TRANSFER EVENTS
    // ==================================================================

    @Async
    public void sendTransferInitiated(String toEmail, String senderName, String amount,
                                       String currency, String recipientEmail,
                                       String referenceCode) {
        String subject = "Transfer Initiated — " + amount + " " + currency;
        String body = buildBody("Transfer In Progress", senderName, """
                <p>Your transfer is being processed.</p>
                <table style="border-collapse:collapse;margin:20px 0">
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Amount</td>
                      <td style="padding:8px;color:#6b7280">%s %s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Recipient</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Reference</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                </table>
                <p style="color:#6b7280">You will receive a confirmation once the transfer completes.</p>
                """.formatted(amount, currency, recipientEmail, referenceCode));
        send(toEmail, subject, body);
    }

    @Async
    public void sendTransferCompletedSender(String toEmail, String senderName, String amount,
                                             String currency, String recipientEmail,
                                             String referenceCode, String description) {
        String subject = "Transfer Sent — " + amount + " " + currency;
        String body = buildBody("Transfer Completed", senderName, """
                <p>Your transfer was sent successfully.</p>
                <table style="border-collapse:collapse;margin:20px 0">
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Amount Sent</td>
                      <td style="padding:8px;color:#6b7280">%s %s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">To</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Note</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Reference</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                </table>
                """.formatted(amount, currency, recipientEmail,
                        description != null ? description : "—", referenceCode));
        send(toEmail, subject, body);
    }

    @Async
    public void sendTransferCompletedReceiver(String toEmail, String receiverName, String amount,
                                               String currency, String senderEmail,
                                               String referenceCode, String description) {
        String subject = "You received " + amount + " " + currency;
        String body = buildBody("Money Received", receiverName, """
                <p>You have received a transfer.</p>
                <table style="border-collapse:collapse;margin:20px 0">
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Amount</td>
                      <td style="padding:8px;color:#6b7280">%s %s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">From</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Note</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Reference</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                </table>
                """.formatted(amount, currency, senderEmail,
                        description != null ? description : "—", referenceCode));
        send(toEmail, subject, body);
    }

    @Async
    public void sendTransferFailed(String toEmail, String senderName, String amount,
                                    String currency, String recipientEmail,
                                    String referenceCode) {
        String subject = "Transfer Failed — " + amount + " " + currency;
        String body = buildBody("Transfer Failed", senderName, """
                <div style="background:#fef2f2;border:1px solid #fca5a5;padding:16px;border-radius:6px;margin:20px 0">
                  <p style="color:#991b1b;margin:0">
                    <strong>Your transfer could not be completed.</strong>
                    Your funds have been returned to your wallet if they were debited.
                  </p>
                </div>
                <table style="border-collapse:collapse;margin:20px 0">
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Amount</td>
                      <td style="padding:8px;color:#6b7280">%s %s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Intended Recipient</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Reference</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                </table>
                <p style="color:#6b7280">Please try again or contact support if this issue persists.</p>
                """.formatted(amount, currency, recipientEmail, referenceCode));
        send(toEmail, subject, body);
    }

    // ==================================================================
    // MERCHANT EVENTS
    // ==================================================================

    @Async
    public void sendMerchantRegistered(String toEmail, String ownerName,
                                        String businessName, String merchantCode) {
        String subject = "Merchant Registration Received — " + businessName;
        String body = buildBody("Merchant Registration Submitted", ownerName, """
                <p>Your merchant account registration for <strong>%s</strong> has been received.</p>
                <table style="border-collapse:collapse;margin:20px 0">
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Business Name</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Merchant Code</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Status</td>
                      <td style="padding:8px;color:#f59e0b">⏳ Pending Review</td></tr>
                </table>
                <p style="color:#6b7280">Our team will review your application. You will receive an email once it is approved.</p>
                """.formatted(businessName, businessName, merchantCode));
        send(toEmail, subject, body);
    }

    @Async
    public void sendMerchantPaymentReceipt(String toEmail, String customerName,
                                            String businessName, String amount,
                                            String currency, String referenceCode,
                                            String description) {
        String subject = "Payment to " + businessName + " — " + amount + " " + currency;
        String body = buildBody("Payment Receipt", customerName, """
                <p>Your payment has been processed successfully.</p>
                <table style="border-collapse:collapse;margin:20px 0">
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Merchant</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Amount</td>
                      <td style="padding:8px;color:#6b7280">%s %s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Note</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Reference</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                </table>
                """.formatted(businessName, amount, currency,
                        description != null ? description : "—", referenceCode));
        send(toEmail, subject, body);
    }

    @Async
    public void sendMerchantPaymentReceived(String toEmail, String businessName,
                                             String customerEmail, String amount,
                                             String currency, String referenceCode) {
        String subject = "Payment Received — " + amount + " " + currency;
        String body = buildBody("Payment Received", businessName, """
                <p>A customer has paid your merchant account.</p>
                <table style="border-collapse:collapse;margin:20px 0">
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Customer</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Amount</td>
                      <td style="padding:8px;color:#6b7280">%s %s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Reference</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                </table>
                """.formatted(customerEmail, amount, currency, referenceCode));
        send(toEmail, subject, body);
    }

    @Async
    public void sendMerchantPaymentFailed(String toEmail, String customerName,
                                           String businessName, String amount,
                                           String currency, String referenceCode) {
        String subject = "Payment Failed — " + businessName;
        String body = buildBody("Payment Failed", customerName, """
                <div style="background:#fef2f2;border:1px solid #fca5a5;padding:16px;border-radius:6px;margin:20px 0">
                  <p style="color:#991b1b;margin:0">
                    <strong>Your payment to %s could not be processed.</strong>
                    If funds were debited, they have been returned to your wallet.
                  </p>
                </div>
                <table style="border-collapse:collapse;margin:20px 0">
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Amount</td>
                      <td style="padding:8px;color:#6b7280">%s %s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Reference</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                </table>
                """.formatted(businessName, amount, currency, referenceCode));
        send(toEmail, subject, body);
    }

    // ==================================================================
    // SAVINGS EVENTS
    // ==================================================================

    @Async
    public void sendSavingsGroupCreated(String toEmail, String creatorName,
                                         String groupName, String contributionAmount,
                                         String currency, String payoutCycle,
                                         String startDate) {
        String subject = "Savings Group Created — " + groupName;
        String body = buildBody("Savings Group Created", creatorName, """
                <p>Your community savings group has been created successfully.</p>
                <table style="border-collapse:collapse;margin:20px 0">
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Group Name</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Contribution</td>
                      <td style="padding:8px;color:#6b7280">%s %s per %s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Start Date</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                </table>
                <p style="color:#6b7280">Share your group with others to fill all the slots and start the cycle.</p>
                """.formatted(groupName, contributionAmount, currency,
                        payoutCycle.toLowerCase(), startDate));
        send(toEmail, subject, body);
    }

    @Async
    public void sendSavingsMemberJoined(String toEmail, String memberName,
                                         String groupName, int payoutOrder) {
        String subject = "You have joined — " + groupName;
        String body = buildBody("Joined Savings Group", memberName, """
                <p>You have successfully joined the savings group.</p>
                <table style="border-collapse:collapse;margin:20px 0">
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Group</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Your Payout Position</td>
                      <td style="padding:8px;color:#6b7280">#%d</td></tr>
                </table>
                <p style="color:#6b7280">You will receive the pot when round %d begins.</p>
                """.formatted(groupName, payoutOrder, payoutOrder));
        send(toEmail, subject, body);
    }

    @Async
    public void sendContributionConfirmation(String toEmail, String memberName,
                                              String groupName, int roundNumber,
                                              String amount, String currency,
                                              String referenceCode, boolean paid) {
        String statusLabel = paid ? "✅ Paid" : "❌ Failed";
        String subject = paid
                ? "Contribution Paid — " + groupName + " Round " + roundNumber
                : "Contribution Failed — " + groupName + " Round " + roundNumber;
        String body = buildBody("Contribution " + (paid ? "Confirmed" : "Failed"), memberName, """
                <table style="border-collapse:collapse;margin:20px 0">
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Group</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Round</td>
                      <td style="padding:8px;color:#6b7280">%d</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Amount</td>
                      <td style="padding:8px;color:#6b7280">%s %s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Status</td>
                      <td style="padding:8px">%s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Reference</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                </table>
                %s
                """.formatted(groupName, roundNumber, amount, currency,
                        statusLabel, referenceCode,
                        paid ? "" : "<p style=\"color:#dc2626\">Please ensure your wallet has sufficient funds and try again.</p>"));
        send(toEmail, subject, body);
    }

    @Async
    public void sendPayoutNotification(String toEmail, String recipientName,
                                        String groupName, int roundNumber,
                                        String amount, String currency,
                                        String referenceCode, boolean completed) {
        String subject = completed
                ? "Payout Received — " + groupName + " Round " + roundNumber
                : "Payout Failed — " + groupName + " Round " + roundNumber;
        String body = buildBody(completed ? "Payout Received 🎉" : "Payout Failed", recipientName, completed
                ? """
                  <p>Congratulations! The savings pot for round %d has been credited to your wallet.</p>
                  <table style="border-collapse:collapse;margin:20px 0">
                    <tr><td style="padding:8px;font-weight:bold;color:#374151">Group</td>
                        <td style="padding:8px;color:#6b7280">%s</td></tr>
                    <tr><td style="padding:8px;font-weight:bold;color:#374151">Amount Received</td>
                        <td style="padding:8px;color:#059669;font-size:20px;font-weight:bold">%s %s</td></tr>
                    <tr><td style="padding:8px;font-weight:bold;color:#374151">Reference</td>
                        <td style="padding:8px;color:#6b7280">%s</td></tr>
                  </table>
                  """.formatted(roundNumber, groupName, amount, currency, referenceCode)
                : """
                  <div style="background:#fef2f2;border:1px solid #fca5a5;padding:16px;border-radius:6px;margin:20px 0">
                    <p style="color:#991b1b;margin:0">
                      <strong>Your payout for round %d could not be processed.</strong>
                      Please contact support with reference: %s
                    </p>
                  </div>
                  <table style="border-collapse:collapse;margin:20px 0">
                    <tr><td style="padding:8px;font-weight:bold;color:#374151">Group</td>
                        <td style="padding:8px;color:#6b7280">%s</td></tr>
                    <tr><td style="padding:8px;font-weight:bold;color:#374151">Amount</td>
                        <td style="padding:8px;color:#6b7280">%s %s</td></tr>
                  </table>
                  """.formatted(roundNumber, referenceCode, groupName, amount, currency));
        send(toEmail, subject, body);
    }

    // ==================================================================
    // Private helpers
    // ==================================================================

    /**
     * Wraps content in the standard Smart Banking HTML email layout.
     */
    private String buildBody(String heading, String recipientName, String contentHtml) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;background:#f9fafb">
                  <div style="background:#1a56db;padding:20px;border-radius:8px 8px 0 0;text-align:center">
                    <h1 style="color:white;margin:0;font-size:22px">Smart Banking</h1>
                  </div>
                  <div style="background:#ffffff;padding:30px;border-radius:0 0 8px 8px;border:1px solid #e5e7eb">
                    <h2 style="color:#111827;margin-top:0">%s</h2>
                    <p style="color:#374151">Hi <strong>%s</strong>,</p>
                    %s
                    <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0"/>
                    <p style="color:#9ca3af;font-size:12px;text-align:center;margin:0">
                      Smart Banking — Secure Digital Finance for Africa<br/>
                      This is an automated notification. Please do not reply to this email.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(heading, recipientName, contentHtml);
    }

    private String buildTransactionTable(String amount, String currency, String balanceAfter,
                                          String referenceCode, String description) {
        return """
                <table style="border-collapse:collapse;margin:20px 0;width:100%%">
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Amount</td>
                      <td style="padding:8px;color:#6b7280">%s %s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">New Balance</td>
                      <td style="padding:8px;color:#059669;font-weight:bold">%s %s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Note</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                  <tr><td style="padding:8px;font-weight:bold;color:#374151">Reference</td>
                      <td style="padding:8px;color:#6b7280">%s</td></tr>
                </table>
                """.formatted(amount, currency, balanceAfter, currency,
                        description != null ? description : "—", referenceCode);
    }

    /**
     * Sends an HTML email. Catches all exceptions so listener threads
     * are never interrupted by email delivery failures.
     */
    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent: to={} subject={}", to, subject);
        } catch (MailException | MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to={} subject={} error={}", to, subject, e.getMessage());
        }
    }
}
