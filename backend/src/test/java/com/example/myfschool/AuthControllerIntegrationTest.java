package com.example.myfschool;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.myfschool.entity.User;
import com.example.myfschool.enums.UserRole;
import com.example.myfschool.enums.UserStatus;
import com.example.myfschool.repository.PasswordResetCodeRepository;
import com.example.myfschool.repository.TimetableEntryRepository;
import com.example.myfschool.repository.UserRepository;
import com.example.myfschool.exception.BusinessException;
import com.example.myfschool.exception.ErrorCode;
import com.example.myfschool.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    private static final String TEST_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String TEST_ISSUER = "myfschool-api-test";
    private static final String PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetCodeRepository passwordResetCodeRepository;

    @Autowired
    private TimetableEntryRepository timetableEntryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        passwordResetCodeRepository.deleteAll();
        timetableEntryRepository.deleteAll();
        userRepository.deleteAll();
        reset(emailService);
    }

    @Test
    void loginReturnsTokenAndSafeUserForCorrectCredentials() throws Exception {
        User user = createUser("0987654321", UserStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(user.getPhoneNumber(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.user.id").value(user.getId()))
                .andExpect(jsonPath("$.data.user.phoneNumber").value(user.getPhoneNumber()))
                .andExpect(jsonPath("$.data.user.fullName").value("Test Student"))
                .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist());
    }

    @Test
    void loginRejectsIncorrectPasswordWithGenericMessage() throws Exception {
        createUser("0987654321", UserStatus.ACTIVE);

        expectInvalidCredentials("0987654321", "wrong-password");
    }

    @Test
    void loginRejectsNonexistentPhoneWithSameGenericMessage() throws Exception {
        expectInvalidCredentials("0999999999", PASSWORD);
    }

    @Test
    void wrongPasswordDoesNotRevealLockedAccount() throws Exception {
        createUser("0987654321", UserStatus.LOCKED);

        expectInvalidCredentials("0987654321", "wrong-password");
    }

    @Test
    void loginRejectsInactiveAccountAfterValidCredentials() throws Exception {
        createUser("0987654321", UserStatus.INACTIVE);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("0987654321", PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40302))
                .andExpect(jsonPath("$.message").value("User account is inactive"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void loginRejectsLockedAccountAfterValidCredentials() throws Exception {
        createUser("0987654321", UserStatus.LOCKED);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("0987654321", PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301))
                .andExpect(jsonPath("$.message").value("User account is locked"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void loginRejectsMissingFields() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("Invalid request"))
                .andExpect(jsonPath("$.data.phoneNumber").exists())
                .andExpect(jsonPath("$.data.password").value("Password is required"));
    }

    @Test
    void loginRejectsPhoneOutsideAgreedFormat() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("+84987654321", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.data.phoneNumber")
                        .value("Phone number must be 10 digits and start with 0"));
    }

    @Test
    void passwordResetRequestIsGenericForUnknownEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailJson("unknown@myfschool.local")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message")
                        .value("If an account exists for this email, a reset code has been sent"))
                .andExpect(jsonPath("$.data").isEmpty());

        verifyNoInteractions(emailService);
    }

    @Test
    void passwordResetRequestStaysGenericAndInvalidatesCodeWhenEmailDeliveryFails() throws Exception {
        String email = "student@myfschool.local";
        User user = createUser("0987654321", UserStatus.ACTIVE, email);
        doThrow(new BusinessException(ErrorCode.EMAIL_DELIVERY_FAILED))
                .when(emailService)
                .sendPasswordResetCode(eq(email), eq("Test Student"), anyString());

        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailJson(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message")
                        .value("If an account exists for this email, a reset code has been sent"))
                .andExpect(jsonPath("$.data").isEmpty());

        var failedDeliveryCode = passwordResetCodeRepository
                .findFirstByUserOrderByCreatedAtDesc(user)
                .orElseThrow();
        assertNotNull(failedDeliveryCode.getConsumedAt());

        mockMvc.perform(post("/api/v1/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VerifyResetPayload(email, "000000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40002));
    }

    @Test
    void passwordResetCodeAndTokenAreOneTimeAndNewPasswordCanLogin() throws Exception {
        String email = "student@myfschool.local";
        User user = createUser("0987654321", UserStatus.ACTIVE, email);
        String code = requestResetAndCaptureCode(email);

        MvcResult verification = mockMvc.perform(post("/api/v1/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyResetPayload(email, code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.resetToken").isNotEmpty())
                .andReturn();
        String resetToken = objectMapper.readTree(verification.getResponse().getContentAsString())
                .path("data")
                .path("resetToken")
                .asText();

        mockMvc.perform(post("/api/v1/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyResetPayload(email, code))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40002));

        String newPassword = "654321Aa";
        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ConfirmResetPayload(resetToken, newPassword, newPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Password reset successfully"));

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ConfirmResetPayload(resetToken, newPassword, newPassword))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40003));

        expectInvalidCredentials(user.getPhoneNumber(), PASSWORD);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(user.getPhoneNumber(), newPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void passwordResetRejectsInvalidCodeAndMismatchedPasswords() throws Exception {
        String email = "student@myfschool.local";
        createUser("0987654321", UserStatus.ACTIVE, email);
        String validCode = requestResetAndCaptureCode(email);
        String invalidCode = validCode.equals("000000") ? "000001" : "000000";

        mockMvc.perform(post("/api/v1/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyResetPayload(email, invalidCode))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40002))
                .andExpect(jsonPath("$.message").value("Invalid or expired reset code"));

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ConfirmResetPayload("unused-token", "654321Aa", "different"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40004))
                .andExpect(jsonPath("$.message").value("Passwords do not match"));
    }

    @Test
    void passwordResetValidatesEmailAndCodeFormats() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailJson("not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.data.email").value("Email must be valid"));

        mockMvc.perform(post("/api/v1/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VerifyResetPayload("student@myfschool.local", "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.data.code").value("Reset code must contain exactly 6 digits"));
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void protectedEndpointRejectsInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40102))
                .andExpect(jsonPath("$.message").value("Invalid or expired access token"));
    }

    @Test
    void protectedEndpointRejectsExpiredToken() throws Exception {
        User user = createUser("0987654321", UserStatus.ACTIVE);
        String expiredToken = expiredTokenFor(user);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40102))
                .andExpect(jsonPath("$.message").value("Invalid or expired access token"));
    }

    @Test
    void protectedEndpointRejectsTokenForPreviousAccountWithReusedPhone() throws Exception {
        User previousUser = createUser("0987654321", UserStatus.ACTIVE);
        String previousToken = loginAndExtractToken(previousUser.getPhoneNumber());
        userRepository.delete(previousUser);
        userRepository.flush();
        createUser("0987654321", UserStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + previousToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40102))
                .andExpect(jsonPath("$.message").value("Invalid or expired access token"));
    }

    @Test
    void meReturnsAuthenticatedUserForValidToken() throws Exception {
        User user = createUser("0987654321", UserStatus.ACTIVE);
        String accessToken = loginAndExtractToken(user.getPhoneNumber());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.phoneNumber").value(user.getPhoneNumber()))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    private User createUser(String phoneNumber, UserStatus status) {
        return createUser(phoneNumber, status, null);
    }

    private User createUser(String phoneNumber, UserStatus status, String email) {
        return userRepository.saveAndFlush(User.builder()
                .phoneNumber(phoneNumber)
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .fullName("Test Student")
                .role(UserRole.STUDENT)
                .status(status)
                .build());
    }

    private String requestResetAndCaptureCode(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailJson(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("If an account exists for this email, a reset code has been sent"));

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetCode(eq(email), eq("Test Student"), codeCaptor.capture());
        return codeCaptor.getValue();
    }

    private String emailJson(String email) throws Exception {
        return objectMapper.writeValueAsString(new EmailPayload(email));
    }

    private void expectInvalidCredentials(String phoneNumber, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(phoneNumber, password)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("Invalid phone number or password"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    private String loginAndExtractToken(String phoneNumber) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(phoneNumber, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("accessToken").asText();
    }

    private String expiredTokenFor(User user) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getPhoneNumber())
                .issuer(TEST_ISSUER)
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private String loginJson(String phoneNumber, String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginPayload(phoneNumber, password));
    }

    private record LoginPayload(String phoneNumber, String password) {
    }

    private record EmailPayload(String email) {
    }

    private record VerifyResetPayload(String email, String code) {
    }

    private record ConfirmResetPayload(String resetToken, String newPassword, String confirmPassword) {
    }
}
