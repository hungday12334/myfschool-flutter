package com.example.myfschool.controller;

import com.example.myfschool.dto.request.AttendanceMarkRequest;
import com.example.myfschool.dto.response.*;
import com.example.myfschool.security.AuthenticatedUserPrincipal;
import com.example.myfschool.service.TeacherService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
@Validated
public class TeacherController {
    private final TeacherService teacherService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> profile(@AuthenticationPrincipal AuthenticatedUserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success("Teacher profile retrieved", teacherService.getProfile(user.getUserId())));
    }

    @GetMapping("/schedule")
    public ResponseEntity<ApiResponse<List<TeacherScheduleLessonResponse>>> schedule(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return ResponseEntity.ok(ApiResponse.success("Teaching schedule retrieved",
                teacherService.getSchedule(user.getUserId(), weekStart)));
    }

    @GetMapping("/classes")
    public ResponseEntity<ApiResponse<List<TeacherClassResponse>>> classes(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success("Assigned classes retrieved", teacherService.getClasses(user.getUserId())));
    }

    @GetMapping("/classes/{assignmentId}/students")
    public ResponseEntity<ApiResponse<List<TeacherStudentResponse>>> students(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user, @PathVariable Long assignmentId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success("Class students retrieved",
                teacherService.getStudents(user.getUserId(), assignmentId, search)));
    }

    @GetMapping("/classes/{assignmentId}/students/{studentId}")
    public ResponseEntity<ApiResponse<TeacherStudentResponse>> student(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user, @PathVariable Long assignmentId,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success("Student details retrieved",
                teacherService.getStudent(user.getUserId(), assignmentId, studentId)));
    }

    @GetMapping("/classes/{assignmentId}/attendance")
    public ResponseEntity<ApiResponse<List<TeacherAttendanceResponse>>> attendance(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user, @PathVariable Long assignmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate selected = date == null ? LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")) : date;
        return ResponseEntity.ok(ApiResponse.success("Class attendance retrieved",
                teacherService.getAttendance(user.getUserId(), assignmentId, selected)));
    }

    @PutMapping("/classes/{assignmentId}/attendance")
    public ResponseEntity<ApiResponse<List<TeacherAttendanceResponse>>> saveAttendance(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user, @PathVariable Long assignmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody @NotEmpty List<@Valid AttendanceMarkRequest> marks) {
        return ResponseEntity.ok(ApiResponse.success("Attendance saved",
                teacherService.saveAttendance(user.getUserId(), assignmentId, date, marks)));
    }
}
