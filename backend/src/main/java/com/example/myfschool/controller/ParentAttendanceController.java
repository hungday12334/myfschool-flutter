package com.example.myfschool.controller;

import com.example.myfschool.dto.response.ApiResponse;
import com.example.myfschool.dto.response.AttendanceReportResponse;
import com.example.myfschool.security.AuthenticatedUserPrincipal;
import com.example.myfschool.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parents/me/attendance")
@RequiredArgsConstructor
public class ParentAttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    public ResponseEntity<ApiResponse<AttendanceReportResponse>> getMonthlyReport(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(name = "month", required = false) String month
    ) {
        AttendanceReportResponse report = attendanceService.getMonthlyReport(
                principal.getUserId(),
                month
        );
        return ResponseEntity.ok(ApiResponse.success("Attendance report retrieved successfully", report));
    }
}
