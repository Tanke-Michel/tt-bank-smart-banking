package com.example.wallet_service.exception;
public class WalletSuspendedException extends RuntimeException {
    public WalletSuspendedException(String message) { super(message); }
}
