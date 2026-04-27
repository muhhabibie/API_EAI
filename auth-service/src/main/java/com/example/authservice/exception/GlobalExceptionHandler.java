package com.example.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.authservice.dto.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        String msg = ex.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;

        if (msg != null && (msg.toLowerCase().contains("tidak ditemukan") || msg.toLowerCase().contains("not found"))) {
            status = HttpStatus.NOT_FOUND;
        } else if (msg != null && msg.toLowerCase().contains("already exists")) {
            status = HttpStatus.CONFLICT;
        }

        return ResponseEntity.status(status)
                .body(ApiResponse.error(msg));
    }
}
