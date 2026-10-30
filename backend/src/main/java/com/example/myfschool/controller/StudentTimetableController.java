package com.example.myfschool.controller;

import com.example.myfschool.dto.response.ApiResponse;
import com.example.myfschool.dto.response.WeeklyTimetableResponse;
import com.example.myfschool.security.AuthenticatedUserPrincipal;
import com.example.myfschool.service.TimetableService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students/me/timetable")
@RequiredArgsConstructor
public class StudentTimetableController {

    private final TimetableService timetableService;

    @GetMapping
    public ResponseEntity<ApiResponse<WeeklyTimetableResponse>> getWeeklyTimetable(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(name = "weekStart", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStart
    ) {
        WeeklyTimetableResponse timetable = timetableService.getWeeklyTimetable(
                principal.getUserId(),
                weekStart
        );
        return ResponseEntity.ok(ApiResponse.success("Timetable retrieved successfully", timetable));
    }
}
