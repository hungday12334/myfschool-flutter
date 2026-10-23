package com.example.myfschool;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.myfschool.entity.User;
import com.example.myfschool.enums.UserRole;
import com.example.myfschool.enums.UserStatus;
import com.example.myfschool.repository.UserRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
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
        return userRepository.saveAndFlush(User.builder()
                .phoneNumber(phoneNumber)
                .password(passwordEncoder.encode(PASSWORD))
                .fullName("Test Student")
                .role(UserRole.STUDENT)
                .status(status)
                .build());
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
}
