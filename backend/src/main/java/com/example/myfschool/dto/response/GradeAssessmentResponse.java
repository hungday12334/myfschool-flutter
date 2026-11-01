package com.example.myfschool.dto.response;

import java.math.BigDecimal;

public record GradeAssessmentResponse(
        Long id,
        String type,
        String label,
        int coefficient,
        BigDecimal score,
        int semester
) {
}
