package com.example.aiFocus.exception;

/**
 * Exception thrown when provided credentials are invalid or authentication fails.
 * Should result in HTTP 401 Unauthorized response.
 */
public class BadCredentialsException extends RuntimeException {
    public BadCredentialsException(String message) {
        super(message);
    }

    public BadCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}

