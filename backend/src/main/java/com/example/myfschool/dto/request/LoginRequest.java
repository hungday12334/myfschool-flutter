package com.example.myfschool.dto.request;

import jakarta.validation.constraints.NotBlank;

import javax.print.attribute.standard.MediaSize;

public record LoginRequest(
        @NotBlank(message = "Username is required") String username,

        @NotBlank(message = "Password is required")
        String password,
        @NotBlank(message = "source is required")
        String source
) {

    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=[REDACTED]]";
    }
}
