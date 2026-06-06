package com.example.auth_service.service;

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
 * Sends transactional emails for:
 *   1. Email verification OTP (after registration)
 *   2. Password reset OTP
 *   3. Welcome email (after email is verified)
 *   4. Password changed confirmation
 *
 * All sends are @Async so they never block the HTTP response thread.
 * If mail fails (network issue, wrong config) we log the error but
 * do NOT propagate — the auth flow continues and the user can request
 * a new code.
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

    @Value("${app.base-url}")
    private String baseUrl;

    // -----------------------------------------------
    // Email Verification OTP
    // -----------------------------------------------

    @Async
    public void sendEmailVerificationOtp(String toEmail, String fullName, String otp) {
        String subject = "Verify your Smart Banking email";
        String body = buildOtpEmailBody(fullName, otp,
                "verify your email address",
                "This code will expire in 10 minutes.");
        send(toEmail, subject, body);
    }

    // -----------------------------------------------
    // Password Reset OTP
    // -----------------------------------------------

    @Async
    public void sendPasswordResetOtp(String toEmail, String fullName, String otp) {
        String subject = "Reset your Smart Banking password";
        String body = buildOtpEmailBody(fullName, otp,
                "reset your password",
                "This code will expire in 10 minutes. " +
                "If you did not request a password reset, please ignore this email.");
        send(toEmail, subject, body);
    }

    // -----------------------------------------------
    // Welcome email (after verification)
    // -----------------------------------------------

    @Async
    public void sendWelcomeEmail(String toEmail, String fullName) {
        String subject = "Welcome to Smart Banking!";
        String body = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                  <div style="background: #1a56db; padding: 20px; border-radius: 8px 8px 0 0; text-align: center;">
                    <h1 style="color: white; margin: 0;">Smart Banking</h1>
                  </div>
                  <div style="background: #f9fafb; padding: 30px; border-radius: 0 0 8px 8px;">
                    <h2 style="color: #111827;">Welcome, %s!</h2>
                    <p style="color: #6b7280;">Your email has been verified and your account is fully active.</p>
                    <p style="color: #6b7280;">You can now:</p>
                    <ul style="color: #6b7280;">
                      <li>Create and manage your digital wallet</li>
                      <li>Send and receive money instantly</li>
                      <li>Pay merchants with QR codes</li>
                      <li>Join community savings groups</li>
                    </ul>
                    <div style="text-align: center; margin-top: 30px;">
                      <a href="%s" style="background: #1a56db; color: white; padding: 12px 32px;
                         border-radius: 6px; text-decoration: none; font-weight: bold;">
                        Open Smart Banking
                      </a>
                    </div>
                    <p style="color: #9ca3af; font-size: 12px; margin-top: 30px; text-align: center;">
                      Smart Banking — Secure Digital Finance for Africa
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(fullName, baseUrl);

        send(toEmail, subject, body);
    }

    // -----------------------------------------------
    // Password changed confirmation
    // -----------------------------------------------

    @Async
    public void sendPasswordChangedEmail(String toEmail, String fullName) {
        String subject = "Your Smart Banking password has been changed";
        String body = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                  <div style="background: #1a56db; padding: 20px; border-radius: 8px 8px 0 0; text-align: center;">
                    <h1 style="color: white; margin: 0;">Smart Banking</h1>
                  </div>
                  <div style="background: #f9fafb; padding: 30px; border-radius: 0 0 8px 8px;">
                    <h2 style="color: #111827;">Password Changed</h2>
                    <p style="color: #6b7280;">Hi %s,</p>
                    <p style="color: #6b7280;">Your Smart Banking password was successfully changed.</p>
                    <div style="background: #fef3c7; border: 1px solid #f59e0b; padding: 16px; border-radius: 6px; margin: 20px 0;">
                      <p style="color: #92400e; margin: 0;">
                        <strong>If you did not make this change</strong>, please contact support immediately
                        and consider your account compromised.
                      </p>
                    </div>
                    <p style="color: #9ca3af; font-size: 12px; margin-top: 30px; text-align: center;">
                      Smart Banking — Secure Digital Finance for Africa
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(fullName);

        send(toEmail, subject, body);
    }

    // -----------------------------------------------
    // Private helpers
    // -----------------------------------------------

    private String buildOtpEmailBody(String fullName, String otp,
                                     String purpose, String expiryNote) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                  <div style="background: #1a56db; padding: 20px; border-radius: 8px 8px 0 0; text-align: center;">
                    <h1 style="color: white; margin: 0;">Smart Banking</h1>
                  </div>
                  <div style="background: #f9fafb; padding: 30px; border-radius: 0 0 8px 8px;">
                    <h2 style="color: #111827;">Hi, %s!</h2>
                    <p style="color: #6b7280;">
                      Use the following code to %s:
                    </p>
                    <div style="text-align: center; margin: 30px 0;">
                      <div style="display: inline-block; background: #1a56db; color: white;
                                  font-size: 36px; font-weight: bold; letter-spacing: 8px;
                                  padding: 16px 32px; border-radius: 8px;">
                        %s
                      </div>
                    </div>
                    <p style="color: #6b7280; text-align: center;">%s</p>
                    <p style="color: #9ca3af; font-size: 12px; margin-top: 30px; text-align: center;">
                      If you did not request this, you can safely ignore this email.<br/>
                      Smart Banking — Secure Digital Finance for Africa
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(fullName, purpose, otp, expiryNote);
    }

    /**
     * Core send method. Builds a MimeMessage (supports HTML) and dispatches it.
     * Errors are caught and logged — email failure must never crash an auth flow.
     */
    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);   // true = HTML content

            mailSender.send(message);
            log.info("Email sent successfully to: {} subject: {}", to, subject);

        } catch (MailException | MessagingException | java.io.UnsupportedEncodingException e) {
            // Log but do NOT rethrow — email failure must never crash the auth flow
            log.error("Failed to send email to: {} subject: {} error: {}", to, subject, e.getMessage());
        }
    }
}
