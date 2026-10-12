package com.example.LoanMatrix.exception;

public class InvalidLoanStatusException extends RuntimeException {
    public InvalidLoanStatusException(String message) {
        super(message);
    }
}
