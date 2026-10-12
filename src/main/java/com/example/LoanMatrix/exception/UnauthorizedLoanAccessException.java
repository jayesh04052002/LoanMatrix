package com.example.LoanMatrix.exception;

public class UnauthorizedLoanAccessException extends RuntimeException {
    public UnauthorizedLoanAccessException(String message) {
        super(message);
    }
}
