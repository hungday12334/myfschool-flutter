package com.example.myfschool.controller.teacher;

import com.example.myfschool.dto.response.ApiResponse;
import com.example.myfschool.dto.teacher.TeacherAttendanceDtos.*;
import com.example.myfschool.security.AuthenticatedUserPrincipal;
import com.example.myfschool.service.teacher.TeacherAttendanceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherAttendanceController {
    private final TeacherAttendanceService service;

    @PostMapping("/attendance/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SessionDetail> create(@AuthenticationPrincipal AuthenticatedUserPrincipal p,
            @Valid @RequestBody CreateSession request) {
        return ApiResponse.success("Đã tạo phiên điểm danh", service.create(p.getUserId(), request));
    }

    @GetMapping("/attendance/sessions/{sessionId}")
    public ApiResponse<SessionDetail> session(@AuthenticationPrincipal AuthenticatedUserPrincipal p,
            @PathVariable Long sessionId) {
        return ApiResponse.success("Phiên điểm danh", service.get(p.getUserId(), sessionId));
    }

    @PostMapping("/attendance/sessions/{sessionId}/records")
    public ApiResponse<SessionDetail> mark(@AuthenticationPrincipal AuthenticatedUserPrincipal p,
            @PathVariable Long sessionId, @Valid @RequestBody MarkBatch request) {
        return ApiResponse.success("Đã lưu điểm danh", service.mark(p.getUserId(), sessionId, request));
    }

    @PutMapping("/attendance/records/{recordId}")
    public ApiResponse<RecordView> update(@AuthenticationPrincipal AuthenticatedUserPrincipal p,
            @PathVariable Long recordId, @Valid @RequestBody UpdateRecord request) {
        return ApiResponse.success("Đã cập nhật điểm danh", service.update(p.getUserId(), recordId, request));
    }

    @PutMapping("/attendance/sessions/{sessionId}/status")
    public ApiResponse<SessionDetail> status(@AuthenticationPrincipal AuthenticatedUserPrincipal p,
            @PathVariable Long sessionId, @Valid @RequestBody UpdateSessionStatus request) {
        return ApiResponse.success("Đã cập nhật phiên", service.changeStatus(p.getUserId(), sessionId, request));
    }

    @GetMapping("/attendance")
    public ApiResponse<Page<SessionView>> history(@AuthenticationPrincipal AuthenticatedUserPrincipal p,
            @RequestParam(required=false) Long classId, @RequestParam(required=false) Long subjectId,
            @RequestParam(required=false) Long sessionId, @RequestParam(required=false) LocalDate from,
            @RequestParam(required=false) LocalDate to, @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        return ApiResponse.success("Lịch sử điểm danh", service.history(p.getUserId(), classId, subjectId, sessionId, from, to, page, size));
    }

    @GetMapping("/classes/{classId}/attendance/summary")
    public ApiResponse<Summary> summary(@AuthenticationPrincipal AuthenticatedUserPrincipal p,
            @PathVariable Long classId, @RequestParam(required=false) LocalDate from,
            @RequestParam(required=false) LocalDate to) {
        return ApiResponse.success("Tổng hợp điểm danh", service.summary(p.getUserId(), classId, from, to));
    }

    @GetMapping("/homeroom/attendance-summary")
    public ApiResponse<HomeroomSummary> homeroom(@AuthenticationPrincipal AuthenticatedUserPrincipal p,
            @RequestParam(required=false) Long classId, @RequestParam(required=false) LocalDate from,
            @RequestParam(required=false) LocalDate to) {
        return ApiResponse.success("Chuyên cần lớp chủ nhiệm", service.homeroomSummary(p.getUserId(), classId, from, to));
    }
}
