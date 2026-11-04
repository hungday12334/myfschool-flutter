package com.example.myfschool;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.myfschool.entity.*;
import com.example.myfschool.enums.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherControllerIntegrationTest {
    private static final String PASSWORD = "123456";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired StudentProfileRepository students;
    @Autowired TeacherClassAssignmentRepository assignments;
    @Autowired AttendanceRecordRepository attendance;
    @Autowired PasswordResetCodeRepository resetCodes;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper mapper;
    @MockBean EmailService emailService;

    @BeforeEach
    void setUp() {
        attendance.deleteAll();
        assignments.deleteAll();
        students.deleteAll();
        resetCodes.deleteAll();
        users.deleteAll();
    }

    @Test
    void teacherCanReadOwnAssignedRosterAndMarkAttendance() throws Exception {
        User teacher = user("0911111111", UserRole.TEACHER);
        User parent = user("0922222222", UserRole.PARENT);
        StudentProfile student = student(parent, "S001", "Class 10A", "MyFSchool");
        TeacherClassAssignment assignment = assignments.saveAndFlush(TeacherClassAssignment.builder()
                .teacher(teacher).className("Class 10A").schoolName("MyFSchool").build());
        String token = token(teacher.getPhoneNumber());

        mockMvc.perform(get("/api/v1/teacher/classes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].studentCount").value(1));

        mockMvc.perform(put("/api/v1/teacher/classes/{id}/attendance", assignment.getId())
                        .queryParam("date", "2026-11-03")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"studentId\":" + student.getId() + ",\"status\":\"PRESENT\"}]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].status").value("PRESENT"));
    }

    @Test
    void teacherCannotReadOrMarkStudentOutsideAssignedClass() throws Exception {
        User teacher = user("0911111111", UserRole.TEACHER);
        User parent = user("0922222222", UserRole.PARENT);
        StudentProfile student = student(parent, "S002", "Class 10B", "MyFSchool");
        TeacherClassAssignment assignment = assignments.saveAndFlush(TeacherClassAssignment.builder()
                .teacher(teacher).className("Class 10A").schoolName("MyFSchool").build());
        String token = token(teacher.getPhoneNumber());

        mockMvc.perform(get("/api/v1/teacher/classes/{id}/students/{studentId}", assignment.getId(), student.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/teacher/classes/{id}/attendance", assignment.getId())
                        .queryParam("date", "2026-11-03").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"studentId\":" + student.getId() + ",\"status\":\"ABSENT\"}]"))
                .andExpect(status().isForbidden());
    }

    @Test
    void parentCannotCallTeacherApiAndPrincipalCanAssignExistingClass() throws Exception {
        User teacher = user("0911111111", UserRole.TEACHER);
        User principal = user("0933333333", UserRole.PRINCIPAL);
        User parent = user("0922222222", UserRole.PARENT);
        student(parent, "S003", "Class 10C", "MyFSchool");

        mockMvc.perform(get("/api/v1/teacher/me").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + token(parent.getPhoneNumber())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/principal/teaching-assignments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(principal.getPhoneNumber()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teacherId\":" + teacher.getId()
                                + ",\"className\":\"Class 10C\",\"schoolName\":\"MyFSchool\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.className").value("Class 10C"))
                .andExpect(jsonPath("$.data.studentCount").value(1));
    }

    private User user(String phone, UserRole role) {
        return users.saveAndFlush(User.builder().phoneNumber(phone).password(encoder.encode(PASSWORD))
                .fullName("Test " + role).role(role).status(UserStatus.ACTIVE).build());
    }

    private StudentProfile student(User parent, String code, String className, String school) {
        return students.saveAndFlush(StudentProfile.builder().parent(parent).studentCode(code)
                .fullName("Student " + code).className(className).schoolName(school).build());
    }

    private String token(String phone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginPayload(phone, PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        JsonNode response = mapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("accessToken").asText();
    }

    private record LoginPayload(String phoneNumber, String password) { }
}
