package com.example.LoanMatrix.exception;

public class PaymentAlreadyVerifiedException extends RuntimeException {
    public PaymentAlreadyVerifiedException(String message) {
        super(message);
    }
}
