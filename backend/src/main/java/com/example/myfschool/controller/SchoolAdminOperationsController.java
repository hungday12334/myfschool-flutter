package com.example.myfschool.controller;

import com.example.myfschool.dto.AdminOperationsDtos.*;
import com.example.myfschool.dto.response.ApiResponse;
import com.example.myfschool.enums.AnnouncementStatus;
import com.example.myfschool.security.AuthenticatedUserPrincipal;
import com.example.myfschool.service.SchoolAdminOperationsService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin-school")
@RequiredArgsConstructor
public class SchoolAdminOperationsController {
    private final SchoolAdminOperationsService service;
    private ApiResponse<?> ok(Object data){return ApiResponse.success("Success",data);}
    @GetMapping("/teaching-assignments") public ApiResponse<?> assignments(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@RequestParam(required=false) Long classId,@RequestParam(required=false) Long teacherId,@RequestParam(required=false) Long subjectId,@RequestParam(required=false) Long semesterId,@RequestParam(required=false) Long academicYearId,@RequestParam(required=false) com.example.myfschool.enums.ActiveStatus status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ok(service.assignments(p.getUserId(),classId,teacherId,subjectId,semesterId,academicYearId,status,page,size));}
    @GetMapping("/timetable") public ApiResponse<?> timetable(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@RequestParam(required=false) Long classId,@RequestParam(required=false) Long teacherId,@RequestParam(required=false) Long semesterId,@RequestParam(required=false) LocalDate date,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ok(service.timetable(p.getUserId(),classId,teacherId,semesterId,date,page,size));}
    @GetMapping("/timetable/{id}") public ApiResponse<?> timetable(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id){return ok(service.timetableDetail(p.getUserId(),id));}
    @PostMapping("/timetable") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<?> createSlot(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@Valid @RequestBody Timetable r){return ok(service.timetable(p.getUserId(),null,r));}
    @PutMapping("/timetable/{id}") public ApiResponse<?> updateSlot(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@Valid @RequestBody Timetable r){return ok(service.timetable(p.getUserId(),id,r));}
    @DeleteMapping("/timetable/{id}") public ApiResponse<?> deleteSlot(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id){service.deleteTimetable(p.getUserId(),id);return ok(null);}
    @GetMapping("/school") public ApiResponse<?> school(@AuthenticationPrincipal AuthenticatedUserPrincipal p){return ok(service.school(p.getUserId()));}
    @PutMapping("/school") public ApiResponse<?> school(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@Valid @RequestBody SchoolInfo r){return ok(service.school(p.getUserId(),r));}
    @PostMapping("/announcements") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<?> create(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@Valid @RequestBody Notice r){return ok(service.createNotice(p.getUserId(),r));}
    @PostMapping("/announcements/{id}/publish") public ApiResponse<?> publish(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id){return ok(service.publish(p.getUserId(),id));}
    @PostMapping("/announcements/{id}/retract") public ApiResponse<?> retract(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id){return ok(service.retract(p.getUserId(),id));}
    @GetMapping("/announcements/{id}") public ApiResponse<?> notice(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id){return ok(service.notice(p.getUserId(),id));}
    @GetMapping("/announcements") public ApiResponse<?> notices(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@RequestParam(required=false) AnnouncementStatus status,@RequestParam(required=false) Long authorId,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ok(service.notices(p.getUserId(),status,authorId,from,to,page,size));}
    @GetMapping("/statistics/students") public ApiResponse<?> students(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@RequestParam(required=false) Long academicYearId){return ok(service.students(p.getUserId(),academicYearId));}
    @GetMapping("/statistics/teachers-classes") public ApiResponse<?> teachers(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@RequestParam(required=false) Long academicYearId){return ok(service.teachersClasses(p.getUserId(),academicYearId));}
    @GetMapping("/statistics/attendance") public ApiResponse<?> attendance(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@RequestParam(required=false) Long classId,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to){return ok(service.attendance(p.getUserId(),classId,from,to));}
    @GetMapping("/statistics/academics") public ApiResponse<?> academics(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@RequestParam(required=false) Long classId,@RequestParam(required=false) Long semesterId){return ok(service.academics(p.getUserId(),classId,semesterId));}
    @GetMapping("/audit-logs") public ApiResponse<?> logs(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@RequestParam(required=false) Long actorId,@RequestParam(required=false) String action,@RequestParam(required=false) String entityType,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ok(service.auditLogs(p.getUserId(),actorId,action,entityType,from,to,page,size));}
}
