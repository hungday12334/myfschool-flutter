package com.example.myfschool.service;

import com.example.myfschool.dto.request.LoginRequest;
import com.example.myfschool.dto.request.PasswordResetConfirmRequest;
import com.example.myfschool.dto.request.PasswordResetRequest;
import com.example.myfschool.dto.request.PasswordResetVerifyRequest;
import com.example.myfschool.dto.response.LoginResponse;
import com.example.myfschool.dto.response.PasswordResetVerifyResponse;
import com.example.myfschool.dto.response.UserResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void requestPasswordReset(PasswordResetRequest request);

    PasswordResetVerifyResponse verifyPasswordResetCode(PasswordResetVerifyRequest request);

    void confirmPasswordReset(PasswordResetConfirmRequest request);

    UserResponse getCurrentUser(String phoneNumber);
}
