package com.example.myfschool.dto.response;

import com.example.myfschool.enums.UserRole;

public record UserResponse(
        Long id,
        String phoneNumber,
        String fullName,
        UserRole role
) {
}
