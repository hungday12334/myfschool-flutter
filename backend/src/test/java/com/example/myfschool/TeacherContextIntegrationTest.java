package com.example.myfschool;

import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TeacherContextIntegrationTest extends TeacherIntegrationSupport {
    @Test void emptyAttendanceAndCrossSchoolScope() throws Exception {
        String auth = bearer(teacherUser,"TEACHER");
        mvc.perform(get("/api/teacher/classes/{id}/attendance/summary",schoolClass.getId()).header("Authorization",auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalRecords").value(0))
                .andExpect(jsonPath("$.data.totalSessions").value(0));
        mvc.perform(get("/api/teacher/students/{id}/parents",student.getId())
                .header("Authorization",bearer(otherUser,"TEACHER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/teacher/homeroom/classes").param("academicYearId",year.getId().toString())
                .header("Authorization",bearer(otherUser,"TEACHER"))).andExpect(status().isForbidden());
    }
    @Test void homeroomContactsAndActivityUseAuthenticatedScope() throws Exception {
        String auth = bearer(teacherUser,"TEACHER");
        mvc.perform(get("/api/teacher/homeroom/classes").header("Authorization",auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(schoolClass.getId()));
        mvc.perform(get("/api/teacher/students/{id}/parents",student.getId()).header("Authorization",auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].fullName").exists());
        mvc.perform(get("/api/teacher/activities").header("Authorization",auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.attendanceSessionsCreated").value(0));
        mvc.perform(get("/api/teacher/students/{id}/parents",student.getId())
                .header("Authorization",bearer(subjectUser,"TEACHER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/teacher/activities").param("from","2026-02-02").param("to","2026-01-01")
                .header("Authorization",auth)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/teacher/homeroom/classes").header("Authorization",bearer(parentUser,"PARENT")))
                .andExpect(status().isForbidden());
    }
}
