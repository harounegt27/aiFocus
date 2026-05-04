package com.example.aiFocus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Data Transfer Object for authentication responses.
 * Contains JWT token and expiration time.
 */
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private long expiresIn;
}

