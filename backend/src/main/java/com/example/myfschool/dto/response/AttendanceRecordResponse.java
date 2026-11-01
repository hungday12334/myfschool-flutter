package com.example.myfschool.dto.response;

import java.time.LocalDate;

public record AttendanceRecordResponse(
        Long id,
        LocalDate date,
        String status,
        String checkInTime,
        String note
) {
}
