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
    INVALID_OR_EXPIRED_RESET_CODE(HttpStatus.BAD_REQUEST, 40002, "Invalid or expired reset code"),
    INVALID_OR_EXPIRED_RESET_TOKEN(HttpStatus.BAD_REQUEST, 40003, "Invalid or expired reset token"),
    PASSWORDS_DO_NOT_MATCH(HttpStatus.BAD_REQUEST, 40004, "Passwords do not match"),
    EMAIL_DELIVERY_FAILED(HttpStatus.SERVICE_UNAVAILABLE, 50301, "Unable to send reset email"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, 40401, "Resource not found"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 50000, "An unexpected error occurred"),
    UNSUCCESS_SENDING_SMS(HttpStatus.BAD_REQUEST, 40103, "Send Email Unsuccessfully");
    ;

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
