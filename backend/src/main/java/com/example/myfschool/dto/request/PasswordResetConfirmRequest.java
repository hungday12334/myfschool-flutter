package com.example.myfschool.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank(message = "Reset token is required")
        @Size(max = 200, message = "Reset token is invalid")
        String resetToken,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 72, message = "New password must be between 8 and 72 characters")
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        @Size(min = 8, max = 72, message = "Password confirmation must be between 8 and 72 characters")
        String confirmPassword
) {
}
