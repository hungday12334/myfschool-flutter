package com.example.myfschool.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record SubjectGradeDetailResponse(
        StudentSummaryResponse student,
        String academicYear,
        String period,
        String subjectCode,
        String subjectName,
        BigDecimal averageScore,
        List<GradeAssessmentResponse> assessments
) {
}
