package com.example.transaction_service.service;

import com.example.transaction_service.dto.WalletInfo;
import com.example.transaction_service.exception.WalletServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;

/**
 * HTTP client for the wallet-service.
 *
 * All calls are synchronous — the transaction must not be committed
 * to the database until both debit and credit succeed (or we know
 * exactly which step failed).
 *
 * Error handling:
 *   - HttpClientErrorException (4xx): wallet said "no" → re-throw as WalletServiceException
 *   - HttpServerErrorException (5xx): wallet-service crashed → re-throw as WalletServiceException
 *   - ResourceAccessException: wallet-service unreachable → re-throw with 503
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.wallet-service.base-url}")
    private String walletBaseUrl;

    // -----------------------------------------------
    // Resolve wallet by email
    // -----------------------------------------------

    /**
     * Looks up a wallet by the owner's email address.
     * Used to resolve the sender's wallet and the recipient's wallet.
     */
    public WalletInfo getWalletByEmail(String email) {
        String url = walletBaseUrl + "/api/v1/wallet/number/{walletNumber}";
        // We don't have a direct "by email" endpoint in wallet-service,
        // so we use the phone-number-based lookup indirectly:
        // In practice the transaction service receives the sender's wallet number
        // directly from the JWT/gateway. For the receiver we resolve by email
        // via a dedicated internal endpoint we'll add to wallet-service.
        // For now, the wallet-service exposes GET /api/v1/wallet/phone/{phone}
        // but NOT GET /api/v1/wallet/email/{email}.
        // We call a lookup by email which we add as an internal endpoint.
        String lookupUrl = walletBaseUrl + "/api/v1/wallet/email/" + email;
        return callGet(lookupUrl, WalletInfo.class);
    }

    /**
     * Looks up a wallet by wallet number.
     */
    public WalletInfo getWalletByNumber(String walletNumber) {
        String url = walletBaseUrl + "/api/v1/wallet/number/" + walletNumber;
        return callGet(url, WalletInfo.class);
    }

    // -----------------------------------------------
    // Debit sender
    // -----------------------------------------------

    /**
     * Debits the sender's wallet.
     * Called FIRST — if this fails, no money moves.
     */
    public void debitWallet(String walletNumber, BigDecimal amount, String referenceCode) {
        String url = UriComponentsBuilder
                .fromHttpUrl(walletBaseUrl + "/api/v1/wallet/internal/debit")
                .queryParam("walletNumber", walletNumber)
                .queryParam("amount", amount.toPlainString())
                .queryParam("referenceCode", referenceCode)
                .toUriString();
        callPost(url, Void.class);
        log.info("Debit succeeded: wallet={} amount={} ref={}", walletNumber, amount, referenceCode);
    }

    // -----------------------------------------------
    // Credit receiver
    // -----------------------------------------------

    /**
     * Credits the receiver's wallet.
     * Called AFTER a successful debit.
     */
    public void creditWallet(String walletNumber, BigDecimal amount, String referenceCode) {
        String url = UriComponentsBuilder
                .fromHttpUrl(walletBaseUrl + "/api/v1/wallet/internal/credit")
                .queryParam("walletNumber", walletNumber)
                .queryParam("amount", amount.toPlainString())
                .queryParam("referenceCode", referenceCode)
                .toUriString();
        callPost(url, Void.class);
        log.info("Credit succeeded: wallet={} amount={} ref={}", walletNumber, amount, referenceCode);
    }

    // -----------------------------------------------
    // Compensating credit — reversal if credit fails after debit succeeded
    // -----------------------------------------------

    /**
     * Reverses a debit by re-crediting the sender's wallet.
     * Called only if creditWallet() throws after debitWallet() succeeded.
     * This is the compensating transaction for the saga pattern.
     */
    public void reversalCredit(String walletNumber, BigDecimal amount, String referenceCode) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(walletBaseUrl + "/api/v1/wallet/internal/credit")
                    .queryParam("walletNumber", walletNumber)
                    .queryParam("amount", amount.toPlainString())
                    .queryParam("referenceCode", referenceCode + "-REV")
                    .toUriString();
            callPost(url, Void.class);
            log.info("Reversal credit succeeded: wallet={} amount={} ref={}-REV",
                    walletNumber, amount, referenceCode);
        } catch (Exception e) {
            // Log at ERROR level — this is a financial inconsistency that needs manual resolution
            log.error("CRITICAL: Reversal credit FAILED for wallet={} amount={} ref={}-REV. " +
                    "Manual intervention required. Error: {}",
                    walletNumber, amount, referenceCode, e.getMessage());
            // Do NOT rethrow — we want to mark the transaction FAILED in the DB
            // and let operations team handle the reversal manually.
        }
    }

    // -----------------------------------------------
    // Internal HTTP helpers
    // -----------------------------------------------

    private <T> T callGet(String url, Class<T> responseType) {
        try {
            ResponseEntity<T> response = restTemplate.getForEntity(url, responseType);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new WalletServiceException(
                    "Wallet service error: " + extractMessage(e.getResponseBodyAsString()),
                    e.getStatusCode().value());
        } catch (HttpServerErrorException e) {
            throw new WalletServiceException(
                    "Wallet service internal error", e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            throw new WalletServiceException(
                    "Wallet service is currently unavailable. Please try again later.", 503);
        }
    }

    private <T> void callPost(String url, Class<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("", headers);
            restTemplate.postForEntity(url, entity, responseType);
        } catch (HttpClientErrorException e) {
            throw new WalletServiceException(
                    "Wallet operation failed: " + extractMessage(e.getResponseBodyAsString()),
                    e.getStatusCode().value());
        } catch (HttpServerErrorException e) {
            throw new WalletServiceException(
                    "Wallet service internal error during operation", e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            throw new WalletServiceException(
                    "Wallet service is currently unavailable. Please try again later.", 503);
        }
    }

    private String extractMessage(String body) {
        // Try to extract the "message" field from a JSON error body
        if (body != null && body.contains("\"message\"")) {
            int start = body.indexOf("\"message\"") + 11;
            int end   = body.indexOf("\"", start + 1);
            if (start > 0 && end > start) {
                return body.substring(start, end);
            }
        }
        return body != null ? body : "Unknown error";
    }
}
