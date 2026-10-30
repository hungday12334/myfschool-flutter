package com.example.myfschool.service;

public interface EmailService {

    void sendPasswordResetCode(String recipientEmail, String recipientName, String code);
}
