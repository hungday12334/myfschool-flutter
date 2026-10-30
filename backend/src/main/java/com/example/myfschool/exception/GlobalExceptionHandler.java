package com.example.myfschool.exception;

import com.example.myfschool.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getDefaultMessage(), fieldErrors));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(Exception exception) {
        // Do not log the parser message: it may embed sensitive request-body values.
        log.debug("Rejected malformed request of type {}", exception.getClass().getSimpleName());
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getDefaultMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException exception) {
        ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getDefaultMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("Unexpected server error", exception);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getDefaultMessage()));
    }
}
