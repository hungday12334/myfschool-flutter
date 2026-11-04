package com.example.myfschool.service.impl;

import com.example.myfschool.dto.request.LoginRequest;
import com.example.myfschool.dto.response.LoginResponse;
import com.example.myfschool.dto.response.UserResponse;
import com.example.myfschool.entity.User;
import com.example.myfschool.entity.UserRole;
import com.example.myfschool.enums.UserStatus;
import com.example.myfschool.exception.BusinessException;
import com.example.myfschool.exception.ErrorCode;
import com.example.myfschool.repository.UserRepository;
import com.example.myfschool.repository.UserRoleRepository;
import com.example.myfschool.security.JwtService;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements com.example.myfschool.service.AuthService {
    private final UserRepository users;
    private final UserRoleRepository userRoles;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository users, UserRoleRepository userRoles,
            PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.userRoles = userRoles;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = users.findByUsername(request.username().trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }
        List<String> roles = roleCodes(user.getId());
        if (roles.isEmpty()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "No role is assigned to this account");
        }
        return new LoginResponse(jwtService.generateToken(user, roles), "Bearer",
                jwtService.getExpirationSeconds(), toResponse(user, roles));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        return toResponse(user, roleCodes(user.getId()));
    }

    private List<String> roleCodes(Long userId) {
        return userRoles.findByUser_IdOrderByIdAsc(userId).stream()
                .map(UserRole::getRole)
                .map(role -> role.getCode())
                .distinct()
                .toList();
    }

    private UserResponse toResponse(User user, List<String> roles) {
        return new UserResponse(user.getId(), user.getUsername(), user.getPhone(), user.getEmail(), roles,
                user.getStatus(), user.isMustChangePassword());
    }
}
