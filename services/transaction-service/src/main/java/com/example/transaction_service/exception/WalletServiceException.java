package com.example.transaction_service.exception;
/**
 * Thrown when the wallet-service HTTP call fails (e.g. service down,
 * wallet not found, wallet suspended, insufficient funds reported
 * by the wallet service).
 */
public class WalletServiceException extends RuntimeException {
    private final int statusCode;
    public WalletServiceException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    public int getStatusCode() { return statusCode; }
}
