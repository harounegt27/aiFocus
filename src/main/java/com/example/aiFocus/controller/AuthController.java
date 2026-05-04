package com.example.aiFocus.controller;

import com.example.aiFocus.dto.AuthResponse;
import com.example.aiFocus.dto.LoginRequest;
import com.example.aiFocus.dto.RegisterRequest;
import com.example.aiFocus.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Authentication REST controller.
 * Provides endpoints for user registration and login.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * User registration endpoint.
     * Creates a new user account and returns JWT token for authentication.
     *
     * @param request registration request with user details
     * @return ResponseEntity with AuthResponse (200 OK)
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * User login endpoint.
     * Authenticates user credentials and returns JWT token.
     *
     * @param request login request with email and password
     * @return ResponseEntity with AuthResponse (200 OK)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}

