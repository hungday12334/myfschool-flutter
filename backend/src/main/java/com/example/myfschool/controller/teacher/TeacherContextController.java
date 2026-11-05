package com.example.myfschool.controller.teacher;

import com.example.myfschool.dto.response.ApiResponse;
import com.example.myfschool.dto.teacher.TeacherContextDtos.*;
import com.example.myfschool.security.AuthenticatedUserPrincipal;
import com.example.myfschool.service.TeacherService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Read APIs for the authenticated teacher's teaching context. */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherContextController {
    private final TeacherService service;

    @GetMapping("/dashboard")
    public ApiResponse<Dashboard> dashboard(@AuthenticationPrincipal AuthenticatedUserPrincipal p,
            @RequestParam(required = false) Long academicYearId) {
        return ApiResponse.success("Teacher dashboard", service.dashboard(p.getUserId(), academicYearId));
    }

    @GetMapping("/profile")
    public ApiResponse<Profile> profile(@AuthenticationPrincipal AuthenticatedUserPrincipal p) {
        return ApiResponse.success("Teacher profile", service.profile(p.getUserId()));
    }

    @GetMapping("/classes")
    public ApiResponse<List<ClassInfo>> classes(@AuthenticationPrincipal AuthenticatedUserPrincipal p,
            @RequestParam(required = false) Long academicYearId) {
        return ApiResponse.success("Assigned classes", service.classes(p.getUserId(), academicYearId));
    }

    @GetMapping("/classes/{classId}/students")
    public ApiResponse<Page<StudentInfo>> students(@AuthenticationPrincipal AuthenticatedUserPrincipal p,
            @PathVariable Long classId, @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Class students", service.students(p.getUserId(), classId, keyword, page, size));
    }

    @GetMapping("/students/{studentId}")
    public ApiResponse<StudentInfo> student(@AuthenticationPrincipal AuthenticatedUserPrincipal p,
            @PathVariable Long studentId) {
        return ApiResponse.success("Student profile", service.student(p.getUserId(), studentId));
    }

    @GetMapping("/timetable")
    public ApiResponse<List<Lesson>> timetable(@AuthenticationPrincipal AuthenticatedUserPrincipal p,
            @RequestParam(required = false) Long academicYearId, @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) LocalDate date) {
        return ApiResponse.success("Teaching timetable", service.timetable(p.getUserId(), academicYearId, semesterId, date));
    }

    @GetMapping("/subjects")
    public ApiResponse<List<Assignment>> subjects(@AuthenticationPrincipal AuthenticatedUserPrincipal p,
            @RequestParam(required = false) Long academicYearId, @RequestParam(required = false) Long semesterId) {
        return ApiResponse.success("Assigned subjects", service.subjects(p.getUserId(), academicYearId, semesterId));
    }
}
