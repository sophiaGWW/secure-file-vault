package com.example.securefilevault.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    // 業務エラーごとに返したい HTTP status を保持する。
    private final HttpStatus status;

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public BusinessException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
