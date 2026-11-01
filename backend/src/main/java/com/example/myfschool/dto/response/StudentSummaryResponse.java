package com.example.myfschool.dto.response;

public record StudentSummaryResponse(
        String studentCode,
        String fullName,
        String className,
        String schoolName
) {
}
