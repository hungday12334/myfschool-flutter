package com.example.myfschool.dto.response;

import java.time.LocalDate;

public record TeacherAttendanceResponse(Long studentId, String studentCode, String fullName,
        String status, String note, LocalDate date) { }
