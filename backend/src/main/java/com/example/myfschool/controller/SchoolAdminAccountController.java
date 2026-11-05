package com.example.myfschool.controller;

import com.example.myfschool.dto.AdminAccountDtos.*;
import com.example.myfschool.dto.response.ApiResponse;
import com.example.myfschool.enums.UserStatus;
import com.example.myfschool.security.AuthenticatedUserPrincipal;
import com.example.myfschool.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin-school")
@RequiredArgsConstructor
public class SchoolAdminAccountController {
    private final SchoolAdminAccountService service;
    private final SchoolAdminImportService imports;
    @GetMapping("/dashboard") public ApiResponse<?> dashboard(@AuthenticationPrincipal AuthenticatedUserPrincipal p){return ApiResponse.success("School dashboard",service.dashboard(p.getUserId()));}
    @PostMapping("/accounts/teachers") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ApiResponse<?> create(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@Valid @RequestBody CreateTeacher r){return ApiResponse.success("Teacher account created",service.create(p.getUserId(),r));}
    @GetMapping("/accounts") public ApiResponse<?> list(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@RequestParam(required=false) String keyword,@RequestParam(required=false) UserStatus status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ApiResponse.success("Accounts",service.list(p.getUserId(),keyword,status,page,size));}
    @PutMapping("/accounts/{id}") public ApiResponse<?> update(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@Valid @RequestBody UpdateAccount r){return ApiResponse.success("Account updated",service.update(p.getUserId(),id,r));}
    @PatchMapping("/accounts/{id}/status") public ApiResponse<?> status(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@Valid @RequestBody ChangeStatus r){return ApiResponse.success("Account status updated",service.status(p.getUserId(),id,r.status()));}
    @PostMapping("/accounts/{id}/reset-password") public ApiResponse<?> reset(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@Valid @RequestBody ResetPassword r){service.reset(p.getUserId(),id,r.password());return ApiResponse.success("Password reset",null);}
    @PutMapping("/accounts/{id}/roles") public ApiResponse<?> roles(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@Valid @RequestBody ChangeRoles r){return ApiResponse.success("Roles updated",service.roles(p.getUserId(),id,r.roles()));}
    @PostMapping(value="/account-imports",consumes="multipart/form-data") public ApiResponse<?> upload(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@RequestPart("file") MultipartFile file){return ApiResponse.success("Import processed",imports.run(p.getUserId(),file));}
    @GetMapping("/account-imports/{id}") public ApiResponse<?> result(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id){return ApiResponse.success("Import result",imports.get(p.getUserId(),id));}
    @GetMapping("/account-imports/{id}/errors") public ApiResponse<?> errors(@AuthenticationPrincipal AuthenticatedUserPrincipal p,@PathVariable Long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ApiResponse.success("Import errors",imports.errors(p.getUserId(),id,page,size));}
}
