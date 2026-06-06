package com.example.savings_service.exception;
public class PayoutNotFoundException extends RuntimeException {
    public PayoutNotFoundException(String message) { super(message); }
}
