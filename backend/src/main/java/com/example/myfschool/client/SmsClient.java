package com.example.myfschool.client;

public interface SmsClient {

    String sendSms(String phoneNumber, String message);
}