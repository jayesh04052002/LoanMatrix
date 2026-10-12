package com.example.LoanMatrix.service.Email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendKycStatusEmail(String to, String subject, String message) {
        try {
            MimeMessage mailMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mailMessage);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(message, true);

            mailSender.send(mailMessage);

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}