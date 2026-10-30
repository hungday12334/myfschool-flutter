package com.example.myfschool.service.impl;

import com.example.myfschool.dto.request.LoginRequest;
import com.example.myfschool.dto.request.PasswordResetConfirmRequest;
import com.example.myfschool.dto.request.PasswordResetRequest;
import com.example.myfschool.dto.request.PasswordResetVerifyRequest;
import com.example.myfschool.dto.response.LoginResponse;
import com.example.myfschool.dto.response.PasswordResetVerifyResponse;
import com.example.myfschool.dto.response.UserResponse;
import com.example.myfschool.entity.PasswordResetCode;
import com.example.myfschool.entity.User;
import com.example.myfschool.enums.UserStatus;
import com.example.myfschool.exception.BusinessException;
import com.example.myfschool.exception.ErrorCode;
import com.example.myfschool.mapper.UserMapper;
import com.example.myfschool.repository.PasswordResetCodeRepository;
import com.example.myfschool.repository.UserRepository;
import com.example.myfschool.security.JwtService;
import com.example.myfschool.service.AuthService;
import com.example.myfschool.service.EmailService;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final int RESET_CODE_EXPIRATION_MINUTES = 10;
    private static final int RESET_TOKEN_EXPIRATION_MINUTES = 10;
    private static final int RESET_CODE_RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAXIMUM_RESET_CODE_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();
    private String dummyPasswordHash;

    @PostConstruct
    void initializeDummyPasswordHash() {
        dummyPasswordHash = passwordEncoder.encode("not-a-real-user-password");
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByPhoneNumber(request.phoneNumber()).orElse(null);
        if (user == null) {
            // Keep the missing-user path close to the BCrypt cost of a wrong password.
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }

        String token = jwtService.generateToken(user);
        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                userMapper.toResponse(user)
        );
    }

    @Override
    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);

        // Keep this endpoint deliberately generic so callers cannot enumerate accounts.
        if (user == null) {
            return;
        }

        LocalDateTime now = utcNow();
        PasswordResetCode latestRequest = passwordResetCodeRepository
                .findFirstByUserOrderByCreatedAtDesc(user)
                .orElse(null);
        if (latestRequest != null
                && latestRequest.getCreatedAt() != null
                && latestRequest.getCreatedAt().isAfter(now.minusSeconds(RESET_CODE_RESEND_COOLDOWN_SECONDS))) {
            return;
        }

        passwordResetCodeRepository.findAllByUserAndConsumedAtIsNull(user)
                .forEach(resetCode -> resetCode.consume(now));

        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        PasswordResetCode resetCode = PasswordResetCode.builder()
                .user(user)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(now.plusMinutes(RESET_CODE_EXPIRATION_MINUTES))
                .build();
        passwordResetCodeRepository.saveAndFlush(resetCode);

        try {
            emailService.sendPasswordResetCode(user.getEmail(), user.getFullName(), code);
        } catch (RuntimeException exception) {
            // Preserve the same outward response for known and unknown accounts, including
            // delivery failures. The unsent code must never remain usable.
            resetCode.consume(utcNow());
            passwordResetCodeRepository.saveAndFlush(resetCode);
        }
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public PasswordResetVerifyResponse verifyPasswordResetCode(PasswordResetVerifyRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.code(), dummyPasswordHash);
            throw new BusinessException(ErrorCode.INVALID_OR_EXPIRED_RESET_CODE);
        }

        PasswordResetCode resetCode = passwordResetCodeRepository
                .findFirstByUserAndConsumedAtIsNullOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_OR_EXPIRED_RESET_CODE));
        LocalDateTime now = utcNow();

        if (resetCode.getVerifiedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_OR_EXPIRED_RESET_CODE);
        }

        if (!resetCode.getExpiresAt().isAfter(now)
                || resetCode.getFailedAttempts() >= MAXIMUM_RESET_CODE_ATTEMPTS) {
            resetCode.consume(now);
            passwordResetCodeRepository.save(resetCode);
            throw new BusinessException(ErrorCode.INVALID_OR_EXPIRED_RESET_CODE);
        }

        if (!passwordEncoder.matches(request.code(), resetCode.getCodeHash())) {
            resetCode.recordFailedAttempt(now, MAXIMUM_RESET_CODE_ATTEMPTS);
            passwordResetCodeRepository.save(resetCode);
            throw new BusinessException(ErrorCode.INVALID_OR_EXPIRED_RESET_CODE);
        }

        String resetToken = generateResetToken();
        resetCode.markVerified(
                sha256(resetToken),
                now.plusMinutes(RESET_TOKEN_EXPIRATION_MINUTES),
                now
        );
        passwordResetCodeRepository.save(resetCode);
        return new PasswordResetVerifyResponse(resetToken);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORDS_DO_NOT_MATCH);
        }

        PasswordResetCode resetCode = passwordResetCodeRepository
                .findByResetTokenHashAndConsumedAtIsNull(sha256(request.resetToken()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_OR_EXPIRED_RESET_TOKEN));
        LocalDateTime now = utcNow();

        if (resetCode.getVerifiedAt() == null
                || resetCode.getResetTokenExpiresAt() == null
                || !resetCode.getResetTokenExpiresAt().isAfter(now)) {
            resetCode.consume(now);
            passwordResetCodeRepository.save(resetCode);
            throw new BusinessException(ErrorCode.INVALID_OR_EXPIRED_RESET_TOKEN);
        }

        User user = resetCode.getUser();
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        resetCode.consume(now);
        passwordResetCodeRepository.findAllByUserAndConsumedAtIsNull(user)
                .forEach(otherCode -> otherCode.consume(now));
        userRepository.save(user);
    }

    @Override
    public UserResponse getCurrentUser(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return userMapper.toResponse(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private String generateResetToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
