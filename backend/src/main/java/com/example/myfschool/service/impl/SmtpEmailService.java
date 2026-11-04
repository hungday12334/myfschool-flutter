package com.example.myfschool.service.impl;

import com.example.myfschool.exception.BusinessException;
import com.example.myfschool.exception.ErrorCode;
import com.example.myfschool.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailService implements EmailService {
    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailService(JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@myfschool.local}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendPasswordResetCode(String recipientEmail, String recipientName, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipientEmail);
        message.setSubject("MyFSchool password reset code");
        message.setText("Hello " + recipientName + ",\n\nYour password reset code is " + code
                + ". It expires in 10 minutes. If you did not request this, you can ignore this message.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new BusinessException(ErrorCode.EMAIL_DELIVERY_FAILED);
        }
    }
}
