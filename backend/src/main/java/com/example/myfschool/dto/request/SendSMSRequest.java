package com.example.myfschool.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SendSMSRequest(
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^$|^0\\d{9}$", message = "Phone number must be 10 digits and start with 0")
        String phoneNumber) {
}
