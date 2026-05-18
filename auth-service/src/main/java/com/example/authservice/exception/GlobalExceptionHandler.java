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

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex) {
        String msg = "Data sudah ada atau melanggar aturan database.";
        if (ex.getCause() != null && ex.getCause().getCause() != null) {
            String sqlMsg = ex.getCause().getCause().getMessage();
            if (sqlMsg.contains("Duplicate entry")) {
                if (sqlMsg.contains("email")) {
                    msg = "Email sudah digunakan oleh akun lain.";
                } else if (sqlMsg.contains("username")) {
                    msg = "Username sudah digunakan oleh akun lain.";
                } else {
                    msg = "Data sudah ada di sistem.";
                }
            }
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(msg));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Forbidden: Anda tidak memiliki izin untuk mengakses resource ini."));
    }
}
