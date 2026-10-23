package com.example.myfschool.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, 40001, "Invalid request"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 40100, "Authentication is required"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, 40101, "Invalid phone number or password"),
    INVALID_OR_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, 40102, "Invalid or expired access token"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, 40300, "Access denied"),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, 40301, "User account is locked"),
    ACCOUNT_INACTIVE(HttpStatus.FORBIDDEN, 40302, "User account is inactive"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, 40401, "Resource not found"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 50000, "An unexpected error occurred");

    private final HttpStatus httpStatus;
    private final int code;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, int code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
