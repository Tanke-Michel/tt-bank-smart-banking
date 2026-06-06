package com.example.merchant_service.exception;
public class MerchantAlreadyExistsException extends RuntimeException {
    public MerchantAlreadyExistsException(String message) { super(message); }
}
