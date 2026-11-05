package com.example.myfschool.dto.teacher;

import com.example.myfschool.enums.GradeType;
import com.example.myfschool.enums.RemarkVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Academic contracts use stored scores; no inferred averages or classifications. */
public final class TeacherGradeDtos {
    private TeacherGradeDtos() {}

    public record CreateGradeItem(@NotNull @Positive Long teachingAssignmentId,
            @NotBlank @Size(max = 200) String title, @NotNull GradeType gradeType,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 4, fraction = 2) BigDecimal weight,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 4, fraction = 2) BigDecimal maxScore,
            LocalDate assessmentDate) {}
    public record GradeEntry(@NotNull @Positive Long studentId,
            @DecimalMin("0") @Digits(integer = 4, fraction = 2) BigDecimal score,
            @Size(max = 10000) String comment) {}
    public record EnterGrades(@NotNull @Positive Long gradeItemId,
            @NotEmpty @Size(max = 200) List<@NotNull @Valid GradeEntry> grades) {}
    public record EditGrade(@DecimalMin("0") @Digits(integer = 4, fraction = 2) BigDecimal score,
            @Size(max = 10000) String comment) {}
    public record EvaluationRequest(@NotNull @Positive Long studentId, @NotNull @Positive Long classId,
            @NotNull @Positive Long semesterId, @Size(max = 50) String academicResult,
            @Size(max = 50) String conductResult, @Size(max = 10000) String overallComment) {}
    public record RemarkRequest(@NotNull @Positive Long studentId, @NotNull @Positive Long classId,
            @NotNull @Positive Long semesterId, @Positive Long teachingAssignmentId,
            @NotBlank @Size(max = 10000) String content, @NotNull LocalDate remarkDate,
            @NotNull RemarkVisibility visibility) {}

    public record NamedId(Long id, String name) {}
    public record GradeItemResponse(Long id, Long teachingAssignmentId, Long classId, NamedId subject,
            NamedId semester, String title, GradeType gradeType, BigDecimal weight, BigDecimal maxScore,
            LocalDate assessmentDate) {}
    public record GradeResponse(Long id, Long gradeItemId, Long studentId, BigDecimal score,
            String comment, LocalDateTime gradedAt) {}
    public record GradebookStudent(Long studentId, String studentCode, String fullName, List<GradeResponse> grades) {}
    public record GradebookResponse(Long classId, String className, NamedId subject, NamedId semester,
            List<GradeItemResponse> gradeItems, List<GradebookStudent> students) {}
    public record EvaluationResponse(Long id, Long studentId, Long classId, Long semesterId,
            String academicResult, String conductResult, String overallComment, Long evaluatedBy,
            LocalDateTime evaluatedAt) {}
    public record RemarkResponse(Long id, Long studentId, Long classId, Long semesterId, Long teacherId,
            String teacherName, String content, LocalDate remarkDate, RemarkVisibility visibility) {}
    public record SubjectResult(NamedId subject, List<GradeItemResponse> gradeItems, List<GradeResponse> grades) {}
    public record StudentAcademicSummary(Long studentId, String studentCode, String fullName,
            List<SubjectResult> subjects, EvaluationResponse evaluation) {}
    public record AcademicSummary(Long classId, String className, NamedId semester,
            List<StudentAcademicSummary> content, int page, int size, long totalElements, long totalPages) {}
}
