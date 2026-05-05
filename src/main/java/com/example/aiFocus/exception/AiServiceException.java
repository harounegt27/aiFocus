package com.example.aiFocus.exception;

/**
 * Exception thrown when an error occurs in AI service communication or processing.
 * Could indicate service unavailability or unexpected errors from AI providers.
 * Should result in HTTP 502 Bad Gateway response.
 */
public class AiServiceException extends RuntimeException {
    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

