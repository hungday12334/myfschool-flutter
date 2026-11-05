package com.example.myfschool.controller;

import com.example.myfschool.dto.AdminSchoolDtos.*;
import com.example.myfschool.dto.response.ApiResponse;
import com.example.myfschool.security.AuthenticatedUserPrincipal;
import com.example.myfschool.service.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin-school")
@RequiredArgsConstructor
public class SchoolAdminSchoolController {
    private final SchoolAdminSchoolService service;
    private final SchoolAdminImportService imports;
    private ApiResponse<?> ok(Object data){return ApiResponse.success("Success",data);}
    @GetMapping("/{resource:teachers|students|parents|academic-years|semesters|subjects|classes|teaching-assignments}")
    public ApiResponse<?> list(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable String resource,@RequestParam(required=false) String keyword,@RequestParam(required=false) Long academicYearId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ok(service.list(p.getUserId(),resource,keyword,academicYearId,page,size));}
    @GetMapping("/{resource:teachers|students|parents|academic-years|semesters|subjects|classes|teaching-assignments}/{id}")
    public ApiResponse<?> get(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable String resource,@PathVariable Long id){return ok(service.get(p.getUserId(),resource,id));}
    @PutMapping("/teachers/{id}") public ApiResponse<?> teacher(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@Valid @RequestBody TeacherProfile r){return ok(service.teacher(p.getUserId(),id,r));}
    @PostMapping("/students") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<?> student(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@Valid @RequestBody StudentProfile r){return ok(service.student(p.getUserId(),null,r));}
    @PutMapping("/students/{id}") public ApiResponse<?> student(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@Valid @RequestBody StudentProfile r){return ok(service.student(p.getUserId(),id,r));}
    @PostMapping("/parents") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<?> parent(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@Valid @RequestBody ParentProfile r){return ok(service.parent(p.getUserId(),null,r));}
    @PutMapping("/parents/{id}") public ApiResponse<?> parent(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@Valid @RequestBody ParentProfile r){return ok(service.parent(p.getUserId(),id,r));}
    @GetMapping("/parents/{id}/students") public ApiResponse<?> links(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ok(service.links(p.getUserId(),id,page,size));}
    @PostMapping("/parents/{parentId}/students/{studentId}") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<?> link(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long parentId,@PathVariable Long studentId,@Valid @RequestBody ParentLink r){return ok(service.link(p.getUserId(),parentId,studentId,r,true));}
    @PutMapping("/parents/{parentId}/students/{studentId}") public ApiResponse<?> changeLink(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long parentId,@PathVariable Long studentId,@Valid @RequestBody ParentLink r){return ok(service.link(p.getUserId(),parentId,studentId,r,false));}
    @DeleteMapping("/parents/{parentId}/students/{studentId}") public ApiResponse<?> unlink(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long parentId,@PathVariable Long studentId){service.unlink(p.getUserId(),parentId,studentId);return ok(null);}
    @PostMapping("/academic-years") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<?> year(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@Valid @RequestBody Year r){return ok(service.year(p.getUserId(),null,r));}
    @PutMapping("/academic-years/{id}") public ApiResponse<?> year(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@Valid @RequestBody Year r){return ok(service.year(p.getUserId(),id,r));}
    @PostMapping("/semesters") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<?> term(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@Valid @RequestBody Term r){return ok(service.term(p.getUserId(),null,r));}
    @PutMapping("/semesters/{id}") public ApiResponse<?> term(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@Valid @RequestBody Term r){return ok(service.term(p.getUserId(),id,r));}
    @PostMapping("/subjects") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<?> subject(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@Valid @RequestBody SubjectProfile r){return ok(service.subject(p.getUserId(),null,r));}
    @PutMapping("/subjects/{id}") public ApiResponse<?> subject(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@Valid @RequestBody SubjectProfile r){return ok(service.subject(p.getUserId(),id,r));}
    @PostMapping("/classes") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<?> schoolClass(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@Valid @RequestBody ClassProfile r){return ok(service.schoolClass(p.getUserId(),null,r));}
    @PutMapping("/classes/{id}") public ApiResponse<?> schoolClass(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@Valid @RequestBody ClassProfile r){return ok(service.schoolClass(p.getUserId(),id,r));}
    @GetMapping("/classes/{id}/students") public ApiResponse<?> roster(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@RequestParam(required=false) LocalDate date,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ok(service.roster(p.getUserId(),id,date,page,size));}
    @PostMapping("/classes/{id}/students") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<?> enroll(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@Valid @RequestBody Enrollment r){return ok(service.enroll(p.getUserId(),id,r));}
    @PostMapping("/enrollments/{id}/transfer") public ApiResponse<?> move(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@Valid @RequestBody Move r){return ok(service.move(p.getUserId(),id,r));}
    @PatchMapping("/enrollments/{id}/end") public ApiResponse<?> end(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@Valid @RequestBody EndEnrollment r){service.endEnrollment(p.getUserId(),id,r);return ok(null);}
    @PutMapping("/classes/{id}/homeroom-teacher") public ApiResponse<?> homeroom(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@Valid @RequestBody Homeroom r){return ok(service.homeroom(p.getUserId(),id,r));}
    @PostMapping("/teaching-assignments") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<?> assignment(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@Valid @RequestBody Assignment r){return ok(service.assignment(p.getUserId(),null,r));}
    @PutMapping("/teaching-assignments/{id}") public ApiResponse<?> assignment(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@Valid @RequestBody Assignment r){return ok(service.assignment(p.getUserId(),id,r));}
    @PostMapping(value="/student-imports",consumes="multipart/form-data") public ApiResponse<?> studentImport(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@RequestPart("file") MultipartFile file){return ok(imports.runStudents(p.getUserId(),file));}
    @GetMapping("/student-imports/{id}") public ApiResponse<?> importResult(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id){return ok(imports.get(p.getUserId(),id));}
    @GetMapping("/student-imports/{id}/errors") public ApiResponse<?> importErrors(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ok(imports.errors(p.getUserId(),id,page,size));}
}
