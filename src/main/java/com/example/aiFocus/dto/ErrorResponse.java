package com.example.aiFocus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Data Transfer Object for error responses.
 * Provides a standardized format for all error responses in the API.
 */
@Data
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    private String timestamp;
    private String path;
}

