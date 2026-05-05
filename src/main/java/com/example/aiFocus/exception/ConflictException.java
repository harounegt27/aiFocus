package com.example.aiFocus.exception;

/**
 * Exception thrown when a request conflicts with the current state of the server.
 * Common use cases: duplicate email, resource already exists, etc.
 * Should result in HTTP 409 Conflict response.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

