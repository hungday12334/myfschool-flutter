package com.example.myfschool.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TuitionItemResponse(
        Long id,
        String feeCode,
        String title,
        String description,
        String academicYear,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        LocalDate paidDate,
        String status
) {
}
