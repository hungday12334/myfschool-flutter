package com.example.myfschool.controller;

import com.example.myfschool.dto.response.ApiResponse;
import com.example.myfschool.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Handle database unique-key races without exposing SQL or account data. */
@RestControllerAdvice(assignableTypes={SchoolAdminAccountController.class,SchoolAdminSchoolController.class})
public class SchoolAdminAccountExceptionHandler {
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(DataIntegrityViolationException ex) {
        ErrorCode code=ErrorCode.RESOURCE_CONFLICT;
        return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.error(code.getCode(),code.getDefaultMessage()));
    }
}
