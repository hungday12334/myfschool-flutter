package com.example.myfschool.dto.response;

public record TimetableLessonResponse(
        Long id,
        int periodNumber,
        String subjectCode,
        String subjectName,
        String teacherName,
        String room,
        String startTime,
        String endTime
) {
}
