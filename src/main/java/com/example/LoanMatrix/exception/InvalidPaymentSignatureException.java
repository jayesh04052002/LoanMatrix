package com.example.LoanMatrix.exception;

public class InvalidPaymentSignatureException extends RuntimeException {
    public InvalidPaymentSignatureException(String message) {
        super(message);
    }
}
