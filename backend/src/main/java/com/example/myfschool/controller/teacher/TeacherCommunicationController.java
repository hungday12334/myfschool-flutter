package com.example.myfschool.controller.teacher;

import static com.example.myfschool.dto.teacher.TeacherCommunicationDtos.*;

import com.example.myfschool.dto.response.ApiResponse;
import com.example.myfschool.enums.AnnouncementStatus;
import com.example.myfschool.enums.LeaveRequestStatus;
import com.example.myfschool.security.AuthenticatedUserPrincipal;
import com.example.myfschool.service.teacher.TeacherCommunicationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherCommunicationController {
    private final TeacherCommunicationService service;

    @PostMapping("/announcements")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AnnouncementResponse> create(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody CreateAnnouncement request) {
        return ApiResponse.success("Đã lưu thông báo nháp", service.createAnnouncement(principal.getUserId(), request));
    }

    @PostMapping("/announcements/{announcementId}/publish")
    public ApiResponse<AnnouncementResponse> publish(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long announcementId) {
        return ApiResponse.success("Đã phát hành thông báo trong ứng dụng", service.publish(principal.getUserId(), announcementId));
    }

    @GetMapping("/announcements")
    public ApiResponse<PageResponse<AnnouncementResponse>> announcements(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(required = false) Long classId, @RequestParam(required = false) AnnouncementStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Lịch sử thông báo", service.announcements(principal.getUserId(), classId, status, page, size));
    }

    @GetMapping("/announcements/{announcementId}")
    public ApiResponse<AnnouncementResponse> announcement(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long announcementId) {
        return ApiResponse.success("Thông báo", service.announcement(principal.getUserId(), announcementId));
    }

    @GetMapping("/announcements/{announcementId}/reads")
    public ApiResponse<AnnouncementReads> reads(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long announcementId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Tình trạng đọc thông báo", service.reads(principal.getUserId(), announcementId, page, size));
    }

    @GetMapping("/leave-requests")
    public ApiResponse<PageResponse<LeaveResponse>> leaves(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(required = false) Long classId, @RequestParam(required = false) LeaveRequestStatus status,
            @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Đơn nghỉ phép", service.leaves(principal.getUserId(), classId, status, from, to, page, size));
    }

    @GetMapping("/leave-requests/{requestId}")
    public ApiResponse<LeaveResponse> leave(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long requestId) {
        return ApiResponse.success("Chi tiết đơn nghỉ phép", service.leave(principal.getUserId(), requestId));
    }

    @PatchMapping("/leave-requests/{requestId}/decision")
    public ApiResponse<LeaveResponse> decision(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long requestId, @Valid @RequestBody LeaveDecision request) {
        return ApiResponse.success("Đã xử lý đơn nghỉ phép", service.decide(principal.getUserId(), requestId, request));
    }

    @GetMapping("/leave-requests/{requestId}/history")
    public ApiResponse<List<HistoryResponse>> history(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long requestId) {
        return ApiResponse.success("Lịch sử xử lý đơn", service.history(principal.getUserId(), requestId));
    }
}
