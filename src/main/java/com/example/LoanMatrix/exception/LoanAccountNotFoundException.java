package com.example.LoanMatrix.exception;

public class LoanAccountNotFoundException extends RuntimeException {
    public LoanAccountNotFoundException(String message) {
        super(message);
    }
}
