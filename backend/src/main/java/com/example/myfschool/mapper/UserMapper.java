package com.example.myfschool.mapper;

import com.example.myfschool.dto.response.UserResponse;
import com.example.myfschool.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getPhoneNumber(),
                user.getFullName(),
                user.getRole()
        );
    }
}
