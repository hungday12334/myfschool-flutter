package com.example.myfschool.dto.response;

import java.util.List;

public record GradeReportResponse(
        StudentSummaryResponse student,
        String academicYear,
        String period,
        List<SubjectGradeSummaryResponse> subjects
) {
}
