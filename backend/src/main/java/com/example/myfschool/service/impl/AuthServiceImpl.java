package com.example.myfschool.service.impl;

import com.example.myfschool.dto.request.LoginRequest;
import com.example.myfschool.dto.response.LoginResponse;
import com.example.myfschool.dto.response.UserResponse;
import com.example.myfschool.dto.request.PasswordResetRequest;
import com.example.myfschool.dto.request.PasswordResetVerifyRequest;
import com.example.myfschool.dto.request.PasswordResetConfirmRequest;
import com.example.myfschool.dto.response.PasswordResetVerifyResponse;
import com.example.myfschool.entity.User;
import com.example.myfschool.entity.UserRole;
import com.example.myfschool.enums.UserStatus;
import com.example.myfschool.enums.SchoolStatus;
import com.example.myfschool.exception.BusinessException;
import com.example.myfschool.exception.ErrorCode;
import com.example.myfschool.repository.UserRepository;
import com.example.myfschool.repository.UserRoleRepository;
import com.example.myfschool.security.JwtService;
import com.example.myfschool.service.EmailService;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, ResetCode> resetCodes = new ConcurrentHashMap<>();
    private final Map<String, ResetToken> resetTokens = new ConcurrentHashMap<>();

    public AuthServiceImpl(UserRepository users, UserRoleRepository userRoles,
            PasswordEncoder passwordEncoder, JwtService jwtService, EmailService emailService) {
        this.users = users;
        this.userRoles = userRoles;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user;
        if("FLUTTER".equalsIgnoreCase(request.source())){
            user = users.findByPhone(request.username().trim())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        } else if ("WEB".equalsIgnoreCase(request.source())) {
            user = users.findByUsername(request.username().trim())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        } else {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (user.getStatus() != UserStatus.ACTIVE
                || user.getSchool().getStatus() != SchoolStatus.ACTIVE) {
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

    @Override
    public void requestPasswordReset(PasswordResetRequest request) {
        users.findByEmailIgnoreCase(request.email().trim()).ifPresent(user -> {
            String code = String.format("%06d", secureRandom.nextInt(1_000_000));
            resetCodes.put(user.getEmail().toLowerCase(), new ResetCode(
                    passwordEncoder.encode(code), Instant.now().plusSeconds(600)));
            emailService.sendPasswordResetCode(user.getEmail(), user.getUsername(), code);
        });
    }

    @Override
    public PasswordResetVerifyResponse verifyPasswordResetCode(PasswordResetVerifyRequest request) {
        String email = request.email().trim().toLowerCase();
        ResetCode resetCode = resetCodes.get(email);
        if (resetCode == null || resetCode.expiresAt().isBefore(Instant.now())
                || !passwordEncoder.matches(request.code(), resetCode.codeHash())) {
            throw new BusinessException(ErrorCode.INVALID_OR_EXPIRED_RESET_CODE);
        }
        resetCodes.remove(email);
        String token = UUID.randomUUID().toString();
        resetTokens.put(token, new ResetToken(email, Instant.now().plusSeconds(600)));
        return new PasswordResetVerifyResponse(token);
    }

    @Override
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        if (request.newPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORDS_DO_NOT_MATCH);
        }
        ResetToken resetToken = resetTokens.get(request.resetToken());
        if (resetToken == null || resetToken.expiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.INVALID_OR_EXPIRED_RESET_TOKEN);
        }
        User user = users.findByEmailIgnoreCase(resetToken.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        users.save(user);
        resetTokens.remove(request.resetToken());
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

    private record ResetCode(String codeHash, Instant expiresAt) { }
    private record ResetToken(String email, Instant expiresAt) { }
}
