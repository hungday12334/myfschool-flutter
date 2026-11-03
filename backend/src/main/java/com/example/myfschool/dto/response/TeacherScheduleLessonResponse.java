package com.example.myfschool.dto.response;

import java.time.LocalDate;

public record TeacherScheduleLessonResponse(Long id, LocalDate date, int periodNumber,
        String subjectCode, String subjectName, String room, String startTime, String endTime) { }
