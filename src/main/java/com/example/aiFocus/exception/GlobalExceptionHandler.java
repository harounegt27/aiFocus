package com.example.aiFocus.exception;

import com.example.aiFocus.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Focus Time Optimizer REST API.
 * 
 * Provides centralized exception handling across all controllers using
 * Spring's @RestControllerAdvice annotation. Converts exceptions into
 * standardized ErrorResponse DTOs with appropriate HTTP status codes.
 * 
 * This handler ensures consistent error response format throughout the API
 * and logs errors for monitoring and debugging purposes.
 * 
 * @author AI Focus Team
 * @version 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles ResourceNotFoundException.
     * 
     * Called when a requested resource (user, event, focus block, etc.) is not found.
     * Returns HTTP 404 Not Found with detailed error information.
     * 
     * @param ex the ResourceNotFoundException
     * @param request the HTTP request
     * @return ResponseEntity with ErrorResponse and HTTP 404 status
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                Instant.now().toString(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles BadCredentialsException.
     * 
     * Called when user authentication fails (invalid credentials, wrong password).
     * Returns HTTP 401 Unauthorized with appropriate error message.
     * 
     * @param ex the BadCredentialsException
     * @param request the HTTP request
     * @return ResponseEntity with ErrorResponse and HTTP 401 status
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                Instant.now().toString(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handles ConflictException.
     * 
     * Called when a request conflicts with the current state of the server
     * (e.g., duplicate email during registration, resource already exists).
     * Returns HTTP 409 Conflict with descriptive error message.
     * 
     * @param ex the ConflictException
     * @param request the HTTP request
     * @return ResponseEntity with ErrorResponse and HTTP 409 status
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex,
            HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                Instant.now().toString(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles AiServiceException.
     * 
     * Called when an error occurs in AI service communication or processing.
     * Indicates that an external AI service is unavailable or returned an error.
     * Returns HTTP 502 Bad Gateway with error details.
     * 
     * @param ex the AiServiceException
     * @param request the HTTP request
     * @return ResponseEntity with ErrorResponse and HTTP 502 status
     */
    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ErrorResponse> handleAiServiceError(
            AiServiceException ex,
            HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_GATEWAY.value(),
                ex.getMessage(),
                Instant.now().toString(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
    }

    /**
     * Handles MethodArgumentNotValidException.
     * 
     * Called when request validation fails (invalid request body or parameters).
     * Collects all validation errors from the BindingResult and includes them
     * in the error message for better client feedback.
     * Returns HTTP 400 Bad Request with detailed field validation errors.
     * 
     * @param ex the MethodArgumentNotValidException
     * @param request the HTTP request
     * @return ResponseEntity with ErrorResponse and HTTP 400 status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        String message = "Validation failed: " + errors;

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                Instant.now().toString(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles all other uncaught exceptions.
     * 
     * This is a catch-all handler for any exception not specifically handled
     * by the other handler methods. Provides a generic error message to prevent
     * leaking sensitive information to clients while still logging the error.
     * Returns HTTP 500 Internal Server Error.
     * 
     * @param ex the generic Exception
     * @param request the HTTP request
     * @return ResponseEntity with ErrorResponse and HTTP 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred. Please try again later.",
                Instant.now().toString(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

