
package com.example.LoanMatrix.service.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendOtpEmail(String email, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("LoanMatrix Email Verification OTP");
        message.setText(
                "Dear Customer,\n\n"
                        + "Your LoanMatrix email verification OTP is: " + otp + "\n\n"
                        + "This OTP is valid for 5 minutes.\n\n"
                        + "Please do not share this OTP with anyone.\n\n"
                        + "Regards,\n"
                        + "LoanMatrix Team"
        );

        mailSender.send(message);
    }
}
