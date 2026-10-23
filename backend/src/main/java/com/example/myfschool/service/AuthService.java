package com.example.myfschool.service;

import com.example.myfschool.dto.request.LoginRequest;
import com.example.myfschool.dto.response.LoginResponse;
import com.example.myfschool.dto.response.UserResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    UserResponse getCurrentUser(String phoneNumber);
}
