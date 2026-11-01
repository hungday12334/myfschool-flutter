package com.example.myfschool.service.impl;

import com.example.myfschool.dto.response.GradeAssessmentResponse;
import com.example.myfschool.dto.response.GradeReportResponse;
import com.example.myfschool.dto.response.StudentSummaryResponse;
import com.example.myfschool.dto.response.SubjectGradeDetailResponse;
import com.example.myfschool.dto.response.SubjectGradeSummaryResponse;
import com.example.myfschool.entity.GradeAssessment;
import com.example.myfschool.entity.StudentProfile;
import com.example.myfschool.exception.BusinessException;
import com.example.myfschool.exception.ErrorCode;
import com.example.myfschool.repository.GradeAssessmentRepository;
import com.example.myfschool.repository.StudentProfileRepository;
import com.example.myfschool.service.GradeService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeServiceImpl implements GradeService {

    private final StudentProfileRepository studentProfileRepository;
    private final GradeAssessmentRepository gradeAssessmentRepository;

    @Override
    public GradeReportResponse getGradeReport(Long parentId, String period) {
        Integer semester = parsePeriod(period);
        StudentProfile student = getStudent(parentId);
        List<GradeAssessment> assessments = semester == null
                ? gradeAssessmentRepository.findByStudentIdOrderBySubjectNameAscSemesterAscIdAsc(student.getId())
                : gradeAssessmentRepository.findByStudentIdAndSemesterOrderBySubjectNameAscIdAsc(
                        student.getId(),
                        semester
                );

        Map<String, List<GradeAssessment>> bySubject = new LinkedHashMap<>();
        assessments.forEach(assessment -> bySubject
                .computeIfAbsent(assessment.getSubjectCode(), ignored -> new java.util.ArrayList<>())
                .add(assessment));
        List<SubjectGradeSummaryResponse> subjects = bySubject.values().stream()
                .map(values -> new SubjectGradeSummaryResponse(
                        values.getFirst().getSubjectCode(),
                        values.getFirst().getSubjectName(),
                        weightedAverage(values)
                ))
                .toList();

        return new GradeReportResponse(
                toStudentResponse(student),
                academicYear(assessments),
                periodName(semester),
                subjects
        );
    }

    @Override
    public SubjectGradeDetailResponse getSubjectGrade(
            Long parentId,
            String subjectCode,
            String period
    ) {
        Integer semester = parsePeriod(period);
        StudentProfile student = getStudent(parentId);
        List<GradeAssessment> assessments = semester == null
                ? gradeAssessmentRepository.findByStudentIdAndSubjectCodeIgnoreCaseOrderBySemesterAscIdAsc(
                        student.getId(),
                        subjectCode
                )
                : gradeAssessmentRepository.findByStudentIdAndSubjectCodeIgnoreCaseAndSemesterOrderByIdAsc(
                        student.getId(),
                        subjectCode,
                        semester
                );
        if (assessments.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Grade data not found");
        }

        GradeAssessment first = assessments.getFirst();
        List<GradeAssessmentResponse> components = assessments.stream()
                .map(value -> new GradeAssessmentResponse(
                        value.getId(),
                        value.getAssessmentType().name(),
                        value.getAssessmentLabel(),
                        value.getCoefficient(),
                        value.getScore().setScale(1, RoundingMode.HALF_UP),
                        value.getSemester()
                ))
                .toList();
        return new SubjectGradeDetailResponse(
                toStudentResponse(student),
                academicYear(assessments),
                periodName(semester),
                first.getSubjectCode(),
                first.getSubjectName(),
                weightedAverage(assessments),
                components
        );
    }

    private StudentProfile getStudent(Long parentId) {
        return studentProfileRepository.findFirstByParentIdOrderByIdAsc(parentId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "No student is linked to this parent"
                ));
    }

    private Integer parsePeriod(String rawPeriod) {
        String normalized = rawPeriod == null ? "1" : rawPeriod.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "1", "semester_1" -> 1;
            case "2", "semester_2" -> 2;
            case "all", "year", "full_year" -> null;
            default -> throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Period must be 1, 2 or all"
            );
        };
    }

    private String periodName(Integer semester) {
        return semester == null ? "FULL_YEAR" : "SEMESTER_" + semester;
    }

    private String academicYear(List<GradeAssessment> assessments) {
        return assessments.isEmpty() ? "" : assessments.getFirst().getAcademicYear();
    }

    private StudentSummaryResponse toStudentResponse(StudentProfile student) {
        return new StudentSummaryResponse(
                student.getStudentCode(),
                student.getFullName(),
                student.getClassName(),
                student.getSchoolName()
        );
    }

    private BigDecimal weightedAverage(List<GradeAssessment> assessments) {
        BigDecimal weightedTotal = assessments.stream()
                .map(value -> value.getScore().multiply(BigDecimal.valueOf(value.getCoefficient())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int coefficientTotal = assessments.stream()
                .mapToInt(GradeAssessment::getCoefficient)
                .sum();
        if (coefficientTotal == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return weightedTotal.divide(BigDecimal.valueOf(coefficientTotal), 1, RoundingMode.HALF_UP);
    }
}
