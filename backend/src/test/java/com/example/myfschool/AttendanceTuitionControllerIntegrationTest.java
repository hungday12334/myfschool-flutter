package com.example.myfschool;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.myfschool.entity.AttendanceRecord;
import com.example.myfschool.entity.StudentProfile;
import com.example.myfschool.entity.TuitionFee;
import com.example.myfschool.entity.User;
import com.example.myfschool.enums.AttendanceStatus;
import com.example.myfschool.enums.UserRole;
import com.example.myfschool.enums.UserStatus;
import com.example.myfschool.repository.AttendanceRecordRepository;
import com.example.myfschool.repository.GradeAssessmentRepository;
import com.example.myfschool.repository.PasswordResetCodeRepository;
import com.example.myfschool.repository.StudentProfileRepository;
import com.example.myfschool.repository.TimetableEntryRepository;
import com.example.myfschool.repository.TuitionFeeRepository;
import com.example.myfschool.repository.UserRepository;
import com.example.myfschool.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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
class AttendanceTuitionControllerIntegrationTest {

    private static final String PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentProfileRepository studentProfileRepository;
    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;
    @Autowired
    private TuitionFeeRepository tuitionFeeRepository;
    @Autowired
    private GradeAssessmentRepository gradeAssessmentRepository;
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
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void attendanceRequiresParentAuthenticationAndValidMonth() throws Exception {
        mockMvc.perform(get("/api/v1/parents/me/attendance"))
                .andExpect(status().isUnauthorized());

        User teacher = createUser("0911111111", UserRole.TEACHER);
        String teacherToken = loginAndExtractToken(teacher.getPhoneNumber());
        mockMvc.perform(get("/api/v1/parents/me/attendance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isForbidden());

        User parent = createUser("0987654321", UserRole.PARENT);
        String parentToken = loginAndExtractToken(parent.getPhoneNumber());
        mockMvc.perform(get("/api/v1/parents/me/attendance")
                        .queryParam("month", "11-2026")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void attendanceReturnsMonthlySummaryAndDescendingRecords() throws Exception {
        User parent = createUser("0987654321", UserRole.PARENT);
        StudentProfile student = createStudent(parent);
        saveAttendance(student, "2026-11-03", AttendanceStatus.PRESENT, "07:25");
        saveAttendance(student, "2026-11-04", AttendanceStatus.LATE, "07:48");
        saveAttendance(student, "2026-11-05", AttendanceStatus.EXCUSED_ABSENCE, null);
        saveAttendance(student, "2026-11-06", AttendanceStatus.UNEXCUSED_ABSENCE, null);
        saveAttendance(student, "2026-10-30", AttendanceStatus.PRESENT, "07:24");
        String token = loginAndExtractToken(parent.getPhoneNumber());

        mockMvc.perform(get("/api/v1/parents/me/attendance")
                        .queryParam("month", "2026-11")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.student.fullName").value("Nguyễn Minh Anh"))
                .andExpect(jsonPath("$.data.month").value("2026-11"))
                .andExpect(jsonPath("$.data.totalSchoolDays").value(4))
                .andExpect(jsonPath("$.data.presentDays").value(1))
                .andExpect(jsonPath("$.data.lateDays").value(1))
                .andExpect(jsonPath("$.data.excusedAbsenceDays").value(1))
                .andExpect(jsonPath("$.data.unexcusedAbsenceDays").value(1))
                .andExpect(jsonPath("$.data.attendanceRate").value(50.0))
                .andExpect(jsonPath("$.data.records", hasSize(4)))
                .andExpect(jsonPath("$.data.records[0].date").value("2026-11-06"))
                .andExpect(jsonPath("$.data.records[3].checkInTime").value("07:25"));
    }

    @Test
    void tuitionReturnsPaidDueAndPartiallyPaidItemsWithTotals() throws Exception {
        User parent = createUser("0987654321", UserRole.PARENT);
        StudentProfile student = createStudent(parent);
        saveFee(student, "PAID", 100_000, 100_000, LocalDate.of(2026, 9, 10));
        saveFee(student, "PARTIAL", 100_000, 40_000, LocalDate.of(2026, 10, 10));
        saveFee(student, "DUE", 200_000, 0, null);
        String token = loginAndExtractToken(parent.getPhoneNumber());

        mockMvc.perform(get("/api/v1/parents/me/tuition")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(400000.0))
                .andExpect(jsonPath("$.data.totalPaid").value(140000.0))
                .andExpect(jsonPath("$.data.totalDue").value(260000.0))
                .andExpect(jsonPath("$.data.paidItems", hasSize(1)))
                .andExpect(jsonPath("$.data.paidItems[0].status").value("PAID"))
                .andExpect(jsonPath("$.data.dueItems", hasSize(2)))
                .andExpect(jsonPath("$.data.dueItems[0].status").value("DUE"))
                .andExpect(jsonPath("$.data.dueItems[1].status").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.data.dueItems[1].remainingAmount").value(60000.0));
    }

    @Test
    void tuitionRejectsNonParentAndMissingLinkedStudent() throws Exception {
        User teacher = createUser("0911111111", UserRole.TEACHER);
        String teacherToken = loginAndExtractToken(teacher.getPhoneNumber());
        mockMvc.perform(get("/api/v1/parents/me/tuition")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isForbidden());

        User parent = createUser("0987654321", UserRole.PARENT);
        String parentToken = loginAndExtractToken(parent.getPhoneNumber());
        mockMvc.perform(get("/api/v1/parents/me/tuition")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    private User createUser(String phone, UserRole role) {
        return userRepository.saveAndFlush(User.builder()
                .phoneNumber(phone)
                .password(passwordEncoder.encode(PASSWORD))
                .fullName("Test User")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private StudentProfile createStudent(User parent) {
        return studentProfileRepository.saveAndFlush(StudentProfile.builder()
                .parent(parent)
                .studentCode("HS001")
                .fullName("Nguyễn Minh Anh")
                .className("10A1")
                .schoolName("Trường THPT FPT")
                .build());
    }

    private void saveAttendance(
            StudentProfile student,
            String date,
            AttendanceStatus status,
            String checkInTime
    ) {
        attendanceRecordRepository.saveAndFlush(AttendanceRecord.builder()
                .student(student)
                .attendanceDate(LocalDate.parse(date))
                .status(status)
                .checkInTime(checkInTime == null ? null : LocalTime.parse(checkInTime))
                .note(status == AttendanceStatus.PRESENT ? null : "Test note")
                .build());
    }

    private void saveFee(
            StudentProfile student,
            String code,
            long amount,
            long paidAmount,
            LocalDate paidDate
    ) {
        tuitionFeeRepository.saveAndFlush(TuitionFee.builder()
                .student(student)
                .feeCode(code)
                .title("Fee " + code)
                .description("Test fee")
                .academicYear("2026-2027")
                .dueDate(switch (code) {
                    case "PAID" -> LocalDate.of(2026, 9, 15);
                    case "PARTIAL" -> LocalDate.of(2026, 10, 15);
                    default -> LocalDate.of(2026, 11, 15);
                })
                .amount(BigDecimal.valueOf(amount))
                .paidAmount(BigDecimal.valueOf(paidAmount))
                .paidDate(paidDate)
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
        attendanceRecordRepository.deleteAll();
        tuitionFeeRepository.deleteAll();
        gradeAssessmentRepository.deleteAll();
        studentProfileRepository.deleteAll();
        passwordResetCodeRepository.deleteAll();
        timetableEntryRepository.deleteAll();
        userRepository.deleteAll();
    }

    private record LoginPayload(String phoneNumber, String password) {
    }
}
