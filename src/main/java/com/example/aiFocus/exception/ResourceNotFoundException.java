package com.example.aiFocus.exception;

/**
 * Exception thrown when a requested resource is not found.
 * Should result in HTTP 404 Not Found response.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

