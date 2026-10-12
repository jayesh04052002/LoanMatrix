package com.example.LoanMatrix.exception;

public class EmiAlreadyPaidException extends RuntimeException {
    public EmiAlreadyPaidException(String message) {
        super(message);
    }
}
