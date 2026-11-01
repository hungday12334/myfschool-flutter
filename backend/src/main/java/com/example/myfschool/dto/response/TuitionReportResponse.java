package com.example.myfschool.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record TuitionReportResponse(
        StudentSummaryResponse student,
        String academicYear,
        BigDecimal totalAmount,
        BigDecimal totalPaid,
        BigDecimal totalDue,
        List<TuitionItemResponse> paidItems,
        List<TuitionItemResponse> dueItems
) {
}
