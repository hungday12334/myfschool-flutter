package com.example.myfschool.dto.response;

import java.time.LocalDate;
import java.util.List;

public record WeeklyTimetableResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        List<TimetableDayResponse> days
) {
}
