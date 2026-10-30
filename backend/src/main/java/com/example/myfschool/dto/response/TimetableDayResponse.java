package com.example.myfschool.dto.response;

import java.time.LocalDate;
import java.util.List;

public record TimetableDayResponse(
        LocalDate date,
        String dayOfWeek,
        List<TimetableLessonResponse> lessons
) {
}
