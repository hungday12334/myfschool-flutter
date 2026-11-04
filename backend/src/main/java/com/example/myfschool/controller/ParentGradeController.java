package com.example.myfschool.controller;

import com.example.myfschool.dto.response.ApiResponse;
import com.example.myfschool.dto.response.GradeReportResponse;
import com.example.myfschool.dto.response.SubjectGradeDetailResponse;
import com.example.myfschool.security.AuthenticatedUserPrincipal;
import com.example.myfschool.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parents/me/grades")
@RequiredArgsConstructor
public class ParentGradeController {

    private final GradeService gradeService;

    @GetMapping
    public ResponseEntity<ApiResponse<GradeReportResponse>> getGradeReport(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(name = "period", defaultValue = "1") String period
    ) {
        GradeReportResponse report = gradeService.getGradeReport(principal.getUserId(), period);
        return ResponseEntity.ok(ApiResponse.success("Grade report retrieved successfully", report));
    }

    @GetMapping("/{subjectCode}")
    public ResponseEntity<ApiResponse<SubjectGradeDetailResponse>> getSubjectGrade(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable String subjectCode,
            @RequestParam(name = "period", defaultValue = "1") String period
    ) {
        SubjectGradeDetailResponse detail = gradeService.getSubjectGrade(
                principal.getUserId(),
                subjectCode,
                period
        );
        return ResponseEntity.ok(ApiResponse.success("Subject grade retrieved successfully", detail));
    }
}
