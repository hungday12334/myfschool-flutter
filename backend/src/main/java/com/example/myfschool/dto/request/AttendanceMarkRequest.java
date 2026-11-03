package com.example.myfschool.dto.request;

import com.example.myfschool.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AttendanceMarkRequest(
        @NotNull Long studentId,
        @NotNull AttendanceStatus status,
        @Size(max = 500) String note
) { }
