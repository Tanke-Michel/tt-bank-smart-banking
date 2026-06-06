package com.example.wallet_service.exception;

public class SelfTransferException extends RuntimeException {
    public SelfTransferException(String message) { super(message); }
}
