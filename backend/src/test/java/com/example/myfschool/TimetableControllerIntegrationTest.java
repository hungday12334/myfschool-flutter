package com.example.myfschool;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.myfschool.entity.TimetableEntry;
import com.example.myfschool.entity.User;
import com.example.myfschool.enums.UserRole;
import com.example.myfschool.enums.UserStatus;
import com.example.myfschool.repository.PasswordResetCodeRepository;
import com.example.myfschool.repository.TimetableEntryRepository;
import com.example.myfschool.repository.UserRepository;
import com.example.myfschool.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class TimetableControllerIntegrationTest {

    private static final String PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TimetableEntryRepository timetableEntryRepository;

    @Autowired
    private PasswordResetCodeRepository passwordResetCodeRepository;

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
    }

    @Test
    void timetableRejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/parents/me/timetable"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void timetableRejectsAuthenticatedNonParent() throws Exception {
        User teacher = createUser("0911111111", UserRole.TEACHER);
        String token = loginAndExtractToken(teacher.getPhoneNumber());

        mockMvc.perform(get("/api/v1/parents/me/timetable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void timetableReturnsSevenDaysSortedAndOnlyForAuthenticatedParent() throws Exception {
        LocalDate monday = LocalDate.of(2026, 10, 26);
        User student = createUser("0987654321", UserRole.PARENT);
        User anotherStudent = createUser("0922222222", UserRole.PARENT);
        saveLesson(student, monday, 2, "ENGLISH", "Tiếng Anh", LocalTime.of(8, 30));
        saveLesson(student, monday, 1, "MATH", "Toán", LocalTime.of(7, 30));
        saveLesson(anotherStudent, monday, 3, "HIDDEN", "Hidden lesson", LocalTime.of(9, 30));
        String token = loginAndExtractToken(student.getPhoneNumber());

        mockMvc.perform(get("/api/v1/parents/me/timetable")
                        .queryParam("weekStart", monday.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Timetable retrieved successfully"))
                .andExpect(jsonPath("$.data.weekStart").value("2026-10-26"))
                .andExpect(jsonPath("$.data.weekEnd").value("2026-11-01"))
                .andExpect(jsonPath("$.data.days", hasSize(7)))
                .andExpect(jsonPath("$.data.days[0].date").value("2026-10-26"))
                .andExpect(jsonPath("$.data.days[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.data.days[0].lessons", hasSize(2)))
                .andExpect(jsonPath("$.data.days[0].lessons[0].periodNumber").value(1))
                .andExpect(jsonPath("$.data.days[0].lessons[0].subjectCode").value("MATH"))
                .andExpect(jsonPath("$.data.days[0].lessons[0].subjectName").value("Toán"))
                .andExpect(jsonPath("$.data.days[0].lessons[0].teacherName").value("Teacher 1"))
                .andExpect(jsonPath("$.data.days[0].lessons[0].room").value("Phòng 101"))
                .andExpect(jsonPath("$.data.days[0].lessons[0].startTime").value("07:30"))
                .andExpect(jsonPath("$.data.days[0].lessons[0].endTime").value("08:15"))
                .andExpect(jsonPath("$.data.days[0].lessons[1].periodNumber").value(2))
                .andExpect(jsonPath("$.data.days[6].date").value("2026-11-01"))
                .andExpect(jsonPath("$.data.days[6].lessons").isEmpty());
    }

    @Test
    void timetableNormalizesMidweekDateToMonday() throws Exception {
        User student = createUser("0987654321", UserRole.PARENT);
        String token = loginAndExtractToken(student.getPhoneNumber());

        mockMvc.perform(get("/api/v1/parents/me/timetable")
                        .queryParam("weekStart", "2026-10-29")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weekStart").value("2026-10-26"))
                .andExpect(jsonPath("$.data.weekEnd").value("2026-11-01"))
                .andExpect(jsonPath("$.data.days", hasSize(7)));
    }

    @Test
    void timetableRejectsMalformedWeekStart() throws Exception {
        User student = createUser("0987654321", UserRole.PARENT);
        String token = loginAndExtractToken(student.getPhoneNumber());

        mockMvc.perform(get("/api/v1/parents/me/timetable")
                        .queryParam("weekStart", "not-a-date")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("Invalid request"));
    }

    @Test
    void timetableReturnsSevenEmptyDaysWhenWeekHasNoLessons() throws Exception {
        User student = createUser("0987654321", UserRole.PARENT);
        String token = loginAndExtractToken(student.getPhoneNumber());

        mockMvc.perform(get("/api/v1/parents/me/timetable")
                        .queryParam("weekStart", "2030-01-07")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days", hasSize(7)))
                .andExpect(jsonPath("$.data.days[0].lessons").isEmpty())
                .andExpect(jsonPath("$.data.days[6].lessons").isEmpty());
    }

    private User createUser(String phoneNumber, UserRole role) {
        return userRepository.saveAndFlush(User.builder()
                .phoneNumber(phoneNumber)
                .password(passwordEncoder.encode(PASSWORD))
                .fullName("Test User")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private void saveLesson(
            User student,
            LocalDate lessonDate,
            int periodNumber,
            String subjectCode,
            String subjectName,
            LocalTime startTime
    ) {
        timetableEntryRepository.saveAndFlush(TimetableEntry.builder()
                .student(student)
                .lessonDate(lessonDate)
                .periodNumber(periodNumber)
                .subjectCode(subjectCode)
                .subjectName(subjectName)
                .teacherName("Teacher " + periodNumber)
                .room("Phòng 10" + periodNumber)
                .startTime(startTime)
                .endTime(startTime.plusMinutes(45))
                .build());
    }

    private String loginAndExtractToken(String phoneNumber) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload(phoneNumber, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("accessToken").asText();
    }

    private record LoginPayload(String phoneNumber, String password) {
    }
}
