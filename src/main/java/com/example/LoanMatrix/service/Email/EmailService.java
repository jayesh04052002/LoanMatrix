package com.example.LoanMatrix.service.Email;


public interface EmailService {

    void sendKycStatusEmail(String to, String subject, String message);
}