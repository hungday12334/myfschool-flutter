package com.example.myfschool.controller.teacher;

import static com.example.myfschool.dto.teacher.TeacherGradeDtos.*;
import com.example.myfschool.dto.response.ApiResponse;
import com.example.myfschool.security.AuthenticatedUserPrincipal;
import com.example.myfschool.service.teacher.TeacherGradeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher")
public class TeacherGradeController {
    private final TeacherGradeService service;
    public TeacherGradeController(TeacherGradeService service) { this.service = service; }

    @PostMapping("/grade-items")
    public ApiResponse<GradeItemResponse> createItem(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestBody @Valid CreateGradeItem request) {
        return ApiResponse.success("Grade item created", service.createItem(principal.getUserId(), request));
    }
    @PostMapping("/grades")
    public ApiResponse<List<GradeResponse>> enterGrades(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestBody @Valid EnterGrades request) {
        return ApiResponse.success("Grades saved", service.enterGrades(principal.getUserId(), request));
    }
    @PutMapping("/grades/{gradeId}")
    public ApiResponse<GradeResponse> editGrade(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long gradeId, @RequestBody @Valid EditGrade request) {
        return ApiResponse.success("Grade updated", service.editGrade(principal.getUserId(), gradeId, request));
    }
    @GetMapping("/classes/{classId}/subjects/{subjectId}/grades")
    public ApiResponse<GradebookResponse> gradebook(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long classId, @PathVariable Long subjectId, @RequestParam(required = false) Long semesterId) {
        return ApiResponse.success("Subject gradebook", service.gradebook(principal.getUserId(), classId, subjectId, semesterId));
    }
    @PostMapping("/evaluations")
    public ApiResponse<EvaluationResponse> evaluate(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestBody @Valid EvaluationRequest request) {
        return ApiResponse.success("Evaluation created", service.evaluate(principal.getUserId(), request));
    }
    @PostMapping("/remarks")
    public ApiResponse<RemarkResponse> remark(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestBody @Valid RemarkRequest request) {
        return ApiResponse.success("Remark created", service.remark(principal.getUserId(), request));
    }
    @GetMapping("/homeroom/academic-summary")
    public ApiResponse<AcademicSummary> homeroomSummary(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(required = false) Long classId, @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long semesterId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Homeroom academic summary", service.homeroomSummary(principal.getUserId(), classId, academicYearId, semesterId, page, size));
    }
}
