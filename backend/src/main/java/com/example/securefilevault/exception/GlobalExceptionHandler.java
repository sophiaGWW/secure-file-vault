package com.example.securefilevault.exception;

import com.example.securefilevault.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
        // 業務例外は Service が指定した HTTP status とメッセージで返す。
        HttpStatus status = exception.getStatus();
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        // Bean Validation のエラーを field: message 形式にまとめて返す。
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining(", "));
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(400, "Bad Request", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
        // 想定外エラーでは内部詳細を隠し、汎用メッセージだけを返す。
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal Server Error", "Unexpected server error"));
    }

    private String formatFieldError(FieldError fieldError) {
        // どの項目が validation に失敗したかを API 利用側に分かりやすくする。
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
