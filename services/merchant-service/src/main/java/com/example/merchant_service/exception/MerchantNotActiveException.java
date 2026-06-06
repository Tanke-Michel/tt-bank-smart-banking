package com.example.merchant_service.exception;
public class MerchantNotActiveException extends RuntimeException {
    public MerchantNotActiveException(String message) { super(message); }
}
