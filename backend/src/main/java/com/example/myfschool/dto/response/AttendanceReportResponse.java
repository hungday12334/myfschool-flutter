package com.example.myfschool.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record AttendanceReportResponse(
        StudentSummaryResponse student,
        String month,
        int totalSchoolDays,
        int presentDays,
        int lateDays,
        int excusedAbsenceDays,
        int unexcusedAbsenceDays,
        BigDecimal attendanceRate,
        List<AttendanceRecordResponse> records
) {
}
