package com.example.myfschool;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.myfschool.entity.GradeAssessment;
import com.example.myfschool.entity.StudentProfile;
import com.example.myfschool.entity.User;
import com.example.myfschool.enums.AssessmentType;
import com.example.myfschool.enums.UserRole;
import com.example.myfschool.enums.UserStatus;
import com.example.myfschool.repository.GradeAssessmentRepository;
import com.example.myfschool.repository.PasswordResetCodeRepository;
import com.example.myfschool.repository.StudentProfileRepository;
import com.example.myfschool.repository.TimetableEntryRepository;
import com.example.myfschool.repository.UserRepository;
import com.example.myfschool.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
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
class GradeControllerIntegrationTest {

    private static final String PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private GradeAssessmentRepository gradeAssessmentRepository;

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
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void gradeReportRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/parents/me/grades"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void gradeReportRejectsNonParentRole() throws Exception {
        User teacher = createUser("0911111111", UserRole.TEACHER);
        String token = loginAndExtractToken(teacher.getPhoneNumber());

        mockMvc.perform(get("/api/v1/parents/me/grades")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void gradeReportReturnsOnlyLinkedStudentAndWeightedAverage() throws Exception {
        User parent = createUser("0987654321", UserRole.PARENT);
        User anotherParent = createUser("0922222222", UserRole.PARENT);
        StudentProfile student = createStudent(parent, "HS001", "Nguyễn Minh Anh");
        StudentProfile hiddenStudent = createStudent(anotherParent, "HS002", "Hidden Student");
        saveGrade(student, 1, "MATH", "Toán", AssessmentType.ORAL, "Miệng", 1, "8.0");
        saveGrade(student, 1, "MATH", "Toán", AssessmentType.FIFTEEN_MINUTES, "15 phút", 1, "7.5");
        saveGrade(student, 1, "MATH", "Toán", AssessmentType.ONE_PERIOD, "1 tiết", 2, "9.0");
        saveGrade(student, 1, "MATH", "Toán", AssessmentType.SEMESTER, "Học kỳ", 3, "9.5");
        saveGrade(student, 2, "MATH", "Toán", AssessmentType.ORAL, "Miệng", 1, "10.0");
        saveGrade(hiddenStudent, 1, "HIDDEN", "Hidden", AssessmentType.ORAL, "Miệng", 1, "10.0");
        String token = loginAndExtractToken(parent.getPhoneNumber());

        mockMvc.perform(get("/api/v1/parents/me/grades")
                        .queryParam("period", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.student.studentCode").value("HS001"))
                .andExpect(jsonPath("$.data.student.fullName").value("Nguyễn Minh Anh"))
                .andExpect(jsonPath("$.data.student.className").value("10A1"))
                .andExpect(jsonPath("$.data.period").value("SEMESTER_1"))
                .andExpect(jsonPath("$.data.subjects", hasSize(1)))
                .andExpect(jsonPath("$.data.subjects[0].subjectCode").value("MATH"))
                .andExpect(jsonPath("$.data.subjects[0].averageScore").value(8.9));
    }

    @Test
    void subjectDetailReturnsComponentsAndSupportsFullYear() throws Exception {
        User parent = createUser("0987654321", UserRole.PARENT);
        StudentProfile student = createStudent(parent, "HS001", "Nguyễn Minh Anh");
        saveGrade(student, 1, "MATH", "Toán", AssessmentType.ORAL, "Miệng", 1, "8.0");
        saveGrade(student, 2, "MATH", "Toán", AssessmentType.SEMESTER, "Học kỳ", 3, "9.0");
        String token = loginAndExtractToken(parent.getPhoneNumber());

        mockMvc.perform(get("/api/v1/parents/me/grades/MATH")
                        .queryParam("period", "all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period").value("FULL_YEAR"))
                .andExpect(jsonPath("$.data.subjectName").value("Toán"))
                .andExpect(jsonPath("$.data.averageScore").value(8.8))
                .andExpect(jsonPath("$.data.assessments", hasSize(2)))
                .andExpect(jsonPath("$.data.assessments[0].label").value("Miệng"))
                .andExpect(jsonPath("$.data.assessments[0].semester").value(1))
                .andExpect(jsonPath("$.data.assessments[1].coefficient").value(3));
    }

    @Test
    void gradeReportValidatesPeriodAndMissingStudent() throws Exception {
        User parent = createUser("0987654321", UserRole.PARENT);
        String token = loginAndExtractToken(parent.getPhoneNumber());

        mockMvc.perform(get("/api/v1/parents/me/grades")
                        .queryParam("period", "quarter")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));

        mockMvc.perform(get("/api/v1/parents/me/grades")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
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

    private StudentProfile createStudent(User parent, String code, String fullName) {
        return studentProfileRepository.saveAndFlush(StudentProfile.builder()
                .parent(parent)
                .studentCode(code)
                .fullName(fullName)
                .className("10A1")
                .schoolName("Trường THPT FPT")
                .build());
    }

    private void saveGrade(
            StudentProfile student,
            int semester,
            String subjectCode,
            String subjectName,
            AssessmentType type,
            String label,
            int coefficient,
            String score
    ) {
        gradeAssessmentRepository.saveAndFlush(GradeAssessment.builder()
                .student(student)
                .academicYear("2026-2027")
                .semester(semester)
                .subjectCode(subjectCode)
                .subjectName(subjectName)
                .assessmentType(type)
                .assessmentLabel(label)
                .coefficient(coefficient)
                .score(new BigDecimal(score))
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

    private void cleanDatabase() {
        gradeAssessmentRepository.deleteAll();
        studentProfileRepository.deleteAll();
        passwordResetCodeRepository.deleteAll();
        timetableEntryRepository.deleteAll();
        userRepository.deleteAll();
    }

    private record LoginPayload(String phoneNumber, String password) {
    }
}
