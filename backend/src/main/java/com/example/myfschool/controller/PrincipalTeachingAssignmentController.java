package com.example.myfschool.controller;

import com.example.myfschool.dto.request.CreateTeacherClassAssignmentRequest;
import com.example.myfschool.dto.response.ApiResponse;
import com.example.myfschool.dto.response.TeacherClassResponse;
import com.example.myfschool.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/principal/teaching-assignments")
@RequiredArgsConstructor
public class PrincipalTeachingAssignmentController {
    private final TeacherService teacherService;

    @PostMapping
    public ResponseEntity<ApiResponse<TeacherClassResponse>> assignClass(
            @Valid @RequestBody CreateTeacherClassAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Class assigned to teacher", teacherService.assignClass(request)));
    }
}
