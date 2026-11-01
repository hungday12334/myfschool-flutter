package com.example.myfschool.service.impl;

import com.example.myfschool.dto.response.StudentSummaryResponse;
import com.example.myfschool.dto.response.TuitionItemResponse;
import com.example.myfschool.dto.response.TuitionReportResponse;
import com.example.myfschool.entity.StudentProfile;
import com.example.myfschool.entity.TuitionFee;
import com.example.myfschool.exception.BusinessException;
import com.example.myfschool.exception.ErrorCode;
import com.example.myfschool.repository.StudentProfileRepository;
import com.example.myfschool.repository.TuitionFeeRepository;
import com.example.myfschool.service.TuitionService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TuitionServiceImpl implements TuitionService {

    private final StudentProfileRepository studentProfileRepository;
    private final TuitionFeeRepository tuitionFeeRepository;

    @Override
    public TuitionReportResponse getTuitionReport(Long parentId) {
        StudentProfile student = studentProfileRepository.findFirstByParentIdOrderByIdAsc(parentId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "No student is linked to this parent"
                ));
        List<TuitionFee> fees = tuitionFeeRepository.findByStudentIdOrderByDueDateDescIdDesc(student.getId());
        List<TuitionItemResponse> items = fees.stream().map(this::toItemResponse).toList();
        List<TuitionItemResponse> paidItems = items.stream()
                .filter(item -> item.status().equals("PAID"))
                .toList();
        List<TuitionItemResponse> dueItems = items.stream()
                .filter(item -> !item.status().equals("PAID"))
                .toList();
        BigDecimal totalAmount = fees.stream()
                .map(TuitionFee::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = items.stream()
                .map(TuitionItemResponse::paidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDue = items.stream()
                .map(TuitionItemResponse::remainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TuitionReportResponse(
                toStudentResponse(student),
                fees.isEmpty() ? "" : fees.getFirst().getAcademicYear(),
                totalAmount,
                totalPaid,
                totalDue,
                paidItems,
                dueItems
        );
    }

    private TuitionItemResponse toItemResponse(TuitionFee fee) {
        BigDecimal paidAmount = fee.getPaidAmount().min(fee.getAmount()).max(BigDecimal.ZERO);
        BigDecimal remaining = fee.getAmount().subtract(paidAmount).max(BigDecimal.ZERO);
        String status = remaining.signum() == 0
                ? "PAID"
                : paidAmount.signum() > 0 ? "PARTIALLY_PAID" : "DUE";
        return new TuitionItemResponse(
                fee.getId(),
                fee.getFeeCode(),
                fee.getTitle(),
                fee.getDescription(),
                fee.getAcademicYear(),
                fee.getDueDate(),
                fee.getAmount(),
                paidAmount,
                remaining,
                fee.getPaidDate(),
                status
        );
    }

    private StudentSummaryResponse toStudentResponse(StudentProfile student) {
        return new StudentSummaryResponse(
                student.getStudentCode(),
                student.getFullName(),
                student.getClassName(),
                student.getSchoolName()
        );
    }
}
