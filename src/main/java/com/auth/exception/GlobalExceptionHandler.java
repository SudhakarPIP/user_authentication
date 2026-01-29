package com.auth.exception;

import com.auth.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for centralized error handling across all REST controllers.
 * 
 * Handles all exceptions thrown by controllers and services, providing consistent
 * error responses with appropriate HTTP status codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation errors from @Valid annotation.
     * Returns field-level validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException e) {
        logger.error("Validation error: {}", e.getMessage());
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Handles missing required request parameters.
     * Used when required @RequestParam is missing.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        logger.error("Missing request parameter: {}", e.getParameterName());
        ApiResponse response = ApiResponse.builder()
                .message("Required parameter '" + e.getParameterName() + "' is missing")
                .success(false)
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles IllegalArgumentException - used for invalid input/business logic errors.
     * Examples: Username/email already exists, invalid credentials, invalid token.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.error("IllegalArgumentException: {}", e.getMessage());
        ApiResponse response = ApiResponse.builder()
                .message(e.getMessage())
                .success(false)
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles IllegalStateException - used for invalid state errors.
     * Example: Account not activated (email not verified).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse> handleIllegalStateException(IllegalStateException e) {
        logger.error("IllegalStateException: {}", e.getMessage());
        ApiResponse response = ApiResponse.builder()
                .message(e.getMessage())
                .success(false)
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handles all other unexpected exceptions.
     * This is a catch-all handler for any unhandled exceptions.
     * In production, this prevents exposing internal error details to clients.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception e) {
        logger.error("Unexpected error: {}", e.getMessage(), e);
        ApiResponse response = ApiResponse.builder()
                .message("An unexpected error occurred. Please try again later.")
                .success(false)
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

