package com.example.myfschool.controller;

import com.example.myfschool.dto.response.ApiResponse;
import com.example.myfschool.dto.response.TuitionReportResponse;
import com.example.myfschool.security.AuthenticatedUserPrincipal;
import com.example.myfschool.service.TuitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parents/me/tuition")
@RequiredArgsConstructor
public class ParentTuitionController {

    private final TuitionService tuitionService;

    @GetMapping
    public ResponseEntity<ApiResponse<TuitionReportResponse>> getTuitionReport(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        TuitionReportResponse report = tuitionService.getTuitionReport(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Tuition report retrieved successfully", report));
    }
}
