package com.example.transaction_service.exception;
public class SelfTransferException extends RuntimeException {
    public SelfTransferException(String message) { super(message); }
}
