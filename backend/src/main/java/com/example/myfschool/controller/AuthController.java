package com.example.myfschool.controller;

import com.example.myfschool.dto.request.LoginRequest;
import com.example.myfschool.dto.request.PasswordResetConfirmRequest;
import com.example.myfschool.dto.request.PasswordResetRequest;
import com.example.myfschool.dto.request.PasswordResetVerifyRequest;
import com.example.myfschool.dto.response.ApiResponse;
import com.example.myfschool.dto.response.LoginResponse;
import com.example.myfschool.dto.response.PasswordResetVerifyResponse;
import com.example.myfschool.dto.response.UserResponse;
import com.example.myfschool.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(
                "If an account exists for this email, a reset code has been sent",
                null
        ));
    }

    @PostMapping("/password-reset/verify")
    public ResponseEntity<ApiResponse<PasswordResetVerifyResponse>> verifyPasswordResetCode(
            @Valid @RequestBody PasswordResetVerifyRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Reset code verified",
                authService.verifyPasswordResetCode(request)
        ));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request
    ) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "User retrieved successfully",
                authService.getCurrentUser(authentication.getName())
        ));
    }
}
