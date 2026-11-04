package com.example.myfschool.service;

/** Provider boundary for a future FCM/APNs adapter. No delivery provider is configured. */
public interface PushNotificationService {
    void sendToUser(Long userId, String title, String body);
}
