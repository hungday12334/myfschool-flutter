package com.example.myfschool.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^$|^0\\d{9}$", message = "Phone number must be 10 digits and start with 0")
        String phoneNumber,

        @NotBlank(message = "Password is required")
        String password
) {

    @Override
    public String toString() {
        return "LoginRequest[phoneNumber=" + phoneNumber + ", password=[REDACTED]]";
    }
}
