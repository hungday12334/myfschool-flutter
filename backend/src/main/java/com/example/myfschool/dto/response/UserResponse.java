package com.example.myfschool.dto.response;

import com.example.myfschool.enums.UserStatus;
import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String phone,
        String email,
        List<String> roles,
        UserStatus status,
        boolean mustChangePassword
) {
    public UserResponse {
        roles = List.copyOf(roles);
    }
}
