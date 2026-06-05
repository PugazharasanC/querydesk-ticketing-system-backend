package com.query.ticket.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Validation errors ─────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            fieldErrors.put(field, error.getDefaultMessage());
        });
        return error(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors.toString());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST,
                "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'");
    }

    // ── Auth errors ───────────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled(DisabledException ex) {
        return error(HttpStatus.UNAUTHORIZED,
                "Your account has been disabled. Please contact your administrator.");
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Map<String, Object>> handleLocked(LockedException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Your account is locked. Please contact support.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action.");
    }

    // ── Business logic errors ─────────────────────────────────────────────────

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        String message = ex.getMessage();
        log.warn("Business error: {}", message);

        // EMAIL_NOT_VERIFIED is a special case — return 403 with the message intact
        if (message != null && message.startsWith("EMAIL_NOT_VERIFIED:")) {
            return error(HttpStatus.FORBIDDEN, message);
        }

        // Map common messages to appropriate HTTP status codes
        if (message != null) {
            if (message.contains("not found")) {
                return error(HttpStatus.NOT_FOUND, message);
            }
            if (message.contains("Access denied") || message.contains("permission")) {
                return error(HttpStatus.FORBIDDEN, message);
            }
            if (message.contains("already") || message.contains("already registered")) {
                return error(HttpStatus.CONFLICT, message);
            }
            if (message.contains("Invalid") || message.contains("expired")
                    || message.contains("incorrect")) {
                return error(HttpStatus.UNAUTHORIZED, message);
            }
            if (message.contains("closed") || message.contains("locked")
                    || message.contains("Escalated tickets")
                    || message.contains("Only managers")
                    || message.contains("assigned to you")) {
                return error(HttpStatus.UNPROCESSABLE_CONTENT, message);
            }
        }

        return error(HttpStatus.BAD_REQUEST, message != null ? message : "An error occurred");
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again.");
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message,
                                                       String detail) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "detail", detail,
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}