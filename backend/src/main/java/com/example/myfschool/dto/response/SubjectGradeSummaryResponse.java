package com.example.myfschool.dto.response;

import java.math.BigDecimal;

public record SubjectGradeSummaryResponse(
        String subjectCode,
        String subjectName,
        BigDecimal averageScore
) {
}
