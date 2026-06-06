package com.example.merchant_service.exception;
public class WalletServiceException extends RuntimeException {
    private final int statusCode;
    public WalletServiceException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    public int getStatusCode() { return statusCode; }
}
