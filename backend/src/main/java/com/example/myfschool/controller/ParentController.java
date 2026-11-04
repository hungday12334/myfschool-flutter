package com.example.myfschool.controller;

import com.example.myfschool.dto.request.ParentDeviceRequest;
import com.example.myfschool.dto.request.ParentDeviceDeleteRequest;
import com.example.myfschool.dto.request.ParentLeaveRequest;
import com.example.myfschool.dto.response.ApiResponse;
import com.example.myfschool.dto.response.ParentDashboardResponse;
import com.example.myfschool.enums.LeaveRequestStatus;
import com.example.myfschool.security.AuthenticatedUserPrincipal;
import com.example.myfschool.service.ParentService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parent")
@RequiredArgsConstructor
public class ParentController {
    private final ParentService service;
    private Long uid(AuthenticatedUserPrincipal principal) { return principal.getUserId(); }

    @GetMapping("/dashboard") public ResponseEntity<ApiResponse<ParentDashboardResponse>> dashboard(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@RequestParam(required=false) Long studentId){return ResponseEntity.ok(ApiResponse.success("Parent dashboard",service.dashboard(uid(p),studentId)));}
    @GetMapping("/children") public ResponseEntity<ApiResponse<List<Map<String,Object>>>> children(@AuthenticationPrincipal AuthenticatedUserPrincipal p){return ResponseEntity.ok(ApiResponse.success("Linked children",service.children(uid(p))));}
    @GetMapping("/children/{studentId}/context") public ResponseEntity<ApiResponse<Map<String,Object>>> context(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long studentId){return ResponseEntity.ok(ApiResponse.success("Child context",service.childContext(uid(p),studentId)));}
    @GetMapping("/children/{studentId}") public ResponseEntity<ApiResponse<Map<String,Object>>> child(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long studentId){return ResponseEntity.ok(ApiResponse.success("Student profile",service.childContext(uid(p),studentId)));}
    @GetMapping("/children/{studentId}/class") public ResponseEntity<ApiResponse<Map<String,Object>>> classInfo(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long studentId){return ResponseEntity.ok(ApiResponse.success("Class context",service.classInfo(uid(p),studentId)));}
    @GetMapping("/children/{studentId}/timetable") public ResponseEntity<ApiResponse<List<Map<String,Object>>>> timetable(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long studentId,@RequestParam(required=false) Long academicYearId,@RequestParam(required=false) Long semesterId){return ResponseEntity.ok(ApiResponse.success("Timetable",service.timetable(uid(p),studentId,academicYearId,semesterId)));}
    @GetMapping("/children/{studentId}/academic-results") public ResponseEntity<ApiResponse<List<Map<String,Object>>>> results(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long studentId,@RequestParam(required=false) Long academicYearId,@RequestParam(required=false) Long semesterId){return ResponseEntity.ok(ApiResponse.success("Academic results",service.academicResults(uid(p),studentId,academicYearId,semesterId)));}
    @GetMapping("/children/{studentId}/subjects/{subjectId}/grades") public ResponseEntity<ApiResponse<List<Map<String,Object>>>> grades(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long studentId,@PathVariable Long subjectId,@RequestParam Long semesterId){return ResponseEntity.ok(ApiResponse.success("Subject grades",service.subjectGrades(uid(p),studentId,subjectId,semesterId)));}
    @GetMapping("/children/{studentId}/evaluations") public ResponseEntity<ApiResponse<Map<String,Object>>> evaluations(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long studentId,@RequestParam(required=false) Long academicYearId,@RequestParam(required=false) Long semesterId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ResponseEntity.ok(ApiResponse.success("Evaluations",service.evaluations(uid(p),studentId,academicYearId,semesterId,page,size)));}
    @GetMapping("/children/{studentId}/attendance") public ResponseEntity<ApiResponse<Map<String,Object>>> attendance(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long studentId,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ResponseEntity.ok(ApiResponse.success("Attendance history",service.attendance(uid(p),studentId,from,to,page,size)));}
    @GetMapping("/children/{studentId}/attendance/summary") public ResponseEntity<ApiResponse<Map<String,Object>>> attendanceSummary(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long studentId,@RequestParam(required=false) Long academicYearId,@RequestParam(required=false) Long semesterId){return ResponseEntity.ok(ApiResponse.success("Attendance summary",service.attendanceSummaryFor(uid(p),studentId,academicYearId,semesterId)));}
    @GetMapping("/announcements") public ResponseEntity<ApiResponse<Map<String,Object>>> announcements(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ResponseEntity.ok(ApiResponse.success("Announcements",service.announcements(uid(p),page,size)));}
    @GetMapping("/announcements/{announcementId}") public ResponseEntity<ApiResponse<Map<String,Object>>> announcement(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long announcementId){return ResponseEntity.ok(ApiResponse.success("Announcement",service.announcement(uid(p),announcementId)));}
    @PostMapping("/announcements/{announcementId}/read") public ResponseEntity<ApiResponse<Void>> markRead(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long announcementId){service.markRead(uid(p),announcementId);return ResponseEntity.ok(ApiResponse.success("Announcement marked read",null));}
    @PostMapping("/devices") public ResponseEntity<ApiResponse<Void>> registerDevice(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@Valid @RequestBody ParentDeviceRequest request){service.registerDevice(uid(p),request);return ResponseEntity.ok(ApiResponse.success("Device registered",null));}
    @DeleteMapping("/devices") public ResponseEntity<ApiResponse<Void>> removeDevice(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@Valid @RequestBody ParentDeviceDeleteRequest request){service.removeDevice(uid(p),request.token());return ResponseEntity.ok(ApiResponse.success("Device removed",null));}
    @PostMapping("/children/{studentId}/leave-requests") public ResponseEntity<ApiResponse<Map<String,Object>>> submitLeave(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long studentId,@Valid @RequestBody ParentLeaveRequest request){return ResponseEntity.ok(ApiResponse.success("Leave request submitted",service.submitLeave(uid(p),studentId,request)));}
    @GetMapping("/leave-requests/{requestId}") public ResponseEntity<ApiResponse<Map<String,Object>>> leave(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long requestId){return ResponseEntity.ok(ApiResponse.success("Leave request",service.leave(uid(p),requestId)));}
    @GetMapping("/children/{studentId}/leave-requests") public ResponseEntity<ApiResponse<Map<String,Object>>> leaveHistory(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long studentId,@RequestParam(required=false) LeaveRequestStatus status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ResponseEntity.ok(ApiResponse.success("Leave request history",service.leaveHistory(uid(p),studentId,status,page,size)));}
}
