package com.example.savings_service.service;

import com.example.savings_service.dto.WalletInfo;
import com.example.savings_service.exception.WalletServiceException;
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
 * Used by the Savings Service to:
 *   1. Validate a member's wallet before they join a group.
 *   2. Debit members' wallets when collecting contributions.
 *   3. Credit the payout recipient's wallet with the accumulated pot.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.wallet-service.base-url}")
    private String walletBaseUrl;

    // ------------------------------------------------------------------
    // Wallet lookup
    // ------------------------------------------------------------------

    public WalletInfo getWalletByNumber(String walletNumber) {
        return callGet(walletBaseUrl + "/api/v1/wallet/number/" + walletNumber,
                WalletInfo.class);
    }

    // ------------------------------------------------------------------
    // Contribution debit — debits a member's wallet
    // ------------------------------------------------------------------

    public void debitWallet(String walletNumber, BigDecimal amount, String referenceCode) {
        String url = UriComponentsBuilder
                .fromHttpUrl(walletBaseUrl + "/api/v1/wallet/internal/debit")
                .queryParam("walletNumber", walletNumber)
                .queryParam("amount", amount.toPlainString())
                .queryParam("referenceCode", referenceCode)
                .toUriString();
        callPost(url);
        log.info("Contribution debit: wallet={} amount={} ref={}", walletNumber, amount, referenceCode);
    }

    // ------------------------------------------------------------------
    // Payout credit — credits the recipient's wallet
    // ------------------------------------------------------------------

    public void creditWallet(String walletNumber, BigDecimal amount, String referenceCode) {
        String url = UriComponentsBuilder
                .fromHttpUrl(walletBaseUrl + "/api/v1/wallet/internal/credit")
                .queryParam("walletNumber", walletNumber)
                .queryParam("amount", amount.toPlainString())
                .queryParam("referenceCode", referenceCode)
                .toUriString();
        callPost(url);
        log.info("Payout credit: wallet={} amount={} ref={}", walletNumber, amount, referenceCode);
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private <T> T callGet(String url, Class<T> type) {
        try {
            return restTemplate.getForEntity(url, type).getBody();
        } catch (HttpClientErrorException e) {
            throw new WalletServiceException(
                    extractMessage(e.getResponseBodyAsString()), e.getStatusCode().value());
        } catch (HttpServerErrorException e) {
            throw new WalletServiceException(
                    "Wallet service internal error", e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            throw new WalletServiceException(
                    "Wallet service is unavailable. Please try again later.", 503);
        }
    }

    private void callPost(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(url, new HttpEntity<>("", headers), Void.class);
        } catch (HttpClientErrorException e) {
            throw new WalletServiceException(
                    extractMessage(e.getResponseBodyAsString()), e.getStatusCode().value());
        } catch (HttpServerErrorException e) {
            throw new WalletServiceException(
                    "Wallet service internal error during operation", e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            throw new WalletServiceException(
                    "Wallet service is unavailable. Please try again later.", 503);
        }
    }

    private String extractMessage(String body) {
        if (body != null && body.contains("\"message\"")) {
            int start = body.indexOf("\"message\"") + 11;
            int end   = body.indexOf("\"", start + 1);
            if (start > 0 && end > start) return body.substring(start, end);
        }
        return body != null ? body : "Unknown wallet error";
    }
}
