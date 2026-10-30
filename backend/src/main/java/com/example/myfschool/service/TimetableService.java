package com.example.myfschool.service;

import com.example.myfschool.dto.response.WeeklyTimetableResponse;
import java.time.LocalDate;

public interface TimetableService {

    WeeklyTimetableResponse getWeeklyTimetable(Long studentId, LocalDate requestedDate);
}
